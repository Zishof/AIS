package ais.action.master.helper;
import ais.common.PesanFormalHelper;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import ais.ui.util.MyColumnConfig;
import org.zkoss.zul.Columns;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import ais.ui.util.MyMessageboxConfig;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

import ais.common.Common;
import ais.common.listener.DataLoader;
import ais.database.dao.DaoFactory;
import ais.database.dao.MahasiswaDapatBeasiswaDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Beasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.MahasiswaDapatBeasiswa;

public class BeasiswaHelper implements DataLoader {

	private MyGrid grid;
	private Beasiswa beasiswa;
	private Textbox nim;
	private Textbox nama;

	class DetailBeasiswaRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");
			final MahasiswaDapatBeasiswa mahasiswaDapatBeasiswa = (MahasiswaDapatBeasiswa) data;

			Mahasiswa mahasiswa = mahasiswaDapatBeasiswa.getMahasiswa();

			new Label(mahasiswa.getNim()).setParent(row);
			new Label(mahasiswa.getNama()).setParent(row);
			new Label(mahasiswa.getJurusan() == null ? "" : mahasiswa.getJurusan().getNama()).setParent(row);
			new Label(mahasiswa.getJurusan() == null ? ""
					: mahasiswa.getJurusan().getFakultas() == null ? ""
							: mahasiswa.getJurusan().getFakultas().getNama()).setParent(row);

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
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
											MahasiswaDapatBeasiswaDao beasiswaDao = DaoFactory.getInstance()
													.getMahasiswaDapatBeasiswaDao();
											// beasiswaDao.beginTransaction();
											beasiswaDao.delete((mahasiswaDapatBeasiswa));
											// beasiswaDao.commitTransaction();

											loadData(null);

										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											PesanFormalHelper.tampilkanGagalException("Menghapus data", "Data yang Bapak/Ibu coba hapus kemungkinan besar masih memiliki keterkaitan/relasi dengan data lain pada tabel terkait (misalnya digunakan sebagai referensi oleh transaksi, detail, atau riwayat lain), sehingga sistem basis data menolak proses penghapusan ini demi menjaga integritas data secara keseluruhan.", e, new String[]{"Periksa kembali apakah data ini masih digunakan atau direferensikan oleh data lain yang berelasi.", "Hapus atau lepaskan terlebih dahulu keterkaitan/relasi data tersebut sebelum mencoba menghapus data ini kembali.", "Jika Bapak/Ibu yakin data ini seharusnya sudah tidak digunakan lagi, hubungi Administrator untuk pengecekan lebih lanjut."});
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

	@SuppressWarnings("unchecked")
	public void loadData(Object value) {
		Session session = HibernateUtil.currentSession();
		List<MahasiswaDapatBeasiswa> mahasiswaDapatBeasiswa = session.createCriteria(MahasiswaDapatBeasiswa.class)
				.addOrder(Order.asc("id")).add(Restrictions.eq("beasiswa", beasiswa))

				.createAlias("mahasiswa", "mahasiswa").addOrder(Order.asc("mahasiswa.nim"))
				.add(Restrictions.ilike("mahasiswa.nim", nim.getValue().trim(), MatchMode.ANYWHERE))
				.add(Restrictions.ilike("mahasiswa.nama", nama.getValue().trim(), MatchMode.ANYWHERE)).list();

		ListModel strset = new SimpleListModel(mahasiswaDapatBeasiswa);
		grid.setRowRenderer(new DetailBeasiswaRenderer());
		grid.setModelCheckMobile(strset);

	}

	private DataLoader getDataloader() {
		return this;
	}

	public void displayPrasyaratBeasiswa(final Beasiswa beasiswa, final Component component, final MyWindow window) {
		this.beasiswa = beasiswa;
		Common.clear(component);

		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(component);
		panel.setWidth("100%");
		panel.setHeight("430px");
		panel.setTitle("Daftar mahasiswa yang mendapatkan beasiswa");
		panel.setBorder("none");
		panel.setStyle("border:0px;");

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(panel);
		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("NIM : ")));
		toolbar.appendChild(nim = new Textbox());
		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Nama : ")));
		toolbar.appendChild(nama = new Textbox());
		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}

		});
		button.setParent(toolbar);
		button = new MyToolbarbuttonConfig("Tambah Data", "/img/new.gif");
		button.addEventListener("onClick", new EventListener() {

			private AmbilDataMahasiswaBeasiswaHelper ambilDataBeasiswaHelper = new AmbilDataMahasiswaBeasiswaHelper();

			@Override
			public void onEvent(Event event) throws Exception {
				ambilDataBeasiswaHelper.display(beasiswa, getDataloader(), window);
			}

		});
		button.setParent(toolbar);

		Panelchildren panelchildren = new Panelchildren();
		panelchildren.setParent(panel);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(panelchildren);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(10);grid.getPagingChild().setMold("os");
		grid.setParent(center);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Kode");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");
		column.setWidth("30%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jurusan");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Fakultas");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("10%");

		loadData(null);
		// borderlayout.setParent(component);

	}

}
