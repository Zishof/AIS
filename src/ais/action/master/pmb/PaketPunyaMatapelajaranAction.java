package ais.action.master.pmb;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Toolbar;

import ais.action.master.helper.generic.AmbilDataMatapelajaranSekolahBanyak;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.MatapelajaranSekolah;
import ais.database.model.Paket;
import ais.database.model.PaketPunyaMatapelajaran;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class PaketPunyaMatapelajaranAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7896939206824822505L;
	private PaketPunyaMatapelajaran paketPunyaMatapelajaran;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Combobox matapelajaranSekolah;
	private Combobox searchjurusansekolah;
	private Combobox paket;
	private Combobox searchpaket;

	private boolean edit = true;
	private boolean delete = true;

	private Paket selectedPaket;

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

		Common.insertCombo(matapelajaranSekolah = new Combobox(), "nama", MatapelajaranSekolah.class);
		Common.insertCombo(searchjurusansekolah, "nama", MatapelajaranSekolah.class);
		Common.insertCombo(paket = new Combobox(), "nama", "keterangan", Paket.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.insertCombo(searchpaket, "nama", "keterangan", Paket.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		if (execution.getParameter("paket") != null) {
			selectedPaket = (Paket) HibernateUtil.currentSession().createCriteria(Paket.class)
					.add(Restrictions.idEq(Long.parseLong(execution.getParameter("paket")))).uniqueResult();
			Common.selectComboItem(searchpaket, selectedPaket);
			searchpaket.setDisabled(true);
		}

		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});
	}

	class PilihanPaketRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final PaketPunyaMatapelajaran paketPunyaMatapelajaran = (PaketPunyaMatapelajaran) arg1;

			new Label(paketPunyaMatapelajaran.getPaket().getNama()).setParent(arg0);
			new Label(paketPunyaMatapelajaran.getMatapelajaranSekolah() == null ? ""
					: paketPunyaMatapelajaran.getMatapelajaranSekolah().getNama()).setParent(arg0);

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(paketPunyaMatapelajaran);
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
											Common.refreshDelete(paketPunyaMatapelajaran);
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
			toolbar.setParent(arg0);
		}

	}

	@SuppressWarnings("unchecked")
	public void onAdd(Event event) throws Exception {
		// init(new PaketPunyaMatapelajaran());
		// addWindow.setVisible(true);
		// addWindow.onModal();

		List<MatapelajaranSekolah> matapelajaranSekolahs = HibernateUtil.currentSession()
				.createCriteria(PaketPunyaMatapelajaran.class)
				.add(selectedPaket == null ? Restrictions.isNull("paket") : Restrictions.eq("paket", selectedPaket))
				.setProjection(Projections.groupProperty("matapelajaranSekolah")).list();

		AmbilDataMatapelajaranSekolahBanyak window = new AmbilDataMatapelajaranSekolahBanyak(matapelajaranSekolahs);

		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);
		window.setWidth("870px");
		window.setHeight("90%");

		window.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				List<MatapelajaranSekolah> matapelajaranSekolahs = (List<MatapelajaranSekolah>) arg0.getData();

				if (matapelajaranSekolahs != null) {
					Session session = HibernateUtil.currentSession();
					for (MatapelajaranSekolah matapelajaranSekolah : matapelajaranSekolahs) {

						PaketPunyaMatapelajaran paketPunyaMatapelajaran = new PaketPunyaMatapelajaran();
						paketPunyaMatapelajaran.setMatapelajaranSekolah(matapelajaranSekolah);
						paketPunyaMatapelajaran.setPaket(selectedPaket);

						session.save(paketPunyaMatapelajaran);

					}

					onSearchDefault(arg0);

				}

			}
		});

		window.onModal();
	}

	private void init(PaketPunyaMatapelajaran pilihanPaketPerJurusan) {
		this.paketPunyaMatapelajaran = pilihanPaketPerJurusan;
		addWindow.setTitle(pilihanPaketPerJurusan.getId() == null ? "Tambah Matapelajaran" : "Ubah Matapelajaran");
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

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(Common.getBahasaConfig("Jurusan") + " Sekolah"));
		Common.selectComboItem(matapelajaranSekolah, pilihanPaketPerJurusan.getMatapelajaranSekolah() == null ? null
				: pilihanPaketPerJurusan.getMatapelajaranSekolah());
		row.appendChild(matapelajaranSekolah);
		matapelajaranSekolah.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Paket"));
		Common.selectComboItem(paket,
				pilihanPaketPerJurusan.getPaket() == null ? null : pilihanPaketPerJurusan.getPaket());
		row.appendChild(paket);
		paket.setWidth("90%");

		if (selectedPaket != null) {
			Common.selectComboItem(paket, selectedPaket);
			paket.setDisabled(true);
		}

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

		Session session = HibernateUtil.currentSession();
		if (paketPunyaMatapelajaran.getId() != null) {
			paketPunyaMatapelajaran = (PaketPunyaMatapelajaran) session.load(PaketPunyaMatapelajaran.class,
					paketPunyaMatapelajaran.getId());

		}

		paketPunyaMatapelajaran
				.setMatapelajaranSekolah((MatapelajaranSekolah) matapelajaranSekolah.getSelectedItem().getValue());

		paketPunyaMatapelajaran.setPaket((Paket) paket.getSelectedItem().getValue());

		Common.refreshSaveOrUpdate(session, paketPunyaMatapelajaran);
		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(PaketPunyaMatapelajaran.class)

				.createAlias("matapelajaranSekolah", "matapelajaranSekolah")
				.add(Restrictions.eq("matapelajaranSekolah.aktif", true))

				.add(searchpaket.getSelectedItem() == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("paket", searchpaket.getSelectedItem().getValue()))

				.add(searchjurusansekolah.getSelectedItem() == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("matapelajaranSekolah", searchjurusansekolah.getSelectedItem().getValue()));

		if (order)
			criteria.addOrder(Order.asc("matapelajaranSekolah.nama"));

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Common.initPaging(initCriteria(false), paging);

		List<PaketPunyaMatapelajaran> paketPunyaMatapelajaran = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(paketPunyaMatapelajaran);
		grid.setRowRenderer(new PilihanPaketRenderer());
		grid.setModelCheckMobile(strset);

	}

}
