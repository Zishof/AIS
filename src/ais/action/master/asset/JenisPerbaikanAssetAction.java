package ais.action.master.asset;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
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

import ais.action.master.helper.RevisiHelper;
import ais.action.master.surat.helper.AmbilDataNomorSuratBanbox;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.common.PesanFormalHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.asset.JenisPerbaikanAsset;
import ais.database.model.asset.KelompokParameterTambahanPerbaikanAsset;
import ais.database.model.file.LampiranLain;
import ais.database.model.surat.NomorSurat;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * <h3>JenisPerbaikanAssetAction — Kontroler CRUD Jenis Perbaikan Aset</h3>
 *
 * <b>Untuk apa:</b><br>
 * Kelas ini adalah kontroler ZK (ZKoss Composer) untuk halaman manajemen jenis perbaikan
 * aset (alat/fasilitas/sarana prasarana) di modul manajemen aset eCampus. Jenis perbaikan
 * aset adalah data referensi yang mengklasifikasikan tipe atau kategori perbaikan yang dapat
 * dilakukan pada aset institusi, misalnya: "Perbaikan Rutin", "Penggantian Suku Cadang",
 * "Kalibrasi Alat Ukur", "Renovasi Fasilitas", dan sebagainya. Setiap jenis perbaikan
 * dapat dikonfigurasi dengan: format nomor agenda (dari master {@code NomorSurat}),
 * kelompok parameter tambahan yang digunakan dalam formulir perbaikan (multi-pilih via
 * checkbox), file template laporan form perbaikan (jrxml/jasper untuk layout per-item),
 * dan file template laporan rekap perbaikan (jrxml/jasper untuk laporan ringkasan).<br><br>
 *
 * <b>Cara kerja:</b><br>
 * Halaman ini menggunakan pola CRUD standar eCampus dengan GenericAutowireComposer ZKoss 5.5.
 * Saat tombol Tambah atau Ubah ditekan, form dibangun secara programatik di metode
 * {@code init(JenisPerbaikanAsset)} dengan komponen ZK. Form mencakup: textbox nama,
 * banbox nomor agenda, textbox keterangan, widget upload file laporan form jrxml,
 * widget upload file laporan rekap jrxml, serta sub-grid checkbox kelompok parameter.
 * Penyimpanan file jrxml dilakukan melalui {@code StreamingHibernateUtil} (sesi Hibernate
 * streaming khusus untuk BLOB) setelah data utama disimpan ke sesi Hibernate biasa,
 * sehingga ID entitas sudah tersedia saat menetapkan referensi file ke jenis perbaikan.<br><br>
 *
 * <b>Threading:</b><br>
 * Tidak ada pemrosesan latar belakang. Semua operasi dilakukan di thread utama ZK.
 * Penggunaan dua sesi Hibernate berbeda (currentSession untuk data utama,
 * StreamingHibernateUtil untuk BLOB/file) memerlukan manajemen transaksi yang hati-hati
 * agar tidak ada konflik atau dangling transaction.<br><br>
 *
 * <b>Pemeliharaan:</b><br>
 * Kelas ini mengimplementasikan tiga antarmuka: {@code DataCriteria}, {@code DataSearchDefault},
 * dan {@code DataInitDefault}. Kelompok parameter yang dipilih disimpan sebagai Set di
 * field {@code selectedKelompokParameterTambahanPerbaikanAsset} yang dimanipulasi via
 * checkbox listener. File jrxml disimpan ke entitas {@code LampiranLain} dengan tipe
 * file yang berbeda (form vs rekap). Penambahan tipe file baru memerlukan konstanta
 * baru di {@code LampiranLain}.
 *
 * @author eCampus Dev Team
 * @version 1.0
 */
public class JenisPerbaikanAssetAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault {

	/**
	 * Serial version UID untuk serialisasi kelas ZK Composer ini.
	 */
	private static final long serialVersionUID = -5779730267402400328L;

	/** Jendela modal untuk form tambah/ubah jenis perbaikan aset. */
	private MyWindow addWindow;

	/** Komponen paginasi untuk grid daftar jenis perbaikan aset. */
	private Paging paging;

	/** Grid utama yang menampilkan daftar jenis perbaikan aset. */
	private MyGrid grid;

