package ais.action.master.epsbed;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Box;
import org.zkoss.zul.Center;
import ais.ui.util.MyColumnConfig;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyComboitemConfig;
import org.zkoss.zul.Decimalbox;
import org.zkoss.zul.Div;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import ais.ui.util.MyMessageboxConfig;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import ais.ui.util.MyToolbarbuttonConfig;

import ais.common.Common;
import ais.database.dao.DaoFactory;
import ais.database.dao.PublikasiDosenDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.epsbed.EpsbedJenisKaryaIlmiah;
import ais.database.model.epsbed.EpsbedMediaPublikasi;
import ais.database.model.epsbed.EpsbedPembiayaanPenelitian;
import ais.database.model.epsbed.EpsbedPeranPenulisan;
import ais.database.model.epsbed.EpsbedPublikasiDosen;

public class PublikasiDosenHelper {

	private MyGrid grid = new MyGrid();
	private Center center = new Center();
	North north = new North();
	South south = new South();

	Toolbar toolbar = new Toolbar();
	MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Simpan dan Kembali", "/img/box-icon64x64.png");

	// private Combobox searchTahunAkademik = new Combobox();
	private Combobox noUrutJumlahKaryaIlmiah = new Combobox();
	private Combobox mandiriKelompok = new Combobox();

	// private Row row;
	private MyComboitemConfig comboitem;
	private Combobox bulanPublikasi = new Combobox();
	private Combobox tahunPublikasi = new Combobox();
	private Combobox mediaPublikasi;
	// private Combobox kodeAuthor;
	private Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
	public Dosen dosen;
	// private EpsbedPublikasiDosen epsbedPublikasiDosen;
	private Combobox peranDalamPenelitian;
	private Combobox jenisPenelitian;
	private Combobox pembiayaanPenelitian;
	private Decimalbox jumlahBiaya;
	private Textbox judul1;
	private Textbox judul2;
	private Textbox judul3;
	private Textbox judul4;
	private Textbox judul5;
	private EpsbedPublikasiDosen publikasiDosen;
	MyToolbarbuttonConfig simpan = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
	MyToolbarbuttonConfig kembali = new MyToolbarbuttonConfig("Kembali", "/img/cancel.gif");

	public PublikasiDosenHelper() {
		// this.dosen = dosen;
		Common.insertCombo(jenisPenelitian = new Combobox(), "nama", EpsbedJenisKaryaIlmiah.class);
		Common.insertCombo(mediaPublikasi = new Combobox(), "nama", EpsbedMediaPublikasi.class);
		// Common.insertCombo(kodeAuthor = new Combobox(), "nama",
		// EpsbedPeranPenulisan.class);
		Common.insertCombo(peranDalamPenelitian = new Combobox(), "nama", EpsbedPeranPenulisan.class);
		Common.insertCombo(pembiayaanPenelitian = new Combobox(), "nama", EpsbedPembiayaanPenelitian.class);
		for (int i = 1; i <= 12; i++) {
			comboitem = new MyComboitemConfig(i + "");
			comboitem.setValue(i);
			noUrutJumlahKaryaIlmiah.appendChild(comboitem);
		}

		comboitem = new MyComboitemConfig("Mandiri");
		comboitem.setValue("Mandiri");
		mandiriKelompok.appendChild(comboitem);
		comboitem = new MyComboitemConfig("Kelompok");
		comboitem.setValue("Kelompok");
		mandiriKelompok.appendChild(comboitem);

		for (int i = 1; i <= 12; i++) {
			comboitem = new MyComboitemConfig(i + "");
			comboitem.setValue(i);
			bulanPublikasi.appendChild(comboitem);
		}

		for (int i = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR) - 20; i <= ais.ui.util.WaktuUtil.getCalendar()
				.get(Calendar.YEAR); i++) {
			comboitem = new MyComboitemConfig(i + "");
			comboitem.setValue(i);
			tahunPublikasi.appendChild(comboitem);
		}

