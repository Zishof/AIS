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
import ais.action.master.feeder.util.FeederJSONImport;
import ais.action.master.helper.AmbilDataKecamatanBanbox;
import ais.action.master.helper.AmbilDataKotaKabupatenBanbox;
import ais.action.master.helper.AmbilDataPropinsiBanbox;
import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.Konfigurasi;
import ais.database.model.Tbmuser;
import ais.database.model.Wilayah;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Controller/action ZK untuk wilayah kecamatan. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code Textbox searchkode}, {@code Textbox searchnama}, {@code Textbox
 * searchkota}, {@code Textbox searchprop}, {@code Textbox nama}; inisialisasi/lifecycle ({@code
 * doBeforeCompose()}, {@code doAfterCompose()}, {@code init()}, {@code init()}, {@code initCriteria()});
 * pembacaan/pencarian ({@code onSearchDefault()}); mutasi data ({@code onSave()}); pelaporan/ekspor ({@code
 * exportKeFeeder()}); operasi domain lain ({@code onAdd()}). Bagian lain dari kontrak tetap mengikuti kelas
 * induk atau interface yang disebut di atas.</p>
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
public class WilayahKecamatanAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchkode;
	private Textbox searchnama;
	private Textbox searchkota;
	private Textbox searchprop;

	private Textbox nama;
	private Textbox keterangan;
	private AmbilDataKotaKabupatenBanbox wilayahInduk;

	private boolean edit = false;
	private boolean delete = false;

	private Wilayah wilayah;
	private MyToolbarbuttonConfig add;

	private String level = "3";
	private Textbox kode;
	private Textbox feeder;
	private AmbilDataPropinsiBanbox wilayahInduk1;

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

		if (add != null) { add.setVisible(false); }
		if (add != null) { add.setTooltiptext("Tambah"); }

		if (CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE)) {
			AmbilDataKecamatanBanbox.tambah(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					onSearchDefault(null);
				}
			}).setParent(add.getParent());
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

		String[] contents = new String[] { "id", "kode", "feeder", "nama", "wilayahInduk", "keterangan" };

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, Wilayah.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);

		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser != null && Common.getApakahAdminBolehAksesFeeder()
				&& Common.bolehKonfigurasi("aktifkan_terhubung_langsung_ke_feeder")) {

			MyToolbarbuttonConfig buttonTagihan = new MyToolbarbuttonConfig("Syn. Feeder",
					"/img/Button-Refresh-icon.png");
			buttonTagihan.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					MyMessageboxConfig.show("Apakah yakin ingin mengambil data dari feeder ?", "Pertanyaan",
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

													exportKeFeeder(username, myLabelProsesDetail, token,
															feederConnector);

													myLabelProsesDetail.setValue("");
												} catch (Exception e) {
													// FIX "gagal diam-diam": sebelumnya exception di sini hanya
													// dicatat ke log admin lalu progres diset "" (=SUKSES palsu)
													// di luar try, menutupi kegagalan dari pengguna.
													ais.common.Common.tampilErrorJikaAdmin(e);
													myLabelProsesDetail.setValue(
															"Error: " + ais.common.PesanFormalHelper.pesanGagalException(
																	"pengambilan data Wilayah/Kecamatan dari Neo Feeder",
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

	private void exportKeFeeder(String username, Label label, String token, FeederConnector feederConnector)
			throws Exception {
		// FIX "gagal diam-diam": sebelumnya exception di sini ditelan total (hanya
		// dicatat ke log admin) sehingga pemanggil (thread latar) menganggap proses
		// SELESAI sukses walau sebenarnya gagal di tengah jalan. Sekarang exception
		// dilempar ke pemanggil agar progres/pesan error ditampilkan ke pengguna.
		for (int indexs = 0; indexs <= (searchnama.getValue().trim().isEmpty() ? 10000 : 110); indexs += 100) {

			String filter = searchnama.getValue().trim().isEmpty() ? "id_level_wilayah='3'"
					: "id_level_wilayah='3' and nama_wilayah ilike '%" + searchnama.getValue().trim() + "%'";

			JSONArray dataMhsPt = feederConnector.getData("GetWilayah", token, filter, "", "100", indexs + "");
			int size = dataMhsPt.length();
			for (int i = 0; i < size; i++) {
				JSONObject jsonObject = dataMhsPt.getJSONObject(i);
				System.out.println("jsonObject -> " + jsonObject);
				label.setValue(
						"(" + Common.numberFormat.get().format((i * 100.0) / size) + "%) " + jsonObject.toString());
				FeederJSONImport.wilayah(jsonObject);
			}

		}
	}

	/**
	 * Renderer lokal untuk layar/komponen {@link WilayahKecamatanAction}. Kelas ini menerjemahkan satu item data
	 * menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link WilayahKecamatanAction} dan dapat mengakses
	 * state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see WilayahKecamatanAction
	 */
	class WilayahRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Wilayah wilayah = (Wilayah) arg1;

			new Label(wilayah.getKode()).setParent(arg0);
			new Label(wilayah.getFeeder()).setParent(arg0);
			RevisiHelper.createNewRevisi(Wilayah.class, wilayah, wilayah.getNama()).setParent(arg0);
			Label kab = new Label(wilayah.getWilayahInduk() == null ? "" : wilayah.getWilayahInduk().getNama());
			kab.setParent(arg0);

			Label prop = new Label(
					wilayah.getWilayahInduk() == null || wilayah.getWilayahInduk().getWilayahInduk() == null ? ""
							: wilayah.getWilayahInduk().getWilayahInduk().getNama());
			prop.setParent(arg0);
			new Label(wilayah.getKeterangan()).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(wilayah.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					wilayah.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(wilayah);
				}
			});

			Common.copyEditDeleteButtons(edit, delete, wilayah, WilayahKecamatanAction.this).setParent(arg0);
		}

	}

	public void onAdd(Event event) throws Exception {
		init(new Wilayah());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		wilayah = (Wilayah) obj;
		init(wilayah);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(Wilayah wilayah) {
		this.wilayah = wilayah;
		addWindow.setTitle(wilayah.getId() == null ? "Tambah Wilayah" : "Ubah Wilayah");
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

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Wilayah"));
		row.appendChild(kode = new Textbox(wilayah.getKode()));
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(Common.getApakahAdminBolehAksesFeeder());
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Feeder Wilayah"));
		row.appendChild(feeder = new Textbox(wilayah.getFeeder()));
		feeder.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Wilayah"));
		row.appendChild(nama = new Textbox(wilayah.getNama() == null ? "" : wilayah.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kota / Kabupaten"));
		row.appendChild(wilayahInduk = new AmbilDataKotaKabupatenBanbox());
		wilayahInduk.setValue(wilayah.getWilayahInduk() == null ? "" : wilayah.getWilayahInduk().getNama());
		wilayahInduk.setAttribute("wilayah", wilayah.getWilayahInduk());
		wilayahInduk.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Propinsi"));
		row.appendChild(wilayahInduk1 = new AmbilDataPropinsiBanbox());
		wilayahInduk1
				.setValue(wilayah.getWilayahInduk() == null || wilayah.getWilayahInduk().getWilayahInduk() == null ? ""
						: wilayah.getWilayahInduk().getWilayahInduk().getNama());
		wilayahInduk1.setAttribute("wilayah",
				wilayah.getWilayahInduk() == null ? null : wilayah.getWilayahInduk().getWilayahInduk());
		wilayahInduk1.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(wilayah.getKeterangan() == null ? "" : wilayah.getKeterangan()));
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
			PesanFormalHelper.tampilkanGagal("penyimpanan data Wilayah",
					"Kolom Nama Wilayah belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Nama Wilayah.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (wilayahInduk.getAttribute("wilayah") == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Kota/Kabupaten",
					"Kolom Kota/Kabupaten belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Kota/Kabupaten.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (wilayahInduk1.getAttribute("wilayah") == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Propinsi",
					"Kolom Propinsi belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Propinsi.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (wilayah.getId() != null) {
			wilayah = (Wilayah) session.load(Wilayah.class, wilayah.getId());

		}

		wilayah.setFeeder(feeder.getValue().trim());
		wilayah.setKode(kode.getValue().trim());
		wilayah.setNama(nama.getValue());
		wilayah.setKeterangan(keterangan.getValue());
		wilayah.setWilayahInduk((Wilayah) wilayahInduk.getAttribute("wilayah"));
		wilayah.setLevel(level);

		Common.refreshUpdate(session, wilayah);

		Wilayah wilayahKab = wilayah.getWilayahInduk();
		if (wilayahKab != null) {
			session.refresh(wilayahKab);
			wilayahKab.setWilayahInduk((Wilayah) wilayahInduk1.getAttribute("wilayah"));
			Common.refreshUpdate(session, wilayahKab);
		}
		session.flush();

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(Wilayah.class).add(Restrictions.eq("level", level))
				.createAlias("wilayahInduk", "kab", Criteria.LEFT_JOIN)
				.createAlias("kab.wilayahInduk", "prop", Criteria.LEFT_JOIN);

		if (order)
			criteria.addOrder(Order.asc("nama"));
		criteria.add(searchkode.getText().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.or(Restrictions.ilike("kode", searchkode.getValue().trim(), MatchMode.ANYWHERE),
						Restrictions.ilike("feeder", searchkode.getValue().trim(), MatchMode.ANYWHERE)))

				.add(searchnama.getText().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))
				.add(searchkota.getText().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("kab.nama", searchkota.getText().trim(), MatchMode.ANYWHERE))

				.add(searchprop.getText().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("prop.nama", searchprop.getText().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<Wilayah> wilayah = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(wilayah);
		grid.setRowRenderer(new WilayahRenderer());
		grid.setModelCheckMobile(strset);

	}

}
