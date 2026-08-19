package ais.action.master.asset;

import java.util.List;

import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Vbox;

import ais.action.master.asset.helper.DetailTransaksiAssetHelper;
import ais.action.master.helper.RevisiHelper;
import ais.action.report.format1.library.LaporanStokItem;
import ais.action.report.format1.library.LaporanTrackingStokItem;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.asset.DetailTransaksiAsset;
import ais.database.model.asset.MasterAsset;
import ais.database.model.rab.SatuanKerja;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * <h3>MonitorStokAssetAction — Kontroler Dasbor Monitor Stok Aset Habis Pakai</h3>
 *
 * <b>Untuk apa:</b><br>
 * Kelas ini merupakan kontroler ZKoss untuk halaman Monitor Stok Aset yang berfungsi
 * sebagai dasbor pemantauan saldo stok aset habis pakai ({@code MasterAsset.TIPE_HABIS_PAKAI})
 * per satuan kerja pada suatu tanggal tertentu. Halaman ini memungkinkan pengguna
 * melihat kondisi stok terkini, termasuk nama aset, kode, satuan kerja, jumlah stok,
 * dan informasi transaksi terakhir (tanggal, kode transaksi, dan detail singkat).
 * Dari setiap baris, pengguna dapat mencetak laporan stok ({@code LaporanStokItem})
 * atau laporan pelacakan stok ({@code LaporanTrackingStokItem}). Tersedia juga tombol
 * cetak global di toolbar untuk keseluruhan laporan.
 * <br><br>
 * Berbeda dari kontroler CRUD master, kelas ini adalah kontroler pemantauan (monitor)
 * sehingga tidak memiliki operasi tambah/ubah/hapus data. Data stok dihitung secara
 * dinamis dari tabel {@code asset.detail_transaksi_asset} menggunakan query SQL native
 * dengan agregasi SUM berbobot (qty + qtybonus) * jenis_transaksi, yang menghasilkan
 * saldo stok neto per aset per satuan kerja.
 *
 * <b>Cara kerja:</b><br>
 * Saat halaman dimuat, {@code doBeforeCompose} memeriksa keamanan. {@code doAfterCompose}
 * mendaftarkan listener paginasi dan timer refresh otomatis (via
 * {@code Common.createDefaultTimer}), serta menyembunyikan tombol cetak untuk pengguna
 * mahasiswa atau dosen. Query stok dilakukan melalui SQL native di {@code initCriteria}
 * yang menggabungkan tabel transaksi aset dengan kode transaksi dan master aset dengan
 * filter tipe habis pakai, filter nama/kode opsional, dan filter tanggal. Hasil query
 * berupa array objek ({@code Object[]}) yang dirender oleh {@code JenisBarangRenderer}.
 * Renderer juga menampilkan transaksi terakhir per aset via query Hibernate criteria
 * tambahan, serta tombol aksi cetak per baris.
 *
 * <b>Threading:</b><br>
 * Kelas ini mendaftarkan timer ZKoss via {@code Common.createDefaultTimer} yang
 * memicu {@code onSearchDefault} secara periodik untuk menyegarkan data stok secara
 * otomatis. Timer berjalan dalam konteks ZK Desktop thread. Tidak ada threading manual.
 * Sesi Hibernate diakses via {@code HibernateUtil.currentSession()} dalam thread request.
 *
 * <b>Pemeliharaan:</b><br>
 * Query SQL native di {@code initCriteria} rentan terhadap SQL injection melalui
 * nilai {@code searchkode} dan {@code searchnama} yang dikoncatenasi langsung ke
 * string SQL. Pertimbangkan menggunakan named parameters Hibernate untuk keamanan
 * yang lebih baik. Total size paginasi di {@code onSearchDefault} di-hardcode ke
 * 50000 tanpa query COUNT aktual; pertimbangkan melakukan COUNT sebenarnya agar
 * paginasi akurat. Tombol cetak per baris saat ini selalu membuka laporan tanpa
 * menyaring berdasarkan aset/satuan kerja yang diklik; ini mungkin perlu diperbaiki.
 *
 * @author AIS Development Team
 * @version 1.0
 * @since ZKoss 5.5, Java 1.7
 */
public class MonitorStokAssetAction extends GenericAutowireComposer {

