package ais.action.master.payroll.helper;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Button;
import org.zkoss.zul.Center;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Fileupload;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Timer;
import org.zkoss.zul.West;
import org.zkoss.zul.Window;

import ais.action.master.payroll.util.MesinNetigen;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.payroll.UploadLog;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;

public class UploadDataHelper extends Window {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4161818126778629657L;
	private Combobox jenisMesin;
	private Fileupload fileUpload;
	private Center center;

	public static final String NETIGEN = "Nitgen (NAC2500-NAC3000)";

	public UploadDataHelper() {
		super();
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
		}
	}

	public UploadDataHelper(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);
		init();
	}

	private void init() throws Exception {

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		West west = new West();
		west.setTitle("Menu");
		west.setCollapsible(true);
		west.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(west, true);
		west.setWidth("350px");

		MyGrid grid = new MyGrid();
		grid.setParent(west);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);
		Column column = new Column();
		column.setWidth("20%");
		column.setParent(columns);
		column = new Column();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Jenis Mesin")));
		row.appendChild(jenisMesin = new Combobox());
		Comboitem mesin = new Comboitem(NETIGEN);
		mesin.setValue(NETIGEN);
		jenisMesin.appendChild(mesin);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Upload File")));
		row.appendChild(fileUpload = new Fileupload("Upload Log Mesin"));

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				try {
					UploadEvent uploadEvent = (UploadEvent) event;
					if (uploadEvent != null) {

						String mesin = (String) (jenisMesin.getSelectedItem() == null ? null
								: jenisMesin.getSelectedItem().getValue());
						if (mesin == null) {
							MyMessageboxConfig.show(
									"Mohon maaf, Jenis Mesin wajib dipilih terlebih dahulu sebelum melakukan unggah data. Langkah yang dapat dilakukan: (1) pilih Jenis Mesin yang sesuai pada pilihan yang tersedia; (2) pastikan pilihan tidak dikosongkan; (3) ulangi kembali proses unggah data.",
									"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
							return;
						}

						UploadLog uploadLog = new UploadLog();
						uploadLog.setNama(uploadEvent.getMedia().getName());
						uploadLog.setKeterangan(uploadEvent.getMedia().getContentType());
						uploadLog.setMesin(mesin);
						uploadLog.setTextUpload(new String(uploadEvent.getMedia().getByteData()));

						process(uploadLog);

						loadData();
					}
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e); 
				}

			}
		};
		fileUpload.addEventListener("onUpload", eventListener);

		loadData();
	}

	private void process(final UploadLog uploadLog) throws Exception {

		if (uploadLog.getMesin().equals(NETIGEN)) {
			MesinNetigen.process(uploadLog);
		}

		Session session = HibernateUtil.currentSession();
		session.saveOrUpdate(uploadLog);

		Timer timer = new Timer(1000);
		timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		timer.addEventListener("onTimer", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				displayHasil(uploadLog);
			}
		});
		timer.start();
	}

	@SuppressWarnings("unchecked")
	public void loadData() {
		Common.clear(center);

		MyGrid grid = new MyGrid();
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		Column column = new Column("Tanggal");
		column.setParent(columns);

		column = new Column("File");
		column.setParent(columns);
		column = new Column("Mesin");
		column.setParent(columns);
		column = new Column("Keterangan");
		column.setParent(columns);
		column = new Column("Hasil");
		column.setWidth("10%");
		column.setParent(columns);

		column = new Column("Hapus");
		column.setWidth("10%");
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		Session session = HibernateUtil.currentSession();
		List<UploadLog> uploadLogs = session.createCriteria(UploadLog.class).addOrder(Order.desc("id")).list();

		for (final UploadLog uploadLog : uploadLogs) {
			MyFormRow row = new MyFormRow();row.setValign("top");
			row.setParent(rows);
			row.appendChild(new Label(Common.dateFormat3.get().format(uploadLog.getTanggal_dirubah())));
			row.appendChild(new Label(uploadLog.getNama()));
			row.appendChild(new Label(uploadLog.getMesin()));
			row.appendChild(new Label(uploadLog.getKeterangan()));

			Button button = new Button("Lihat Hasil");
			button.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					displayHasil(uploadLog);
				}
			});
			row.appendChild(button);

			button = new MyToolbarbuttonConfig("Hapus", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show(
							"Apakah Bapak/Ibu yakin ingin menghapus data ini? Perlu diketahui bahwa data yang telah dihapus tidak dapat dikembalikan lagi.",
							"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

						@Override
						public void onEvent(Event event) throws Exception {
							int i = Integer.parseInt(event.getData().toString());
							if (i == MyMessageboxConfig.OK) {
								try {
									Session session = HibernateUtil.currentSession();
									session.delete(uploadLog);
									loadData();
								} catch (Exception e) {
									Common.tampilErrorJikaAdmin(e);
									MyMessageboxConfig.show(MyMessageboxConfig.format(
											"Mohon maaf, data ini tidak dapat dihapus karena masih berelasi dengan data lainnya. Rincian teknis kesalahan: {V1}. Langkah yang dapat dilakukan: (1) hapus terlebih dahulu data lain yang masih terkait dengan data ini; (2) pastikan tidak ada transaksi yang masih menggunakan data ini; (3) ulangi kembali proses penghapusan.",
											e.getMessage()));
								}

							}

						}
					});

				}
			});
			button.setParent(row);
		}
	}

	private void displayHasil(UploadLog uploadLog) throws Exception {
		Window window = new Window("Lihat Hasil", "none", true);
		window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		window.setHeight("97%");
		window.setWidth("80%");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(window);

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		MyGrid grid = new MyGrid();
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Rows rows = new Rows();
		rows.setParent(grid);

		HibernateUtil.currentSession().refresh(uploadLog);

		for (String ss : uploadLog.getLogDetail()) {
			MyFormRow row = new MyFormRow();row.setValign("top");
			row.setParent(rows);
			row.appendChild(new Label(ss));
		}

		window.onModal();
	}
}
