package ais.action.master.rab;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Doublebox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.dao.DaoFactory;
import ais.database.dao.rab.RencanaDanRealisasiOutputKegiatanDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.rab.OutputKegiatan;
import ais.database.model.rab.RencanaDanRealisasiOutputKegiatan;
import ais.database.model.rab.Satuan;
import ais.database.model.rab.SatuanKerja;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class RencanaDanRealisasiOutputKegiatanAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private AmbilDataSatuanKerjaBanbox searchparent;

	private Combobox outputKegiatan;
	private Combobox satuan;
	private AmbilDataSatuanKerjaBanbox satuanKerja;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private RencanaDanRealisasiOutputKegiatan rencanaDanRealisasiOutputKegiatan;
	private MyToolbarbuttonConfig add;

	private SatuanKerjaTreeModel satuanKerjaTreeModel;

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

		satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		searchparent.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});
	}

	class RencanaDanRealisasiOutputKegiatanRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final RencanaDanRealisasiOutputKegiatan rencanaDanRealisasiOutputKegiatan = (RencanaDanRealisasiOutputKegiatan) arg1;

			new Label(rencanaDanRealisasiOutputKegiatan.getSatuanKerja() == null ? ""
					: rencanaDanRealisasiOutputKegiatan.getSatuanKerja().toString()).setParent(arg0);
			new Label(rencanaDanRealisasiOutputKegiatan.getOutputKegiatan() == null ? ""
					: rencanaDanRealisasiOutputKegiatan.getOutputKegiatan().getKode()).setParent(arg0);
			RevisiHelper.createNewRevisi(RencanaDanRealisasiOutputKegiatan.class, rencanaDanRealisasiOutputKegiatan,
					rencanaDanRealisasiOutputKegiatan.getOutputKegiatan() == null ? ""
							: rencanaDanRealisasiOutputKegiatan.getOutputKegiatan().getNama())
					.setParent(arg0);

			final Doublebox targetBulan1 = new Doublebox(rencanaDanRealisasiOutputKegiatan.getTargetBulan1());
			final Doublebox realisasiBulan1 = new Doublebox(rencanaDanRealisasiOutputKegiatan.getRealisasiBulan1());
			final Doublebox prosentaseRealisasiBulan1 = new Doublebox(
					rencanaDanRealisasiOutputKegiatan.getProsentaseRealisasiBulan1());

			new Vbox(new Component[] { new Hbox(new Component[] { new Label("T"), targetBulan1 }),
					new Hbox(new Component[] { new Label("R"), realisasiBulan1 }),
					new Hbox(new Component[] { new Label("%"), prosentaseRealisasiBulan1 }) }).setParent(arg0);

			final Doublebox targetBulan2 = new Doublebox(rencanaDanRealisasiOutputKegiatan.getTargetBulan2());
			final Doublebox realisasiBulan2 = new Doublebox(rencanaDanRealisasiOutputKegiatan.getRealisasiBulan2());
			final Doublebox prosentaseRealisasiBulan2 = new Doublebox(
					rencanaDanRealisasiOutputKegiatan.getProsentaseRealisasiBulan2());

			new Vbox(new Component[] { new Hbox(new Component[] { new Label("T"), targetBulan2 }),
					new Hbox(new Component[] { new Label("R"), realisasiBulan2 }),
					new Hbox(new Component[] { new Label("%"), prosentaseRealisasiBulan2 }) }).setParent(arg0);

			final Doublebox targetBulan3 = new Doublebox(rencanaDanRealisasiOutputKegiatan.getTargetBulan3());
			final Doublebox realisasiBulan3 = new Doublebox(rencanaDanRealisasiOutputKegiatan.getRealisasiBulan3());
			final Doublebox prosentaseRealisasiBulan3 = new Doublebox(
					rencanaDanRealisasiOutputKegiatan.getProsentaseRealisasiBulan3());

			new Vbox(new Component[] { new Hbox(new Component[] { new Label("T"), targetBulan3 }),
					new Hbox(new Component[] { new Label("R"), realisasiBulan3 }),
					new Hbox(new Component[] { new Label("%"), prosentaseRealisasiBulan3 }) }).setParent(arg0);

			final Doublebox targetBulan4 = new Doublebox(rencanaDanRealisasiOutputKegiatan.getTargetBulan4());
			final Doublebox realisasiBulan4 = new Doublebox(rencanaDanRealisasiOutputKegiatan.getRealisasiBulan4());
			final Doublebox prosentaseRealisasiBulan4 = new Doublebox(
					rencanaDanRealisasiOutputKegiatan.getProsentaseRealisasiBulan4());

			new Vbox(new Component[] { new Hbox(new Component[] { new Label("T"), targetBulan4 }),
					new Hbox(new Component[] { new Label("R"), realisasiBulan4 }),
					new Hbox(new Component[] { new Label("%"), prosentaseRealisasiBulan4 }) }).setParent(arg0);

			final Doublebox targetBulan5 = new Doublebox(rencanaDanRealisasiOutputKegiatan.getTargetBulan5());
			final Doublebox realisasiBulan5 = new Doublebox(rencanaDanRealisasiOutputKegiatan.getRealisasiBulan5());
			final Doublebox prosentaseRealisasiBulan5 = new Doublebox(
					rencanaDanRealisasiOutputKegiatan.getProsentaseRealisasiBulan5());

			new Vbox(new Component[] { new Hbox(new Component[] { new Label("T"), targetBulan5 }),
					new Hbox(new Component[] { new Label("R"), realisasiBulan5 }),
					new Hbox(new Component[] { new Label("%"), prosentaseRealisasiBulan5 }) }).setParent(arg0);

			final Doublebox targetBulan6 = new Doublebox(rencanaDanRealisasiOutputKegiatan.getTargetBulan6());
			final Doublebox realisasiBulan6 = new Doublebox(rencanaDanRealisasiOutputKegiatan.getRealisasiBulan6());
			final Doublebox prosentaseRealisasiBulan6 = new Doublebox(
					rencanaDanRealisasiOutputKegiatan.getProsentaseRealisasiBulan6());

			new Vbox(new Component[] { new Hbox(new Component[] { new Label("T"), targetBulan6 }),
					new Hbox(new Component[] { new Label("R"), realisasiBulan6 }),
					new Hbox(new Component[] { new Label("%"), prosentaseRealisasiBulan6 }) }).setParent(arg0);

			final Doublebox targetBulan7 = new Doublebox(rencanaDanRealisasiOutputKegiatan.getTargetBulan7());
			final Doublebox realisasiBulan7 = new Doublebox(rencanaDanRealisasiOutputKegiatan.getRealisasiBulan7());
			final Doublebox prosentaseRealisasiBulan7 = new Doublebox(
					rencanaDanRealisasiOutputKegiatan.getProsentaseRealisasiBulan7());

			new Vbox(new Component[] { new Hbox(new Component[] { new Label("T"), targetBulan7 }),
					new Hbox(new Component[] { new Label("R"), realisasiBulan7 }),
					new Hbox(new Component[] { new Label("%"), prosentaseRealisasiBulan7 }) }).setParent(arg0);

			final Doublebox targetBulan8 = new Doublebox(rencanaDanRealisasiOutputKegiatan.getTargetBulan8());
			final Doublebox realisasiBulan8 = new Doublebox(rencanaDanRealisasiOutputKegiatan.getRealisasiBulan8());
			final Doublebox prosentaseRealisasiBulan8 = new Doublebox(
					rencanaDanRealisasiOutputKegiatan.getProsentaseRealisasiBulan8());

			new Vbox(new Component[] { new Hbox(new Component[] { new Label("T"), targetBulan8 }),
					new Hbox(new Component[] { new Label("R"), realisasiBulan8 }),
					new Hbox(new Component[] { new Label("%"), prosentaseRealisasiBulan8 }) }).setParent(arg0);

			final Doublebox targetBulan9 = new Doublebox(rencanaDanRealisasiOutputKegiatan.getTargetBulan9());
			final Doublebox realisasiBulan9 = new Doublebox(rencanaDanRealisasiOutputKegiatan.getRealisasiBulan9());
			final Doublebox prosentaseRealisasiBulan9 = new Doublebox(
					rencanaDanRealisasiOutputKegiatan.getProsentaseRealisasiBulan9());

			new Vbox(new Component[] { new Hbox(new Component[] { new Label("T"), targetBulan9 }),
					new Hbox(new Component[] { new Label("R"), realisasiBulan9 }),
					new Hbox(new Component[] { new Label("%"), prosentaseRealisasiBulan9 }) }).setParent(arg0);

			final Doublebox targetBulan10 = new Doublebox(rencanaDanRealisasiOutputKegiatan.getTargetBulan10());
			final Doublebox realisasiBulan10 = new Doublebox(rencanaDanRealisasiOutputKegiatan.getRealisasiBulan10());
			final Doublebox prosentaseRealisasiBulan10 = new Doublebox(
					rencanaDanRealisasiOutputKegiatan.getProsentaseRealisasiBulan10());

			new Vbox(new Component[] { new Hbox(new Component[] { new Label("T"), targetBulan10 }),
					new Hbox(new Component[] { new Label("R"), realisasiBulan10 }),
					new Hbox(new Component[] { new Label("%"), prosentaseRealisasiBulan10 }) }).setParent(arg0);

			final Doublebox targetBulan11 = new Doublebox(rencanaDanRealisasiOutputKegiatan.getTargetBulan11());
			final Doublebox realisasiBulan11 = new Doublebox(rencanaDanRealisasiOutputKegiatan.getRealisasiBulan11());
			final Doublebox prosentaseRealisasiBulan11 = new Doublebox(
					rencanaDanRealisasiOutputKegiatan.getProsentaseRealisasiBulan11());

			new Vbox(new Component[] { new Hbox(new Component[] { new Label("T"), targetBulan11 }),
					new Hbox(new Component[] { new Label("R"), realisasiBulan11 }),
					new Hbox(new Component[] { new Label("%"), prosentaseRealisasiBulan11 }) }).setParent(arg0);

			final Doublebox targetBulan12 = new Doublebox(rencanaDanRealisasiOutputKegiatan.getTargetBulan12());
			final Doublebox realisasiBulan12 = new Doublebox(rencanaDanRealisasiOutputKegiatan.getRealisasiBulan12());
			final Doublebox prosentaseRealisasiBulan12 = new Doublebox(
					rencanaDanRealisasiOutputKegiatan.getProsentaseRealisasiBulan12());

			new Vbox(new Component[] { new Hbox(new Component[] { new Label("T"), targetBulan12 }),
					new Hbox(new Component[] { new Label("R"), realisasiBulan12 }),
					new Hbox(new Component[] { new Label("%"), prosentaseRealisasiBulan12 }) }).setParent(arg0);

			final Label target;
			(target = new Label(Common.numberFormat.get().format(rencanaDanRealisasiOutputKegiatan.getTarget())))
					.setParent(arg0);

			final Label realisasi;
			(realisasi = new Label(Common.numberFormat.get().format(rencanaDanRealisasiOutputKegiatan.getRealisasi())))
					.setParent(arg0);

			final Label prosentase;
			(prosentase = new Label(Common.numberFormat.get().format(rencanaDanRealisasiOutputKegiatan.getProsentase())))
					.setParent(arg0);

			final Textbox kendala = new Textbox(rencanaDanRealisasiOutputKegiatan.getKendala());
			kendala.setRows(4);
			kendala.setWidth("90%");
			kendala.setParent(arg0);

			final Textbox solusi = new Textbox(rencanaDanRealisasiOutputKegiatan.getSolusi());
			solusi.setRows(4);
			solusi.setWidth("90%");
			solusi.setParent(arg0);

			final Textbox keterangan = new Textbox(rencanaDanRealisasiOutputKegiatan.getKeterangan());
			keterangan.setRows(4);
			keterangan.setWidth("90%");
			keterangan.setParent(arg0);

			EventListener eventListener = new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					rencanaDanRealisasiOutputKegiatan
							.setProsentaseRealisasiBulan1(prosentaseRealisasiBulan1.getValue());
					rencanaDanRealisasiOutputKegiatan
							.setProsentaseRealisasiBulan10(prosentaseRealisasiBulan10.getValue());
					rencanaDanRealisasiOutputKegiatan
							.setProsentaseRealisasiBulan11(prosentaseRealisasiBulan11.getValue());
					rencanaDanRealisasiOutputKegiatan
							.setProsentaseRealisasiBulan12(prosentaseRealisasiBulan12.getValue());
					rencanaDanRealisasiOutputKegiatan
							.setProsentaseRealisasiBulan2(prosentaseRealisasiBulan2.getValue());
					rencanaDanRealisasiOutputKegiatan
							.setProsentaseRealisasiBulan3(prosentaseRealisasiBulan3.getValue());
					rencanaDanRealisasiOutputKegiatan
							.setProsentaseRealisasiBulan4(prosentaseRealisasiBulan4.getValue());
					rencanaDanRealisasiOutputKegiatan
							.setProsentaseRealisasiBulan5(prosentaseRealisasiBulan5.getValue());
					rencanaDanRealisasiOutputKegiatan
							.setProsentaseRealisasiBulan6(prosentaseRealisasiBulan6.getValue());
					rencanaDanRealisasiOutputKegiatan
							.setProsentaseRealisasiBulan7(prosentaseRealisasiBulan7.getValue());
					rencanaDanRealisasiOutputKegiatan
							.setProsentaseRealisasiBulan8(prosentaseRealisasiBulan8.getValue());
					rencanaDanRealisasiOutputKegiatan
							.setProsentaseRealisasiBulan9(prosentaseRealisasiBulan9.getValue());

					rencanaDanRealisasiOutputKegiatan.setTargetBulan1(targetBulan1.getValue());
					rencanaDanRealisasiOutputKegiatan.setTargetBulan10(targetBulan10.getValue());
					rencanaDanRealisasiOutputKegiatan.setTargetBulan11(targetBulan11.getValue());
					rencanaDanRealisasiOutputKegiatan.setTargetBulan12(targetBulan12.getValue());
					rencanaDanRealisasiOutputKegiatan.setTargetBulan2(targetBulan2.getValue());
					rencanaDanRealisasiOutputKegiatan.setTargetBulan3(targetBulan3.getValue());
					rencanaDanRealisasiOutputKegiatan.setTargetBulan4(targetBulan4.getValue());
					rencanaDanRealisasiOutputKegiatan.setTargetBulan5(targetBulan5.getValue());
					rencanaDanRealisasiOutputKegiatan.setTargetBulan6(targetBulan6.getValue());
					rencanaDanRealisasiOutputKegiatan.setTargetBulan7(targetBulan7.getValue());
					rencanaDanRealisasiOutputKegiatan.setTargetBulan8(targetBulan8.getValue());
					rencanaDanRealisasiOutputKegiatan.setTargetBulan9(targetBulan9.getValue());

					rencanaDanRealisasiOutputKegiatan.setRealisasiBulan1(realisasiBulan1.getValue());
					rencanaDanRealisasiOutputKegiatan.setRealisasiBulan10(realisasiBulan10.getValue());
					rencanaDanRealisasiOutputKegiatan.setRealisasiBulan11(realisasiBulan11.getValue());
					rencanaDanRealisasiOutputKegiatan.setRealisasiBulan12(realisasiBulan12.getValue());
					rencanaDanRealisasiOutputKegiatan.setRealisasiBulan2(realisasiBulan2.getValue());
					rencanaDanRealisasiOutputKegiatan.setRealisasiBulan3(realisasiBulan3.getValue());
					rencanaDanRealisasiOutputKegiatan.setRealisasiBulan4(realisasiBulan4.getValue());
					rencanaDanRealisasiOutputKegiatan.setRealisasiBulan5(realisasiBulan5.getValue());
					rencanaDanRealisasiOutputKegiatan.setRealisasiBulan6(realisasiBulan6.getValue());
					rencanaDanRealisasiOutputKegiatan.setRealisasiBulan7(realisasiBulan7.getValue());
					rencanaDanRealisasiOutputKegiatan.setRealisasiBulan8(realisasiBulan8.getValue());
					rencanaDanRealisasiOutputKegiatan.setRealisasiBulan9(realisasiBulan9.getValue());

					target.setValue(Common.numberFormat.get().format(rencanaDanRealisasiOutputKegiatan.getTarget()));
					realisasi.setValue(Common.numberFormat.get().format(rencanaDanRealisasiOutputKegiatan.getRealisasi()));
					prosentase.setValue(Common.numberFormat.get().format(rencanaDanRealisasiOutputKegiatan.getProsentase()));

					rencanaDanRealisasiOutputKegiatan.setKendala(kendala.getValue());
					rencanaDanRealisasiOutputKegiatan.setSolusi(solusi.getValue());
					rencanaDanRealisasiOutputKegiatan.setKeterangan(keterangan.getValue());

					Session session = HibernateUtil.currentSession();
					session.update((rencanaDanRealisasiOutputKegiatan));

				}
			};

			kendala.addEventListener("onChange", eventListener);
			solusi.addEventListener("onChange", eventListener);
			keterangan.addEventListener("onChange", eventListener);

			targetBulan1.addEventListener("onChange", eventListener);
			targetBulan2.addEventListener("onChange", eventListener);
			targetBulan3.addEventListener("onChange", eventListener);
			targetBulan4.addEventListener("onChange", eventListener);
			targetBulan5.addEventListener("onChange", eventListener);
			targetBulan6.addEventListener("onChange", eventListener);
			targetBulan7.addEventListener("onChange", eventListener);
			targetBulan8.addEventListener("onChange", eventListener);
			targetBulan9.addEventListener("onChange", eventListener);
			targetBulan10.addEventListener("onChange", eventListener);
			targetBulan11.addEventListener("onChange", eventListener);
			targetBulan12.addEventListener("onChange", eventListener);

			realisasiBulan1.addEventListener("onChange", eventListener);
			realisasiBulan2.addEventListener("onChange", eventListener);
			realisasiBulan3.addEventListener("onChange", eventListener);
			realisasiBulan4.addEventListener("onChange", eventListener);
			realisasiBulan5.addEventListener("onChange", eventListener);
			realisasiBulan6.addEventListener("onChange", eventListener);
			realisasiBulan7.addEventListener("onChange", eventListener);
			realisasiBulan8.addEventListener("onChange", eventListener);
			realisasiBulan9.addEventListener("onChange", eventListener);
			realisasiBulan10.addEventListener("onChange", eventListener);
			realisasiBulan11.addEventListener("onChange", eventListener);
			realisasiBulan12.addEventListener("onChange", eventListener);

			prosentaseRealisasiBulan1.addEventListener("onChange", eventListener);
			prosentaseRealisasiBulan2.addEventListener("onChange", eventListener);
			prosentaseRealisasiBulan3.addEventListener("onChange", eventListener);
			prosentaseRealisasiBulan4.addEventListener("onChange", eventListener);
			prosentaseRealisasiBulan5.addEventListener("onChange", eventListener);
			prosentaseRealisasiBulan6.addEventListener("onChange", eventListener);
			prosentaseRealisasiBulan7.addEventListener("onChange", eventListener);
			prosentaseRealisasiBulan8.addEventListener("onChange", eventListener);
			prosentaseRealisasiBulan9.addEventListener("onChange", eventListener);
			prosentaseRealisasiBulan10.addEventListener("onChange", eventListener);
			prosentaseRealisasiBulan11.addEventListener("onChange", eventListener);
			prosentaseRealisasiBulan12.addEventListener("onChange", eventListener);

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(rencanaDanRealisasiOutputKegiatan);
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			button.setParent(toolbar);

			button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
			button.setVisible(delete);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {
											RencanaDanRealisasiOutputKegiatanDao rencanaDanRealisasiOutputKegiatanDao = DaoFactory
													.getInstance().getRencanaDanRealisasiOutputKegiatanDao();
											// rencanaDanRealisasiOutputKegiatanDao.beginTransaction();
											rencanaDanRealisasiOutputKegiatanDao
													.delete((rencanaDanRealisasiOutputKegiatan));
											// rencanaDanRealisasiOutputKegiatanDao.commitTransaction();
											onSearchDefault(event);
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											MyMessageboxConfig.show(
													"Data ini tidak dapat dihapus .., karena berelasi dengan data lainnya, error-nya adalah sbagai berikut:"
															+ e.getMessage());
										}

									}

								}
							});

				}
			});
			button.setParent(toolbar);
			ais.ui.util.MenuAksiBaris.pasang(toolbar);
			toolbar.setParent(arg0);
		}

	}

	public void onAdd(Event event) throws Exception {
		init(new RencanaDanRealisasiOutputKegiatan());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(RencanaDanRealisasiOutputKegiatan rencanaDanRealisasiOutputKegiatan) throws Exception {
		this.rencanaDanRealisasiOutputKegiatan = rencanaDanRealisasiOutputKegiatan;
		addWindow.setTitle(rencanaDanRealisasiOutputKegiatan.getId() == null ? "Tambah RencanaDanRealisasiOutputKegiatan" : "Ubah RencanaDanRealisasiOutputKegiatan");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("35%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Output Kegiatan"));
		row.appendChild(outputKegiatan = new Combobox());
		Common.insertCombo(outputKegiatan, "kode", "nama", OutputKegiatan.class);
		outputKegiatan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Satuan Kerja"));
		row.appendChild(satuanKerja = new AmbilDataSatuanKerjaBanbox(false));
		satuanKerja.setValue(rencanaDanRealisasiOutputKegiatan.getSatuanKerja() == null
				? (Common.getCurrentUser().ambilSatuanKerja() == null ? ""
						: Common.getCurrentUser().ambilSatuanKerja().toString())
				: rencanaDanRealisasiOutputKegiatan.getSatuanKerja().toString());
		satuanKerja.setAttribute("satuanKerja",
				rencanaDanRealisasiOutputKegiatan.getSatuanKerja() == null ? Common.getCurrentUser().ambilSatuanKerja()
						: rencanaDanRealisasiOutputKegiatan.getSatuanKerja());
		satuanKerja.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Satuan / Unit"));
		row.appendChild(satuan = new Combobox());
		Common.insertCombo(satuan, "nama", "keterangan", Satuan.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		satuan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(rencanaDanRealisasiOutputKegiatan.getKeterangan() == null ? ""
				: rencanaDanRealisasiOutputKegiatan.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);
		keterangan.setRows(4);

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(south);
		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				addWindow.setVisible(false);
			}
		});
		cancel.setParent(toolbar);
		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (onSave(event)) {
					onSearchDefault(null);
					addWindow.setVisible(false);
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);

	}

	public boolean onSave(Event event) throws Exception {
		if (outputKegiatan.getSelectedItem() == null) {
			MyMessageboxConfig.show("Output Kegiatan harus dipilih", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (satuan.getSelectedItem() == null) {
			MyMessageboxConfig.show("Satuan harus dipilih", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (satuanKerja.getAttribute("satuanKerja") == null) {
			MyMessageboxConfig.show("Satuan Kerja harus dipilih", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		RencanaDanRealisasiOutputKegiatanDao rencanaDanRealisasiOutputKegiatanDao = DaoFactory.getInstance()
				.getRencanaDanRealisasiOutputKegiatanDao();
		if (rencanaDanRealisasiOutputKegiatan.getId() != null) {
			rencanaDanRealisasiOutputKegiatan = rencanaDanRealisasiOutputKegiatanDao
					.load(rencanaDanRealisasiOutputKegiatan.getId());

		}

		rencanaDanRealisasiOutputKegiatan
				.setOutputKegiatan((OutputKegiatan) outputKegiatan.getSelectedItem().getValue());
		rencanaDanRealisasiOutputKegiatan.setSatuan((Satuan) satuan.getSelectedItem().getValue());
		rencanaDanRealisasiOutputKegiatan.setKeterangan(keterangan.getValue());
		rencanaDanRealisasiOutputKegiatan.setSatuanKerja((SatuanKerja) satuanKerja.getAttribute("satuanKerja"));

		if (rencanaDanRealisasiOutputKegiatan.getId() != null) {
			rencanaDanRealisasiOutputKegiatanDao.update(rencanaDanRealisasiOutputKegiatan);
		} else {
			rencanaDanRealisasiOutputKegiatanDao.save(rencanaDanRealisasiOutputKegiatan);
		}

		return true;
	}

	public Criteria initCriteria(boolean order) {
		SatuanKerja parent = (SatuanKerja) searchparent.getAttribute("satuanKerja");
		Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
		if (parent != null) {
			satuanKerjas.clear();
			satuanKerjas.add(parent);
			satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
		}

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(RencanaDanRealisasiOutputKegiatan.class)
				.createAlias("outputKegiatan", "outputKegiatan");
		if (order)
			criteria.addOrder(Order.asc("outputKegiatan.kode"));
		criteria.add(satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
				: Restrictions.or(
						parent == null ? Restrictions.isNull("satuanKerja") : Restrictions.sqlRestriction("false"),
						Restrictions.in("satuanKerja", satuanKerjas)))
				.add(Restrictions.ilike("outputKegiatan.nama", searchnama.getValue(), MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<RencanaDanRealisasiOutputKegiatan> rencanaDanRealisasiOutputKegiatan = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(rencanaDanRealisasiOutputKegiatan);
		grid.setRowRenderer(new RencanaDanRealisasiOutputKegiatanRenderer());
		grid.setModelCheckMobile(strset);

	}

}
