package ais.action.master;

import java.io.ByteArrayOutputStream;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zss.model.Worksheet;
import org.zkoss.zss.ui.Spreadsheet;
import org.zkoss.zss.ui.impl.Utils;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Doublebox;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Hbox;
import ais.ui.util.MyInclude;
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

import ais.action.master.helper.AmbilDataMahasiswaBanbox;
import ais.action.master.helper.RevisiHelper;
import ais.action.report.format1.akademik.LaporanToefl;
import ais.action.report.format1.akademik.LaporanToeflMahasiswa;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.database.dao.DaoFactory;
import ais.database.dao.NilaiToeflToaflDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.JenisToefl;
import ais.database.model.Mahasiswa;
import ais.database.model.NilaiToeflToaflMahasiswa;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Controller/action ZK untuk nilai toefl toafl mahasiswa. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code Textbox searchnim}, {@code AmbilDataMahasiswaBanbox mahasiswa}, {@code
 * MyDatebox tanggalTes}, {@code Combobox jenisTes}, {@code Combobox searchjenisTes}; inisialisasi/lifecycle
 * ({@code doBeforeCompose()}, {@code doAfterCompose()}, {@code init()}, {@code initCriteria()}, {@code
 * initspreadsheet()}); pembacaan/pencarian ({@code onSearchDefault()}); mutasi data ({@code onSave()}); operasi
 * domain lain ({@code onJenis()}, {@code onLaporanToefl()}, {@code onLaporanToeflMahasiswa()}, {@code onAdd()}).
 * Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 * <p><b>Lifecycle:</b> instance mengikuti lifecycle komponen ZK dan menyimpan state layar; jangan digunakan
 * sebagai singleton atau dibagikan antar desktop/session. Event handler harus tetap memakai konteks pengguna
 * serta session Hibernate milik request yang aktif.</p>
 *
 * @see GenericAutowireComposer
 */
