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
import org.zkoss.zul.Borderlayout;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyCheckboxConfig;

import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;

import ais.ui.util.MyColumnConfig;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyDetail;
import ais.ui.util.MyGrid;

import org.zkoss.zul.Hbox;
import ais.ui.util.MyInclude;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import ais.ui.util.MyMessageboxConfig;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.database.dao.DaoFactory;
import ais.database.dao.asset.PemilikAssetDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.PerguruanTinggi;
import ais.database.model.asset.PemilikAsset;

/**
 * <h3>PemilikAssetAction — Kontroler CRUD Pemilik Aset</h3>
 *
 * <b>Untuk apa:</b><br>
 * Kelas ini adalah kontroler ZK (ZKoss Composer) untuk halaman manajemen pemilik aset
 * di modul manajemen aset eCampus. Pemilik aset adalah data referensi yang mendefinisikan
 * entitas atau unit yang memiliki dan bertanggung jawab atas aset tertentu dalam institusi.
 * Dalam konteks perguruan tinggi, pemilik aset dapat berupa: departemen/program studi
 * (Jurusan), fakultas (Fakultas), perguruan tinggi/institusi (PerguruanTinggi), atau
 * kombinasinya. Konfigurasi multi-level kepemilikan ini memungkinkan pelacakan aset
 * yang lebih granular sesuai struktur organisasi institusi. Setiap pemilik aset memiliki
 * status aktif yang dapat diubah langsung dari grid tanpa membuka form edit, serta
 * sub-panel expandable yang menampilkan daftar aset yang dimiliki secara lazy-loaded.<br><br>
 *
 * <b>Cara kerja:</b><br>
 * Halaman ini menggunakan pola CRUD dengan GenericAutowireComposer ZKoss 5.5. Setiap
 * baris grid memiliki komponen {@code MyDetail} yang ketika dibuka akan memuat daftar
 * aset milik pemilik tersebut (asset.zul) secara lazy via MyInclude, dengan menyimpan
 * referensi pemilik ke atribut sesi ZK. Form tambah/ubah dibangun programatik di metode
 * {@code init(PemilikAsset)} dengan combobox Fakultas, Jurusan, dan PerguruanTinggi yang
 * saling terkait. Penyimpanan data menggunakan {@code PemilikAssetDao} (DAO layer) bukan
 * langsung ke Hibernate session. Validasi duplikasi nama dilakukan via query count sebelum
 * simpan. Filter pencarian mendukung nama, status aktif, fakultas, dan jurusan.<br><br>
 *
 * <b>Threading:</b><br>
 * Tidak ada pemrosesan latar belakang. Semua operasi dilakukan di thread utama ZK.
 * Sub-panel daftar aset dalam MyDetail dimuat secara lazy saat pengguna membuka detail,
 * menggunakan sesi ZK yang sama. Checkbox aktif di grid menggunakan
 * {@code Common.refreshSaveOrUpdate} yang beroperasi di thread utama ZK.<br><br>
 *
 * <b>Pemeliharaan:</b><br>
 * Kelas ini menggunakan DAO layer ({@code PemilikAssetDao}) untuk operasi simpan dan ubah,
 * berbeda dari pola kebanyakan Action lain yang menggunakan {@code Common.refreshSaveOrUpdate}.
 * Filter pencarian mendukung kombinasi nama dan status aktif. Combobox Jurusan difilter
 * berdasarkan Fakultas yang dipilih dan status aktif. Combobox PerguruanTinggi menampilkan
 * semua perguruan tinggi yang terdaftar. Duplikasi nama dicegah via query count dengan
 * eksklusi ID entitas saat ini pada mode ubah.
 *
 * @author eCampus Dev Team
 * @version 1.0
 */
public class PemilikAssetAction extends GenericAutowireComposer {

	/**
	 * Serial version UID untuk serialisasi kelas ZK Composer ini.
	 */
	private static final long serialVersionUID = -5779730267402400328L;

	/** Jendela modal untuk form tambah/ubah pemilik aset. */
	private MyWindow addWindow;

	/** Komponen paginasi untuk grid daftar pemilik aset. */
	private Paging paging;

	/** Grid utama yang menampilkan daftar pemilik aset. */
	private MyGrid grid;