	/**
	 * Versi serial untuk serialisasi kelas ini.
	 */
	private static final long serialVersionUID = 3786091220301468178L;

	/** Grid utama yang menampilkan data stok aset habis pakai. */
	private MyGrid grid;

	/**
	 * Textbox filter pencarian berdasarkan nama aset.
	 * Nilai dikoncatenasi langsung ke query SQL native.
	 */
	private MyTextbox searchnama;

	/**
	 * Textbox filter pencarian berdasarkan kode aset.
	 * Nilai dikoncatenasi langsung ke query SQL native.
	 */
	private MyTextbox searchkode;

	/**
	 * Datebox filter untuk menentukan tanggal acuan perhitungan stok.
	 * Stok dihitung dari semua transaksi sampai dengan tanggal ini.
	 * Jika null, menggunakan tanggal hari ini.
	 */
	private MyDatebox searchperTanggal;

	/**
	 * Tombol cetak laporan stok keseluruhan ({@code LaporanStokItem}).
	 * Disembunyikan untuk pengguna dengan role mahasiswa atau dosen.
	 */
	private MyToolbarbuttonConfig cetak;

	/**
	 * Tombol cetak laporan pelacakan/tracking stok keseluruhan
	 * ({@code LaporanTrackingStokItem}).
	 * Disembunyikan untuk pengguna dengan role mahasiswa atau dosen.
	 */
	private MyToolbarbuttonConfig cetakTrack;

	/** Komponen paginasi untuk navigasi halaman daftar stok. */
	private Paging paging;

