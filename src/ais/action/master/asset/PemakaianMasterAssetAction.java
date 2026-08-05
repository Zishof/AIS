package ais.action.master.asset;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.asset.helper.PemakaianMasterAssetDetailAction;
import ais.action.master.asset.helper.PemakaianPunyaMasterAssetHelper;
import ais.action.master.helper.AmbilDataRuangBanbox;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.library.util.LibraryUtil;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.action.master.sop.TampilanAlurSopAction;
import ais.action.report.Report;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.Ruang;
import ais.database.model.Tbmuser;
import ais.database.model.asset.DetailTransaksiAsset;
import ais.database.model.asset.Lokasi;
import ais.database.model.asset.NomorSuratAlurPengadaan;
import ais.database.model.asset.PemakaianMasterAsset;
import ais.database.model.asset.PemakaianMasterAssetDetail;
import ais.database.model.asset.PemilikAsset;
import ais.database.model.rab.Mitra;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.sop.DataSop;
import ais.database.model.sop.DisposisiSop;
import ais.database.model.surat.NomorSurat;
import ais.ui.util.FormSop;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;

/**
 * <h3>PemakaianMasterAssetAction</h3>
 *
 * <b>Untuk apa:</b><br>
 * Controller ZK (ZUL composer) yang mengelola seluruh siklus hidup pencatatan
 * <em>pemakaian aset</em> (asset consumption/usage). Modul ini memungkinkan
 * pengguna membuat dokumen pemakaian aset yang merinci aset-aset mana yang
 * digunakan, berapa jumlahnya, oleh satuan kerja mana, dan pada tanggal berapa.
 * Setelah dokumen disetujui, sistem secara otomatis mencatat transaksi pengurangan
 * stok aset melalui {@link DetailTransaksiAsset} dengan kode transaksi
 * {@link LibraryUtil#PEMAKAIAN}. Pembatalan persetujuan akan menghapus
 * transaksi stok tersebut.<br><br>
 *
 * <b>Cara kerja:</b><br>
 * <ol>
 *   <li>Kelas ini mengimplementasikan {@link FormSop}, yang memungkinkannya diintegrasikan
 *       ke dalam alur kerja SOP (Standard Operating Procedure) sistem. Method {@code form()}
 *       membangun tampilan form secara programatik dan dapat dipanggil baik oleh SOP maupun
 *       oleh modal tambah/ubah internal.</li>
 *   <li>Saat {@code doAfterCompose} dipanggil, komponen-komponen ZUL di-wire secara otomatis,
 *       filter satuan kerja diinisialisasi dengan model pohon, dan grid data dimuat melalui
 *       timer ZK untuk mencegah blocking UI.</li>
 *   <li>Renderer baris grid ({@link PemakaianMasterAssetRenderer}) menampilkan detail
 *       pemakaian menggunakan komponen {@link PemakaianMasterAssetDetailAction} yang di-embed
 *       langsung ke dalam sel pertama grid, memberikan tampilan master-detail inline.</li>
 *   <li>Proses simpan ({@code onSave}) melakukan validasi input, menyimpan entitas
 *       {@link PemakaianMasterAsset} ke database, menyimpan detail-detailnya, dan memicu
 *       pencetakan PDF secara asinkron melalui timer.</li>
 *   <li>Proses persetujuan ({@code disetujui} event listener) mencatat
 *       {@link DetailTransaksiAsset} untuk setiap baris detail, yang secara efektif
 *       mengurangi stok aset. Pembatalan menghapus record tersebut.</li>
 *   <li>Kode dokumen di-generate melalui {@code generateCode} yang menggunakan
 *       {@link NomorSuratAlurPengadaan#PEMAKAIAN_BARANG_DATA} sebagai template nomor surat,
 *       dan dijamin keunikannya melalui {@code KodeUnikUtil.pastikanUnik}.</li>
 * </ol>
 *
 * <b>Threading:</b><br>
 * Semua operasi dijalankan di thread event ZK (single-threaded event dispatch).
 * Tidak ada thread latar yang dibuat. Pencetakan PDF setelah simpan menggunakan
 * {@code Common.createDefaultTimer} untuk menunda eksekusi satu siklus event, sehingga
 * tidak memblok respons UI. Akses Hibernate menggunakan {@code HibernateUtil.currentSession()}
 * yang terikat ke thread event ZK saat ini.<br><br>
 *
 * <b>Pemeliharaan:</b><br>
 * <ul>
 *   <li>Kelas ini digunakan dalam dua mode: mode standalone (dipanggil langsung dari ZUL)
 *       dan mode SOP (dipanggil oleh sistem alur kerja melalui interface {@link FormSop}).
 *       Flag {@code persetujuan} menentukan apakah form ditampilkan dalam mode read-only
 *       untuk persetujuan.</li>
 *   <li>Field-field form ({@code pemilikAsset}, {@code lokasi}, {@code ruang}) saat ini
 *       disembunyikan ({@code setVisible(false)}) di form; jika perlu diaktifkan kembali,
 *       hapus baris tersebut.</li>
 *   <li>Komponen ZUL yang harus tersedia: {@code addWindow}, {@code grid}, {@code paging},
 *       {@code searchkode}, {@code searchpemilikAsset}, {@code searchruang}, {@code blmDisetujui},
 *       {@code disetujui}, {@code searchparent}, {@code add}.</li>
 *   <li>Laporan PDF menggunakan template {@code asset/pemakaian}; pastikan template
 *       JasperReports tersedia di classpath yang sesuai.</li>
 * </ul>
 *
 * @author AIS Development Team
 * @version 1.0
 * @since ZKoss 5.5, Java 1.7
 */
public class PemakaianMasterAssetAction extends GenericAutowireComposer implements FormSop {

	/**
	 * Versi serial untuk serialisasi kelas oleh mekanisme ZK.
	 */
	private static final long serialVersionUID = -5779730267402400328L;

	/** Window modal yang menampilkan form tambah/ubah pemakaian aset. */
	private MyWindow addWindow;

	/** Grid utama yang menampilkan daftar dokumen pemakaian aset dengan detail inline. */
	private MyGrid grid;

	/** Komponen paging untuk navigasi halaman pada grid utama. */
	private Paging paging;

	/** Kotak teks pencarian berdasarkan kode pemakaian. */
	private Textbox searchkode;

	/** Combo filter pemilik aset pada panel pencarian. */
	private Combobox searchpemilikAsset;

	/** Banbox filter ruang pada panel pencarian; nilai ruang disimpan sebagai atribut. */
	private AmbilDataRuangBanbox searchruang;

	/** Label yang menampilkan kode dokumen pemakaian yang di-generate otomatis. */
	private Label kode;

	/**
	 * Combo pemilih pemilik aset pada form isian.
	 * Saat ini baris form ini disembunyikan; field tetap dipertahankan untuk fleksibilitas.
	 */
	private Combobox pemilikAsset;

	/**
	 * Combo pemilih lokasi aset pada form isian.
	 * Saat ini baris form ini disembunyikan.
	 */
	private Combobox lokasi;

	/**
	 * Banbox pemilih ruang pada form isian.
	 * Saat ini baris form ini disembunyikan.
	 */
	private AmbilDataRuangBanbox ruang;

	/** Kotak teks keterangan/deskripsi dokumen pemakaian. */
	private MyTextbox keterangan;

	/** Datebox tanggal pembuatan/pemakaian dokumen. */
	private MyDatebox tanggalPembuatan;

	/** {@code true} jika pengguna memiliki hak UPDATE untuk mengubah data pemakaian. */
	private boolean edit = false;

	/** {@code true} jika pengguna memiliki hak DELETE untuk menghapus data pemakaian. */
	private boolean delete = false;

	/** {@code true} jika pengguna memiliki hak APPROVE untuk menyetujui pemakaian. */
	private boolean approve = false;

	/** {@code true} jika pengguna memiliki hak REJECT untuk membatalkan persetujuan. */
	private boolean reject = false;

	/**
	 * Disposisi SOP yang terkait dengan pemakaian ini, jika dokumen dibuat melalui
	 * alur kerja SOP. {@code null} jika dibuat langsung tanpa SOP.
	 */
	private DisposisiSop disposisiSop = null;

	/** Entitas pemakaian aset yang sedang aktif diedit atau ditampilkan. */
	private PemakaianMasterAsset pemakaianMasterAsset;

	/** Tombol tambah data baru pada toolbar grid utama. */
	private MyToolbarbuttonConfig add;

	/** Grid yang menampilkan daftar detail aset (baris-baris item) dalam form isian. */
	private MyGrid gridMasterAsset;

	/** Combo pemilih mitra/vendor pada form isian. Saat ini disembunyikan. */
	private Combobox mitra;

	/** Banbox pemilih satuan kerja pada form isian pemakaian. */
	private AmbilDataSatuanKerjaBanbox satuanKerja;

	/** Checkbox filter "Belum Disetujui" pada panel pencarian grid. */
	private MyCheckboxConfig blmDisetujui;

	/** Checkbox filter "Sudah Disetujui" pada panel pencarian grid. */
	private MyCheckboxConfig disetujui;

	/**
	 * Model pohon satuan kerja yang digunakan untuk membangun hierarki filter
	 * dan menentukan satuan kerja anak saat filter satuan kerja induk dipilih.
	 */
	private SatuanKerjaTreeModel satuanKerjaTreeModel;

	/**
	 * Menandai apakah form sedang berjalan dalam mode persetujuan SOP (read-only).
	 * Jika {@code true}, field-field form diganti dengan Label statis dan tombol
	 * ubah/hapus disembunyikan.
	 */
	private boolean persetujuan = false;

