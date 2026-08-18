package ais.action.master.helper;

import java.io.File;
import java.net.URLEncoder;
import java.util.Calendar;
import java.util.Date;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;
import org.zkoss.zul.Messagebox;
import ais.ui.util.MyMessageboxConfig;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;

import ais.common.BarcodeCommon;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.payroll.LiburNasional;
import ais.database.model.payroll.LiburRutin;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelBold;
import ais.ui.util.MyLabelBoldConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class AbsensiKehadiranPegawaiPerHariHelper extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8823784546257272901L;
	private Combobox bulan;
	private Combobox tahun;
	private MyGrid grid;

	public AbsensiKehadiranPegawaiPerHariHelper() {
		super();
		display();
	}

	public AbsensiKehadiranPegawaiPerHariHelper(String title, String border, boolean closable) {
		super(title, border, closable);
		display();
	}

	public void display() {
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		North north = new North();
		north.setHeight("25px");
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, true);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(north);
		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Bulan : ")));
		toolbar.appendChild(bulan = new Combobox());
		for (int i = 0; i < 12; i++) {
			Comboitem comboitem = new Comboitem(Common.BULAN[i]);
			comboitem.setValue(i + 1);
			bulan.appendChild(comboitem);
		}

		Common.selectComboItem(bulan, ais.ui.util.WaktuUtil.getCalendar().get(Calendar.MONTH) + 1);

		bulan.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});

		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Tahun : ")));
		toolbar.appendChild(tahun = new Combobox());

		Integer currTahun = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
		for (int i = currTahun - 10; i < currTahun + 10; i++) {
			Comboitem comboitem = new Comboitem(i + "");
			comboitem.setValue(i);
			tahun.appendChild(comboitem);
		}

		Common.selectComboItem(tahun, currTahun);

		tahun.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});
		Toolbarbutton button = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}

		});
		button.setParent(toolbar);

		grid = new MyGrid();
		grid.setMold("paging");
		grid.setPageSize(100);
		grid.setParent(center);

		Columns columns = new Columns();

		columns.setParent(grid);

		Column column = new Column();
		column.setParent(columns);
		column.setWidth("40px");
		column.setLabel("");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Tanggal");
		column.setWidth("80%");

		column = new Column();
		column.setParent(columns);
		column.setWidth("20%");
		column.setLabel("QR-Code");

		try {
			loadData(null);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public void loadData(Object object) throws Exception {
		Integer bulan = (Integer) (this.bulan.getSelectedItem() == null ? null
				: this.bulan.getSelectedItem().getValue());

		Integer tahun = (Integer) (this.tahun.getSelectedItem() == null ? null
				: this.tahun.getSelectedItem().getValue());

		if (bulan == null) {
			MyMessageboxConfig.show("Mohon maaf, bulan belum dipilih. Silakan pilih bulan terlebih dahulu, kemudian ulangi proses ini.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return;
		}
		if (tahun == null) {
			MyMessageboxConfig.show("Mohon maaf, tahun belum dipilih. Silakan pilih tahun terlebih dahulu, kemudian ulangi proses ini.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return;
		}

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.MONTH, bulan - 1);
		calendar.set(Calendar.YEAR, tahun);
		calendar.set(Calendar.DATE, 1);

		int jumlahHari = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);

		Session session = HibernateUtil.currentSession();

		Rows rows = grid.getRows() == null ? new Rows() : grid.getRows();
		grid.appendChild(rows);
		rows.setParent(grid);
		Common.clear(rows);

		for (int i = 1; i <= jumlahHari; i++) {

			calendar.set(Calendar.DATE, i);

			final Date tanggal = calendar.getTime();
			final Integer hari = calendar.get(Calendar.DAY_OF_WEEK);
			AbsensiPegawaiAction detail = new AbsensiPegawaiAction(tanggal);

			LiburNasional liburNasional = LiburNasional.ambilLiburNasional(tanggal);
			LiburRutin liburRutin = (LiburRutin) session.createCriteria(LiburRutin.class)
					.add(Restrictions.eq("hari", hari)).setMaxResults(1).uniqueResult();

			Row row = new Row();
			row.setValign("top");
			detail.setParent(row);
			if (liburRutin != null && liburRutin.getLibur()) {
				row.setStyle("border:0px;background: yellow;");
			}

			if (liburNasional != null) {
				row.setStyle("border:0px;background: pink;");
			}

			row.setParent(rows);

			row.appendChild(new Label(Common.dateFormat6.get().format(calendar.getTime())
					+ (liburNasional == null ? "" : " (" + liburNasional.toString() + ")")));

			MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Absen via QR-Code",
					"/img/Ecommerce-Qr-Code-icon.png");
			row.appendChild(toolbarbutton);
			toolbarbutton.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event a) throws Exception {
					String code = "P-" + Common.desEncrypter.get().encrypt(Common.dateFormat8.get().format(tanggal));
					File myfilebarcode1 = new File(
							Common.ambilREAL_PATH_REPORT() + "/crcode_" + URLEncoder.encode(code, "UTF-8") + ".png");
					BarcodeCommon.generateCRCode(code, myfilebarcode1);
					String url = Common.getRequestHostWithProtocol() + "/report/"
							+ URLEncoder.encode(myfilebarcode1.getName(), "UTF-8");

					final MyWindow window = new MyWindow();
					window.setClosable(false);
					ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);

					Borderlayout borderlayout = new Borderlayout();
					borderlayout.setParent(window);

					Center center = new Center();
					center.setBorder("none");
					center.setParent(borderlayout);
					ais.ui.util.ZkCompat.setFlex(center, true);
					Image img = new Image(url);
					final Row row = Common.tampilanScroll(center);
					row.appendChild(
							new MyLabelBold("Absensi kehadiran pegawai per " + Common.dateFormat6.get().format(tanggal)));

					Row rowbaru = new Row();
					rowbaru.setParent(row.getParent());
					rowbaru.appendChild(new MyLabelBoldConfig("Scan / baca QR-Code berikut untuk melakukan absen :"));

					rowbaru = new Row();
					rowbaru.setParent(row.getParent());
					rowbaru.appendChild(img);
					img.setWidth("100%");

					South south = new South();
					ais.ui.util.ZkCompat.setFlex(south, true);
					south.setParent(borderlayout);

					Toolbar toolbar = new Toolbar();
					toolbar.setParent(south);
					MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Selesai", "/img/cancel.gif");
					cancel.setTooltiptext("Tutup");
					cancel.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {
							window.detach();
						}
					});
					cancel.setParent(toolbar);

					boolean mobile = Common.isMobile();
					window.setVisible(true);
					window.setHeight("98%");
					window.setWidth(mobile ? "98%" : "550px");
					window.onModal();
				}
			});

		}

	}
}
