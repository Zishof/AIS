package ais.action.master;

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
import ais.ui.util.MyColumnConfig;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import ais.ui.util.MyMessageboxConfig;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

import ais.action.master.helper.AmbilDataMahasiswaBanbox;
import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.dao.DaoFactory;
import ais.database.dao.PengajuanBeasiswaDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Beasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.PengajuanBeasiswa;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDoublebox;

public class PengajuanBeasiswaAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;

	private MyDatebox tanggalPengajuan;
	private AmbilDataMahasiswaBanbox mahasiswa;
	private Combobox beasiswa;

	private Textbox namaBapak;
	private Textbox namaIbu;

	private Textbox pekerjaanBapak;
	private Textbox pekerjaanIbu;

	private Textbox kelurahan;
	private Textbox kodePos;
	private Textbox kecamatan;
	private Textbox kabupaten;
	private Textbox provinsi;

	private MyDoublebox jarakKotaKecamatan;
	private MyDoublebox jarakKampus;

	private Combobox penghasilan;
	private Combobox rumahTinggal;
	private Combobox peneranganRumah;
	private Combobox sumberAirBersih;
	private Textbox penjelasanAlasanBeasiswa;

	private boolean edit = false;
	private boolean delete = false;

	private PengajuanBeasiswa pengajuanBeasiswa;
	private MyToolbarbuttonConfig add;
	private Textbox alatTransportasi;
	private MyDoublebox luasBangunanRumah;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(
			org.zkoss.zk.ui.Page page, org.zkoss.zk.ui.Component parent,
			org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		Common.initLaguage();
		if (session.getAttribute("usersTemp") == null
				|| !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		add.setVisible(CommonPrivilages
				.checkPrevilages(CommonPrivilages.CREATE));
		if (add != null) { add.setTooltiptext("Tambah"); }

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

	class PengajuanBeasiswaRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {arg0.setValign("top");
			// TODO Auto-generated method stub
			final PengajuanBeasiswa pengajuanBeasiswa = (PengajuanBeasiswa) arg1;

			new Label(pengajuanBeasiswa.getBeasiswa().toString())
					.setParent(arg0);
			RevisiHelper.createNewRevisi(PengajuanBeasiswa.class,
					pengajuanBeasiswa,
					pengajuanBeasiswa.getMahasiswa().toString())
					.setParent(arg0);
			new Label(pengajuanBeasiswa.getPenjelasanAlasanBeasiswa())
					.setParent(arg0);
			new Label(
					pengajuanBeasiswa.getMahasiswaDapatBeasiswa() == null ? "Belum mensetujui"
							: "Sudah disetujui").setParent(arg0);

			Hbox toolbar = new Hbox();
			toolbar.setVisible(pengajuanBeasiswa.getBeasiswa()
					.getDibukaUtkMahasiswa().equals(1)
					&& pengajuanBeasiswa.getBeasiswa().getMasihBuka());
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(pengajuanBeasiswa);
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
					MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?",
							"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
							MyMessageboxConfig.QUESTION, new EventListener() {

								@Override
								public void onEvent(Event event)
										throws Exception {
									int i = new Integer(event.getData()
											.toString());
									if (i == MyMessageboxConfig.OK) {
										try {

											Common.refreshDelete(pengajuanBeasiswa);

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
		init(new PengajuanBeasiswa());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(PengajuanBeasiswa pengajuanBeasiswa) {
		this.pengajuanBeasiswa = pengajuanBeasiswa;
		addWindow.setTitle(pengajuanBeasiswa.getId() == null ? "Tambah Pengajuan Beasiswa" : "Ubah Pengajuan Beasiswa");
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

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Pengajuan"));
		row.appendChild(tanggalPengajuan = new MyDatebox(pengajuanBeasiswa
				.getTanggalPengajuan()));
		tanggalPengajuan.setWidth("90%");
		tanggalPengajuan.setDisabled(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Mahasiswa"));
		row.appendChild(mahasiswa = new AmbilDataMahasiswaBanbox());
		mahasiswa.setAttribute("mahasiswa", pengajuanBeasiswa.getMahasiswa());
		mahasiswa.setValue(pengajuanBeasiswa.getMahasiswa() == null ? ""
				: pengajuanBeasiswa.getMahasiswa().getNama());
		mahasiswa.setWidth("90%");

		if (Common.getCurrentUser().getMahasiswa() != null) {
			pengajuanBeasiswa.setMahasiswa(Common.getCurrentUser()
					.getMahasiswa());
			mahasiswa.setAttribute("mahasiswa",
					pengajuanBeasiswa.getMahasiswa());
			mahasiswa.setValue(pengajuanBeasiswa.getMahasiswa() == null ? ""
					: pengajuanBeasiswa.getMahasiswa().getNama());
			mahasiswa.setDisabled(true);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Beasiswa"));
		row.appendChild(beasiswa = new Combobox());
		Common.insertCombo(beasiswa, new String[] { "nama", "instansi",
				"tanggalBuka", "tanggalTutup" }, "keterangan", Beasiswa.class,
				Restrictions.eq("dibukaUtkMahasiswa", 1));
		Common.selectComboItem(beasiswa, pengajuanBeasiswa.getBeasiswa());
		beasiswa.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Bapak"));
		row.appendChild(namaBapak = new Textbox(pengajuanBeasiswa
				.getNamaBapak()));
		namaBapak.setWidth("90%");
//		namaBapak.setConstraint("no empty");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Ibu"));
		row.appendChild(namaIbu = new Textbox(pengajuanBeasiswa.getNamaIbu()));
		namaIbu.setWidth("90%");
//		namaIbu.setConstraint("no empty");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pekerjaan Ibu"));
		row.appendChild(pekerjaanIbu = new Textbox(pengajuanBeasiswa
				.getPekerjaanIbu()));
		pekerjaanIbu.setWidth("90%");
//		pekerjaanIbu.setConstraint("no empty");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pekerjaan Bapak"));
		row.appendChild(pekerjaanBapak = new Textbox(pengajuanBeasiswa
				.getPekerjaanBapak()));
		pekerjaanBapak.setWidth("90%");
//		pekerjaanBapak.setConstraint("no empty");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kelurahan"));
		row.appendChild(kelurahan = new Textbox(pengajuanBeasiswa
				.getKelurahan()));
		kelurahan.setWidth("90%");
//		kelurahan.setConstraint("no empty");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Pos"));
		row.appendChild(kodePos = new Textbox(pengajuanBeasiswa.getKodePos()));
		kodePos.setWidth("90%");
//		kodePos.setConstraint("no empty");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kecamatan"));
		row.appendChild(kecamatan = new Textbox(pengajuanBeasiswa
				.getKecamatan()));
		kecamatan.setWidth("90%");
//		kecamatan.setConstraint("no empty");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kabupaten"));
		row.appendChild(kabupaten = new Textbox(pengajuanBeasiswa
				.getKabupaten()));
		kabupaten.setWidth("90%");
//		kabupaten.setConstraint("no empty");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Provinsi"));
		row.appendChild(provinsi = new Textbox(pengajuanBeasiswa.getProvinsi()));
		provinsi.setWidth("90%");
//		provinsi.setConstraint("no empty");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jarak Kota Kecamatan"));
		row.appendChild(jarakKotaKecamatan = new MyDoublebox(pengajuanBeasiswa
				.getJarakKotaKecamatan()));
		jarakKotaKecamatan.setWidth("90%");
//		jarakKotaKecamatan.setConstraint("no empty");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jarak Kampus"));
		row.appendChild(jarakKampus = new MyDoublebox(pengajuanBeasiswa
				.getJarakKampus()));
		jarakKampus.setWidth("90%");
//		jarakKampus.setConstraint("no empty");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Alat Transportasi yang Digunakan"));
		row.appendChild(alatTransportasi = new Textbox(pengajuanBeasiswa
				.getAlatTransportasi()));
		alatTransportasi.setWidth("90%");
//		alatTransportasi.setConstraint("no empty");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Penghasilan"));
		row.appendChild(penghasilan = new Combobox());
		penghasilan.setWidth("90%");

		String[] penghasilans = new String[] { "< Rp. 250.000",
				"Rp. 250.000 s.d Rp. 500.000", "Rp. 500.000 s.d Rp. 1.000.000",
				"Rp. 1.000.000 s.d Rp. 2.500.000",
				"Rp. 2.500.000 s.d Rp. 5.000.000" };
		for (String p : penghasilans) {
			MyComboitemConfig comboitem = new MyComboitemConfig(p);
			comboitem.setValue(p);
			penghasilan.appendChild(comboitem);
		}
		Common.selectComboItem(penghasilan, pengajuanBeasiswa.getPenghasilan());

//		penghasilan.setConstraint("no empty");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Rumah Tinggal"));
		row.appendChild(rumahTinggal = new Combobox());
		rumahTinggal.setWidth("90%");

		String[] rumahTinggals = new String[] { "Milik Sendiri", "Menyewa" };
		for (String p : rumahTinggals) {
			MyComboitemConfig comboitem = new MyComboitemConfig(p);
			comboitem.setValue(p);
			rumahTinggal.appendChild(comboitem);
		}
		Common.selectComboItem(rumahTinggal,
				pengajuanBeasiswa.getRumahTinggal());

//		rumahTinggal.setConstraint("no empty");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Luas bangunan rumah adalah"));
		row.appendChild(luasBangunanRumah = new MyDoublebox(pengajuanBeasiswa
				.getLuasBangunanRumah()));
		luasBangunanRumah.setWidth("90%");

//		luasBangunanRumah.setConstraint("no empty");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Penerangan Rumah"));
		row.appendChild(peneranganRumah = new Combobox());
		peneranganRumah.setWidth("90%");

		String[] peneranganRumahs = new String[] { "Listrik dari PLN",
				"Listrik swadaya masyarakat", "Lampu minyak tanah" };
		for (String p : peneranganRumahs) {
			MyComboitemConfig comboitem = new MyComboitemConfig(p);
			comboitem.setValue(p);
			peneranganRumah.appendChild(comboitem);
		}
		Common.selectComboItem(peneranganRumah,
				pengajuanBeasiswa.getPeneranganRumah());

//		peneranganRumah.setConstraint("no empty");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sumber Air Bersih"));
		row.appendChild(sumberAirBersih = new Combobox());
		sumberAirBersih.setWidth("90%");

		String[] sumberAirBersihs = new String[] { "PAM", "Sumur",
				"Sumber lainnya" };
		for (String p : sumberAirBersihs) {
			MyComboitemConfig comboitem = new MyComboitemConfig(p);
			comboitem.setValue(p);
			sumberAirBersih.appendChild(comboitem);
		}
		Common.selectComboItem(sumberAirBersih,
				pengajuanBeasiswa.getSumberAirBersih());

//		sumberAirBersih.setConstraint("no empty");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Alasan mengajukan mendapatkan beasiswa"));
		row.appendChild(penjelasanAlasanBeasiswa = new Textbox(
				pengajuanBeasiswa.getPenjelasanAlasanBeasiswa()));
		penjelasanAlasanBeasiswa.setWidth("90%");
		penjelasanAlasanBeasiswa.setRows(4);

//		penjelasanAlasanBeasiswa.setConstraint("no empty");

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
		if (mahasiswa.getAttribute("mahasiswa") == null) {
			MyMessageboxConfig.show("Mahasiswa harus dipilih", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (beasiswa.getSelectedItem() == null) {
			MyMessageboxConfig.show("Beasiswa harus dipilih", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}

		PengajuanBeasiswaDao pengajuanBeasiswaDao = DaoFactory.getInstance()
				.getPengajuanBeasiswaDao();
		if (pengajuanBeasiswa.getId() != null) {
			pengajuanBeasiswa = pengajuanBeasiswaDao.load(pengajuanBeasiswa
					.getId());

		}

		pengajuanBeasiswa.setMahasiswa((Mahasiswa) mahasiswa
				.getAttribute("mahasiswa"));
		pengajuanBeasiswa.setBeasiswa((Beasiswa) beasiswa.getSelectedItem()
				.getValue());
		pengajuanBeasiswa.setAlatTransportasi(alatTransportasi.getValue());
		pengajuanBeasiswa.setJarakKampus(jarakKampus.getValue());
		pengajuanBeasiswa.setJarakKotaKecamatan(jarakKotaKecamatan.getValue());
		pengajuanBeasiswa.setKabupaten(kabupaten.getValue());
		pengajuanBeasiswa.setKecamatan(kecamatan.getValue());
		pengajuanBeasiswa.setKodePos(kodePos.getValue());
		pengajuanBeasiswa.setLuasBangunanRumah(luasBangunanRumah.getValue());
		pengajuanBeasiswa.setNamaBapak(namaBapak.getValue());
		pengajuanBeasiswa.setNamaIbu(namaIbu.getValue());
		pengajuanBeasiswa.setPeneranganRumah(peneranganRumah.getValue());
		pengajuanBeasiswa.setPenghasilan(penghasilan.getValue());
		pengajuanBeasiswa.setPenjelasanAlasanBeasiswa(penjelasanAlasanBeasiswa
				.getValue());
		pengajuanBeasiswa.setRumahTinggal(rumahTinggal.getValue());
		pengajuanBeasiswa.setSumberAirBersih(sumberAirBersih.getValue());
		pengajuanBeasiswa.setTanggalPengajuan(tanggalPengajuan.getValue());

		if (pengajuanBeasiswa.getId() != null) {
			pengajuanBeasiswaDao.update(pengajuanBeasiswa);
		} else {
			pengajuanBeasiswaDao.save(pengajuanBeasiswa);
		}

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(PengajuanBeasiswa.class);

		if (order)
			criteria.addOrder(Order.asc("nama"));
		criteria.add(Restrictions.ilike("nama", searchnama.getValue(),
				MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<PengajuanBeasiswa> pengajuanBeasiswa = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(
						Common.ROWS_COUNT_ON_PAGE
								* (paging == null ? 0 : paging.getActivePage()))
				.list();
		ListModel strset = new SimpleListModel(pengajuanBeasiswa);
		grid.setRowRenderer(new PengajuanBeasiswaRenderer());
		grid.setModelCheckMobile(strset);

		

	}

}
