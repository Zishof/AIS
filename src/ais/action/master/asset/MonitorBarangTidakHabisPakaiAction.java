package ais.action.master.asset;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
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

import ais.action.master.asset.helper.PenerimaanPengadaanMasterAssetDetailAction;
import ais.action.master.helper.RevisiHelper;
import ais.action.report.format1.library.LaporanStokItem;
import ais.action.report.format1.library.LaporanTrackingStokItem;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.asset.MasterAsset;
import ais.database.model.asset.PenerimaanPengadaanMasterAssetDetail;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * <h3>MonitorBarangTidakHabisPakaiAction</h3>
 *
 * <b>Untuk apa:</b><br>
 * Kelas ini merupakan kontroler ZK (ZKoss GenericAutowireComposer) yang mengelola
 * halaman pemantauan inventaris <em>barang tidak habis pakai</em> (durable goods / aset tetap)
 * dalam sistem informasi aset kampus. Barang tidak habis pakai adalah aset yang secara
 * fisik tidak berkurang saat digunakan, misalnya meja, kursi, komputer, kendaraan dinas,
 * dan peralatan laboratorium. Halaman ini merupakan pintu masuk utama bagi operator logistik
 * atau admin aset untuk memantau seluruh penerimaan pengadaan barang tidak habis pakai
 * yang telah disetujui, melihat detail inventaris per item, serta mencetak laporan stok
 * maupun laporan pelacakan (tracking) stok barang.
 *
 * <b>Cara kerja:</b><br>
 * Setelah halaman ZUL dimuat oleh ZK, {@code doBeforeCompose} dipanggil terlebih dahulu
 * untuk memeriksa hak akses pengguna melalui {@code Common.doCheckSecurity()}. Jika
 * pengguna tidak memiliki hak yang cukup, proses akan dihentikan sebelum komponen
 * diinisialisasi. Selanjutnya {@code doAfterCompose} menyelesaikan inisialisasi:
 * <ol>
 *   <li>Komponen paging dikonfigurasi dengan pendengar (listener) yang memanggil
 *       {@code onSearchDefault} setiap kali pengguna berpindah halaman.</li>
 *   <li>Hak edit diperiksa menggunakan {@code CommonPrivilages.checkPrevilages(UPDATE)};
 *       hasilnya disimpan di field {@code edit} dan diteruskan ke renderer baris.</li>
 *   <li>Jika pengguna yang sedang login adalah mahasiswa atau dosen, tombol cetak
 *       disembunyikan agar laporan hanya bisa diakses oleh staf administrasi.</li>
 *   <li>Timer default dibuat sehingga data di-refresh secara otomatis pada interval
 *       tertentu tanpa pengguna harus menekan tombol cari.</li>
 * </ol>
 * Data ditarik dari tabel {@code PenerimaanPengadaanMasterAssetDetail} dengan filter
 * utama: (a) penerimaan pengadaan sudah disetujui ({@code disetujuiOleh} tidak null),
 * dan (b) tipe master aset adalah {@code TIPE_TIDAK_HABIS_PAKAI}. Pencarian teks
 * mendukung pencarian multi-kolom (kode penerimaan, kode aset, nama aset) dengan
 * pencocokan substring ({@code MatchMode.ANYWHERE}).
 *
 * <b>Threading:</b><br>
 * Kelas ini mengikuti model eksekusi event-driven ZK di mana semua event diproses
 * pada thread ZK Desktop. Listener timer dan paging dieksekusi di thread ZK yang
 * sama. Tidak ada akses concurrent langsung ke field instance dari thread lain.
 * Sesi Hibernate diperoleh dari {@code HibernateUtil.currentSession()} yang mengikat
 * sesi ke thread saat ini (thread-local). Instance kelas ini tidak aman digunakan
 * secara bersamaan dari beberapa thread.
 *
 * <b>Pemeliharaan:</b><br>
 * Untuk menambah kolom baru pada grid, modifikasi method {@code render} di kelas
 * inner {@code JenisBarangRenderer} serta kolom-kolom yang didefinisikan di file ZUL
 * yang berpasangan. Untuk menambah kriteria pencarian, perbarui method
 * {@code initCriteria}. Saat skema database berubah (penambahan kolom di
 * {@code penerimaan_pengadaan_master_asset_detail}), pastikan mapping Hibernate
 * dan entitas {@code PenerimaanPengadaanMasterAssetDetail} juga diperbarui. Jika
 * jenis laporan baru perlu ditambahkan, ikuti pola {@code onCetak}/{@code onCetakTrack}
 * dengan membuat kelas laporan baru yang mewarisi pola yang sama.
 *
 * @author Tim Pengembang AIS
 * @version 1.0
 * @since ZKoss 5.5, Java 1.7
 */
