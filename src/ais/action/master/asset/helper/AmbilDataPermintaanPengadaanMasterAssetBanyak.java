package ais.action.master.asset.helper;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Div;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Groupbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.asset.PemesananPengadaanMasterAssetAction;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.akunting.UangMuka;
import ais.database.model.asset.PemesananPengadaanMasterAsset;
import ais.database.model.asset.PemesananPengadaanMasterAssetDetail;
import ais.database.model.asset.PenerimaanPengadaanMasterAsset;
import ais.database.model.asset.PerjanjianKerjasamaMasterAsset;
import ais.database.model.asset.PermintaanPengadaanMasterAsset;
import ais.database.model.asset.PermintaanPengadaanMasterAssetDetail;
import ais.database.model.rab.SatuanKerja;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class AmbilDataPermintaanPengadaanMasterAssetBanyak extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6452461056684904810L;
	private MyGrid grid;

	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
	private EventListener eventListener;
	private List<PermintaanPengadaanMasterAsset> permintaanPengadaanMasterAssets;
	private List<PermintaanPengadaanMasterAsset> permintaanPengadaanMasterAssetsHanyaDitampilkan;

	private List<PermintaanPengadaanMasterAssetDetail> idsData = new ArrayList<PermintaanPengadaanMasterAssetDetail>();

	private AmbilDataSatuanKerjaBanbox searchparent;
	private SatuanKerja satuanKerja = null;
	private boolean beliLangsung;

	public AmbilDataPermintaanPengadaanMasterAssetBanyak(boolean beliLangsung,
			List<PermintaanPengadaanMasterAsset> permintaanPengadaanMasterAssets, SatuanKerja satuanKerja) {
		super();
		this.beliLangsung = beliLangsung;
		this.satuanKerja = satuanKerja;
		this.permintaanPengadaanMasterAssets = permintaanPengadaanMasterAssets;
		try {
			display();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/asset/helper/AmbilDataPermintaanPengadaanMasterAssetBanyak.java:86");
		}
		
		onSearchDefault(null);
	}

	public AmbilDataPermintaanPengadaanMasterAssetBanyak(boolean beliLangsung,
			List<PermintaanPengadaanMasterAsset> permintaanPengadaanMasterAssets,
			List<PermintaanPengadaanMasterAsset> permintaanPengadaanMasterAssetsHanyaDitampilkan,
			SatuanKerja satuanKerja) {
		super();
		this.satuanKerja = satuanKerja;
		this.permintaanPengadaanMasterAssets = permintaanPengadaanMasterAssets;
		this.permintaanPengadaanMasterAssetsHanyaDitampilkan = permintaanPengadaanMasterAssetsHanyaDitampilkan;

		try {
			display();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/asset/helper/AmbilDataPermintaanPengadaanMasterAssetBanyak.java:105");
		}

		onSearchDefault(null);
	}

	private MyTextbox nama;
	private MyTextbox kode;
	private MyCheckboxConfig searchaktif;

	class PermintaanPengadaanMasterAssetRenderer extends ais.ui.util.MyRowRenderer {

		@SuppressWarnings("unchecked")
		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final PermintaanPengadaanMasterAsset permintaanPengadaanMasterAsset = (PermintaanPengadaanMasterAsset) arg1;
			arg0.setAttribute("permintaanPengadaanMasterAsset", permintaanPengadaanMasterAsset);

			Session session = HibernateUtil.currentSession();
			MyDetail detail = new MyDetail();
			detail.setParent(arg0);
			detail.setOpen(true);
			List<PermintaanPengadaanMasterAssetDetail> pengadaanMasterAssetDetails = session
					.createCriteria(PermintaanPengadaanMasterAssetDetail.class)
					.add(Restrictions.eq("permintaanPengadaanMasterAsset", permintaanPengadaanMasterAsset))
					.createAlias("masterAsset", "masterAsset").addOrder(Order.asc("masterAsset.nama")).list();

			Groupbox groupbox = new Groupbox();
			groupbox.setParent(detail);

			Grid grid = new Grid();
			grid.setSclass("dgrid");
			grid.setHeight("100%");
			grid.setWidth("100%");
			grid.setParent(groupbox);

			Columns columns = new Columns();
			columns.setParent(grid);

			MyColumnConfig column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Kode Barang/Jasa");
			column.setWidth("30%");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Nama Barang/Jasa");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Qty");
			column.setAlign("right");
			column.setWidth("10%");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Harga");
			column.setAlign("right");
			column.setWidth("12%");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Total");
			column.setAlign("right");
			column.setWidth("12%");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Qty BAST");
			column.setAlign("right");
			column.setWidth("10%");

			Rows myrows = new Rows();
			myrows.setParent(grid);

			for (final PermintaanPengadaanMasterAssetDetail assetDetail : pengadaanMasterAssetDetails) {
				MyFormRow myrow = new MyFormRow();
				myrows.appendChild(myrow);

				final Checkbox checkbox = new Checkbox(assetDetail.getMasterAsset().getKode());

				Vbox aA = new Vbox();

				if (assetDetail.getPerjanjianKerjasamaMasterAssetDetail() != null) {

					String kode = assetDetail.getPerjanjianKerjasamaMasterAssetDetail()
							.getPerjanjianKerjasamaMasterAsset().getKode();

					aA.appendChild(RevisiHelper.createNewRevisi(PerjanjianKerjasamaMasterAsset.class,
							assetDetail.getPerjanjianKerjasamaMasterAssetDetail().getPerjanjianKerjasamaMasterAsset(),
							kode));
				}

				if (assetDetail.getUangMuka() != null) {

					String kode = assetDetail.getUangMuka().getKode();

					aA.appendChild(RevisiHelper.createNewRevisi(UangMuka.class, assetDetail.getUangMuka(), kode));
				}

				List<PemesananPengadaanMasterAssetDetail> objects = session
						.createCriteria(PemesananPengadaanMasterAssetDetail.class)
						.add(Restrictions.eq("permintaanPengadaanMasterAssetDetail", assetDetail)).list();

				Double pemesanan = 0.0;
				for (PemesananPengadaanMasterAssetDetail pemesananPengadaanMasterAssetDetail : objects) {
					String kode = pemesananPengadaanMasterAssetDetail.getPemesananPengadaanMasterAsset().getKode();

					aA.appendChild(RevisiHelper.createNewRevisi(PemesananPengadaanMasterAsset.class,
							pemesananPengadaanMasterAssetDetail.getPemesananPengadaanMasterAsset(), kode));

					if (pemesananPengadaanMasterAssetDetail.getPenerimaanPengadaanMasterAssetDetail() != null
							&& pemesananPengadaanMasterAssetDetail.getPenerimaanPengadaanMasterAssetDetail()
									.getPenerimaanPengadaanMasterAsset() != null) {
						aA.appendChild(RevisiHelper.createNewRevisi(PenerimaanPengadaanMasterAsset.class,
								pemesananPengadaanMasterAssetDetail.getPenerimaanPengadaanMasterAssetDetail()
										.getPenerimaanPengadaanMasterAsset(),
								pemesananPengadaanMasterAssetDetail.getPenerimaanPengadaanMasterAssetDetail()
										.getPenerimaanPengadaanMasterAsset().getKode()));
					}

					pemesanan += pemesananPengadaanMasterAssetDetail.getPenerimaanPengadaanMasterAssetDetail() == null
							? 0.0
							: pemesananPengadaanMasterAssetDetail.getPenerimaanPengadaanMasterAssetDetail()
									.getDiterima();
				}

				if (pemesanan.intValue() >= assetDetail.getJumlah().intValue()) {
					new Label(assetDetail.getMasterAsset().getKode()).setParent(myrow);
				} else if (checkbox.isDisabled()) {
					new Label(assetDetail.getMasterAsset().getKode()).setParent(myrow);
				} else {
					checkbox.setParent(myrow);
				}

				checkbox.setChecked(idsData.contains(assetDetail));
				checkbox.addEventListener("onCheck", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						if (checkbox.isChecked()) {
							idsData.add(assetDetail);
						} else {
							idsData.remove(assetDetail);
						}
					}
				});
				Vbox v = new Vbox();
				v.setParent(myrow);

				new Label(assetDetail.getMasterAsset().getNama()).setParent(v);

				aA.setParent(v);

				new MyLabelKecil(Common.numberFormat.get().format(assetDetail.getJumlah())).setParent(myrow);
				new MyLabelKecil(Common.numberFormat.get().format(assetDetail.getHargaBeli())).setParent(myrow);

				Label total = new Label();
				total.setParent(myrow);
				total.setValue(Common.numberFormat.get().format((assetDetail.getJumlah() * assetDetail.getHargaBeli())));

				new MyLabelKecil(Common.numberFormat.get().format(pemesanan)).setParent(myrow);

				if (pemesanan.intValue() != assetDetail.getJumlahDatang().intValue()) {
					assetDetail.setJumlahDatang(pemesanan);
					Common.refreshUpdate(assetDetail);
				}

			}

			new Label(permintaanPengadaanMasterAsset.getKode()).setParent(arg0);

			new Label(permintaanPengadaanMasterAsset.getSatuanKerja() == null ? ""
					: permintaanPengadaanMasterAsset.getSatuanKerja().getNama()).setParent(arg0);
			new Label(permintaanPengadaanMasterAsset.getDisetujuiOleh() == null ? ""
					: permintaanPengadaanMasterAsset.getDisetujuiOleh().getUserNama()).setParent(arg0);
			new Label(permintaanPengadaanMasterAsset.getTanggalPersetujuan() == null ? "" :

					Common.dateFormat51.get().format(permintaanPengadaanMasterAsset.getTanggalPersetujuan())

			).setParent(arg0);

			new Label(permintaanPengadaanMasterAsset.getKeterangan()).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Tutup");
			checkbox.setChecked(permintaanPengadaanMasterAsset.getTutup());
			checkbox.setParent(arg0);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					permintaanPengadaanMasterAsset.setTutup(checkbox.isChecked());
					Common.refreshSaveOrUpdate(permintaanPengadaanMasterAsset);
				}
			});
		}

	}

	public void display() throws Exception {

		setTitle("Daftar Permintaan Barang / Jasa (PR)");
		setBorder("none");
		setClosable(true);

		satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);
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

		row.appendChild(new ais.ui.util.MyLabelConfig("Kode"));
		row.appendChild(kode = new MyTextbox());
		kode.addEventListener(Events.ON_OK, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(nama = new MyTextbox());
		nama.addEventListener(Events.ON_OK, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		row.appendChild(new ais.ui.util.MyLabelConfig("Satuan Kerja"));
		row.appendChild(searchparent = new AmbilDataSatuanKerjaBanbox(true));
		searchparent.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});

		Tbmuser tbmuser = Common.getCurrentUser();
		if (satuanKerja != null && tbmuser != null && tbmuser.hakAkses() != null
				&& !tbmuser.hakAkses().getMelihatDataSatkerLain()) {
			searchparent.setValue(satuanKerja.getNama());
			searchparent.setAttribute("satuanKerja", satuanKerja);
			searchparent.setAttribute("myValue", satuanKerja);
			searchparent.setDisabled(true);
		}

		Toolbar toolbar = new Toolbar();
		Row rowKedua = new Row();
		rowKedua.setParent(rowsUtama);
		toolbar.setHeight("32px");
		toolbar.setParent(rowKedua);

		toolbar.appendChild(searchaktif = new MyCheckboxConfig("Tampilkan hanya yang belum tutup"));
		searchaktif.setChecked(true);
		searchaktif.addEventListener("onClick", new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

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
		/* Paging server-side (AmbilDataPagingHelper) menggantikan mold "paging"
		 * client-side yang dibatasi MAX_RESULT. */
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
		column.setWidth("0%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Kode Permintaan");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Satuan Kerja");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Disetujui");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Disetujui Wkt");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Keterangan");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Tutup");
		column.setWidth("8%");

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
				AmbilDataPermintaanPengadaanMasterAssetBanyak.this.detach();
			}
		});
		cancel.setParent(toolbar);

		if (beliLangsung) {
			button = new MyToolbarbuttonConfig("Beli Langsung", "/img/svg/cash.svg");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					PemesananPengadaanMasterAsset pemesananPengadaanMasterAsset = new PemesananPengadaanMasterAsset();

					String s = "";
					String a = "";

					Double nilai = 0.0;
					for (PermintaanPengadaanMasterAssetDetail permintaanPengadaanMasterAssetDetail : idsData) {

						nilai += permintaanPengadaanMasterAssetDetail.getHargaBeli()
								* permintaanPengadaanMasterAssetDetail.getJumlah();

						s += s.isEmpty() ? permintaanPengadaanMasterAssetDetail.getId().toString()
								: "," + permintaanPengadaanMasterAssetDetail.getId();

						if (permintaanPengadaanMasterAssetDetail.getPermintaanPengadaanMasterAsset()
								.getWorkspace() != null) {
							a += a.isEmpty()
									? permintaanPengadaanMasterAssetDetail.getPermintaanPengadaanMasterAsset()
											.getWorkspace().getId().toString()
									: "," + permintaanPengadaanMasterAssetDetail.getPermintaanPengadaanMasterAsset()
											.getWorkspace().getId();
						}
					}
					pemesananPengadaanMasterAsset.setPermintaanPengadaanMasterAssets(s);
					pemesananPengadaanMasterAsset.setAngarans(a);
					pemesananPengadaanMasterAsset.setDp(nilai);

					pemesananPengadaanMasterAsset.setPembelianLangsung(true);
					pemesananPengadaanMasterAsset.setKeterangan("Pembelian langsung");

					PemesananPengadaanMasterAssetAction.onAddExternal(eventListener, pemesananPengadaanMasterAsset,
							true);

					AmbilDataPermintaanPengadaanMasterAssetBanyak.this.detach();
				}
			});
			button.setParent(toolbar);
		} else {

			button = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					if (eventListener != null && grid.getRows() != null && grid.getRows().getChildren() != null) {
						Event myEvent = new Event("myEvent", event.getTarget(), idsData);
						eventListener.onEvent(myEvent);
					}
					AmbilDataPermintaanPengadaanMasterAssetBanyak.this.detach();
				}
			});
			button.setParent(toolbar);
		}

	}

	private SatuanKerjaTreeModel satuanKerjaTreeModel;

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		SatuanKerja parent = (SatuanKerja) searchparent.getAttribute("satuanKerja");
		Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
		if (parent != null) {
			satuanKerjas.clear();
			satuanKerjas.add(parent);
			satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
		}

		Session session = HibernateUtil.currentSession();

		List<Long> values = new ArrayList<Long>();
		if (permintaanPengadaanMasterAssetsHanyaDitampilkan != null) {
			for (PermintaanPengadaanMasterAsset permintaanPengadaanMasterAsset : permintaanPengadaanMasterAssetsHanyaDitampilkan) {
				values.add(permintaanPengadaanMasterAsset.getId());
			}
		}

		List<Long> valuesNot = new ArrayList<Long>();
		if (permintaanPengadaanMasterAssets != null) {
			for (PermintaanPengadaanMasterAsset permintaanPengadaanMasterAsset : permintaanPengadaanMasterAssets) {
				valuesNot.add(permintaanPengadaanMasterAsset.getId());
			}
		}

		List<Long> ids = new ArrayList<Long>();
		for (PermintaanPengadaanMasterAssetDetail masterAssetDetail : idsData) {
			ids.add(masterAssetDetail.getPermintaanPengadaanMasterAsset().getId());
		}

		List<PermintaanPengadaanMasterAsset> permintaanPengadaanMasterAsset = session
				.createCriteria(PermintaanPengadaanMasterAsset.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

				.add(searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("tutup"), Restrictions.eq("tutup", false))
						: Restrictions.sqlRestriction("true"))

				.add(satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.isNull("satuanKerja"),
								Restrictions.or(
										parent == null ? Restrictions.isNull("satuanKerja")
												: Restrictions.sqlRestriction("false"),
										Restrictions.in("satuanKerja", satuanKerjas))))

				.add(Restrictions.isNotNull("disetujuiOleh")).addOrder(Order.desc("id"))

				.add(idsData.size() == 0 ? Restrictions.sqlRestriction("1!=1") : Restrictions.in("id", ids))

				.add(valuesNot.size() == 0 ? Restrictions.sqlRestriction("true")
						: Restrictions.not(Restrictions.in("id", valuesNot)))

				.list();

		List<PermintaanPengadaanMasterAsset> myPermintaanPengadaanMasterAsset = session
				.createCriteria(PermintaanPengadaanMasterAsset.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

				.add(searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("tutup"), Restrictions.eq("tutup", false))
						: Restrictions.sqlRestriction("true"))

				.add(satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.isNull("satuanKerja"),
								Restrictions.or(
										parent == null ? Restrictions.isNull("satuanKerja")
												: Restrictions.sqlRestriction("false"),
										Restrictions.in("satuanKerja", satuanKerjas))))

				.add(Restrictions.isNotNull("disetujuiOleh")).addOrder(Order.desc("id"))

				.add(permintaanPengadaanMasterAssetsHanyaDitampilkan == null || values.size() == 0
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.in("id", values))

				.add(kode.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("kode", kode.getValue().trim(), MatchMode.ANYWHERE))

				.add(nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("keterangan", nama.getValue().trim(), MatchMode.ANYWHERE))

				.add(valuesNot.size() == 0 ? Restrictions.sqlRestriction("true")
						: Restrictions.not(Restrictions.in("id", valuesNot)))

				.setMaxResults(Common.MAX_RESULT).list();

		permintaanPengadaanMasterAsset.addAll(myPermintaanPengadaanMasterAsset);

		ListModel strset = new SimpleListModel(permintaanPengadaanMasterAsset);
		grid.setRowRenderer(new PermintaanPengadaanMasterAssetRenderer());
		grid.setModelCheckMobile(strset);

	}

	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	public EventListener getEventListener() {
		return eventListener;
	}
}