	/** Banbox filter satuan kerja induk pada panel pencarian; memfilter berdasarkan hierarki. */
	private AmbilDataSatuanKerjaBanbox searchparent;

	/**
	 * <b>Tujuan:</b> Konstruktor default tanpa argumen yang diperlukan oleh ZK
	 * untuk instantiasi composer melalui refleksi saat ZUL di-load.<br><br>
	 *
	 * <b>Cara kerja:</b><br>
	 * Tidak melakukan inisialisasi apapun; semua inisialisasi dilakukan di
	 * {@code doAfterCompose}. Konstruktor ini digunakan ketika halaman diakses
	 * secara langsung (bukan melalui SOP).<br><br>
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Pertahankan konstruktor ini agar ZK dapat membuat instance melalui refleksi.
	 * Jangan menambahkan logika inisialisasi di sini.
	 */
	public PemakaianMasterAssetAction() {

	}

	/**
	 * <b>Tujuan:</b> Konstruktor yang menerima flag mode persetujuan, digunakan
	 * ketika instance dibuat secara programatik oleh sistem SOP atau halaman
	 * persetujuan untuk menampilkan form dalam mode read-only.<br><br>
	 *
	 * <b>Cara kerja:</b><br>
	 * Menetapkan flag {@code persetujuan} yang akan digunakan oleh method {@code form()}
	 * untuk memilih antara komponen input interaktif atau label statis read-only
	 * pada setiap field form.<br><br>
	 *
	 * <b>Parameter:</b><br>
	 * @param persetujuan {@code true} untuk mode read-only (tampilan persetujuan);
	 *                    {@code false} untuk mode edit penuh
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Nilai {@code persetujuan} dapat diubah setelah konstruksi melalui
	 * {@code setPersetujuan(boolean)}; tidak perlu membuat instance baru.
	 */
	public PemakaianMasterAssetAction(boolean persetujuan) {
		this.persetujuan = persetujuan;
	}

	/**
	 * <b>Tujuan:</b> Interceptor siklus hidup ZK yang dijalankan <em>sebelum</em> komponen
	 * ZUL di-compose. Digunakan untuk memeriksa keamanan akses halaman.<br><br>
	 *
	 * <b>Cara kerja:</b><br>
	 * Memanggil {@code Common.doCheckSecurity()} untuk memvalidasi sesi dan hak akses
	 * sebelum komponen apapun diinisialisasi, kemudian mendelegasikan ke
	 * {@code super.doBeforeCompose} untuk melanjutkan proses compose ZK.<br><br>
	 *
	 * <b>Parameter:</b><br>
	 * @param page halaman ZK tempat komponen akan di-compose
	 * @param parent komponen induk dalam hierarki ZK
	 * @param compInfo metadata komponen dari definisi ZUL
	 *
	 * <b>Return:</b><br>
	 * @return {@code ComponentInfo} dari implementasi induk untuk melanjutkan compose.<br><br>
	 *
	 * <b>Penanganan error:</b><br>
	 * Jika sesi tidak valid, {@code doCheckSecurity} akan menghentikan proses sebelum
	 * {@code super.doBeforeCompose} dipanggil.<br><br>
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Tetap gunakan hanya untuk pemeriksaan keamanan awal. Logika bisnis di sini
	 * dapat menyebabkan masalah karena komponen ZUL belum di-wire.
	 */
	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	/**
	 * <b>Tujuan:</b> Metode inisialisasi utama yang dijalankan ZK setelah seluruh
	 * komponen ZUL berhasil di-wire ke field Java. Menginisialisasi semua komponen UI,
	 * hak akses pengguna, model data, dan memuat data grid awal.<br><br>
	 *
	 * <b>Cara kerja:</b><br>
	 * <ol>
	 *   <li>Memanggil {@code super.doAfterCompose} untuk menyelesaikan wire otomatis ZK.</li>
	 *   <li>Memvalidasi sesi {@code usersTemp} dan privilege READ; jika gagal, mengarahkan
	 *       ke logoff.</li>
	 *   <li>Mendaftarkan event listener pada {@code searchparent} agar perubahan filter
	 *       satuan kerja memicu pencarian ulang otomatis.</li>
	 *   <li>Menginisialisasi {@link SatuanKerjaTreeModel} (tidak include satuan kerja
	 *       non-aktif: {@code false}).</li>
	 *   <li>Mengisi combo {@code searchpemilikAsset} dengan semua pemilik aset aktif.</li>
	 *   <li>Mengatur visibilitas dan tooltip tombol {@code add} berdasarkan privilege CREATE.</li>
	 *   <li>Menetapkan flag {@code edit}, {@code delete}, {@code approve}, {@code reject}
	 *       dari privilege masing-masing.</li>
	 *   <li>Menginisialisasi komponen paging dengan event listener yang memicu
	 *       {@code onSearchDefault} saat halaman berubah.</li>
	 *   <li>Menggunakan timer ZK untuk memuat data grid pada siklus event berikutnya,
	 *       mencegah blocking render awal halaman.</li>
	 * </ol>
	 *
	 * <b>Parameter:</b><br>
	 * @param comp komponen root ZUL yang selesai di-compose
	 * @throws Exception jika terjadi kesalahan inisialisasi komponen ZK atau akses database
	 *
	 * <b>Penanganan error:</b><br>
	 * Kegagalan validasi sesi dialihkan ke logoff. Exception lain merambat ke ZK
	 * dan ditampilkan sebagai error dialog.<br><br>
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Pastikan komponen ZUL yang diperlukan tersedia dan namanya sesuai dengan
	 * nama field Java yang ada. Tambahan filter baru harus diinisialisasi di sini
	 * sebelum pemuatan data grid.
	 */
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
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

		Common.insertComboDanSemua(searchpemilikAsset, new String[] { "nama", "id" }, "keterangan", PemilikAsset.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		approve = CommonPrivilages.checkPrevilages(CommonPrivilages.APPROVE);
		reject = CommonPrivilages.checkPrevilages(CommonPrivilages.REJECT);

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
	}

	/**
	 * <b>Untuk apa:</b><br>
	 * Inner class renderer baris grid yang menampilkan setiap dokumen
	 * {@link PemakaianMasterAsset} beserta detail aset-nya secara inline, lengkap
	 * dengan tombol aksi: Cetak, Setujui, Batalkan, Ubah, dan Hapus.<br><br>
	 *
	 * <b>Cara kerja:</b><br>
	 * Setiap baris ({@link Row}) menerima satu entitas {@link PemakaianMasterAsset}.
	 * Kolom pertama berisi komponen {@link PemakaianMasterAssetDetailAction} yang
	 * di-embed langsung untuk menampilkan daftar aset yang dipakai. Kolom-kolom
	 * berikutnya menampilkan kode, informasi pembuat, informasi persetujuan, keterangan
	 * dan link SOP, serta toolbar aksi. Visibilitas tombol disesuaikan dengan hak akses
	 * dan status persetujuan dokumen.<br><br>
	 *
	 * <b>Threading:</b><br>
	 * Dijalankan di thread event ZK. Operasi database dalam event listener tombol
	 * menggunakan {@code HibernateUtil.currentSession()}.<br><br>
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Urutan penambahan komponen ke {@code arg0} harus konsisten dengan jumlah dan
	 * urutan kolom di definisi grid ZUL. Tombol "Ubah" dan "Hapus" hanya tampil
	 * jika dokumen belum disetujui ({@code disetujuiOleh == null}).
	 */
	class PemakaianMasterAssetRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * <b>Tujuan:</b> Merender satu baris grid dengan detail pemakaian aset, informasi
		 * pembuat dan persetujuan, keterangan, serta tombol-tombol aksi yang dikontrol
		 * berdasarkan hak akses pengguna dan status dokumen.<br><br>
		 *
		 * <b>Cara kerja:</b><br>
		 * <ol>
		 *   <li>Membuat dan menyematkan {@link PemakaianMasterAssetDetailAction} ke sel
		 *       pertama untuk tampilan detail aset inline.</li>
		 *   <li>Membuat link revisi menggunakan {@link RevisiHelper} yang menampilkan
		 *       riwayat perubahan dokumen.</li>
		 *   <li>Menampilkan Vbox berisi nama pembuat dan tanggal pembuatan.</li>
		 *   <li>Menampilkan Vbox berisi nama dan tanggal persetujuan (diperbarui secara
		 *       reaktif saat tombol setujui/batalkan diklik).</li>
		 *   <li>Menampilkan keterangan dan link SOP jika ada.</li>
		 *   <li>Menyiapkan semua tombol dengan event listener masing-masing:
		 *     <ul>
		 *       <li><b>Cetak:</b> Menghasilkan laporan PDF menggunakan {@code Report.generatePDFReport}.</li>
		 *       <li><b>Setujui:</b> Meminta konfirmasi, menyimpan persetujuan, membuat
		 *           {@link DetailTransaksiAsset} untuk setiap item detail, memperbarui
		 *           label status secara reaktif.</li>
		 *       <li><b>Batalkan:</b> Meminta konfirmasi, menghapus persetujuan dan semua
		 *           {@link DetailTransaksiAsset} terkait menggunakan SQL native.</li>
		 *       <li><b>Ubah:</b> Membuka {@code addWindow} modal dengan form edit.</li>
		 *       <li><b>Hapus:</b> Meminta konfirmasi, menghapus semua detail terlebih dahulu
		 *           lalu menghapus dokumen induk.</li>
		 *     </ul>
		 *   </li>
		 * </ol>
		 *
		 * <b>Parameter:</b><br>
		 * @param arg0 baris ZK ({@link Row}) yang akan diisi komponen-komponen UI
		 * @param arg1 objek data baris berupa {@link PemakaianMasterAsset}
		 * @throws Exception jika terjadi kesalahan akses database atau manipulasi komponen ZK
		 *
		 * <b>Penanganan error:</b><br>
		 * Kesalahan penghapusan data ditangkap dan ditampilkan sebagai pesan error yang
		 * menyertakan pesan exception asli. Exception dari operasi Hibernate di event
		 * listener merambat ke ZK framework. Exception {@code ClassCastException} tidak
		 * mungkin terjadi karena model grid selalu berisi {@link PemakaianMasterAsset}.<br><br>
		 *
		 * <b>Pemeliharaan:</b><br>
		 * Label status persetujuan ({@code disetujuiOleh} dan {@code disetujuiTanggal})
		 * diperbarui secara in-place setelah aksi setujui/batalkan untuk menghindari
		 * reload grid penuh. Jika ada field baru yang perlu ditampilkan, tambahkan ke
		 * Vbox yang sesuai.
		 */
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final PemakaianMasterAsset pemakaianMasterAsset = (PemakaianMasterAsset) arg1;