	/** Kotak teks filter pencarian berdasarkan nama pemilik aset. */
	private Textbox searchnama;

	/** Combobox filter berdasarkan fakultas pemilik aset. */
	private Combobox searchfakultas;

	/** Combobox filter berdasarkan program studi/jurusan pemilik aset. */
	private Combobox searchjurusan;

	/** Checkbox filter untuk menampilkan hanya data aktif atau semua data. */
	private Checkbox searchaktif;

	/** Kotak teks input nama pemilik aset pada form tambah/ubah. */
	private Textbox nama;

	/** Kotak teks keterangan tambahan pada form tambah/ubah. */
	private Textbox keterangan;

	/**
	 * Combobox pemilihan program studi/jurusan pada form tambah/ubah.
	 * Diisi ulang saat pilihan Fakultas berubah.
	 */
	private Combobox jurusan;

	/**
	 * Combobox pemilihan fakultas pada form tambah/ubah.
	 * Perubahan pilihan mempengaruhi konten combobox jurusan.
	 */
	private Combobox fakultas;

	/** Combobox pemilihan perguruan tinggi/institusi pada form tambah/ubah. */
	private Combobox perguruanTinggi;

	/** Penanda apakah pengguna memiliki hak ubah data. */
	private boolean edit = false;

	/** Penanda apakah pengguna memiliki hak hapus data. */
	private boolean delete = false;

	/** Entitas pemilik aset yang sedang dikelola (tambah atau ubah). */
	private PemilikAsset pemilikAsset;

	/** Tombol toolbar tambah data baru. */
	private MyToolbarbuttonConfig add;