public class NilaiToeflToaflMahasiswaAction extends GenericAutowireComposer implements DataCriteria, DataSearchDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnim;
	private AmbilDataMahasiswaBanbox mahasiswa;
	private MyDatebox tanggalTes;
	private Combobox jenisTes;
	private Combobox searchjenisTes;
	private NilaiToeflToaflMahasiswa nilaiToeflToaflMahasiswa;

	private boolean edit = false;
	private boolean delete = false;

	private MyToolbarbuttonConfig add;
	// private MyToolbarbuttonConfig upload;
	// private MyToolbarbuttonConfig download;
	private Doublebox skor1;
	private Doublebox skor2;
	private Doublebox skor3;
	private Doublebox skor4;
	private Tabpanel jenisToefl;

	public void onJenis(Event event) {
		if (jenisToefl.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(jenisToefl);
			MyInclude iframe = new MyInclude("/pages/master/jenis_toefl.zul");
			iframe.setParent(window);
		}
	}

	private Tabpanel laporanToefl;

	private Tabpanel laporanToeflMhs;
	private MyDatebox masaBerlaku;

	public void onLaporanToefl(Event event) {

		if (laporanToefl.getChildren().size() == 0) {
			LaporanToefl laporan = new LaporanToefl();
			laporan.setHeight("100%");
			laporan.setWidth("100%");
			laporan.setParent(laporanToefl);
		}
	}

	public void onLaporanToeflMahasiswa(Event event) {

		if (laporanToeflMhs.getChildren().size() == 0) {
			LaporanToeflMahasiswa laporan = new LaporanToeflMahasiswa();
			laporan.setHeight("100%");
			laporan.setWidth("100%");
			laporan.setParent(laporanToeflMhs);
		}
	}

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
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

		Common.insertComboDanSemua(searchjenisTes, "nama", "rumus", JenisToefl.class);

		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		String[] contents = new String[] { "id", "mahasiswa.nim", "mahasiswa.nama", "tanggalTest", "jenisToefl",
				"mahasiswa.jurusan.nama", "mahasiswa.jurusan.fakultas.nama", "mahasiswa.tanggallahir", "masaBerlaku",
				"skor1", "skor2", "skor3", "skor4", "total" };

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, NilaiToeflToaflMahasiswa.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);
	}

	class NilaiToeflToaflRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {arg0.setValign("top");
			// TODO Auto-generated method stub
			final NilaiToeflToaflMahasiswa nilaiToeflToaflMahasiswa = (NilaiToeflToaflMahasiswa) arg1;

			RevisiHelper.createNewRevisi(NilaiToeflToaflMahasiswa.class, nilaiToeflToaflMahasiswa,
					nilaiToeflToaflMahasiswa.getMahasiswa().getNim() + "-"
							+ nilaiToeflToaflMahasiswa.getMahasiswa().getNama())
					.setParent(arg0);

			new Label(nilaiToeflToaflMahasiswa.getMahasiswa().getJurusan().getFakultas().getNama()).setParent(arg0);
			new Label(nilaiToeflToaflMahasiswa.getMahasiswa().getJurusan().getNama()).setParent(arg0);

			new Label(Common.dateFormat2.get().format(nilaiToeflToaflMahasiswa.getTanggalTest())).setParent(arg0);
			new Label(nilaiToeflToaflMahasiswa.getMasaBerlaku() == null ? ""
					: Common.dateFormat2.get().format(nilaiToeflToaflMahasiswa.getMasaBerlaku())).setParent(arg0);
			new Label(nilaiToeflToaflMahasiswa.getJenisToefl() == null ? ""
					: nilaiToeflToaflMahasiswa.getJenisToefl().getNama()).setParent(arg0);
			new Label(Common.numberFormat.get().format(nilaiToeflToaflMahasiswa.getSkor1())).setParent(arg0);
			new Label(Common.numberFormat.get().format(nilaiToeflToaflMahasiswa.getSkor2())).setParent(arg0);
			new Label(Common.numberFormat.get().format(nilaiToeflToaflMahasiswa.getSkor3())).setParent(arg0);
			new Label(Common.numberFormat.get().format(nilaiToeflToaflMahasiswa.getSkor4())).setParent(arg0);
			new Label(nilaiToeflToaflMahasiswa.getTotal().intValue()+"").setParent(arg0);

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(nilaiToeflToaflMahasiswa);
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
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {

											Common.refreshDelete(nilaiToeflToaflMahasiswa);

											onSearchDefault(event);
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											MyMessageboxConfig
													.show("Data ini tidak dapat dihapus karena berelasi dengan data lainnya, error-nya adalah sebagai berikut:"
															+ e.getMessage());
										}

									}

								}
							});

				}
			});
			button.setParent(toolbar);
			ais.ui.util.MenuAksiBaris.pasang(toolbar);
			toolbar.setParent(arg0);
		}

	}

	public void onAdd(Event event) throws Exception {
		init(new NilaiToeflToaflMahasiswa());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(NilaiToeflToaflMahasiswa nilaiToeflToaflMahasiswa) {
		this.nilaiToeflToaflMahasiswa = nilaiToeflToaflMahasiswa;
		addWindow.setTitle(nilaiToeflToaflMahasiswa.getId() == null ? "Tambah Toefl dan Toafl Mahasiswa" : "Ubah Toefl dan Toafl Mahasiswa");
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
		row.appendChild(new ais.ui.util.MyLabelConfig(Common.getBahasa("label_mahasiswa")));
		row.appendChild(mahasiswa = new AmbilDataMahasiswaBanbox());
		mahasiswa.setValue(nilaiToeflToaflMahasiswa.getMahasiswa() == null ? ""
				: (nilaiToeflToaflMahasiswa.getMahasiswa().getNim() + " - "
						+ nilaiToeflToaflMahasiswa.getMahasiswa().getNama()));
		mahasiswa.setId("" + nilaiToeflToaflMahasiswa.getMahasiswa() == null ? "mhs_-1"
				: "mhs_" + nilaiToeflToaflMahasiswa.getId());
		mahasiswa.setAttribute("mahasiswa", nilaiToeflToaflMahasiswa.getMahasiswa());
		mahasiswa.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Tes"));
		row.appendChild(tanggalTes = new MyDatebox(nilaiToeflToaflMahasiswa.getTanggalTest() == null ? ais.ui.util.WaktuUtil.getDate()
				: nilaiToeflToaflMahasiswa.getTanggalTest()));
		tanggalTes.setWidth("90%");
		
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Masa Berlaku Sampai"));
		row.appendChild(masaBerlaku = new MyDatebox(nilaiToeflToaflMahasiswa.getMasaBerlaku()));
		masaBerlaku.setWidth("90%"); 

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Test *"));
		row.appendChild(jenisTes = new Combobox());
		Common.insertCombo(jenisTes, "nama", "rumus", JenisToefl.class);
		Common.selectComboItem(jenisTes, nilaiToeflToaflMahasiswa.getJenisToefl());
		jenisTes.setWidth("90%");
		jenisTes.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Section 1"));
		row.appendChild(skor1 = new MyDoublebox(
				nilaiToeflToaflMahasiswa.getSkor1() == null ? 0.0 : nilaiToeflToaflMahasiswa.getSkor1()));
		skor1.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Section 2"));
		row.appendChild(skor2 = new MyDoublebox(
				nilaiToeflToaflMahasiswa.getSkor2() == null ? 0.0 : nilaiToeflToaflMahasiswa.getSkor2()));
		skor2.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Section 3"));
		row.appendChild(skor3 = new MyDoublebox(
				nilaiToeflToaflMahasiswa.getSkor3() == null ? 0.0 : nilaiToeflToaflMahasiswa.getSkor3()));
		skor3.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Section 4"));
		row.appendChild(skor4 = new MyDoublebox(
				nilaiToeflToaflMahasiswa.getSkor4() == null ? 0.0 : nilaiToeflToaflMahasiswa.getSkor4()));
		skor4.setWidth("90%");

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
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

	public boolean onSave(Event event) throws Exception {
		if (mahasiswa.getValue().trim().equals("")) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Mahasiswa",
					"Kolom Mahasiswa belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Mahasiswa.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		if (jenisTes.getSelectedItem() == null || jenisTes.getSelectedItem().getValue() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Jenis Tes",
					"Kolom Jenis Tes belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Jenis Tes.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		NilaiToeflToaflDao nilaiToeflToaflDao = DaoFactory.getInstance().getNilaiToeflToaflDao();
		if (nilaiToeflToaflMahasiswa.getId() != null) {
			nilaiToeflToaflMahasiswa = nilaiToeflToaflDao.load(nilaiToeflToaflMahasiswa.getId());

		}

		nilaiToeflToaflMahasiswa.setMahasiswa((Mahasiswa) mahasiswa.getAttribute("mahasiswa"));
		nilaiToeflToaflMahasiswa.setJenisToefl((JenisToefl) jenisTes.getSelectedItem().getValue());
		nilaiToeflToaflMahasiswa.setTanggalTest(tanggalTes.getValue());
		nilaiToeflToaflMahasiswa.setSkor1(skor1.getValue());
		nilaiToeflToaflMahasiswa.setSkor2(skor2.getValue());
		nilaiToeflToaflMahasiswa.setSkor3(skor3.getValue());
		nilaiToeflToaflMahasiswa.setSkor4(skor4.getValue());
		nilaiToeflToaflMahasiswa.setMasaBerlaku(masaBerlaku.getValue());

		if (nilaiToeflToaflMahasiswa.getId() != null) {
			nilaiToeflToaflDao.update(nilaiToeflToaflMahasiswa);
		} else {
			nilaiToeflToaflDao.save(nilaiToeflToaflMahasiswa);
		}

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(NilaiToeflToaflMahasiswa.class);
		if (order)
			criteria.addOrder(Order.desc("tanggalTest"));

		criteria.add(searchjenisTes.getSelectedItem() == null || searchjenisTes.getSelectedItem().getValue() == null
				? Restrictions.sqlRestriction("true")
				: Restrictions.eq("jenisToefl", searchjenisTes.getSelectedItem().getValue()));

		criteria.createCriteria("mahasiswa")

				.add(searchnim.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.or(Restrictions.ilike("nim", searchnim.getValue().trim(), MatchMode.ANYWHERE),
								Restrictions.ilike("nama", searchnim.getValue().trim(), MatchMode.ANYWHERE)));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<NilaiToeflToaflMahasiswa> nilaiToeflToaflMahasiswa = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(nilaiToeflToaflMahasiswa);
		grid.setRowRenderer(new NilaiToeflToaflRenderer());
		grid.setModelCheckMobile(strset);

	}

	// public void onUpload(Event event) throws Exception {
	//
	// UploadEvent uploadEvent = (UploadEvent) event;
	// Media media = uploadEvent.getMedia();if(!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media))return;
	// if (media.getName().toLowerCase().endsWith("xlsx")) {
	//
	// InputStream inputStream = media.getStreamData();
	// // System.out.println("media = " + media);
	// File file = new File(Sessions.getCurrent().getWebApp().getRealPath(
	// "/temp/" + media.getName()));
	// // System.out.println("file = " + file.getAbsolutePath());
	// file.getParentFile().mkdirs();
	// FileOutputStream fileOutputStream = new FileOutputStream(file);
	// int c;
	// while ((c = inputStream.read()) != -1) {
	// fileOutputStream.write(c);
	//
	// }
	// fileOutputStream.close();
	// inputStream.close();
	// XSSFWorkbook workbook = new XSSFWorkbook(file.getAbsolutePath());
	// XSSFSheet sheet = workbook.getSheetAt(0);
	// Session session = HibernateUtil.currentSession();
	// jxl.Cell cell;
	// System.out.println("rows : " + (sheet.getLastRowNum() + 1));
	//
	// for (int i = 1; i < (sheet.getLastRowNum() + 1); i++) {
	// System.out.println(i + " :i");
	// cell = sheet.getCell(0, i);
	// if (cell == null) {
	// break;
	// }
	// Double skor1 = new Double(sheet.getCell(1, i).getContents()
	// .trim());
	// // Double uts = new Double(sheet.getCell(4, i)
	// // .getContents().trim());
	// // Double uas = new Double(sheet.getCell(5, i)
	// // .getContents().trim());
	// System.out.println("ehm");
	//
	// System.out.println(sheet.getCell(0, i).getContents());
	// System.out.println(sheet.getCell(1, i).getContents());
	//
	// nilaiToeflToaflMahasiswa = new NilaiToeflToaflMahasiswa();
	//
	// nilaiToeflToaflMahasiswa.setSkor1(skor1);
	//
	// session.save(nilaiToeflToaflMahasiswa);
	//
	// }
	//
	// } else {
	// MyMessageboxConfig.show("File yang anda upload harus ber-format excel: "
	// + media, "Error", MyMessageboxConfig.OK, MyMessageboxConfig.ERROR);
	// }
	//
	// // // upload
	// // upload.setUpload(Common.ukuranFileUpload());
	// // upload.addEventListener("onUpload", new EventListener() {
	// // @Override
	// // public void onEvent(Event event) throws Exception {
	// //
	// // }
	// // });
	// //
	// // // upload
	// }

	@SuppressWarnings("unused")
	private void initspreadsheet() throws Exception {

		Spreadsheet spreadsheet = new ais.ui.util.MySpreadsheet();
		spreadsheet.setWidth("100%");
		spreadsheet.setHeight("100%");
		spreadsheet.setSrc("../../WEB-INF/rowcolumn.xlsx");
		// spreadsheet.setMaxcolumns((formatNilais.size()) + 3);
		spreadsheet.setMaxrows(300);
		// final String color = "#000000";

		Worksheet sheet = spreadsheet.getSelectedSheet();

		int rowIndex = 0;
		// int colIndex = 0;
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, "NIM");
		Utils.setColumnWidth(sheet, 0, 200);
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1, "Skor1");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 2, "Skor2");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 3, "Skor3");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 4, "Skor4");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 5, "TOEFL/TOAFL");
		Utils.setColumnWidth(sheet, 5, 100);
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 6, "Tgl Tes (dd-mm-yyyy)");
		Utils.setColumnWidth(sheet, 6, 180);

		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		spreadsheet.getBook().write(bout);
		bout.close();
		String fileName = "template_daftar_nilai_toefl_toafl_mahasiswa_.xlsx";
		fileName = fileName.replaceAll(" ", "_");
		Filedownload.save(bout.toByteArray(), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", fileName);

	}
}
