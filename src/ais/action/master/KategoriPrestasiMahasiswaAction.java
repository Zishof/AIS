package ais.action.master;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
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

import ais.action.master.feeder.util.FeederConnector;
import ais.action.master.feeder.util.FeederExporter;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.KategoriPrestasiMahasiswa;
import ais.database.model.Konfigurasi;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Tbmuser;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class KategoriPrestasiMahasiswaAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;

	private Textbox kode;
	private Textbox nama;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private KategoriPrestasiMahasiswa kategoriPrestasiMahasiswa;
	private MyToolbarbuttonConfig add;
	private Textbox feeder;

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

		String[] contents = new String[] { "id", "kode", "nama", "keterangan", "feeder"  };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, KategoriPrestasiMahasiswa.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);
		
		
		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser != null && Common.getApakahAdminBolehAksesFeeder()
				&& Common.bolehKonfigurasi("aktifkan_terhubung_langsung_ke_feeder")) {
			final PerguruanTinggi perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();
			MyToolbarbuttonConfig buttonTagihan = new MyToolbarbuttonConfig("Syn. Feeder",
					"/img/Button-Refresh-icon.png");
			buttonTagihan.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					MyMessageboxConfig.show("Apakah yakin ingin singkronkan data ke feeder ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {

										String[] kon = EksporFromFeederAction.koneksi();
										final String ip = kon[0];
										final String port = kon[1];
										final String username = kon[2];
										final String password = kon[3];
										final String url = kon[4];

										if (!EksporFromFeederAction.exists(url)) {

											MyMessageboxConfig.show(
													ais.action.master.feeder.util.NeoFeederPesanFormalHelper.pesanGagalKoneksi(ip, port, Common.bolehKonfigurasi("aktifkan_https_ke_feeder", ais.database.model.Konfigurasi.TIDAK_AKTIF), "Pemeriksaan ketersediaan alamat " + url + " gagal (server Neo Feeder tidak merespons)."),
													"Peringatan", MyMessageboxConfig.OK,
													MyMessageboxConfig.EXCLAMATION);
											return;
										}

										final Label myLabelProsesDetail = Common.displayLoadBar(new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {
												if (arg0 != null && !arg0.getName().isEmpty()) {
													EksporFromFeederAction.display();
													MyMessageboxConfig.show(arg0.getName(), "Info",
															MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
												}
												onSearchDefault(null);
											}
										});

										new Thread(new Runnable() {

											@Override
											public void run() {
												try {
													FeederConnector feederConnector = new FeederConnector(ip,
															Integer.parseInt(port), null);

													String token = feederConnector.getToken(username, password);
													System.out.println("TOKEN => " + token);

													if (token == null || token.trim().isEmpty()
															|| token.trim().toLowerCase().startsWith("error")) {
														myLabelProsesDetail
																.setValue("Error: " + ais.action.master.feeder.util.NeoFeederPesanFormalHelper.pesanGagalLogin(username, null));
														return;
													}

													FeederExporter feederImporter = new FeederExporter(feederConnector,
															token, null, null, myLabelProsesDetail);

													exportKeFeeder(perguruanTinggi, feederImporter, token,
															feederConnector);
													// FIX "gagal diam-diam": penanda sukses (setValue("")) dipindah ke akhir try agar exception di bawah tidak dianggap sukses.
													myLabelProsesDetail.setValue("");
												} catch (Exception e) {
													ais.common.Common.tampilErrorJikaAdmin(e);
													myLabelProsesDetail.setValue("Error: " + ais.common.PesanFormalHelper.pesanGagalException(
															"sinkronisasi data Kategori Prestasi Mahasiswa ke Neo Feeder",
															null, e,
															new String[] {
																	"Periksa kembali koneksi ke server Neo Feeder (Pengaturan Koneksi) dan coba ulangi.",
																	"Pastikan Username/Password Feeder pada Pengaturan Koneksi masih benar.",
																	"Jika kendala berulang, hubungi Administrator Sistem atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini." })
															.replace("\n", " "));
												}
											}
										}).start();
									}

								}
							});

				}
			});
			Common.appendKeToolbar(buttonTagihan, add, comp);
		}
	}
	
	
	private void exportKeFeeder(PerguruanTinggi perguruanTinggi, FeederExporter feederImporter, String token,
			FeederConnector feederConnector) {
		try {

			JSONArray dataProdi = feederConnector.getData("GetTingkatPrestasi", token, "", "", "5000", "0");
			System.out.println("kategoriPrestasiMahasiswa size -> " + dataProdi.length());
			for (int index = 0; index < dataProdi.length(); index++) {
				JSONObject jsonObject = dataProdi.getJSONObject(index);
				System.out.println("jsonObject -> " + jsonObject);
				String id_tingkat_prestasi = jsonObject.getString("id_tingkat_prestasi");
				Session session = HibernateUtil.currentNativeSession();
				String nama_tingkat_prestasi = jsonObject.getString("nama_tingkat_prestasi");
				KategoriPrestasiMahasiswa existing = (KategoriPrestasiMahasiswa) session
						.createCriteria(KategoriPrestasiMahasiswa.class)
						.add(Restrictions.ilike("nama", nama_tingkat_prestasi, MatchMode.EXACT)).setMaxResults(1)
						.uniqueResult();
				if (existing == null) {
					existing = (KategoriPrestasiMahasiswa) session.createCriteria(KategoriPrestasiMahasiswa.class)
							.add(Restrictions.eq("feeder", id_tingkat_prestasi)).setMaxResults(1).uniqueResult();
				}
				System.out.println("existing -> " + existing);
				if (existing != null) {
					existing.setFeeder(id_tingkat_prestasi);
					session.getTransaction().begin();
					session.saveOrUpdate(existing);
					session.getTransaction().commit();
				} else {
					existing = new KategoriPrestasiMahasiswa();
					existing.setNama(nama_tingkat_prestasi);
					existing.setKode(id_tingkat_prestasi);
					existing.setFeeder(id_tingkat_prestasi);
					session.getTransaction().begin();
					session.save(existing);
					session.getTransaction().commit();
				}
				HibernateUtil.closeSession();

			}

		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
	}

	class KategoriPrestasiMahasiswaRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {arg0.setValign("top");
			// TODO Auto-generated method stub
			final KategoriPrestasiMahasiswa kategoriPrestasiMahasiswa = (KategoriPrestasiMahasiswa) arg1;
			new Label(kategoriPrestasiMahasiswa.getKode()).setParent(arg0);
			RevisiHelper.createNewRevisi(KategoriPrestasiMahasiswa.class, kategoriPrestasiMahasiswa,
					kategoriPrestasiMahasiswa.getNama()).setParent(arg0);
			new Label(kategoriPrestasiMahasiswa.getKeterangan()).setParent(arg0);

			Common.copyEditDeleteButtons(edit, delete, kategoriPrestasiMahasiswa, KategoriPrestasiMahasiswaAction.this)
					.setParent(arg0);

		}

	}

	public void onAdd(Event event) throws Exception {
		init(new KategoriPrestasiMahasiswa());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		kategoriPrestasiMahasiswa = (KategoriPrestasiMahasiswa) obj;
		init(kategoriPrestasiMahasiswa);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(KategoriPrestasiMahasiswa kategoriPrestasiMahasiswa) {
		this.kategoriPrestasiMahasiswa = kategoriPrestasiMahasiswa;
		addWindow.setTitle(kategoriPrestasiMahasiswa.getId() == null ? "Tambah Kategori Prestasi Mahasiswa" : "Ubah Kategori Prestasi Mahasiswa");
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

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Kategori Prestasi"));
		row.appendChild(kode = new Textbox(kategoriPrestasiMahasiswa.getKode()));
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Kategori Prestasi"));
		row.appendChild(nama = new Textbox(kategoriPrestasiMahasiswa.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(kategoriPrestasiMahasiswa.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);
		
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Feeder"));
		row.appendChild(feeder = new Textbox(kategoriPrestasiMahasiswa.getFeeder()));
		feeder.setWidth("90%"); 

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
			PesanFormalHelper.tampilkanGagal("penyimpanan data Kategori Prestasi",
					"Kolom Nama Kategori Prestasi belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Nama Kategori Prestasi.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		boolean i = checkNamaKategoriPrestasiMahasiswa();
		if (i) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Kategori Prestasi",
					"Nama Kategori Prestasi sudah terdaftar sebelumnya di database, sehingga tidak dapat disimpan kembali untuk menghindari duplikasi data.",
					new String[] {
							"Gunakan nama kategori prestasi yang berbeda dari data yang sudah ada.",
							"Periksa kembali daftar data yang sudah tersimpan apabila Bapak/Ibu ragu."
					});
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (kategoriPrestasiMahasiswa.getId() != null) {
			kategoriPrestasiMahasiswa = (KategoriPrestasiMahasiswa) session.load(KategoriPrestasiMahasiswa.class,
					kategoriPrestasiMahasiswa.getId());

		}

		kategoriPrestasiMahasiswa.setKode(kode.getValue().trim());
		kategoriPrestasiMahasiswa.setNama(nama.getValue());
		kategoriPrestasiMahasiswa.setKeterangan(keterangan.getValue()); 
		kategoriPrestasiMahasiswa.setFeeder(feeder.getValue());

		Common.refreshSaveOrUpdate(session, kategoriPrestasiMahasiswa);

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(KategoriPrestasiMahasiswa.class);

		if (order)
			criteria.addOrder(Order.asc("nama"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<KategoriPrestasiMahasiswa> kategoriPrestasiMahasiswa = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(kategoriPrestasiMahasiswa);
		grid.setRowRenderer(new KategoriPrestasiMahasiswaRenderer());
		grid.setModelCheckMobile(strset);

	}

	public Boolean checkNamaKategoriPrestasiMahasiswa() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(KategoriPrestasiMahasiswa.class)
				.setProjection(Projections.rowCount()).add(Restrictions.eq("nama", nama.getValue().trim()))
				.add(this.kategoriPrestasiMahasiswa.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.kategoriPrestasiMahasiswa.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

}
