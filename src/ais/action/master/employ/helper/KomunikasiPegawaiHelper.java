package ais.action.master.employ.helper;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.East;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.employ.KomunikasiPegawai;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyCkEditor;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class KomunikasiPegawaiHelper {

	private MyGrid gridKomunikasiPegawai;
	private boolean add = true;
	private boolean delete = true;
	private GeneralValueObject generalValueObject;
	@SuppressWarnings("rawtypes")
	private Class clazz;

	public KomunikasiPegawaiHelper(MyGrid gridKomunikasiPegawai) {
		this.gridKomunikasiPegawai = gridKomunikasiPegawai;
	}

	@SuppressWarnings("rawtypes")
	public Borderlayout initDetail(final GeneralValueObject generalValueObject,
			final Class clazz) throws Exception {
		this.generalValueObject = generalValueObject;
		this.clazz = clazz;

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, true);

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("30px");
		toolbar.setParent(north);

		MyButtonConfig fileupload = new MyButtonConfig("Buat Komentar", "/img/new.gif");
		fileupload.setVisible(KomunikasiPegawaiHelper.this.add);
		fileupload.setParent(toolbar);
		fileupload.setTooltiptext("Buat Komentar");

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				KomunikasiPegawai komunikasiPegawai = new KomunikasiPegawai();
				init(komunikasiPegawai, null);
			}
		};
		fileupload.addEventListener("onUpload", eventListener);

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		Common.clear(gridKomunikasiPegawai);
		gridKomunikasiPegawai.setParent(center);
		gridKomunikasiPegawai.setWidth("100%");
		gridKomunikasiPegawai.setHeight("100%");
		Columns columns = new Columns();
		columns.setParent(gridKomunikasiPegawai);

		MyColumnConfig column = new MyColumnConfig("");
		column.setParent(columns);
		column.setWidth("93%");

		column = new MyColumnConfig("");
		column.setParent(columns);

		loadDataDetail(generalValueObject, clazz);

		return borderlayout;
	}

	private void init(final KomunikasiPegawai komunikasiPegawai,
			final KomunikasiPegawai parent) throws Exception {
		final MyWindow window = new MyWindow("Tambah komentar", "none", true);
		window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage()
				.getFirstRoot());
		window.setHeight("97%");
		window.setWidth("97%");

		final Textbox nama = new Textbox(komunikasiPegawai.getNama());
		nama.setWidth("90%");
		nama.setRows(5);

		final MyCkEditor keterangan = new MyCkEditor();
		keterangan.setValue(komunikasiPegawai.getKeterangan());
		keterangan.setHeight("100%");
		keterangan.setWidth("100%");


		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(window);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		East east = new East();
		east.setWidth("70%");
		ais.ui.util.ZkCompat.setFlex(east, true);
		east.setParent(borderlayout);
		east.appendChild(keterangan);

		MyGrid grid = new MyGrid();grid.setWidth("100%");
		grid.setParent(center);

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("35%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Judul"));
		row.appendChild(nama);
		nama.setWidth("90%");

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
					MyMessageboxConfig.show("Mohon maaf, Judul komentar belum diisi. Langkah yang dapat dilakukan: (1) isi kolom Judul komentar pada form; (2) pastikan judul tidak kosong atau hanya spasi; (3) ulangi proses ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
							MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return;
				}
				if (keterangan.getValue().trim().equals("")) {
					MyMessageboxConfig.show("Mohon maaf, Isi komentar belum diisi. Langkah yang dapat dilakukan: (1) isi kolom Isi komentar dengan teks komentar Anda; (2) pastikan kolom tidak kosong; (3) ulangi proses ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
							MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return;
				}

				Rows rows = gridKomunikasiPegawai.getRows() == null ? new Rows()
						: gridKomunikasiPegawai.getRows();
				rows.setParent(gridKomunikasiPegawai);

				komunikasiPegawai.setKeterangan(keterangan.getValue());
				komunikasiPegawai.setNama(nama.getValue());
				komunikasiPegawai.setClazz(clazz.getName());
				komunikasiPegawai.setItem(generalValueObject.getId());
				komunikasiPegawai.setTbmuser(Common.getCurrentUser());
				komunikasiPegawai.setParent(parent);

				Session session = HibernateUtil.currentSession();
				session.saveOrUpdate(komunikasiPegawai);

				if (generalValueObject.getId() == null) {
					MyFormRow row = new MyFormRow();row.setValign("top");
					row.setParent(rows);
					try {
						initRow(row, komunikasiPegawai);
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e); 
					}
				} else {
					loadDataDetail(generalValueObject, clazz);
				}
				window.detach();
			}
		});
		save.setParent(toolbar);

		window.onModal();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void loadDataDetail(final GeneralValueObject generalValueObject,
			final Class clazz) throws Exception {

		Session session = HibernateUtil.currentSession();
		List<KomunikasiPegawai> komunikasiPegawais = generalValueObject == null
				|| generalValueObject.getId() == null ? new ArrayList<KomunikasiPegawai>()
				: session
						.createCriteria(KomunikasiPegawai.class)
						.add(Restrictions.eq("item", generalValueObject.getId()))
						.add(Restrictions.eq("clazz", clazz.getName()))
						.addOrder(Order.desc("id")).list();

		Rows rows = gridKomunikasiPegawai.getRows() == null ? new Rows()
				: gridKomunikasiPegawai.getRows();
		rows.setParent(gridKomunikasiPegawai);

		for (KomunikasiPegawai komunikasiPegawai : komunikasiPegawais) {
			MyFormRow row = new MyFormRow();row.setValign("top");
			row.setParent(rows);
			initRow(row, komunikasiPegawai);
		}

	}

	public void initRow(final Row row, final KomunikasiPegawai komunikasiPegawai)
			throws Exception {
		row.setValign("top");row.setAttribute("komunikasiPegawai", komunikasiPegawai);
		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				init(new KomunikasiPegawai(), komunikasiPegawai);
			}
		};

		Html a = new ais.ui.util.MyHtml(
				"<font style=\"font-size: x-small;\">"
						+ (komunikasiPegawai.getParent() == null ? ""
								: "<div style=\"width: 90%;border-color: black;background-color: rgba(169,169,169,0.4);border: thin;\"><u>Quote</u><br>"
										+ (komunikasiPegawai.getParent()
												.getNama() + "<hr>" + komunikasiPegawai
												.getParent().getKeterangan())
										+ "</div>")
						+ komunikasiPegawai.getNama() + "<hr>"
						+ komunikasiPegawai.getKeterangan() + "</font>");
		a.setParent(row);

		Hbox hbox = new Hbox();
		hbox.setParent(row);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Quote", "/img/upload.gif");
		button.setTooltiptext("Quote");
		button.setParent(hbox);
		button.addEventListener("onClick", eventListener);

		button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
		button.setTooltiptext("Hapus Data");
		button.setVisible(delete
				&& komunikasiPegawai.getTbmuser().getUserId()
						.equals(Common.getCurrentUser().getUserId()));
		button.setParent(hbox);

		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?",
						"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
						MyMessageboxConfig.QUESTION, new EventListener() {

							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {
									if (komunikasiPegawai.getId() != null) {
										Session session = HibernateUtil
												.currentSession();
										session.delete(komunikasiPegawai);
									}
	row.setVisible(false);row.detach();
								}

							}
						});

			}
		});
	}
}
