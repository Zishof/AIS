package ais.action.master;

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

import ais.action.master.helper.DetailKelompokKegiatanKemahasiswaanHelper;
import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.JenisKelompokKegiatanKemahasiswaan;
import ais.database.model.KelompokKegiatanKemahasiswaan;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class KelompokKegiatanKemahasiswaanAction extends GenericAutowireComposer
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
	private Combobox jenisKelompokKegiatanKemahasiswaan;

	// private boolean edit = false;
	// private boolean delete = false;

	private KelompokKegiatanKemahasiswaan kelompokKegiatanKemahasiswaan;
	private MyToolbarbuttonConfig add;

	private Tabpanel jenisKelompokKegiatanKemahasiswaanTab;

	public void onKelompokAspek(Event event) {
		if (jenisKelompokKegiatanKemahasiswaanTab.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(jenisKelompokKegiatanKemahasiswaanTab);
			MyInclude iframe = new MyInclude("/pages/master/jenis_kelompok_kegiatan_kemahasiswaan.zul");
			iframe.setParent(window);
		}
	}

	private Tabpanel jabatanKegiatanKemahasiswaan;

	public void onJabatanKegiatanKemahasiswaan(Event event) {
		if (jabatanKegiatanKemahasiswaan.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(jabatanKegiatanKemahasiswaan);
			MyInclude iframe = new MyInclude("/pages/master/jabatan_kegiatan_kemahasiswaan.zul");
			iframe.setParent(window);
		}
	}

	private Tabpanel skalaKegiatanKemahasiswaanTab;

	public void onSkalaKegiatanKemahasiswaan(Event event) {
		if (skalaKegiatanKemahasiswaanTab.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(skalaKegiatanKemahasiswaanTab);
			MyInclude iframe = new MyInclude("/pages/master/skala_kegiatan_kemahasiswaan.zul");
			iframe.setParent(window);
		}
	}

	private Tabpanel nilaiKegiatanKemahasiswaanTab;

	public void onNilaiKegiatanKemahasiswaan(Event event) {
		if (nilaiKegiatanKemahasiswaanTab.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(nilaiKegiatanKemahasiswaanTab);
			MyInclude iframe = new MyInclude("/pages/master/nilai_kegiatan_kemahasiswaan.zul");
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

		String[] contents = new String[] { "id", "nama", "jenisKelompokKegiatanKemahasiswaan", "nomorUrut", "bobot",
				"nilaiMinimal", "bisaDipilihMahasiswa", "aktif", "keterangan" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, KelompokKegiatanKemahasiswaan.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible())); }
		Common.appendKeToolbar(upload, add, comp);
	}

	class KelompokKegiatanKemahasiswaanRenderer extends ais.ui.util.MyRowRenderer {

		private DetailKelompokKegiatanKemahasiswaanHelper detailKelompokKegiatanKemahasiswaanHelper = new DetailKelompokKegiatanKemahasiswaanHelper();

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {arg0.setValign("top");
			// TODO Auto-generated method stub
			final KelompokKegiatanKemahasiswaan kelompokKegiatanKemahasiswaan = (KelompokKegiatanKemahasiswaan) arg1;

			final MyDetail detail = new MyDetail();
			detail.setParent(arg0);
			detail.addEventListener("onOpen", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					// detailJenisKegiatanHelper.displayJenisKegiatanDetail(
					// jenisKegiatan, detail, addWindow);
					Common.clear(detail);
					if (detail.isOpen())
						detailKelompokKegiatanKemahasiswaanHelper
								.displayDetailKelompokKegiatanKemahasiswaan(kelompokKegiatanKemahasiswaan, detail);
				}
			});

			RevisiHelper.createNewRevisi(KelompokKegiatanKemahasiswaan.class, kelompokKegiatanKemahasiswaan,
					kelompokKegiatanKemahasiswaan.getNama()).setParent(arg0);
			new Label(kelompokKegiatanKemahasiswaan.getJenisKelompokKegiatanKemahasiswaan() == null ? ""
					: kelompokKegiatanKemahasiswaan.getJenisKelompokKegiatanKemahasiswaan().getNama()).setParent(arg0);

			final Intbox nomorUrut = new Intbox(kelompokKegiatanKemahasiswaan.getNomorUrut());
			nomorUrut.setParent(arg0);
			nomorUrut.setWidth("90%");

			nomorUrut.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					kelompokKegiatanKemahasiswaan.setNomorUrut(nomorUrut.getValue());
					Common.refreshUpdate(kelompokKegiatanKemahasiswaan);
				}
			});

			final MyDoublebox bobot = new MyDoublebox(kelompokKegiatanKemahasiswaan.getBobot());
			bobot.setParent(arg0);
			bobot.setWidth("90%");

			bobot.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					kelompokKegiatanKemahasiswaan.setBobot(bobot.getValue());
					Common.refreshUpdate(kelompokKegiatanKemahasiswaan);
				}
			});

			final MyDoublebox nilaiMinimal = new MyDoublebox(kelompokKegiatanKemahasiswaan.getNilaiMinimal());
			nilaiMinimal.setParent(arg0);
			nilaiMinimal.setWidth("90%");

			nilaiMinimal.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					kelompokKegiatanKemahasiswaan.setNilaiMinimal(nilaiMinimal.getValue());
					Common.refreshUpdate(kelompokKegiatanKemahasiswaan);
				}
			});

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setChecked(kelompokKegiatanKemahasiswaan.getAktif());
			checkbox.setParent(arg0);arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					kelompokKegiatanKemahasiswaan.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(kelompokKegiatanKemahasiswaan);
				}
			});

			final MyCheckboxConfig bisaDipilihMahasiswa = new MyCheckboxConfig("Bisa Dipilih Mahasiswa");
			bisaDipilihMahasiswa.setChecked(kelompokKegiatanKemahasiswaan.getBisaDipilihMahasiswa());
			bisaDipilihMahasiswa.setParent(arg0);
			bisaDipilihMahasiswa.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					kelompokKegiatanKemahasiswaan.setBisaDipilihMahasiswa(bisaDipilihMahasiswa.isChecked());
					Common.refreshSaveOrUpdate(kelompokKegiatanKemahasiswaan);
				}
			});

			new Label(kelompokKegiatanKemahasiswaan.getKeterangan()).setParent(arg0);

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			// button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(kelompokKegiatanKemahasiswaan);
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
											Common.refreshDelete(kelompokKegiatanKemahasiswaan);
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
			ais.ui.util.MenuAksiBaris.pasang(toolbar);
			toolbar.setParent(arg0);
		}

	}

	public void onAdd(Event event) throws Exception {
		init(new KelompokKegiatanKemahasiswaan());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(KelompokKegiatanKemahasiswaan kelompokKegiatanKemahasiswaan) {
		this.kelompokKegiatanKemahasiswaan = kelompokKegiatanKemahasiswaan;
		addWindow.setTitle(kelompokKegiatanKemahasiswaan.getId() == null ? "Tambah Aspek Kegiatan Kemahasiswaan" : "Ubah Aspek Kegiatan Kemahasiswaan");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Aspek Kegiatan Kemahasiswaan"));
		row.appendChild(nama = new Textbox(kelompokKegiatanKemahasiswaan.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kelompok Kegiatan Kemahasiswaan"));
		row.appendChild(jenisKelompokKegiatanKemahasiswaan = new Combobox());
		jenisKelompokKegiatanKemahasiswaan.setWidth("90%");
		Common.insertCombo(jenisKelompokKegiatanKemahasiswaan, "nama", "keterangan",
				JenisKelompokKegiatanKemahasiswaan.class);
		Common.selectComboItem(jenisKelompokKegiatanKemahasiswaan,
				kelompokKegiatanKemahasiswaan.getJenisKelompokKegiatanKemahasiswaan());
		jenisKelompokKegiatanKemahasiswaan.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(kelompokKegiatanKemahasiswaan.getKeterangan()));
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
			PesanFormalHelper.tampilkanGagal("penyimpanan data Aspek Kegiatan Kemahasiswaan",
					"Kolom Nama Aspek Kegiatan Kemahasiswaan belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Nama Aspek Kegiatan Kemahasiswaan.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (jenisKelompokKegiatanKemahasiswaan.getSelectedItem() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Kelompok Aspek Kemahasiswaan",
					"Kolom Kelompok Aspek Kemahasiswaan belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Kelompok Aspek Kemahasiswaan.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		boolean i = checkNamaKelompokKegiatanKemahasiswaan();
		if (i) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Aspek Kegiatan Kemahasiswaan",
					"Nama Aspek Kegiatan Kemahasiswaan sudah terdaftar sebelumnya di database, sehingga tidak dapat disimpan kembali untuk menghindari duplikasi data.",
					new String[] {
							"Gunakan nama aspek kegiatan kemahasiswaan yang berbeda dari data yang sudah ada.",
							"Periksa kembali daftar data yang sudah tersimpan apabila Bapak/Ibu ragu."
					});
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (kelompokKegiatanKemahasiswaan.getId() != null) {
			kelompokKegiatanKemahasiswaan = (KelompokKegiatanKemahasiswaan) session
					.load(KelompokKegiatanKemahasiswaan.class, kelompokKegiatanKemahasiswaan.getId());

		}

		kelompokKegiatanKemahasiswaan.setNama(nama.getValue());
		kelompokKegiatanKemahasiswaan.setKeterangan(keterangan.getValue());
		kelompokKegiatanKemahasiswaan.setJenisKelompokKegiatanKemahasiswaan(
				(JenisKelompokKegiatanKemahasiswaan) jenisKelompokKegiatanKemahasiswaan.getSelectedItem().getValue());

		Common.refreshSaveOrUpdate(session, kelompokKegiatanKemahasiswaan);

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(KelompokKegiatanKemahasiswaan.class);

		if (order)
			criteria.addOrder(Order.asc("nomorUrut")).addOrder(Order.asc("nama"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<KelompokKegiatanKemahasiswaan> kelompokKegiatanKemahasiswaan = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(kelompokKegiatanKemahasiswaan);
		grid.setRowRenderer(new KelompokKegiatanKemahasiswaanRenderer());
		grid.setModelCheckMobile(strset);

	}

	public Boolean checkNamaKelompokKegiatanKemahasiswaan() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(KelompokKegiatanKemahasiswaan.class)
				.setProjection(Projections.rowCount()).add(Restrictions.eq("nama", nama.getValue().trim()))
				.add(this.kelompokKegiatanKemahasiswaan.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.kelompokKegiatanKemahasiswaan.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

}
