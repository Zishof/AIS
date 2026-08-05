package ais.action.master;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;

import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.StatusLulusCalonMahasiswa;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyWindow;

public class CekCalonMahasiswaAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4604126575556546716L;
	private MyWindow window;
	private String namaFile;
	private MyGrid grids;

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

		init();
		// initData(null);

	}

	@SuppressWarnings("deprecation")
	private void init() throws InterruptedException, IOException {
		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(window);
		panel.setTitle("Cek Calon Mahasiswa yang lulus ujian");
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setFramable(true);
		Panelchildren panelchildren = new Panelchildren();
		panelchildren.setParent(panel);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(panelchildren);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(center);

		North north = new North();
		north.setParent(borderlayout);

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(north);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);
		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("10%");
		column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("90%");

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Upload File"));

		/*
		 * Toolbar toolbar = new Toolbar();// toolbar.setHeight("25px");
		 * toolbar.setParent(south);
		 */
		MyButtonConfig button = new MyButtonConfig("Browse" + Common.ukuranLabelFileUpload(), "");
		button.setUpload(Common.ukuranFileUpload());
		button.addEventListener("onUpload", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {

				UploadEvent uploadEvent = (UploadEvent) event;
				Session session = Common.getManualSession();
				Media media = uploadEvent.getMedia();if(!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media))return;

				// Blob blob = new javax.sql.rowset.serial.SerialBlob(inputStream);
				/*
				 * LogCsvFileUpload logCsvFileUpload = new LogCsvFileUpload();
				 * logCsvFileUpload.setFileContent(blob); logCsvFileUpload.setMimeType(media
				 * .getContentType()); logCsvFileUpload.setFileName(media.getName());
				 * logCsvFileUpload.setUploadDate(ais.ui.util.WaktuUtil.getDate());
				 * session.save(logCsvFileUpload);
				 */

				try {

					// File blobFile = new File(Common.folderFoto +
					// fotobm.getNamaFile());
					// File blobFile = new File(strDirectoy + "/"
					// +media.getName());
					// FileOutputStream outStream = new
					// FileOutputStream(blobFile);
					// InputStream inStream = blob.// getBinaryStream();
					//

					/*
					 * CekCalonMahasiswaAction.this.setNamaFile(media.getName
					 * ());System.out.println("Nama File in = "+
					 * CekCalonMahasiswaAction.this.getNamaFile());
					 */

					XSSFWorkbook workbook = new XSSFWorkbook(media.getStreamData());
					XSSFSheet sheet = workbook.getSheetAt(0);

					// Session session = HibernateUtil.currentSession();

					try {
						int i = 4;
						while (true) {
							String a1 = Common.getCellContent(Common.getCell(sheet, 2, i));
							String b2 = Common.getCellContent(Common.getCell(sheet, 3, i));
							String c2 = Common.getCellContent(Common.getCell(sheet, 4, i));

							Integer integer = ((Number) session.createCriteria(StatusLulusCalonMahasiswa.class)
									.setProjection(Projections.rowCount()).add(Restrictions.eq("noUjian", b2))
									.uniqueResult()).intValue();

							// System.out.println("Integer ke- " + i);
							i++;
							if (!integer.equals(0))
								continue;

							StatusLulusCalonMahasiswa statusLulusCalonMahasiswa = new StatusLulusCalonMahasiswa();

							statusLulusCalonMahasiswa.setProgramStudi(a1);
							statusLulusCalonMahasiswa.setNoUjian(b2);
							statusLulusCalonMahasiswa.setNama(c2);

							session.save(statusLulusCalonMahasiswa);

						}
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);
					}

					MyMessageboxConfig.show("Upload File CSV berhasil, "
							+ "silahkan cek kelulusan calon mahasiswa dengan meng-klik button Check Kelulusan dibawah ini",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);

				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
					System.out.println("ERROR(djv_exportBlob) Unable to export: " + media.getName());

					MyMessageboxConfig.show("Upload File CSV gagal", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
				}

			}

		});
		button.setParent(row);

		row = new MyFormRow();
		row.setParent(rows);
		button = new MyButtonConfig("Check Kelulusan", "");
		button.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {
				// TODO Auto-generated method stub
				Session session = HibernateUtil.currentSession();

				List<StatusLulusCalonMahasiswa> statusLulusCalonMahasiswas = session
						.createCriteria(StatusLulusCalonMahasiswa.class).addOrder(Order.desc("id")).list();

				try {
					List<BiodataCalonMahasiswa> calonMahasiswaLulus = new ArrayList<BiodataCalonMahasiswa>();
					for (StatusLulusCalonMahasiswa st : statusLulusCalonMahasiswas) {

						/*
						 * Integer integer = ((Number) session.createCriteria(
						 * BiodataCalonMahasiswa.class).setProjection( Projections.rowCount()).add(
						 * Restrictions.eq("noUjian", st.getNoUjian())) .uniqueResult()).intValue();
						 */

						BiodataCalonMahasiswa biodataCalonMahasiswa = (BiodataCalonMahasiswa) session
								.createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
								.add(Restrictions.eq("noUjian", st.getNoUjian().trim())).setMaxResults(1)
								.uniqueResult();

						// System.out.println("data ke-"+i+" nomor ujian: "+st.getNoUjian()+"
						// biodataCalonMahasiswa = "+biodataCalonMahasiswa);

						if (biodataCalonMahasiswa != null) {

							// System.out.println("==================== Biodata Calon Mahasiswa Tidak Sama
							// dengan Null ====================");

							biodataCalonMahasiswa.setStatusLulus(BiodataCalonMahasiswa.LULUS);

							Common.refreshUpdate(session, (biodataCalonMahasiswa));

							calonMahasiswaLulus.add(biodataCalonMahasiswa);

							loadDataCalonMahasiswaLulus(calonMahasiswaLulus);

						}
					}

				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}

				MyMessageboxConfig.show("Cek Status Lulus mahasiswa selesai", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.INFORMATION);
			}
		});
		button.setParent(row);

		row = new MyFormRow();
		row.setParent(rows);
		// South south = new
		// South();ais.ui.util.ZkCompat.setFlex(south, true);south.setParent(borderlayout);

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		center.setTitle("Daftar Calon Mahasiswa yang sudah dinyatakan Lulus");

		grids = new MyGrid();
		grids.setParent(center);
		grids.setWidth("100%");
		grids.setHeight("100%");
		grids.setOddRowSclass("non-odd");

		columns = new Columns();
		columns.setParent(grids);
		column = new MyColumnConfig();
		column.setLabel("Nama");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setLabel("No.Registrasi");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setLabel("No.Ujian");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setLabel("Status Kelulusan");
		column.setParent(columns);

	}

	@SuppressWarnings("rawtypes")
	public void loadDataCalonMahasiswaLulus(List calonMahasiswaLulus) {

		ListModel strset = new SimpleListModel(calonMahasiswaLulus);
		grids.setRowRenderer(new CalonMahasiswaLulusRenderer());
		grids.setModelCheckMobile(strset);

		grids.renderAll();
		grids.setOddRowSclass("non-odd");
		// grids.setPageSize(8);

	}

	class CalonMahasiswaLulusRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final BiodataCalonMahasiswa biodataCalonMahasiswa = (BiodataCalonMahasiswa) arg1;

			new Label(biodataCalonMahasiswa.getNama()).setParent(arg0);
			new Label(biodataCalonMahasiswa.getNoRegistrasi()).setParent(arg0);
			new Label(biodataCalonMahasiswa.getNoUjian()).setParent(arg0);
			new Label(biodataCalonMahasiswa.getStatusLulus().equals(1) ? "Lulus" : "").setParent(arg0);

		}

	}

	public String getNamaFile() {
		return namaFile;
	}

	public void setNamaFile(String namaFile) {
		this.namaFile = namaFile;
	}

}
