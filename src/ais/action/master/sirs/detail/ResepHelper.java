package ais.action.master.sirs.detail;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Div;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Messagebox;
import ais.ui.util.MyMessageboxConfig;
import org.zkoss.zul.North;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import org.zkoss.zul.RowRenderer;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.sirs.helper.AmbilDataItemMedisBanyak;
import ais.action.master.sirs.helper.AmbilDataRacikanBanyak;
import ais.action.master.sirs.helper.BuatRacikanBaruHelper;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.common.listener.OnSave;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.sirs.DiagnosaPenyakit;
import ais.database.model.sirs.ItemMedis;
import ais.database.model.sirs.JenisItemMedis;
import ais.database.model.sirs.PaketPerawatanDetail;
import ais.database.model.sirs.Racikan;
import ais.database.model.sirs.Resep;
import ais.database.model.sirs.ResepDetail;
import ais.database.model.sirs.Tindakan;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyTextbox;

public class ResepHelper {

	private Grid grid;
	private Resep resep;
	private MyTextbox kode;
	private DiagnosaPenyakit diagnosaPenyakit;

	private boolean delete = false;

	private Paging paging;
	private OnSave onSave;
	private Toolbarbutton save;
	private North north;

	public ResepHelper(OnSave onSave, Toolbarbutton save) {
		this.save = save;
		this.onSave = onSave;
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		paging = new Paging();
		Common.initPaging(paging, new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(arg0);
			}
		});
	}

	public Borderlayout display(final DiagnosaPenyakit diagnosaPenyakit) {
		this.diagnosaPenyakit = diagnosaPenyakit;
		if (diagnosaPenyakit != null && diagnosaPenyakit.getId() != null) {
			Session session = HibernateUtil.currentSession();
			resep = (Resep) session.createCriteria(Resep.class)
					.add(Restrictions.eq("diagnosaPenyakit", diagnosaPenyakit))
					.setMaxResults(1).uniqueResult();
		}

		Borderlayout borderlayout = new Borderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, true);

		Div div = new Div();
		div.setParent(north);

		Grid searchgrid = new Grid();
		searchgrid.setParent(div);

		Rows rows = new Rows();
		rows.setParent(searchgrid);

		Row row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Kode Resep")));
		row.appendChild(kode = new MyTextbox(resep == null
				|| resep.getId() == null ? Common.generateCode(Resep.class, 8,
				"RSP") : resep.getKode()));
		kode.setWidth("90%");
		kode.setDisabled(true);

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("25px");
		toolbar.setParent(div);

		Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig("Ambil Obat",
				"/img/add_item.png");
		button.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {

				if (diagnosaPenyakit.getId() == null)
					if (!onSave.onSave(event)) {
						return;
					}

				// Session session = HibernateUtil.currentSession();

				// List<Item> items = session
				// .createCriteria(ResepDetail.class)
				// .add(Restrictions.isNotNull("item"))
				// .setProjection(Projections.groupProperty("item"))
				// .add(Restrictions.eq("resep", resep)).list();

				AmbilDataItemMedisBanyak ambilDataItemBanyak = new AmbilDataItemMedisBanyak(
						new ArrayList<ItemMedis>(), new JenisItemMedis());
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot()
						.appendChild(ambilDataItemBanyak);
				ambilDataItemBanyak.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						List<ItemMedis> items = (List<ItemMedis>) arg0.getData();
						save.setDisabled(items.size() == 0);
						Session session = HibernateUtil.currentSession();

						if (resep == null || resep.getId() == null) {
							resep = new Resep();
							resep.setDiagnosaPenyakit(diagnosaPenyakit);
							resep.setKeterangan("");
							resep.setKode(Common.generateCode(Resep.class, 8,
									"RSP"));
							kode.setValue(resep.getKode());
							session.save(resep);
						}

						for (ItemMedis item : items) {
							ResepDetail resepDetail = new ResepDetail();
							resepDetail.setItem(item);
							resepDetail.setJumlah(0.0);
							resepDetail.setKeterangan("");
							resepDetail.setResep(resep);
							session.save(resepDetail);
						}

						loadData(null);
					}
				});
				ambilDataItemBanyak.setWidth("95%");
				ambilDataItemBanyak.setHeight("97%");
				ambilDataItemBanyak.setVisible(true);
				ambilDataItemBanyak.onModal();
			}

		});
		button.setParent(toolbar);

		button = new ais.ui.util.MyToolbarbuttonConfig("Ambil Racikan", "/img/add_item.png");
		button.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {

				if (diagnosaPenyakit.getId() == null)
					if (!onSave.onSave(event)) {
						return;
					}

				Session session = HibernateUtil.currentSession();

				List<Racikan> racikans = session
						.createCriteria(ResepDetail.class)
						.add(Restrictions.isNotNull("racikan"))
						.setProjection(Projections.groupProperty("racikan"))
						.add(Restrictions.eq("resep", resep)).list();

				AmbilDataRacikanBanyak ambilDataRacikanBanyak = new AmbilDataRacikanBanyak(
						racikans);
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot()
						.appendChild(ambilDataRacikanBanyak);
				ambilDataRacikanBanyak.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						List<Racikan> racikans = (List<Racikan>) arg0.getData();

						save.setDisabled(racikans.size() == 0);
						Session session = HibernateUtil.currentSession();

						if (resep == null || resep.getId() == null) {
							resep = new Resep();
							resep.setDiagnosaPenyakit(diagnosaPenyakit);
							resep.setKeterangan("");
							resep.setKode(Common.generateCode(Resep.class, 8,
									"RSP"));
							kode.setValue(resep.getKode());
							session.save(resep);
						}

						for (Racikan racikan : racikans) {
							ResepDetail resepDetail = new ResepDetail();
							resepDetail.setRacikan(racikan);
							resepDetail.setJumlah(0.0);
							resepDetail.setKeterangan("");
							resepDetail.setResep(resep);
							session.save(resepDetail);
						}

						loadData(null);
					}
				});
				ambilDataRacikanBanyak.setWidth("95%");
				ambilDataRacikanBanyak.setHeight("97%");
				ambilDataRacikanBanyak.setVisible(true);
				ambilDataRacikanBanyak.onModal();
			}

		});
		button.setParent(toolbar);

		button = new ais.ui.util.MyToolbarbuttonConfig("Racikan Baru", "/img/add_item.png");
		button.setParent(toolbar);
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				BuatRacikanBaruHelper buatRacikanBaruHelper = new BuatRacikanBaruHelper(
						new EventListener() {

							@Override
							public void onEvent(Event event) throws Exception {
								Racikan racikan = (Racikan) event.getData();
								if (racikan != null) {

									Session session = HibernateUtil.currentSession();

									if (resep == null || resep.getId() == null) {
										resep = new Resep();
										resep.setDiagnosaPenyakit(diagnosaPenyakit);
										resep.setKeterangan("");
										resep.setKode(Common.generateCode(
												Resep.class, 8, "RSP"));
										kode.setValue(resep.getKode());
										session.save(resep);
									}

									ResepDetail resepDetail = new ResepDetail();
									resepDetail.setRacikan(racikan);
									resepDetail.setJumlah(0.0);
									resepDetail.setKeterangan("");
									resepDetail.setResep(resep);
									session.save(resepDetail);

									save.setDisabled(false);

									loadData(null);
								}
							}
						});

				buatRacikanBaruHelper.setParent(ExecutionsCtrl.getCurrentCtrl()
						.getCurrentPage().getFirstRoot());
				buatRacikanBaruHelper.setHeight("95%");
				buatRacikanBaruHelper.setWidth("700px");
				buatRacikanBaruHelper.onModal();
			}
		});

		// BuatRacikanBaruHelper

		grid = new Grid();
		grid.setMold("paging");
		grid.setPageSize(25);
		grid.setParent(center);

		Columns columns = new Columns();

		columns.setParent(grid);

		Column column = new Column();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("40px");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Kode");
		column.setWidth("15%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Nama");
		column.setWidth("25%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Jumlah");
		column.setWidth("15%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Satuan");
		column.setWidth("15%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Keterangan");

		column = new Column();
		column.setParent(columns);
		column.setWidth("10%");

		loadData(null);

		return borderlayout;
	}

	@SuppressWarnings("unchecked")
	public void loadData(Object value) {
		Session session = HibernateUtil.currentSession();
		List<ResepDetail> resepDetails = resep == null || resep.getId() == null ? new ArrayList<ResepDetail>()
				: session.createCriteria(ResepDetail.class)
						.addOrder(Order.desc("id"))
						.add(Restrictions.eq("resep", resep)).list();

		ListModel strset = new SimpleListModel(resepDetails);
		grid.setRowRenderer(new ResepDetailRenderer());
		grid.setModel(strset);
		grid.renderAll();
	}

	@SuppressWarnings("unchecked")
	public boolean setPaket(Set<Tindakan> pakets,
			DiagnosaPenyakit diagnosaPenyakit) throws Exception {
		this.diagnosaPenyakit = diagnosaPenyakit;
		if (north != null) {
			north.setVisible(true);
			Common.freeze(grid, false);
		}

		if (pakets.isEmpty()) {
			return true;
		}
		Session session = HibernateUtil.currentSession();
		List<ItemMedis> items = new ArrayList<ItemMedis>();

		for (Tindakan tindakan : pakets) {
			List<PaketPerawatanDetail> paketPerawatanDetails = session
					.createCriteria(PaketPerawatanDetail.class)
					.add(Restrictions.eq("paketPerawatan", tindakan)).list();
			for (PaketPerawatanDetail paketPerawatanDetail : paketPerawatanDetails) {
				if (paketPerawatanDetail.getItem() != null) {
					items.add(paketPerawatanDetail.getItem());
				}
			}
		}
		if (items.isEmpty()) {
			return true;
		}

		if (diagnosaPenyakit.getId() == null)
			if (!onSave.onSave(null)) {
				return false;
			}

		if (resep == null || resep.getId() == null) {
			resep = new Resep();
			resep.setDiagnosaPenyakit(diagnosaPenyakit);
			resep.setKeterangan("");
			resep.setKode(Common.generateCode(Resep.class, 8, "RSP"));
			session.save(resep);
		}

		List<ResepDetail> resepDetails = new ArrayList<ResepDetail>();
		for (Tindakan tindakan : pakets) {
			List<PaketPerawatanDetail> paketPerawatanDetails = session
					.createCriteria(PaketPerawatanDetail.class)
					.add(Restrictions.eq("paketPerawatan", tindakan)).list();
			for (PaketPerawatanDetail paketPerawatanDetail : paketPerawatanDetails) {
				if (paketPerawatanDetail.getItem() != null) {
					ItemMedis item = paketPerawatanDetail.getItem();
					ResepDetail resepDetail = (ResepDetail) session
							.createCriteria(ResepDetail.class)
							.add(Restrictions.eq("item", item))
							.add(Restrictions.eq("resep", resep))
							.setMaxResults(1).uniqueResult();
					if (resepDetail == null) {
						resepDetail = new ResepDetail();
						resepDetail.setItem(item);
						resepDetail.setJumlah(paketPerawatanDetail.getJumlah());
						resepDetail.setKeterangan(paketPerawatanDetail
								.getKeterangan());
						resepDetail.setResep(resep);
						session.save(resepDetail);
					}
					resepDetails.add(resepDetail);
				}
			}
		}

		if (north != null) {
			ListModel strset = new SimpleListModel(resepDetails);
			grid.setRowRenderer(new ResepDetailRenderer());
			grid.setModel(strset);
			grid.renderAll();

			north.setVisible(false);
			Common.freeze(grid, true);
		}

		return true;
	}

	class ResepDetailRenderer extends ais.ui.util.MyRowRenderer {

		public ResepDetailRenderer() {

		}

		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");
			final ResepDetail resepDetail = (ResepDetail) data;

			if (resepDetail.getItem() != null) {
				new Label("").setParent(row);
				new Label(resepDetail.getItem() == null ? "" : resepDetail
						.getItem().getKode()).setParent(row);

				RevisiHelper.createNewRevisi(
						ResepDetail.class,
						resepDetail,
						resepDetail.getItem() == null ? "" : resepDetail
								.getItem().getNama()).setParent(row);
			} else {

				final Label namaRacikan = new Label(
						resepDetail.getRacikan() == null ? "" : resepDetail
								.getRacikan().getNama());

				new ResepRacikanDetailAction(
						resepDetail.getRacikan(),
						resepDetail,
						diagnosaPenyakit.getPendaftaran() == null
								|| diagnosaPenyakit.getPendaftaran()
										.getKelasPerawatan() == null ? ConstantValues.kelasNormal
								: diagnosaPenyakit.getPendaftaran()
										.getKelasPerawatan(),
						new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								Racikan racikan = (Racikan) arg0.getData();
								namaRacikan.setValue(racikan.getNama());
							}
						}).setParent(row);

				RevisiHelper.createNewRevisi(
						ResepDetail.class,
						resepDetail,
						resepDetail.getRacikan() == null ? "" : resepDetail
								.getRacikan().getKode()).setParent(row);

				namaRacikan.setParent(row);

			}

			final MyDoublebox jumlah;
			(jumlah = new MyDoublebox(resepDetail.getJumlah() == null ? 0.0
					: resepDetail.getJumlah())).setParent(row);
			jumlah.setStyle("text-align:right");
			jumlah.setWidth("90%");
			jumlah.setWidth("90%");
			jumlah.addEventListener(Events.ON_CHANGE, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					Session session = HibernateUtil.currentSession();
					resepDetail.setJumlah(jumlah.getValue());
					session.update(resepDetail);

				}
			});

			new Label(resepDetail.getItem() == null ? "Racik" : resepDetail
					.getItem().getSatuanItem() == null ? "" : resepDetail
					.getItem().getSatuanItem().getNama()).setParent(row);

			final MyTextbox keterangan = new MyTextbox(
					resepDetail.getKeterangan() == null ? ""
							: resepDetail.getKeterangan());
			keterangan.setWidth("90%");
			keterangan.setHeight("95%");
			keterangan.setParent(row);

			keterangan.addEventListener(Events.ON_CHANGE, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					resepDetail.setKeterangan(keterangan.getValue());
					session.update(resepDetail);
				}
			});

			Hbox toolbar = new Hbox();

			Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig("", "/img/delete.gif");
			button.setTooltiptext("Hapus Data");
			button.setVisible(delete);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah Bapak/Ibu yakin ingin menghapus data ini? Perlu diketahui, data yang telah dihapus tidak dapat dikembalikan.",
							"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
							MyMessageboxConfig.QUESTION, new EventListener() {

								@Override
								public void onEvent(Event event)
										throws Exception {
									int i = new Integer(event.getData()
											.toString());
									if (i == MyMessageboxConfig.OK) {
										try {
											Common.refreshDelete(resepDetail); 

											loadData(null);

										} catch (Exception e) {
											e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sirs/detail/ResepHelper.java:561");
											MyMessageboxConfig.show(Common.pesan(
												"Mohon maaf, data ini tidak dapat dihapus karena masih berelasi dengan data lainnya. Rincian teknis kesalahan: {V1}. Langkah yang dapat dilakukan: (1) pastikan tidak ada data lain yang masih terkait dengan data ini; (2) hapus terlebih dahulu seluruh data yang berelasi; (3) apabila kendala masih berlanjut, mohon hubungi administrator sistem.",
													e.getMessage()));
										}

									}

								}
							});

				}

			});
			button.setParent(toolbar);
			toolbar.setParent(row);

		}

	}

}