			final PemakaianMasterAssetDetailAction detail;
			(detail = new PemakaianMasterAssetDetailAction(pemakaianMasterAsset)).setParent(arg0);

			RevisiHelper
					.createNewRevisi(PemakaianMasterAsset.class, pemakaianMasterAsset, pemakaianMasterAsset.getKode())
					.setParent(arg0);

			Vbox a = new Vbox();
			a.setParent(arg0);
			new MyLabelAgakKecil(pemakaianMasterAsset.getDibuatOleh() == null ? ""
					: pemakaianMasterAsset.getDibuatOleh().getUserNama()).setParent(a);
			new MyLabelAgakKecil(pemakaianMasterAsset.getTanggalPembuatan() == null ? ""
					: Common.dateFormat3.get().format(pemakaianMasterAsset.getTanggalPembuatan())).setParent(a);

			a = new Vbox();
			a.setParent(arg0);
			final MyLabelAgakKecil disetujuiOleh;
			(disetujuiOleh = new MyLabelAgakKecil(pemakaianMasterAsset.getDisetujuiOleh() == null ? ""
					: pemakaianMasterAsset.getDisetujuiOleh().getUserNama())).setParent(a);
			final MyLabelAgakKecil disetujuiTanggal;
			(disetujuiTanggal = new MyLabelAgakKecil(pemakaianMasterAsset.getTanggalPersetujuan() == null ? ""
					: Common.dateFormat3.get().format(pemakaianMasterAsset.getTanggalPersetujuan()))).setParent(a);