	/**
	 * Pemeriksaan keamanan sebelum halaman dikompilasi oleh ZK.
	 *
	 * <b>Tujuan:</b><br>
	 * Memastikan pengguna yang mengakses halaman pemilik aset telah memiliki sesi
	 * yang valid dan hak akses sesuai sebelum proses komposisi halaman dimulai.
	 * Gate keamanan pertama dalam siklus hidup halaman ZK ini.<br><br>
	 *
	 * <b>Cara kerja:</b><br>
	 * Memanggil {@code Common.doCheckSecurity()} untuk validasi sesi dan hak akses,
	 * lalu memanggil {@code super.doBeforeCompose} untuk melanjutkan proses ZK normal.<br><br>
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
	 * Titik masuk utama inisialisasi halaman pemilik aset. Memeriksa sesi dan hak akses,
	 * mengatur visibilitas tombol, memuat data awal ke grid, dan mendaftarkan listener
	 * paginasi.<br><br>
	 *
	 * <b>Cara kerja:</b><br>
	 * (1) Memanggil {@code super.doAfterCompose} dan menginisialisasi bahasa.
	 * (2) Memeriksa sesi dan hak akses READ; jika tidak valid, mengarahkan ke logoff
	 *     dan menghentikan eksekusi lebih lanjut.
	 * (3) Mengatur visibilitas dan tooltip tombol tambah berdasarkan hak akses CREATE.
	 * (4) Membaca hak akses UPDATE dan DELETE ke flag edit dan delete.
	 * (5) Memanggil {@code onSearchDefault} untuk memuat data awal ke grid.
	 * (6) Mengatur event listener paginasi agar setiap pergantian halaman memicu
	 *     {@code onSearchDefault}.<br><br>
	 *
	 * <b>Penanganan error:</b><br>
	 * Jika sesi tidak valid atau hak akses READ tidak ada, pengguna diarahkan ke logoff
	 * dan method berhenti lebih awal dengan {@code return} setelah membersihkan sesi.<br><br>
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Tidak ada tombol cetak atau upload di halaman ini. Jika diperlukan, tambahkan
	 * menggunakan pola yang sama dengan KelompokAssetAction.
	 *
	 * @param comp Komponen akar halaman ZUL yang telah di-autowire.
	 * @throws Exception Jika terjadi kesalahan inisialisasi ZK atau Hibernate.
	 */
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		Common.initLaguage();
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});
	}

	/**
	 * Renderer baris grid untuk daftar pemilik aset.
	 *
	 * <b>Tujuan:</b><br>
	 * Merender setiap entri {@code PemilikAsset} ke dalam baris grid ZK dengan
	 * komponen detail expandable, informasi kepemilikan, checkbox aktif interaktif,
	 * dan tombol ubah/hapus manual.<br><br>
	 *
	 * <b>Cara kerja:</b><br>
	 * Setiap baris memiliki {@code MyDetail} yang ketika dibuka akan lazy-load sub-panel
	 * daftar aset (asset.zul) dengan menyimpan pemilik ke atribut sesi. Kolom berikutnya
	 * menampilkan nama, nama fakultas, nama jurusan, nama perguruan tinggi, checkbox aktif
	 * (dapat diklik langsung), keterangan, dan toolbar aksi (ubah + hapus manual).
	 * Tombol ubah dan hapus diimplementasikan manual (bukan via Common.copyEditDeleteButtons)
	 * karena PemilikAsset tidak mengimplementasikan DataInitDefault.<br><br>
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Penambahan kolom baru memerlukan sinkronisasi dengan header di ZUL.
	 * Sub-panel daftar aset menggunakan atribut sesi "PemilikAsset" sebagai filter.
	 */
	class PemilikAssetRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * Merender satu baris data pemilik aset ke dalam baris grid ZK.
		 *
		 * <b>Tujuan:</b><br>
		 * Mengisi sel-sel baris grid dengan data entitas {@code PemilikAsset}, termasuk
		 * komponen detail expandable untuk daftar aset dan tombol aksi ubah/hapus.<br><br>
		 *
		 * <b>Cara kerja:</b><br>
		 * (1) Membuat MyDetail dengan event listener lazy-loading sub-panel asset.zul.
		 * (2) Label nama via RevisiHelper, fakultas, jurusan, perguruan tinggi.
		 * (3) Checkbox aktif dengan event listener onCheck yang langsung menyimpan ke DB.
		 * (4) Label keterangan.
		 * (5) Hbox toolbar dengan tombol ubah (membuka form init) dan hapus (dengan konfirmasi).
		 * Penghapusan gagal karena relasi FK ditangkap dan ditampilkan sebagai pesan error.<br><br>
		 *
		 * <b>Penanganan error:</b><br>
		 * Kegagalan hapus karena relasi FK ditangkap dan dilaporkan ke pengguna.
		 * Kegagalan penyimpanan checkbox aktif diteruskan ke ZK.
		 *
		 * @param arg0 Baris grid ZK yang akan diisi.
		 * @param arg1 Objek {@code PemilikAsset} yang dirender.
		 * @throws Exception Jika terjadi kesalahan pembuatan komponen ZK.
		 */
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final PemilikAsset pemilikAsset = (PemilikAsset) arg1;

			final MyDetail detail = new MyDetail();
			detail.setParent(arg0);
			detail.addEventListener("onOpen", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Common.clear(detail);
					if (detail.isOpen()) {

						ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
						groupbox.setStyle("min-height: 200px;");
						groupbox.setParent(detail);
						groupbox.appendChild(new MyCaptionStyled("Daftar Aset"));

						session.setAttribute("PemilikAsset", pemilikAsset);
						MyInclude iframe = new MyInclude("/pages/master/asset/asset.zul");
						iframe.setHeight("650px");
						iframe.setWidth("100%");
						iframe.setParent(groupbox);
					}
				}
			});

			RevisiHelper.createNewRevisi(PemilikAsset.class, pemilikAsset, pemilikAsset.getNama()).setParent(arg0);
			new Label(pemilikAsset.getFakultas() == null ? "" : pemilikAsset.getFakultas().getNama()).setParent(arg0);
			new Label(pemilikAsset.getJurusan() == null ? "" : pemilikAsset.getJurusan().getNama()).setParent(arg0);
			new Label(pemilikAsset.getPerguruanTinggi() == null ? "" : pemilikAsset.getPerguruanTinggi().getNama())
					.setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(pemilikAsset.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					pemilikAsset.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(pemilikAsset);
				}
			});
			new Label(pemilikAsset.getKeterangan()).setParent(arg0);

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(pemilikAsset);
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			button.setParent(toolbar);

			button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
			button.setVisible(delete);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = new Integer(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {

											Common.refreshDelete(pemilikAsset);

											onSearchDefault(event);
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
			button.setParent(toolbar);
			toolbar.setParent(arg0);
		}

	}

	/**
	 * Menangani klik tombol "Tambah" untuk membuat pemilik aset baru.
	 *
	 * <b>Tujuan:</b><br>
	 * Membuka form tambah pemilik aset baru dengan instance kosong {@code PemilikAsset},
	 * lalu menampilkan jendela modal form.<br><br>
	 *
	 * <b>Cara kerja:</b><br>
	 * Memanggil {@code init(new PemilikAsset())} dengan objek kosong, kemudian menampilkan
	 * {@code addWindow} dalam mode modal. Judul akan otomatis "Tambah PemilikAsset"
	 * karena ID entitas masih null.<br><br>
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Tidak ada validasi pre-tambah. Semua validasi dilakukan di {@code onSave}.
	 *
	 * @param event Event ZK dari klik tombol tambah.
	 * @throws Exception Jika terjadi kesalahan inisialisasi form atau jendela modal.
	 */
	public void onAdd(Event event) throws Exception {
		init(new PemilikAsset());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	/**
	 * Menginisialisasi dan membangun form tambah/ubah pemilik aset secara programatik.
	 *
	 * <b>Tujuan:</b><br>
	 * Membersihkan dan membangun ulang form input pemilik aset yang mencakup nama,
	 * fakultas, program studi/jurusan (difilter berdasarkan fakultas), perguruan tinggi,
	 * dan keterangan.<br><br>
	 *
	 * <b>Cara kerja:</b><br>
	 * (1) Menginisialisasi combobox Fakultas dan Jurusan via {@code Common.initFakultasDanJurusan}
	 *     yang mengatur hubungan dinamis antar kedua combobox.
	 * (2) Mengatur judul jendela dan membersihkan konten lama.
	 * (3) Membangun Borderlayout dengan Center (grid form 2 kolom) dan South (toolbar).
	 * (4) Textbox nama pemilik aset (wajib).
	 * (5) Combobox Fakultas diisi dengan semua fakultas aktif, nilai awal dari entitas
	 *     atau fakultas pengguna saat ini jika pemilik baru.
	 * (6) Combobox Jurusan diisi berdasarkan fakultas yang dipilih, nilai awal dari
	 *     entitas atau jurusan pengguna saat ini.
	 * (7) Combobox PerguruanTinggi dibuat baru dan diisi dengan semua perguruan tinggi.
	 * (8) Textbox keterangan multi-baris.
	 * (9) Toolbar dengan tombol Batal dan Simpan.<br><br>
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Combobox Jurusan difilter berdasarkan dua kondisi: status aktif (null atau true)
	 * dan fakultas yang dipilih. Jika fakultas filter berubah, combobox Jurusan harus
	 * di-refresh secara manual atau menggunakan mekanisme yang sudah ada di
	 * {@code Common.initFakultasDanJurusan}.
	 *
	 * @param pemilikAsset Entitas yang akan ditampilkan/diedit di form.
	 */
	private void init(PemilikAsset pemilikAsset) {

		fakultas = new Combobox();
		jurusan = new Combobox();
		Common.initFakultasDanJurusan(fakultas, jurusan, searchfakultas, searchjurusan);

		this.pemilikAsset = pemilikAsset;
		addWindow.setTitle(pemilikAsset.getId() == null ? "Tambah PemilikAsset" : "Ubah PemilikAsset");
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
		column.setWidth("35%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Pemilik Asset"));
		row.appendChild(nama = new Textbox(pemilikAsset.getNama() == null ? "" : pemilikAsset.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		Common.insertCombo(fakultas, new String[] { "nama", "kode" }, Fakultas.class, Restrictions.eq("aktif", true));
		Common.selectComboItem(fakultas,
				pemilikAsset.getFakultas() == null || pemilikAsset.getFakultas() == null
						? Common.getCurrentUser().ambilFakultas()
						: pemilikAsset.getFakultas());
		row.appendChild(fakultas);
		fakultas.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		Common.clear(jurusan);
		Common.insertCombo(jurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)), Restrictions.eq(
						"fakultas", fakultas.getSelectedItem() == null ? null : fakultas.getSelectedItem().getValue()));
		Common.pilihJurusan(jurusan,
				pemilikAsset.getJurusan() == null ? Common.getCurrentUser().ambilJurusan() : pemilikAsset.getJurusan());
		row.appendChild(jurusan);
		jurusan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Instansi/Perguruan Tinggi"));
		Common.insertCombo(perguruanTinggi = new Combobox(), new String[] { "nama", "kodePerguruanTinggi" },
				PerguruanTinggi.class);
		Common.selectComboItem(perguruanTinggi, pemilikAsset.getPerguruanTinggi());
		row.appendChild(perguruanTinggi);
		perguruanTinggi.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(
				keterangan = new Textbox(pemilikAsset.getKeterangan() == null ? "" : pemilikAsset.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);
		keterangan.setRows(3);

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
	 * Menyimpan data pemilik aset ke database setelah validasi.
	 *
	 * <b>Tujuan:</b><br>
	 * Memvalidasi input (nama wajib dan tidak duplikat), kemudian menyimpan atau memperbarui
	 * entitas {@code PemilikAsset} ke database melalui {@code PemilikAssetDao}.<br><br>
	 *
	 * <b>Cara kerja:</b><br>
	 * (1) Memvalidasi nama tidak kosong; jika kosong, menampilkan peringatan dan return false.
	 * (2) Memanggil {@code checkNamaPemilikAsset()} untuk cek duplikasi nama; jika duplikat,
	 *     menampilkan peringatan dan return false.
	 * (3) Mengambil DAO pemilik aset dari DaoFactory.
	 * (4) Jika mode ubah, me-load ulang entitas via DAO untuk menghindari stale state.
	 * (5) Mengisi field entitas: fakultas, jurusan, nama, keterangan, dan perguruan tinggi
	 *     dari nilai komponen form (null jika tidak dipilih).
	 * (6) Memanggil {@code update} atau {@code save} via DAO tergantung mode.<br><br>
	 *
	 * <b>Return:</b><br>
	 * {@code true} jika berhasil disimpan; {@code false} jika validasi gagal.<br><br>
	 *
	 * <b>Penanganan error:</b><br>
	 * Duplikasi nama ditangani secara manual via query count. Exception DAO diteruskan ke ZK.<br><br>
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Combobox Fakultas dan Jurusan mengembalikan null sebagai value jika tidak dipilih atau
	 * dipilih item tanpa value (misalnya "Semua"). Pastikan entitas mengizinkan null pada
	 * field fakultas dan jurusan.
	 *
	 * @param event Event ZK dari klik tombol Simpan.
	 * @return {@code true} jika berhasil disimpan; {@code false} jika validasi gagal.
	 * @throws Exception Jika terjadi kesalahan DAO saat menyimpan data.
	 */
	public boolean onSave(Event event) throws Exception {

		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Nama Pemilik Aset belum diisi. Langkah yang dapat dilakukan: (1) Isi field Nama dengan nama pemilik aset yang sesuai; (2) Pemilik aset digunakan untuk pencatatan kepemilikan dan pelaporan; (3) ulangi proses simpan ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		boolean i = checkNamaPemilikAsset();
		if (i) {
			MyMessageboxConfig.show("Nama Pemilik Asset sudah ada di database", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		PemilikAssetDao pemilikAssetDao = DaoFactory.getInstance().getPemilikAssetDao();
		if (pemilikAsset.getId() != null) {
			pemilikAsset = pemilikAssetDao.load(pemilikAsset.getId());

		}

		pemilikAsset.setFakultas(
				(Fakultas) (fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null ? null
						: fakultas.getSelectedItem().getValue()));
		pemilikAsset.setJurusan(
				(Jurusan) (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null ? null
						: jurusan.getSelectedItem().getValue()));
		pemilikAsset.setNama(nama.getValue());
		pemilikAsset.setKeterangan(keterangan.getValue());
		pemilikAsset.setPerguruanTinggi((PerguruanTinggi) (perguruanTinggi.getSelectedItem() == null ? null
				: perguruanTinggi.getSelectedItem().getValue()));

		if (pemilikAsset.getId() != null) {
			pemilikAssetDao.update(pemilikAsset);
		} else {
			pemilikAssetDao.save(pemilikAsset);
		}

		return true;
	}

	/**
	 * Membangun Criteria Hibernate untuk kueri daftar pemilik aset.
	 *
	 * <b>Tujuan:</b><br>
	 * Menyusun filter Hibernate yang mendukung pencarian berdasarkan status aktif dan nama.
	 * Digunakan oleh {@code onSearchDefault} dan {@code Common.initPaging}.<br><br>
	 *
	 * <b>Cara kerja:</b><br>
	 * Filter status aktif: jika checkbox searchaktif null atau dicentang, menampilkan hanya
	 * yang aktif (null dianggap aktif via OR isNull/eq true); jika tidak dicentang,
	 * menampilkan semua (sqlRestriction "true"). Filter nama menggunakan ILIKE partial match.
	 * Urutan default adalah ascending berdasarkan nama.<br><br>
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Filter fakultas dan jurusan saat ini tidak diimplementasikan di method ini meskipun
	 * ada field {@code searchfakultas} dan {@code searchjurusan}. Jika filter tersebut
	 * perlu diaktifkan, tambahkan kondisi Restrictions yang sesuai.
	 *
	 * @param order {@code true} untuk menambahkan ORDER BY nama ASC.
	 * @return Objek {@code Criteria} Hibernate siap dieksekusi.
	 */
	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(PemilikAsset.class)
				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"));
		if (order)
			criteria.addOrder(Order.asc("nama"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true") : Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	/**
	 * Memuat ulang data grid pemilik aset berdasarkan filter saat ini.
	 *
	 * <b>Tujuan:</b><br>
	 * Me-refresh tampilan grid daftar pemilik aset setelah perubahan filter atau
	 * operasi CRUD. Dipanggil dari inisialisasi halaman, paginasi, dan setelah
	 * operasi tambah/ubah/hapus.<br><br>
	 *
	 * <b>Cara kerja:</b><br>
	 * (1) Menghitung total data dan memperbarui komponen paginasi.
	 * (2) Mengeksekusi kueri Hibernate dengan criteria terurut, dibatasi per halaman.
	 * (3) Membungkus hasil dalam SimpleListModel dan menetapkan renderer serta model ke grid.<br><br>
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Dipanggil dari berbagai titik. Pastikan implementasi efisien dan tidak membebani
	 * database secara berlebihan, terutama jika data pemilik aset sangat banyak.
	 *
	 * @param event Event ZK yang memicu pencarian ulang, bisa {@code null}.
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<PemilikAsset> pemilikAsset = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(pemilikAsset);
		grid.setRowRenderer(new PemilikAssetRenderer());
		grid.setModelCheckMobile(strset);

	}

	/**
	 * Memeriksa apakah nama pemilik aset yang akan disimpan sudah ada di database.
	 *
	 * <b>Tujuan:</b><br>
	 * Mencegah duplikasi nama pemilik aset di database dengan melakukan query count
	 * sebelum penyimpanan. Pada mode ubah, mengecualikan entitas yang sedang diedit
	 * dari pengecekan agar nama yang sama dapat dipertahankan tanpa peringatan palsu.<br><br>
	 *
	 * <b>Cara kerja:</b><br>
	 * Menggunakan Hibernate Criteria dengan {@code Projections.rowCount()} untuk menghitung
	 * berapa baris yang memiliki nama yang persis sama (case-sensitive via {@code Restrictions.eq}).
	 * Jika entitas sedang diedit (ID tidak null), menambahkan kondisi {@code ne("id", currentId)}
	 * untuk mengecualikan entitas saat ini dari hitungan. Hasil count diubah menjadi Integer
	 * dan diperiksa apakah tidak sama dengan nol (berarti ada duplikat).<br><br>
	 *
	 * <b>Return:</b><br>
	 * {@code true} jika nama sudah digunakan oleh pemilik lain (duplikat ada di database);
	 * {@code false} jika nama aman untuk digunakan.<br><br>
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Pengecekan ini case-sensitive. Trim dilakukan pada nilai nama dari textbox untuk
	 * menghindari mismatch karena spasi. Untuk pengecekan case-insensitive, ganti
	 * {@code Restrictions.eq} dengan {@code Restrictions.ilike}.
	 *
	 * @return {@code true} jika nama sudah terduplikat di database; {@code false} jika belum ada.
	 */
	public Boolean checkNamaPemilikAsset() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(PemilikAsset.class).setProjection(Projections.rowCount())
				.add(Restrictions.eq("nama", nama.getValue().trim()))
				.add(this.pemilikAsset.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.pemilikAsset.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

}
