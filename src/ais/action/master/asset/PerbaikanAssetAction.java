package ais.action.master.asset;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.asset.helper.AmbilDataAssetDetailBanbox;
import ais.action.master.helper.BroadcastHelper;
import ais.action.master.helper.ParameterTambahanPerbaikanAssetListener;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.generic.AmbilDataTbmuserBanyak;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.action.master.sop.TampilanAlurSopAction;
import ais.action.report.Report;
import ais.action.report.helper.asset.LaporanPerbaikanAsset;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.common.UIClassHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.ParameterTambahan;
import ais.database.model.Tbmuser;
import ais.database.model.asset.AssetDetail;
import ais.database.model.asset.JenisPerbaikanAsset;
import ais.database.model.asset.KelompokParameterTambahanPerbaikanAsset;
import ais.database.model.asset.ParameterTambahanPerbaikanAsset;
import ais.database.model.asset.PerbaikanAsset;
import ais.database.model.file.LampiranLain;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.sop.DataSop;
import ais.database.model.sop.DisposisiSop;
import ais.database.model.surat.NomorSurat;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.FormSop;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyLabelKecilBold;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;

/**
 * <h3>PerbaikanAssetAction — Manajemen Work Order Perbaikan dan Pemeliharaan Aset/Fasilitas</h3>
 *
 * <b>Untuk apa:</b><br>
 * Kelas ini adalah Action (Composer ZK) yang mengelola seluruh siklus hidup Work Order Perbaikan
 * Aset atau Fasilitas dalam sistem informasi aset. Fungsinya mencakup pencatatan pengajuan
 * perbaikan, manajemen parameter tambahan yang dinamis sesuai jenis perbaikan, penerbitan nomor
 * agenda otomatis berdasarkan konfigurasi NomorSurat, cetak laporan perbaikan, penyebaran
 * notifikasi broadcast ke pengguna terdaftar, dan integrasi dengan sistem SOP alur persetujuan.
 * Halaman ini dapat diakses dari tab "Perbaikan Aset" pada modul manajemen aset.
 *
 * <b>Cara kerja:</b><br>
 * Setelah halaman ZUL dimuat, {@link #doAfterCompose(Component)} menginisialisasi searchparent
 * (filter satuan kerja), tree model satuan kerja, tombol tambah/cetak/upload, hak akses, dan
 * memuat data grid awal secara asinkron melalui timer. Grid menampilkan daftar {@link PerbaikanAsset}
 * yang dirender oleh inner class {@link PerbaikanAssetRenderer}, menampilkan kode agenda, jenis
 * perbaikan, detail aset, parameter tambahan dinamis, tautan SOP, status aktif, dan tombol aksi.
 * Form tambah/ubah dibuat secara programatik di {@link #form(GeneralValueObject, DisposisiSop,
 * MyToolbarbuttonConfig, EventListener)} yang mengisi parameter tambahan secara dinamis berdasarkan
 * jenis perbaikan yang dipilih. Nomor agenda dibangkitkan oleh {@link #generateCode(JenisPerbaikanAsset, boolean)}
 * menggunakan konfigurasi {@link NomorSurat} yang terhubung ke jenis perbaikan. Setelah simpan,
 * laporan PDF dicetak otomatis dan data opsional dikirim ke endpoint eksternal via HTTP POST (curl).
 *
 * <b>Threading:</b><br>
 * Pengiriman notifikasi broadcast email dan POST ke endpoint eksternal berjalan di thread terpisah
 * ({@code new Thread(Runnable).start()}) agar tidak memblokir event thread ZK. Operasi simpan
 * lampiran file menggunakan sesi streaming Hibernate terpisah ({@link StreamingHibernateUtil})
 * karena file BLOB dikelola oleh session factory yang berbeda. Pastikan tidak mengakses komponen
 * ZK dari dalam thread latar belakang ini secara langsung.
 *
 * <b>Pemeliharaan:</b><br>
 * Kelas ini mengimplementasi empat interface: {@link DataCriteria}, {@link DataSearchDefault},
 * {@link DataInitDefault}, dan {@link FormSop}. Implementasi FormSop memungkinkan integrasi
 * dengan alur SOP (Standar Operasional Prosedur) untuk persetujuan perbaikan. Parameter tambahan
 * dinamis dikelola melalui {@link ParameterTambahanPerbaikanAssetListener} yang bertanggung jawab
 * merender field input sesuai tipe parameter dan menyimpan nilainya. Konfigurasi endpoint eksternal
 * dibaca dari tabel konfigurasi dengan kunci "link_post_perbaikan-asset"; kosongkan untuk
 * menonaktifkan integrasi ini. Jika performa grid lambat, pertimbangkan untuk menambahkan
 * indeks pada kolom kode dan keterangan di tabel perbaikan_asset.
 */
public class PerbaikanAssetAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault, FormSop {

	/**
	 * Serial version UID untuk serialisasi kelas ini.
	 * Nilai ini dibangkitkan otomatis dan tidak boleh diubah tanpa kebutuhan yang jelas.
	 */
	private static final long serialVersionUID = -5779730267402400328L;

	/** Jendela modal yang digunakan untuk form tambah dan ubah data perbaikan aset. */
	private MyWindow addWindow;

	/** Komponen paging untuk navigasi halaman pada grid daftar perbaikan aset. */
	private Paging paging;

	/** Grid utama yang menampilkan daftar work order perbaikan aset. */
	private MyGrid grid;

	/** Textbox untuk filter pencarian berdasarkan kode nomor agenda perbaikan. */
	private Textbox searchkode;

	/** Textbox untuk filter pencarian berdasarkan keterangan atau nama perbaikan. */
	private Textbox searchnama;

	/** Banbox untuk filter berdasarkan satuan kerja/unit yang mengajukan perbaikan. */
	private AmbilDataSatuanKerjaBanbox searchparent;

	/** Flag yang menandakan apakah pengguna memiliki hak akses UPDATE (ubah data). */
	private boolean edit = false;

	/** Flag yang menandakan apakah pengguna memiliki hak akses DELETE (hapus data). */
	private boolean delete = false;

	/** Entitas perbaikan aset yang sedang dalam proses tambah atau ubah di form. */
	private PerbaikanAsset perbaikanAsset;

	/** Tombol toolbar "Tambah" untuk membuka form tambah data perbaikan aset baru. */
	private MyToolbarbuttonConfig add;

	/** Datebox untuk input tanggal dan waktu pengajuan perbaikan pada form. */
	private MyDatebox waktu;

	/** Banbox untuk memilih satuan kerja yang bertanggung jawab atas perbaikan. */
	private AmbilDataSatuanKerjaBanbox satuanKerja;

	/** Combobox untuk memilih jenis perbaikan yang menentukan parameter tambahan dinamis. */
	private Combobox jenisPerbaikanAsset;

	/** Tab panel yang memuat halaman manajemen jenis perbaikan aset (lazy load). */
	private Tabpanel tabJenisPerbaikanAsset;

	/** Tab panel yang memuat halaman manajemen parameter tambahan perbaikan (lazy load). */
	private Tabpanel tabManajemenParameter;

	/** Tab panel yang memuat halaman laporan perbaikan aset (lazy load). */
	private Tabpanel tabLaporan;

	/** Daftar baris parameter tambahan yang dirender secara dinamis di form. */
	private ArrayList<Row> parameterRows;

	/** Peta lampiran file yang diupload, dengan kunci berupa kombinasi kelompok dan parameter id. */
	private HashMap<String, LampiranLain> lampiranLains;

	/** Listener yang mengelola rendering dan penyimpanan parameter tambahan dinamis. */
	private ParameterTambahanPerbaikanAssetListener parameterTambahanListener;

	/** Textbox untuk input nama/judul perbaikan pada form. */
	private MyTextbox nama;

	/** Checkbox untuk menandai apakah notifikasi perbaikan disebarkan ke pengguna tertentu. */
	private MyCheckboxConfig broadcast;

	/** Textbox untuk daftar username penerima notifikasi broadcast, dipisah koma. */
	private Textbox keterangan;

