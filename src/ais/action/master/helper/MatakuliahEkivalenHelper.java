package ais.action.master.helper;
import ais.common.PesanFormalHelper;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import ais.ui.util.MyColumnConfig;
import org.zkoss.zul.Columns;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import ais.ui.util.MyMessageboxConfig;
import org.zkoss.zul.Panelchildren;
import ais.ui.util.MyRadioConfig;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Toolbar;
import ais.ui.util.MyToolbarbuttonConfig;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.common.listener.DataLoader;
import ais.database.dao.DaoFactory;
import ais.database.dao.MatakuliahEkivalenDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Detailperkuliahan;
import ais.database.model.Mahasiswa;
import ais.database.model.Matakuliah;
import ais.database.model.MatakuliahEkivalen;
import ais.ui.util.MyPanel;

public class MatakuliahEkivalenHelper implements DataLoader {

	private MyGrid grid;
	private Matakuliah matakuliah;
	private boolean editable = true;
	private Mahasiswa mahasiswa;
	private Integer semester;
	private EventListener eventListener;

	public MatakuliahEkivalenHelper(Mahasiswa mahasiswa, Integer semester, EventListener eventListener) {

		this.mahasiswa = mahasiswa;
		this.semester = semester;
		this.eventListener = eventListener;
	}

	public MatakuliahEkivalenHelper(boolean editable, Mahasiswa mahasiswa, Integer semester,
			EventListener eventListener) {
		this.editable = editable;
		this.mahasiswa = mahasiswa;
		this.semester = semester;
		this.eventListener = eventListener;

	}

	class DetailMatakuliahRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row row, Object data) throws Exception {
			row.setValign("top");
			final MatakuliahEkivalen matakuliahEkivalen = (MatakuliahEkivalen) data;

			final Matakuliah matakuliah = matakuliahEkivalen.getMatakuliahEkivalen();

			if (mahasiswa == null || semester == null || eventListener == null) {
				new Label().setParent(row);
			} else {
				final MyRadioConfig radio = new MyRadioConfig();
				radio.setParent(row);
				Session session = HibernateUtil.currentSession();
				final Integer matakuliahAsli = ((Number) session.createCriteria(Detailperkuliahan.class)
						.add(Restrictions.isNull("ikutiPerkuliahan")).add(Restrictions.eq("mahasiswa", mahasiswa))
						.add(Restrictions.eq("matakuliahAsliSebelumKonversi", matakuliah))
						.add(Restrictions.eq("semester", semester)).setProjection(Projections.rowCount())
						.setMaxResults(1).uniqueResult()).intValue();

				radio.setChecked(!matakuliahAsli.equals(0));
				radio.addEventListener("onCheck", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						eventListener.onEvent(
								new Event(radio.isChecked() + "", radio, matakuliahEkivalen.getMatakuliahEkivalen()));

					}
				});
			}

			new Label(matakuliah.getKode()).setParent(row);
			new Label(matakuliah.getNama()).setParent(row);
			new Label(matakuliah.getSks() + "").setParent(row);
			new Label(matakuliah.getJurusan() == null ? "" : matakuliah.getJurusan().getNama()).setParent(row);
			new Label(matakuliah.getJenisMatakuliah()).setParent(row);

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
											MatakuliahEkivalenDao matakuliahDao = DaoFactory.getInstance()
													.getMatakuliahEkivalenDao();
											// matakuliahDao.beginTransaction();
											matakuliahDao.delete((matakuliahEkivalen));
											// matakuliahDao.commitTransaction();

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
		List<MatakuliahEkivalen> matakuliahEkivalen = ConstantValues
				.simpleList(session.createCriteria(MatakuliahEkivalen.class).addOrder(Order.asc("id"))
						.add(Restrictions.eq("matakuliah", matakuliah)), MatakuliahEkivalen.class);

		ListModel strset = new SimpleListModel(matakuliahEkivalen);
		grid.setRowRenderer(new DetailMatakuliahRenderer());
		grid.setModelCheckMobile(strset);

	}

	public void display(final Matakuliah matakuliah, final Component component) {
		this.matakuliah = matakuliah;
		Common.clear(component);

		MyPanel panel = new MyPanel();
		panel.setParent(component);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Daftar matakuliah Ekivalen");
		panel.setBorder("none");
		panel.setStyle("border:0px;");

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(panel);
		toolbar.setVisible(editable);
		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Ambil Matakuliah Ekivalen", "/img/new.gif");
		button.addEventListener("onClick", new EventListener() {

			private AmbilDataMatakuliahEkivalenHelper ambilDataMatakuliahHelper = new AmbilDataMatakuliahEkivalenHelper();

			@Override
			public void onEvent(Event event) throws Exception {
				ambilDataMatakuliahHelper.display(matakuliah, MatakuliahEkivalenHelper.this);
			}

		});
		button.setParent(toolbar);

		Panelchildren panelchildren = new Panelchildren();
		panelchildren.setParent(panel);

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(10);
		grid.getPagingChild().setMold("os");
		grid.setParent(panelchildren);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("40px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Kode");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");
		column.setWidth("30%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("SKS");
		column.setWidth("25%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jurusan");
		column.setWidth("25%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Keberadaan");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("10%");

		loadData(null);

	}

}
