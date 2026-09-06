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
import ais.database.model.CabangPrestasiMahasiswa;
import ais.database.model.GeneralValueObject;
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

/**
 * Controller/action ZK untuk cabang prestasi mahasiswa. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code Textbox searchnama}, {@code Textbox kode}, {@code Textbox nama}, {@code
 * Textbox keterangan}, {@code boolean edit}; inisialisasi/lifecycle ({@code doBeforeCompose()}, {@code
 * doAfterCompose()}, {@code init()}, {@code init()}, {@code initCriteria()}); pembacaan/pencarian ({@code
 * onSearchDefault()}); validasi/perhitungan ({@code checkNamaCabangPrestasiMahasiswa()}); mutasi data ({@code
 * onSave()}); pelaporan/ekspor ({@code exportKeFeeder()}); operasi domain lain ({@code onAdd()}). Bagian lain
 * dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 * <p><b>Lifecycle:</b> instance mengikuti lifecycle komponen ZK dan menyimpan state layar; jangan digunakan
 * sebagai singleton atau dibagikan antar desktop/session. Event handler harus tetap memakai konteks pengguna
 * serta session Hibernate milik request yang aktif.</p>
 *
 * @see GenericAutowireComposer
 */
public class CabangPrestasiMahasiswaAction extends GenericAutowireComposer
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

	private CabangPrestasiMahasiswa cabangPrestasiMahasiswa;
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

		String[] contents = new String[] { "id", "kode", "nama", "keterangan", "feeder" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(CabangPrestasiMahasiswa.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, CabangPrestasiMahasiswa.class, contents);
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
													System.out.println("TOKEN => " + (token == null || token.isEmpty() ? "(gagal)" : "(berhasil, disamarkan)"));

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

													// FIX "gagal diam-diam": sebelumnya exception di sini (mis. gagal
													// konek/parse port) hanya dicatat ke log admin lalu progres
													// diset "" (=SUKSES palsu) di luar try, menutupi kegagalan.
													myLabelProsesDetail.setValue("");
												} catch (Exception e) {
													ais.common.Common.tampilErrorJikaAdmin(e);
													myLabelProsesDetail.setValue(
															"Error: " + ais.common.PesanFormalHelper.pesanGagalException(
																	"sinkronisasi data Cabang Prestasi Mahasiswa ke Neo Feeder",
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

			JSONArray dataProdi = feederConnector.getData("GetJenisPrestasi", token, "", "", "5000", "0");
			System.out.println("cabangPrestasiMahasiswa size -> " + dataProdi.length());
			for (int index = 0; index < dataProdi.length(); index++) {
				JSONObject jsonObject = dataProdi.getJSONObject(index);
				System.out.println("jsonObject -> " + jsonObject);
				String id_jenis_prestasi = jsonObject.getString("id_jenis_prestasi");
				Session session = HibernateUtil.currentNativeSession();
				String nama_jenis_prestasi = jsonObject.getString("nama_jenis_prestasi");
				CabangPrestasiMahasiswa existing = (CabangPrestasiMahasiswa) session
						.createCriteria(CabangPrestasiMahasiswa.class)
						.add(Restrictions.ilike("nama", nama_jenis_prestasi, MatchMode.EXACT)).setMaxResults(1)
						.uniqueResult();
				if (existing == null) {
					existing = (CabangPrestasiMahasiswa) session.createCriteria(CabangPrestasiMahasiswa.class)
							.add(Restrictions.eq("feeder", id_jenis_prestasi)).setMaxResults(1).uniqueResult();
				}
				System.out.println("existing -> " + existing);
				if (existing != null) {
					existing.setFeeder(id_jenis_prestasi);
					session.getTransaction().begin();
					session.saveOrUpdate(existing);
					session.getTransaction().commit();
				} else {
					existing = new CabangPrestasiMahasiswa();
					existing.setNama(nama_jenis_prestasi);
					existing.setKode(id_jenis_prestasi);
					existing.setFeeder(id_jenis_prestasi);
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

	/**
	 * Renderer lokal untuk layar/komponen {@link CabangPrestasiMahasiswaAction}. Kelas ini menerjemahkan satu item
	 * data menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link CabangPrestasiMahasiswaAction} dan dapat
	 * mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see CabangPrestasiMahasiswaAction
	 */
	class CabangPrestasiMahasiswaRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final CabangPrestasiMahasiswa cabangPrestasiMahasiswa = (CabangPrestasiMahasiswa) arg1;

			new Label(cabangPrestasiMahasiswa.getKode()).setParent(arg0);
			RevisiHelper.createNewRevisi(CabangPrestasiMahasiswa.class, cabangPrestasiMahasiswa,
					cabangPrestasiMahasiswa.getNama()).setParent(arg0);
			new Label(cabangPrestasiMahasiswa.getKeterangan()).setParent(arg0);

			Common.copyEditDeleteButtons(edit, delete, cabangPrestasiMahasiswa, CabangPrestasiMahasiswaAction.this)
					.setParent(arg0);

		}

	}

	public void onAdd(Event event) throws Exception {
		init(new CabangPrestasiMahasiswa());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		cabangPrestasiMahasiswa = (CabangPrestasiMahasiswa) obj;
		init(cabangPrestasiMahasiswa);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(CabangPrestasiMahasiswa cabangPrestasiMahasiswa) {
		this.cabangPrestasiMahasiswa = cabangPrestasiMahasiswa;
		addWindow.setTitle(cabangPrestasiMahasiswa.getId() == null ? "Tambah Cabang Prestasi Mahasiswa" : "Ubah Cabang Prestasi Mahasiswa");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Cabang Prestasi"));
		row.appendChild(kode = new Textbox(cabangPrestasiMahasiswa.getKode()));
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Cabang Prestasi"));
		row.appendChild(nama = new Textbox(cabangPrestasiMahasiswa.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(cabangPrestasiMahasiswa.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Feeder"));
		row.appendChild(feeder = new Textbox(cabangPrestasiMahasiswa.getFeeder()));
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
			PesanFormalHelper.tampilkanGagal("penyimpanan data Cabang Prestasi",
					"Kolom Nama Cabang Prestasi belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Nama Cabang Prestasi.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		boolean i = checkNamaCabangPrestasiMahasiswa();
		if (i) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Cabang Prestasi",
					"Nama Cabang Prestasi sudah terdaftar sebelumnya di database, sehingga tidak dapat disimpan kembali untuk menghindari duplikasi data.",
					new String[] {
							"Gunakan nama cabang prestasi yang berbeda dari data yang sudah ada.",
							"Periksa kembali daftar data yang sudah tersimpan apabila Bapak/Ibu ragu."
					});
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (cabangPrestasiMahasiswa.getId() != null) {
			cabangPrestasiMahasiswa = (CabangPrestasiMahasiswa) session.load(CabangPrestasiMahasiswa.class,
					cabangPrestasiMahasiswa.getId());

		}

		cabangPrestasiMahasiswa.setKode(kode.getValue().trim());
		cabangPrestasiMahasiswa.setNama(nama.getValue());
		cabangPrestasiMahasiswa.setKeterangan(keterangan.getValue());
		cabangPrestasiMahasiswa.setFeeder(feeder.getValue());

		Common.refreshSaveOrUpdate(session, cabangPrestasiMahasiswa);

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(CabangPrestasiMahasiswa.class);

		if (order)
			criteria.addOrder(Order.asc("nama"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<CabangPrestasiMahasiswa> cabangPrestasiMahasiswa = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(cabangPrestasiMahasiswa);
		grid.setRowRenderer(new CabangPrestasiMahasiswaRenderer());
		grid.setModelCheckMobile(strset);

	}

	public Boolean checkNamaCabangPrestasiMahasiswa() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(CabangPrestasiMahasiswa.class)
				.setProjection(Projections.rowCount()).add(Restrictions.eq("nama", nama.getValue().trim()))
				.add(this.cabangPrestasiMahasiswa.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.cabangPrestasiMahasiswa.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

}
