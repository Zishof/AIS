package ais.action.master.sekolah.helper;

import java.io.File;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;

import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.ConstantValues;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.sekolah.JadwalPelajaran;
import ais.database.model.sekolah.KelasSiswa;
import ais.database.model.sekolah.KelasSiswaPunyaSiswa;
import ais.database.model.sekolah.Siswa;
import ais.database.model.sekolah.VoKelasPunyaSiswa;
import ais.ui.util.DataCriteria;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class DetailJadwalMatapelajaranHelper implements DataLoader, DataCriteria {

	private MyGrid grid;
	// private Siswa siswa;
	private KelasSiswa kelasSiswa;
	// private boolean delete = false;

	private Textbox nama;
	private Intbox angkatan;
	// private boolean create;

	private Paging paging;
	private JadwalPelajaran jadwalPelajaran;

	public DetailJadwalMatapelajaranHelper() {
		// delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		// create = CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE);

		paging = new Paging();
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(arg0);
			}
		});
	}

	class DetailPARenderer extends ais.ui.util.MyRowRenderer {

		public DetailPARenderer() {

		}

		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");
			final KelasSiswaPunyaSiswa kelasSiswaPunyaSiswa = (KelasSiswaPunyaSiswa) data;
			final Siswa siswa = kelasSiswaPunyaSiswa.getSiswa();

			if (kelasSiswaPunyaSiswa.getKelasSiswa().getTahunAjaran().equals(Common.getCurrentTahunAkademik())) {

				if (siswa.getKelas() == null) {
					siswa.setKelas(kelasSiswaPunyaSiswa.getKelasSiswa());
					Common.refreshUpdate(siswa);
				}
			}
			CommonMedia.tampilkanGambarKecil(siswa).setParent(row);
			RevisiHelper.createNewRevisi(Siswa.class, siswa, siswa.getNomorInduk()).setParent(row);

			new Label(siswa.getNama()).setParent(row);
			new Label(siswa.getTahunMasuk() + "").setParent(row);
			new Label(siswa.getStatusSiswa()).setParent(row);

		}

	}

	public Criteria initCriteria(boolean order) {

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(KelasSiswaPunyaSiswa.class)

				.add(Restrictions.eq("kelasSiswa", kelasSiswa))

				.createAlias("siswa", "siswa")

				.add(nama == null || nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(
								Restrictions.ilike("siswa.namaSiswa", nama.getValue().trim(), MatchMode.ANYWHERE),
								Restrictions.or(
										Restrictions.ilike("siswa.nomorInduk", nama.getValue().trim(),
												MatchMode.ANYWHERE),
										Restrictions.ilike("siswa.nomorIndukNasional", nama.getValue().trim(),
												MatchMode.ANYWHERE))))
				.add(angkatan == null || angkatan.getValue() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("siswa.tahunMasuk", angkatan.getValue()));

		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser != null && tbmuser.getOrangTua() != null && !tbmuser.getOrangTua().ambilAnakSiswa().isEmpty()) {
			criteria.add(Restrictions.in("siswa.id", tbmuser.getOrangTua().ambilAnakSiswa()));
		}

		if (order) {
			criteria.addOrder(Order.asc("nomorUrut"))
					.addOrder(Order.asc("siswa.nomorIndukNasional")).addOrder(Order.desc("siswa.id"));
		}

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void loadData(Object value) {
		Common.initPaging(initCriteria(false), paging);
		List<? extends VoKelasPunyaSiswa> siswa = ConstantValues.simpleList(
				initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
						.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())),
				KelasSiswaPunyaSiswa.class);

		ListModel strset = new SimpleListModel(
				KelasSiswaPunyaSiswa.filterMk(siswa, jadwalPelajaran.getMatapelajaran()));
		grid.setRowRenderer(new DetailPARenderer());
		grid.setModelCheckMobile(strset);

	}

	public void displayDetailPA(final JadwalPelajaran jadwalPelajaran, final Component component,
			final MyWindow window) {
		this.jadwalPelajaran = jadwalPelajaran;
		this.kelasSiswa = jadwalPelajaran.getKelas();
		Common.clear(component);

		ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
		groupbox.setStyle("min-height: 200px;");
		groupbox.setParent(component);

		Toolbar toolbar = new Toolbar();
		toolbar.setParent(groupbox);
		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Siswa : ")));
		toolbar.appendChild(nama = new Textbox());
		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Angkatan : ")));
		toolbar.appendChild(angkatan = new Intbox());
		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}

		});
		button.setParent(toolbar);

		nama.addEventListener("onOK", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}

		});
		angkatan.addEventListener("onOK", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}

		});

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, "nomorInduk", "nomorIndukNasional",
				"namaSiswa", "tahunMasuk", "sekolah.nama", "sekolah.yayasan", "statusSiswa");
		toolbar.appendChild(cetakToolbarbutton);

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(50);
		grid.getPagingChild().setMold("os");
		grid.setParent(groupbox);

		paging.setParent(groupbox);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Foto");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("NIS");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Angkatan");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Status");
		column.setWidth("10%");

		loadData(null);

	}

	public void uploadDataSiswa(final File file, final EventListener eventListener) throws Exception {

		final Label peringatan = new Label("");

		final Label label = new Label(ais.common.Common.getBahasaConfig("Proses upload data data .."));
		Clients.showBusy(label.getValue());
		final Timer timer = new Timer(200);
		timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		timer.setRepeats(true);
		timer.addEventListener("onTimer", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Clients.showBusy(label.getValue());
				if (label.getValue().isEmpty()) {
					System.out.println("loading file " + file.getAbsolutePath());
					MyMessageboxConfig.show(
							"Upload data siswa berhasil dilakukan."
									+ (peringatan.getValue().isEmpty() ? "" : "\n" + peringatan.getValue()),
							"Pemberitahuan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, eventListener);
					Clients.clearBusy();
					timer.detach();
				}

			}
		});
		timer.start();

		new Thread(new Runnable() {

			@Override
			public void run() {
				try {

				try {

					XSSFWorkbook workbook = new XSSFWorkbook(file.getAbsolutePath());
					XSSFSheet sheet = workbook.getSheetAt(0);

					Session session = HibernateUtil.currentNativeSession();

					int rowCount = (sheet.getLastRowNum() + 1);
					for (int i = 1; i < rowCount; i++) {
						try {

							Siswa siswa = (Siswa) Common.getSheetContentAsObject(sheet, 0, i, Siswa.class);
							if (siswa != null && siswa.getId() != null) {

								siswa.setKelas(kelasSiswa);

								session.getTransaction().begin();
								Common.refreshUpdate(session, siswa);
								session.getTransaction().commit();

								label.setValue("Upload data \"" + siswa.getNim() + " - " + siswa.getNama() + "\" ("
										+ Common.numberFormat.get().format(i * 100.0 / rowCount) + " %)");
							}

						} catch (Exception e) {
							Common.tampilErrorJikaAdmin(e);
						}

					}
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/sekolah/helper/DetailJadwalMatapelajaranHelper.java:308");
				}

				HibernateUtil.closeSession();

				label.setValue("");
							} finally {
					ais.database.hibernate.HibernateUtil.closeSession();
				}
			}
		}).start();
	}

}