public class MonitorBarangTidakHabisPakaiAction extends GenericAutowireComposer {

	/**
	 * UID serialisasi untuk kompatibilitas versi kelas saat diserialisasi.
	 * Nilai ini tidak boleh diubah kecuali struktur kelas berubah secara
	 * tidak kompatibel dengan versi yang sudah ter-deploy.
	 */
	private static final long serialVersionUID = 3786091220301468178L;

	/**
	 * Komponen grid ZK yang menampilkan daftar baris barang tidak habis pakai.
	 * Komponen ini di-autowire oleh ZK berdasarkan atribut {@code id} di file ZUL.
	 * Grid dikonfigurasi menggunakan renderer kelas inner {@code JenisBarangRenderer}.
	 */
	private MyGrid grid;

	/**
	 * Kotak teks untuk memasukkan kata kunci pencarian. Pengguna dapat mengetikkan
	 * kode penerimaan pengadaan, kode aset, atau nama aset. Pencarian bersifat
	 * case-insensitive dan mendukung pencocokan substring di semua kolom tersebut.
	 */
	private MyTextbox searchkode;

	/**
	 * Tombol toolbar untuk membuka laporan stok barang tidak habis pakai dalam
	 * format cetakan (LaporanStokItem). Tombol ini disembunyikan untuk pengguna
	 * dengan peran mahasiswa atau dosen.
	 */
	private MyToolbarbuttonConfig cetak;

	/**
	 * Tombol toolbar untuk membuka laporan pelacakan (tracking) stok barang tidak
	 * habis pakai (LaporanTrackingStokItem). Tombol ini juga disembunyikan untuk
	 * pengguna mahasiswa atau dosen karena laporan tracking hanya relevan bagi
	 * staf logistik dan admin aset.
	 */
	private MyToolbarbuttonConfig cetakTrack;

	/**
	 * Komponen paging ZK yang mengelola navigasi halaman pada grid. Jumlah baris
	 * per halaman dikonfigurasi dari {@code Common.ROWS_COUNT_ON_PAGE} dan
	 * disesuaikan untuk tampilan mobile (5 baris) atau desktop (10 baris).
	 */
	private Paging paging;

	/**
	 * Flag yang menandakan apakah pengguna saat ini memiliki hak untuk mengubah
	 * (edit/update) data. Nilai ini diambil dari {@code CommonPrivilages.checkPrevilages}
	 * pada saat inisialisasi dan diteruskan ke renderer baris untuk menentukan
	 * apakah tombol aksi inventaris ditampilkan dalam mode aktif atau read-only.
	 */
	private boolean edit = false;

