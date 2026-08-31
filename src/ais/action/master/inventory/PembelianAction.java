package ais.action.master.inventory;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.East;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Messagebox;
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
import org.zkoss.zul.Vbox;

import ais.action.master.helper.AmbilDataMahasiswaBanbox;
import ais.action.master.helper.AmbilDataUploadLogBanbox;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.inventory.helper.PembelianPunyaBarangHelper;
import ais.action.master.sekolah.helper.AmbilDataSiswaBanbox;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.Mahasiswa;
import ais.database.model.Tbmuser;
import ais.database.model.inventory.Pembelian;
import ais.database.model.inventory.Produk;
import ais.database.model.inventory.Toko;
import ais.database.model.sekolah.Siswa;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyLabelBold;
import ais.ui.util.MyLabelBoldAja;
import ais.ui.util.MyLabelBolder;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Controller/action ZK untuk pembelian. Tipe ini merupakan titik masuk UI yang menghubungkan event
 * layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code Textbox searchsiswa}, {@code Textbox searchnama}, {@code MyDatebox
 * start}, {@code MyDatebox end}, {@code AmbilDataUploadLogBanbox searchUploadLog}; inisialisasi/lifecycle
 * ({@code doBeforeCompose()}, {@code doAfterCompose()}, {@code init()}, {@code init()}, {@code initDetail()},
 * {@code initCriteria()}); pembacaan/pencarian ({@code onCatatanUpload()}, {@code onSearchDefault()}, {@code
 * tampilkanIntroDashboardInventoryV1()}); operasi domain lain ({@code onAdd()}, {@code
 * escapeDashboardHtmlInventoryV1()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang
 * disebut di atas.</p>
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
public class PembelianAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchsiswa;
	private Textbox searchnama;
	private MyDatebox start;
	private MyDatebox end;
	private AmbilDataUploadLogBanbox searchUploadLog;

	private MyLabelBolder infobeli;

	private Textbox keterangan;
	private Textbox invoice;

	private MyLabelBolder hargaSatuan;
	private MyLabelBolder hargaTotal;

	private boolean edit = false;
	private boolean delete = false;

	private Pembelian pembelian;
	private MyToolbarbuttonConfig add;
	private AmbilDataSiswaBanbox siswa;

	private PembelianPunyaBarangHelper pembelianPunyaBarangHelper;
	private Combobox toko;
	private Combobox searchtoko;
	private Toko currentToko;
	private Siswa selectedSiswa;
	private MyLabelBold tabungan;
	private Vbox foto;

	private Tabpanel uploadLog;

	public void onCatatanUpload(Event event) {
		if (uploadLog.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(uploadLog);
			MyInclude iframe = new MyInclude("/pages/master/upload_log.zul?className=" + Pembelian.class.getName());
			iframe.setParent(window);
		}
	}

	private Label siswatampil, nimsiswatampil, kelassiswatampil;
	private AmbilDataMahasiswaBanbox mahasiswa;

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

		tampilkanIntroDashboardInventoryV1(comp, "Dashboard Pembelian POS", "Menampilkan riwayat pembelian, pelanggan, toko, invoice, produk, dan total belanja agar petugas mudah menelusuri transaksi. Data ini membantu pengecekan struk, audit penjualan, serta mengurangi risiko transaksi ganda.");

		Common.insertComboDanSemua(searchtoko, "nama", Toko.class, Restrictions.eq("aktif", true));
		currentToko = Common.getCurrentToko();
		if (currentToko != null) {
			Common.selectComboItem(true, searchtoko, currentToko);
			searchtoko.setDisabled(!currentToko.getBolehMelihatTokolain());
		}

		if (ExecutionsCtrl.getCurrent().getParameter("siswa") != null) {
			selectedSiswa = (Siswa) HibernateUtil.currentSession().createCriteria(Siswa.class)
					.add(Restrictions.isNotNull("namaSiswa")).add(Restrictions.ne("namaSiswa", ""))
					.add(Restrictions.isNotNull("sekolah"))
					.add(Restrictions.idEq(Long.parseLong(ExecutionsCtrl.getCurrent().getParameter("siswa"))))
					.uniqueResult();

			searchsiswa.setValue(selectedSiswa == null ? "" : selectedSiswa.getNomorInduk());
			searchsiswa.setDisabled(selectedSiswa != null);
			// siswa.setAttribute("siswa", selectedSiswa);
			// siswa.setValue(selectedSiswa == null ? "" :
			// selectedSiswa.getNama());
			// siswa.setDisabled(true);

		}

		if (searchUploadLog != null) {
			searchUploadLog.setEventListener(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					onSearchDefault(null);
				}
			});
		}

		Tbmuser tbmuser = Common.getCurrentUser();

		if (tbmuser != null && tbmuser.getSiswa() != null) {
			selectedSiswa = tbmuser.getSiswa();
			searchsiswa.setValue(selectedSiswa == null ? "" : selectedSiswa.getNomorInduk());
			searchsiswa.setDisabled(selectedSiswa != null);
		}

		if (selectedSiswa != null) {
			if (siswatampil != null)
				siswatampil.setValue("Nama Siswa : " + (selectedSiswa == null ? "" : selectedSiswa.getNama()));
			if (nimsiswatampil != null)
				nimsiswatampil.setValue("NIS Siswa : " + (selectedSiswa == null ? "" : selectedSiswa.getNim()));
			if (kelassiswatampil != null)
				kelassiswatampil.setValue("Kelas Siswa : " + (selectedSiswa == null ? ""
						: (selectedSiswa.getKelas() == null ? "" : selectedSiswa.getKelas().getNama())));
		}

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE) && selectedSiswa == null);
		add.setTooltiptext("Tambah");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE) && selectedSiswa == null;
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE) && selectedSiswa == null;
		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		String[] contents = new String[] { "id", "kode", "nama", "produk", "qty", "member", "hargaSatuan", "hargaJual",
				"waktu", "siswa", "mahasiswa", "calonSiswa", "biodataCalonMahasiswa", "tbmuser", "toko", "keterangan",
				"uploadLog" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, Pembelian.class, new EventListener() {

			@SuppressWarnings("rawtypes")
			@Override
			public void onEvent(Event arg0) throws Exception {
				Object[] o = (Object[]) arg0.getData();
				Map datum = (Map) o[2];
				Session session = (Session) o[1];
				Pembelian pembelian = (Pembelian) o[0];
				XSSFSheet sheet = (XSSFSheet) o[4];
				Integer row = (Integer) o[5];
				if (currentToko != null) {
					pembelian.setToko(currentToko);
				}

				if (pembelian.getSiswa() == null && datum.get("siswa") != null
						&& !datum.get("siswa").toString().trim().isEmpty()) {
					Siswa siswa = (Siswa) session.createCriteria(Siswa.class).add(Restrictions.isNotNull("namaSiswa"))
							.add(Restrictions.ne("namaSiswa", "")).add(Restrictions.isNotNull("sekolah"))
							.add(Restrictions.eq("nomorInduk", datum.get("siswa").toString().trim()))
							.addOrder(Order.desc("id")).setMaxResults(1).uniqueResult();
					pembelian.setSiswa(siswa);
				}

				if (pembelian.getToko() != null) {

					if ((datum.get("produk") != null && !datum.get("produk").toString().trim().isEmpty())) {

						Produk produk = (Produk) Common.getSheetContentAsObject(sheet, 3, row, Produk.class,
								Restrictions.eq("toko", pembelian.getToko()));

						if (produk == null) {
							produk = (Produk) session.createCriteria(Produk.class)
									.add(Restrictions.ilike("kode", datum.get("produk").toString().trim(),
											MatchMode.EXACT))
									.add(Restrictions.eq("toko", pembelian.getToko())).setMaxResults(1).uniqueResult();
						}
						if (produk == null) {
							produk = (Produk) session.createCriteria(Produk.class)
									.add(Restrictions.ilike("nama", datum.get("produk").toString().trim(),
											MatchMode.EXACT))
									.add(Restrictions.eq("toko", pembelian.getToko())).setMaxResults(1).uniqueResult();
						}
						if (produk == null) {
							produk = new Produk();
							produk.setHargaJual(pembelian.getHargaJual());
							produk.setToko(pembelian.getToko());
							produk.setNama(datum.get("produk").toString().trim());
							produk.setKode(datum.get("produk").toString().trim());
							session.getTransaction().begin();
							session.save(produk);
							session.getTransaction().commit();
						}
						pembelian.setProduk(produk);
					}
				}
			}

		}, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete && selectedSiswa == null); }
		Common.appendKeToolbar(upload, add, comp);
	}

	class PembelianRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Pembelian pembelian = (Pembelian) arg1;

			Vbox a;
			(a = RevisiHelper.createNewRevisi(Pembelian.class, pembelian, pembelian.getKode())).setParent(arg0);

			if (Common.isMobile()) {
				new Label(("Waktu : ")
						+ (pembelian.getWaktu() == null ? "" : Common.dateFormat5.get().format(pembelian.getWaktu())))
						.setParent(a);
				new Label(("Produk : ") + (pembelian.getProduk() == null ? "" : pembelian.getProduk().getNama()))
						.setParent(a);
				new Label(("Qty : ") + (Common.numberFormat.get().format(pembelian.getQty()))).setParent(a);
				new Label(("Harga : ") + (Common.numberFormat.get().format(pembelian.getHargaSatuan()))).setParent(a);
				new Label(("Total : ") + (Common.numberFormat.get().format(pembelian.getHargaJual()))).setParent(a);
				new Label(pembelian.getToko() == null ? "" : pembelian.getToko().getNama()).setParent(a);
				new Label(pembelian.getKeterangan()).setParent(a);
			}

			else if (pembelian.getUploadLog() != null) {
				A aa = new A(pembelian.getUploadLog().getNama());
				String url = Common.getRequestHostWithProtocol() + "/AmbilFileServer?file="
						+ URLEncoder.encode(pembelian.getUploadLog().getKeterangan(), "UTF-8");

				aa.setHref(url);
				aa.setParent(a);
			}

			new Label(pembelian.getProduk() == null ? "" : pembelian.getProduk().getKode()).setParent(arg0);
			new Label(pembelian.getProduk() == null ? "" : pembelian.getProduk().getNama()).setParent(arg0);
			new Label(Common.numberFormat.get().format(pembelian.getQty())).setParent(arg0);
			new Label(Common.numberFormat.get().format(pembelian.getHargaSatuan())).setParent(arg0);
			new Label(Common.numberFormat.get().format(pembelian.getHargaJual())).setParent(arg0);
			new Label(pembelian.getWaktu() == null ? "" : Common.dateFormat5.get().format(pembelian.getWaktu()))
					.setParent(arg0);
			new Label(pembelian.getMember()).setParent(arg0);
			new Label(pembelian.getToko() == null ? "" : pembelian.getToko().getNama()).setParent(arg0);
			new Label(pembelian.getKeterangan()).setParent(arg0);

			Common.copyEditDeleteButtons(edit, delete, pembelian, PembelianAction.this).setParent(arg0);

		}

	}

	public void onAdd(Event event) throws Exception {
		init(new Pembelian());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		pembelian = (Pembelian) obj;
		init(pembelian);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(Pembelian pembelian) throws Exception {
		this.pembelian = pembelian;
		addWindow.setTitle(pembelian.getId() == null ? "Tambah Pembelian" : "Ubah Pembelian");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

		East east = new East();
		east.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(east, true);
		east.setWidth("70%");

		String invoiceCode = "INV-" + (ais.ui.util.WaktuUtil.getDate().getTime());
		if (pembelian.getId() == null) {
			pembelian.setKode(invoiceCode);
			pembelian.setMember("1");
		} else {
			invoiceCode = pembelian.getKode();
		}

		toko = new Combobox();

		initDetail(pembelian, east);

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
		row.appendChild(new ais.ui.util.MyLabelConfig("Invoice"));
		row.appendChild(invoice = new Textbox(invoiceCode));
		invoice.setWidth("90%");
		invoice.setRows(1);

		if (pembelian.getId() != null) {
			invoice.setDisabled(true);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Siswa"));
		row.appendChild(siswa = new AmbilDataSiswaBanbox());
		siswa.setValue(pembelian.getSiswa() == null ? "" : pembelian.getSiswa().getNamaSiswa());
		siswa.setAttribute("siswa", pembelian.getSiswa());
		siswa.setAttribute("myValue", pembelian.getSiswa());
		siswa.setWidth("90%");
		siswa.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Mahasiswa"));
		row.appendChild(mahasiswa = new AmbilDataMahasiswaBanbox());
		mahasiswa.setValue(pembelian.getMahasiswa() == null ? "" : pembelian.getMahasiswa().getNama());
		mahasiswa.setAttribute("mahasiswa", pembelian.getMahasiswa());
		mahasiswa.setAttribute("myValue", pembelian.getMahasiswa());
		mahasiswa.setWidth("90%");
		mahasiswa.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Foto"));
		row.appendChild(foto = new Vbox());
		foto.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tabungan"));
		row.appendChild(tabungan = new MyLabelBold("Rp. 0.0"));

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				mahasiswa.getParent().setVisible(true);
				siswa.getParent().setVisible(true);

				if (siswa.getAttribute("siswa") != null) {
					mahasiswa.getParent().setVisible(false);

					Siswa s = (Siswa) siswa.getAttribute("siswa");

					tabungan.setValue(
							"Rp. " + Common.numberFormat.get().format(s.hitungSisaDeposit(ais.ui.util.WaktuUtil.getDate())));

					Common.clear(foto);
					CommonMedia.tampilkanGambarKecil(s).setParent(foto);
					foto.appendChild(new MyLabelBoldAja(s.getNamaSiswa()));

				} else if (mahasiswa.getAttribute("mahasiswa") != null) {
					siswa.getParent().setVisible(false);

					Mahasiswa s = (Mahasiswa) mahasiswa.getAttribute("mahasiswa");

					double tab = ais.action.master.sekolah.util.DepositHelper.hitungDeposit(s);
					tabungan.setValue("Rp. " + Common.numberFormat.get().format(tab));

					Common.clear(foto);
					CommonMedia.tampilkanGambarKecil(s).setParent(foto);

				}

			}
		};

		mahasiswa.setEventListener(eventListener);
		siswa.setEventListener(eventListener);
		eventListener.onEvent(null);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Toko / Penjual *"));
		row.appendChild(toko);
		Common.insertCombo(toko, "nama", Toko.class);
		Common.selectComboItem(toko, pembelian.getToko());
		toko.setWidth("90%");
		toko.setReadonly(true);

		if (currentToko != null) {
			Common.selectComboItem(true, toko, currentToko);
			toko.setDisabled(!currentToko.getBolehMelihatTokolain());
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(pembelian.getKeterangan()));
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
			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {

				Siswa s = (Siswa) siswa.getAttribute("siswa");
				if (s != null) {
					if (s.hitungSisaDeposit(ais.ui.util.WaktuUtil.getDate()) < pembelianPunyaBarangHelper.total) {
						MyMessageboxConfig.show("Mohon maaf, saldo Tabungan Siswa tidak mencukupi untuk menyelesaikan transaksi ini. Langkah yang dapat dilakukan: (1) periksa kembali saldo Tabungan Siswa; (2) lakukan penambahan saldo terlebih dahulu; (3) ulangi kembali proses transaksi.", "Peringatan", MyMessageboxConfig.OK,
								MyMessageboxConfig.INFORMATION);
						return;
					}
				}

				Mahasiswa m = (Mahasiswa) mahasiswa.getAttribute("mahasiswa");

				if (m != null) {
					double tab = ais.action.master.sekolah.util.DepositHelper.hitungDeposit(m);
					if (tab < pembelianPunyaBarangHelper.total) {
						MyMessageboxConfig.show("Mohon maaf, saldo Tabungan Mahasiswa tidak mencukupi untuk menyelesaikan transaksi ini. Langkah yang dapat dilakukan: (1) periksa kembali saldo Tabungan Mahasiswa; (2) lakukan penambahan saldo terlebih dahulu; (3) ulangi kembali proses transaksi.", "Peringatan",
								MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
						return;
					}
				}

				if (toko.getSelectedItem() == null || toko.getSelectedItem().getValue() == null) {
					MyMessageboxConfig.show("Mohon maaf, Bapak/Ibu diminta untuk memilih Toko / Penjual terlebih dahulu sebelum melanjutkan proses. Langkah yang dapat dilakukan: (1) buka daftar Toko / Penjual; (2) pilih salah satu Toko / Penjual yang sesuai; (3) ulangi kembali proses ini.", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return;
				}

				Session session = HibernateUtil.currentSession();

				if (s != null) {
					List<Pembelian> produksYangDibeliHariItu = session.createCriteria(Pembelian.class)
							.add(Restrictions.eq("siswa", s))
							.add(Restrictions.sqlRestriction("date(waktu)=CURRENT_DATE")).list();
					List<Pembelian> tambahan = pembelianPunyaBarangHelper.tambahan();

					produksYangDibeliHariItu.addAll(tambahan);

					Map<Long, List<Pembelian>> map = new java.util.HashMap<Long, List<Pembelian>>();
					for (Pembelian pembelian : produksYangDibeliHariItu) {
						if (map.containsKey(pembelian.getProduk().getJenisProduk().getId())) {
							map.get(pembelian.getProduk().getJenisProduk().getId()).add(pembelian);
						} else {
							List<Pembelian> ss = new ArrayList<Pembelian>();
							ss.add(pembelian);
							map.put(pembelian.getProduk().getJenisProduk().getId(), ss);
						}
					}
					for (Long jenisProduk : map.keySet()) {
						List<Pembelian> pem = map.get(jenisProduk);
						Double max = pem.get(0).getProduk().getJenisProduk().getMaksimalHarian();
						Double total = 0.0;
						for (Pembelian beli : pem) {
							total += beli.getHargaJual();
						}
						if (total > max) {
							MyMessageboxConfig.showFormat(
									"Mohon maaf, untuk Jenis Produk \"{V1}\" batas maksimal pembelian harian adalah {V2}, sedangkan total pembelian telah melebihi batas harian tersebut yaitu {V3}. Langkah yang dapat dilakukan: (1) periksa kembali total pembelian harian pembeli; (2) kurangi jumlah pembelian agar tidak melebihi batas; (3) ulangi kembali proses ini.",
									"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
									pem.get(0).getProduk().getJenisProduk().getNama(), Common.numberFormat.get().format(max),
									Common.numberFormat.get().format(total));
							return;
						}
					}
				}

				PembelianAction.this.pembelian.setSiswa(s);
				PembelianAction.this.pembelian.setMahasiswa(m);
				PembelianAction.this.pembelian.setToko((Toko) toko.getSelectedItem().getValue());
				pembelianPunyaBarangHelper.simpan(PembelianAction.this.pembelian);
				onSearchDefault(null);
				addWindow.setVisible(false);
				// }
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);

	}

	protected void initDetail(final Pembelian pembelian, Component component) throws Exception {
		this.pembelian = pembelian;
		Tabbox tabbox = new Tabbox();
		tabbox.setParent(component);
		tabbox.setHeight("100%");
		tabbox.setWidth("100%");

		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);

		MyTabConfig tabDipinjam = new MyTabConfig("Item yang dibeli");
		tabDipinjam.setParent(tabs);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		final Tabpanel tabpanelDibeli = new ais.ui.util.MyTabpanel();
		tabpanelDibeli.setParent(tabpanels);
		tabpanelDibeli.setWidth("100%");

		tabpanelDibeli.appendChild((pembelianPunyaBarangHelper = new PembelianPunyaBarangHelper(new MyGrid(), toko))
				.initDetail(PembelianAction.this.pembelian));

	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(Pembelian.class)
				.add(searchUploadLog != null && searchUploadLog.getAttribute("uploadLog") != null
						? Restrictions.eq("uploadLog", searchUploadLog.getAttribute("uploadLog"))
						: Restrictions.sqlRestriction("true"))

				.add(selectedSiswa != null ? Restrictions.eq("siswa", selectedSiswa)
						: Restrictions.sqlRestriction("true"))

				.add(start == null || start.getValue() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.sqlRestriction("(this_.waktu) >= ('"
								+ Common.databaseDateFormat.get().format(start.getValue()) + " 00:00:00')"))

				.add(end == null || end.getValue() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.sqlRestriction("(this_.waktu) <= ('"
								+ Common.databaseDateFormat.get().format(end.getValue()) + " 23:59:59')"));

		if (!searchsiswa.getValue().trim().isEmpty()) {
			criteria.createAlias("siswa", "siswa", Criteria.LEFT_JOIN)
					.createAlias("calonSiswa", "calonSiswa", Criteria.LEFT_JOIN)

					.add(Restrictions.or(
							Restrictions.ilike("siswa.nomorIndukNasional", searchsiswa.getValue().trim(),
									MatchMode.ANYWHERE),
							Restrictions.or(
									Restrictions.ilike("calonSiswa.nomorIndukNasional", searchsiswa.getValue().trim(),
											MatchMode.ANYWHERE),
									Restrictions.or(
											Restrictions.ilike("calonSiswa.namaSiswa", searchsiswa.getValue().trim(),
													MatchMode.ANYWHERE),
											Restrictions.or(
													Restrictions.ilike("siswa.namaSiswa", searchsiswa.getValue().trim(),
															MatchMode.ANYWHERE),
													Restrictions.or(
															Restrictions.ilike("siswa.nomorInduk",
																	searchsiswa.getValue().trim(), MatchMode.ANYWHERE),
															Restrictions.ilike("calonSiswa.nomorInduk",
																	searchsiswa.getValue().trim(),
																	MatchMode.ANYWHERE)))))));
		}

		if (order)
			criteria.addOrder(
					searchUploadLog != null && searchUploadLog.getAttribute("uploadLog") != null ? Order.asc("id")
							: Order.desc("id"));

		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))

				.add(searchtoko.getSelectedItem() == null || searchtoko.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("true")
						: Restrictions.eq("toko", searchtoko.getSelectedItem().getValue()));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<Pembelian> pembelian = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(pembelian);
		grid.setRowRenderer(new PembelianRenderer());
		grid.setModelCheckMobile(strset);

		if (hargaSatuan != null && hargaTotal != null) {
			Object[] a = (Object[]) initCriteria(false).setProjection(
					Projections.projectionList().add(Projections.sum("hargaSatuan")).add(Projections.sum("hargaJual")))
					.uniqueResult();
			Double hs = a[0] == null ? 0.0 : ((Number) a[0]).doubleValue();
			Double ht = a[1] == null ? 0.0 : ((Number) a[1]).doubleValue();

			hargaSatuan.setValue(Common.numberFormat.get().format(hs));
			hargaTotal.setValue(Common.numberFormat.get().format(ht));

			if (infobeli != null) {
				infobeli.setValue("Total : " + Common.numberFormat.get().format(ht));
			}
		}

	}


	private void tampilkanIntroDashboardInventoryV1(Component parent, String judul, String deskripsi) {
		if (parent == null) {
			return;
		}
		try {
			org.zkoss.zul.Html html = new org.zkoss.zul.Html("<div style=\"margin:0 0 10px 0;padding:14px 16px;"
					+ "border-radius:16px;background:linear-gradient(135deg, rgba(0,0,0,.35), rgba(0,0,0,0) 55%), linear-gradient(135deg, var(--ais-theme-primary,#1d4ed8) 0%, var(--ais-theme-primary,#1d4ed8) 45%, var(--ais-theme-accent,#06b6d4) 100%);color:#ffffff;"
					+ "box-shadow:0 12px 24px rgba(15,23,42,.16);\">"
					+ "<div style=\"font-size:17px;font-weight:900;line-height:1.25;\">" + escapeDashboardHtmlInventoryV1(judul) + "</div>"
					+ "<div style=\"font-size:12px;line-height:1.65;margin-top:6px;opacity:.93;\">" + escapeDashboardHtmlInventoryV1(deskripsi) + "</div>"
					+ "<div style=\"display:flex;gap:8px;flex-wrap:wrap;margin-top:10px;\">"
					+ "<span style=\"display:inline-block;padding:5px 9px;border-radius:999px;background:#dbeafe;color:#1e40af;font-size:10.5px;font-weight:900;\">HTML/CSS modern</span>"
					+ "<span style=\"display:inline-block;padding:5px 9px;border-radius:999px;background:#dcfce7;color:#166534;font-size:10.5px;font-weight:900;\">Data operasional POS</span>"
					+ "<span style=\"display:inline-block;padding:5px 9px;border-radius:999px;background:#fef3c7;color:#92400e;font-size:10.5px;font-weight:900;\">Mudah dipahami end user</span>"
					+ "</div></div>");
			if (parent.getChildren() != null && parent.getChildren().size() > 0) {
				parent.insertBefore(html, (org.zkoss.zk.ui.Component) parent.getChildren().get(0));
			} else {
				parent.appendChild(html);
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/inventory/PembelianAction.java:724");
			/* informasi intro tidak boleh menggagalkan halaman utama */
		}
	}

	private String escapeDashboardHtmlInventoryV1(String value) {
		if (value == null) {
			return "";
		}
		String s = value;
		s = s.replace("&", "&amp;");
		s = s.replace("<", "&lt;");
		s = s.replace(">", "&gt;");
		s = s.replace("\"", "&quot;");
		s = s.replace("'", "&#39;");
		return s;
	}

}