	/** Disposisi SOP yang terhubung ke work order perbaikan ini, null jika tidak via SOP. */
	private DisposisiSop disposisiSop;

	/** Label yang menampilkan nomor agenda yang dibangkitkan secara otomatis. */
	private Label kode;

	/** Banbox untuk memilih aset detail (sarpras) yang akan diperbaiki. */
	private AmbilDataAssetDetailBanbox assetDetail;

	/** Checkbox filter untuk menampilkan hanya perbaikan yang belum disetujui. */
	private MyCheckboxConfig blmDisetujui;

	/** Checkbox filter untuk menampilkan hanya perbaikan yang sudah disetujui. */
	private MyCheckboxConfig disetujui;

	/**
	 * <h3>onLaporan — Lazy Load Tab Laporan Perbaikan Aset</h3>
	 *
	 * <b>Tujuan:</b><br>
	 * Memuat konten tab laporan perbaikan aset secara lazy (hanya saat tab pertama kali dibuka).
	 * Pola lazy load ini digunakan untuk mengoptimalkan waktu muat halaman awal.
	 *
	 * <b>Cara kerja:</b><br>
	 * Memeriksa apakah tab laporan sudah memiliki anak (children). Jika belum, membuat instance
	 * {@link LaporanPerbaikanAsset} dan memasangnya ke tab panel dengan ukuran penuh (100%).
	 * Jika tab sudah pernah dimuat sebelumnya, tidak ada tindakan yang dilakukan.
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Jika komponen laporan perlu di-refresh setiap kali tab dibuka, hapus pengecekan
	 * {@code getChildren().size() == 0} dan buat instance baru setiap kali.
	 *
	 * @param event event ZK yang dipicu ketika tab Laporan diklik atau diaktifkan
	 */
	public void onLaporan(Event event) {
		if (tabLaporan.getChildren().size() == 0) {
			LaporanPerbaikanAsset window = new LaporanPerbaikanAsset();
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(tabLaporan);
		}
	}

	/**
	 * <h3>onJenisPerbaikanAsset — Lazy Load Tab Manajemen Jenis Perbaikan Aset</h3>
	 *
	 * <b>Tujuan:</b><br>
	 * Memuat konten tab manajemen jenis perbaikan aset secara lazy (hanya saat tab pertama kali
	 * dibuka). Konten diambil dari halaman ZUL yang sudah ada untuk manajemen master jenis perbaikan.
	 *
	 * <b>Cara kerja:</b><br>
	 * Memeriksa jumlah anak tab panel. Jika nol, membuat MyWindow tanpa judul dan tanpa dekorasi
	 * (mode "none"), kemudian memasang MyInclude yang memuat file ZUL jenis_perbaikan_asset.zul.
	 * Pendekatan ini menggunakan iframe ZK (MyInclude) untuk mengisolasi scope composer.
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Path ZUL "/pages/master/asset/jenis_perbaikan_asset.zul" harus tersedia dan dapat diakses.
	 * Jika path berubah, perbarui di sini.
	 *
	 * @param event event ZK yang dipicu ketika tab Jenis Perbaikan diklik atau diaktifkan
	 */
	public void onJenisPerbaikanAsset(Event event) {
		if (tabJenisPerbaikanAsset.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(tabJenisPerbaikanAsset);
			MyInclude iframe = new MyInclude("/pages/master/asset/jenis_perbaikan_asset.zul");
			iframe.setParent(window);
		}
	}

	/**
	 * <h3>onManajemenParameter — Lazy Load Tab Manajemen Parameter Tambahan Perbaikan</h3>
	 *
	 * <b>Tujuan:</b><br>
	 * Memuat konten tab manajemen parameter tambahan perbaikan aset secara lazy. Tab ini
	 * menampilkan halaman untuk mengkonfigurasi parameter-parameter tambahan yang dapat
	 * diisi saat membuat work order perbaikan, seperti deskripsi kerusakan, biaya estimasi, dll.
	 *
	 * <b>Cara kerja:</b><br>
	 * Sama dengan pola lazy load di onJenisPerbaikanAsset, namun memuat file ZUL
	 * parameter_tambahan_perbaikan_asset.zul ke dalam MyWindow tanpa dekorasi.
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Path ZUL "/pages/master/asset/parameter_tambahan_perbaikan_asset.zul" harus tersedia.
	 * Pastikan akses keamanan halaman tersebut juga dikonfigurasi dengan benar.
	 *
	 * @param event event ZK yang dipicu ketika tab Manajemen Parameter diklik atau diaktifkan
	 */
	public void onManajemenParameter(Event event) {
		if (tabManajemenParameter.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(tabManajemenParameter);
			MyInclude iframe = new MyInclude("/pages/master/asset/parameter_tambahan_perbaikan_asset.zul");
			iframe.setParent(window);
		}
	}

	/**
	 * <h3>doBeforeCompose — Pemeriksaan Keamanan Sebelum Halaman Dimuat</h3>
	 *
	 * <b>Tujuan:</b><br>
	 * Memverifikasi bahwa pengguna yang mengakses halaman ini telah terotentikasi dan
	 * berwenang. Dipanggil oleh framework ZK sebelum proses wiring komponen dimulai.
	 *
	 * <b>Cara kerja:</b><br>
	 * Memanggil {@link Common#doCheckSecurity()} yang memeriksa sesi dan hak akses.
	 * Jika gagal, pengguna diarahkan keluar. Kemudian mendelegasikan ke superclass.
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Standar keamanan yang harus ada di semua halaman Action AIS. Jangan hapus.
	 *
	 * @param page     halaman ZK yang sedang dikomposisi
	 * @param parent   komponen induk
	 * @param compInfo informasi metadata komponen dari ZUL
	 * @return ComponentInfo dari superclass untuk melanjutkan proses wiring
	 */
	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	/**
	 * <h3>doAfterCompose — Inisialisasi Halaman Perbaikan Aset Setelah Wiring Selesai</h3>
	 *
	 * <b>Tujuan:</b><br>
	 * Menginisialisasi seluruh state halaman setelah komponen ZUL berhasil di-wire ke field-field
	 * kelas ini. Meliputi pengaturan listener filter satuan kerja, tree model, tombol tambah,
	 * hak akses, paging, tombol cetak, tombol upload, dan pemuatan data grid awal.
	 *
	 * <b>Cara kerja:</b><br>
	 * Pertama mengatur event listener pada searchparent agar setiap perubahan satuan kerja
	 * memicu pencarian ulang. Kemudian menginisialisasi tree model satuan kerja untuk filter
	 * hierarki. Tombol "Tambah" hanya ditampilkan jika pengguna memiliki hak CREATE. Hak UPDATE
	 * dan DELETE disimpan ke flag instance. Paging dikonfigurasi dengan listener. Tombol cetak
	 * dan upload diinisialisasi menggunakan helper Common. Data grid dimuat pertama kali melalui
	 * timer default untuk menghindari pemblokiran event thread inisialisasi.
	 *
	 * <b>Parameter:</b><br>
	 * @param comp komponen root ZUL, digunakan oleh Common untuk menambahkan tombol ke toolbar
	 *
	 * <b>Penanganan error:</b><br>
	 * Exception dari superclass dilempar ke atas. Tidak ada penanganan khusus di sini.
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Array {@code contents} mendefinisikan field-field yang diekspor saat cetak/upload.
	 * Perbarui array ini jika entitas PerbaikanAsset mendapat field baru yang perlu diekspor.
	 *
	 * @throws Exception jika terjadi error pada inisialisasi superclass atau komponen ZK
	 */
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		Common.initLaguage();

