package ais.action.master.pmb;

import java.util.List;

import org.hibernate.Session;
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
import ais.ui.util.MyGrid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import ais.ui.util.MyMessageboxConfig;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Toolbar;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

import ais.action.master.helper.generic.AmbilDataJurusanSekolahMahasiswaBaruBanyak;
import ais.common.Common;
import ais.common.CommonSearchFilterHelper;
import ais.database.dao.DaoFactory;
import ais.database.dao.PilihanPaketPerJurusanDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.JurusanSekolahMahasiswaBaru;
import ais.database.model.Paket;
import ais.database.model.PilihanPaketPerJurusanMhsBaru;

public class PilihanPaketPerJurusanMahasiswaBaruAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7896939206824822505L;
	private PilihanPaketPerJurusanMhsBaru pilihanPaketPerJurusanMhsBaru;
	private MyWindow addWindow;
	private MyGrid grid;

	private Combobox jurusanSekolah;
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

		Common.insertCombo(jurusanSekolah = new Combobox(), new String[] { "nama", "jenisSekolahMahasiswaBaru" },
				"jenisSekolahMahasiswaBaru", JurusanSekolahMahasiswaBaru.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.insertCombo(searchjurusansekolah, new String[] { "nama", "jenisSekolahMahasiswaBaru" },
				"jenisSekolahMahasiswaBaru", JurusanSekolahMahasiswaBaru.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.insertCombo(paket = new Combobox(), "nama", "keterangan", Paket.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.insertCombo(searchpaket, "nama", "keterangan", Paket.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		if (execution.getParameter("paket") != null) {
			selectedPaket = (Paket) HibernateUtil.currentSession().createCriteria(Paket.class)
					.add(Restrictions.idEq(Long.parseLong(execution.getParameter("paket")))).uniqueResult();
			Common.selectComboItem(true, searchpaket, selectedPaket);
			searchpaket.setDisabled(true);
		}

		onSearchDefault(null);
	}

	class PilihanPaketRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final PilihanPaketPerJurusanMhsBaru pilihanPaketPerJurusan = (PilihanPaketPerJurusanMhsBaru) arg1;

			new Label(pilihanPaketPerJurusan.getPaket().getNama()).setParent(arg0);
			new Label(pilihanPaketPerJurusan.getJurusanSekolahMahasiswaBaru() == null ? ""
					: pilihanPaketPerJurusan.getJurusanSekolahMahasiswaBaru().getNama()).setParent(arg0);

			new Label(pilihanPaketPerJurusan.getJurusanSekolahMahasiswaBaru() == null
					|| pilihanPaketPerJurusan.getJurusanSekolahMahasiswaBaru().getJenisSekolahMahasiswaBaru() == null
							? ""
							: pilihanPaketPerJurusan.getJurusanSekolahMahasiswaBaru().getJenisSekolahMahasiswaBaru()
									.getNama())
					.setParent(arg0);

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(pilihanPaketPerJurusan);
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

											Common.refreshDelete(pilihanPaketPerJurusan);

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
		// init(new PilihanPaketPerJurusanMhsBaru());
		// addWindow.setVisible(true);
		// addWindow.onModal();

		List<JurusanSekolahMahasiswaBaru> jurusanSekolahMahasiswaBarus = HibernateUtil.currentSession()
				.createCriteria(PilihanPaketPerJurusanMhsBaru.class)
				.add(selectedPaket == null ? Restrictions.isNull("paket") : Restrictions.eq("paket", selectedPaket))
				.setProjection(Projections.groupProperty("jurusanSekolahMahasiswaBaru")).list();

		AmbilDataJurusanSekolahMahasiswaBaruBanyak window = new AmbilDataJurusanSekolahMahasiswaBaruBanyak(
				jurusanSekolahMahasiswaBarus);

		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);
		window.setWidth("870px");
		window.setHeight("90%");

		window.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				List<JurusanSekolahMahasiswaBaru> jurusanSekolahMahasiswaBarus = (List<JurusanSekolahMahasiswaBaru>) arg0
						.getData();

				if (jurusanSekolahMahasiswaBarus != null) {
					Session session = HibernateUtil.currentSession();
					for (JurusanSekolahMahasiswaBaru jurusanSekolahMahasiswaBaru : jurusanSekolahMahasiswaBarus) {

						PilihanPaketPerJurusanMhsBaru pilihanPaketPerJurusanMhsBaru = new PilihanPaketPerJurusanMhsBaru();
						pilihanPaketPerJurusanMhsBaru.setJurusanSekolahMahasiswaBaru(jurusanSekolahMahasiswaBaru);
						pilihanPaketPerJurusanMhsBaru.setPaket(selectedPaket);

						session.save(pilihanPaketPerJurusanMhsBaru);

					}

					onSearchDefault(arg0);

				}

			}
		});

		window.onModal();
	}

	private void init(PilihanPaketPerJurusanMhsBaru pilihanPaketPerJurusan) {
		this.pilihanPaketPerJurusanMhsBaru = pilihanPaketPerJurusan;
		addWindow.setTitle(pilihanPaketPerJurusan.getId() == null ? "Tambah Jurusan Sekolah Mahasiswa Baru" : "Ubah Jurusan Sekolah Mahasiswa Baru");
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
		Common.selectComboItem(jurusanSekolah, pilihanPaketPerJurusan.getJurusanSekolahMahasiswaBaru() == null ? null
				: pilihanPaketPerJurusan.getJurusanSekolahMahasiswaBaru());
		row.appendChild(jurusanSekolah);
		jurusanSekolah.setWidth("90%");

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

		PilihanPaketPerJurusanDao pilihanPaketPerJurusanDao = DaoFactory.getInstance().getPilihanPaketPerJurusanDao();
		if (pilihanPaketPerJurusanMhsBaru.getId() != null) {
			pilihanPaketPerJurusanMhsBaru = pilihanPaketPerJurusanDao.load(pilihanPaketPerJurusanMhsBaru.getId());

		}

		pilihanPaketPerJurusanMhsBaru.setJurusanSekolahMahasiswaBaru(
				(JurusanSekolahMahasiswaBaru) jurusanSekolah.getSelectedItem().getValue());

		pilihanPaketPerJurusanMhsBaru.setPaket((Paket) paket.getSelectedItem().getValue());

		// jurusanSekolahMahasiswaBaruDao.beginTransaction();
		if (pilihanPaketPerJurusanMhsBaru.getId() != null) {
			pilihanPaketPerJurusanDao.update(pilihanPaketPerJurusanMhsBaru);
		} else {
			pilihanPaketPerJurusanDao.save(pilihanPaketPerJurusanMhsBaru);
		}
		// jurusanSekolahMahasiswaBaruDao.commitTransaction();
		return true;
	}

	// jurusanSekolahMahasiswaBaru

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Session session = HibernateUtil.currentSession();
		List<PilihanPaketPerJurusanMhsBaru> pilihanPaketPerJurusanMhsBaru = session
				.createCriteria(PilihanPaketPerJurusanMhsBaru.class)

				.add(searchpaket.getSelectedItem() == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("paket", searchpaket.getSelectedItem().getValue()))

				.add(searchjurusansekolah.getSelectedItem() == null ? Restrictions.sqlRestriction("true")
						: CommonSearchFilterHelper.eqSelectedWithId("jurusanSekolahMahasiswaBaru", searchjurusansekolah, false))

				.setMaxResults(Common.MAX_RESULT).list();
		ListModel strset = new SimpleListModel(pilihanPaketPerJurusanMhsBaru);
		grid.setRowRenderer(new PilihanPaketRenderer());
		grid.setModelCheckMobile(strset);

	}

}
