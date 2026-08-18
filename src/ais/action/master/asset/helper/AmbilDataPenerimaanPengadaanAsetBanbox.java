package ais.action.master.asset.helper;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Bandbox;
import org.zkoss.zul.Bandpopup;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Div;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Radio;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.asset.PenerimaanPengadaanMasterAsset;
import ais.database.model.file.LampiranLain;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;

public class AmbilDataPenerimaanPengadaanAsetBanbox extends Bandbox {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6452461056684904810L;
	private MyGrid grid;
	private EventListener eventListener;

	private Paging paging;

	public AmbilDataPenerimaanPengadaanAsetBanbox() {
		super();
		setReadonly(true);
		paging = new Paging();
		Common.initPaging15(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		display();

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});
	}

	private MyTextbox kode;
	private MyTextbox nama;
	private MyTextbox penyedia;

	class PenerimaanPengadaanMasterAssetRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final PenerimaanPengadaanMasterAsset penerimaanPengadaanMasterAsset = (PenerimaanPengadaanMasterAsset) arg1;
			arg0.setAttribute("penerimaanPengadaanMasterAsset", penerimaanPengadaanMasterAsset);
			final Radio checkbox = new Radio();
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);

			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					AmbilDataPenerimaanPengadaanAsetBanbox.this.setOpen(false);
					AmbilDataPenerimaanPengadaanAsetBanbox.this.setAttribute("penerimaanPengadaanMasterAsset",
							penerimaanPengadaanMasterAsset);
					AmbilDataPenerimaanPengadaanAsetBanbox.this.setValue(penerimaanPengadaanMasterAsset.toString());
					if (eventListener != null) {
						eventListener.onEvent(arg0);
					}
				}
			});
			new Label(penerimaanPengadaanMasterAsset.getKode()).setParent(arg0);

			Vbox myvbox = new Vbox();
			myvbox.setParent(arg0);

			new Label(penerimaanPengadaanMasterAsset.getKeterangan()).setParent(myvbox);

			Hbox hbox = new Hbox();
			hbox.setParent(myvbox);
			LampiranLain.createDownloadUploadFileLain(hbox, penerimaanPengadaanMasterAsset.getId(),
					PenerimaanPengadaanMasterAsset.class.getName(), "Tagihan", false, null, null, false, false, false,
					false);

			new Label(penerimaanPengadaanMasterAsset.getPenyedia() == null ? ""
					: penerimaanPengadaanMasterAsset.getPenyedia().getNama()).setParent(arg0);
			new Label(penerimaanPengadaanMasterAsset.getTanggalPersetujuan() == null ? ""
					: Common.dateFormat4.get().format(penerimaanPengadaanMasterAsset.getTanggalPersetujuan()))
					.setParent(arg0);
		}

	}

	public void display() {

		Bandpopup bandpopup = new ais.ui.util.MyBandpopup();
		bandpopup.setParent(this);
		bandpopup.setWidth("800px");
		bandpopup.setHeight("600px");

		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(bandpopup);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Daftar Penerimaan Barang / Jasa");
		panel.setBorder("none");
		panel.setStyle("border:0px;");

		Panelchildren panelchildren = new Panelchildren();
		panelchildren.setParent(panel);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(panelchildren);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, false);
		north.setHeight("130px");
		north.setAutoscroll(true);

		Div div = new Div();
		div.setParent(north);

		MyGrid searchgrid = new MyGrid();
		searchgrid.setWidth("100%");
		searchgrid.setParent(div);

		Rows rows = new Rows();
		rows.setParent(searchgrid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);

		row.appendChild(new ais.ui.util.MyLabelConfig("Kode"));
		row.appendChild(kode = new MyTextbox());
		kode.setWidth("90%");

		kode.addEventListener("onOK", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(null);
			}
		});

		row.appendChild(new ais.ui.util.MyLabelConfig("Nama"));
		row.appendChild(nama = new MyTextbox());
		nama.setWidth("90%");

		nama.addEventListener("onOK", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(null);
			}
		});

		row.appendChild(new ais.ui.util.MyLabelConfig("Penyedia"));
		row.appendChild(penyedia = new MyTextbox());
		penyedia.setWidth("90%");

		penyedia.addEventListener("onOK", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(null);
			}
		});

		Toolbar toolbar = new Toolbar();
		ais.ui.util.BanboxFilterToggle.pasang(north, searchgrid, toolbar);
		toolbar.setParent(div);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(null);
			}
		});
		button.setParent(toolbar);

		Borderlayout myBorderlayout1 = new ais.ui.util.MyBorderlayout();
		myBorderlayout1.setParent(center);

		Center myCenter1 = new Center();
		ais.ui.util.ZkCompat.setFlex(myCenter1, true);
		myCenter1.setParent(myBorderlayout1);

		South mySouth = new South();
		mySouth.setParent(myBorderlayout1);

		paging.setParent(mySouth);

		grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(myCenter1);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("40px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Kode");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Keterangan");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Penyedia");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Tanggal");

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		toolbar = new Toolbar();
		toolbar.setParent(south);

		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				AmbilDataPenerimaanPengadaanAsetBanbox.this.detach();
			}
		});
		cancel.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		button.addEventListener("onClick", new EventListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {
				if (eventListener != null && grid.getRows() != null && grid.getRows().getChildren() != null) {
					List<PenerimaanPengadaanMasterAsset> penerimaanPengadaanMasterAssets = new ArrayList<PenerimaanPengadaanMasterAsset>();
					List<Row> rows = grid.getRows().getChildren();
					for (Row row : rows) {
						try {
							MyCheckboxConfig checkbox = (MyCheckboxConfig) row.getAttribute("checkbox");
							if (checkbox != null && checkbox.isChecked() && !checkbox.isDisabled()) {
								PenerimaanPengadaanMasterAsset myPenerimaanPengadaanMasterAsset = (PenerimaanPengadaanMasterAsset) row
										.getAttribute("penerimaanPengadaanMasterAsset");
								penerimaanPengadaanMasterAssets.add(myPenerimaanPengadaanMasterAsset);
							}
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/asset/helper/AmbilDataPenerimaanPengadaanAsetBanbox.java:297");
							// TODO: handle exception
						}
					}
					Event myEvent = new Event("myEvent", event.getTarget(), penerimaanPengadaanMasterAssets);
					eventListener.onEvent(myEvent);
				}
				AmbilDataPenerimaanPengadaanAsetBanbox.this.detach();
			}
		});
		button.setParent(toolbar);

	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();

		Criteria criteria = session.createCriteria(PenerimaanPengadaanMasterAsset.class)

				.add(Restrictions.isNotNull("disetujuiOleh"))

				.add(nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("keterangan", nama.getValue().trim(), MatchMode.ANYWHERE))

				.add(kode.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("kode", kode.getValue().trim(), MatchMode.ANYWHERE))

		;

		if (!penyedia.getValue().trim().isEmpty()) {
			criteria.createAlias("penyedia", "penyedia")
					.add(Restrictions.ilike("penyedia.nama", kode.getValue().trim(), MatchMode.ANYWHERE));
		}

		if (order) {
			criteria.addOrder(Order.desc("id"));
		}

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Common.initPaging15(initCriteria(false), paging);

		List<PenerimaanPengadaanMasterAsset> penerimaanPengadaanMasterAssets = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE_15)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE_15 * (paging == null ? 0 : paging.getActivePage())).list();

		ListModel strset = new SimpleListModel(penerimaanPengadaanMasterAssets);
		grid.setRowRenderer(new PenerimaanPengadaanMasterAssetRenderer());
		grid.setModelCheckMobile(strset);

	}

	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	public EventListener getEventListener() {
		return eventListener;
	}
}
