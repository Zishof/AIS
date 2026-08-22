package ais.action.master.rab;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.rab.helper.AmbilDataKppnBanbox;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.helper.AmbilDataSatuanLokasiBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.action.master.sekolah.util.SekolahUtil;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.dao.DaoFactory;
import ais.database.dao.rab.SatuanKerjaDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.file.LampiranLain;
import ais.database.model.rab.Kppn;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.rab.SatuanLokasi;
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class SatuanKerjaAction extends GenericAutowireComposer implements DataCriteria, DataSearchDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Textbox searchkode;
	private AmbilDataSatuanKerjaBanbox searchparent;

	private Textbox kode;
	private Textbox nama;
	private AmbilDataKppnBanbox kppn;
	private AmbilDataSatuanLokasiBanbox satuanLokasi;
	private AmbilDataSatuanKerjaBanbox parent;
	private Textbox keterangan;
	private Textbox domain;
	private MyCheckboxConfig defaultItem;
	private Textbox alamat;
	private MyCheckboxConfig searchaktif;

	private boolean edit = false;
	private boolean delete = false;

	private SatuanKerja satuanKerja;
	private MyToolbarbuttonConfig add;
	private Yayasan yayasan = null;
	private SatuanKerjaTreeModel satuanKerjaTreeModel;
	protected LampiranLain kop;
	protected LampiranLain kopBawah;

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

		yayasan = SekolahUtil.getYayasan();

		searchparent.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});

		satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		String[] contents = new String[] { "id", "kode", "nama", "parent", "satuanLokasi", "kppn", "alamat" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, SatuanKerja.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});
	}

	class SatuanKerjaRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final SatuanKerja satuanKerja = (SatuanKerja) arg1;

			final MyDetail detail = new MyDetail();
			detail.setParent(arg0);
			detail.addEventListener("onOpen", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Common.clear(detail);
					if (detail.isOpen()) {

						Tabbox tabbox = new Tabbox();
						tabbox.setParent(detail);
						tabbox.setHeight("650px");
						tabbox.setWidth("100%");

						Tabs tabs = new Tabs();
						tabs.setParent(tabbox);

						MyTabConfig tabJawaban = new MyTabConfig("Pegawai");
						tabJawaban.setParent(tabs);

						Tabpanels tabpanels = new Tabpanels();
						tabpanels.setParent(tabbox);

						Tabpanel tabpanelUtama = new ais.ui.util.MyTabpanel();
						tabpanelUtama.setParent(tabpanels);

						session.setAttribute("satuanKerjaOnSession", satuanKerja);
						MyInclude iframe = new MyInclude(
								"/pages/master/pegawai.zul?satuan_kerja=" + satuanKerja.getId());
						iframe.setParent(tabpanelUtama);

					}
				}
			});

			RevisiHelper.createNewRevisi(SatuanKerja.class, satuanKerja, satuanKerja.getKode()).setParent(arg0);
			new Label(satuanKerja.getNama()).setParent(arg0);
			new Label(satuanKerja.getSatuanLokasi() == null ? "" : satuanKerja.getSatuanLokasi().toString())
					.setParent(arg0);
