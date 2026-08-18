package ais.action.master.asset.helper;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Session;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Div;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.asset.MasterAsset;
import ais.database.model.rab.SatuanKerja;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class AmbilDataAssetBanyakBerdasarkanStok extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6452461056684904810L;
	private MyGrid grid;
	private EventListener eventListener;
	private List<MasterAsset> masterAssets;
	private SatuanKerja satuanKerja;

	private Set<Long> ids = new HashSet<Long>();

	private String orderBy = "stok";
	private Boolean disableStokHabis;

	public AmbilDataAssetBanyakBerdasarkanStok(List<MasterAsset> masterAssets, SatuanKerja satuanKerja,
			Boolean disableStokHabis) {
		this(masterAssets, satuanKerja, null, disableStokHabis);
	}

	public AmbilDataAssetBanyakBerdasarkanStok(List<MasterAsset> masterAssets, SatuanKerja satuanKerja, String orderBy,
			Boolean disableStokHabis) {
		super();
		this.disableStokHabis = disableStokHabis;
		this.masterAssets = masterAssets;
		this.satuanKerja = satuanKerja;
		if (orderBy != null) {
			this.orderBy = orderBy;
		}
		display();

		onSearchDefault(null);
	}

	private MyTextbox kodeMasterAssetan;
	private MyTextbox nama;

	class MasterAssetRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			Object[] objects = (Object[]) arg1;
			Long masterAssetId = ((Number) objects[0]).longValue();
			Date tanggalTerakhirPengadaan = (Date) objects[2];
			Number stok = (Number) objects[3];
			final MasterAsset masterAsset = (MasterAsset) ConstantValues.ambil(MasterAsset.class.getName(),
					masterAssetId);
			arg0.setAttribute("masterAsset", masterAsset);
			final MyCheckboxConfig checkbox = new MyCheckboxConfig();
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			for (MasterAsset myMasterAsset : masterAssets) {
				if (myMasterAsset.getId().equals(masterAsset.getId())) {
					checkbox.setChecked(true);
					checkbox.setDisabled(true);
					break;
				}
			}

			if (stok != null && stok.doubleValue() < 1.0) {
				checkbox.setDisabled(disableStokHabis != null && disableStokHabis);
			}
			checkbox.setChecked(ids.contains(masterAsset.getId()));

			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (checkbox.isChecked()) {
						ids.add(masterAsset.getId());
					} else {
						ids.remove(masterAsset.getId());
					}
				}
			});

			new Label(masterAsset.getKode()).setParent(arg0);
			new Label(masterAsset.getNama()).setParent(arg0);

			new Label(tanggalTerakhirPengadaan == null ? "" : Common.dateFormat1.get().format(tanggalTerakhirPengadaan))
					.setParent(arg0);
			new Label(stok == null ? "" : Common.numberFormat.get().format(stok)).setParent(arg0);
		}

	}

	public void display() {

		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(this);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Daftar Barang");
		panel.setBorder("none");
		panel.setStyle("border:0px;");

		Panelchildren panelchildren = new Panelchildren();
		panelchildren.setParent(panel);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(panelchildren);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		org.zkoss.zul.Grid gridUtama = new org.zkoss.zul.Grid();
		gridUtama.setWidth("100%");
		ais.ui.util.ZkCompat.setFlex(gridUtama, true);
		gridUtama.setParent(center);
		Rows rowsUtama = new Rows();
		rowsUtama.setParent(gridUtama);

		Row rowUtama = new Row();
		rowUtama.setParent(rowsUtama);

		MyGrid searchgrid = new MyGrid();
		searchgrid.setWidth("100%");
		searchgrid.setParent(rowUtama);

		Rows rows = new Rows();
		rows.setParent(searchgrid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Barang"));
		row.appendChild(kodeMasterAssetan = new MyTextbox());
		kodeMasterAssetan.setWidth("90%");
		kodeMasterAssetan.addEventListener(Events.ON_OK, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Barang"));
		row.appendChild(nama = new MyTextbox());
		nama.addEventListener(Events.ON_OK, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Satuan Kerja"));
		row.appendChild(new ais.ui.util.MyLabelConfig(satuanKerja == null ? "" : satuanKerja.getNama()));

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		Row rowKedua = new Row();
		rowKedua.setParent(rowsUtama);
		toolbar.setHeight("32px");
		toolbar.setParent(rowKedua);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(null);
			}
		});
		button.setParent(toolbar);

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(10);
		grid.getPagingChild().setMold("os");
		Row rowKetiga = new Row();
		rowKetiga.setParent(rowsUtama);
		grid.setMold("paging");
		grid.setPageSize(50);
		grid.getPagingChild().setMold("os");
		grid.setParent(rowKetiga);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("40px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("70px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Kode Barang");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama Barang");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Tgl Pengadaan");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Stok");
		column.setAlign("right");
		column.setWidth("10%");

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(south);

		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				AmbilDataAssetBanyakBerdasarkanStok.this.detach();
			}
		});
		cancel.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		button.addEventListener("onClick", new EventListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {
				if (eventListener != null && grid.getRows() != null && grid.getRows().getChildren() != null) {
					List<MasterAsset> masterAssets = new ArrayList<MasterAsset>();
					List<Row> rows = grid.getRows().getChildren();
					for (Row row : rows) {
						try {
							MyCheckboxConfig checkbox = (MyCheckboxConfig) row.getAttribute("checkbox");
							if (checkbox.isChecked() && !checkbox.isDisabled()) {
								MasterAsset myMasterAsset = (MasterAsset) row.getAttribute("masterAsset");
								masterAssets.add(myMasterAsset);
							}
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/asset/helper/AmbilDataAssetBanyakBerdasarkanStok.java:287");
							// TODO: handle exception
						}
					}
					Event myEvent = new Event("myEvent", event.getTarget(), masterAssets);
					eventListener.onEvent(myEvent);
				}
				AmbilDataAssetBanyakBerdasarkanStok.this.detach();
			}
		});
		button.setParent(toolbar);

	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Session session = HibernateUtil.currentSession();

		String sql = "select a.master_asset, max(c.nama) as nama_masterAsset, "
				+ "max(a.tanggal) as tanggal_terakhir_pengadaan, " + "sum((a.qty+a.qtybonus)*b.jenis) as stok "
				+ "from asset.detail_transaksi_asset a "
				+ "inner join library.kode_transaksi b on (a.kode_transaksi = b.id) "
				+ "left join asset.master_asset c on (a.master_asset = c.id and c.tipe='" + MasterAsset.TIPE_HABIS_PAKAI
				+ "') where  a.master_asset "
				+ (ids.size() == 0 ? "!=c.id"
						: "in (" + ids.toString().replaceAll("\\[", "").replaceAll("\\]", "") + ")")
				+ "  group by a.master_asset order by " + orderBy + " asc";

		System.out.println(sql);
		List<Object[]> masterAsset = session.createSQLQuery(sql).list();

		sql = "select a.master_asset, max(c.nama) as nama_masterAsset, "
				+ "max(a.tanggal) as tanggal_terakhir_pengadaan, " + "sum((a.qty+a.qtybonus)*b.jenis) as stok "
				+ "from asset.detail_transaksi_asset a "
				+ "inner join library.kode_transaksi b on (a.kode_transaksi = b.id) "
				+ "left join asset.master_asset c on (a.master_asset = c.id and c.tipe='" + MasterAsset.TIPE_HABIS_PAKAI
				+ "') where a.master_asset "
				+ (ids.size() == 0 ? "=c.id"
						: "not in (" + ids.toString().replaceAll("\\[", "").replaceAll("\\]", "") + ")")
				+ " and c.kode ilike '%" + kodeMasterAssetan.getValue().trim() + "%' and c.nama ilike '%"
				+ nama.getValue().trim() + "%' " + " and a.satuan_kerja = "
				+ (satuanKerja == null ? "a.satuan_kerja" : satuanKerja.getId()) + " group by a.master_asset order by "
				+ orderBy + " asc limit " + Common.MAX_RESULT_50;
		System.out.println(sql);
		List<Object[]> myMasterAsset = session.createSQLQuery(sql).list();

		masterAsset.addAll(myMasterAsset);

		ListModel strset = new SimpleListModel(masterAsset);
		grid.setRowRenderer(new MasterAssetRenderer());
		grid.setModelCheckMobile(strset);

	}

	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	public EventListener getEventListener() {
		return eventListener;
	}
}
