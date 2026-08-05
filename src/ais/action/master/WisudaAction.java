package ais.action.master;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Intbox;
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

import ais.action.master.dashboard.admin.DashboardWisudaMahasiswa;
import ais.action.master.helper.AktifitasWisudaHelper;
import ais.action.master.helper.DetailwisudaHelper;
import ais.action.master.helper.GenerateNoKursiDanNoRegistrasiWindow;
import ais.action.master.helper.QuotaWisudaUntukFakultasHelper;
import ais.action.report.format1.akademik.LaporanAlbumMahasiswaWisuda;
import ais.action.report.format1.akademik.LaporanAlbumProfileWisuda;
import ais.action.report.format1.akademik.LaporanMahasiswaWisuda;
import ais.action.report.format1.akademik.LaporanTranskipWisuda;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.database.dao.DaoFactory;
import ais.database.dao.WisudaDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Wisuda;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class WisudaAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1923649953931592079L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;
	private Textbox searchWisudaKe;
	private Textbox wisudaKe;
	private Intbox maksimalQuota;
	private MyDatebox tanggal;
	private Textbox moto;
	private Textbox keterangan;

	private Checkbox searchaktif;

	private Wisuda wisuda;
	private MyToolbarbuttonConfig add;
	private boolean edit;
	private boolean delete;

	private Tabpanel pesertaWisuda;
	private Tabpanel albumWisuda;
	private Tabpanel albumProfile;
	private Tabpanel generateNoKursi;
	private Tabpanel transkipWisuda;
	private MyCheckboxConfig hanyaGunakanKuotaPerguruanTinggi;

	protected Tabpanel statistik;

	public void onStatistik(Event event) {

		if (statistik.getChildren().size() == 0) {
			DashboardWisudaMahasiswa include = new DashboardWisudaMahasiswa();
			ais.ui.util.BaseDasbordPortal.mountWrapped(include, statistik,
				"Statistik Wisuda", "Gambaran sebaran wisudawan per prodi, IPK, dan masa studi rata-rata.");
		}
	}

	public void onGenerateNoKursi(Event event) {

		if (generateNoKursi.getChildren().size() == 0) {
			GenerateNoKursiDanNoRegistrasiWindow laporan = new GenerateNoKursiDanNoRegistrasiWindow();
			laporan.setTitle("");
			laporan.setHeight("100%");
			laporan.setWidth("100%");
			laporan.setParent(generateNoKursi);
		}
	}

	public void onPesertaWisuda(Event event) {

		if (pesertaWisuda.getChildren().size() == 0) {
			LaporanMahasiswaWisuda laporan = new LaporanMahasiswaWisuda();
			laporan.setHeight("100%");
			laporan.setWidth("100%");
			laporan.setParent(pesertaWisuda);
		}
	}

	public void onAlbumWisuda(Event event) {

		if (albumWisuda.getChildren().size() == 0) {
			LaporanAlbumMahasiswaWisuda laporan = new LaporanAlbumMahasiswaWisuda();
			laporan.setHeight("100%");
			laporan.setWidth("100%");
			laporan.setParent(albumWisuda);
		}
	}

	public void onAlbumProfile(Event event) {

		if (albumProfile.getChildren().size() == 0) {
			LaporanAlbumProfileWisuda laporan = new LaporanAlbumProfileWisuda();
			laporan.setHeight("100%");
			laporan.setWidth("100%");
			laporan.setParent(albumProfile);
		}
	}

	public void onTranskipWisuda(Event event) {

		if (transkipWisuda.getChildren().size() == 0) {
			LaporanTranskipWisuda laporan = new LaporanTranskipWisuda();
			laporan.setHeight("100%");
			laporan.setWidth("100%");
			laporan.setParent(transkipWisuda);
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
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

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
	}

	public void onAdd(Event event) throws Exception {
		init(new Wisuda());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(Wisuda wisuda) {
		this.wisuda = wisuda;
		addWindow.setTitle(wisuda.getId() == null ? "Tambah Wisuda" : "Ubah Wisuda");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Wisuda Ke"));
		row.appendChild(wisudaKe = new Textbox(wisuda.getWisudaKe() == null ? "" : wisuda.getWisudaKe().toString()));
		wisudaKe.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Maksimal Quota"));
		row.appendChild(maksimalQuota = new Intbox(wisuda.getMaksimalQuota()));
		maksimalQuota.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(
				hanyaGunakanKuotaPerguruanTinggi = new MyCheckboxConfig("Hanya menggunakan kuota Perguruan Tinggi"));
		hanyaGunakanKuotaPerguruanTinggi.setChecked(wisuda.getHanyaGunakanKuotaPerguruanTinggi());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal"));
		row.appendChild(tanggal = new MyDatebox(
				wisuda.getTanggal() == null ? ais.ui.util.WaktuUtil.getDate() : wisuda.getTanggal()));
		tanggal.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Moto"));
		row.appendChild(moto = new Textbox(wisuda.getMoto() == null ? "" : wisuda.getMoto()));
		moto.setWidth("90%");
		moto.setRows(5);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(wisuda.getKeterangan() == null ? "" : wisuda.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);
		keterangan.setRows(3);

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
		if (wisudaKe.getValue().trim().equals("")) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Nama",
					"Kolom Nama belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Nama.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		WisudaDao wisudaDao = DaoFactory.getInstance().getWisudaDao();
		if (wisuda.getId() != null) {
			wisuda = wisudaDao.load(wisuda.getId());
		}

		wisuda.setWisudaKe(Integer.parseInt(wisudaKe.getValue()));
		wisuda.setMaksimalQuota(maksimalQuota.getValue());
		wisuda.setHanyaGunakanKuotaPerguruanTinggi(hanyaGunakanKuotaPerguruanTinggi.isChecked());
		wisuda.setTanggal(tanggal.getValue());
		wisuda.setMoto(moto.getValue());
		wisuda.setKeterangan(keterangan.getValue());

		// wisudaDao.beginTransaction();
		if (wisuda.getId() != null) {
			wisudaDao.update(wisuda);
		} else {
			wisudaDao.save(wisuda);
		}
		// wisudaDao.commitTransaction();
		return true;
	}

	class WisudaRenderer extends ais.ui.util.MyRowRenderer {
		private DetailwisudaHelper detailwisudaHelper = new DetailwisudaHelper();
		private AktifitasWisudaHelper aktifitasWisudaHelper = new AktifitasWisudaHelper();

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Wisuda wisuda = (Wisuda) arg1;

			final MyDetail detail = new MyDetail();
			detail.setParent(arg0);
			detail.addEventListener("onOpen", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					Common.clear(detail);
					if (detail.isOpen()) {

						Tabbox tabbox = new Tabbox();
						tabbox.setParent(detail);
						tabbox.setHeight("100%");
						tabbox.setWidth("100%");

						Tabs tabs = new Tabs();
						tabs.setParent(tabbox);

						MyTabConfig tabSoal = new MyTabConfig("Peserta Wisuda");
						tabSoal.setParent(tabs);

						MyTabConfig tab1Pertemuan = new MyTabConfig();
						tab1Pertemuan.setParent(tabs);
						tab1Pertemuan.setLabel("Agenda Wisuda");

						Tabpanels tabpanels = new Tabpanels();
						tabpanels.setParent(tabbox);

						int tinggi = 14;

						Tabpanel tabpanelUtama = new ais.ui.util.MyTabpanel();
						tabpanelUtama.setStyle("min-height: 200px;");
						tabpanelUtama.setHeight((100 + (100 * tinggi)) + "px");
						tabpanelUtama.setParent(tabpanels);

						detailwisudaHelper.display(wisuda, tabpanelUtama, addWindow);

						final Tabpanel tabpanelAgenda = new ais.ui.util.MyTabpanel();
						tabpanelAgenda.setStyle("min-height: 10000px;");
						tabpanelAgenda.setParent(tabpanels);

						tab1Pertemuan.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								if (tabpanelAgenda.getChildren().isEmpty()) {
									ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
									groupbox.setStyle("min-height: 10000px;");
									aktifitasWisudaHelper.initDetail(wisuda, groupbox);
									tabpanelAgenda.appendChild(groupbox);
								}
							}
						});

					}
				}

			});

			new Label(wisuda.getWisudaKe().toString()).setParent(arg0);
			new Label(Common.numberFormat.get().format(wisuda.getMaksimalQuota())).setParent(arg0);
			new Label(Common.dateFormat2.get().format(wisuda.getTanggal())).setParent(arg0);
			new Label(wisuda.getMoto()).setParent(arg0);
			new Label(wisuda.getKeterangan()).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(wisuda.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					wisuda.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(wisuda);
				}
			});

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(wisuda);
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

											Common.refreshDelete(wisuda);

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

			if (!wisuda.getHanyaGunakanKuotaPerguruanTinggi()) {
				button = new MyToolbarbuttonConfig("", "/img/upload.gif");
				button.setTooltiptext("Tampilan data quota fakultas untuk wisuda");
				button.addEventListener("onClick", new EventListener() {

					QuotaWisudaUntukFakultasHelper quotaWisudaUntukFakultasHelper = new QuotaWisudaUntukFakultasHelper();

					@Override
					public void onEvent(Event event) throws Exception {
						quotaWisudaUntukFakultasHelper.tampil(wisuda, addWindow);
					}

				});
				button.setParent(toolbar);
			}

			toolbar.setParent(arg0);
		}

	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(Wisuda.class)
				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"));
		if (order)
			criteria.addOrder(Order.asc("wisudaKe"));
		criteria.add(searchWisudaKe.getValue() == null || searchWisudaKe.getValue().equals("")
				? Restrictions.sqlRestriction("1=1")
				: Restrictions.eq("wisudaKe", Integer.parseInt(searchWisudaKe.getValue())));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<Wisuda> wisuda = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(wisuda);
		grid.setRowRenderer(new WisudaRenderer());
		grid.setModelCheckMobile(strset);

	}
}