//			new Label(satuanKerja.getKppn() == null ? "" : satuanKerja.getKppn().toString()).setParent(arg0);
			new Label(satuanKerja.getParent() == null ? "" : satuanKerja.getParent().toString()).setParent(arg0);
			new Label(satuanKerja.getDefaultItem() != null && satuanKerja.getDefaultItem() ? "Aktif" : "Tidak")
					.setParent(arg0);
			new Label(satuanKerja.getAlamat()).setParent(arg0);
			new Label(satuanKerja.getKeterangan()).setParent(arg0);

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(satuanKerja);
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			button.setParent(toolbar);

			// Pemilih member satuan kerja -- memakai helper yang sama dgn POS
			// Desktop/Android dan halaman JSP, jadi aturan penugasannya tidak
			// mungkin berbeda antar kanal.
			button = new MyToolbarbuttonConfig("", "/img/svg/user-box-line.svg");
			button.setTooltiptext("Pilih Member");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					ais.action.master.rab.helper.SatuanKerjaMemberZkDialog.buka(satuanKerja);
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
											SatuanKerjaDao satuanKerjaDao = DaoFactory.getInstance()
													.getSatuanKerjaDao();
											// satuanKerjaDao.beginTransaction();
											satuanKerjaDao.delete(satuanKerjaDao.merge(satuanKerja));
											// satuanKerjaDao.commitTransaction();
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
		init(new SatuanKerja());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(SatuanKerja satuanKerja) throws Exception {
		this.satuanKerja = satuanKerja;
		addWindow.setTitle(satuanKerja.getId() == null ? "Tambah Satuan Kerja" : "Ubah Satuan Kerja");
		addWindow.setHeight("95%");
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
		column.setWidth("30%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode"));
		row.appendChild(kode = new Textbox(satuanKerja.getKode()));
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama"));
		row.appendChild(nama = new Textbox(satuanKerja.getNama() == null ? "" : satuanKerja.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Lokasi"));
		row.appendChild(satuanLokasi = new AmbilDataSatuanLokasiBanbox());
		satuanLokasi.setValue(satuanKerja.getSatuanLokasi() == null ? "" : satuanKerja.getSatuanLokasi().toString());
		satuanLokasi.setAttribute("satuanLokasi", satuanKerja.getSatuanLokasi());
		satuanLokasi.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(false);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("KPPN"));
		row.appendChild(kppn = new AmbilDataKppnBanbox());
		kppn.setValue(satuanKerja.getKppn() == null ? "" : satuanKerja.getKppn().toString());
		kppn.setAttribute("kppn", satuanKerja.getKppn());
		kppn.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Satuan Kerja Parent"));
		row.appendChild(parent = new AmbilDataSatuanKerjaBanbox(true));
		parent.setValue(satuanKerja.getParent() == null ? "" : satuanKerja.getParent().toString());
		parent.setAttribute("satuanKerja", satuanKerja.getParent());
		parent.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Aktif"));
		row.appendChild(defaultItem = new MyCheckboxConfig());
		defaultItem.setChecked(satuanKerja.getDefaultItem() != null && satuanKerja.getDefaultItem());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Alamat"));
		row.appendChild(alamat = new Textbox(satuanKerja.getAlamat()));
		alamat.setWidth("90%");
		alamat.setRows(3);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(
				keterangan = new Textbox(satuanKerja.getKeterangan() == null ? "" : satuanKerja.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Domain"));
		row.appendChild(domain = new Textbox(satuanKerja.getDomain() == null ? "" : satuanKerja.getDomain()));
		domain.setWidth("90%");
		ais.common.Common.initKeterangan(rows,
				"Jika diisi & host/URL mengandung domain ini, tampilan Satuan Kerja langsung terkunci ke "
						+ "unit ini beserta seluruh child-nya. Boleh lebih dari satu domain, dipisah koma.");

		kop = null;
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("KOP Atas (JPG) "));
		Hbox hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, satuanKerja.getId(), LampiranLain.KOP_SATKER, "KOP", false,
				new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						kop = (LampiranLain) arg0.getData();
					}
				});
		hbox.setParent(row);

		row = new MyFormRow();
		row.setParent(rows);
		kopBawah = null;
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("KOP Bawah (JPG) "));
		hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, satuanKerja.getId(), LampiranLain.KOP_BAWAH_SATKER, "KOP",
				false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						kopBawah = (LampiranLain) arg0.getData();
					}
				});
		hbox.setParent(row);

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
		if (kode.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Kode harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Nama harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		// boolean i = checkKodeSatuanKerja();
		// if (i) {
		// MyMessageboxConfig.show("Kode sudah ada di database", "Peringatan",
		// MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
		// return false;
		// }
		SatuanKerjaDao satuanKerjaDao = DaoFactory.getInstance().getSatuanKerjaDao();
		// if (defaultItem.isChecked()) {
		// satuanKerjaDao
		// .getCurrentSession()
		// .createSQLQuery(
		// "update rab.satuan_kerja set default_item = false;")
		// .executeUpdate();
		// }

		if (satuanKerja.getId() != null) {
			satuanKerja = satuanKerjaDao.load(satuanKerja.getId());
		}

		satuanKerja.setParent((SatuanKerja) parent.getAttribute("satuanKerja"));
		satuanKerja.setAlamat(alamat.getValue());
		satuanKerja.setDefaultItem(defaultItem.isChecked());
		satuanKerja.setKode(kode.getValue());
		satuanKerja.setNama(nama.getValue());
		satuanKerja.setKeterangan(keterangan.getValue());
		satuanKerja.setDomain(domain.getValue());
		satuanKerja.setKppn((Kppn) kppn.getAttribute("kppn"));
		satuanKerja.setSatuanLokasi((SatuanLokasi) satuanLokasi.getAttribute("satuanLokasi"));
		satuanKerja.setYayasan(yayasan);

		if (satuanKerja.getId() != null) {
			satuanKerjaDao.update(satuanKerja);
		} else {
			satuanKerjaDao.save(satuanKerja);
		}

		if (kop != null && kop.getId() != null) {
			try {
				Session session = StreamingHibernateUtil.getInstance().currentSession();

				session.refresh(kop);
				kop.setRef(satuanKerja.getId());

				session.getTransaction().begin();
				session.update(kop);
				session.getTransaction().commit();

				StreamingHibernateUtil.getInstance().closeSession();
			} catch (Exception e) {
				StreamingHibernateUtil.getInstance().rollbackTransaction();
				Common.tampilErrorJikaAdmin(e);
			}

		}

		if (kopBawah != null && kopBawah.getId() != null) {
			try {
				Session session = StreamingHibernateUtil.getInstance().currentSession();

				session.refresh(kopBawah);
				kopBawah.setRef(satuanKerja.getId());

				session.getTransaction().begin();
				session.update(kopBawah);
				session.getTransaction().commit();

				StreamingHibernateUtil.getInstance().closeSession();
			} catch (Exception e) {
				StreamingHibernateUtil.getInstance().rollbackTransaction();
				Common.tampilErrorJikaAdmin(e);
			}

		}

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		SatuanKerja parent = (SatuanKerja) searchparent.getAttribute("satuanKerja");
		Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
		if (parent != null) {
			satuanKerjas.clear();
			satuanKerjas.add(parent);
			satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
		}

		List<Long> ids = new ArrayList<Long>();
		for (SatuanKerja kerja : satuanKerjas) {
			ids.add(kerja.getId());
		}

		if (parent != null) {
			ids.add(parent.getId());
		}

		Criteria criteria = session.createCriteria(SatuanKerja.class)

				.add(yayasan == null || yayasan.getId() == null ? Restrictions.isNull("yayasan")
						: Restrictions.eq("yayasan", yayasan))
				.add(searchaktif == null || searchaktif.isChecked() ? Restrictions.eq("defaultItem", true)
						: Restrictions.sqlRestriction("true"));
		if (order)
			criteria.addOrder(Order.asc("nama"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))
				.add(searchkode.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("kode", searchkode.getValue().trim(), MatchMode.ANYWHERE))
				.add(ids.size() == 0 ? Restrictions.sqlRestriction("1=1") : Restrictions.in("id", ids));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<SatuanKerja> satuanKerja = ConstantValues
				.simpleList(
						initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE).setFirstResult(
								Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())),
						SatuanKerja.class);
		ListModel strset = new SimpleListModel(satuanKerja);
		grid.setRowRenderer(new SatuanKerjaRenderer());
		grid.setModelCheckMobile(strset);

	}

	public Boolean checkKodeSatuanKerja() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(SatuanKerja.class).setProjection(Projections.rowCount())
				.add(Restrictions.eq("kode", kode.getValue().trim()))
				.add(this.satuanKerja.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.satuanKerja.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

}
