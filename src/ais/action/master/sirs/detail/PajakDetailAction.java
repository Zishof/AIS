package ais.action.master.sirs.detail;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Caption;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Groupbox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Messagebox;
import ais.ui.util.MyMessageboxConfig;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import org.zkoss.zul.RowRenderer;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Space;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;

import ais.action.master.sirs.helper.AmbilDataAlatMedisBanyak;
import ais.action.master.sirs.helper.AmbilDataItemMedisBanyak;
import ais.action.master.sirs.helper.AmbilDataTindakanBanyak;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.sirs.AlatMedis;
import ais.database.model.sirs.ItemMedis;
import ais.database.model.sirs.PajakDetail;
import ais.database.model.sirs.PajakMedis;
import ais.database.model.sirs.Tindakan;
import ais.ui.util.MyTextbox;

public class PajakDetailAction extends MyDetail {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5086031585928643232L;

	private PajakMedis pajak;
	private Paging paging;
	private Grid grid;

	public PajakDetailAction(PajakMedis pajak) {
		super();
		this.pajak = pajak;
		this.addEventListener(Events.ON_OPEN, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(PajakDetailAction.this);
				if (isOpen()) {
					display();
				}
			}
		});

	}

	class PajakDetailRenderer extends ais.ui.util.MyRowRenderer {

		public PajakDetailRenderer() {

		}

		@Override
		public void render(final Row arg0, Object data) throws Exception {
			final PajakDetail pajakDetail = (PajakDetail) data;
			final ItemMedis item = pajakDetail.getItem();
			final Tindakan tindakan = pajakDetail.getTindakan();
			final AlatMedis alatMedis = pajakDetail.getAlatMedis();

			if (item != null) {
				new Label(item.getKode()).setParent(arg0);
				new Label(item.getNama()).setParent(arg0);
				new Label(ais.common.Common.getBahasaConfig("Item dan Obat")).setParent(arg0);
			} else if (tindakan != null) {
				new Label(tindakan.getKode()).setParent(arg0);
				new Label(tindakan.getNama()).setParent(arg0);
				new Label(ais.common.Common.getBahasaConfig("Tindakan dan Perawatan")).setParent(arg0);
			} else if (alatMedis != null) {
				new Label(alatMedis.getKode()).setParent(arg0);
				new Label(alatMedis.getNama()).setParent(arg0);
				new Label(ais.common.Common.getBahasaConfig("Alat Medis dan Kesehatan")).setParent(arg0);
			}

			if (item != null) {
				new Label(item.getSatuanItem() == null ? "" : item.getSatuanItem().getNama()).setParent(arg0);
			} else if (tindakan != null) {
				new Label(ais.common.Common.getBahasaConfig("Perawatan")).setParent(arg0);
			} else if (alatMedis != null) {
				new Label(alatMedis.getPer()).setParent(arg0);
			}

			final MyTextbox keterangan = new MyTextbox(
					pajakDetail.getKeterangan() == null ? "" : pajakDetail.getKeterangan());
			keterangan.setWidth("90%");
			keterangan.setParent(arg0);

			keterangan.addEventListener(Events.ON_CHANGE, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					pajakDetail.setKeterangan(keterangan.getValue());
					Common.refreshUpdate(session, (pajakDetail));
				}
			});

			Hbox toolbar = new Hbox();

			Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig("", "/img/delete.gif");
			button.setTooltiptext("Hapus Data");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah Bapak/Ibu yakin ingin menghapus data ini? Data yang sudah dihapus tidak dapat dikembalikan.", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = new Integer(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {
											Common.refreshDelete(pajakDetail);

											loadData(null);

										} catch (Exception e) {
											e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sirs/detail/PajakDetailAction.java:147");
											MyMessageboxConfig.show(Common.pesan(
													"Mohon maaf, data ini tidak dapat dihapus karena masih berelasi dengan data lainnya. Langkah yang dapat dilakukan: (1) periksa dan hapus terlebih dahulu data lain yang terkait dengan data ini; (2) pastikan tidak ada transaksi yang masih menggunakan data ini; (3) apabila kendala berlanjut, mohon hubungi administrator sistem. Rincian kesalahan: {V1}"
															, e.getMessage()));
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

	private MyTextbox kode;
	private MyTextbox nama;
	private Checkbox isItem;
	private Checkbox isTindakan;
	private Checkbox isAlatMedis;

	private Criteria initCriteria(boolean order) {

		Criterion critKode = Restrictions.sqlRestriction("false");
		if (!kode.getValue().trim().equals("")) {
			critKode = Restrictions.or(critKode,
					Restrictions.ilike("item.kode", kode.getValue().trim(), MatchMode.ANYWHERE));
			critKode = Restrictions.or(critKode,
					Restrictions.ilike("tindakan.kode", kode.getValue().trim(), MatchMode.ANYWHERE));
			critKode = Restrictions.or(critKode,
					Restrictions.ilike("alatMedis.kode", kode.getValue().trim(), MatchMode.ANYWHERE));
		} else {
			critKode = Restrictions.sqlRestriction("true");
		}

		Criterion critNama = Restrictions.sqlRestriction("false");
		if (!nama.getValue().trim().equals("")) {
			critNama = Restrictions.or(critNama,
					Restrictions.ilike("item.nama", nama.getValue().trim(), MatchMode.ANYWHERE));
			critNama = Restrictions.or(critNama,
					Restrictions.ilike("tindakan.nama", nama.getValue().trim(), MatchMode.ANYWHERE));
			critNama = Restrictions.or(critNama,
					Restrictions.ilike("alatMedis.nama", nama.getValue().trim(), MatchMode.ANYWHERE));
		} else {
			critNama = Restrictions.sqlRestriction("true");
		}

		Criterion crit = Restrictions.sqlRestriction("false");
		Boolean ada = false;
		if (isAlatMedis.isChecked()) {
			crit = Restrictions.or(crit, Restrictions.isNotNull("alatMedis"));
			ada = true;
		}
		if (isItem.isChecked()) {
			crit = Restrictions.or(crit, Restrictions.isNotNull("item"));
			ada = true;
		}
		if (isTindakan.isChecked()) {
			crit = Restrictions.or(crit, Restrictions.isNotNull("tindakan"));
			ada = true;
		}

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(PajakDetail.class)

				.createAlias("item", "item", Criteria.LEFT_JOIN).createAlias("tindakan", "tindakan", Criteria.LEFT_JOIN)
				.createAlias("alatMedis", "alatMedis", Criteria.LEFT_JOIN)

				.add(ada ? crit : Restrictions.sqlRestriction("false"))

				.add(critKode).add(critNama)

				.add(Restrictions.eq("pajak", pajak));
		if (order)
			criteria.addOrder(Order.asc("item.nama")).addOrder(Order.asc("tindakan.nama"))
					.addOrder(Order.asc("alatMedis.nama"));

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void loadData(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<PajakDetail> pajakDetails = pajak == null
				|| pajak.getId() == null
						? new ArrayList<PajakDetail>()
						: ConstantValues
								.simpleList(
										initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
												.setFirstResult(Common.ROWS_COUNT_ON_PAGE
														* (paging == null ? 0 : paging.getActivePage())),
										PajakDetail.class);

		ListModel strset = new SimpleListModel(pajakDetails);
		grid.setRowRenderer(new PajakDetailRenderer());
		grid.setModel(strset);
		grid.renderAll();
	}

	private void display() {

		Groupbox groupbox = new Groupbox();
		groupbox.setParent(this);
		groupbox.appendChild(new Caption("Daftar Item Pajak"));

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("25px");
		toolbar.setParent(groupbox);
		Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig("Ambil Obat", "/img/add_item.png");
		button.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {
				Session session = HibernateUtil.currentSession();

				List<ItemMedis> items = ConstantValues.simpleList(
						session.createCriteria(PajakDetail.class).setProjection(Projections.groupProperty("item.id"))
								.add(Restrictions.isNotNull("item")).add(Restrictions.eq("pajak", pajak)),
						ItemMedis.class, false);

				AmbilDataItemMedisBanyak ambilDataItemBanyak = new AmbilDataItemMedisBanyak(items);
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(ambilDataItemBanyak);
				ambilDataItemBanyak.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						List<ItemMedis> items = (List<ItemMedis>) arg0.getData();
						Session session = HibernateUtil.currentSession();
						for (ItemMedis item : items) {
							PajakDetail pajakDetail = new PajakDetail();
							pajakDetail.setItem(item);
							pajakDetail.setKeterangan("");
							pajakDetail.setPajak(pajak);
							session.save(pajakDetail);
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

		button = new ais.ui.util.MyToolbarbuttonConfig("Ambil Tindakan", "/img/add_item.png");
		button.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {
				Session session = HibernateUtil.currentSession();

				List<Tindakan> tindakans = ConstantValues.simpleList(session.createCriteria(PajakDetail.class)
						.add(Restrictions.isNotNull("tindakan")).setProjection(Projections.groupProperty("tindakan.id"))
						.add(Restrictions.eq("pajak", pajak)), Tindakan.class, false);

				AmbilDataTindakanBanyak ambilDataTindakanBanyak = new AmbilDataTindakanBanyak(tindakans);
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(ambilDataTindakanBanyak);
				ambilDataTindakanBanyak.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						List<Tindakan> tindakans = (List<Tindakan>) arg0.getData();
						Session session = HibernateUtil.currentSession();
						for (Tindakan tindakan : tindakans) {
							PajakDetail pajakDetail = new PajakDetail();
							pajakDetail.setTindakan(tindakan);
							pajakDetail.setKeterangan("");
							pajakDetail.setPajak(pajak);
							session.save(pajakDetail);
						}

						loadData(null);
					}
				});
				ambilDataTindakanBanyak.setWidth("95%");
				ambilDataTindakanBanyak.setHeight("97%");
				ambilDataTindakanBanyak.setVisible(true);
				ambilDataTindakanBanyak.onModal();
			}

		});
		button.setParent(toolbar);

		button = new ais.ui.util.MyToolbarbuttonConfig("Ambil Alat Medis", "/img/add_item.png");
		button.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {
				Session session = HibernateUtil.currentSession();

				List<AlatMedis> alatMediss = ConstantValues.simpleList(session.createCriteria(PajakDetail.class)
						.add(Restrictions.isNotNull("alatMedis"))
						.setProjection(Projections.groupProperty("alatMedis.id")).add(Restrictions.eq("pajak", pajak)),
						AlatMedis.class, false);

				AmbilDataAlatMedisBanyak ambilDataAlatMedisBanyak = new AmbilDataAlatMedisBanyak(alatMediss,
						AlatMedis.JENIS_UMUM);
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(ambilDataAlatMedisBanyak);
				ambilDataAlatMedisBanyak.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						List<AlatMedis> alatMediss = (List<AlatMedis>) arg0.getData();
						Session session = HibernateUtil.currentSession();
						for (AlatMedis alatMedis : alatMediss) {
							PajakDetail pajakDetail = new PajakDetail();
							pajakDetail.setAlatMedis(alatMedis);
							pajakDetail.setKeterangan("");
							pajakDetail.setPajak(pajak);
							session.save(pajakDetail);
						}

						loadData(null);
					}
				});
				ambilDataAlatMedisBanyak.setWidth("95%");
				ambilDataAlatMedisBanyak.setHeight("97%");
				ambilDataAlatMedisBanyak.setVisible(true);
				ambilDataAlatMedisBanyak.onModal();
			}

		});
		button.setParent(toolbar);

		button = new ais.ui.util.MyToolbarbuttonConfig("Ambil Bed", "/img/add_item.png");
		button.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {
				Session session = HibernateUtil.currentSession();

				List<AlatMedis> alatMediss = ConstantValues.simpleList(session.createCriteria(PajakDetail.class)
						.add(Restrictions.isNotNull("alatMedis"))
						.setProjection(Projections.groupProperty("alatMedis.id")).add(Restrictions.eq("pajak", pajak)),
						AlatMedis.class, false);

				AmbilDataAlatMedisBanyak ambilDataAlatMedisBanyak = new AmbilDataAlatMedisBanyak(alatMediss,
						AlatMedis.JENIS_TEMPAT_TIDUR);
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(ambilDataAlatMedisBanyak);
				ambilDataAlatMedisBanyak.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						List<AlatMedis> alatMediss = (List<AlatMedis>) arg0.getData();
						Session session = HibernateUtil.currentSession();
						for (AlatMedis alatMedis : alatMediss) {
							PajakDetail pajakDetail = new PajakDetail();
							pajakDetail.setAlatMedis(alatMedis);
							pajakDetail.setKeterangan("");
							pajakDetail.setPajak(pajak);
							session.save(pajakDetail);
						}

						loadData(null);
					}
				});
				ambilDataAlatMedisBanyak.setWidth("95%");
				ambilDataAlatMedisBanyak.setHeight("97%");
				ambilDataAlatMedisBanyak.setVisible(true);
				ambilDataAlatMedisBanyak.onModal();
			}

		});
		button.setParent(toolbar);

		toolbar.appendChild(new Space());
		toolbar.appendChild(new Space());
		toolbar.appendChild(new Space());
		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Kode:")));
		toolbar.appendChild(kode = new MyTextbox());
		kode.setWidth("80px");
		kode.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(arg0);
			}
		});

		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Nama:")));
		toolbar.appendChild(nama = new MyTextbox());
		nama.setWidth("80px");
		nama.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(arg0);
			}
		});

		toolbar.appendChild(isItem = new Checkbox("Obat saja"));
		isItem.setChecked(true);
		isItem.addEventListener("onCheck", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(arg0);
			}
		});

		toolbar.appendChild(isTindakan = new Checkbox("Perawatan saja"));
		isTindakan.setChecked(true);
		isTindakan.addEventListener("onCheck", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(arg0);
			}
		});

		toolbar.appendChild(isAlatMedis = new Checkbox("Alkes saja"));
		isAlatMedis.setChecked(true);
		isAlatMedis.addEventListener("onCheck", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(arg0);
			}
		});

		Toolbarbutton search;
		toolbar.appendChild(search = new ais.ui.util.MyToolbarbuttonConfig("", "/img/search.gif"));
		search.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(arg0);
			}
		});

		grid = new Grid();
		grid.setMold("paging");
		grid.setPageSize(25);
		grid.setParent(groupbox);

		Columns columns = new Columns();

		columns.setParent(grid);

		Column column = new Column();
		column.setParent(columns);
		column.setLabel("Kode");
		column.setWidth("15%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Nama");
		column.setWidth("20%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Jenis");
		column.setWidth("10%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Satuan");
		column.setWidth("15%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Keterangan");

		column = new Column();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("10%");

		paging = new Paging();
		Common.initPaging(paging, new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(arg0);
			}
		});
		paging.setParent(groupbox);

		loadData(null);
	}

}