		// display(dosen);

	}

	class PublikasiDosenRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row row, Object arg1) throws Exception {
			// TODO Auto-generated method stub
			final EpsbedPublikasiDosen epsbedPublikasiDosen = (EpsbedPublikasiDosen) arg1;
			new Label(epsbedPublikasiDosen.getTahunPublikasi() + "/" + epsbedPublikasiDosen.getBulanPublikasi())
					.setParent(row);
			new Label(epsbedPublikasiDosen.getJudul1()).setParent(row);
			new Label(epsbedPublikasiDosen.getKodeKegiatanMandiriKelompok()).setParent(row);
			new Label(epsbedPublikasiDosen.getKodeMediaPublikasi() == null ? ""
					: epsbedPublikasiDosen.getKodeMediaPublikasi().getNama()).setParent(row);

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(epsbedPublikasiDosen);

				}

			});
			button.setParent(toolbar);

			button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");

			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {
											PublikasiDosenDao publikasiDosenDao = DaoFactory.getInstance()
													.getPublikasiDosenDao();
											publikasiDosenDao.delete(publikasiDosenDao.merge(publikasiDosen));
											// agamaDao.commitTransaction();
											onSearchDefault(dosen);
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
			toolbar.setParent(row);
		}
	}

	public Borderlayout display(final Dosen dosen) {
		this.dosen = dosen;

		borderlayout.setWidth("100%");
		Common.clear(center);
		Common.clear(north);
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, true);

		Div div = new Div();
		div.setParent(north);

		MyGrid searchgrid = new MyGrid();
		searchgrid.setWidth("100%");
		searchgrid.setParent(div);

		Rows rows = new Rows();
		rows.setParent(searchgrid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Tambah Data", "/img/new.gif");
		row.appendChild(toolbarbutton);
		toolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				// TODO Auto-generated method stub
				init(new EpsbedPublikasiDosen());
			}
		});

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(div);

		grid = new MyGrid();//grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(10);grid.getPagingChild().setMold("os");
		grid.setParent(center);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Tahun / Bulan");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Judul");
		column.setWidth("35%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jenis");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Publikasi");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");

		onSearchDefault(dosen);

		// window.setVisible(true);
		// try {
		// window.onModal();
		// } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/epsbed/PublikasiDosenHelper.java:261");
		// // TODO Auto-generated catch block
		// Common.tampilErrorJikaAdmin(e); 
		// }

		return borderlayout;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Dosen dosen) {

		Session session = HibernateUtil.currentSession();
		List<EpsbedPublikasiDosen> epsbedPublikasiDosen = session.createCriteria(EpsbedPublikasiDosen.class)
				.addOrder(Order.desc("tahunPublikasi")).addOrder(Order.desc("bulanPublikasi"))
				.add(Restrictions.eq("dosen", dosen)).setMaxResults(Common.MAX_RESULT).list();

		ListModel strset = new SimpleListModel(epsbedPublikasiDosen);

		grid.setRowRenderer(new PublikasiDosenRenderer());
		grid.setModelCheckMobile(strset);

		

	}

	public void init(final EpsbedPublikasiDosen epsbedPublikasiDosen) throws Exception {
		this.publikasiDosen = epsbedPublikasiDosen;
		// System.out.println("test");
		Common.clear(borderlayout);
		borderlayout.setWidth("100%");
		Common.clear(center);
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		// Common.generateTahunAjaran(searchTahunAkademik);
		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(
				new Label(epsbedPublikasiDosen.getDosen() == null ? "" : epsbedPublikasiDosen.getDosen().getNama()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("No Urut"));
		row.appendChild(noUrutJumlahKaryaIlmiah);
		Common.selectComboItem(noUrutJumlahKaryaIlmiah,
				epsbedPublikasiDosen.getUrut() == null ? null : epsbedPublikasiDosen.getUrut());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Penelitian"));
		row.appendChild(jenisPenelitian);
		Common.selectComboItem(jenisPenelitian, epsbedPublikasiDosen.getKodeJenisPenelitian());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Media Publikasi"));
		row.appendChild(mediaPublikasi);
		Common.selectComboItem(mediaPublikasi, epsbedPublikasiDosen.getKodeMediaPublikasi());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Peran Dalam Penelitian"));
		row.appendChild(peranDalamPenelitian);
		Common.selectComboItem(peranDalamPenelitian,
				epsbedPublikasiDosen.getKodeAuthor() == null ? null : epsbedPublikasiDosen.getKodeAuthor());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Penelitian"));
		row.appendChild(mandiriKelompok);
		Common.selectComboItem(mandiriKelompok, epsbedPublikasiDosen.getKodeKegiatanMandiriKelompok());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun / Bulan Penelitian"));
		Box box = new Box();
		box.setWidth("100%");
		row.appendChild(box);
		box.appendChild(tahunPublikasi);
		box.appendChild(bulanPublikasi);
		Common.selectComboItem(tahunPublikasi, epsbedPublikasiDosen.getTahunPublikasi());
		Common.selectComboItem(bulanPublikasi, epsbedPublikasiDosen.getBulanPublikasi());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pembiayaan Penelitian"));
		row.appendChild(pembiayaanPenelitian);
		Common.selectComboItem(pembiayaanPenelitian, epsbedPublikasiDosen.getKodePembiayaan());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jumlah Biaya"));
		row.appendChild(jumlahBiaya = new Decimalbox(new BigDecimal(epsbedPublikasiDosen.getJumlahBiaya())));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Judul 1"));
		row.appendChild(judul1 = new Textbox(epsbedPublikasiDosen.getJudul1()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Judul 2"));
		row.appendChild(judul2 = new Textbox(epsbedPublikasiDosen.getJudul2()));
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Judul 3"));
		row.appendChild(judul3 = new Textbox(epsbedPublikasiDosen.getJudul3()));
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Judul 4"));
		row.appendChild(judul4 = new Textbox(epsbedPublikasiDosen.getJudul4()));
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Judul 5"));
		row.appendChild(judul5 = new Textbox(epsbedPublikasiDosen.getJudul5()));

		judul1.setWidth("97%");
		judul2.setWidth("97%");
		judul3.setWidth("97%");
		judul4.setWidth("97%");
		judul5.setWidth("97%");

		judul1.setRows(2);
		judul2.setRows(2);
		judul3.setRows(2);
		judul4.setRows(2);
		judul5.setRows(2);

		south.setParent(borderlayout);

		toolbar.setParent(south);

		simpan.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				// TODO Auto-generated method stub
				if (save(arg0)) {
					display(dosen);
					south.detach();
				}
			}
		});
		simpan.setParent(toolbar);

		kembali.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				// TODO Auto-generated method stub
				display(dosen);
				south.detach();
			}
		});
		kembali.setParent(toolbar);

	}

	public boolean save(Event event) {
		PublikasiDosenDao publikasiDosenDao = DaoFactory.getInstance().getPublikasiDosenDao();
		if (publikasiDosen.getId() != null) {
			publikasiDosen = publikasiDosenDao.load(publikasiDosen.getId());
		}
		publikasiDosen.setDosen(dosen);
		publikasiDosen.setBulanPublikasi((Integer) (bulanPublikasi.getSelectedItem() == null ? null
				: bulanPublikasi.getSelectedItem().getValue()));
		publikasiDosen.setJudul1(judul1.getValue());
		publikasiDosen.setJudul2(judul2.getValue());
		publikasiDosen.setJudul3(judul3.getValue());
		publikasiDosen.setJudul4(judul4.getValue());
		publikasiDosen.setJudul5(judul5.getValue());
		publikasiDosen.setJumlahBiaya(jumlahBiaya.getValue() == null ? 0 : jumlahBiaya.getValue().longValue());
		publikasiDosen.setKodeAuthor((EpsbedPeranPenulisan) peranDalamPenelitian.getSelectedItem().getValue());
		publikasiDosen.setKodeJenisPenelitian((EpsbedJenisKaryaIlmiah) (jenisPenelitian.getSelectedItem() == null ? null
				: jenisPenelitian.getSelectedItem().getValue()));
		publikasiDosen.setKodeKegiatanMandiriKelompok((String) (mandiriKelompok.getSelectedItem() == null ? null
				: mandiriKelompok.getSelectedItem().getValue()));
		publikasiDosen.setKodeMediaPublikasi((EpsbedMediaPublikasi) (mediaPublikasi.getSelectedItem() == null ? null
				: mediaPublikasi.getSelectedItem().getValue()));
		publikasiDosen.setKodePembiayaan((EpsbedPembiayaanPenelitian) (pembiayaanPenelitian.getSelectedItem() == null
				? null : pembiayaanPenelitian.getSelectedItem().getValue()));
		publikasiDosen.setTahunPublikasi((Integer) (tahunPublikasi.getSelectedItem() == null ? null
				: tahunPublikasi.getSelectedItem().getValue()));
		publikasiDosen.setUrut((Integer) (noUrutJumlahKaryaIlmiah.getSelectedItem() == null ? null
				: noUrutJumlahKaryaIlmiah.getSelectedItem().getValue()));

		if (publikasiDosen.getId() != null) {
			publikasiDosenDao.update(publikasiDosen);
		} else {
			publikasiDosenDao.save(publikasiDosen);
		}
		return true;
	}
}
