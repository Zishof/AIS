package ais.action.master.rab.helper;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.sql.rowset.serial.SerialBlob;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Fileupload;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.CommonPrivilages;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.file.FotoInformasiRab;
import ais.database.model.rab.InformasiRab;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyFileUploadConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;

public class InformasiRabPunyaFotoHelper {

	private MyGrid gridFotoGambar;
	private boolean add = false;
	private boolean delete = false;

	public InformasiRabPunyaFotoHelper(MyGrid gridFotoGambar) {
		this.gridFotoGambar = gridFotoGambar;
		add = CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
	}

	public Borderlayout initDetail(
			final InformasiRab informasiRab) throws Exception {
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, true);

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("30px");
		toolbar.setParent(north);

		Fileupload fileupload = new MyFileUploadConfig("Tambah File", "/img/new.gif");
		fileupload.setVisible(InformasiRabPunyaFotoHelper.this.add);
		fileupload.setParent(toolbar);
		fileupload.setTooltiptext("Tambah");

		EventListener eventListener = new EventListener() {

			@SuppressWarnings("deprecation")
			@Override
			public void onEvent(Event event) throws Exception {
				try {
					UploadEvent uploadEvent = (UploadEvent) event;
					if (uploadEvent != null) {

						Session streamingSession = StreamingHibernateUtil
								.getInstance().currentSession();

						FotoInformasiRab fotoInformasiRab = new FotoInformasiRab();
						fotoInformasiRab.setNama(uploadEvent
								.getMedia().getName());
						fotoInformasiRab.setKeterangan(uploadEvent
								.getMedia().getContentType());
						fotoInformasiRab
								.setInformasiRab(informasiRab
										.getId() == null ? new Random(
										Long.MIN_VALUE).nextLong()
										: informasiRab.getId());

						fotoInformasiRab.setFoto(new SerialBlob(uploadEvent.getMedia().getByteData()));

						streamingSession.getTransaction().begin();
						streamingSession.save(fotoInformasiRab);
						streamingSession.getTransaction().commit();

						StreamingHibernateUtil.getInstance().closeSession();

						Rows rows = gridFotoGambar.getRows() == null ? new Rows()
								: gridFotoGambar.getRows();
						rows.setParent(gridFotoGambar);
						Row row = new Row();row.setValign("top");
						row.setParent(rows);
						initRow(row, fotoInformasiRab);
					}
				} catch (Exception e) {
					StreamingHibernateUtil.getInstance().rollbackTransaction();
				}

			}
		};
		fileupload.addEventListener("onUpload", eventListener);

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		Common.clear(gridFotoGambar);
		gridFotoGambar.setParent(center);
		gridFotoGambar.setWidth("100%");
		gridFotoGambar.setHeight("100%");
		Columns columns = new Columns();
		columns.setParent(gridFotoGambar);

		MyColumnConfig column = new MyColumnConfig("Nama");
		column.setParent(columns);

		column = new MyColumnConfig("Jenis");
		column.setParent(columns);
		column.setWidth("20%");

		column = new MyColumnConfig("Tampil");
		column.setParent(columns);
		column.setWidth("10%");

		column = new MyColumnConfig("");
		column.setParent(columns);
		column.setWidth("20%");

		loadDataDetail(informasiRab);

		return borderlayout;
	}

	@SuppressWarnings("unchecked")
	private void loadDataDetail(
			final InformasiRab informasiRab) throws Exception {

		Session session = StreamingHibernateUtil.getInstance().currentSession();
		List<FotoInformasiRab> fotoInformasiRabs = informasiRab == null
				|| informasiRab.getId() == null ? new ArrayList<FotoInformasiRab>()
				: session
						.createCriteria(FotoInformasiRab.class)
						.add(Restrictions.eq("informasiRab",
								informasiRab.getId()))
						.addOrder(Order.desc("id")).list();

		Rows rows = gridFotoGambar.getRows() == null ? new Rows()
				: gridFotoGambar.getRows();
		rows.setParent(gridFotoGambar);

		for (FotoInformasiRab fotoInformasiRab : fotoInformasiRabs) {
			Row row = new Row();row.setValign("top");
			row.setParent(rows);
			initRow(row, fotoInformasiRab);
		}
		StreamingHibernateUtil.getInstance().closeSession();
	}

	public void initRow(final Row row,
			final FotoInformasiRab fotoInformasiRab)
			throws Exception {
		row.setValign("top");row.setAttribute("fotoInformasiRab", fotoInformasiRab);
		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				try {
					Session session = StreamingHibernateUtil.getInstance()
							.currentSession();

					FotoInformasiRab myfotoInformasiRab = (FotoInformasiRab) session
							.createCriteria(FotoInformasiRab.class)
							.add(Restrictions.idEq(fotoInformasiRab
									.getId())).uniqueResult();
					Filedownload.save(CommonMedia
							.getFileFotoLangsungOld(myfotoInformasiRab,false),
							myfotoInformasiRab.getKeterangan());

					StreamingHibernateUtil.getInstance().closeSession();
				} catch (Exception e) {
					StreamingHibernateUtil.getInstance().rollbackTransaction();
					Common.tampilErrorJikaAdmin(e); 
				}
			}
		};

		A a = new A(fotoInformasiRab.getNama());
		a.setParent(row);
		a.addEventListener("onClick", eventListener);

		a = new A(fotoInformasiRab.getKeterangan());
		a.setParent(row);
		a.addEventListener("onClick", eventListener);

		final MyCheckboxConfig checkbox = new MyCheckboxConfig();
		checkbox.setDisabled(add && delete);
		checkbox.setChecked(fotoInformasiRab.getDitampilkan());
		checkbox.setParent(row);row.setValign("top");row.setAttribute("checkbox", checkbox);
		checkbox.addEventListener("onCheck", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				fotoInformasiRab.setDitampilkan(checkbox.isChecked());
				if (fotoInformasiRab.getId() != null) {
					Session session = StreamingHibernateUtil.getInstance()
							.currentSession();
					session.getTransaction().begin();
					Common.refreshUpdate(session,(fotoInformasiRab));
					session.getTransaction().commit();
					StreamingHibernateUtil.getInstance().closeSession();
				}
				row.setValign("top");row.setAttribute("fotoInformasiRab",
						fotoInformasiRab);
			}
		});

		Hbox hbox = new Hbox();
		hbox.setParent(row);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/upload.gif");
		button.setTooltiptext("Download");
		button.setParent(hbox);
		button.addEventListener("onClick", eventListener);

		button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
		button.setTooltiptext("Hapus Data");
		button.setVisible(delete);
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
									if (fotoInformasiRab.getId() != null) {
										Session session = StreamingHibernateUtil
												.getInstance().currentSession();
										session.getTransaction().begin();
										session.delete(fotoInformasiRab);
										session.getTransaction().commit();
										StreamingHibernateUtil.getInstance()
												.closeSession();
									}
	row.setVisible(false);row.detach();
								}

							}
						});

			}
		});
	}

}
