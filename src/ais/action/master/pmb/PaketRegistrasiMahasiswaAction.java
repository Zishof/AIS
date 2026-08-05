package ais.action.master.pmb;

import java.util.List;

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
import ais.ui.util.MyDetail;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Toolbar;

import ais.action.master.helper.DetailPaketRegistrasiMahasiswaHelper;
import ais.action.master.helper.generic.AmbilDataJurusanSekolahMahasiswaBaruBanyak;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.dao.DaoFactory;
import ais.database.dao.PaketRegistrasiMahasiswaDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.JurusanSekolahMahasiswaBaru;
import ais.database.model.Paket;
import ais.database.model.PaketRegistrasiMahasiswa;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class PaketRegistrasiMahasiswaAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 379408237994454706L;
	private MyWindow addWindow;
	private MyGrid grid;

	private MyToolbarbuttonConfig add;
	private boolean edit;
	private boolean delete;

	private Combobox namaPaket;
	private Combobox jurusanSekolah;

	private PaketRegistrasiMahasiswa paketRegistrasiMahasiswa;

	private Paket paket = null;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		Common.initLaguage();
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		if (execution.getParameter("paket") != null) {
			paket = (Paket) HibernateUtil.currentSession().createCriteria(Paket.class)
					.add(Restrictions.idEq(Long.parseLong(execution.getParameter("paket")))).uniqueResult();
		}

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		onSearchDefault(null);
	}

	class PaketRegistrasiMahasiswaRenderer extends ais.ui.util.MyRowRenderer {
		private DetailPaketRegistrasiMahasiswaHelper detailPaketRegistrasiMahasiswaHelper = new DetailPaketRegistrasiMahasiswaHelper();

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {arg0.setValign("top");
			final PaketRegistrasiMahasiswa paketRegistrasiMahasiswa = (PaketRegistrasiMahasiswa) arg1;

			final MyDetail detail = new MyDetail();
			detail.setParent(arg0);
			detail.addEventListener("onOpen", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					Common.clear(detail);
					if (detail.isOpen())
						detailPaketRegistrasiMahasiswaHelper.displayDetailJurusan(paketRegistrasiMahasiswa, detail,
								addWindow);
				}
			});

			new Label(paketRegistrasiMahasiswa.getPaket().getNama()).setParent(arg0);
			new Label(paketRegistrasiMahasiswa.getJurusanSekolahMahasiswaBaru().getNama()).setParent(arg0);

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(paketRegistrasiMahasiswa);
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
					MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

						@Override
						public void onEvent(Event event) throws Exception {
							int i = Integer.parseInt(event.getData().toString());
							if (i == MyMessageboxConfig.OK) {
								try {
									PaketRegistrasiMahasiswaDao paketRegistrasiMahasiswaDao = DaoFactory.getInstance()
											.getPaketRegistrasiMahasiswaDao();
									// paketRegistrasiMahasiswaDao.beginTransaction();
									paketRegistrasiMahasiswaDao
											.delete(paketRegistrasiMahasiswaDao.merge(paketRegistrasiMahasiswa));
									// paketRegistrasiMahasiswaDao.commitTransaction();
									onSearchDefault(event);
								} catch (Exception e) {
									Common.tampilErrorJikaAdmin(e); 
									MyMessageboxConfig.show("Mohon maaf, data ini tidak dapat dihapus karena mungkin sudah digunakan di bagian lain sistem. Langkah yang dapat dilakukan: (1) pastikan tidak ada data registrasi mahasiswa yang terkait dengan paket ini; (2) hubungi Administrator untuk menghapus dependensi terkait terlebih dahulu; (3) ulangi proses penghapusan setelah dependensi dihapus. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.");
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
		// init(new PaketRegistrasiMahasiswa());
		// addWindow.setVisible(true);
		// addWindow.onModal();

		List<JurusanSekolahMahasiswaBaru> jurusanSekolahMahasiswaBarus = HibernateUtil.currentSession()
				.createCriteria(PaketRegistrasiMahasiswa.class)
				.add(paket == null ? Restrictions.isNull("paket") : Restrictions.eq("paket", paket))
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

						PaketRegistrasiMahasiswa paketRegistrasiMahasiswa = new PaketRegistrasiMahasiswa();
						paketRegistrasiMahasiswa.setJurusanSekolahMahasiswaBaru(jurusanSekolahMahasiswaBaru);
						paketRegistrasiMahasiswa.setPaket(paket);

						session.save(paketRegistrasiMahasiswa);

					}

					onSearchDefault(arg0);

				}

			}
		});

		window.onModal();
	}

	private void init(PaketRegistrasiMahasiswa paketRegistrasiMahasiswa) {
		this.paketRegistrasiMahasiswa = paketRegistrasiMahasiswa;
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

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Paket"));
		Common.insertCombo(namaPaket = new Combobox(), "nama", Paket.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.selectComboItem(namaPaket, paketRegistrasiMahasiswa.getPaket());
		row.appendChild(namaPaket);
		namaPaket.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(Common.getBahasaConfig("Jurusan") + " Sekolah"));

		Common.insertCombo(jurusanSekolah = new Combobox(), "nama", JurusanSekolahMahasiswaBaru.class, Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.selectComboItem(jurusanSekolah, paketRegistrasiMahasiswa.getJurusanSekolahMahasiswaBaru());
		row.appendChild(jurusanSekolah);
		jurusanSekolah.setWidth("90%");

		// row = new MyFormRow();
		//		// row.setParent(rows);
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
		if (namaPaket.getSelectedItem() == null) {
			MyMessageboxConfig.show("Mohon maaf, Nama Paket belum dipilih. Langkah yang dapat dilakukan: (1) pilih Nama Paket dari daftar dropdown yang tersedia; (2) pastikan data paket sudah terdaftar di sistem; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		PaketRegistrasiMahasiswaDao paketRegistrasiMahasiswaDao = DaoFactory.getInstance()
				.getPaketRegistrasiMahasiswaDao();
		if (paketRegistrasiMahasiswa.getId() != null) {
			paketRegistrasiMahasiswa = paketRegistrasiMahasiswaDao.load(paketRegistrasiMahasiswa.getId());
		}

		paketRegistrasiMahasiswa.setPaket((Paket) namaPaket.getSelectedItem().getValue());
		paketRegistrasiMahasiswa.setJurusanSekolahMahasiswaBaru(
				(JurusanSekolahMahasiswaBaru) jurusanSekolah.getSelectedItem().getValue());

		// paketRegistrasiMahasiswaDao.beginTransaction();
		if (paketRegistrasiMahasiswa.getId() != null) {
			paketRegistrasiMahasiswaDao.update(paketRegistrasiMahasiswa);
		} else {
			paketRegistrasiMahasiswaDao.save(paketRegistrasiMahasiswa);
		}
		// paketRegistrasiMahasiswaDao.commitTransaction();
		return true;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Session session = HibernateUtil.currentSession();
		List<PaketRegistrasiMahasiswa> paketRegistrasiMahasiswa = session.createCriteria(PaketRegistrasiMahasiswa.class)
				.add(paket == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("paket", paket))
				.addOrder(Order.asc("nama")).setMaxResults(Common.MAX_RESULT).list();

		ListModel strset = new SimpleListModel(paketRegistrasiMahasiswa);
		grid.setRowRenderer(new PaketRegistrasiMahasiswaRenderer());
		grid.setModelCheckMobile(strset);

	}

}