	/** Kotak teks filter pencarian berdasarkan nama jenis perbaikan. */
	private Textbox searchnama;

	/** Kotak teks input nama jenis perbaikan pada form tambah/ubah. */
	private Textbox nama;

	/** Kotak teks keterangan tambahan pada form tambah/ubah. */
	private Textbox keterangan;

	/** Penanda apakah pengguna memiliki hak ubah data. */
	private boolean edit = false;

	/** Penanda apakah pengguna memiliki hak hapus data. */
	private boolean delete = false;

	/** Entitas jenis perbaikan aset yang sedang dikelola. */
	private JenisPerbaikanAsset jenisPerbaikanAsset;

	/** Tombol toolbar tambah data baru. */
	private MyToolbarbuttonConfig add;

	/**
	 * Set kelompok parameter tambahan yang dipilih melalui checkbox di form.
	 * Dimanipulasi langsung oleh event listener setiap checkbox kelompok parameter.
	 */
	private Set<KelompokParameterTambahanPerbaikanAsset> selectedKelompokParameterTambahanPerbaikanAsset;

	/** File lampiran laporan form perbaikan (jrxml/jasper) yang diunggah, diisi oleh callback upload. */
	protected LampiranLain lampiran;

	/** Banbox untuk memilih format nomor agenda perbaikan dari master NomorSurat. */
	private AmbilDataNomorSuratBanbox nomorSurat;

	/** File lampiran laporan form per-item perbaikan (jrxml/jasper), diisi oleh callback upload. */
	protected LampiranLain lampiran1;

	/**
	 * Pemeriksaan keamanan sebelum halaman dikompilasi oleh ZK.
	 *
	 * <b>Tujuan:</b><br>
	 * Memastikan pengguna yang mengakses halaman jenis perbaikan aset telah memiliki
	 * sesi dan hak akses yang valid sebelum proses komposisi halaman dimulai. Gate keamanan
	 * pertama dalam siklus hidup halaman ZK.<br><br>
	 *
	 * <b>Cara kerja:</b><br>
	 * Memanggil {@code Common.doCheckSecurity()} untuk validasi sesi dan hak akses,
	 * lalu memanggil {@code super.doBeforeCompose} untuk melanjutkan proses ZK.<br><br>
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Pola standar keamanan eCampus. Tidak dimodifikasi tanpa koordinasi tim keamanan.
	 *
	 * @param page     Halaman ZK yang sedang dimuat.
	 * @param parent   Komponen induk.
	 * @param compInfo Informasi metadata komponen ZUL.
	 * @return {@code ComponentInfo} untuk melanjutkan proses komposisi ZK.
	 */
	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	/**
	 * Inisialisasi komponen dan logika bisnis setelah semua elemen ZUL terhubung.
	 *
	 * <b>Tujuan:</b><br>
	 * Titik masuk utama inisialisasi halaman jenis perbaikan aset. Menyiapkan hak akses,
	 * paginasi, tombol toolbar (cetak dan upload), serta timer auto-refresh.<br><br>
	 *
	 * <b>Cara kerja:</b><br>
	 * (1) Memanggil {@code super.doAfterCompose} dan menginisialisasi bahasa.
	 * (2) Mengatur visibilitas tombol tambah berdasarkan hak akses CREATE.
	 * (3) Membaca hak akses UPDATE dan DELETE ke flag edit dan delete.
	 * (4) Mengatur event listener paginasi.
	 * (5) Mendaftarkan tombol cetak data ke toolbar.
	 * (6) Mendaftarkan tombol upload data ke toolbar (visibel hanya jika punya CREATE+UPDATE+DELETE).
	 * (7) Mendaftarkan timer default untuk auto-refresh grid.<br><br>
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Kolom yang diekspor via tombol cetak/upload didefinisikan di array {@code contents}.
	 * Perbarui array ini jika kolom ekspor berubah.
	 *
	 * @param comp Komponen akar halaman ZUL yang telah di-autowire.
	 * @throws Exception Jika terjadi kesalahan inisialisasi ZK atau Hibernate.
	 */
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		Common.initLaguage();

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