			Vbox vbox1 = new Vbox();
			vbox1.setParent(arg0);
			new MyLabelKecil(Common.simpleString(pemakaianMasterAsset.getKeterangan())).setParent(vbox1);
			if (pemakaianMasterAsset.getDisposisiSop() != null) {
				A aa;
				(aa = new A("SOP " + Common.simpleString(pemakaianMasterAsset.getDisposisiSop().getKeterangan()) + " ("
						+ pemakaianMasterAsset.getDisposisiSop().getSop().getNama() + ")")).setParent(vbox1);
				aa.setStyle("font-size:9px;");
				aa.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						TampilanAlurSopAction.prosess(pemakaianMasterAsset.getDisposisiSop().getId(), null, null, true,
								arg0.getTarget());
					}
				});
			}

			Hbox toolbar = new Hbox();

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/print.png");
			button.setTooltiptext("Cetak Pemakaian");
			button.addEventListener("onClick", new EventListener() {
				@SuppressWarnings({})
				@Override
				public void onEvent(Event event) throws Exception {

					Report.generatePDFReport(Report.PDF, parameter(pemakaianMasterAsset), "asset/pemakaian",
							pemakaianMasterAsset.getTanggalPembuatan());
				}

			});
			button.setParent(toolbar);

			final MyToolbarbuttonConfig disetujui = new MyToolbarbuttonConfig("", "/img/svg/check2.svg");

			final MyToolbarbuttonConfig dibatalkan = new MyToolbarbuttonConfig("", "/img/svg/warning-outline.svg");
			final MyToolbarbuttonConfig hapus = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			final MyToolbarbuttonConfig rubah = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");

			disetujui.setVisible(approve && pemakaianMasterAsset.getDisetujuiOleh() == null);
			dibatalkan.setVisible(reject && pemakaianMasterAsset.getDisetujuiOleh() != null);

			disetujui.setTooltiptext("Persetujuan");

			disetujui.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah yakin ingin mensetujui Pemakaian ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@SuppressWarnings({ "unchecked" })
								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										Session session = HibernateUtil.currentSession();

										pemakaianMasterAsset.setDisetujuiOleh(Common.getCurrentUser());
										pemakaianMasterAsset.setTanggalPersetujuan(ais.ui.util.WaktuUtil.getDate());
										Common.refreshUpdate(session, (pemakaianMasterAsset));

										List<PemakaianMasterAssetDetail> pemakaianMasterAssetDetails = session
												.createCriteria(PemakaianMasterAssetDetail.class)
												.add(Restrictions.eq("pemakaianMasterAsset", pemakaianMasterAsset))
												.list();

										session.createSQLQuery(
												"delete from asset.detail_transaksi_asset where pemakaian_master_asset_detail in (select id from asset.pemakaian_master_asset_detail where pemakaian_master_asset = "
														+ pemakaianMasterAsset.getId() + ");")
												.executeUpdate();
										for (PemakaianMasterAssetDetail pemakaianMasterAssetDetail : pemakaianMasterAssetDetails) {

											DetailTransaksiAsset detailTransaksiAsset = new DetailTransaksiAsset();
											detailTransaksiAsset
													.setPemakaianMasterAssetDetail(pemakaianMasterAssetDetail);
											detailTransaksiAsset.setQtyBonus(0.0);

											detailTransaksiAsset
													.setMasterAsset(pemakaianMasterAssetDetail.getMasterAsset());
											detailTransaksiAsset.setKeterangan("Transaksi Pemakaian");
											detailTransaksiAsset.setKodeTransaksi(LibraryUtil.PEMAKAIAN);
											detailTransaksiAsset
													.setPemilikAsset(pemakaianMasterAsset.getPemilikAsset());
											detailTransaksiAsset.setLokasi(pemakaianMasterAsset.getLokasi());
											detailTransaksiAsset.setRuang(pemakaianMasterAsset.getRuang());
											detailTransaksiAsset.setQty(pemakaianMasterAssetDetail.getJumlah());
											detailTransaksiAsset.setTanggal(pemakaianMasterAsset.getTanggalPembuatan());

											session.save(detailTransaksiAsset);
										}

										disetujuiTanggal
												.setValue(pemakaianMasterAsset.getTanggalPersetujuan() == null ? ""
														: Common.dateFormat3.get()
																.format(pemakaianMasterAsset.getTanggalPersetujuan()));
										disetujuiOleh.setValue(pemakaianMasterAsset.getDisetujuiOleh() == null ? ""
												: pemakaianMasterAsset.getDisetujuiOleh().getUserNama());
										disetujui
												.setVisible(approve && pemakaianMasterAsset.getDisetujuiOleh() == null);
										dibatalkan
												.setVisible(reject && pemakaianMasterAsset.getDisetujuiOleh() != null);
										rubah.setVisible(edit && pemakaianMasterAsset.getDisetujuiOleh() == null);
										hapus.setVisible(delete && pemakaianMasterAsset.getDisetujuiOleh() == null);
										if (detail != null) {
											Common.clear(detail);
											detail.display();
										}

									}
								}
							});
				}

			});
			disetujui.setParent(toolbar);

			dibatalkan.setTooltiptext("Dibatalkan");
			dibatalkan.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					MyMessageboxConfig.show("Apakah yakin ingin membatalkan Pemakaian ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										Session session = HibernateUtil.currentSession();

										pemakaianMasterAsset.setDisetujuiOleh(null);
										pemakaianMasterAsset.setTanggalPersetujuan(null);
										Common.refreshUpdate(session, (pemakaianMasterAsset));

										session.createSQLQuery(
												"delete from asset.detail_transaksi_asset where pemakaian_master_asset_detail in (select id from asset.pemakaian_master_asset_detail where pemakaian_master_asset = "
														+ pemakaianMasterAsset.getId() + ");")
												.executeUpdate();

										disetujuiTanggal
												.setValue(pemakaianMasterAsset.getTanggalPersetujuan() == null ? ""
														: Common.dateFormat3.get()
																.format(pemakaianMasterAsset.getTanggalPersetujuan()));
										disetujuiOleh.setValue(pemakaianMasterAsset.getDisetujuiOleh() == null ? ""
												: pemakaianMasterAsset.getDisetujuiOleh().getUserNama());
										disetujui
												.setVisible(approve && pemakaianMasterAsset.getDisetujuiOleh() == null);
										dibatalkan
												.setVisible(reject && pemakaianMasterAsset.getDisetujuiOleh() != null);
										rubah.setVisible(edit && pemakaianMasterAsset.getDisetujuiOleh() == null);
										hapus.setVisible(delete && pemakaianMasterAsset.getDisetujuiOleh() == null);
										if (detail != null) {
											Common.clear(detail);
											detail.display();
										}
									}
								}
							});
				}

			});
			dibatalkan.setParent(toolbar);

			rubah.setTooltiptext("Ubah Data");
			rubah.setVisible(edit && pemakaianMasterAsset.getDisetujuiOleh() == null);
			rubah.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(pemakaianMasterAsset);
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			rubah.setParent(toolbar);

			hapus.setTooltiptext("Hapus Data");
			hapus.setVisible(delete && pemakaianMasterAsset.getDisetujuiOleh() == null);
			hapus.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@SuppressWarnings("unchecked")
								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {

											Session session = HibernateUtil.currentSession();
											List<PemakaianMasterAssetDetail> pemakaianMasterAssetDetails = session
													.createCriteria(PemakaianMasterAssetDetail.class)
													.add(Restrictions.eq("pemakaianMasterAsset", pemakaianMasterAsset))
													.list();
											for (PemakaianMasterAssetDetail pemakaianMasterAssetDetail : pemakaianMasterAssetDetails) {
												session.delete(pemakaianMasterAssetDetail);
											}

											Common.createDefaultTimer(new EventListener() {

												@Override
												public void onEvent(Event arg0) throws Exception {
													Session session = HibernateUtil.currentSession();
													Common.refreshDelete(session, pemakaianMasterAsset);

													onSearchDefault(arg0);
												}
											});
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
										PesanFormalHelper.tampilkanGagalException(
												"penghapusan data ini",
												"Data yang Bapak/Ibu coba hapus kemungkinan besar masih digunakan/direferensikan oleh data transaksi Asset lain di sistem (mis. dokumen pengadaan, penerimaan, pembayaran, peminjaman, atau riwayat terkait), sehingga database menolak penghapusan demi menjaga integritas data.",
												e,
												new String[] {
														"Periksa apakah data ini masih digunakan/dirujuk oleh transaksi atau data lain yang berelasi.",
														"Hapus atau ubah terlebih dahulu data yang masih berelasi tersebut, baru ulangi penghapusan data ini.",
														"Nonaktifkan saja data ini (bukan menghapus) apabila data ini memang masih perlu dirujuk oleh data lain." });

										}

									}

								}
							});

				}
			});
			hapus.setParent(toolbar);
			toolbar.setParent(arg0);
		}
	}

	/**
	 * <b>Tujuan:</b> Event handler ZK yang dipanggil saat pengguna mengklik tombol
	 * "Tambah" untuk membuat dokumen pemakaian aset baru. Membuka window modal
	 * dengan form kosong yang siap diisi.<br><br>
	 *
	 * <b>Cara kerja:</b><br>
	 * Membuat instance {@link PemakaianMasterAsset} baru (tanpa ID), kemudian
	 * memanggil {@code init()} untuk membangun tampilan form di {@code addWindow},
	 * lalu menjadikan window visible dan membukanya sebagai modal.<br><br>
	 *
	 * <b>Parameter:</b><br>
	 * @param event event ZK dari onClick tombol tambah di toolbar grid
	 * @throws Exception jika terjadi kesalahan saat membangun form atau membuka window
	 *
	 * <b>Penanganan error:</b><br>
	 * Exception merambat ke ZK framework dan ditampilkan sebagai error dialog.<br><br>
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Untuk menambahkan nilai default pada entitas baru (misalnya tanggal otomatis),
	 * set field tersebut pada objek {@link PemakaianMasterAsset} baru sebelum
	 * memanggil {@code init()}.
	 */
	public void onAdd(Event event) throws Exception {
		init(new PemakaianMasterAsset());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	/**
	 * <b>Tujuan:</b> Membangun dan mengembalikan komponen grid form isian pemakaian aset
	 * yang dapat digunakan baik oleh modal internal maupun oleh sistem SOP. Method ini
	 * merupakan implementasi dari interface {@link FormSop} dan menjadi titik tunggal
	 * pembangunan form untuk semua skenario penggunaan.<br><br>
	 *
	 * <b>Cara kerja:</b><br>
	 * <ol>
	 *   <li>Menginisialisasi {@link SatuanKerjaTreeModel} jika belum ada.</li>
	 *   <li>Menetapkan entitas {@code pemakaianMasterAsset} dan {@code disposisiSop}
	 *       aktif dari parameter yang diberikan, dengan logika khusus untuk
	 *       mempertahankan disposisiSop lama jika disposisiSop baru null/kosong.</li>
	 *   <li>Membuat grid dengan dua kolom dan menetapkan satuan kerja dari pengguna aktif.</li>
	 *   <li>Jika kode belum ada, men-generate kode baru menggunakan {@code generateCode(false)}
	 *       (tanpa menaikkan counter nomor surat).</li>
	 *   <li>Membangun baris-baris form secara programatik dengan memilih antara komponen
	 *       input (mode edit) atau Label statis (mode persetujuan) untuk setiap field:
	 *       Kode, Satuan Kerja, Tanggal, Pemilik, Lokasi, Ruang, Mitra, Keterangan.</li>
	 *   <li>Baris terakhir berisi grid detail aset yang dibangun oleh
	 *       {@link PemakaianPunyaMasterAssetHelper#initDetail}.</li>
	 * </ol>
	 *
	 * <b>Parameter:</b><br>
	 * @param generalValueObject entitas pemakaian aset; harus berupa {@link PemakaianMasterAsset}
	 * @param disposisiSop disposisi SOP yang mengaitkan form ini dengan alur kerja SOP;
	 *                     {@code null} jika tidak melalui SOP
	 * @param save tombol simpan yang disediakan oleh pemanggil (SOP atau modal internal)
	 *             agar event listener save dapat dipasang secara terpusat
	 * @param setujuiData event listener persetujuan dari SOP; {@code null} jika tidak
	 *                    digunakan
	 * @return {@link MyGrid} yang berisi semua komponen form, siap untuk ditambahkan
	 *         ke layout window atau panel SOP
	 * @throws Exception jika terjadi kesalahan inisialisasi komponen ZK atau akses database
	 *
	 * <b>Penanganan error:</b><br>
	 * Exception merambat ke pemanggil. Pastikan {@code generalValueObject} adalah
	 * instance {@link PemakaianMasterAsset} yang valid; {@code ClassCastException}
	 * akan dilempar jika tidak.<br><br>
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Field pemilik, lokasi, ruang, dan mitra saat ini disembunyikan ({@code setVisible(false)}).
	 * Untuk mengaktifkannya, hapus baris tersebut. Field satuan kerja di-set ke satuan
	 * kerja pengguna aktif dan dibuat read-only; jika perlu bisa diubah oleh pengguna,
	 * hapus {@code setReadonly(true)}.
	 *
	 * @deprecated Anotasi {@code @SuppressWarnings("deprecation")} diterapkan karena
	 *             penggunaan API ZK yang sudah deprecated di versi ini.
	 */
	@SuppressWarnings("deprecation")
	@Override
	public MyGrid form(GeneralValueObject generalValueObject, DisposisiSop disposisiSop,
			final MyToolbarbuttonConfig save, final EventListener setujuiData) throws Exception {

		if (satuanKerjaTreeModel == null) {
			satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);
		}
		this.pemakaianMasterAsset = (PemakaianMasterAsset) generalValueObject;
		this.disposisiSop = (this.disposisiSop != null && (disposisiSop == null || disposisiSop.getId() == null)) ? this.disposisiSop : disposisiSop;
		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setWidth("100%");
		grid.setHeight("100%");

		Tbmuser tbmuser = Common.getCurrentUser();

		pemakaianMasterAsset.setSatuanKerja(tbmuser == null ? null : tbmuser.ambilSatuanKerja());

		Rows rows = new Rows();
		rows.setParent(grid);


		if (pemakaianMasterAsset.getKode() == null) {
			String noAgenda = generateCode(false);
			pemakaianMasterAsset.setKode(noAgenda);
		}

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Pemakaian *"));
		kode = new Label(pemakaianMasterAsset.getKode());
		if (persetujuan) {
			row.appendChild(new Label(pemakaianMasterAsset.getKode()));
		} else {
			row.appendChild(kode);
		}

		kode.setWidth("90%");

		final MyFormRow rowSatker = new MyFormRow();
		rowSatker.setParent(rows);
		rowSatker.appendChild(new ais.ui.util.MyLabelConfig("Satuan Kerja *"));
		satuanKerja = new AmbilDataSatuanKerjaBanbox(true, false);
		satuanKerja.setValue(
				pemakaianMasterAsset.getSatuanKerja() == null ? "" : pemakaianMasterAsset.getSatuanKerja().getNama());
		satuanKerja.setAttribute("satuanKerja", pemakaianMasterAsset.getSatuanKerja());
		satuanKerja.setReadonly(true);
		if (persetujuan) {
			rowSatker.appendChild(new Label(pemakaianMasterAsset.getSatuanKerja() == null ? ""
					: pemakaianMasterAsset.getSatuanKerja().getNama()));
		} else {
			rowSatker.appendChild(satuanKerja);
		}
		satuanKerja.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Pemakaian"));
		tanggalPembuatan = new MyDatebox(
				pemakaianMasterAsset.getTanggalPembuatan() == null ? ais.ui.util.WaktuUtil.getDate()
						: pemakaianMasterAsset.getTanggalPembuatan());

		if (persetujuan) {
			row.appendChild(new Label(Common.dateFormat6.get()
					.format(pemakaianMasterAsset.getTanggalPembuatan() == null ? ais.ui.util.WaktuUtil.getDate()
							: pemakaianMasterAsset.getTanggalPembuatan())));
		} else {
			row.appendChild(tanggalPembuatan);
		}

		tanggalPembuatan.setFormat(Common.dateFormat.get().toPattern());
		tanggalPembuatan.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(false);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pemilik"));
		pemilikAsset = new Combobox();

		if (persetujuan) {
			row.appendChild(new Label(pemakaianMasterAsset.getPemilikAsset() == null ? ""
					: pemakaianMasterAsset.getPemilikAsset().getNama()));
		} else {
			row.appendChild(pemilikAsset);
		}

		Common.insertComboDanSemua(pemilikAsset, new String[] { "nama", "id" }, "keterangan", PemilikAsset.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.selectComboItem(pemilikAsset, pemakaianMasterAsset.getPemilikAsset());
		pemilikAsset.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(false);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Lokasi"));
		lokasi = new Combobox();

		if (persetujuan) {
			row.appendChild(new Label(
					pemakaianMasterAsset.getLokasi() == null ? "" : pemakaianMasterAsset.getLokasi().getNama()));
		} else {
			row.appendChild(lokasi);
		}

		Common.insertComboDanSemua(lokasi, new String[] { "nama" }, "alamat", Lokasi.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.selectComboItem(lokasi, pemakaianMasterAsset.getLokasi());
		lokasi.setWidth("90%");

		LokasiAction.kunciLokasi(lokasi);

		row = new MyFormRow();
		row.setVisible(false);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Ruang"));
		ruang = new AmbilDataRuangBanbox();

		if (persetujuan) {
			row.appendChild(new Label(
					pemakaianMasterAsset.getRuang() == null ? "" : pemakaianMasterAsset.getRuang().getNama()));
		} else {
			row.appendChild(ruang);
		}

		ruang.setValue(
				pemakaianMasterAsset.getRuang() == null ? "" : (pemakaianMasterAsset.getRuang().getKodeRuangan()));
		ruang.setAttribute("ruang", pemakaianMasterAsset.getRuang());
		ruang.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(false);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Mitra"));
		mitra = new Combobox();

		if (persetujuan) {
			row.appendChild(new Label(
					pemakaianMasterAsset.getMitra() == null ? "" : pemakaianMasterAsset.getMitra().getNama()));
		} else {
			row.appendChild(mitra);
		}

		Common.insertComboDanSemua(mitra, new String[] { "nama", "kode", "alamat" }, "keterangan", Mitra.class);
		Common.selectComboItem(mitra, pemakaianMasterAsset.getMitra());
		mitra.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		keterangan = new MyTextbox(
				pemakaianMasterAsset.getKeterangan() == null ? "" : pemakaianMasterAsset.getKeterangan());

		if (persetujuan) {
			row.appendChild(new Label(pemakaianMasterAsset.getKeterangan()));
		} else {
			row.appendChild(keterangan);
		}

		keterangan.setWidth("90%");
		keterangan.setRows(4);

		row = new MyFormRow();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.appendChild(new PemakaianPunyaMasterAssetHelper().initDetail(gridMasterAsset = new MyGrid(),
				pemakaianMasterAsset, satuanKerja, persetujuan));

		return grid;
	}

	/**
	 * <b>Tujuan:</b> Mempersiapkan window modal {@code addWindow} untuk operasi tambah
	 * atau ubah pemakaian aset: mengatur judul, membersihkan konten lama, membangun
	 * layout form baru, dan menambahkan tombol Batal dan Simpan di toolbar bawah.<br><br>
	 *
	 * <b>Cara kerja:</b><br>
	 * <ol>
	 *   <li>Menyimpan entitas yang diedit ke field {@code pemakaianMasterAsset}.</li>
	 *   <li>Mengatur judul window berdasarkan ada/tidaknya ID (tambah vs ubah).</li>
	 *   <li>Membersihkan konten window lama dengan {@code Common.clear(addWindow)}.</li>
	 *   <li>Membangun layout {@link Borderlayout} dengan area Center berisi form dari
	 *       {@code form()} dan area South berisi toolbar dengan dua tombol.</li>
	 *   <li>Tombol "Batal" menyembunyikan window tanpa menyimpan.</li>
	 *   <li>Tombol "Simpan" memanggil {@code onSave}, dan jika berhasil: memuat ulang
	 *       grid, meng-inisialisasi ulang paging, dan menyembunyikan window.</li>
	 * </ol>
	 *
	 * <b>Parameter:</b><br>
	 * @param pemakaianMasterAsset entitas yang akan diedit; jika {@code getId() == null}
	 *                              maka ini adalah operasi tambah data baru
	 * @throws Exception jika terjadi kesalahan membangun form atau komponen ZK
	 *
	 * <b>Penanganan error:</b><br>
	 * Exception dari {@code form()} merambat ke pemanggil. Exception dari event listener
	 * tombol simpan merambat ke ZK framework.<br><br>
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Method ini bersifat private dan hanya dipanggil oleh {@code onAdd} dan renderer
	 * baris (tombol ubah). Jika ada field form baru yang ditambahkan, cukup update
	 * method {@code form()} tanpa mengubah {@code init()}.
	 */
	private void init(PemakaianMasterAsset pemakaianMasterAsset) throws Exception {
		this.pemakaianMasterAsset = pemakaianMasterAsset;
		addWindow.setTitle(pemakaianMasterAsset.getId() == null ? "Tambah Pengadaan Aset" : "Ubah Pengadaan Aset");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		disposisiSop=null;center.appendChild(form(pemakaianMasterAsset, disposisiSop, save, null));

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("30px");
		toolbar.setParent(south);
		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				addWindow.setVisible(false);
			}
		});
		cancel.setParent(toolbar);

		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (onSave(event)) {
					onSearchDefault(null);
					Common.initPaging(paging, new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							onSearchDefault(null);
						}
					});
					addWindow.setVisible(false);
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);

	}

	/**
	 * <b>Tujuan:</b> Mengembalikan nama istilah/label yang digunakan oleh sistem SOP
	 * untuk mengidentifikasi jenis formulir ini dalam antarmuka alur kerja.<br><br>
	 *
	 * <b>Cara kerja:</b><br>
	 * Mengembalikan string literal {@code "Pemakaian Barang"} yang digunakan sebagai
	 * label oleh sistem SOP untuk menampilkan nama jenis dokumen.<br><br>
	 *
	 * <b>Return:</b><br>
	 * @return string {@code "Pemakaian Barang"} sebagai nama istilah formulir ini
	 * @throws Exception tidak melempar exception dalam implementasi ini
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Ubah string ini jika terminologi bisnis untuk pemakaian aset berubah.
	 * Perubahan ini akan tercermin di semua tampilan SOP yang menampilkan nama
	 * jenis formulir.
	 */
	@Override
	public String istilah() throws Exception {
		return "Pemakaian Barang";
	}

	/**
	 * <b>Tujuan:</b> Mengembalikan entitas {@link DataSop} (dalam hal ini
	 * {@link PemakaianMasterAsset}) yang sedang aktif dalam sesi form ini,
	 * digunakan oleh sistem SOP untuk mengakses data dokumen.<br><br>
	 *
	 * <b>Cara kerja:</b><br>
	 * Mengembalikan field {@code pemakaianMasterAsset} yang merupakan entitas
	 * aktif yang di-set oleh {@code form()} atau {@code init()}.<br><br>
	 *
	 * <b>Return:</b><br>
	 * @return {@link PemakaianMasterAsset} yang sedang aktif; dapat {@code null}
	 *         jika {@code form()} belum dipanggil
	 * @throws Exception tidak melempar exception dalam implementasi ini
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Method ini adalah delegator sederhana. Pastikan {@code form()} selalu dipanggil
	 * sebelum sistem SOP memanggil {@code ambil()}.
	 */
	@Override
	public DataSop ambil() throws Exception {
		return pemakaianMasterAsset;
	}

	/**
	 * <b>Tujuan:</b> Mengembalikan kelas Java yang merepresentasikan tipe entitas
	 * yang dikelola oleh form ini. Digunakan oleh sistem SOP untuk operasi refleksi
	 * dan query generik.<br><br>
	 *
	 * <b>Cara kerja:</b><br>
	 * Mengembalikan {@code PemakaianMasterAsset.class} secara langsung sebagai
	 * referensi kelas literal Java.<br><br>
	 *
	 * <b>Return:</b><br>
	 * @return {@code Class} dari {@link PemakaianMasterAsset}
	 * @throws Exception tidak melempar exception dalam implementasi ini
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Perubahan pada kelas entitas pemakaian harus disertai pembaruan method ini.
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public Class ambilClass() throws Exception {
		return PemakaianMasterAsset.class;
	}

	/**
	 * <b>Tujuan:</b> Memvalidasi input form, menyimpan atau memperbarui entitas
	 * {@link PemakaianMasterAsset} ke database beserta semua detail asetnya, dan
	 * memicu pencetakan laporan PDF secara asinkron.<br><br>
	 *
	 * <b>Cara kerja:</b><br>
	 * <ol>
	 *   <li>Validasi: kode tidak boleh kosong, satuan kerja wajib dipilih,
	 *       dan setiap baris detail harus memiliki {@code masterAsset} yang dipilih.</li>
	 *   <li>Mengambil sesi Hibernate saat ini.</li>
	 *   <li>Jika ID sudah ada (operasi ubah), memuat ulang entitas dari database
	 *       untuk menghindari masalah detached object.</li>
	 *   <li>Memetakan nilai-nilai dari komponen UI ke field entitas:
	 *       lokasi, pemilikAsset, ruang, kode, keterangan, tanggalPembuatan, mitra,
	 *       satuanKerja.</li>
	 *   <li>Jika operasi ubah: memanggil {@code Common.refreshUpdate}.
	 *       Jika operasi tambah: menetapkan pembuat, men-generate kode baru dengan
	 *       menaikkan counter ({@code generateCode(true)}), dan memanggil
	 *       {@code session.save} serta {@code session.flush}.</li>
	 *   <li>Menyimpan atau memperbarui setiap baris detail aset dari grid.</li>
	 *   <li>Menggunakan timer ZK untuk memicu pembuatan PDF laporan pemakaian.</li>
	 * </ol>
	 *
	 * <b>Parameter:</b><br>
	 * @param event event ZK dari onClick tombol simpan; tidak digunakan secara langsung
	 *              di dalam method ini
	 * @return {@code true} jika semua validasi lolos dan penyimpanan berhasil;
	 *         {@code false} jika validasi gagal (window tetap terbuka)
	 * @throws Exception jika terjadi kesalahan Hibernate atau komponen ZK
	 *
	 * <b>Penanganan error:</b><br>
	 * Kesalahan pengambilan satuan kerja dari atribut banbox ditangkap secara lokal
	 * (blok catch kosong); nilai satuan kerja mungkin null jika terjadi error ini.
	 * Kesalahan Hibernate lainnya merambat ke framework ZK.<br><br>
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Kode dokumen di-generate ulang pada operasi tambah untuk memastikan keunikan;
	 * pada operasi ubah, kode yang sudah ada dipertahankan. Jika field baru ditambahkan
	 * ke {@link PemakaianMasterAsset}, tambahkan pula pemetaan dari komponen UI ke
	 * field tersebut di method ini.
	 */
	@SuppressWarnings("unchecked")
	public boolean onSave(Event event) throws Exception {
		if (kode.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Kode Pemakaian belum diisi. Langkah yang dapat dilakukan: (1) Isi field Kode Pemakaian atau gunakan tombol generate kode; (2) Pastikan kode bersifat unik dan belum digunakan sebelumnya; (3) ulangi proses simpan ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (satuanKerja.getAttribute("satuanKerja") == null) {
			MyMessageboxConfig.show("Mohon maaf, Satuan Kerja belum dipilih. Langkah yang dapat dilakukan: (1) Klik tombol pilih pada field Satuan Kerja; (2) Pilih satuan kerja yang melakukan pemakaian dari daftar; (3) ulangi proses simpan ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		List<Row> rowsMasterAsset = gridMasterAsset.getRows().getChildren();
		for (Row row : rowsMasterAsset) {
			PemakaianMasterAssetDetail pemakaianMasterAssetDetail = (PemakaianMasterAssetDetail) row
					.getAttribute("pemakaianMasterAssetDetail");
			if (pemakaianMasterAssetDetail.getMasterAsset() == null) {
				MyMessageboxConfig.show("Mohon maaf, Data Barang pada daftar pemakaian belum lengkap. Langkah yang dapat dilakukan: (1) Klik tombol pilih barang pada baris yang masih kosong; (2) Cari dan pilih barang/aset yang digunakan dari daftar; (3) ulangi proses simpan ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.EXCLAMATION);
				return false;
			}
		}

		Session session = HibernateUtil.currentSession();
		if (pemakaianMasterAsset.getId() != null) {
			pemakaianMasterAsset = (PemakaianMasterAsset) session.load(PemakaianMasterAsset.class,
					pemakaianMasterAsset.getId());
		}

		pemakaianMasterAsset.setLokasi(
				(Lokasi) (lokasi.getSelectedItem() == null || lokasi.getSelectedItem().getValue() == null ? null
						: lokasi.getSelectedItem().getValue()));
		pemakaianMasterAsset.setPemilikAsset((PemilikAsset) (pemilikAsset.getSelectedItem() == null
				|| pemilikAsset.getSelectedItem().getValue() == null ? null
						: pemilikAsset.getSelectedItem().getValue()));
		pemakaianMasterAsset.setRuang((Ruang) ruang.getAttribute("ruang"));
		pemakaianMasterAsset.setKode(kode.getValue());
		pemakaianMasterAsset.setKeterangan(keterangan.getValue());
		pemakaianMasterAsset.setTanggalPembuatan(tanggalPembuatan.getValue());
		pemakaianMasterAsset
				.setMitra((Mitra) (mitra.getSelectedItem() == null || mitra.getSelectedItem().getValue() == null ? null
						: mitra.getSelectedItem().getValue()));
		try {
			pemakaianMasterAsset.setSatuanKerja((SatuanKerja) satuanKerja.getAttribute("satuanKerja"));
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/asset/PemakaianMasterAssetAction.java:1217");
			// pengambilan satuan kerja gagal; nilai tetap null
		}

		if (pemakaianMasterAsset.getId() != null) {
			Common.refreshUpdate(session, pemakaianMasterAsset);
		} else {
			pemakaianMasterAsset.setDibuatOleh(Common.getCurrentUser());
			String noAgenda = generateCode(true);
			kode.setValue(noAgenda);
			pemakaianMasterAsset.setKode(kode.getValue());
			session.save(pemakaianMasterAsset);
			session.flush();
		}

		for (Row row : rowsMasterAsset) {
			PemakaianMasterAssetDetail pemakaianMasterAssetDetail = (PemakaianMasterAssetDetail) row
					.getAttribute("pemakaianMasterAssetDetail");
			pemakaianMasterAssetDetail.setPemakaianMasterAsset(pemakaianMasterAsset);
			Common.refreshSaveOrUpdate(session, pemakaianMasterAssetDetail);
		}

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Report.generatePDFReport(Report.PDF, parameter(pemakaianMasterAsset), "asset/pemakaian",
						pemakaianMasterAsset.getTanggalPembuatan());
			}
		});

		return true;
	}

	/**
	 * <b>Tujuan:</b> Membangun {@link Criteria} Hibernate yang mencerminkan semua
	 * kondisi filter aktif pada grid utama pemakaian aset.<br><br>
	 *
	 * <b>Cara kerja:</b><br>
	 * <ol>
	 *   <li>Mengambil satuan kerja yang dipilih pada {@code searchparent} dan membangun
	 *       set satuan kerja yang mencakup induk dan semua turunannya menggunakan
	 *       {@link SatuanKerjaTreeModel#getChildsSet}.</li>
	 *   <li>Menerapkan filter satuan kerja: jika set kosong, tidak ada filter; jika tidak,
	 *       memfilter berdasarkan satuan kerja yang ada di set (termasuk null untuk
	 *       data yang tidak punya satuan kerja).</li>
	 *   <li>Menerapkan filter status persetujuan berdasarkan checkbox:
	 *     <ul>
	 *       <li>Keduanya dicentang atau keduanya tidak: tampilkan semua.</li>
	 *       <li>Hanya belum disetujui: filter {@code disetujuiOleh IS NULL}.</li>
	 *       <li>Hanya sudah disetujui: filter {@code disetujuiOleh IS NOT NULL}.</li>
	 *     </ul>
	 *   </li>
	 *   <li>Menerapkan filter pemilik aset dan ruang dari komponen pencarian.</li>
	 *   <li>Menerapkan filter pencarian teks kode (ILIKE, case-insensitive).</li>
	 *   <li>Jika {@code order=true}, menambahkan urutan descending berdasarkan ID.</li>
	 * </ol>
	 *
	 * <b>Parameter:</b><br>
	 * @param order {@code true} untuk menambahkan ORDER BY id DESC;
	 *              {@code false} untuk digunakan dalam perhitungan jumlah total
	 * @return {@link Criteria} siap pakai untuk {@code .list()} atau {@code .setProjection(rowCount)}
	 *
	 * <b>Penanganan error:</b><br>
	 * Jika {@code blmDisetujui} atau {@code disetujui} null (komponen tidak tersedia
	 * di ZUL), filter persetujuan menggunakan {@code sqlRestriction("true")} sehingga
	 * semua data ditampilkan. Ini adalah perilaku aman (fail-open).<br><br>
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Method ini dipanggil dua kali dalam {@code onSearchDefault}: sekali untuk
	 * menghitung total baris (paging) dan sekali untuk mengambil data halaman aktif.
	 * Pastikan filter yang sama diterapkan pada keduanya.
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
		Criteria criteria = session.createCriteria(PemakaianMasterAsset.class)

				.add(satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.isNull("satuanKerja"),
								Restrictions.or(
										parent == null ? Restrictions.isNull("satuanKerja")
												: Restrictions.sqlRestriction("false"),
										Restrictions.in("satuanKerja", satuanKerjas))))

				.add(blmDisetujui == null || disetujui == null ? Restrictions.sqlRestriction("true") : blmDisetujui.isChecked() && disetujui.isChecked() ? Restrictions.sqlRestriction("true")
						: !blmDisetujui.isChecked() && !disetujui.isChecked() ? Restrictions.sqlRestriction("false")
								: blmDisetujui.isChecked() ? Restrictions.isNull("disetujuiOleh")
										: Restrictions.isNotNull("disetujuiOleh"))

				.add(searchpemilikAsset.getSelectedItem() == null
						|| searchpemilikAsset.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("pemilikAsset", searchpemilikAsset.getSelectedItem().getValue()))

				.add((searchruang == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchruang.getAttribute("ruang") == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("ruang", searchruang.getAttribute("ruang"))))
				.add(searchkode.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("kode", searchkode.getValue(), MatchMode.ANYWHERE));
		if (order)
			criteria.addOrder(Order.desc("id"));

		return criteria;
	}

	/**
	 * <b>Tujuan:</b> Mengeksekusi pencarian dan memperbarui tampilan grid pemakaian
	 * aset berdasarkan filter yang aktif dan halaman paging saat ini.<br><br>
	 *
	 * <b>Cara kerja:</b><br>
	 * <ol>
	 *   <li>Memanggil {@code Common.initPaging(initCriteria(false), paging)} untuk
	 *       menghitung total baris dan memperbarui komponen paging.</li>
	 *   <li>Memanggil {@code initCriteria(true)} dengan batasan baris per halaman
	 *       ({@code Common.ROWS_COUNT_ON_PAGE}) dan offset berdasarkan halaman aktif
	 *       paging untuk mengambil data halaman saat ini.</li>
	 *   <li>Membungkus hasil dalam {@link SimpleListModel} dan menetapkannya ke grid
	 *       dengan {@link PemakaianMasterAssetRenderer} sebagai renderer.</li>
	 * </ol>
	 *
	 * <b>Parameter:</b><br>
	 * @param event event ZK yang memicu pencarian; dapat {@code null} jika dipanggil
	 *              secara programatik
	 *
	 * <b>Penanganan error:</b><br>
	 * Exception dari query Hibernate merambat ke ZK framework.<br><br>
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Jumlah baris per halaman dikontrol oleh konstanta {@code Common.ROWS_COUNT_ON_PAGE}.
	 * Jika {@code paging == null} (komponen tidak ada di ZUL), offset default 0
	 * digunakan sehingga hanya halaman pertama yang ditampilkan.
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);
		List<PemakaianMasterAsset> pemakaianMasterAsset = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(pemakaianMasterAsset);
		grid.setRowRenderer(new PemakaianMasterAssetRenderer());
		grid.setModelCheckMobile(strset);

	}

	/**
	 * <b>Tujuan:</b> Mengatur flag mode persetujuan setelah instance dibuat, memungkinkan
	 * pengubahan mode tampilan form tanpa membuat instance baru. Implementasi dari
	 * interface {@link FormSop}.<br><br>
	 *
	 * <b>Cara kerja:</b><br>
	 * Menetapkan field {@code persetujuan} ke nilai yang diberikan. Nilai ini
	 * akan digunakan oleh {@code form()} pada pemanggilan berikutnya untuk
	 * menentukan apakah komponen input atau label statis yang ditampilkan.<br><br>
	 *
	 * <b>Parameter:</b><br>
	 * @param persetujuan {@code true} untuk mode read-only (persetujuan SOP);
	 *                    {@code false} untuk mode edit penuh
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Perubahan flag ini tidak langsung mengubah tampilan form yang sudah dirender;
	 * {@code form()} harus dipanggil ulang untuk menerapkan perubahan.
	 */
	@Override
	public void setPersetujuan(boolean persetujuan) {
		this.persetujuan = persetujuan;
	}

	/**
	 * <b>Tujuan:</b> Membangun map parameter yang dibutuhkan oleh template laporan
	 * JasperReports untuk menghasilkan dokumen PDF pemakaian aset.<br><br>
	 *
	 * <b>Cara kerja:</b><br>
	 * <ol>
	 *   <li>Membuat map kosong menggunakan {@code HashMapGenerator.getRand()} yang
	 *       menghasilkan map dengan key acak untuk menghindari collision cache.</li>
	 *   <li>Menambahkan {@code id} dari entitas pemakaian sebagai parameter langsung.</li>
	 *   <li>Menggunakan {@code Common.insertProperty} untuk merefleksikan semua
	 *       getter dari {@link PemakaianMasterAsset} ke dalam map sebagai parameter
	 *       dengan prefix kosong.</li>
	 *   <li>Mengambil semua baris detail aset, diurutkan berdasarkan nama aset,
	 *       dan untuk setiap detail membuat sub-map yang berisi: semua property
	 *       detail, nama aset, merk, satuan, kelompok aset, jumlah, tanggal persetujuan,
	 *       status persetujuan, dan kode pemakaian.</li>
	 *   <li>List sub-map disimpan dengan key {@code "maps"} di map utama.</li>
	 * </ol>
	 *
	 * <b>Parameter:</b><br>
	 * @param pemakaianMasterAsset entitas pemakaian aset yang akan dicetak
	 * @return {@link Map} berisi semua parameter yang dibutuhkan template laporan
	 *
	 * <b>Penanganan error:</b><br>
	 * Jika {@code getSatuanMasterAsset()} atau {@code getKelompokAsset()} null,
	 * nilai string kosong digunakan sebagai fallback agar laporan tetap bisa
	 * dihasilkan tanpa NullPointerException.<br><br>
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Query detail menggunakan {@code HibernateUtil.currentSession()} tanpa filter
	 * tambahan selain join ke masterAsset; jika ada kebutuhan filter tambahan
	 * (misalnya hanya detail aktif), tambahkan restriction di sini.
	 * Nama key parameter harus konsisten dengan nama field di template JasperReports.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map parameter(PemakaianMasterAsset pemakaianMasterAsset) {

		Map parameters = ais.common.HashMapGenerator.getRand();
		parameters.put("id", pemakaianMasterAsset.getId());

		Common.insertProperty(PemakaianMasterAsset.class, pemakaianMasterAsset, parameters, "");

		List maps = new ArrayList();
		List<PemakaianMasterAssetDetail> pemakaianMasterAssetDetails = HibernateUtil.currentSession()
				.createCriteria(PemakaianMasterAssetDetail.class).createAlias("masterAsset", "masterAsset")
				.addOrder(Order.asc("masterAsset.nama")).list();

		for (PemakaianMasterAssetDetail pemakaianMasterAssetDetail : pemakaianMasterAssetDetails) {
			Map map = new HashMap();
			Common.insertProperty(PemakaianMasterAssetDetail.class, pemakaianMasterAssetDetail, map, "data", 2,
					"pemakaianMasterAsset");
			map.put("merk", pemakaianMasterAssetDetail.getMasterAsset().getMerk());
			map.put("nama", pemakaianMasterAssetDetail.getMasterAsset().getNama());
			map.put("satuan", pemakaianMasterAssetDetail.getMasterAsset().getSatuanMasterAsset() == null ? ""
					: pemakaianMasterAssetDetail.getMasterAsset().getSatuanMasterAsset().getNama());
			map.put("kelompok_asset", pemakaianMasterAssetDetail.getMasterAsset().getKelompokAsset() == null ? ""
					: pemakaianMasterAssetDetail.getMasterAsset().getKelompokAsset().getNama());
			map.put("jumlah", pemakaianMasterAssetDetail.getJumlah());

			map.put("tanggal_persetujuan", pemakaianMasterAsset.getTanggalPersetujuan());
			map.put("status_persetujuan", pemakaianMasterAsset.getDisetujuiOleh() == null ? "Belum disetujui"
					: "Disetujui oleh " + pemakaianMasterAsset.getDisetujuiOleh().getUserNama());
			map.put("kode", pemakaianMasterAsset.getKode());
			maps.add(map);
		}
		parameters.put("maps", maps);
		return parameters;
	}

	/**
	 * <b>Tujuan:</b> Menghasilkan file PDF laporan pemakaian aset yang dapat diunduh
	 * atau dikirim, digunakan oleh sistem SOP sebagai implementasi interface
	 * {@link FormSop#cetakData}.<br><br>
	 *
	 * <b>Cara kerja:</b><br>
	 * Mendelegasikan ke {@code Report.generateFileReport} dengan template
	 * {@code "asset/pemakaian"}, menggunakan map parameter dari {@code parameter()},
	 * tanggal pemakaian sebagai konteks tanggal laporan, dan locale default sistem.<br><br>
	 *
	 * <b>Parameter:</b><br>
	 * @param generalValueObject entitas pemakaian; harus berupa {@link PemakaianMasterAsset}
	 * @return {@link File} PDF yang dihasilkan; tidak null jika berhasil
	 * @throws Exception jika template laporan tidak ditemukan, terjadi error JasperReports,
	 *                   atau masalah I/O pada penulisan file
	 *
	 * <b>Pemeliharaan:</b><br>
	 * File yang dihasilkan disimpan sementara; pastikan direktori temp memiliki ruang
	 * yang cukup. Template JasperReports harus tersedia di path {@code asset/pemakaian}
	 * sesuai konfigurasi {@link Report}.
	 */
	@SuppressWarnings({})
	@Override
	public File cetakData(GeneralValueObject generalValueObject) throws Exception {
		PemakaianMasterAsset pemakaianMasterAsset = (PemakaianMasterAsset) generalValueObject;

		return Report.generateFileReport(Report.PDF, parameter(pemakaianMasterAsset), "asset/pemakaian",
				pemakaianMasterAsset.getTanggalPembuatan(), Common.locale);
	}

	/**
	 * <b>Tujuan:</b> Wrapper instance method yang mendelegasikan ke versi statis
	 * {@code generateCode(boolean, Date)}, mengambil tanggal dari komponen
	 * {@code tanggalPembuatan} yang ter-wire ke UI.<br><br>
	 *
	 * <b>Cara kerja:</b><br>
	 * Jika {@code tanggalPembuatan} null (komponen belum ter-wire atau belum diisi),
	 * menggunakan tanggal hari ini dari {@link WaktuUtil#getDate()}. Kemudian
	 * mendelegasikan ke {@code generateCode(tambah, tanggal)} statis.<br><br>
	 *
	 * <b>Parameter:</b><br>
	 * @param tambah {@code true} untuk menaikkan counter nomor surat (saat menyimpan
	 *               dokumen baru); {@code false} hanya untuk preview kode tanpa
	 *               menaikkan counter
	 * @return kode pemakaian yang di-generate, dijamin unik
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Method ini hanya boleh dipanggil setelah {@code form()} selesai membangun UI
	 * agar {@code tanggalPembuatan} sudah ter-wire. Panggil versi statis langsung
	 * jika tanggal sudah diketahui.
	 */
	public String generateCode(boolean tambah) {
		return generateCode(tambah, tanggalPembuatan == null ? WaktuUtil.getDate() : tanggalPembuatan.getValue());
	}

	/**
	 * <b>Tujuan:</b> Men-generate kode dokumen pemakaian aset berdasarkan template
	 * nomor surat yang dikonfigurasi di {@link NomorSuratAlurPengadaan#PEMAKAIAN_BARANG_DATA},
	 * memastikan kode yang dihasilkan unik di seluruh tabel pemakaian.<br><br>
	 *
	 * <b>Cara kerja:</b><br>
	 * <ol>
	 *   <li>Jika konfigurasi nomor surat belum ada ({@code PEMAKAIAN_BARANG_DATA == null}
	 *       atau {@code getNomorSurat() == null}), mengembalikan barcode acak dari
	 *       {@code Common.getGeneratedBarCode()} sebagai fallback.</li>
	 *   <li>Menentukan index urutan: jika konfigurasi menggunakan index urut sequential
	 *       ({@code getGunakanIndexUrut() == true}), mengambil {@code getNomorIndex()};
	 *       jika tidak, menghitung dari database melalui {@code getindex()}.</li>
	 *   <li>Jika {@code tambah == true}, menaikkan index nomor surat melalui
	 *       {@code NomorSurat.tambahIndexNomorSurat()} untuk keperluan dokumen berikutnya.</li>
	 *   <li>Memformat kode menggunakan {@code NomorSurat.format(index, tanggalPembuatan)}
	 *       yang mengaplikasikan template nomor surat dengan substitusi variabel
	 *       tanggal dan index.</li>
	 *   <li>Memastikan keunikan kode melalui {@code KodeUnikUtil.pastikanUnik()} yang
	 *       akan menambahkan sufiks jika kode sudah dipakai.</li>
	 * </ol>
	 *
	 * <b>Parameter:</b><br>
	 * @param tambah {@code true} untuk menaikkan counter nomor surat setelah generate
	 *               (gunakan saat benar-benar menyimpan dokumen baru);
	 *               {@code false} untuk hanya preview kode tanpa side effect
	 * @param tanggalPembuatan tanggal yang digunakan sebagai konteks substitusi
	 *                          variabel tanggal dalam template nomor surat
	 * @return string kode pemakaian yang sudah diformat dan dijamin unik
	 *
	 * <b>Penanganan error:</b><br>
	 * Tidak melempar exception; jika konfigurasi tidak valid, fallback ke barcode acak.<br><br>
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Method ini bersifat statis agar dapat dipanggil dari konteks non-instance
	 * (misalnya import massal). Pastikan konfigurasi {@code PEMAKAIAN_BARANG_DATA}
	 * sudah diisi di database sebelum modul digunakan.
	 */
	public static String generateCode(boolean tambah, Date tanggalPembuatan) {
		if (NomorSuratAlurPengadaan.PEMAKAIAN_BARANG_DATA == null
				|| NomorSuratAlurPengadaan.PEMAKAIAN_BARANG_DATA.getNomorSurat() == null) {
			return Common.getGeneratedBarCode();
		}

		Long index = NomorSuratAlurPengadaan.PEMAKAIAN_BARANG_DATA.getNomorSurat().getGunakanIndexUrut()
				? NomorSuratAlurPengadaan.PEMAKAIAN_BARANG_DATA.getNomorSurat().getNomorIndex()
				: getindex(NomorSuratAlurPengadaan.PEMAKAIAN_BARANG_DATA.getNomorSurat());
		if (tambah) {
			NomorSurat.tambahIndexNomorSurat(NomorSuratAlurPengadaan.PEMAKAIAN_BARANG_DATA.getNomorSurat());
		}
		String noAgenda = NomorSuratAlurPengadaan.PEMAKAIAN_BARANG_DATA.getNomorSurat().format(index, tanggalPembuatan);
		return ais.action.master.KodeUnikUtil.pastikanUnik(PemakaianMasterAsset.class, noAgenda);
	}

	/**
	 * <b>Tujuan:</b> Menghitung index urutan berikutnya untuk penomoran dokumen pemakaian
	 * aset berdasarkan jumlah dokumen yang sudah ada, dengan mempertimbangkan berbagai
	 * aturan urutan: per nomor surat, per kelompok, per tahun, per bulan, atau sejak
	 * tanggal reset tertentu.<br><br>
	 *
	 * <b>Cara kerja:</b><br>
	 * <ol>
	 *   <li>Jika {@code nomorSurat == null}, mengembalikan 0 sebagai index awal.</li>
	 *   <li>Membuat {@link Criteria} Hibernate ke tabel {@link PemakaianMasterAsset}
	 *       dengan LEFT JOIN ke {@code nomorSuratAlurPengadaan} dan {@code nomorSurat}.</li>
	 *   <li>Menerapkan filter berdasarkan konfigurasi nomor surat:
	 *     <ul>
	 *       <li>{@code getUrutBerdasarkanNomor()}: filter nomor surat spesifik.</li>
	 *       <li>{@code getUrutBerdasarkanKelompok()}: filter kelompok nomor surat.</li>
	 *       <li>Default: tidak ada filter (semua dokumen dihitung).</li>
	 *     </ul>
	 *   </li>
	 *   <li>Menerapkan filter periode:
	 *     <ul>
	 *       <li>{@code getResetUrutanTiapTahun()}: hanya hitung dokumen tahun ini.</li>
	 *       <li>{@code getResetUrutanTiapBulan()}: hanya hitung dokumen bulan ini.</li>
	 *       <li>{@code getResetTiap()}: hanya hitung dokumen sejak tanggal reset.</li>
	 *     </ul>
	 *   </li>
	 *   <li>Menggunakan {@code Projections.rowCount()} untuk mendapatkan jumlah baris.</li>
	 *   <li>Menambahkan 1 ke jumlah yang ditemukan untuk mendapatkan index berikutnya.</li>
	 * </ol>
	 *
	 * <b>Parameter:</b><br>
	 * @param nomorSurat konfigurasi nomor surat yang menentukan aturan penomoran;
	 *                   {@code null} menghasilkan return 0
	 * @return index urutan berikutnya (selalu &ge; 1); dimulai dari 1 untuk dokumen pertama
	 *
	 * <b>Penanganan error:</b><br>
	 * Jika {@code uniqueResult()} mengembalikan null (tidak ada data), index 0 digunakan
	 * dan dikembalikan sebagai 1. Exception Hibernate merambat ke pemanggil.<br><br>
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Method ini menggunakan waktu server ({@code WaktuUtil.getCalendar()}) untuk
	 * menentukan tahun dan bulan saat ini. Pastikan zona waktu server sesuai dengan
	 * kebijakan penomoran lembaga. Jika ada aturan penomoran baru, tambahkan
	 * kondisi filter di sini sesuai dengan konfigurasi {@link NomorSurat}.
	 */
	public static Long getindex(NomorSurat nomorSurat) {
		if (nomorSurat == null) {
			return 0L;
		}
		Session session = HibernateUtil.currentSession();
		int tahun = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
		int bulan = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.MONTH) + 1;
		Date sekarang = WaktuUtil.getDate();
		Number indexO = (Number) session.createCriteria(PemakaianMasterAsset.class)
				.createAlias("nomorSuratAlurPengadaan", "nomorSuratAlurPengadaan", Criteria.LEFT_JOIN)
				.createAlias("nomorSuratAlurPengadaan.nomorSurat", "nomorSurat", Criteria.LEFT_JOIN)

				.add(nomorSurat.getUrutBerdasarkanNomor()
						? Restrictions.eq("nomorSuratAlurPengadaan.nomorSurat", nomorSurat)

						: (nomorSurat.getUrutBerdasarkanKelompok() && nomorSurat.getKelompokNomorSurat() != null
								? Restrictions.eq("nomorSurat.kelompokNomorSurat", nomorSurat.getKelompokNomorSurat())
								: Restrictions.sqlRestriction("true")))

				.add(nomorSurat.getResetUrutanTiapTahun() ? Restrictions.eq("tahun", tahun)
						: Restrictions.sqlRestriction("true"))

				.add(nomorSurat.getResetUrutanTiapBulan()
						? Restrictions.and(Restrictions.eq("tahun", tahun), Restrictions.eq("bulan", bulan))
						: Restrictions.sqlRestriction("true"))

				.add(nomorSurat.getResetTiap() != null && (Common.dateFormat8.get().format(nomorSurat.getResetTiap())
						.equals(Common.dateFormat8.get().format(sekarang)) || nomorSurat.getResetTiap().before(sekarang))
								? Restrictions.ge("tanggalPembuatan", nomorSurat.getResetTiap())
								: Restrictions.sqlRestriction("true"))

				.setProjection(Projections.rowCount()).uniqueResult();

		Long index = indexO == null ? null : indexO.longValue();
		if (index == null) {
			index = 0L;
		}
		return ++index;
	}

}
