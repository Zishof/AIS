package ais.action.master.asset.helper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
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
import ais.database.hibernate.HibernateUtil;
import ais.database.model.asset.MasterAsset;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class AmbilDataMasterAssetBanyak extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6452461056684904810L;
	private MyGrid grid;
	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
	private EventListener eventListener;
	private List<MasterAsset> masterAssets;
	private List<MasterAsset> masterAssetsHanyaDitampilkan;

	private Set<Long> idsSudahDipakai = new HashSet<Long>();
	private Map<Long, MasterAsset> masterAssetsDipilih = new LinkedHashMap<Long, MasterAsset>();
	private String tipe = null;

	public AmbilDataMasterAssetBanyak(List<MasterAsset> masterAssets, String tipe) {
		super();
		this.tipe = tipe;
		this.masterAssets = masterAssets;
		initIdsSudahDipakai();

		display();

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});
	}

	public AmbilDataMasterAssetBanyak(List<MasterAsset> masterAssets, List<MasterAsset> masterAssetsHanyaDitampilkan,
			String tipe) {
		super();
		this.tipe = tipe;
		this.masterAssets = masterAssets;
		this.masterAssetsHanyaDitampilkan = masterAssetsHanyaDitampilkan;
		initIdsSudahDipakai();

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
	private MyTextbox merk;

	private void initIdsSudahDipakai() {
		idsSudahDipakai.clear();
		if (masterAssets == null) {
			return;
		}
		for (MasterAsset masterAsset : masterAssets) {
			if (masterAsset != null && masterAsset.getId() != null) {
				idsSudahDipakai.add(masterAsset.getId());
			}
		}
	}

	class MasterAssetRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {

			arg0.setValign("top");
			// TODO Auto-generated method stub
			final MasterAsset masterAsset = (MasterAsset) arg1;
			arg0.setAttribute("masterAsset", masterAsset);
			final Checkbox checkbox = new Checkbox(masterAsset.getKode());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			if (masterAsset.getId() != null && idsSudahDipakai.contains(masterAsset.getId())) {
				checkbox.setChecked(true);
				checkbox.setDisabled(true);
			} else {
				checkbox.setChecked(masterAsset.getId() != null && masterAssetsDipilih.containsKey(masterAsset.getId()));
			}

			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (masterAsset.getId() == null || idsSudahDipakai.contains(masterAsset.getId())) {
						return;
					}
					if (checkbox.isChecked()) {
						masterAssetsDipilih.put(masterAsset.getId(), masterAsset);
					} else {
						masterAssetsDipilih.remove(masterAsset.getId());
					}
				}
			});
			new Label(masterAsset.getNama()).setParent(arg0);
			new Label(masterAsset.getMerk()).setParent(arg0);
			new Label(masterAsset.getJenisAsset() == null ? "" : masterAsset.getJenisAsset().getNama()).setParent(arg0);
			new Label(masterAsset.getTipe()).setParent(arg0);
			new Label(masterAsset.getKelompokAsset() == null ? "" : masterAsset.getKelompokAsset().getNama())
					.setParent(arg0);

			new Label(masterAsset.getSpesifikasi()).setParent(arg0);
		}

	}

	public void display() {

		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(this);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Daftar Barang dan Jasa");
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
		ais.ui.util.ZkCompat.setFlex(north, true);

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

		row.appendChild(new ais.ui.util.MyLabelConfig("Merk"));
		row.appendChild(merk = new MyTextbox());
		merk.setWidth("90%");

		merk.addEventListener("onOK", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(null);
			}
		});

		Toolbar toolbar = new Toolbar();
		ais.ui.util.BanboxFilterToggle.pasang(north, searchgrid, toolbar);
		// toolbar.setHeight("25px");
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

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		/* Paging server-side (AmbilDataPagingHelper) menggantikan mold "paging"
		 * client-side yang dibatasi MAX_RESULT_100. */
		pagingHelper.pasangOnPaging(new EventListener() {
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});
		pagingHelper.pasangGridDanPaging(myCenter1, grid);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Kode");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Merk");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jenis");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Tipe");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Kelompok");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Spesifikasi");

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
				AmbilDataMasterAssetBanyak.this.detach();
			}
		});
		cancel.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		button.addEventListener("onClick", new EventListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {
				if (eventListener != null) {
					/*
					 * PENTING: sumber seleksi adalah Set<Long> ids yang dikelola persisten lewat onCheck di
					 * renderer — BUKAN grid.getRows().getChildren(). Grid ini memakai ListModel + renderer
					 * dengan mold "paging" (pageSize 10), sehingga getChildren() HANYA mengembalikan baris
					 * halaman AKTIF. Bila memakai getChildren(), centang di halaman lain hilang → user memilih
					 * 20 item tetapi hanya 10 (satu halaman) yang tersimpan. Dari ids, seluruh item terpilih
					 * di SEMUA halaman ikut terbawa.
					 */
					List<MasterAsset> masterAssets = new ArrayList<MasterAsset>(masterAssetsDipilih.values());
					Event myEvent = new Event("myEvent", event.getTarget(), masterAssets);
					eventListener.onEvent(myEvent);
				}
				AmbilDataMasterAssetBanyak.this.detach();
			}
		});
		button.setParent(toolbar);

	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();

		List<Long> values = new ArrayList<Long>();
		if (masterAssetsHanyaDitampilkan != null) {
			for (MasterAsset masterAsset : masterAssetsHanyaDitampilkan) {
				values.add(masterAsset.getId());
			}
		}

		Criteria criteria = session.createCriteria(MasterAsset.class)

				.add(tipe == null ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("tipe", tipe))

				.add(idsSudahDipakai.size() == 0 ? Restrictions.sqlRestriction("1=1")
						: Restrictions.not(Restrictions.in("id", idsSudahDipakai)))
				.add(masterAssetsDipilih.size() == 0 ? Restrictions.sqlRestriction("1=1")
						: Restrictions.not(Restrictions.in("id", masterAssetsDipilih.keySet())))
				.add(masterAssetsHanyaDitampilkan == null || values.size() == 0 ? Restrictions.sqlRestriction("1=1")
						: Restrictions.in("id", values))
				.add(nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.ANYWHERE))

				.add(merk.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("merk", merk.getValue().trim(), MatchMode.ANYWHERE))

				.add(kode.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("kode", kode.getValue().trim(), MatchMode.ANYWHERE))

		;

		if (order) {
			criteria.addOrder(Order.asc("nama"));
		}

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Session session = HibernateUtil.currentSession();

		List<MasterAsset> masterAsset = new ArrayList<MasterAsset>(masterAssetsDipilih.values());

		// Paging server-side ditangani sepenuhnya oleh pagingHelper (hitung total, offset, dan
		// komponen Paging tunggal). Tidak ada lagi paging legacy sehingga tak muncul pager ganda.
		List<MasterAsset> myMasterAsset = pagingHelper.cariDenganCriteria(
				initCriteria(true), MasterAsset.class);

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
