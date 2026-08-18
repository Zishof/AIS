package ais.action.master.pmb;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
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

import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.dao.DaoFactory;
import ais.database.dao.JenisSekolahMahasiswaBaruDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.JenisSekolahMahasiswaBaru;
import ais.ui.util.DataCriteria;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class JenisSekolahMahasiswaBaruAction extends GenericAutowireComposer implements DataCriteria {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3829770598967565539L;
	private MyWindow addWindow;
	private MyGrid grid;

	private Textbox searchnama;
	private Textbox searchkode;
	private Textbox nama;
	private Textbox keterangan;
	private Textbox kode;

	private boolean edit = false;
	private boolean delete = false;
	private Paging paging;

	private JenisSekolahMahasiswaBaru jenisSekolahMahasiswaBaru;
	private MyToolbarbuttonConfig add;

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

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, "id", "nama", "kode");
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = new MyToolbarbuttonConfig("Upload" + Common.ukuranLabelFileUpload(),
				"/img/excel.png");
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		if (upload != null) { upload.setUpload(Common.ukuranFileUpload()); }
		upload.addEventListener("onUpload", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				UploadEvent uploadEvent = (UploadEvent) event;
				Media media = uploadEvent.getMedia();if(!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media))return;
				if (media.getName().toLowerCase().endsWith("xlsx")) {

					InputStream inputStream = media.getStreamData();
					// System.out.println("media = " + media);
					File file = new File(Sessions.getCurrent().getWebApp().getRealPath("/temp/" + media.getName()));
					// System.out.println("file = " + file.getAbsolutePath());
					file.getParentFile().mkdirs();
					FileOutputStream fileOutputStream = new FileOutputStream(file);
					int c;
					while ((c = inputStream.read()) != -1) {
						fileOutputStream.write(c);
					}
					fileOutputStream.close();
					inputStream.close();

					XSSFWorkbook workbook = new XSSFWorkbook(file.getAbsolutePath());
					XSSFSheet sheet = workbook.getSheetAt(0);

					String peringatan = "";
					for (int i = 1; i < (sheet.getLastRowNum() + 1); i++) {
						try {

							Session session = HibernateUtil.currentNativeSession();

							if (Common.getSheetContentAsString(sheet, 1, i) == null) {
								break;
							}

							String nama = Common.getSheetContentAsString(sheet, 1, i);
							String kode = Common.getSheetContentAsString(sheet, 2, i);

							if (nama != null && !nama.trim().isEmpty()) {
								Long id = Common.getSheetContentAsLong(sheet, 0, i);
								JenisSekolahMahasiswaBaru jenisSekolahMahasiswaBaru = id == null || id.equals(-1L)
										? null
										: (JenisSekolahMahasiswaBaru) session
												.createCriteria(JenisSekolahMahasiswaBaru.class)
												.add(Restrictions.or(Restrictions.isNull("aktif"),
														Restrictions.eq("aktif", true)))
												.add(Restrictions.idEq(id)).uniqueResult();

								if (jenisSekolahMahasiswaBaru == null) {
									jenisSekolahMahasiswaBaru = new JenisSekolahMahasiswaBaru();
								}

								jenisSekolahMahasiswaBaru.setNama(nama);
								jenisSekolahMahasiswaBaru.setKode(kode);

								session.getTransaction().begin();
								session.saveOrUpdate(jenisSekolahMahasiswaBaru);
								session.getTransaction().commit();
							}

							HibernateUtil.closeSession();

						} catch (Exception e) {
							Common.tampilErrorJikaAdmin(e);
						}

					}

					MyMessageboxConfig.show(
							"Upload data berhasil dilakukan." + (peringatan.isEmpty() ? "" : "\n" + peringatan),
							"Pemberitahuan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
							new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									onSearchDefault(null);
								}
							});

				} else {
					MyMessageboxConfig
							.show("File yang anda upload harus ber-format Excel Open XML Spreadsheet (xlsx). Jika masih menggunakan format lain, buka file excel tersebut, kemudian Save As Excel Open XML Spreadsheet (xlsx). "
									+ media, "Error", MyMessageboxConfig.OK, MyMessageboxConfig.ERROR);
				}
			}
		});
		Common.appendKeToolbar(upload, add, comp);

		onSearchDefault(null);
	}

	class JenisSekolahMahasiswaBaruRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {arg0.setValign("top");
			// TODO Auto-generated method stub
			final JenisSekolahMahasiswaBaru jenisSekolahMahasiswaBaru = (JenisSekolahMahasiswaBaru) arg1;

			new Label(jenisSekolahMahasiswaBaru.getNama()).setParent(arg0);
			new Label(jenisSekolahMahasiswaBaru.getKode()).setParent(arg0);
			new Label(jenisSekolahMahasiswaBaru.getKeterangan()).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(jenisSekolahMahasiswaBaru.getAktif());
			checkbox.setParent(arg0);arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					jenisSekolahMahasiswaBaru.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(jenisSekolahMahasiswaBaru);
				}
			});

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(jenisSekolahMahasiswaBaru);
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
											JenisSekolahMahasiswaBaruDao jenisSekolahMahasiswaBaruDao = DaoFactory
													.getInstance().getJenisSekolahMahasiswaBaruDao();
											// jenisSekolahMahasiswaBaruDao.beginTransaction();
											jenisSekolahMahasiswaBaruDao.delete(
													jenisSekolahMahasiswaBaruDao.merge(jenisSekolahMahasiswaBaru));
											// jenisSekolahMahasiswaBaruDao.commitTransaction();
											onSearchDefault(event);
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											MyMessageboxConfig
													.show("Data ini tidak dapat dihapus .., karena berelasi dengan data lainnya, error-nya adalah sbagai berikut:"
															+ e.getMessage());
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

	public void onAdd(Event event) throws Exception {
		init(new JenisSekolahMahasiswaBaru());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(JenisSekolahMahasiswaBaru jenisSekolahMahasiswaBaru) {
		this.jenisSekolahMahasiswaBaru = jenisSekolahMahasiswaBaru;
		addWindow.setTitle(jenisSekolahMahasiswaBaru.getId() == null ? "Tambah Jenis Sekolah" : "Ubah Jenis Sekolah");
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

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama"));
		row.appendChild(nama = new Textbox(
				jenisSekolahMahasiswaBaru.getNama() == null ? "" : jenisSekolahMahasiswaBaru.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode"));
		row.appendChild(kode = new Textbox(jenisSekolahMahasiswaBaru.getKode()));
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(
				jenisSekolahMahasiswaBaru.getKeterangan() == null ? "" : jenisSekolahMahasiswaBaru.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		// row = new MyFormRow();
		//		// row.setParent(rows);
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
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Jenis Sekolah Mahasiswa Baru harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		boolean i = checkNamaAgama();
		if (i) {
			MyMessageboxConfig.show("Jenis Sekolah Mahasiswa Baru ini sudah ada di database", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}

		JenisSekolahMahasiswaBaruDao jenisSekolahMahasiswaBaruDao = DaoFactory.getInstance()
				.getJenisSekolahMahasiswaBaruDao();
		if (jenisSekolahMahasiswaBaru.getId() != null) {
			jenisSekolahMahasiswaBaru = jenisSekolahMahasiswaBaruDao.load(jenisSekolahMahasiswaBaru.getId());

		}

		jenisSekolahMahasiswaBaru.setNama(nama.getValue());
		jenisSekolahMahasiswaBaru.setKeterangan(keterangan.getValue());
		jenisSekolahMahasiswaBaru.setKode(kode.getValue().trim());

		// jenisSekolahMahasiswaBaruDao.beginTransaction();
		if (jenisSekolahMahasiswaBaru.getId() != null) {
			jenisSekolahMahasiswaBaruDao.update(jenisSekolahMahasiswaBaru);
		} else {
			jenisSekolahMahasiswaBaruDao.save(jenisSekolahMahasiswaBaru);
		}
		// jenisSekolahMahasiswaBaruDao.commitTransaction();
		return true;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);
		List<JenisSekolahMahasiswaBaru> jenisSekolahMahasiswaBaru = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(jenisSekolahMahasiswaBaru);
		grid.setRowRenderer(new JenisSekolahMahasiswaBaruRenderer());
		grid.setModelCheckMobile(strset);

	}

	public Boolean checkNamaAgama() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(JenisSekolahMahasiswaBaru.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.setProjection(Projections.rowCount()).add(Restrictions.eq("nama", nama.getValue().trim()))
				.add(this.jenisSekolahMahasiswaBaru.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.jenisSekolahMahasiswaBaru.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

	@Override
	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(JenisSekolahMahasiswaBaru.class);
		if (order)
			criteria.addOrder(Order.asc("nama"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true") : Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
		criteria.add(searchkode == null || searchkode.getValue().trim().isEmpty()
		        ? Restrictions.sqlRestriction("true")
		        : Restrictions.ilike("kode", searchkode.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}
}