		String[] contents = new String[] { "id", "nama", "nomorSurat", "keterangan", "aktif" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, JenisPerbaikanAsset.class, contents);
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
	 * Renderer baris grid untuk daftar jenis perbaikan aset.
	 *
	 * <b>Tujuan:</b><br>
	 * Merender setiap entri {@code JenisPerbaikanAsset} ke dalam baris grid ZK.
	 * Menampilkan nama, nomor agenda, keterangan, checkbox aktif, dan tombol aksi.<br><br>
	 *
	 * <b>Cara kerja:</b><br>
	 * Checkbox aktif dapat diklik langsung untuk mengubah status tanpa membuka form.
	 * Tombol ubah/hapus menggunakan {@code Common.copyEditDeleteButtons}.<br><br>
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Penambahan kolom baru memerlukan sinkronisasi dengan header di ZUL.
	 */
	class JenisPerbaikanAssetRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * Merender satu baris data jenis perbaikan aset ke dalam baris grid ZK.
		 *
		 * <b>Tujuan:</b><br>
		 * Mengisi sel-sel baris grid dengan data entitas {@code JenisPerbaikanAsset},
		 * termasuk checkbox aktif interaktif dan tombol ubah/hapus.<br><br>
		 *
		 * <b>Cara kerja:</b><br>
		 * Menampilkan nama (via RevisiHelper untuk audit trail), contoh format nomor agenda,
		 * keterangan, checkbox aktif yang mengubah status langsung ke database saat diklik,
		 * dan tombol ubah/hapus standar.<br><br>
		 *
		 * <b>Penanganan error:</b><br>
		 * Kegagalan penyimpanan status aktif diteruskan ke ZK framework.
		 *
		 * @param arg0 Baris grid ZK yang akan diisi.
		 * @param arg1 Objek {@code JenisPerbaikanAsset} yang dirender.
		 * @throws Exception Jika terjadi kesalahan pembuatan komponen ZK.
		 */
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final JenisPerbaikanAsset jenisPerbaikanAsset = (JenisPerbaikanAsset) arg1;