	/**
	 * <b>Tujuan:</b><br>
	 * Metode siklus hidup ZK yang dipanggil sebelum komponen ZUL dikompilasi dan
	 * diinisialisasi. Metode ini berfungsi sebagai titik pemeriksaan keamanan awal
	 * untuk memastikan bahwa hanya pengguna yang berwenang yang dapat mengakses
	 * halaman monitor barang tidak habis pakai.
	 *
	 * <b>Cara kerja:</b><br>
	 * Memanggil {@code Common.doCheckSecurity()} yang akan memeriksa sesi pengguna
	 * aktif dan memvalidasi apakah pengguna memiliki hak akses ke halaman ini.
	 * Jika pemeriksaan gagal, framework akan mengalihkan pengguna ke halaman login
	 * atau menampilkan pesan tidak berwenang. Setelah pemeriksaan berhasil,
	 * metode induk {@code super.doBeforeCompose} dipanggil untuk melanjutkan proses
	 * perakitan komponen ZK secara normal.
	 *
	 * <b>Parameter:</b><br>
	 * @param page     halaman ZK yang sedang dimuat; digunakan oleh framework untuk
	 *                 konteks perakitan komponen
	 * @param parent   komponen induk tempat kontroler ini dipasang; bisa null jika
	 *                 kontroler langsung terikat ke root halaman
	 * @param compInfo metadata komponen ZK yang berisi informasi definisi ZUL
	 *
	 * <b>Return:</b><br>
	 * @return objek {@code ComponentInfo} dari pemanggilan {@code super.doBeforeCompose},
	 *         yang digunakan oleh framework ZK untuk melanjutkan proses inisialisasi
	 *
	 * <b>Penanganan error:</b><br>
	 * Jika {@code Common.doCheckSecurity()} melempar exception karena pengguna tidak
	 * berwenang, proses inisialisasi halaman akan berhenti sebelum komponen terbentuk.
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Jangan menambahkan logika bisnis di sini; gunakan hanya untuk pemeriksaan
	 * keamanan atau konfigurasi awal yang harus terjadi sebelum komponen dibentuk.
	 */
	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	/**
	 * <b>Tujuan:</b><br>
	 * Metode siklus hidup ZK yang dipanggil setelah seluruh komponen ZUL selesai
	 * dirakit dan di-wire ke field kontroler. Metode ini bertanggung jawab untuk
	 * inisialisasi lengkap halaman monitor barang tidak habis pakai, termasuk
	 * konfigurasi paging, pemeriksaan hak akses pengguna, visibilitas tombol cetak,
	 * serta pemasangan timer otomatis untuk refresh data berkala.
	 *
	 * <b>Cara kerja:</b><br>
	 * Proses inisialisasi dilakukan secara berurutan:
	 * <ol>
	 *   <li>{@code super.doAfterCompose(comp)} dipanggil agar framework ZK menyelesaikan
	 *       proses autowiring komponen.</li>
	 *   <li>Paging diinisialisasi melalui {@code Common.initPaging} dengan listener yang
	 *       memanggil {@code onSearchDefault} setiap kali pengguna berganti halaman,
	 *       sehingga grid selalu menampilkan data sesuai halaman aktif.</li>
	 *   <li>Hak edit diperiksa: jika pengguna memiliki privilege UPDATE, field
	 *       {@code edit} bernilai true sehingga tombol aksi inventaris pada setiap
	 *       baris grid akan ditampilkan dalam mode aktif.</li>
	 *   <li>Pengguna bertipe mahasiswa atau dosen tidak diizinkan mencetak laporan,
	 *       sehingga tombol {@code cetak} dan {@code cetakTrack} disembunyikan.</li>
	 *   <li>Timer default dibuat untuk secara otomatis memperbarui data grid tanpa
	 *       interaksi manual dari pengguna, berguna di lingkungan monitor real-time.</li>
	 * </ol>
	 *
	 * <b>Parameter:</b><br>
	 * @param comp komponen root ZK yang menjadi kontainer dari seluruh komponen
	 *             yang didefinisikan di file ZUL; digunakan oleh metode induk
	 *             untuk menyelesaikan proses autowiring
	 *
	 * <b>Return:</b><br>
	 * Metode ini tidak mengembalikan nilai (void).
	 *
	 * <b>Penanganan error:</b><br>
	 * @throws Exception jika {@code super.doAfterCompose} gagal, misalnya karena
	 *                   komponen yang di-wire tidak ditemukan di halaman ZUL.
	 *                   Exception akan dipropagasi ke framework ZK dan halaman
	 *                   akan gagal dimuat dengan pesan error.
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Jika ada komponen baru yang perlu diinisialisasi saat halaman dimuat, tambahkan
	 * logikanya di sini setelah pemanggilan {@code super.doAfterCompose}. Pastikan
	 * semua field yang diinisialisasi memiliki pasangan komponen di file ZUL dengan
	 * atribut {@code id} yang sesuai.
	 */
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);

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
	 * <b>Tujuan:</b><br>
	 * Menangani event klik tombol "Cetak" pada toolbar halaman untuk membuka
	 * jendela cetak laporan stok barang tidak habis pakai menggunakan kelas
	 * {@code LaporanStokItem}. Laporan ini memberikan gambaran menyeluruh tentang
	 * stok aset yang telah diterima dan disetujui, dalam format yang siap cetak.
	 *
	 * <b>Cara kerja:</b><br>
	 * Membuat instance baru dari {@code LaporanStokItem} dengan parameter null
	 * (menggunakan konfigurasi default laporan). Instance tersebut kemudian
	 * ditambahkan sebagai anak dari komponen root halaman pertama ({@code page.getFirstRoot()}),
	 * sehingga muncul sebagai jendela modal di atas konten halaman. Properti modal
	 * diatur: judul "Cetak Laporan", tinggi 95%, lebar 90%, dapat ditutup, dan
	 * ditampilkan dalam mode modal sehingga pengguna harus menutup jendela ini
	 * sebelum kembali ke halaman utama.
	 *
	 * <b>Parameter:</b><br>
	 * @param event objek event ZK yang dipicu oleh klik tombol cetak; tidak digunakan
	 *              secara langsung dalam metode ini tetapi wajib ada sebagai parameter
	 *              sesuai konvensi event handler ZK
	 *
	 * <b>Return:</b><br>
	 * Metode ini tidak mengembalikan nilai (void).
	 *
	 * <b>Penanganan error:</b><br>
	 * @throws Exception jika konstruksi atau penampilan jendela laporan gagal,
	 *                   misalnya karena file template laporan tidak ditemukan
	 *                   atau konfigurasi JasperReports tidak valid
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Untuk mengubah ukuran atau judul jendela laporan, modifikasi parameter
	 * setHeight/setWidth/setTitle. Jika perlu meneruskan filter pencarian ke laporan,
	 * modifikasi konstruktor {@code LaporanStokItem} atau tambahkan setter setelah
	 * instansiasi.
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
	 * <b>Tujuan:</b><br>
	 * Menangani event klik tombol "Cetak Track" pada toolbar halaman untuk membuka
	 * jendela cetak laporan pelacakan (tracking) stok barang tidak habis pakai
	 * menggunakan kelas {@code LaporanTrackingStokItem}. Laporan tracking memberikan
	 * riwayat pergerakan aset dari pengadaan hingga distribusi, berguna untuk audit
	 * dan rekonsiliasi inventaris.
	 *
	 * <b>Cara kerja:</b><br>
	 * Membuat instance baru dari {@code LaporanTrackingStokItem} dengan parameter
	 * null. Instance tersebut ditambahkan sebagai anak dari komponen root halaman
	 * pertama dan ditampilkan sebagai jendela modal dengan konfigurasi yang identik
	 * dengan {@code onCetak}: tinggi 95%, lebar 90%, dapat ditutup. Perbedaannya
	 * hanya pada kelas laporan yang digunakan, yaitu {@code LaporanTrackingStokItem}
	 * yang menghasilkan tampilan data historis pergerakan stok, bukan snapshot stok
	 * saat ini.
	 *
	 * <b>Parameter:</b><br>
	 * @param event objek event ZK yang dipicu oleh klik tombol cetak tracking;
	 *              tidak digunakan secara langsung tetapi wajib ada sesuai konvensi
	 *              event handler ZK
	 *
	 * <b>Return:</b><br>
	 * Metode ini tidak mengembalikan nilai (void).
	 *
	 * <b>Penanganan error:</b><br>
	 * @throws Exception jika konstruksi atau penampilan jendela laporan tracking
	 *                   gagal, misalnya karena sumber data tracking kosong atau
	 *                   template JasperReports tidak tersedia
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Jika laporan tracking perlu difilter berdasarkan rentang tanggal atau satuan
	 * kerja tertentu, tambahkan dialog parameter sebelum membuka jendela laporan,
	 * atau teruskan parameter ke konstruktor {@code LaporanTrackingStokItem}.
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
	 * <b>Tujuan:</b><br>
	 * Kelas inner yang bertindak sebagai renderer baris untuk grid monitor barang
	 * tidak habis pakai. Setiap instance {@code JenisBarangRenderer} bertanggung jawab
	 * mengisi satu baris ZK {@code Row} dengan data dari satu objek
	 * {@code PenerimaanPengadaanMasterAssetDetail}, menampilkan informasi dokumen
	 * pengadaan, identitas aset, penyedia, satuan kerja, jumlah diterima, keterangan,
	 * serta toolbar aksi inventaris.
	 *
	 * <b>Cara kerja:</b><br>
	 * Setiap baris di-render dengan kolom-kolom berikut secara berurutan:
	 * <ol>
	 *   <li><b>Kolom 1 (Vbox dokumen pengadaan):</b> Menampilkan tiga label kecil
	 *       secara vertikal: kode penerimaan pengadaan, kode pemesanan pengadaan
	 *       (jika ada), dan kode uang muka (jika ada).</li>
	 *   <li><b>Kolom 2 (Kode aset):</b> Label berisi kode unik master aset.</li>
	 *   <li><b>Kolom 3 (Nama aset dengan revisi):</b> Widget RevisiHelper yang
	 *       menampilkan nama aset beserta tautan riwayat revisi jika tersedia.</li>
	 *   <li><b>Kolom 4 (Nama penyedia):</b> Label berisi nama vendor/penyedia
	 *       dari penerimaan pengadaan; kosong jika penyedia null.</li>
	 *   <li><b>Kolom 5 (Satuan kerja):</b> Label berisi nama satuan kerja yang
	 *       melakukan penerimaan; kosong jika satuan kerja null.</li>
	 *   <li><b>Kolom 6 (Jumlah diterima):</b> Label berisi angka jumlah unit
	 *       yang diterima, diformat dengan pemisah ribuan.</li>
	 *   <li><b>Kolom 7 (Keterangan):</b> Label berisi catatan atau keterangan
	 *       tambahan pada detail penerimaan.</li>
	 *   <li><b>Kolom 8 (Toolbar aksi):</b> Hbox berisi tombol-tombol aksi inventaris
	 *       yang dihasilkan oleh
	 *       {@code PenerimaanPengadaanMasterAssetDetailAction.tampilInventaris}.</li>
	 * </ol>
	 *
	 * <b>Threading:</b><br>
	 * Renderer dipanggil dari thread ZK Desktop pada saat model grid di-set.
	 * Tidak ada akses ke sumber daya bersama dari thread lain.
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Untuk menambah kolom baru, tambahkan komponen ke {@code arg0} (Row) setelah
	 * kolom yang ada, dan tambahkan definisi {@code <column>} yang sesuai di file
	 * ZUL. Perhatikan urutan penambahan komponen harus sesuai dengan urutan kolom
	 * di ZUL agar data tidak tampil di kolom yang salah.
	 */
	class JenisBarangRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * <b>Tujuan:</b><br>
		 * Mengisi satu baris ZK {@code Row} dengan data dari objek
		 * {@code PenerimaanPengadaanMasterAssetDetail} yang diberikan, menghasilkan
		 * tampilan baris lengkap pada grid monitor barang tidak habis pakai.
		 *
		 * <b>Cara kerja:</b><br>
		 * Metode meng-cast parameter {@code arg1} ke {@code PenerimaanPengadaanMasterAssetDetail},
		 * kemudian secara berurutan membuat dan menambahkan komponen ZK ke baris {@code arg0}:
		 * Vbox dengan tiga label kecil untuk informasi dokumen pengadaan, diikuti label kode
		 * aset, widget revisi nama aset, label penyedia, label satuan kerja, label jumlah
		 * diterima berformat angka, label keterangan, dan terakhir Hbox toolbar aksi inventaris
		 * yang dibuat oleh {@code PenerimaanPengadaanMasterAssetDetailAction.tampilInventaris}.
		 * Alignment vertikal baris diset ke "top" agar konten multi-baris (Vbox) rata atas.
		 *
		 * <b>Parameter:</b><br>
		 * @param arg0 baris ZK ({@code Row}) yang akan diisi komponen; merupakan wadah
		 *             untuk seluruh sel pada satu baris grid
		 * @param arg1 objek data baris; akan di-cast ke
		 *             {@code PenerimaanPengadaanMasterAssetDetail} yang berisi semua
		 *             informasi detail penerimaan pengadaan aset
		 *
		 * <b>Return:</b><br>
		 * Metode ini tidak mengembalikan nilai (void); hasilnya adalah modifikasi
		 * langsung pada komponen baris {@code arg0}.
		 *
		 * <b>Penanganan error:</b><br>
		 * @throws Exception jika cast {@code arg1} gagal (data model tidak konsisten)
		 *                   atau jika pembuatan komponen ZK gagal karena masalah
		 *                   siklus hidup komponen
		 *
		 * <b>Pemeliharaan:</b><br>
		 * Null-check defensif sudah diterapkan untuk {@code getPemesananPengadaanMasterAsset()},
		 * {@code getUangMuka()}, {@code getPenyedia()}, dan {@code getSatuanKerja()} untuk
		 * mencegah NullPointerException pada data yang belum lengkap. Pertahankan pola
		 * ini jika menambahkan referensi navigasi ke entitas terkait.
		 */
		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			PenerimaanPengadaanMasterAssetDetail penerimaanPengadaanMasterAssetDetail = (PenerimaanPengadaanMasterAssetDetail) arg1;

			Vbox vbox = new Vbox();
			vbox.setParent(arg0);
			new MyLabelAgakKecil(penerimaanPengadaanMasterAssetDetail.getPenerimaanPengadaanMasterAsset().getKode())
					.setParent(vbox);
			new MyLabelAgakKecil(penerimaanPengadaanMasterAssetDetail.getPenerimaanPengadaanMasterAsset()
					.getPemesananPengadaanMasterAsset() == null ? ""
							: penerimaanPengadaanMasterAssetDetail.getPenerimaanPengadaanMasterAsset()
									.getPemesananPengadaanMasterAsset().getKode())
					.setParent(vbox);
			new MyLabelAgakKecil(
					penerimaanPengadaanMasterAssetDetail.getPenerimaanPengadaanMasterAsset().getUangMuka() == null ? ""
							: penerimaanPengadaanMasterAssetDetail.getPenerimaanPengadaanMasterAsset().getUangMuka()
									.getKode())
					.setParent(vbox);

			new Label(penerimaanPengadaanMasterAssetDetail.getMasterAsset().getKode()).setParent(arg0);

			RevisiHelper.createNewRevisi(MasterAsset.class, penerimaanPengadaanMasterAssetDetail.getMasterAsset(),
					penerimaanPengadaanMasterAssetDetail.getMasterAsset().getNama()).setParent(arg0);

			new Label(penerimaanPengadaanMasterAssetDetail.getPenerimaanPengadaanMasterAsset().getPenyedia() == null
					? ""
					: penerimaanPengadaanMasterAssetDetail.getPenerimaanPengadaanMasterAsset().getPenyedia().getNama())
					.setParent(arg0);

			new Label(penerimaanPengadaanMasterAssetDetail.getPenerimaanPengadaanMasterAsset().getSatuanKerja() == null
					? ""
					: penerimaanPengadaanMasterAssetDetail.getPenerimaanPengadaanMasterAsset().getSatuanKerja()
							.getNama())
					.setParent(arg0);

			new Label(Common.numberFormat.get().format(penerimaanPengadaanMasterAssetDetail.getDiterima())).setParent(arg0);

			new Label(penerimaanPengadaanMasterAssetDetail.getKeterangan()).setParent(arg0);

			Hbox toolbar = new Hbox();
			toolbar.setParent(arg0);
			PenerimaanPengadaanMasterAssetDetailAction.tampilInventaris(penerimaanPengadaanMasterAssetDetail,
					penerimaanPengadaanMasterAssetDetail.getPenerimaanPengadaanMasterAsset(), toolbar, edit);

		}

	}

	/**
	 * <b>Tujuan:</b><br>
	 * Membangun dan mengembalikan objek {@code Criteria} Hibernate yang digunakan
	 * untuk mengambil data {@code PenerimaanPengadaanMasterAssetDetail} dari database,
	 * dengan filter utama: penerimaan sudah disetujui, tipe aset adalah tidak habis
	 * pakai, dan teks pencarian (jika ada) cocok dengan kode atau nama aset maupun
	 * kode penerimaan. Metode ini dapat dikonfigurasi untuk mode hitung (count)
	 * maupun mode pengambilan data dengan paginasi.
	 *
	 * <b>Cara kerja:</b><br>
	 * Sesi Hibernate saat ini diperoleh dari {@code HibernateUtil.currentSession()}.
	 * Criteria dibuat untuk entitas {@code PenerimaanPengadaanMasterAssetDetail}
	 * dengan join alias ke {@code masterAsset} dan {@code penerimaanPengadaanMasterAsset}.
	 * Filter utama yang selalu diterapkan:
	 * <ul>
	 *   <li>{@code penerimaanPengadaanMasterAsset.disetujuiOleh} tidak null, memastikan
	 *       hanya penerimaan yang sudah mendapat persetujuan yang ditampilkan.</li>
	 *   <li>{@code masterAsset.tipe == TIPE_TIDAK_HABIS_PAKAI}, memastikan hanya
	 *       aset durable goods yang ditampilkan, bukan bahan habis pakai.</li>
	 *   <li>Filter pencarian teks: jika {@code searchkode} null, gunakan
	 *       {@code sqlRestriction("1=1")} (semua data); jika kosong, gunakan
	 *       {@code sqlRestriction("true")}; jika ada nilai, gunakan OR dari
	 *       ilike pada kode penerimaan, kode aset, dan nama aset.</li>
	 * </ul>
	 * Jika parameter {@code order} true, tambahkan urutan descending berdasarkan ID
	 * dan terapkan batasan {@code maxResults} serta {@code firstResult} untuk paginasi.
	 *
	 * <b>Parameter:</b><br>
	 * @param order  jika {@code true}, tambahkan ORDER BY id DESC serta batasan
	 *               jumlah baris ({@code count}) dan offset awal ({@code start})
	 *               untuk mendukung paginasi; jika {@code false}, criteria digunakan
	 *               untuk operasi hitungan total baris
	 * @param count  jumlah maksimum baris yang dikembalikan; hanya berlaku saat
	 *               {@code order} bernilai {@code true}
	 * @param start  offset awal (indeks baris pertama yang dikembalikan); hanya
	 *               berlaku saat {@code order} bernilai {@code true}
	 *
	 * <b>Return:</b><br>
	 * @return objek {@code Criteria} Hibernate yang siap dieksekusi dengan
	 *         {@code .list()} untuk mengambil data atau {@code .setProjection(Projections.rowCount()).uniqueResult()}
	 *         untuk menghitung total baris
	 *
	 * <b>Penanganan error:</b><br>
	 * Jika sesi Hibernate tidak aktif atau koneksi database terputus, Hibernate
	 * akan melempar {@code HibernateException} saat criteria dieksekusi. Tidak ada
	 * penanganan exception di dalam metode ini; caller bertanggung jawab menangani
	 * exception tersebut.
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Jika perlu menambahkan filter baru (misalnya filter satuan kerja atau rentang
	 * tanggal), tambahkan {@code Restrictions} baru setelah filter pencarian teks
	 * yang ada. Pastikan alias join yang digunakan sudah didefinisikan di criteria
	 * sebelum digunakan dalam filter.
	 */
	public Criteria initCriteria(boolean order, int count, int start) {

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(PenerimaanPengadaanMasterAssetDetail.class)
				.createAlias("masterAsset", "masterAsset")
				.createAlias("penerimaanPengadaanMasterAsset", "penerimaanPengadaanMasterAsset")

				.add(Restrictions.isNotNull("penerimaanPengadaanMasterAsset.disetujuiOleh"))

				.add(Restrictions.eq("masterAsset.tipe", MasterAsset.TIPE_TIDAK_HABIS_PAKAI))

				.add((searchkode == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchkode.getValue().trim().equals("") ? Restrictions.sqlRestriction("true")
						: Restrictions.or(
								Restrictions.ilike("penerimaanPengadaanMasterAsset.kode", searchkode.getValue().trim(),
										MatchMode.ANYWHERE),
								Restrictions.or(
										Restrictions.ilike("masterAsset.kode", searchkode.getValue().trim(),
												MatchMode.ANYWHERE),
										Restrictions.ilike("masterAsset.nama", searchkode.getValue().trim(),
												MatchMode.ANYWHERE)))));

		if (order) {
			criteria.addOrder(Order.desc("id")).setMaxResults(count).setFirstResult(start);
		}

		return criteria;
	}

	/**
	 * <b>Tujuan:</b><br>
	 * Memuat dan menampilkan ulang data barang tidak habis pakai pada grid sesuai
	 * dengan kata kunci pencarian dan halaman yang sedang aktif. Metode ini merupakan
	 * inti dari fungsi pencarian/filter di halaman ini dan dipanggil baik secara
	 * manual (klik tombol cari) maupun secara otomatis (timer dan pergantian halaman).
	 *
	 * <b>Cara kerja:</b><br>
	 * Proses dilakukan dalam beberapa langkah:
	 * <ol>
	 *   <li>Menghitung total baris data yang sesuai filter dengan memanggil
	 *       {@code initCriteria(false, 0, 10)} dan menggunakan proyeksi
	 *       {@code Projections.rowCount()}, menghasilkan angka integer {@code size}.</li>
	 *   <li>Mengonfigurasi komponen paging: increment halaman disesuaikan untuk
	 *       mobile (5) atau desktop (10), mold "os" digunakan untuk gaya tampilan
	 *       yang lebih kompak, detail nonaktifkan, total size diset, dan paging
	 *       hanya ditampilkan jika total data melebihi {@code Common.ROWS_COUNT_ON_PAGE}.</li>
	 *   <li>Mengambil data untuk halaman aktif menggunakan {@code initCriteria(true, ...)}
	 *       dengan offset dihitung dari {@code paging.getActivePage() * ROWS_COUNT_ON_PAGE}.</li>
	 *   <li>Membuat model list dari hasil query dan menerapkan renderer baru
	 *       {@code JenisBarangRenderer} ke grid.</li>
	 *   <li>Memanggil {@code grid.setModelCheckMobile(strset)} untuk menampilkan
	 *       data dengan dukungan deteksi mobile (menampilkan card atau tabel
	 *       tergantung perangkat).</li>
	 * </ol>
	 *
	 * <b>Parameter:</b><br>
	 * @param event objek event ZK yang memicu pencarian; bisa null jika dipanggil
	 *              secara programatik (misalnya dari timer atau inisialisasi). Event
	 *              tidak digunakan secara langsung dalam metode ini.
	 *
	 * <b>Return:</b><br>
	 * Metode ini tidak mengembalikan nilai (void); hasilnya adalah pembaruan tampilan
	 * grid dan komponen paging pada halaman ZK.
	 *
	 * <b>Penanganan error:</b><br>
	 * Exception dari query Hibernate (misalnya koneksi terputus) tidak ditangani
	 * di sini; akan dipropagasi ke framework ZK. Anotasi {@code @SuppressWarnings("unchecked")}
	 * digunakan karena metode {@code Criteria.list()} mengembalikan raw type pada
	 * Java generics level kompiler.
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Jika perlu menambahkan logika pasca-pencarian (misalnya menyorot baris tertentu
	 * atau menampilkan pesan "tidak ada data"), tambahkan logika tersebut setelah
	 * {@code grid.setModelCheckMobile(strset)}. Pastikan setiap perubahan pada
	 * logika paginasi konsisten dengan cara {@code initCriteria} menghitung offset.
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		int size = ((Number) initCriteria(false, 0, 10).setProjection(Projections.rowCount()).uniqueResult())
				.intValue();
		paging.setPageIncrement(Common.isMobile() ? 5 : 10);
		paging.setMold("os");
		paging.setDetailed(false);
		paging.setTotalSize(size);
		paging.setVisible(size > Common.ROWS_COUNT_ON_PAGE);
		List<PenerimaanPengadaanMasterAssetDetail> item = initCriteria(true, Common.ROWS_COUNT_ON_PAGE,
				Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(item);
		grid.setRowRenderer(new JenisBarangRenderer());
		grid.setModelCheckMobile(strset);

	}

}