	/**
	 * <b>Tujuan:</b> Memeriksa keamanan akses sebelum komponen ZKoss dikomposis.<br>
	 * <br>
	 * <b>Cara kerja:</b> Memanggil {@code Common.doCheckSecurity()} untuk memvalidasi
	 * sesi dan hak akses pengguna. Jika tidak lolos, eksekusi dihentikan dan pengguna
	 * diarahkan ke halaman login sebelum UI dibangun. Kemudian meneruskan ke implementasi
	 * induk {@code super.doBeforeCompose}.<br>
	 * <br>
	 * <b>Parameter:</b>
	 * <ul>
	 *   <li>{@code page} — halaman ZK aktif tempat komponen dikomposis</li>
	 *   <li>{@code parent} — komponen induk dalam hierarki ZK</li>
	 *   <li>{@code compInfo} — metadata komponen dari berkas ZUL</li>
	 * </ul>
	 * <b>Return:</b> {@code ComponentInfo} dari implementasi induk.<br>
	 * <br>
	 * <b>Penanganan error:</b> {@code Common.doCheckSecurity()} menangani
	 * pengalihan dan exception akses tidak sah.<br>
	 * <br>
	 * <b>Pemeliharaan:</b> Wajib ada sebagai garis pertahanan pertama.
	 *
	 * @param page     halaman ZK aktif
	 * @param parent   komponen induk dalam pohon komponen ZK
	 * @param compInfo metadata komponen dari berkas ZUL
	 * @return {@code ComponentInfo} dari implementasi induk
	 */
	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	/**
	 * <b>Tujuan:</b> Menginisialisasi komponen UI dan pengaturan awal halaman
	 * monitor stok aset, termasuk mendaftarkan paginasi, mengatur visibilitas
	 * tombol cetak, dan memulai timer refresh otomatis.<br>
	 * <br>
	 * <b>Cara kerja:</b>
	 * <ol>
	 *   <li>Memanggil {@code super.doAfterCompose(comp)} untuk autowiring ZKoss.</li>
	 *   <li>Mendaftarkan listener paginasi yang memanggil {@code onSearchDefault}
	 *       setiap kali pengguna berpindah halaman.</li>
	 *   <li>Mengambil pengguna saat ini via {@code Common.getCurrentUser()} dan
	 *       menyembunyikan tombol cetak {@code cetak} dan {@code cetakTrack} jika
	 *       pengguna adalah mahasiswa (memiliki relasi {@code getMahasiswa() != null})
	 *       atau dosen (memiliki relasi {@code ambilDosen() != null}).</li>
	 *   <li>Mendaftarkan timer default via {@code Common.createDefaultTimer} yang
	 *       memicu {@code onSearchDefault} secara periodik untuk auto-refresh data stok.</li>
	 * </ol>
	 * <b>Parameter:</b>
	 * <ul>
	 *   <li>{@code comp} — komponen root hasil komposisi ZKoss</li>
	 * </ul>
	 * <b>Return:</b> void.<br>
	 * <br>
	 * <b>Penanganan error:</b> Exception dilempar jika inisialisasi komponen gagal.<br>
	 * <br>
	 * <b>Pemeliharaan:</b> Catatan: {@code doAfterCompose} tidak memanggil
	 * {@code onSearchDefault} secara eksplisit; pengisian grid pertama dilakukan
	 * oleh timer yang langsung aktif setelah registrasi. Jika timer tidak dikehendaki,
	 * tambahkan pemanggilan eksplisit {@code onSearchDefault(null)} sebelum registrasi timer.
	 *
	 * @param comp komponen root hasil komposisi ZKoss
	 * @throws Exception jika terjadi kesalahan inisialisasi
	 */
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser != null && (tbmuser.getMahasiswa() != null || tbmuser.ambilDosen() != null)) {
			cetak.setVisible(false);
			cetakTrack.setVisible(false);
		}

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});
	}

	/**
	 * <b>Tujuan:</b> Menangani event klik tombol "Cetak" pada toolbar utama,
	 * membuka laporan stok keseluruhan dalam window modal.<br>
	 * <br>
	 * <b>Cara kerja:</b> Membuat instans baru {@code LaporanStokItem} (komponen laporan
	 * berbasis Jasper Report atau template cetak), menambahkannya ke root halaman ZKoss,
	 * mengatur dimensi window laporan (95% tinggi, 90% lebar), mengizinkan penutupan
	 * oleh pengguna, dan menampilkannya sebagai dialog modal menggunakan {@code onModal()}.<br>
	 * <br>
	 * <b>Parameter:</b>
	 * <ul>
	 *   <li>{@code event} — objek event ZKoss dari klik tombol cetak</li>
	 * </ul>
	 * <b>Return:</b> void.<br>
	 * <br>
	 * <b>Penanganan error:</b> Exception dilempar jika inisialisasi komponen laporan
	 * gagal atau tidak ada hak akses laporan.<br>
	 * <br>
	 * <b>Pemeliharaan:</b> Terikat ke event {@code onCetak} di ZUL. Laporan dibuka tanpa
	 * filter aset atau satuan kerja tertentu; laporan menampilkan semua stok.
	 *
	 * @param event objek event dari klik tombol cetak stok
	 * @throws Exception jika inisialisasi laporan gagal
	 */
	public void onCetak(Event event) throws Exception {
		LaporanStokItem laporan = new LaporanStokItem(null, null);
		laporan.setTitle("Cetak Laporan");
		page.getFirstRoot().appendChild(laporan);
		laporan.setHeight("95%");
		laporan.setWidth("90%");
		laporan.setClosable(true);
		laporan.onModal();
	}

	/**
	 * <b>Tujuan:</b> Menangani event klik tombol "Cetak Track" pada toolbar utama,
	 * membuka laporan pelacakan (tracking) stok dalam window modal.<br>
	 * <br>
	 * <b>Cara kerja:</b> Membuat instans baru {@code LaporanTrackingStokItem}, menambahkannya
	 * ke root halaman ZKoss, mengatur dimensi dan izin tutup, lalu menampilkannya sebagai
	 * dialog modal. Laporan tracking menampilkan riwayat pergerakan stok (masuk dan keluar)
	 * per aset, bukan hanya saldo akhir.<br>
	 * <br>
	 * <b>Parameter:</b>
	 * <ul>
	 *   <li>{@code event} — objek event ZKoss dari klik tombol cetak tracking</li>
	 * </ul>
	 * <b>Return:</b> void.<br>
	 * <br>
	 * <b>Penanganan error:</b> Exception dilempar jika inisialisasi laporan gagal.<br>
	 * <br>
	 * <b>Pemeliharaan:</b> Terikat ke event {@code onCetakTrack} di ZUL.
	 *
	 * @param event objek event dari klik tombol cetak tracking stok
	 * @throws Exception jika inisialisasi laporan gagal
	 */
	public void onCetakTrack(Event event) throws Exception {
		LaporanTrackingStokItem laporan = new LaporanTrackingStokItem(null, null);
		laporan.setTitle("Cetak Laporan");
		page.getFirstRoot().appendChild(laporan);
		laporan.setHeight("95%");
		laporan.setWidth("90%");
		laporan.setClosable(true);
		laporan.onModal();
	}

	/**
	 * <h3>JenisBarangRenderer — Renderer Baris Grid Monitor Stok Aset</h3>
	 *
	 * <b>Untuk apa:</b><br>
	 * Kelas batin ini merender setiap baris data stok aset ke dalam komponen Row ZKoss
	 * pada grid monitor. Setiap baris mewakili satu kombinasi aset ({@code MasterAsset})
	 * dan satuan kerja ({@code SatuanKerja}), menampilkan informasi detail stok termasuk
	 * panel aksi {@code DetailTransaksiAssetHelper}, kode aset, nama aset (dengan revisi),
	 * satuan kerja, jumlah stok neto, informasi transaksi terakhir, serta tombol cetak
	 * stok dan tracking per baris.<br>
	 * <br>
	 * <b>Cara kerja:</b> Menerima {@code Object[]} dari hasil query SQL native dengan
	 * indeks: [0]=master_asset ID, [1]=nama_item, [2]=tanggal_terakhir_pengadaan,
	 * [3]=stok (hasil SUM), [4]=satuan_kerja ID. Memuat entitas {@code MasterAsset}
	 * dan {@code SatuanKerja} dari cache memori ({@code ConstantValues.ambil}).
	 * Transaksi terakhir diambil via query Hibernate criteria tambahan per baris
	 * (N+1 query pattern; pertimbangkan optimasi JOIN fetch jika performa bermasalah).<br>
	 * <br>
	 * <b>Threading:</b> Single-thread ZKoss Desktop per event. Query database dilakukan
	 * dalam thread rendering ZKoss.<br>
	 * <br>
	 * <b>Pemeliharaan:</b> Tombol cetak per baris saat ini membuka laporan tanpa
	 * parameter filter spesifik aset atau satuan kerja. Jika diinginkan laporan
	 * per-baris yang tersaring, tambahkan parameter ke konstruktor
	 * {@code LaporanStokItem} dan {@code LaporanTrackingStokItem}.
	 */
	class JenisBarangRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * <b>Tujuan:</b> Mengisi komponen Row ZKoss dengan data satu record stok aset
		 * dari hasil query SQL native untuk ditampilkan dalam grid monitor.<br>
		 * <br>
		 * <b>Cara kerja:</b>
		 * <ol>
		 *   <li>Menetapkan vertical alignment baris ke "top".</li>
		 *   <li>Mengekstrak data dari array {@code Object[]}: ID aset (index 0),
		 *       jumlah stok (index 3), dan ID satuan kerja (index 4).</li>
		 *   <li>Memuat entitas {@code SatuanKerja} dari cache via {@code ConstantValues.ambil};
		 *       null jika ID satuan kerja null.</li>
		 *   <li>Memuat entitas {@code MasterAsset} dari cache via {@code ConstantValues.ambil}.</li>
		 *   <li>Membuat dan menambahkan panel aksi {@code DetailTransaksiAssetHelper}
		 *       yang memungkinkan tambah transaksi stok dari grid ini.</li>
		 *   <li>Menambahkan Label kode aset, komponen riwayat revisi nama aset via
		 *       {@code RevisiHelper.createNewRevisi}, dan Label nama satuan kerja
		 *       (kosong jika null).</li>
		 *   <li>Menambahkan Label jumlah stok diformat dengan {@code Common.numberFormat}
		 *       (kosong jika null).</li>
		 *   <li>Mengambil transaksi terakhir per aset via query Hibernate criteria
		 *       (ORDER BY tanggalDanWaktu DESC, LIMIT 1) dan menampilkannya sebagai
		 *       Label dengan format: tanggal + kode transaksi + nama transaksi + info detail.</li>
		 *   <li>Membangun kolom aksi kebab popup (⋯) via {@code UIHelper.buatBarisAksi}
		 *       berisi tombol "Stok" untuk cetak {@code LaporanStokItem} dan tombol "Track"
		 *       untuk cetak {@code LaporanTrackingStokItem}.</li>
		 * </ol>
		 * <b>Parameter:</b>
		 * <ul>
		 *   <li>{@code arg0} — Row ZKoss yang akan diisi</li>
		 *   <li>{@code arg1} — objek data; harus dapat di-cast ke {@code Object[]}
		 *       berisi hasil query SQL native stok aset</li>
		 * </ul>
		 * <b>Return:</b> void.<br>
		 * <br>
		 * <b>Penanganan error:</b> Exception dipropagasi ke engine renderer ZKoss.
		 * Null handling untuk stok dan satuan kerja dilakukan secara eksplisit di kode.<br>
		 * <br>
		 * <b>Pemeliharaan:</b> Indeks array hasil query ([0],[3],[4]) harus sesuai
		 * dengan urutan SELECT di {@code initCriteria}. Jika kolom query berubah,
		 * perbarui indeks di sini. Query transaksi terakhir per baris dapat menjadi
		 * bottleneck performa jika ada banyak baris; pertimbangkan batch loading.
		 *
		 * @param arg0 baris Row ZKoss target
		 * @param arg1 array {@code Object[]} hasil query SQL native stok aset
		 * @throws Exception jika rendering gagal
		 */
		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			Object[] objects = (Object[]) arg1;
			// Guard baris query: kolom bisa null (mis. satuan_kerja null untuk aset global/
			// belum ditugaskan ke satuan kerja) atau item id kosong → hindari NPE saat cast
			// Number -> longValue(). Bila id item tak ada, sembunyikan baris.
			if (objects == null || objects.length < 5 || objects[0] == null) {
				arg0.setVisible(false);
				return;
			}
			Long itemId = ((Number) objects[0]).longValue();
			Number stok = (Number) objects[3];

			Long satuan_kerja = objects[4] == null ? null : ((Number) objects[4]).longValue();

			final SatuanKerja satuanKerja = (SatuanKerja) (satuan_kerja == null ? null
					: ConstantValues.ambil(SatuanKerja.class.getName(), satuan_kerja));

			Session session = HibernateUtil.currentSession();
			final MasterAsset masterAsset = (MasterAsset) ConstantValues.ambil(MasterAsset.class.getName(), itemId);
			if (masterAsset == null) {
				// Master aset tidak ditemukan (mungkin sudah dihapus) → sembunyikan baris
				// agar tidak NPE saat mengakses kode/nama di bawah.
				arg0.setVisible(false);
				return;
			}

			final DetailTransaksiAssetHelper detail = new DetailTransaksiAssetHelper(masterAsset, satuanKerja);
			detail.setParent(arg0);

			new Label(masterAsset.getKode()).setParent(arg0);

			RevisiHelper.createNewRevisi(MasterAsset.class, masterAsset, masterAsset.getNama()).setParent(arg0);

			new Label(satuanKerja == null ? "" : satuanKerja.getNama()).setParent(arg0);

			new Label(stok == null ? "" : Common.numberFormat.get().format(stok)).setParent(arg0);

			DetailTransaksiAsset detailTransaksi = (DetailTransaksiAsset) session
					.createCriteria(DetailTransaksiAsset.class).add(Restrictions.eq("masterAsset", masterAsset))
					.addOrder(Order.desc("tanggalDanWaktu")).setMaxResults(1).uniqueResult();
			new Label(detailTransaksi == null ? ""
					: (Common.dateFormat3.get().format(detailTransaksi.getTanggalDanWaktu()) + " "
							+ (detailTransaksi.getKodeTransaksi() == null ? ""
									: (detailTransaksi.getKodeTransaksi().getKode() + " - "
											+ detailTransaksi.getKodeTransaksi().getNama()))
							+ " - " + DetailTransaksiAssetHelper.dapatkanInfo(detailTransaksi)))
					.setParent(arg0);

			// Kolom aksi rapi (pola MahasiswaAction): semua tombol dibungkus kebab popup (⋯)
			// via UIHelper.buatBarisAksi — kolom aksi jadi kecil dan konsisten antar layar.
			final java.util.List<org.zkoss.zk.ui.Component> aksiButtons =
					new java.util.ArrayList<org.zkoss.zk.ui.Component>();

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Stok", "/img/print.png");
			button.setOrient("vertical");
			button.setTooltiptext("Cetak Stok");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					LaporanStokItem laporan = new LaporanStokItem(null, null);
					laporan.setTitle("Cetak Laporan");
					page.getFirstRoot().appendChild(laporan);
					laporan.setHeight("95%");
					laporan.setWidth("90%");
					laporan.setClosable(true);
					laporan.onModal();

				}

			});
			aksiButtons.add(button);

			button = new MyToolbarbuttonConfig("Track", "/img/print.png");
			button.setOrient("vertical");
			button.setTooltiptext("Cetak Stok");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					LaporanTrackingStokItem laporan = new LaporanTrackingStokItem(null, null);
					laporan.setTitle("Cetak Laporan");
					page.getFirstRoot().appendChild(laporan);
					laporan.setHeight("95%");
					laporan.setWidth("90%");
					laporan.setClosable(true);
					laporan.onModal();

				}

			});
			aksiButtons.add(button);

			ais.ui.util.UIHelper.buatBarisAksi(arg0, 3, aksiButtons);

		}

	}

	/**
	 * <b>Tujuan:</b> Membangun query SQL native Hibernate untuk mengambil data stok
	 * aset habis pakai yang diagregasi per aset dan satuan kerja.<br>
	 * <br>
	 * <b>Cara kerja:</b>
	 * <ol>
	 *   <li>Mengambil sesi Hibernate saat ini.</li>
	 *   <li>Membangun string SQL dengan SELECT: ID master aset, nama item (MAX),
	 *       tanggal pengadaan terakhir (MAX), total stok neto (SUM (qty+qtybonus)*jenis),
	 *       dan ID satuan kerja.</li>
	 *   <li>FROM {@code asset.detail_transaksi_asset} dengan INNER JOIN ke
	 *       {@code library.kode_transaksi} (untuk mendapatkan faktor 'jenis' yang
	 *       menentukan apakah transaksi menambah atau mengurangi stok) dan INNER JOIN
	 *       ke {@code asset.master_asset} (dengan filter tipe habis pakai).</li>
	 *   <li>Filter kondisional dari input UI: jika {@code searchkode} tidak kosong,
	 *       ditambahkan klausa {@code c.kode ilike '%...%'}; jika {@code searchnama}
	 *       tidak kosong, ditambahkan klausa {@code c.nama ilike '%...%'}.</li>
	 *   <li>Filter tanggal: hanya transaksi dengan tanggal ≤ tanggal acuan
	 *       ({@code searchperTanggal}, default hari ini).</li>
	 *   <li>GROUP BY master_asset dan satuan_kerja untuk agregasi per kombinasi unik.</li>
	 *   <li>ORDER BY stok ASC jika {@code order} true (menampilkan stok terendah di atas).</li>
	 *   <li>LIMIT dan OFFSET untuk paginasi.</li>
	 * </ol>
	 * <b>Parameter:</b>
	 * <ul>
	 *   <li>{@code order} — {@code true} untuk menambahkan ORDER BY stok ASC</li>
	 *   <li>{@code count} — jumlah maksimal baris yang dikembalikan (LIMIT)</li>
	 *   <li>{@code start} — offset baris awal (OFFSET) untuk paginasi</li>
	 * </ul>
	 * <b>Return:</b> Objek {@code SQLQuery} Hibernate yang siap dieksekusi dengan
	 * {@code .list()} yang menghasilkan {@code List<Object[]>}.<br>
	 * <br>
	 * <b>Penanganan error:</b> Exception Hibernate dipropagasi ke pemanggil. Tidak
	 * ada penanganan khusus untuk nilai null pada {@code searchperTanggal}
	 * (ditangani dengan fallback ke {@code WaktuUtil.getDate()}).<br>
	 * <br>
	 * <b>Pemeliharaan:</b> PERINGATAN KEAMANAN: nilai {@code searchkode} dan
	 * {@code searchnama} dikoncatenasi langsung ke SQL tanpa parameterisasi,
	 * berpotensi SQL injection jika input tidak dibersihkan. Gunakan named parameters
	 * Hibernate atau escaping yang tepat untuk produksi yang aman. Total stok dihitung
	 * sebagai SUM((qty+qtybonus)*jenis) di mana 'jenis' dari kode_transaksi bernilai
	 * positif untuk masuk dan negatif untuk keluar.
	 *
	 * @param order {@code true} untuk menyertakan ORDER BY stok ASC
	 * @param count jumlah baris maksimal yang diambil (nilai LIMIT SQL)
	 * @param start offset baris awal untuk paginasi (nilai OFFSET SQL)
	 * @return {@code SQLQuery} Hibernate untuk data stok aset habis pakai
	 */
	public SQLQuery initCriteria(boolean order, int count, int start) {
		Session session = HibernateUtil.currentSession();

		String sql = "select a.master_asset as item, max(c.nama) as nama_item, max(a.tanggal) as tanggal_terakhir_pengadaan, "
				+ "sum((a.qty+a.qtybonus)*b.jenis) as stok, a.satuan_kerja  from asset.detail_transaksi_asset a "
				+ "inner join library.kode_transaksi b on (a.kode_transaksi = b.id) "

				+ "inner join asset.master_asset c on (a.master_asset = c.id and c.tipe='"
				+ MasterAsset.TIPE_HABIS_PAKAI + "') "
				+ (searchkode.getValue().trim().equals("") ? ""
						: " and c.kode ilike '%" + searchkode.getValue().trim() + "%' ")
				+ " "
				+ (searchnama.getValue().trim().equals("") ? ""
						: " and c.nama ilike '%" + searchnama.getValue().trim() + "%' ")
				+ " and date(a.tanggal) <= date('"
				+ (Common.databaseDateFormat.get()
						.format(searchperTanggal.getValue() == null ? ais.ui.util.WaktuUtil.getDate()
								: searchperTanggal.getValue()))
				+ "') group by a.master_asset, a.satuan_kerja  ";

		if (order)
			sql += " order by stok asc ";

		sql += " LIMIT " + count + " OFFSET " + start;

		return session.createSQLQuery(sql);
	}

	/**
	 * <b>Tujuan:</b> Menjalankan query stok dan memperbarui tampilan grid monitor
	 * berdasarkan filter yang aktif, dengan paginasi yang dikonfigurasi secara khusus.<br>
	 * <br>
	 * <b>Cara kerja:</b>
	 * <ol>
	 *   <li>Menetapkan total size paginasi ke 50000 (nilai hardcode sebagai estimasi
	 *       maksimum; tidak melakukan COUNT query aktual).</li>
	 *   <li>Mengonfigurasi komponen paginasi: increment per halaman (5 untuk mobile,
	 *       10 untuk desktop), mode "os" (tanpa tombol first/last), nonaktifkan
	 *       tampilan detail, dan sembunyikan jika data di bawah satu halaman.</li>
	 *   <li>Menjalankan query stok via {@code initCriteria(true, ROWS_COUNT_ON_PAGE,
	 *       halaman * ROWS_COUNT_ON_PAGE)} untuk mendapatkan data halaman aktif.</li>
	 *   <li>Membungkus hasil dalam {@code SimpleListModel}.</li>
	 *   <li>Menetapkan renderer {@code JenisBarangRenderer} dan memperbarui model grid.</li>
	 * </ol>
	 * <b>Parameter:</b>
	 * <ul>
	 *   <li>{@code event} — event pemicu; boleh null untuk pemanggilan programatik
	 *       (dari timer atau inisialisasi awal)</li>
	 * </ul>
	 * <b>Return:</b> void.<br>
	 * <br>
	 * <b>Penanganan error:</b> Exception dari query SQL atau update model dipropagasi.<br>
	 * <br>
	 * <b>Pemeliharaan:</b> Total size 50000 bersifat hardcode; jika data stok bisa
	 * melebihi angka ini, lakukan query COUNT sebenarnya untuk akurasi paginasi.
	 * Metode ini juga dipanggil oleh timer auto-refresh dan listener paginasi.
	 *
	 * @param event event pemicu pencarian; boleh null
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		int size = 50000;
		paging.setPageIncrement(Common.isMobile() ? 5 : 10);
		paging.setMold("os");
		paging.setDetailed(false);
		paging.setTotalSize(size);
		paging.setVisible(size > Common.ROWS_COUNT_ON_PAGE);
		List<Object[]> item = initCriteria(true, Common.ROWS_COUNT_ON_PAGE,
				Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(item);
		grid.setRowRenderer(new JenisBarangRenderer());
		grid.setModelCheckMobile(strset);

	}

}