			RevisiHelper.createNewRevisi(JenisPerbaikanAsset.class, jenisPerbaikanAsset,
					jenisPerbaikanAsset.getNama()).setParent(arg0);
			new Label(jenisPerbaikanAsset.getNomorSurat() == null ? ""
					: jenisPerbaikanAsset.getNomorSurat().getContohFormat()).setParent(arg0);
			new Label(jenisPerbaikanAsset.getKeterangan()).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(jenisPerbaikanAsset.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					jenisPerbaikanAsset.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(jenisPerbaikanAsset);
				}
			});

			Common.copyEditDeleteButtons(edit, delete, jenisPerbaikanAsset, JenisPerbaikanAssetAction.this)
					.setParent(arg0);

		}

	}

	/**
	 * Menangani klik tombol "Tambah" untuk membuat jenis perbaikan aset baru.
	 *
	 * <b>Tujuan:</b><br>
	 * Membuka form tambah jenis perbaikan aset baru dengan instance kosong,
	 * lalu menampilkan jendela modal.<br><br>
	 *
	 * <b>Cara kerja:</b><br>
	 * Memanggil {@code init(new JenisPerbaikanAsset())} dengan objek kosong,
	 * kemudian menampilkan {@code addWindow} dalam mode modal. Judul akan
	 * otomatis "Tambah Jenis Perbaikan Alat/Fasilitas" karena ID null.<br><br>
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Tidak ada validasi pre-tambah di metode ini.
	 *
	 * @param event Event ZK dari klik tombol tambah.
	 * @throws Exception Jika terjadi kesalahan inisialisasi form atau jendela modal.
	 */
	public void onAdd(Event event) throws Exception {
		init(new JenisPerbaikanAsset());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	/**
	 * Implementasi antarmuka {@code DataInitDefault} untuk inisialisasi form dari objek generik.
	 *
	 * <b>Tujuan:</b><br>
	 * Dipanggil oleh mekanisme generik ubah data dengan {@code GeneralValueObject} yang
	 * merupakan entitas {@code JenisPerbaikanAsset}. Mendelegasikan ke {@code init(JenisPerbaikanAsset)}
	 * setelah melakukan cast tipe.<br><br>
	 *
	 * <b>Cara kerja:</b><br>
	 * Meng-cast {@code obj} ke {@code JenisPerbaikanAsset}, menyimpan ke field instance,
	 * memanggil {@code init} untuk membangun form, lalu menampilkan jendela modal.<br><br>
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Kontrak antarmuka {@code DataInitDefault}. Dipanggil oleh {@code Common.copyEditDeleteButtons}.
	 *
	 * @param obj Entitas {@code JenisPerbaikanAsset} sebagai {@code GeneralValueObject}.
	 * @throws Exception Jika cast gagal atau inisialisasi form mengalami kesalahan.
	 */
	@Override
	public void init(GeneralValueObject obj) throws Exception {
		jenisPerbaikanAsset = (JenisPerbaikanAsset) obj;
		init(jenisPerbaikanAsset);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	/**
	 * Menginisialisasi dan membangun form tambah/ubah jenis perbaikan aset secara programatik.
	 *
	 * <b>Tujuan:</b><br>
	 * Membersihkan dan membangun ulang form lengkap untuk jenis perbaikan aset. Form ini
	 * mencakup: nama, nomor agenda, keterangan, widget upload file laporan form (jrxml),
	 * widget upload file laporan rekap (jrxml), dan sub-grid checkbox kelompok parameter.<br><br>
	 *
	 * <b>Cara kerja:</b><br>
	 * (1) Mengatur judul jendela berdasarkan mode (tambah/ubah).
	 * (2) Membersihkan konten jendela dan membangun Borderlayout baru.
	 * (3) Textbox nama jenis perbaikan (wajib).
	 * (4) Banbox nomor agenda yang readonly, diisi dari master NomorSurat.
	 * (5) Textbox keterangan multi-baris.
	 * (6) Widget upload lampiran file laporan form jrxml via {@code LampiranLain.createDownloadUploadFileLain}
	 *     dengan tipe {@code FILE_JRXML_LAYOUT_JENIS_FORM_PERBAIKAN}, callback ke {@code lampiran1}.
	 * (7) Widget upload lampiran file laporan rekap jrxml dengan tipe
	 *     {@code FILE_JRXML_LAYOUT_JENIS_PERBAIKAN}, callback ke {@code lampiran}.
	 * (8) Sub-grid checkbox kelompok parameter via {@code initKelompokParameterTambahanPerbaikanAsset}.
	 * (9) Toolbar dengan tombol Batal dan Simpan.<br><br>
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Field {@code lampiran} dan {@code lampiran1} di-reset ke null di awal inisialisasi
	 * agar tidak ada referensi stale dari sesi edit sebelumnya. Urutan widget upload
	 * penting: form (lampiran1) didaftarkan sebelum rekap (lampiran).
	 *
	 * @param jenisPerbaikanAsset Entitas yang akan ditampilkan/diedit di form.
	 */
	private void init(JenisPerbaikanAsset jenisPerbaikanAsset) {
		this.jenisPerbaikanAsset = jenisPerbaikanAsset;
		addWindow.setTitle(jenisPerbaikanAsset.getId() == null ? "Tambah Jenis Perbaikan Alat/Fasilitas" : "Ubah Jenis Perbaikan Alat/Fasilitas");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(center);
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

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Perbaikan Sarpras *"));
		row.appendChild(nama = new Textbox(jenisPerbaikanAsset.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nomor Agenda *"));
		row.appendChild(nomorSurat = new AmbilDataNomorSuratBanbox());
		nomorSurat.setAttribute("nomorSurat", jenisPerbaikanAsset.getNomorSurat());
		nomorSurat.setValue(jenisPerbaikanAsset.getNomorSurat() == null ? ""
				: jenisPerbaikanAsset.getNomorSurat().getNama());
		nomorSurat.setWidth("90%");
		nomorSurat.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(jenisPerbaikanAsset.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		lampiran1 = null;
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("File Laporan Form (jrxml atau jasper)"));
		Hbox hbox = new Hbox();
		hbox.setParent(row);
		LampiranLain.createDownloadUploadFileLain(hbox, jenisPerbaikanAsset.getId(),
				LampiranLain.FILE_JRXML_LAYOUT_JENIS_FORM_PERBAIKAN, "File Laporan form jrxml", false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						lampiran1 = (LampiranLain) arg0.getData();
					}
				}, null, false, false, false, true);


		lampiran = null;
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("File Laporan Rekap (jrxml atau jasper)"));
		 hbox = new Hbox();
		hbox.setParent(row);
		LampiranLain.createDownloadUploadFileLain(hbox, jenisPerbaikanAsset.getId(),
				LampiranLain.FILE_JRXML_LAYOUT_JENIS_PERBAIKAN, "File Laporan rekap jrxml", false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						lampiran = (LampiranLain) arg0.getData();
					}
				}, null, false, false, false, true);


		initKelompokParameterTambahanPerbaikanAsset(rows);

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
		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
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
	 * Membangun sub-grid checkbox pemilihan kelompok parameter tambahan perbaikan aset.
	 *
	 * <b>Tujuan:</b><br>
	 * Menambahkan section di dalam form jenis perbaikan yang menampilkan semua kelompok
	 * parameter yang tersedia sebagai daftar checkbox. Pengguna dapat memilih kelompok
	 * parameter mana saja yang akan digunakan dalam formulir perbaikan untuk jenis ini.<br><br>
	 *
	 * <b>Cara kerja:</b><br>
	 * (1) Membuat baris form spanning 2 kolom untuk menampung sub-grid.
	 * (2) Mendapatkan semua kelompok parameter dari {@code ConstantValues} (cache statis).
	 * (3) Jika entitas sudah ada, me-refresh dari Hibernate untuk mendapat data terkini.
	 * (4) Mengambil set kelompok yang sudah terpilih dan mengekstrak ID-nya ke Set<Long>
	 *     untuk pengecekan O(1).
	 * (5) Membuat checkbox untuk setiap kelompok, mencentang jika ID-nya ada di set terpilih.
	 * (6) Event listener setiap checkbox menambah atau menghapus kelompok dari
	 *     {@code selectedKelompokParameterTambahanPerbaikanAsset}.<br><br>
	 *
	 * <b>Penanganan error:</b><br>
	 * Menggunakan iterasi dengan break saat menghapus untuk menghindari
	 * {@code ConcurrentModificationException}.<br><br>
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Anotasi {@code @SuppressWarnings("deprecation")} diperlukan karena
	 * {@code ZkCompat.setSpans} menggunakan API yang deprecated di versi ZK yang lebih baru.
	 * Metode ini menggunakan ConstantValues sebagai cache sehingga perubahan kelompok
	 * di database tidak langsung terlihat tanpa restart atau invalidasi cache.
	 *
	 * @param rows Container Rows ZK tempat sub-grid checkbox akan ditambahkan.
	 */
	@SuppressWarnings("deprecation")
	private void initKelompokParameterTambahanPerbaikanAsset(Rows rows) {
		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		final MyGrid subGrid = new MyGrid();
		row.appendChild(subGrid);

		Columns subColumns = new Columns();
		subColumns.setParent(subGrid);
		Column c = new Column("Parameter Perbaikan Alat/Fasilitas");
		subColumns.appendChild(c);

		Rows subRows = new Rows();
		subRows.setParent(subGrid);

		MyFormRow subRow = new MyFormRow();
		subRow.setStyle("border:0px;background: transparent;");
		subRow.setParent(subRows);
		subRow.setValign("top");

		@SuppressWarnings("unchecked")
		Map<Long, KelompokParameterTambahanPerbaikanAsset> kelompokParameterTambahanPerbaikanAssets = ConstantValues
				.ambilBerdasarClass(KelompokParameterTambahanPerbaikanAsset.class);

		if (jenisPerbaikanAsset != null && jenisPerbaikanAsset.getId() != null) {
			HibernateUtil.currentSession().refresh(jenisPerbaikanAsset);
		}

		selectedKelompokParameterTambahanPerbaikanAsset = this.jenisPerbaikanAsset
				.getKelompokParameterTambahanPerbaikanAssets();
		Set<Long> ids = new HashSet<Long>();
		for (KelompokParameterTambahanPerbaikanAsset v : selectedKelompokParameterTambahanPerbaikanAsset) {
			ids.add(v.getId());
		}

		System.out.println("ids ->" + ids);

		Vbox vboxSkala = new Vbox();
		vboxSkala.setPack("top");
		vboxSkala.setParent(subRow);
		for (final KelompokParameterTambahanPerbaikanAsset kelompokParameterTambahanPerbaikanAsset : kelompokParameterTambahanPerbaikanAssets
				.values()) {
			final Checkbox checkbox = new Checkbox(kelompokParameterTambahanPerbaikanAsset.getNama());
			checkbox.setParent(vboxSkala);
			checkbox.setChecked(ids.contains(kelompokParameterTambahanPerbaikanAsset.getId()));
			checkbox.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (checkbox.isChecked()) {
						selectedKelompokParameterTambahanPerbaikanAsset
								.add(kelompokParameterTambahanPerbaikanAsset);
					} else {

						for (KelompokParameterTambahanPerbaikanAsset a : selectedKelompokParameterTambahanPerbaikanAsset) {
							if (a.getId().equals(kelompokParameterTambahanPerbaikanAsset.getId())) {
								selectedKelompokParameterTambahanPerbaikanAsset.remove(a);
								break;
							}
						}

					}

					System.out.println("selectedKelompokParameterTambahanPerbaikanAsset => "
							+ selectedKelompokParameterTambahanPerbaikanAsset);
				}
			});
		}
	}

	/**
	 * Menyimpan data jenis perbaikan aset ke database setelah validasi.
	 *
	 * <b>Tujuan:</b><br>
	 * Memvalidasi field wajib (nama dan nomor agenda), menyimpan entitas utama
	 * {@code JenisPerbaikanAsset} ke sesi Hibernate biasa, kemudian menyimpan
	 * referensi file lampiran jrxml (laporan form dan laporan rekap) ke sesi
	 * {@code StreamingHibernateUtil} khusus BLOB.<br><br>
	 *
	 * <b>Cara kerja:</b><br>
	 * (1) Memvalidasi nama tidak kosong; jika kosong, return false.
	 * (2) Memvalidasi nomor agenda sudah dipilih; jika belum, return false.
	 * (3) Me-load ulang entitas jika mode ubah (untuk menghindari stale state).
	 * (4) Mengisi semua field entitas dari form (nama, keterangan, kelompok parameter, nomor surat).
	 * (5) Menyimpan entitas utama via {@code Common.refreshSaveOrUpdate}.
	 * (6) Membuka sesi streaming dan memperbarui referensi (ref = ID jenis perbaikan)
	 *     untuk lampiran file laporan rekap (lampiran) jika ada.
	 * (7) Memperbarui referensi untuk lampiran file laporan form (lampiran1) jika ada.
	 * (8) Menutup sesi streaming; jika ada error, melakukan rollback dan melaporkan ke admin.<br><br>
	 *
	 * <b>Return:</b><br>
	 * {@code true} jika berhasil disimpan; {@code false} jika validasi gagal.<br><br>
	 *
	 * <b>Penanganan error:</b><br>
	 * Error pada penyimpanan file lampiran ditangkap, rollback dilakukan pada sesi streaming,
	 * dan error dilaporkan via {@code Common.tampilErrorJikaAdmin}. Data utama tetap tersimpan
	 * meskipun lampiran gagal.<br><br>
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Penggunaan dua sesi Hibernate berbeda (currentSession dan StreamingHibernateUtil)
	 * memerlukan manajemen transaksi yang hati-hati. Pastikan sesi streaming selalu ditutup
	 * di blok finally atau setelah try-catch.
	 *
	 * @param event Event ZK dari klik tombol Simpan.
	 * @return {@code true} jika berhasil disimpan; {@code false} jika validasi gagal.
	 * @throws Exception Jika terjadi kesalahan Hibernate saat menyimpan data utama.
	 */
	public boolean onSave(Event event) throws Exception {
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Nama Jenis Perbaikan belum diisi. Langkah yang dapat dilakukan: (1) Isi field Nama dengan nama jenis perbaikan yang sesuai; (2) Nama jenis perbaikan digunakan untuk mengklasifikasikan pekerjaan perbaikan aset; (3) ulangi proses simpan ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (nomorSurat.getAttribute("nomorSurat") == null) {
			MyMessageboxConfig.show("Mohon maaf, Format Nomor Agenda belum dipilih. Langkah yang dapat dilakukan: (1) Klik tombol pilih pada field Format Nomor Agenda; (2) Pilih format nomor yang sesuai dari daftar; (3) ulangi proses simpan ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (jenisPerbaikanAsset.getId() != null) {
			jenisPerbaikanAsset = (JenisPerbaikanAsset) session.load(JenisPerbaikanAsset.class,
					jenisPerbaikanAsset.getId());

		}

		jenisPerbaikanAsset.setNama(nama.getValue());
		jenisPerbaikanAsset.setKeterangan(keterangan.getValue());
		jenisPerbaikanAsset
				.setKelompokParameterTambahanPerbaikanAssets(selectedKelompokParameterTambahanPerbaikanAsset);
		jenisPerbaikanAsset.setNomorSurat((NomorSurat) nomorSurat.getAttribute("nomorSurat"));
		Common.refreshSaveOrUpdate(session, jenisPerbaikanAsset);

		try {
			session = StreamingHibernateUtil.getInstance().currentSession();

			if (lampiran != null && lampiran.getId() != null) {
				session.refresh(lampiran);
				lampiran.setRef(jenisPerbaikanAsset.getId());

				session.getTransaction().begin();
				session.update(lampiran);
				session.getTransaction().commit();
			}


			if (lampiran1 != null && lampiran1.getId() != null) {
				session.refresh(lampiran1);
				lampiran1.setRef(jenisPerbaikanAsset.getId());

				session.getTransaction().begin();
				session.update(lampiran1);
				session.getTransaction().commit();
			}

			StreamingHibernateUtil.getInstance().closeSession();
		} catch (Exception e) {
			StreamingHibernateUtil.getInstance().rollbackTransaction();
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("penyimpanan berkas lampiran Jenis Perbaikan Asset", e,
					new String[] {
							"Data utama Jenis Perbaikan Asset ini tetap tersimpan; hanya lampiran yang gagal disimpan.",
							"Periksa kembali ukuran dan format berkas lampiran, lalu unggah ulang berkas tersebut.",
							"Pastikan koneksi jaringan Bapak/Ibu stabil selama proses unggah berlangsung." });
		}

		return true;
	}

	/**
	 * Membangun Criteria Hibernate untuk kueri daftar jenis perbaikan aset.
	 *
	 * <b>Tujuan:</b><br>
	 * Menyusun filter Hibernate sederhana berdasarkan nama jenis perbaikan.
	 * Digunakan oleh {@code onSearchDefault} dan {@code Common.initPaging}.<br><br>
	 *
	 * <b>Cara kerja:</b><br>
	 * Membuat Criteria dari {@code JenisPerbaikanAsset}. Filter nama menggunakan
	 * ILIKE case-insensitive partial match. Jika filter nama kosong, menampilkan
	 * semua data. Urutan default adalah ascending berdasarkan nama.<br><br>
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Filter tambahan (misalnya berdasarkan status aktif atau kelompok parameter)
	 * dapat ditambahkan sebagai klausa {@code .add(...)} baru.
	 *
	 * @param order {@code true} untuk menambahkan ORDER BY nama ASC.
	 * @return Objek {@code Criteria} Hibernate siap dieksekusi.
	 */
	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(JenisPerbaikanAsset.class);

		if (order)
			criteria.addOrder(Order.asc("nama"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))

		;
		return criteria;
	}

	/**
	 * Memuat ulang data grid jenis perbaikan aset berdasarkan filter saat ini.
	 *
	 * <b>Tujuan:</b><br>
	 * Implementasi {@code DataSearchDefault}. Me-refresh tampilan grid setelah
	 * perubahan filter atau operasi CRUD. Dipanggil juga oleh timer auto-refresh.<br><br>
	 *
	 * <b>Cara kerja:</b><br>
	 * (1) Menghitung total data dan memperbarui paginasi.
	 * (2) Mengeksekusi kueri dengan criteria terurut, dibatasi per halaman.
	 * (3) Menetapkan renderer dan model ke grid ZK.<br><br>
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Dipanggil dari berbagai titik termasuk timer auto-refresh. Pastikan
	 * implementasi efisien untuk menghindari beban berlebih di database.
	 *
	 * @param event Event ZK yang memicu pencarian ulang, bisa {@code null}.
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<JenisPerbaikanAsset> jenisPerbaikanAsset = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(jenisPerbaikanAsset);
		grid.setRowRenderer(new JenisPerbaikanAssetRenderer());
		grid.setModelCheckMobile(strset);

	}

}