		searchparent.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});

		satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		String[] contents = new String[] { "id", "kode", "nama", "assetDetail", "waktu", "satuanKerja",
				"jenisPerbaikanAsset", "parameterTambahan", "parameterTambahanInds" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(PerbaikanAsset.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, PerbaikanAsset.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});
	}

	/**
	 * <h3>PerbaikanAssetRenderer — Renderer Baris Grid Daftar Perbaikan Aset</h3>
	 *
	 * <b>Untuk apa:</b><br>
	 * Inner class ini bertanggung jawab merender setiap baris pada grid halaman manajemen
	 * perbaikan aset. Setiap baris merepresentasikan satu work order {@link PerbaikanAsset}
	 * beserta detail parameter tambahan dinamisnya, status aktif, dan tombol aksi.
	 *
	 * <b>Cara kerja:</b><br>
	 * Mengimplementasi metode render dari MyRowRenderer. Untuk setiap entitas perbaikan:
	 * 1. Jika kode belum dibangkitkan, generate otomatis dan simpan ke basis data.
	 * 2. Tampilkan kode agenda dan tanggal waktu.
	 * 3. Tampilkan nama jenis perbaikan.
	 * 4. Tampilkan kode dan nama aset detail yang diperbaiki.
	 * 5. Refresh jenis perbaikan dari basis data dan tampilkan semua parameter tambahan
	 *    beserta nilainya dengan memanggil {@link ParameterTambahan#tampil}.
	 * 6. Tampilkan tautan SOP jika ada.
	 * 7. Tampilkan checkbox aktif atau label (tergantung hak akses).
	 * 8. Tampilkan tombol Ubah, Hapus, dan Cetak.
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Refresh sesi Hibernate pada jenis perbaikan diperlukan karena parameter tambahan menggunakan
	 * lazy loading. Jika terjadi LazyInitializationException, pastikan session masih aktif saat
	 * render dipanggil. Refresh ini berdampak pada performa — pertimbangkan batch fetch jika
	 * jumlah data besar.
	 */
	class PerbaikanAssetRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * <h3>render — Merender Satu Baris Data Perbaikan Aset ke Grid</h3>
		 *
		 * <b>Tujuan:</b><br>
		 * Mengisi komponen-komponen ZK pada satu baris grid dengan data lengkap dari satu entitas
		 * {@link PerbaikanAsset}, termasuk parameter tambahan dinamis dan tombol aksi.
		 *
		 * <b>Cara kerja:</b><br>
		 * Jika kode belum ada dan jenis perbaikan tersedia, generate kode baru dan simpan.
		 * Kemudian tampilkan semua data baris dalam urutan kolom: kode, jenis, aset, parameter
		 * tambahan (diambil dari string parameterTambahanInds yang ter-encode), status aktif,
		 * dan toolbar aksi berisi tombol ubah/hapus/cetak.
		 *
		 * <b>Penanganan error:</b><br>
		 * Setiap akses ke asosiasi yang mungkin null sudah menggunakan operator ternary untuk
		 * menampilkan string kosong. Refresh sesi dilakukan sebelum iterasi parameter tambahan.
		 *
		 * @param arg0 komponen Row ZK yang akan diisi dengan komponen child
		 * @param arg1 objek data baris, dicast ke {@link PerbaikanAsset}
		 * @throws Exception jika terjadi error pada operasi basis data atau pembuatan komponen
		 */
		@SuppressWarnings("unchecked")
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final PerbaikanAsset perbaikanAsset = (PerbaikanAsset) arg1;

			if ((perbaikanAsset.getKode() == null || perbaikanAsset.getKode().isEmpty())
					&& perbaikanAsset.getJenisPerbaikanAsset() != null) {
				String noAgenda = generateCode(perbaikanAsset.getJenisPerbaikanAsset(), true);
				perbaikanAsset.setKode(noAgenda);
				Long currentIndex = getindex(perbaikanAsset.getJenisPerbaikanAsset());
				perbaikanAsset.setIndex(++currentIndex);
				Common.refreshUpdate(perbaikanAsset);
			}

			Vbox a;
			(a = RevisiHelper.createNewRevisi(PerbaikanAsset.class, perbaikanAsset,
					Common.dateFormat5.get().format(perbaikanAsset.getWaktu()))).setParent(arg0);
			a.appendChild(new Label(perbaikanAsset.getKode()));

			Vbox vbox = new Vbox();
			vbox.setParent(arg0);
			new Label(perbaikanAsset.getJenisPerbaikanAsset() == null ? ""
					: perbaikanAsset.getJenisPerbaikanAsset().getNama()).setParent(vbox);

			vbox = new Vbox();
			vbox.setParent(arg0);
			new Label(perbaikanAsset.getAssetDetail() == null ? "" : perbaikanAsset.getAssetDetail().getKode())
					.setParent(vbox);
			new Label(perbaikanAsset.getAssetDetail() == null ? "" : perbaikanAsset.getAssetDetail().getNama())
					.setParent(vbox);

			JenisPerbaikanAsset j = perbaikanAsset.getJenisPerbaikanAsset();
			Session session = HibernateUtil.currentSession();
			session.refresh(j);

			Vbox vbox2 = new Vbox();
			vbox2.setParent(arg0);

			for (KelompokParameterTambahanPerbaikanAsset kelompokParameterTambahanPerbaikanAsset : j
					.getKelompokParameterTambahanPerbaikanAssets()) {

				List<ParameterTambahan> parameterTambahans = ConstantValues.simpleList(
						session.createCriteria(ParameterTambahanPerbaikanAsset.class)
								.add(Restrictions.eq("kelompokParameterTambahanPerbaikanAsset",
										kelompokParameterTambahanPerbaikanAsset))
								.createAlias("parameterTambahan", "parameterTambahan")
								.createAlias("kelompokParameterTambahanPerbaikanAsset",
										"kelompokParameterTambahanPerbaikanAsset")
								.add(Restrictions.eq("parameterTambahan.aktif", true))
								.add(Restrictions.eq("kelompokParameterTambahanPerbaikanAsset.aktif", true))
								.setProjection(Projections.groupProperty("parameterTambahan.id")),
						ParameterTambahan.class, false);
				Collections.sort(parameterTambahans);

				for (ParameterTambahan parameterTambahan : parameterTambahans) {
					String jenis = kelompokParameterTambahanPerbaikanAsset.getId() + "->" + parameterTambahan.getId();

					String val = "";
					String[] spl = perbaikanAsset.getParameterTambahanInds().split("\n");
					for (String d : spl) {
						String[] value = d.split("<=>");
						if (value[0].trim().equalsIgnoreCase(jenis)) {
							val = value.length > 1 ? value[1].trim() : "";
						}
					}
					vbox2.appendChild(new MyLabelKecilBold(parameterTambahan.getLabelInputan()));
					LampiranLain lampiranLain = LampiranLain.ambil(perbaikanAsset.getId(), jenis);

					ParameterTambahan.tampil(vbox2, parameterTambahan, lampiranLain, val);
				}

			}

			if (perbaikanAsset.getDisposisiSop() != null) {
				A aa;
				(aa = new A()).setParent(vbox2);
				aa.setStyle("font-size:9px;");
				UIClassHelper.applyReadMore(aa, "SOP " + perbaikanAsset.getDisposisiSop().getKeterangan() + " ("
						+ perbaikanAsset.getDisposisiSop().getSop().getNama() + ")");
				aa.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						TampilanAlurSopAction.prosess(perbaikanAsset.getDisposisiSop().getId(), null, null, true,
								arg0.getTarget());
					}
				});
			}

			if (perbaikanAsset.getDisposisiSop() != null && !perbaikanAsset.getDisposisiSop().getAktif()) {
				new Label(ais.common.Common.getBahasaConfig("Tidak aktif")).setParent(arg0);
			} else if (edit) {
				final MyCheckboxConfig aktif = new MyCheckboxConfig("Aktif");
				aktif.setChecked(perbaikanAsset.getAktif());
				aktif.setParent(arg0);
				aktif.addEventListener("onCheck", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						perbaikanAsset.setAktif(aktif.isChecked());
						Common.refreshSaveOrUpdate(perbaikanAsset);
					}
				});
			} else {
				new Label(perbaikanAsset.getAktif() ? "Ya" : "Tidak").setParent(arg0);
			}

			Hbox toolbar;
			(toolbar = Common.copyEditDeleteButtons(edit, delete, perbaikanAsset, PerbaikanAssetAction.this))
					.setParent(arg0);

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/print.png");
			button.setTooltiptext("Cetak");
			button.addEventListener("onClick", new EventListener() {
				@SuppressWarnings({})
				@Override
				public void onEvent(Event event) throws Exception {

					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							LaporanPerbaikanAsset.cetak(perbaikanAsset);
						}
					});
				}

			});
			button.setParent(toolbar);

		}

	}

	/**
	 * <h3>cetakData — Menghasilkan File PDF Laporan Perbaikan Aset untuk Satu Entitas</h3>
	 *
	 * <b>Tujuan:</b><br>
	 * Mengimplementasi metode dari interface {@link DataInitDefault} untuk menghasilkan file
	 * laporan cetak (PDF) dari satu entitas {@link PerbaikanAsset}. Digunakan oleh mekanisme
	 * cetak data Common.cetakData yang dipanggil dari tombol cetak di toolbar.
	 *
	 * <b>Cara kerja:</b><br>
	 * Memeriksa apakah jenis perbaikan tersedia. Kemudian mencari file template JRXML yang
	 * disimpan sebagai lampiran pada jenis perbaikan dengan tipe
	 * {@link LampiranLain#FILE_JRXML_LAYOUT_JENIS_FORM_PERBAIKAN}. Jika template tidak tersedia,
	 * mengembalikan null. Jika tersedia, membangkitkan parameter laporan menggunakan
	 * {@link LaporanPerbaikanAsset#generateParameter} dan mengkompilasi serta mengeksekusi laporan
	 * JasperReports menggunakan {@link Report#generateCompileFileReport}.
	 *
	 * <b>Return:</b><br>
	 * @return File PDF yang berisi laporan perbaikan aset, atau null jika jenis atau template tidak tersedia
	 *
	 * <b>Penanganan error:</b><br>
	 * Jika jenis perbaikan null, langsung return null. Jika template JRXML null, return null.
	 * Exception dari Report.generateCompileFileReport dilempar ke atas.
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Template JRXML untuk laporan harus diupload melalui halaman manajemen lampiran jenis
	 * perbaikan dengan tipe yang sesuai. Jika format laporan perlu diubah, modifikasi template
	 * JRXML, bukan kode Java ini.
	 *
	 * @param generalValueObject entitas perbaikan aset yang akan dicetak, dicast ke PerbaikanAsset
	 * @return file PDF laporan atau null jika tidak dapat dibangkitkan
	 * @throws Exception jika terjadi error saat kompilasi atau eksekusi laporan JasperReports
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public File cetakData(GeneralValueObject generalValueObject) throws Exception {
		PerbaikanAsset perbaikanAsset = (PerbaikanAsset) generalValueObject;
		JenisPerbaikanAsset j = perbaikanAsset.getJenisPerbaikanAsset();
		if (j == null) {
			return null;
		}
		LampiranLain lainMahaadministrasi = LampiranLain.ambil(j.getId(),
				LampiranLain.FILE_JRXML_LAYOUT_JENIS_FORM_PERBAIKAN);

		if (lainMahaadministrasi == null) {
			return null;
		}

		Map parameters = LaporanPerbaikanAsset.generateParameter(perbaikanAsset.getTanggal_dirubah(),
				perbaikanAsset.getTanggal_dirubah(), perbaikanAsset, perbaikanAsset.getJenisPerbaikanAsset());

		File file = Report.generateCompileFileReport(Report.PDF, parameters,
				lainMahaadministrasi.ambilFile().getAbsolutePath(), ais.ui.util.WaktuUtil.getDate());
		return file;
	}

	/**
	 * <h3>onAdd — Membuka Form Tambah Data Perbaikan Aset Baru</h3>
	 *
	 * <b>Tujuan:</b><br>
	 * Menangani event klik tombol "Tambah" pada toolbar halaman. Membuka form modal untuk
	 * memasukkan data work order perbaikan aset baru dengan entitas kosong.
	 *
	 * <b>Cara kerja:</b><br>
	 * Membuat instansi baru {@link PerbaikanAsset} yang kosong, kemudian memanggil
	 * {@link #init(PerbaikanAsset)} untuk menginisialisasi form dan menampilkannya sebagai
	 * dialog modal.
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Pastikan semua field form di-reset dengan benar di metode init ketika dipanggil
	 * dengan entitas baru (id null). Jangan tambahkan inisialisasi field di sini; lakukan di init.
	 *
	 * @param event event ZK yang dipicu oleh klik tombol Tambah
	 * @throws Exception jika terjadi error pada inisialisasi form atau komponen ZK
	 */
	public void onAdd(Event event) throws Exception {
		init(new PerbaikanAsset());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	/**
	 * <h3>init — Membuka Form dengan Entitas yang Diberikan (Tambah atau Ubah)</h3>
	 *
	 * <b>Tujuan:</b><br>
	 * Mengimplementasi metode dari interface {@link DataInitDefault}. Metode ini dipanggil
	 * oleh framework Common saat pengguna mengklik tombol Ubah (edit) pada baris grid.
	 * Membuka form modal yang terisi dengan data dari entitas yang diberikan.
	 *
	 * <b>Cara kerja:</b><br>
	 * Menyimpan referensi entitas perbaikanAsset, memanggil overload init(PerbaikanAsset)
	 * untuk membangun form, kemudian membuat jendela modal terlihat.
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Metode ini adalah delegasi ke init(PerbaikanAsset) — semua logika ada di sana.
	 *
	 * @param obj entitas yang akan diubah, dicast ke {@link PerbaikanAsset}
	 * @throws Exception jika terjadi error pada inisialisasi form
	 */
	@Override
	public void init(GeneralValueObject obj) throws Exception {
		perbaikanAsset = (PerbaikanAsset) obj;
		init(perbaikanAsset);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	/**
	 * <h3>form — Membangun Grid Form Input Perbaikan Aset</h3>
	 *
	 * <b>Tujuan:</b><br>
	 * Mengimplementasi metode dari interface {@link FormSop}. Membangun dan mengembalikan
	 * grid form yang berisi semua field input untuk work order perbaikan aset, termasuk
	 * parameter tambahan dinamis yang bergantung pada jenis perbaikan yang dipilih.
	 *
	 * <b>Cara kerja:</b><br>
	 * Membangun grid ZK dengan dua kolom (label di kiri, input di kanan). Field-field yang
	 * dibuat: nomor agenda (readonly, auto-generate), judul perbaikan, sarpras (aset detail),
	 * tanggal/waktu pengajuan, satuan kerja, jenis perbaikan, checkbox broadcast, daftar
	 * username penerima (tampil kondisional berdasarkan broadcast), dan grid lampiran dinamis.
	 * Saat jenis perbaikan berubah (onChange), event listener membersihkan grid lampiran dan
	 * memanggil {@link ParameterTambahanPerbaikanAssetListener} untuk me-render ulang parameter
	 * tambahan sesuai kelompok parameter dari jenis perbaikan yang baru. Baris broadcast dan
	 * daftar username disembunyikan/ditampilkan berdasarkan status checkbox broadcast.
	 *
	 * <b>Parameter:</b><br>
	 * @param generalValueObject entitas perbaikan aset yang akan diisi ke form, dicast ke PerbaikanAsset
	 * @param disposisiSop       disposisi SOP yang terhubung, null jika bukan dari alur SOP
	 * @param save               tombol simpan yang akan dikonfigurasi oleh caller
	 * @param setujui            listener aksi setujui dari alur SOP, null jika bukan dari SOP
	 *
	 * <b>Return:</b><br>
	 * @return MyGrid yang berisi semua komponen form, siap dipasang ke container
	 *
	 * <b>Penanganan error:</b><br>
	 * Jika satuanKerjaTreeModel null (misalnya dipanggil tanpa doAfterCompose), diinisialisasi
	 * ulang secara defensif. Event listener onChange jenis perbaikan dilindungi dengan null check.
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Pola parameter tambahan dinamis bergantung pada {@link KelompokParameterTambahanPerbaikanAsset}
	 * yang terhubung ke jenis perbaikan. Tambahkan tipe parameter baru di entitas ParameterTambahan
	 * dan implementasikan rendering-nya di ParameterTambahanPerbaikanAssetListener.
	 *
	 * @throws Exception jika terjadi error pada pembuatan komponen ZK atau akses basis data
	 */
	@SuppressWarnings("deprecation")
	@Override
	public MyGrid form(GeneralValueObject generalValueObject, DisposisiSop disposisiSop,
			final MyToolbarbuttonConfig save, EventListener setujui) throws Exception {
		this.perbaikanAsset = (PerbaikanAsset) generalValueObject;
		this.disposisiSop = (this.disposisiSop != null && (disposisiSop == null || disposisiSop.getId() == null))
				? this.disposisiSop
				: disposisiSop;

		if (satuanKerjaTreeModel == null) {
			satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);
		}

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("30%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("No. Agenda"));
		row.appendChild(kode = new Label(perbaikanAsset.getKode()));
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Judul Perbaikan *"));
		row.appendChild(nama = new MyTextbox(perbaikanAsset.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sarpras *"));
		row.appendChild(assetDetail = new AmbilDataAssetDetailBanbox());
		assetDetail.setAttribute("assetDetail", perbaikanAsset.getAssetDetail());
		assetDetail.setValue(perbaikanAsset.getAssetDetail() == null ? "" : perbaikanAsset.getAssetDetail().getNama());
		assetDetail.setWidth("90%");
		assetDetail.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal dan Waktu Pengajuan *"));
		row.appendChild(waktu = new MyDatebox(perbaikanAsset.getWaktu()));
		waktu.setFormat(Common.dateFormat3.get().toPattern());
		waktu.setWidth("90%");
		waktu.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Satuan Kerja"));
		satuanKerja = new AmbilDataSatuanKerjaBanbox(true);
		satuanKerja.setValue(perbaikanAsset.getSatuanKerja() == null ? "" : perbaikanAsset.getSatuanKerja().getNama());
		satuanKerja.setAttribute("satuanKerja", perbaikanAsset.getSatuanKerja());
		row.appendChild(satuanKerja);
		satuanKerja.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Perbaikan *"));
		row.appendChild(jenisPerbaikanAsset = new Combobox());
		jenisPerbaikanAsset.setWidth("90%");
		jenisPerbaikanAsset.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		broadcast = new MyCheckboxConfig("Sebarkan / broadcast perbaikan ini");
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(broadcast);
		broadcast.setChecked(perbaikanAsset.getBroadcast());

		final MyFormRow rowUsernameDisposisi = new MyFormRow();
		rowUsernameDisposisi.setParent(rows);
		rowUsernameDisposisi.appendChild(new ais.ui.util.MyLabelConfig("Disebarkan ke username pengguna"));
		rowUsernameDisposisi.appendChild(keterangan = new Textbox(perbaikanAsset.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(2);

		MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Ambil Username Pengguna",
				"/img/user_male_add.png");

		final MyFormRow rowAmbilPengguna = new MyFormRow();
		rowAmbilPengguna.setParent(rows);
		rowAmbilPengguna.appendChild(new ais.ui.util.MyLabelConfig(""));
		rowAmbilPengguna.appendChild(toolbarbutton);
		toolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				AmbilDataTbmuserBanyak ambil = new AmbilDataTbmuserBanyak(new ArrayList<Tbmuser>());
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(ambil);
				ambil.setEventListener(new EventListener() {

					@SuppressWarnings("unchecked")
					@Override
					public void onEvent(Event arg0) throws Exception {
						List<Tbmuser> tbmusers = (List<Tbmuser>) arg0.getData();
						if (tbmusers != null && tbmusers.size() != 0) {
							for (Tbmuser tbmuser : tbmusers) {
								keterangan.setValue(
										keterangan.getValue() + (keterangan.getValue().isEmpty() ? tbmuser.getUserId()
												: "," + tbmuser.getUserId()));
							}
						}
					}
				});
				ambil.setWidth("850px");
				ambil.setHeight("97%");
				ambil.setVisible(true);
				ambil.onModal();
			}
		});

		final Row r = Common.initKeterangan(rows, "Jika lebih dari satu pengguna, pisahkan dengan tanda koma (,)");

		EventListener startEvent = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				rowUsernameDisposisi.setVisible(broadcast.isChecked());
				rowAmbilPengguna.setVisible(broadcast.isChecked());
				r.setVisible(broadcast.isChecked());
			}

		};

		broadcast.addEventListener("onClick", startEvent);
		startEvent.onEvent(null);

		MyFormRow rowLampiran = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(rowLampiran, "2");
		rowLampiran.setParent(rows);

		final Grid gridLampiran = new Grid();
		gridLampiran.setSclass("fgrid");
		gridLampiran.setParent(rowLampiran);

		columns = new Columns();
		columns.setParent(gridLampiran);

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("30%");

		column = new MyColumnConfig();
		column.setParent(columns);

		final Rows rowsLampiran = new Rows();
		rowsLampiran.setParent(gridLampiran);

		final EventListener eventListenerJenisPerbaikanAsset = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Common.clear(rowsLampiran);

				JenisPerbaikanAsset j = (JenisPerbaikanAsset) (jenisPerbaikanAsset.getSelectedItem() == null ? null
						: jenisPerbaikanAsset.getSelectedItem().getValue());

				if (j != null) {

					if (perbaikanAsset.getId() == null) {
						String noAgenda = generateCode(j, false);
						kode.setValue(noAgenda);
					}

					parameterRows = new ArrayList<Row>();
					lampiranLains = new HashMap<String, LampiranLain>();
					HibernateUtil.currentSession().refresh(j);

					Set<KelompokParameterTambahanPerbaikanAsset> kelompokParameterTambahanPerbaikanAssets = new TreeSet<KelompokParameterTambahanPerbaikanAsset>();
					for (KelompokParameterTambahanPerbaikanAsset kelompokParameterTambahanPerbaikanAsset : j
							.getKelompokParameterTambahanPerbaikanAssets()) {
						kelompokParameterTambahanPerbaikanAssets.add(kelompokParameterTambahanPerbaikanAsset);
					}

					parameterTambahanListener = new ParameterTambahanPerbaikanAssetListener(perbaikanAsset,
							kelompokParameterTambahanPerbaikanAssets, parameterRows, lampiranLains, rowsLampiran);

					parameterTambahanListener.onEvent(null);
				}
			}

		};

		Common.insertCombo(jenisPerbaikanAsset, new String[] { "nama", "kode" }, "keterangan",
				JenisPerbaikanAsset.class, Restrictions.eq("aktif", true));
		Common.selectComboItem(jenisPerbaikanAsset, perbaikanAsset.getJenisPerbaikanAsset());

		jenisPerbaikanAsset.addEventListener("onChange", eventListenerJenisPerbaikanAsset);
		eventListenerJenisPerbaikanAsset.onEvent(null);

		return grid;
	}

	/**
	 * <h3>generateCode — Membangkitkan Nomor Agenda Perbaikan Aset</h3>
	 *
	 * <b>Tujuan:</b><br>
	 * Membangkitkan nomor agenda (kode) untuk work order perbaikan aset berdasarkan konfigurasi
	 * {@link NomorSurat} yang terhubung ke jenis perbaikan yang dipilih. Kode yang dibangkitkan
	 * dijamin unik menggunakan {@link ais.action.master.KodeUnikUtil#pastikanUnik}.
	 *
	 * <b>Cara kerja:</b><br>
	 * Memeriksa apakah jenis perbaikan dan konfigurasi NomorSurat tersedia. Menentukan index
	 * berdasarkan konfigurasi: jika gunakanIndexUrut aktif, menggunakan nomorIndex dari NomorSurat;
	 * jika tidak, menghitung dari database via getindex(). Jika parameter tambah true, index
	 * NomorSurat di-increment. Format nomor dibuat menggunakan NomorSurat.format(). Hasilnya
	 * dipastikan unik menggunakan KodeUnikUtil.
	 *
	 * <b>Parameter:</b><br>
	 * @param j      jenis perbaikan aset yang memiliki konfigurasi NomorSurat
	 * @param tambah jika true, increment index NomorSurat setelah pembangkitan kode
	 *
	 * <b>Return:</b><br>
	 * @return string kode yang unik, atau string kosong jika jenis null atau NomorSurat tidak dikonfigurasi
	 *
	 * <b>Penanganan error:</b><br>
	 * Seluruh logika dibungkus try-catch; jika terjadi error apapun, dikembalikan string kosong
	 * sehingga tidak memblokir proses lain.
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Logika urutannya bergantung pada konfigurasi NomorSurat: resetUrutanTiapTahun,
	 * resetUrutanTiapBulan, resetTiap, urutBerdasarkanNomor, urutBerdasarkanKelompok.
	 * Ubah konfigurasi ini dari halaman master NomorSurat, bukan dari kode Java.
	 */
	private String generateCode(JenisPerbaikanAsset j, boolean tambah) {

		try {
			if (j == null || j.getNomorSurat() == null) {
				return "";
			}

			Long index = j.getNomorSurat().getGunakanIndexUrut() ? j.getNomorSurat().getNomorIndex() : getindex(j);
			if (tambah) {
				NomorSurat.tambahIndexNomorSurat(j.getNomorSurat());
			}
			String noAgenda = j.getNomorSurat().format(index, waktu.getValue());
			return ais.action.master.KodeUnikUtil.pastikanUnik(PerbaikanAsset.class, noAgenda);
		} catch (Exception e) {
			return "";
		}
	}

	/**
	 * <h3>getindex — Menghitung Index Urutan Berikutnya untuk Penomoran Agenda</h3>
	 *
	 * <b>Tujuan:</b><br>
	 * Menghitung nomor urut (index) berikutnya untuk pembangkitan kode agenda perbaikan aset,
	 * berdasarkan konfigurasi reset urutan pada {@link NomorSurat} yang terhubung ke jenis perbaikan.
	 *
	 * <b>Cara kerja:</b><br>
	 * Memeriksa apakah NomorSurat tersedia pada jenis perbaikan. Membangun query Hibernate Criteria
	 * pada entitas {@link PerbaikanAsset} dengan filter yang bergantung pada konfigurasi:
	 * - urutBerdasarkanNomor: filter per NomorSurat tertentu
	 * - urutBerdasarkanKelompok: filter per kelompok NomorSurat
	 * - default: semua data
	 * Filter tambahan: reset per tahun (filter tahun saat ini), per bulan (filter tahun dan bulan),
	 * atau per tanggal tertentu (filter berdasarkan resetTiap). Hasil rowCount diambil dan
	 * di-increment satu untuk mendapat index berikutnya.
	 *
	 * <b>Parameter:</b><br>
	 * @param jenisPerbaikanAsset jenis perbaikan yang konfigurasi NomorSurat-nya digunakan
	 *
	 * <b>Return:</b><br>
	 * @return index berikutnya (selalu minimal 1), bertipe Long
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Metode ini melakukan query ke database setiap kali dipanggil; pada sistem dengan banyak
	 * transaksi bersamaan, ada potensi race condition dalam penomoran. Pertimbangkan mekanisme
	 * sequence database untuk kebutuhan high-concurrency.
	 */
	private Long getindex(JenisPerbaikanAsset jenisPerbaikanAsset) {
		if (jenisPerbaikanAsset.getNomorSurat() == null) {
			return 0L;
		}
		Session session = HibernateUtil.currentSession();
		int tahun = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
		int bulan = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.MONTH) + 1;
		Date sekarang = WaktuUtil.getDate();
		Number indexO = (Number) session.createCriteria(PerbaikanAsset.class)
				.createAlias("jenisPerbaikanAsset", "jenisPerbaikanAsset", Criteria.LEFT_JOIN)
				.createAlias("jenisPerbaikanAsset.nomorSurat", "nomorSurat", Criteria.LEFT_JOIN)

				.add(jenisPerbaikanAsset.getNomorSurat().getUrutBerdasarkanNomor()
						? Restrictions.eq("jenisPerbaikanAsset.nomorSurat", jenisPerbaikanAsset.getNomorSurat())

						: (jenisPerbaikanAsset.getNomorSurat().getUrutBerdasarkanKelompok()
								&& jenisPerbaikanAsset.getNomorSurat().getKelompokNomorSurat() != null
										? Restrictions.eq("nomorSurat.kelompokNomorSurat",
												jenisPerbaikanAsset.getNomorSurat().getKelompokNomorSurat())
										: Restrictions.sqlRestriction("true")))

				.add(jenisPerbaikanAsset.getNomorSurat().getResetUrutanTiapTahun() ? Restrictions.eq("tahun", tahun)
						: Restrictions.sqlRestriction("true"))

				.add(jenisPerbaikanAsset.getNomorSurat().getResetUrutanTiapBulan()
						? Restrictions.and(Restrictions.eq("tahun", tahun), Restrictions.eq("bulan", bulan))
						: Restrictions.sqlRestriction("true"))

				.add(jenisPerbaikanAsset.getNomorSurat().getResetTiap() != null
						&& (Common.dateFormat8.get().format(jenisPerbaikanAsset.getNomorSurat().getResetTiap())
								.equals(Common.dateFormat8.get().format(sekarang))
								|| jenisPerbaikanAsset.getNomorSurat().getResetTiap().before(sekarang))
										? Restrictions.ge("waktu", jenisPerbaikanAsset.getNomorSurat().getResetTiap())
										: Restrictions.sqlRestriction("true"))

				.setProjection(Projections.rowCount()).uniqueResult();

		Long index = indexO == null ? null : indexO.longValue();
		if (index == null) {
			index = 0L;
		}
		return ++index;
	}

	/**
	 * <h3>init — Menginisialisasi dan Membangun Form Modal Perbaikan Aset</h3>
	 *
	 * <b>Tujuan:</b><br>
	 * Membangun seluruh struktur layout form modal (Borderlayout dengan Center dan South)
	 * untuk tambah atau ubah data work order perbaikan aset. Menggunakan metode
	 * {@link #form(GeneralValueObject, DisposisiSop, MyToolbarbuttonConfig, EventListener)}
	 * untuk membangun konten form di area Center.
	 *
	 * <b>Cara kerja:</b><br>
	 * Menyimpan entitas, mengatur judul jendela berdasarkan kondisi tambah/ubah,
	 * membersihkan konten addWindow yang ada, membangun Borderlayout baru, dan memuat
	 * form input di area Center. Di area South dibuat toolbar dengan tombol Batal
	 * (menyembunyikan jendela) dan tombol Simpan (memanggil onSave dan refresh grid).
	 *
	 * <b>Parameter:</b><br>
	 * @param perbaikanAsset entitas yang akan diedit; jika id null berarti tambah baru
	 *
	 * <b>Penanganan error:</b><br>
	 * Exception dari form() dan onSave() dilempar ke atas dan ditangkap oleh framework ZK
	 * untuk ditampilkan sebagai pesan error.
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Pembersihan addWindow menggunakan Common.clear() untuk menghapus semua anak komponen
	 * secara aman. Jangan gunakan addWindow.getChildren().clear() langsung karena dapat
	 * menyebabkan masalah dengan lifecycle komponen ZK.
	 *
	 * @throws Exception jika terjadi error pada pembuatan komponen ZK atau inisialisasi form
	 */
	private void init(final PerbaikanAsset perbaikanAsset) throws Exception {
		this.perbaikanAsset = perbaikanAsset;
		addWindow.setTitle(perbaikanAsset.getId() == null ? "Tambah Perbaikan Alat/Fasilitas" : "Ubah Perbaikan Alat/Fasilitas");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");

		disposisiSop = null;
		center.appendChild(form(perbaikanAsset, disposisiSop, save, null));

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
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
					addWindow.setVisible(false);
				}
			}
		});
		save.setParent(toolbar);

		borderlayout.setParent(addWindow);

	}

	/**
	 * <h3>onSave — Menyimpan Data Work Order Perbaikan Aset ke Basis Data</h3>
	 *
	 * <b>Tujuan:</b><br>
	 * Memvalidasi input form, menyimpan atau memperbarui entitas {@link PerbaikanAsset} ke basis
	 * data, menyimpan lampiran file, mengirim broadcast email jika diperlukan, dan mencetak
	 * laporan PDF serta mengirim data ke endpoint eksternal di latar belakang.
	 *
	 * <b>Cara kerja:</b><br>
	 * 1. Validasi wajib: assetDetail dan jenisPerbaikanAsset tidak boleh null.<br>
	 * 2. Validasi parameter tambahan melalui parameterTambahanListener.validate().<br>
	 * 3. Jika ubah, load ulang entitas dari basis data untuk menghindari detached object.<br>
	 * 4. Set semua field entitas dari nilai form.<br>
	 * 5. Jika disposisiSop tersedia, set ke entitas.<br>
	 * 6. Panggil parameterTambahanListener.onSave() untuk menyimpan nilai parameter tambahan.<br>
	 * 7. Jika ubah dan index belum ada, generate kode baru. Jika tambah, generate kode.<br>
	 * 8. Simpan atau update entitas ke basis data.<br>
	 * 9. Jika ada lampiran file, update ref pada streaming session.<br>
	 * 10. Jika broadcast aktif, kirim email di timer default.<br>
	 * 11. Cetak laporan PDF dan kirim HTTP POST ke endpoint eksternal di thread terpisah.
	 *
	 * <b>Return:</b><br>
	 * @return true jika berhasil disimpan, false jika validasi gagal
	 *
	 * <b>Threading:</b><br>
	 * Broadcast email dan HTTP POST ke endpoint eksternal berjalan di thread terpisah agar tidak
	 * memblokir event thread. Sesi streaming untuk lampiran dibuka dan ditutup secara eksplisit.
	 *
	 * <b>Penanganan error:</b><br>
	 * Validasi ditampilkan melalui MyMessageboxConfig. Error pada HTTP POST ditangkap dan
	 * dilaporkan via Common.tampilErrorJikaAdmin. Tidak ada rollback otomatis jika gagal
	 * setelah penyimpanan entitas berhasil.
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Konfigurasi endpoint eksternal dibaca dari kunci "link_post_perbaikan-asset". Kosongkan
	 * nilai ini di tabel konfigurasi untuk menonaktifkan integrasi tanpa mengubah kode.
	 *
	 * @param event event ZK yang memicu simpan (dari klik tombol Simpan)
	 * @return true jika operasi simpan berhasil, false jika validasi gagal
	 * @throws Exception jika terjadi error yang tidak tertangani pada operasi basis data
	 */
	public boolean onSave(Event event) throws Exception {
		if (assetDetail.getAttribute("assetDetail") == null) {
			MyMessageboxConfig.show("Mohon maaf, Sarpras (Sarana dan Prasarana) belum dipilih. Langkah yang dapat dilakukan: (1) Klik tombol pilih pada field Sarpras; (2) Cari dan pilih sarana/prasarana yang akan diperbaiki dari daftar; (3) ulangi proses simpan ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (jenisPerbaikanAsset.getSelectedItem() == null) {
			MyMessageboxConfig.show("Mohon maaf, Jenis Perbaikan belum dipilih. Langkah yang dapat dilakukan: (1) Pilih jenis perbaikan yang sesuai dari dropdown Jenis Perbaikan; (2) Pastikan daftar jenis perbaikan sudah tersedia (dapat dikonfigurasi di menu Jenis Perbaikan Aset); (3) ulangi proses simpan ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (parameterTambahanListener != null && !parameterTambahanListener.validate()) {
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (perbaikanAsset.getId() != null) {
			perbaikanAsset = (PerbaikanAsset) session.load(PerbaikanAsset.class, perbaikanAsset.getId());

		}

		perbaikanAsset.setWaktu(waktu.getValue());
		perbaikanAsset.setSatuanKerja((SatuanKerja) satuanKerja.getAttribute("satuanKerja"));
		perbaikanAsset
				.setJenisPerbaikanAsset((JenisPerbaikanAsset) (jenisPerbaikanAsset.getSelectedItem() == null ? null
						: jenisPerbaikanAsset.getSelectedItem().getValue()));

		perbaikanAsset.setNama(nama.getValue());
		perbaikanAsset.setBroadcast(broadcast.isChecked());
		perbaikanAsset.setAssetDetail((AssetDetail) assetDetail.getAttribute("assetDetail"));

		if (disposisiSop != null && disposisiSop.getId() != null) {
			perbaikanAsset.setDisposisiSop(disposisiSop);
		}

		parameterTambahanListener.onSave(perbaikanAsset);

		if (perbaikanAsset.getId() != null) {

			if (perbaikanAsset.getIndex() == null) {
				String noAgenda = generateCode(perbaikanAsset.getJenisPerbaikanAsset(), true);
				kode.setValue(noAgenda);
				perbaikanAsset.setKode(noAgenda);
				Long currentIndex = getindex(perbaikanAsset.getJenisPerbaikanAsset());
				perbaikanAsset.setIndex(++currentIndex);
			}

			Common.refreshUpdate(session, perbaikanAsset);
		} else {
			if (perbaikanAsset.getKode() == null) {
				String noAgenda = generateCode(perbaikanAsset.getJenisPerbaikanAsset(), true);
				kode.setValue(noAgenda);
				perbaikanAsset.setKode(noAgenda);
			}

			Long currentIndex = getindex(perbaikanAsset.getJenisPerbaikanAsset());
			perbaikanAsset.setIndex(++currentIndex);
			session.save(perbaikanAsset);
		}

		if (!lampiranLains.isEmpty()) {
			Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();
			streamingSession.getTransaction().begin();
			for (LampiranLain lampiranLain : lampiranLains.values()) {
				streamingSession.refresh(lampiranLain);
				lampiranLain.setRef(perbaikanAsset.getId());
				streamingSession.update(lampiranLain);
			}
			streamingSession.getTransaction().commit();
			StreamingHibernateUtil.getInstance().closeSession();
		}

		if (broadcast.isChecked()) {
			Common.createDefaultTimer(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					BroadcastHelper.kirimEmailPerbaikanAsset(perbaikanAsset);
				}
			});
		}

		Common.createDefaultTimer(new EventListener() {

			@SuppressWarnings({ "rawtypes", "unchecked" })
			@Override
			public void onEvent(Event arg0) throws Exception {
				LaporanPerbaikanAsset.cetak(perbaikanAsset);

				new Thread(new Runnable() {

					@Override
					public void run() {
						String linkPost = Common.getKonfigurasi("link_post_perbaikan-asset", "").getNilai();
						if (!linkPost.isEmpty()) {
							String hasil = "";
							try {

								Map parameters = LaporanPerbaikanAsset.initData(perbaikanAsset);
								Common.insertProperty(PerbaikanAsset.class, perbaikanAsset, parameters,
										"perbaikanAsset");

								JSONObject postData = new JSONObject(parameters);

								System.out.println("linkPost -> " + linkPost);
								System.out.println("postData -> " + postData);

								String[] command = { "curl", "-k", "-H", "Accept: application/json", "-X", "POST",
										linkPost, "--data", postData.toString() };

								ProcessBuilder process = new ProcessBuilder(command);
								Process p;
								p = process.start();
								BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
								StringBuilder builder = new StringBuilder();
								String line = null;
								while ((line = reader.readLine()) != null) {
									builder.append(line);
									builder.append(System.getProperty("line.separator"));
								}
								hasil = builder.toString();

							} catch (Exception e) {
								ais.common.Common.tampilErrorJikaAdmin(e);
							}

							System.out.println("hasil -> " + hasil);
						}
					}
				}).start();
			}
		});

		return true;
	}

	/**
	 * Model pohon satuan kerja yang digunakan untuk navigasi hierarki organisasi pada filter
	 * pencarian. Diinisialisasi di doAfterCompose dan digunakan di initCriteria.
	 */
	private SatuanKerjaTreeModel satuanKerjaTreeModel;

	/**
	 * Checkbox filter untuk menampilkan hanya perbaikan yang aktif (belum selesai/dibatalkan).
	 * Null-safe dalam initCriteria: jika null, semua data ditampilkan.
	 */
	private Checkbox searchaktif;

	/**
	 * <h3>initCriteria — Membangun Kriteria Pencarian Data Perbaikan Aset</h3>
	 *
	 * <b>Tujuan:</b><br>
	 * Membangun objek {@link Criteria} Hibernate yang mencerminkan seluruh kondisi filter pencarian
	 * yang aktif. Digunakan untuk query data grid maupun penghitungan total untuk paging.
	 *
	 * <b>Cara kerja:</b><br>
	 * Menentukan set satuan kerja yang relevan berdasarkan filter searchparent: jika ada satuan
	 * kerja yang dipilih, ambil juga semua turunannya dari tree model. Membangun Criteria pada
	 * {@link PerbaikanAsset} dengan filter: status aktif (dari checkbox searchaktif),
	 * status persetujuan (dari checkbox blmDisetujui/disetujui), filter kode, filter keterangan/nama,
	 * dan filter satuan kerja (termasuk null dan set turunan). Jika order true, tambahkan
	 * urutan descending berdasarkan id.
	 *
	 * <b>Parameter:</b><br>
	 * @param order jika true, tambahkan ORDER BY id DESC pada kriteria
	 *
	 * <b>Return:</b><br>
	 * @return objek {@link Criteria} siap dieksekusi, atau null jika searchparent belum terinisialisasi
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Filter satuan kerja menggunakan OR yang kompleks untuk menangani kasus parent null dan
	 * set kosong. Jika logika filter berubah, pastikan semua kasus edge case (satuan kerja null,
	 * set kosong, parent dipilih) masih berfungsi dengan benar.
	 */
	public Criteria initCriteria(boolean order) {
		if (searchparent == null) return null;

		SatuanKerja parent = (SatuanKerja) searchparent.getAttribute("satuanKerja");
		Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
		if (parent != null) {
			satuanKerjas.clear();
			satuanKerjas.add(parent);
			satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
		}

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(PerbaikanAsset.class)

				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"))

				.add(blmDisetujui == null || disetujui == null ? Restrictions.sqlRestriction("true") : blmDisetujui.isChecked() && disetujui.isChecked() ? Restrictions.sqlRestriction("true")
						: !blmDisetujui.isChecked() && !disetujui.isChecked() ? Restrictions.sqlRestriction("false")
								: blmDisetujui.isChecked() ? Restrictions.isNull("disetujuiOleh")
										: Restrictions.isNotNull("disetujuiOleh"));

		if (order)
			criteria.addOrder(Order.desc("id"));
		criteria

				.add(searchkode.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("kode", searchkode.getValue().trim(), MatchMode.ANYWHERE))

				.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("keterangan", searchnama.getValue().trim(), MatchMode.ANYWHERE))

				.add(satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.isNull("satuanKerja"),
								Restrictions.or(
										parent == null ? Restrictions.isNull("satuanKerja")
												: Restrictions.sqlRestriction("false"),
										Restrictions.in("satuanKerja", satuanKerjas))))

		;
		return criteria;
	}

	/**
	 * <h3>onSearchDefault — Memuat Data Grid Perbaikan Aset</h3>
	 *
	 * <b>Tujuan:</b><br>
	 * Melakukan query data ke basis data berdasarkan filter aktif dan memperbarui grid serta
	 * paging. Metode ini merupakan implementasi dari interface {@link DataSearchDefault} dan
	 * dipanggil oleh berbagai event: perubahan filter, pergantian halaman paging, dan setelah
	 * simpan/hapus data.
	 *
	 * <b>Cara kerja:</b><br>
	 * Memanggil {@link #initCriteria(boolean)} dua kali: tanpa urutan untuk paging,
	 * dengan urutan untuk query data halaman aktif. Hasil dibungkus dalam {@link SimpleListModel}
	 * dan di-set ke grid dengan renderer {@link PerbaikanAssetRenderer}.
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Guard null pada initCriteria() diperlukan karena pada beberapa kondisi inisialisasi,
	 * searchparent mungkin belum tersedia.
	 *
	 * @param event event ZK yang memicu pencarian (boleh null)
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<PerbaikanAsset> perbaikanAsset = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(perbaikanAsset);
		grid.setRowRenderer(new PerbaikanAssetRenderer());
		grid.setModelCheckMobile(strset);

	}

	/**
	 * <h3>istilah — Mengembalikan Nama Istilah Entitas untuk Tampilan</h3>
	 *
	 * <b>Tujuan:</b><br>
	 * Mengimplementasi metode dari interface {@link DataInitDefault} yang mengembalikan
	 * nama/istilah entitas dalam bahasa Indonesia untuk digunakan pada judul dialog,
	 * pesan konfirmasi, dan label lain yang dibangkitkan secara generik oleh framework Common.
	 *
	 * <b>Return:</b><br>
	 * @return string "Perbaikan Alat/Fasilitas" sebagai nama entitas dalam bahasa Indonesia
	 *
	 * @throws Exception tidak dilempar pada implementasi ini
	 */
	@Override
	public String istilah() throws Exception {
		return "Perbaikan Alat/Fasilitas";
	}

	/**
	 * <h3>ambil — Mengembalikan Entitas Perbaikan Aset yang Sedang Aktif di Form</h3>
	 *
	 * <b>Tujuan:</b><br>
	 * Mengimplementasi metode dari interface {@link FormSop} yang memberikan akses ke entitas
	 * {@link DataSop} saat ini. Digunakan oleh alur SOP untuk mendapatkan objek yang sedang
	 * diproses melalui workflow persetujuan.
	 *
	 * <b>Return:</b><br>
	 * @return entitas {@link PerbaikanAsset} yang sedang dalam form, atau null jika belum ada
	 *
	 * @throws Exception tidak dilempar pada implementasi ini
	 */
	@Override
	public DataSop ambil() throws Exception {
		return perbaikanAsset;
	}

	/**
	 * <h3>ambilClass — Mengembalikan Class Entitas Perbaikan Aset</h3>
	 *
	 * <b>Tujuan:</b><br>
	 * Mengimplementasi metode dari interface {@link FormSop} yang mengembalikan Class entitas
	 * yang dikelola oleh Action ini. Digunakan oleh framework SOP dan mekanisme generik lain
	 * untuk refleksi dan operasi berbasis tipe.
	 *
	 * <b>Return:</b><br>
	 * @return {@code PerbaikanAsset.class}
	 *
	 * @throws Exception tidak dilempar pada implementasi ini
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public Class ambilClass() throws Exception {
		return PerbaikanAsset.class;
	}

	/**
	 * <h3>setPersetujuan — Mengatur Flag Persetujuan dari Alur SOP</h3>
	 *
	 * <b>Tujuan:</b><br>
	 * Mengimplementasi metode dari interface {@link FormSop} yang dipanggil oleh framework SOP
	 * untuk mengatur status persetujuan saat work order perbaikan aset melewati alur persetujuan.
	 *
	 * <b>Cara kerja:</b><br>
	 * Implementasi saat ini tidak melakukan tindakan apa pun (no-op). Field persetujuan di
	 * entitas PerbaikanAsset dikelola melalui mekanisme lain (disposisiSop dan disetujuiOleh).
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Jika di masa depan perlu merespons perubahan status persetujuan, implementasikan logika
	 * yang sesuai di sini. Jangan hapus metode ini karena merupakan kontrak interface FormSop.
	 *
	 * @param persetujuan true jika tindakan persetujuan dilakukan, false jika ditolak
	 */
	@Override
	public void setPersetujuan(boolean persetujuan) {
		// implementasi sengaja kosong: flag persetujuan dikelola via disposisiSop dan disetujuiOleh
	}
}
