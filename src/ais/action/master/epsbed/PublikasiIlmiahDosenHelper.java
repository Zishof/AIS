package ais.action.master.epsbed;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.A;
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
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.epsbed.EpsbedJenisKaryaIlmiah;
import ais.database.model.epsbed.EpsbedMediaPublikasi;
import ais.database.model.epsbed.EpsbedPembiayaanPenelitian;
import ais.database.model.epsbed.EpsbedPeranPenulisan;
import ais.database.model.epsbed.EpsbedPublikasiIlmiahDosen;

/**
 * Helper ZK untuk mengelola daftar publikasi ilmiah dosen ({@link EpsbedPublikasiIlmiahDosen}) pada
 * modul pelaporan EPSBED, dipasang pada layar detail dosen. Menyediakan tampilan daftar berpaging
 * (tahun/bulan, judul, jenis, media publikasi, tautan URL, tombol edit/hapus) dan form
 * tambah/ubah satu baris publikasi (jenis penelitian, media publikasi, peran penulisan,
 * mandiri/kelompok, tahun/bulan, pembiayaan, jumlah biaya, judul, URL). Komponen combobox referensi
 * (jenis penelitian, media publikasi, peran, pembiayaan, pilihan bulan 1-12, pilihan tahun 20 tahun
 * ke belakang) dibangun sekali di konstruktor dan dipakai ulang antar pemanggilan form.
 */
public class PublikasiIlmiahDosenHelper {

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
	// private EpsbedPublikasiIlmiahDosen epsbedPublikasiDosen;
	private Combobox peranDalamPenelitian;
	private Combobox jenisPenelitian;
	private Combobox pembiayaanPenelitian;
	private Decimalbox jumlahBiaya;
	private Textbox judul1;

	private EpsbedPublikasiIlmiahDosen publikasiDosen;
	MyToolbarbuttonConfig simpan = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
	MyToolbarbuttonConfig kembali = new MyToolbarbuttonConfig("Kembali", "/img/cancel.gif");
	private Textbox url;

	/** Membuat helper dan menyiapkan seluruh combobox referensi (jenis penelitian, media publikasi, peran, pembiayaan, mandiri/kelompok, bulan, tahun) yang dipakai ulang oleh form tambah/ubah. */
	public PublikasiIlmiahDosenHelper() {
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

	/** Renderer baris grid daftar publikasi: kolom tahun/bulan, judul, mandiri/kelompok, media publikasi, tautan URL (buka tab baru), dan tombol edit/hapus. */
	class PublikasiDosenRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row row, Object arg1) throws Exception {
			// TODO Auto-generated method stub
			final EpsbedPublikasiIlmiahDosen epsbedPublikasiDosen = (EpsbedPublikasiIlmiahDosen) arg1;
			new Label(epsbedPublikasiDosen.getTahunPublikasi() + "/" + epsbedPublikasiDosen.getBulanPublikasi())
					.setParent(row);
			new Label(epsbedPublikasiDosen.getJudul()).setParent(row);
			new Label(epsbedPublikasiDosen.getKodeKegiatanMandiriKelompok()).setParent(row);
			new Label(epsbedPublikasiDosen.getKodeMediaPublikasi() == null ? ""
					: epsbedPublikasiDosen.getKodeMediaPublikasi().getNama()).setParent(row);
			A a;
			(a = new A(epsbedPublikasiDosen.getUrl())).setParent(row);
			a.setHref(epsbedPublikasiDosen.getUrl());
			a.setTarget("_blank");

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
											Common.refreshDelete(publikasiDosen);
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

	/**
	 * Membangun panel daftar publikasi ilmiah untuk satu dosen: tombol tambah data di utara dan grid
	 * daftar berpaging di tengah.
	 *
	 * @param dosen dosen yang daftar publikasinya ditampilkan
	 * @return komponen {@link Borderlayout} siap dipasang ke layar pemanggil
	 */
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
				init(new EpsbedPublikasiIlmiahDosen());
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
		column.setWidth("25%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jenis");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Publikasi");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Alamat URL Publikasi");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");

		onSearchDefault(dosen);

		// window.setVisible(true);
		// try {
		// window.onModal();
		// } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/epsbed/PublikasiIlmiahDosenHelper.java:264");
		// // TODO Auto-generated catch block
		// Common.tampilErrorJikaAdmin(e); 
		// }

		return borderlayout;
	}

	@SuppressWarnings("unchecked")
	/** Memuat daftar publikasi ilmiah milik dosen yang diberikan (diurutkan tahun lalu bulan terbaru lebih dulu) dan merender hasilnya ke grid. */
	public void onSearchDefault(Dosen dosen) {

		Session session = HibernateUtil.currentSession();
		List<EpsbedPublikasiIlmiahDosen> epsbedPublikasiDosen = session.createCriteria(EpsbedPublikasiIlmiahDosen.class)
				.addOrder(Order.desc("tahunPublikasi")).addOrder(Order.desc("bulanPublikasi"))
				.add(Restrictions.eq("dosen", dosen)).setMaxResults(Common.MAX_RESULT).list();

		ListModel strset = new SimpleListModel(epsbedPublikasiDosen);

		grid.setRowRenderer(new PublikasiDosenRenderer());
		grid.setModelCheckMobile(strset);

		

	}

	/**
	 * Membangun form tambah/ubah satu baris publikasi ilmiah dosen, mem-prefill combobox/textbox
	 * dari data yang diberikan (kosong untuk data baru), dan memasang tombol simpan/kembali.
	 *
	 * @param epsbedPublikasiDosen data publikasi yang diedit (entitas baru untuk tambah data)
	 * @throws Exception diteruskan apa adanya dari kegagalan pembangunan komponen
	 */
	public void init(final EpsbedPublikasiIlmiahDosen epsbedPublikasiDosen) throws Exception {
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
		Common.selectComboItem(peranDalamPenelitian, epsbedPublikasiDosen.getKodeAuthor());

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
		row.appendChild(new ais.ui.util.MyLabelConfig("Judul"));
		row.appendChild(judul1 = new Textbox(epsbedPublikasiDosen.getJudul()));
		judul1.setRows(3);
		judul1.setWidth("97%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Alamat URL Publikasi"));
		row.appendChild(url = new Textbox(epsbedPublikasiDosen.getUrl()));
		url.setRows(2);
		url.setWidth("97%");

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

	/**
	 * Menyimpan data publikasi ilmiah dari form ke entitas {@link #publikasiDosen} (memuat ulang
	 * entitas terkelola dari database bila sudah punya id) dan mempersistennya.
	 *
	 * @param event event ZK pemicu penyimpanan (tombol simpan)
	 * @return selalu {@code true} pada implementasi saat ini
	 */
	public boolean save(Event event) {
		Session session = HibernateUtil.currentSession();
		if (publikasiDosen.getId() != null) {
			publikasiDosen = (EpsbedPublikasiIlmiahDosen) session.load(EpsbedPublikasiIlmiahDosen.class,
					publikasiDosen.getId());
		}
		publikasiDosen.setDosen(dosen);
		publikasiDosen.setBulanPublikasi((Integer) (bulanPublikasi.getSelectedItem() == null ? null
				: bulanPublikasi.getSelectedItem().getValue()));
		publikasiDosen.setJudul(judul1.getValue());

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
		publikasiDosen.setUrl(url.getValue());

		Common.refreshSaveOrUpdate(session, publikasiDosen);
		return true;
	}
}
