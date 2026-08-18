package ais.action.master;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
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
import ais.database.model.JenisKegiatanMahasiswa;
import ais.database.model.Konfigurasi;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Tbmuser;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class JenisKegiatanMahasiswaAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3786091220301468178L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;
	private Textbox searchkode;
	private Textbox searchnama;

	private Textbox nama;
	private Textbox keterangan;
	private JenisKegiatanMahasiswa jenisKegiatanMahasiswa;
	private Textbox kode;

	private Checkbox searchaktif;

	private MyToolbarbuttonConfig add;
	//
	private boolean edit = false;
	private boolean delete = false;

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

		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser != null && Common.getApakahAdminBolehAksesFeeder()
				&& Common.bolehKonfigurasi("aktifkan_terhubung_langsung_ke_feeder")) {
			final PerguruanTinggi perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();
			MyToolbarbuttonConfig buttonTagihan = new MyToolbarbuttonConfig("Syn. Feeder PDDIKTI",
					"/img/Button-Refresh-icon.png");
			buttonTagihan.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					MyMessageboxConfig.show("Apakah yakin ingin singkronkan data ke feeder PDDIKTI ?", "Pertanyaan",
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
															"sinkronisasi data Jenis Kegiatan Mahasiswa ke Neo Feeder PDDIKTI",
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

		String[] contents = new String[] { "id", "kode", "nama", "keterangan", "aktif" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(JenisKegiatanMahasiswa.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, JenisKegiatanMahasiswa.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});
	}

	private void exportKeFeeder(PerguruanTinggi perguruanTinggi, FeederExporter feederImporter, String token,
			FeederConnector feederConnector) {
		try {

			JSONArray dataProdi = feederConnector.getData("GetJenisAktivitasMahasiswa", token, "", "", "5000", "0");
			System.out.println("GetJenisAktivitasMahasiswa size -> " + dataProdi.length());
			for (int index = 0; index < dataProdi.length(); index++) {
				JSONObject jsonObject = dataProdi.getJSONObject(index);
				System.out.println("jsonObject -> " + jsonObject);
				String id_jenis_aktivitas_mahasiswa = jsonObject.getString("id_jenis_aktivitas_mahasiswa");
				Session session = HibernateUtil.currentNativeSession();
				String nama_jenis_aktivitas_mahasiswa = jsonObject.getString("nama_jenis_aktivitas_mahasiswa");
				JenisKegiatanMahasiswa existing = (JenisKegiatanMahasiswa) session
						.createCriteria(JenisKegiatanMahasiswa.class)
						.add(Restrictions.ilike("kode", id_jenis_aktivitas_mahasiswa, MatchMode.EXACT)).setMaxResults(1)
						.uniqueResult();

				System.out.println("existing -> " + existing);
				if (existing == null) {

					existing = new JenisKegiatanMahasiswa();
					existing.setNama(nama_jenis_aktivitas_mahasiswa);
					existing.setKode(id_jenis_aktivitas_mahasiswa);
					session.getTransaction().begin();
					session.save(existing);
					session.getTransaction().commit();
				}
				// session.disconnect();
				if (session.isOpen()) {session.disconnect();session.close();}
				HibernateUtil.closeSession();

			}

		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
	}

	class JenisKegiatanMahasiswaRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final JenisKegiatanMahasiswa jenisKegiatanMahasiswa = (JenisKegiatanMahasiswa) arg1;

			(RevisiHelper.createNewRevisi(JenisKegiatanMahasiswa.class, jenisKegiatanMahasiswa,
					jenisKegiatanMahasiswa.getKode())).setParent(arg0);

			new Label(jenisKegiatanMahasiswa.getNama()).setParent(arg0);
			new Label(jenisKegiatanMahasiswa.getKeterangan()).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(jenisKegiatanMahasiswa.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					jenisKegiatanMahasiswa.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(jenisKegiatanMahasiswa);
				}
			});

			Common.copyEditDeleteButtons(edit, delete, jenisKegiatanMahasiswa, JenisKegiatanMahasiswaAction.this)
					.setParent(arg0);

		}

	}

	public void onAdd(Event event) throws Exception {
		init(new JenisKegiatanMahasiswa());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		jenisKegiatanMahasiswa = (JenisKegiatanMahasiswa) obj;
		init(jenisKegiatanMahasiswa);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(JenisKegiatanMahasiswa jenisKegiatanMahasiswa) {
		this.jenisKegiatanMahasiswa = jenisKegiatanMahasiswa;
		addWindow.setTitle(jenisKegiatanMahasiswa.getId() == null ? "Tambah Jenis Kegiatan Mahasiswa" : "Ubah Jenis Kegiatan Mahasiswa");
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

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode *"));
		row.appendChild(kode = new Textbox(jenisKegiatanMahasiswa.getKode()));
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama *"));
		row.appendChild(
				nama = new Textbox(jenisKegiatanMahasiswa.getNama() == null ? "" : jenisKegiatanMahasiswa.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(
				jenisKegiatanMahasiswa.getKeterangan() == null ? "" : jenisKegiatanMahasiswa.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(5);

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
			PesanFormalHelper.tampilkanGagal("penyimpanan data Kode",
					"Kolom Kode belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Kode.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (nama.getValue().trim().equals("")) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Nama",
					"Kolom Nama belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Nama.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		jenisKegiatanMahasiswa.setKode(kode.getValue().trim());
		jenisKegiatanMahasiswa.setNama(nama.getValue().trim());
		jenisKegiatanMahasiswa.setKeterangan(keterangan.getValue());

		Common.refreshSaveOrUpdate(jenisKegiatanMahasiswa);

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(JenisKegiatanMahasiswa.class)
				.add(searchaktif == null || searchaktif.isChecked() ? Restrictions.eq("aktif", true) : Restrictions.sqlRestriction("true"));
		if (order)
			criteria.addOrder(Order.asc("kode")).addOrder(Order.asc("nama"));
		criteria.add(searchkode.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("kode", searchkode.getValue().trim(), MatchMode.ANYWHERE))
				.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<JenisKegiatanMahasiswa> jenisKegiatanMahasiswa = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(jenisKegiatanMahasiswa);
		grid.setRowRenderer(new JenisKegiatanMahasiswaRenderer());
		grid.setModelCheckMobile(strset);

	}

}
