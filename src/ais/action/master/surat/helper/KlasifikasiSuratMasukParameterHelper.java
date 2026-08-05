package ais.action.master.surat.helper;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.surat.KlasifikasiSuratMasuk;
import ais.database.model.surat.KlasifikasiSuratMasukParemeter;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class KlasifikasiSuratMasukParameterHelper implements DataCriteria, DataSearchDefault {

	private MyGrid gridParemeter;
	private boolean add = false;
	private boolean delete = false;
	private KlasifikasiSuratMasuk klasifikasiSuratMasuk;

	public KlasifikasiSuratMasukParameterHelper(MyGrid gridParemeter) {
		this.gridParemeter = gridParemeter;
		add = CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
	}

	public Borderlayout initDetail(final KlasifikasiSuratMasuk klasifikasiSuratMasuk) throws Exception {
		this.klasifikasiSuratMasuk = klasifikasiSuratMasuk;
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, true);

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("30px");
		toolbar.setParent(north);

		MyToolbarbuttonConfig add = new MyToolbarbuttonConfig("Tambah Paremeter", "/img/new.gif");
		add.setVisible(KlasifikasiSuratMasukParameterHelper.this.add);
		add.setParent(toolbar);
		add.setTooltiptext("Tambah");
		add.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				final MyWindow window = new MyWindow("KlasifikasiSuratMasuk Batch", "none", true);
				window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				window.setHeight("140px");
				window.setWidth("550px");

				final Textbox nama = new Textbox("");
				final Textbox keterangan = new Textbox("");

				Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
				borderlayout.setParent(window);
				Center center = new Center();
				center.setParent(borderlayout);
				ais.ui.util.ZkCompat.setFlex(center, true);

				MyGrid grid = new MyGrid();
				grid.setWidth("100%");
				grid.setParent(center);

				Columns columns = new Columns();
				columns.setParent(grid);

				MyColumnConfig column = new MyColumnConfig();
				column.setParent(columns);
				column.setWidth("20%");

				column = new MyColumnConfig();
				column.setParent(columns);
				column.setWidth("90%");

				Rows rows = new Rows();
				rows.setParent(grid);

				MyFormRow row = new MyFormRow();row.setValign("top");
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Nama Parameter"));
				row.appendChild(nama);
				nama.setWidth("90%");

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
				row.appendChild(keterangan);
				keterangan.setWidth("90%");

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
						window.detach();
					}
				});
				cancel.setParent(toolbar);
				MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
				save.setTooltiptext("Simpan");
				save.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						if (nama.getValue().trim().equals("")) {
							MyMessageboxConfig.show("Mohon maaf, Nama Parameter Klasifikasi Surat Masuk belum diisi. Langkah yang dapat dilakukan: (1) klik kolom Nama Parameter; (2) isikan nama parameter secara lengkap; (3) klik tombol Simpan kembali. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
									MyMessageboxConfig.EXCLAMATION);
							return;
						}

						Rows rows = gridParemeter.getRows() == null ? new Rows() : gridParemeter.getRows();
						rows.setParent(gridParemeter);

						KlasifikasiSuratMasukParemeter klasifikasiSuratMasukParemeter = new KlasifikasiSuratMasukParemeter();
						klasifikasiSuratMasukParemeter.setNama(nama.getValue().trim());
						klasifikasiSuratMasukParemeter.setKeterangan(keterangan.getValue().trim());

						if (klasifikasiSuratMasuk.getId() != null) {
							Session session = HibernateUtil.currentSession();
							session.save(klasifikasiSuratMasukParemeter);
						}

						MyFormRow row = new MyFormRow();row.setValign("top");
						row.setParent(rows);
						try {
							initRow(row, klasifikasiSuratMasukParemeter);
						} catch (Exception e) {
							Common.tampilErrorJikaAdmin(e);
						}

						window.detach();
					}
				});
				save.setParent(toolbar);

				window.onModal();
			}
		});

		String[] contents = new String[] { "id", "nama", "key", "nilai", "tipe", "keterangan" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(KlasifikasiSuratMasukParemeter.class, this,
				contents);
		toolbar.appendChild(cetakToolbarbutton);

		HashMap<String, Object> nilai = null;
		Criterion idCrit = null;
		if (klasifikasiSuratMasuk != null) {
			idCrit = Restrictions.eq("klasifikasiSuratMasuk", klasifikasiSuratMasuk);
			nilai = new HashMap<String, Object>();
			nilai.put("klasifikasiSuratMasuk", klasifikasiSuratMasuk);
		}

		MyToolbarbuttonConfig upload = Common.uploadData(this, KlasifikasiSuratMasukParemeter.class, null, idCrit,
				nilai, contents);
		upload.setVisible(add.isVisible());
		toolbar.appendChild(upload);

		MyToolbarbuttonConfig cari = new MyToolbarbuttonConfig("Refresh", "/img/Button-Refresh-icon.png");
		cari.setParent(toolbar);
		cari.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadDataDetail();
			}
		});

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		Common.clear(gridParemeter);
		gridParemeter.setParent(center);
		gridParemeter.setWidth("100%");
		gridParemeter.setHeight("100%");
		Columns columns = new Columns();
		columns.setParent(gridParemeter);

		MyColumnConfig column = new MyColumnConfig("Paremeter");
		column.setParent(columns);

		column = new MyColumnConfig("Key");
		column.setParent(columns);

		column = new MyColumnConfig("Nilai Default");
		column.setParent(columns);

		column = new MyColumnConfig("Tipe");
		column.setParent(columns);

		column = new MyColumnConfig("Hapus");
		column.setParent(columns);
		column.setWidth("10%");

		loadDataDetail();

		return borderlayout;
	}

	@SuppressWarnings("unchecked")
	private void loadDataDetail() {

		List<KlasifikasiSuratMasukParemeter> klasifikasiSuratMasukParemeters = klasifikasiSuratMasuk == null
				|| klasifikasiSuratMasuk.getId() == null
						? new ArrayList<KlasifikasiSuratMasukParemeter>()
						: HibernateUtil.currentSession().createCriteria(KlasifikasiSuratMasukParemeter.class)
								.addOrder(Order.desc("id"))
								.add(Restrictions.eq("klasifikasiSuratMasuk", klasifikasiSuratMasuk)).list();

		Rows rows = gridParemeter.getRows() == null ? new Rows() : gridParemeter.getRows();
		Common.clear(rows);
		rows.setParent(gridParemeter);

		for (KlasifikasiSuratMasukParemeter klasifikasiSuratMasukParemeter : klasifikasiSuratMasukParemeters) {
			MyFormRow row = new MyFormRow();row.setValign("top");
			row.setParent(rows);
			try {
				initRow(row, klasifikasiSuratMasukParemeter);
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		}
	}

	public void initRow(final Row row, final KlasifikasiSuratMasukParemeter klasifikasiSuratMasukParemeter)
			throws Exception {
		row.setValign("top");row.setAttribute("klasifikasiSuratMasukParemeter", klasifikasiSuratMasukParemeter);

		new Label(klasifikasiSuratMasukParemeter.getNama()).setParent(row);

		new Label(klasifikasiSuratMasukParemeter.getKey()).setParent(row);

		final Textbox nilai = new Textbox(klasifikasiSuratMasukParemeter.getNilai());
		nilai.setWidth("90%");
		nilai.setParent(row);
		nilai.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				klasifikasiSuratMasukParemeter.setNilai(nilai.getValue().trim());
				row.setValign("top");row.setAttribute("klasifikasiSuratMasukParemeter", klasifikasiSuratMasukParemeter);
				if (klasifikasiSuratMasukParemeter.getId() != null) {
					Session session = HibernateUtil.currentSession();
					session.update(klasifikasiSuratMasukParemeter);
				}
			}
		});

		final Combobox tipe = new Combobox();
		tipe.setWidth("90%");
		tipe.setParent(row);
		MyComboitemConfig comboitem = new MyComboitemConfig(String.class.getSimpleName());
		comboitem.setValue(String.class.getName());
		tipe.appendChild(comboitem);

		comboitem = new MyComboitemConfig(Integer.class.getSimpleName());
		comboitem.setValue(Integer.class.getName());
		tipe.appendChild(comboitem);

		comboitem = new MyComboitemConfig(Double.class.getSimpleName());
		comboitem.setValue(Double.class.getName());
		tipe.appendChild(comboitem);

		comboitem = new MyComboitemConfig(Date.class.getSimpleName());
		comboitem.setValue(Date.class.getName());
		tipe.appendChild(comboitem);

		Common.selectComboItem(tipe, klasifikasiSuratMasukParemeter.getTipe());

		tipe.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				klasifikasiSuratMasukParemeter
						.setTipe((String) (tipe.getSelectedItem() == null ? null : tipe.getSelectedItem().getValue()));
				row.setValign("top");row.setAttribute("klasifikasiSuratMasukParemeter", klasifikasiSuratMasukParemeter);
				if (klasifikasiSuratMasukParemeter.getId() != null) {
					Session session = HibernateUtil.currentSession();
					session.update(klasifikasiSuratMasukParemeter);
				}
			}
		});

		// new Label(klasifikasiSuratMasukParemeter.getKeterangan())
		// .setParent(row);

		Hbox hbox = new Hbox();
		hbox.setParent(row);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
		button.setTooltiptext("Hapus Data");
		button.setVisible(delete);
		button.setParent(hbox);

		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
						MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {

							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {
									if (klasifikasiSuratMasukParemeter.getId() != null) {
										Session session = HibernateUtil.currentSession();
										session.delete(klasifikasiSuratMasukParemeter);
									}
	row.setVisible(false);row.detach();
								}

							}
						});

			}
		});
	}

	@Override
	public Object initCriteria(boolean order) {
		// TODO Auto-generated method stub
		return HibernateUtil.currentSession().createCriteria(KlasifikasiSuratMasukParemeter.class)
				.addOrder(Order.desc("id")).add(Restrictions.eq("klasifikasiSuratMasuk", klasifikasiSuratMasuk));
	}

	@Override
	public void onSearchDefault(Event event) {
		loadDataDetail();
	}

}
