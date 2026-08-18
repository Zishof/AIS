package ais.action.master.library.helper;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Fileupload;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Image;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;

import ais.action.master.library.util.LibraryUtil;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.file.FotoImagePerHalamanItem;
import ais.database.model.library.Item;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyFileUploadConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class FotoImagePerHalamanItemHelper {

	private MyGrid gridFotoImagePerHalamanItem;
	private boolean add = false;
	private boolean delete = false;

	private Intbox halamanMulai = new Intbox(0);
	private Intbox halamanSampai = new Intbox(10);
	private Item item;

	public FotoImagePerHalamanItemHelper(MyGrid gridFotoImagePerHalamanItem) {
		this.gridFotoImagePerHalamanItem = gridFotoImagePerHalamanItem;
		add = CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
	}

	public Borderlayout initDetail(final Item item) throws Exception {
		this.item = item;
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, true);

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("30px");
		toolbar.setParent(north);

		Fileupload fileupload = new MyFileUploadConfig("Tambah Halaman", "/img/new.gif");
		fileupload.setVisible(FotoImagePerHalamanItemHelper.this.add);
		fileupload.setParent(toolbar);
		fileupload.setTooltiptext("Tambah");

		EventListener eventListener = new EventListener() {

			@SuppressWarnings("deprecation")
			@Override
			public void onEvent(Event event) throws Exception {
				try {
					UploadEvent uploadEvent = (UploadEvent) event;
					if (uploadEvent != null) {

						Clients.showBusy("Loading...");

						Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();

						final FotoImagePerHalamanItem fotoImagePerHalamanItem = new FotoImagePerHalamanItem();
						fotoImagePerHalamanItem.setNama(uploadEvent.getMedia().getName());
						fotoImagePerHalamanItem.setKeterangan(uploadEvent.getMedia().getContentType());
						fotoImagePerHalamanItem
								.setItem(item.getId() == null ? new Random(Long.MIN_VALUE).nextLong() : item.getId());

						fotoImagePerHalamanItem.setFoto(new javax.sql.rowset.serial.SerialBlob(uploadEvent.getMedia().getByteData()));

						streamingSession.getTransaction().begin();
						streamingSession.save(fotoImagePerHalamanItem);
						streamingSession.getTransaction().commit();

						StreamingHibernateUtil.getInstance().closeSession();

						final Timer timer = new Timer(500);
						timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());

						timer.addEventListener("onTimer", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {

								Clients.clearBusy();

								final MyWindow window = new MyWindow("Masukkan halaman", "none", false);
								window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());

								window.setWidth("70%");
								window.setHeight("97%");

								Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
								borderlayout.setParent(window);

								North north = new North();
								north.setParent(borderlayout);
								ais.ui.util.ZkCompat.setFlex(north, true);
								// north.setHeight("100px");

								MyGrid grid = new MyGrid();
								grid.setWidth("100%");
								grid.setParent(north);
								grid.setWidth("100%");
								grid.setHeight("100%");

								Rows rows = new Rows();
								rows.setParent(grid);
								Row row = new Row();row.setValign("top");
								row.setParent(rows);
								row.appendChild(new ais.ui.util.MyLabelConfig("Dokumen ini masuk halaman ke ?"));
								final Textbox halaman = new Textbox();
								row.appendChild(halaman);

								row = new Row();
								ais.ui.util.ZkCompat.setSpans(row, "2");
								row.setParent(rows);

								MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
								save.setTooltiptext("Simpan");
								save.addEventListener("onClick", new EventListener() {
									@Override
									public void onEvent(Event event) throws Exception {

										if (halaman.getValue().trim().equals("")) {
											MyMessageboxConfig.show("Halaman harus diisi", "Peringatan",
													MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
											return;
										}

										fotoImagePerHalamanItem.setHalaman(halaman.getValue().trim().toUpperCase());

										Session streamingSession = StreamingHibernateUtil.getInstance()
												.currentSession();
										streamingSession.getTransaction().begin();
										streamingSession.update(fotoImagePerHalamanItem);
										streamingSession.getTransaction().commit();

										StreamingHibernateUtil.getInstance().closeSession();

										window.detach();

										LibraryUtil.convertLampiranToText(fotoImagePerHalamanItem);

										loadDataDetail(item);

										if (FotoImagePerHalamanItemHelper.this.item != null
												&& FotoImagePerHalamanItemHelper.this.item.getId() != null) {
											FotoImagePerHalamanItemHelper.this.item.populateScanLinks();
											Common.refreshUpdate(HibernateUtil.currentSession(),
													FotoImagePerHalamanItemHelper.this.item);
										}
									}
								});

								row.appendChild(save);

								Center center = new Center();
								center.setParent(borderlayout);
								ais.ui.util.ZkCompat.setFlex(center, true);

								Image image = new Image(CommonMedia.getImageItemPerHalaman(item.getId(),
										fotoImagePerHalamanItem.getId(), fotoImagePerHalamanItem.getHalaman(), null,
										null, false));
								image.setWidth("100%");
								image.setParent(row);

								center.appendChild(image);

								window.onModal();

								timer.detach();

							}
						});

						timer.start();

					}
				} catch (Exception e) {
					StreamingHibernateUtil.getInstance().rollbackTransaction();
				}

			}
		};
		fileupload.addEventListener("onUpload", eventListener);

		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Tampilkan mulai halaman :")));
		toolbar.appendChild(halamanMulai);
		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("s.d")));
		toolbar.appendChild(halamanSampai);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				loadDataDetail(item);
			}
		});
		button.setParent(toolbar);

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		Common.clear(gridFotoImagePerHalamanItem);
		gridFotoImagePerHalamanItem.setParent(center);
		gridFotoImagePerHalamanItem.setWidth("100%");
		gridFotoImagePerHalamanItem.setHeight("100%");
		Columns columns = new Columns();
		columns.setParent(gridFotoImagePerHalamanItem);

		MyColumnConfig column = new MyColumnConfig("Tampilan");
		column.setParent(columns);
		column.setWidth("250px");

		column = new MyColumnConfig("Jenis");
		column.setParent(columns);

		column = new MyColumnConfig("Tampil");
		column.setParent(columns);
		column.setWidth("10%");

		column = new MyColumnConfig("Halaman");
		column.setParent(columns);
		column.setWidth("10%");

		column = new MyColumnConfig("");
		column.setParent(columns);
		column.setWidth("10%");

		loadDataDetail(item);

		return borderlayout;
	}

	@SuppressWarnings("unchecked")
	private void loadDataDetail(final Item item) throws Exception {

		Session session = StreamingHibernateUtil.getInstance().currentSession();
		List<FotoImagePerHalamanItem> fotoImagePerHalamanItems = item == null || item.getId() == null
				? new ArrayList<FotoImagePerHalamanItem>()
				: session.createCriteria(FotoImagePerHalamanItem.class).add(Restrictions.eq("item", item.getId()))
						.add(Restrictions.between("halamanIndex",
								new Double(halamanMulai.getValue() == null ? 0 : halamanMulai.getValue()),
								new Double(halamanSampai.getValue() == null ? 10 : halamanSampai.getValue())))
						.addOrder(Order.asc("halamanIndex")).list();

		Rows rows = gridFotoImagePerHalamanItem.getRows() == null ? new Rows() : gridFotoImagePerHalamanItem.getRows();
		Common.clear(rows);
		rows.setParent(gridFotoImagePerHalamanItem);

		for (FotoImagePerHalamanItem fotoImagePerHalamanItem : fotoImagePerHalamanItems) {
			Row row = new Row();row.setValign("top");
			row.setParent(rows);
			initRow(row, fotoImagePerHalamanItem);
		}
		StreamingHibernateUtil.getInstance().closeSession();

	}

	public void initRow(final Row row, final FotoImagePerHalamanItem fotoImagePerHalamanItem) throws Exception {
		row.setValign("top");row.setAttribute("fotoImagePerHalamanItem", fotoImagePerHalamanItem);
		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				try {
					Session session = StreamingHibernateUtil.getInstance().currentSession();

					FotoImagePerHalamanItem myfotoImagePerHalamanItem = (FotoImagePerHalamanItem) session
							.createCriteria(FotoImagePerHalamanItem.class)
							.add(Restrictions.idEq(fotoImagePerHalamanItem.getId())).uniqueResult();
					Filedownload.save(myfotoImagePerHalamanItem.ambilFile(), myfotoImagePerHalamanItem.getKeterangan());

					StreamingHibernateUtil.getInstance().closeSession();
				} catch (Exception e) {
					StreamingHibernateUtil.getInstance().rollbackTransaction();
					Common.tampilErrorJikaAdmin(e);
				}
			}
		};

		// A a = new A(fotoImagePerHalamanItem.getNama());
		// a.setParent(row);
		// a.addEventListener("onClick", eventListener);

		Long item = fotoImagePerHalamanItem.getItem();

		Image image = new Image(CommonMedia.getImageItemPerHalaman(item == null ? -1L : item,
				fotoImagePerHalamanItem.getId(), fotoImagePerHalamanItem.getHalaman(), null, null, false));
		image.setWidth("100%");
		image.setParent(row);

		A a = new A(fotoImagePerHalamanItem.getKeterangan());
		a.setParent(row);
		a.addEventListener("onClick", eventListener);

		final MyCheckboxConfig checkbox = new MyCheckboxConfig();
		checkbox.setChecked(fotoImagePerHalamanItem.getDitampilkan());
		checkbox.setParent(row);
		row.setValign("top");row.setAttribute("checkbox", checkbox);
		checkbox.addEventListener("onCheck", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				fotoImagePerHalamanItem.setDitampilkan(checkbox.isChecked());
				if (fotoImagePerHalamanItem.getId() != null) {
					Session session = StreamingHibernateUtil.getInstance().currentSession();
					session.getTransaction().begin();
					Common.refreshUpdate(session, (fotoImagePerHalamanItem));
					session.getTransaction().commit();
					StreamingHibernateUtil.getInstance().closeSession();
				}
				row.setValign("top");row.setAttribute("fotoImagePerHalamanItem", fotoImagePerHalamanItem);
			}
		});

		final Textbox halaman = new Textbox(fotoImagePerHalamanItem.getHalaman());
		halaman.setParent(row);
		halaman.setWidth("90%");
		halaman.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				fotoImagePerHalamanItem.setHalaman(halaman.getValue().toUpperCase().trim());
				if (fotoImagePerHalamanItem.getId() != null) {
					Session session = StreamingHibernateUtil.getInstance().currentSession();
					session.getTransaction().begin();
					Common.refreshUpdate(session, (fotoImagePerHalamanItem));
					session.getTransaction().commit();
					StreamingHibernateUtil.getInstance().closeSession();

					if (FotoImagePerHalamanItemHelper.this.item != null
							&& FotoImagePerHalamanItemHelper.this.item.getId() != null) {
						FotoImagePerHalamanItemHelper.this.item.populateScanLinks();
						Common.refreshUpdate(HibernateUtil.currentSession(), FotoImagePerHalamanItemHelper.this.item);
					}

				}
				row.setValign("top");row.setAttribute("fotoImagePerHalamanItem", fotoImagePerHalamanItem);
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

				MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
						MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {

							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {
									if (fotoImagePerHalamanItem.getId() != null) {
										Session session = StreamingHibernateUtil.getInstance().currentSession();
										session.getTransaction().begin();
										session.delete(fotoImagePerHalamanItem);
										session.getTransaction().commit();
										StreamingHibernateUtil.getInstance().closeSession();

										if (FotoImagePerHalamanItemHelper.this.item != null
												&& FotoImagePerHalamanItemHelper.this.item.getId() != null) {
											FotoImagePerHalamanItemHelper.this.item.populateScanLinks();
											Common.refreshUpdate(HibernateUtil.currentSession(),
													FotoImagePerHalamanItemHelper.this.item);
										}

									}
	row.setVisible(false);row.detach();
								}

							}
						});

			}
		});
	}

}
