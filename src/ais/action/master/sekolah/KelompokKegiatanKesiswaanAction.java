package ais.action.master.sekolah;

import java.util.List;

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
import org.zkoss.zul.Combobox;
import ais.ui.util.MyDetail;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Hbox;
import ais.ui.util.MyInclude;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.action.master.sekolah.helper.DetailKelompokKegiatanKesiswaanHelper;
import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.sekolah.JenisKelompokKegiatanKesiswaan;
import ais.database.model.sekolah.KelompokKegiatanKesiswaan;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class KelompokKegiatanKesiswaanAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Textbox nama;
	private Textbox keterangan;
	private Combobox jenisKelompokKegiatanKesiswaan;

	// private boolean edit = false;
	// private boolean delete = false;

	private KelompokKegiatanKesiswaan kelompokKegiatanKesiswaan;
	private MyToolbarbuttonConfig add;

	private Tabpanel jenisKelompokKegiatanKesiswaanTab;

	public void onKelompokAspek(Event event) {
		if (jenisKelompokKegiatanKesiswaanTab.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(jenisKelompokKegiatanKesiswaanTab);
			MyInclude iframe = new MyInclude("/pages/master/sekolah/jenis_kelompok_kegiatan_kesiswaan.zul");
			iframe.setParent(window);
		}
	}

	private Tabpanel jabatanKegiatanKesiswaan;

	public void onJabatanKegiatanKesiswaan(Event event) {
		if (jabatanKegiatanKesiswaan.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(jabatanKegiatanKesiswaan);
			MyInclude iframe = new MyInclude("/pages/master/sekolah/jabatan_kegiatan_kesiswaan.zul");
			iframe.setParent(window);
		}
	}

	private Tabpanel skalaKegiatanKesiswaanTab;

	public void onSkalaKegiatanKesiswaan(Event event) {
		if (skalaKegiatanKesiswaanTab.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(skalaKegiatanKesiswaanTab);
			MyInclude iframe = new MyInclude("/pages/master/sekolah/skala_kegiatan_kesiswaan.zul");
			iframe.setParent(window);
		}
	}

	private Tabpanel nilaiKegiatanKesiswaanTab;

	public void onNilaiKegiatanKesiswaan(Event event) {
		if (nilaiKegiatanKesiswaanTab.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(nilaiKegiatanKesiswaanTab);
			MyInclude iframe = new MyInclude("/pages/master/sekolah/nilai_kegiatan_kesiswaan.zul");
			iframe.setParent(window);
		}
	}

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

		// add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		// add.setTooltiptext("Tambah");

		// edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		// delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		String[] contents = new String[] { "id", "nama", "jenisKelompokKegiatanKesiswaan", "nomorUrut", "bobot",
				"nilaiMinimal", "bisaDipilihSiswa", "aktif", "keterangan" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, KelompokKegiatanKesiswaan.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible())); }
		Common.appendKeToolbar(upload, add, comp);
	}

	class KelompokKegiatanKesiswaanRenderer extends ais.ui.util.MyRowRenderer {

		private DetailKelompokKegiatanKesiswaanHelper detailKelompokKegiatanKesiswaanHelper = new DetailKelompokKegiatanKesiswaanHelper();

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {arg0.setValign("top");
			// TODO Auto-generated method stub
			final KelompokKegiatanKesiswaan kelompokKegiatanKesiswaan = (KelompokKegiatanKesiswaan) arg1;

			final MyDetail detail = new MyDetail();
			detail.setParent(arg0);
			detail.addEventListener("onOpen", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					// detailJenisKegiatanHelper.displayJenisKegiatanDetail(
					// jenisKegiatan, detail, addWindow);
					Common.clear(detail);
					if (detail.isOpen())
						detailKelompokKegiatanKesiswaanHelper
								.displayDetailKelompokKegiatanKesiswaan(kelompokKegiatanKesiswaan, detail);
				}
			});

			RevisiHelper.createNewRevisi(KelompokKegiatanKesiswaan.class, kelompokKegiatanKesiswaan,
					kelompokKegiatanKesiswaan.getNama()).setParent(arg0);
			new Label(kelompokKegiatanKesiswaan.getJenisKelompokKegiatanKesiswaan() == null ? ""
					: kelompokKegiatanKesiswaan.getJenisKelompokKegiatanKesiswaan().getNama()).setParent(arg0);

			final Intbox nomorUrut = new Intbox(kelompokKegiatanKesiswaan.getNomorUrut());
			nomorUrut.setParent(arg0);
			nomorUrut.setWidth("90%");

			nomorUrut.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					kelompokKegiatanKesiswaan.setNomorUrut(nomorUrut.getValue());
					Common.refreshUpdate(kelompokKegiatanKesiswaan);
				}
			});

			final MyDoublebox bobot = new MyDoublebox(kelompokKegiatanKesiswaan.getBobot());
			bobot.setParent(arg0);
			bobot.setWidth("90%");

			bobot.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					kelompokKegiatanKesiswaan.setBobot(bobot.getValue());
					Common.refreshUpdate(kelompokKegiatanKesiswaan);
				}
			});

			final MyDoublebox nilaiMinimal = new MyDoublebox(kelompokKegiatanKesiswaan.getNilaiMinimal());
			nilaiMinimal.setParent(arg0);
			nilaiMinimal.setWidth("90%");

			nilaiMinimal.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					kelompokKegiatanKesiswaan.setNilaiMinimal(nilaiMinimal.getValue());
					Common.refreshUpdate(kelompokKegiatanKesiswaan);
				}
			});

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setChecked(kelompokKegiatanKesiswaan.getAktif());
			checkbox.setParent(arg0);arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					kelompokKegiatanKesiswaan.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(kelompokKegiatanKesiswaan);
				}
			});

			final MyCheckboxConfig bisaDipilihSiswa = new MyCheckboxConfig("Bisa Dipilih Siswa");
			bisaDipilihSiswa.setChecked(kelompokKegiatanKesiswaan.getBisaDipilihSiswa());
			bisaDipilihSiswa.setParent(arg0);
			bisaDipilihSiswa.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					kelompokKegiatanKesiswaan.setBisaDipilihSiswa(bisaDipilihSiswa.isChecked());
					Common.refreshSaveOrUpdate(kelompokKegiatanKesiswaan);
				}
			});

			new Label(kelompokKegiatanKesiswaan.getKeterangan()).setParent(arg0);

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			// button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(kelompokKegiatanKesiswaan);
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			button.setParent(toolbar);

			button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
			// button.setVisible(delete);
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
											Common.refreshDelete(kelompokKegiatanKesiswaan);
											onSearchDefault(event);
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											MyMessageboxConfig
													.show("Data ini tidak dapat dihapus .., karena berelasi dengan data lainnya, error-nya adalah sbagai berikut:"
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

	public void onAdd(Event event) throws Exception {
		init(new KelompokKegiatanKesiswaan());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(KelompokKegiatanKesiswaan kelompokKegiatanKesiswaan) {
		this.kelompokKegiatanKesiswaan = kelompokKegiatanKesiswaan;
		addWindow.setTitle(kelompokKegiatanKesiswaan.getId() == null ? "Tambah Aspek Kegiatan Kesiswaan" : "Ubah Aspek Kegiatan Kesiswaan");
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
		column.setWidth("40%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Aspek Kegiatan Kesiswaan"));
		row.appendChild(nama = new Textbox(kelompokKegiatanKesiswaan.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kelompok Kegiatan Kesiswaan"));
		row.appendChild(jenisKelompokKegiatanKesiswaan = new Combobox());
		jenisKelompokKegiatanKesiswaan.setWidth("90%");
		Common.insertCombo(jenisKelompokKegiatanKesiswaan, "nama", "keterangan",
				JenisKelompokKegiatanKesiswaan.class);
		Common.selectComboItem(jenisKelompokKegiatanKesiswaan,
				kelompokKegiatanKesiswaan.getJenisKelompokKegiatanKesiswaan());
		jenisKelompokKegiatanKesiswaan.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(kelompokKegiatanKesiswaan.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

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
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Nama Aspek Kegiatan Kesiswaan harus diisi", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (jenisKelompokKegiatanKesiswaan.getSelectedItem() == null) {
			MyMessageboxConfig.show("Kelompok Aspek Kesiswaan harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		boolean i = checkNamaKelompokKegiatanKesiswaan();
		if (i) {
			MyMessageboxConfig.show("Nama Aspek Kegiatan Kesiswaan sudah ada di database", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (kelompokKegiatanKesiswaan.getId() != null) {
			kelompokKegiatanKesiswaan = (KelompokKegiatanKesiswaan) session
					.load(KelompokKegiatanKesiswaan.class, kelompokKegiatanKesiswaan.getId());

		}

		kelompokKegiatanKesiswaan.setNama(nama.getValue());
		kelompokKegiatanKesiswaan.setKeterangan(keterangan.getValue());
		kelompokKegiatanKesiswaan.setJenisKelompokKegiatanKesiswaan(
				(JenisKelompokKegiatanKesiswaan) jenisKelompokKegiatanKesiswaan.getSelectedItem().getValue());

		Common.refreshSaveOrUpdate(session, kelompokKegiatanKesiswaan);

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(KelompokKegiatanKesiswaan.class);

		if (order)
			criteria.addOrder(Order.asc("nomorUrut")).addOrder(Order.asc("nama"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<KelompokKegiatanKesiswaan> kelompokKegiatanKesiswaan = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(kelompokKegiatanKesiswaan);
		grid.setRowRenderer(new KelompokKegiatanKesiswaanRenderer());
		grid.setModelCheckMobile(strset);

	}

	public Boolean checkNamaKelompokKegiatanKesiswaan() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(KelompokKegiatanKesiswaan.class)
				.setProjection(Projections.rowCount()).add(Restrictions.eq("nama", nama.getValue().trim()))
				.add(this.kelompokKegiatanKesiswaan.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.kelompokKegiatanKesiswaan.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

}
