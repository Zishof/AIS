package ais.action.master.library;

import java.util.List;

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
import ais.ui.util.MyGrid;
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

import ais.action.master.helper.RevisiHelper;
import ais.action.master.library.helper.AmbilDataAnggotaBanbox;
import ais.action.master.library.helper.AmbilDataPerpustakaanBanbox;
import ais.action.master.library.util.LibraryUtil;
import ais.action.report.format1.library.LaporanPesananItem;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.library.Anggota;
import ais.database.model.library.AnggotaYangDiblokir;
import ais.database.model.library.Perpustakaan;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class AnggotaYangDiblokirAction extends GenericAutowireComposer implements DataCriteria, DataSearchDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private AmbilDataPerpustakaanBanbox searchperpustakaan;
	private AmbilDataAnggotaBanbox searchanggota;

	private MyDatebox mulai;
	private MyDatebox sampai;
	private AmbilDataAnggotaBanbox anggota;
	private AmbilDataPerpustakaanBanbox perpustakaan;
	private Textbox keterangan;
	private MyCheckboxConfig tidakBisaLogin;
	private Textbox informasiKeMahasiswaTidakBisaLogin;

	private boolean edit = false;
	private boolean delete = false;

	private AnggotaYangDiblokir anggotaYangDiblokir;
	private MyToolbarbuttonConfig add;

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

		searchperpustakaan.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		searchanggota.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		String[] contents = new String[] { "id", "anggota", "anggota.kode", "anggota.nama", "perpustakaan", "mulai",
				"sampai", "tidakBisaLogin", "informasiKeMahasiswaTidakBisaLogin", "keterangan" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, AnggotaYangDiblokir.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);
	}

	public void onCetak(Event event) throws Exception {
		LaporanPesananItem laporan = new LaporanPesananItem();
		laporan.setTitle("Cetak Laporan");
		page.getFirstRoot().appendChild(laporan);
		laporan.setHeight("95%");
		laporan.setWidth("90%");
		laporan.setClosable(true);
		laporan.onModal();
	}

	class AnggotaYangDiblokirRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {arg0.setValign("top");
			// TODO Auto-generated method stub
			final AnggotaYangDiblokir anggotaYangDiblokir = (AnggotaYangDiblokir) arg1;

			LibraryUtil.gambarAnggota(anggotaYangDiblokir.getAnggota()).setParent(arg0);

			RevisiHelper.createNewRevisi(AnggotaYangDiblokir.class, anggotaYangDiblokir, anggotaYangDiblokir.getKode())
					.setParent(arg0);
			new Label(anggotaYangDiblokir.getMulai() == null ? ""
					: Common.dateFormat4.get().format(anggotaYangDiblokir.getMulai())).setParent(arg0);
			new Label(anggotaYangDiblokir.getSampai() == null ? "Selamanya"
					: Common.dateFormat4.get().format(anggotaYangDiblokir.getSampai())).setParent(arg0);

			new Label(anggotaYangDiblokir.getAnggota() == null ? "" : anggotaYangDiblokir.getAnggota().toString())
					.setParent(arg0);

			new Label(anggotaYangDiblokir.getPerpustakaan() == null ? "Semua"
					: anggotaYangDiblokir.getPerpustakaan().getNama()).setParent(arg0);

			new Label(anggotaYangDiblokir.getKeterangan()).setParent(arg0);

			new Label(anggotaYangDiblokir.getTidakBisaLogin() ? "Tidak" : "Ya").setParent(arg0);
			new Label(anggotaYangDiblokir.getInformasiKeMahasiswaTidakBisaLogin()).setParent(arg0);

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(anggotaYangDiblokir);
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

											Common.refreshDelete(anggotaYangDiblokir);

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
		init(new AnggotaYangDiblokir());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(AnggotaYangDiblokir anggotaYangDiblokir) throws Exception {
		this.anggotaYangDiblokir = anggotaYangDiblokir;
		addWindow.setTitle(anggotaYangDiblokir.getId() == null ? "Tambah Pesanan Anggota" : "Ubah Pesanan Anggota");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Mulai Blokir"));
		row.appendChild(mulai = new MyDatebox(anggotaYangDiblokir.getMulai()));
		mulai.setFormat(Common.dateFormat1.get().toPattern());
		mulai.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sampai Blokir (Kosongkan jika selamanya)"));
		row.appendChild(sampai = new MyDatebox(anggotaYangDiblokir.getSampai()));
		sampai.setFormat(Common.dateFormat1.get().toPattern());
		sampai.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Anggota"));
		row.appendChild(anggota = new AmbilDataAnggotaBanbox());
		anggota.setAttribute("anggota", anggotaYangDiblokir.getAnggota());
		anggota.setValue(anggotaYangDiblokir.getAnggota() == null ? "" : anggotaYangDiblokir.getAnggota().toString());
		anggota.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Perpustakaan (Kosongkan jika semua)"));
		row.appendChild(perpustakaan = new AmbilDataPerpustakaanBanbox());
		perpustakaan.setAttribute("perpustakaan", anggotaYangDiblokir.getPerpustakaan());
		perpustakaan.setValue(
				anggotaYangDiblokir.getPerpustakaan() == null ? "" : anggotaYangDiblokir.getPerpustakaan().getNama());
		perpustakaan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(
				anggotaYangDiblokir.getKeterangan() == null ? "" : anggotaYangDiblokir.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);
		keterangan.setRows(3);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Apakah tidak bisa login ?"));
		row.appendChild(tidakBisaLogin = new MyCheckboxConfig());
		tidakBisaLogin.setChecked(anggotaYangDiblokir.getTidakBisaLogin());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Informasi ke mahasiswa yang tidak bisa login"));
		row.appendChild(informasiKeMahasiswaTidakBisaLogin = new Textbox(
				anggotaYangDiblokir.getInformasiKeMahasiswaTidakBisaLogin()));
		informasiKeMahasiswaTidakBisaLogin.setWidth("90%");
		informasiKeMahasiswaTidakBisaLogin.setRows(3);

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
		if (mulai.getValue() == null) {
			MyMessageboxConfig.show("Mulai Pesanan harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (anggota.getAttribute("anggota") == null) {
			MyMessageboxConfig.show("Anggota harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (anggotaYangDiblokir.getId() != null) {
			anggotaYangDiblokir = (AnggotaYangDiblokir) session.load(AnggotaYangDiblokir.class,
					anggotaYangDiblokir.getId());
		}

		anggotaYangDiblokir.setPerpustakaan((Perpustakaan) perpustakaan.getAttribute("perpustakaan"));

		anggotaYangDiblokir.setAnggota((Anggota) anggota.getAttribute("anggota"));
		anggotaYangDiblokir.setMulai(mulai.getValue());
		anggotaYangDiblokir.setSampai(sampai.getValue());
		anggotaYangDiblokir.setKeterangan(keterangan.getValue());
		anggotaYangDiblokir.setInformasiKeMahasiswaTidakBisaLogin(informasiKeMahasiswaTidakBisaLogin.getValue());
		anggotaYangDiblokir.setTidakBisaLogin(tidakBisaLogin.isChecked());

		Common.refreshSaveOrUpdate(session, anggotaYangDiblokir);

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(AnggotaYangDiblokir.class);
		if (order)
			criteria.addOrder(Order.desc("id"));
		criteria.createAlias("anggota", "anggota")
				.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("anggota.kode", searchnama.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<AnggotaYangDiblokir> anggotaYangDiblokir = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(anggotaYangDiblokir);
		grid.setRowRenderer(new AnggotaYangDiblokirRenderer());
		grid.setModelCheckMobile(strset);

	}

}
