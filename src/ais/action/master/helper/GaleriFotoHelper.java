package ais.action.master.helper;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Fileupload;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Image;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Space;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.CommonPrivilages;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.GaleriFoto;
import ais.database.model.file.GaleriFotoImage;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyCkEditor;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyFileUploadConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyIntbox;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class GaleriFotoHelper {

	private MyGrid gridGaleriFotoImage;
	private boolean add = false;
	private boolean delete = false;

	private List<Long> idsRand;

	private Intbox halamanMulai = new Intbox(0);
	private Intbox halamanSampai = new Intbox(1000);

	public GaleriFotoHelper(MyGrid gridGaleriFotoImage) {
		this.gridGaleriFotoImage = gridGaleriFotoImage;
		add = CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
	}

	public Borderlayout initDetail(final GaleriFoto galeriFoto) throws Exception {
		idsRand = new ArrayList<Long>();
		if (galeriFoto != null && galeriFoto.getId() != null) {
			idsRand.add(galeriFoto.getId());
		}
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, true);

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("30px");
		toolbar.setParent(north);

		Fileupload fileupload = new MyFileUploadConfig("Tambah Gambar", "/img/new.gif");
		fileupload.setVisible(GaleriFotoHelper.this.add);
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

						final GaleriFotoImage galeriFotoImage = new GaleriFotoImage();
						galeriFotoImage.setNama(uploadEvent.getMedia().getName());
						galeriFotoImage.setKeterangan(uploadEvent.getMedia().getContentType());
						galeriFotoImage
								.setGaleriFoto(galeriFoto.getId() == null ? -Common.randLong() : galeriFoto.getId());

						galeriFotoImage.setFoto(Common.getBlobFromMedia(uploadEvent.getMedia()));

						streamingSession.getTransaction().begin();
						streamingSession.save(galeriFotoImage);
						streamingSession.getTransaction().commit();

						StreamingHibernateUtil.getInstance().closeSession();

						idsRand.add(galeriFotoImage.getGaleriFoto());

						final Timer timer = new Timer(500);
						timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());

						timer.addEventListener("onTimer", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {

								Clients.clearBusy();

								int fotoKe = 1;
								Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();
								Number m = (Number) streamingSession.createCriteria(GaleriFotoImage.class)
										.add(idsRand.isEmpty() ? Restrictions.sqlRestriction("false")
												: Restrictions.in("galeriFoto", idsRand))
										.setProjection(Projections.max("halaman")).uniqueResult();
								StreamingHibernateUtil.getInstance().closeSession();

								fotoKe = m == null ? 1 : m.intValue() + 1;

								final MyWindow window = new MyWindow("Masukkan halaman", "none", true);
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

								Columns columns = new Columns();
								columns.setParent(grid);

								MyColumnConfig column = new MyColumnConfig();
								column.setParent(columns);
								column.setWidth("25%");

								column = new MyColumnConfig();
								column.setParent(columns);

								Rows rows = new Rows();
								rows.setParent(grid);
								Row row = new Row();
								row.setValign("top");
								row.setParent(rows);
								row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan Foto"));
								final Textbox keterangan = new Textbox();
								keterangan.setWidth("90%");
								keterangan.setRows(2);
								row.appendChild(keterangan);

								row = new Row();
								row.setParent(rows);
								row.appendChild(new ais.ui.util.MyLabelConfig("Foto ke"));
								final MyIntbox halaman = new MyIntbox(fotoKe);
								row.appendChild(halaman);

								row = new Row();
								ais.ui.util.ZkCompat.setSpans(row, "2");
								row.setParent(rows);

								MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
								save.setTooltiptext("Simpan");
								save.addEventListener("onClick", new EventListener() {
									@Override
									public void onEvent(Event event) throws Exception {

										if (halaman.getValue() == null) {
											MyMessageboxConfig.show("Foto ke", "Peringatan", MyMessageboxConfig.OK,
													MyMessageboxConfig.INFORMATION);
											return;
										}

										galeriFotoImage.setHalaman(halaman.getValue());
										galeriFotoImage.setKeterangan(keterangan.getValue());

										Session streamingSession = StreamingHibernateUtil.getInstance()
												.currentSession();
										streamingSession.getTransaction().begin();
										streamingSession.update(galeriFotoImage);
										streamingSession.getTransaction().commit();

										StreamingHibernateUtil.getInstance().closeSession();

										window.detach();

										loadDataDetail(galeriFoto);

									}
								});

								row.appendChild(save);

								MyToolbarbuttonConfig batal = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
								batal.setTooltiptext("Batal");
								batal.addEventListener("onClick", new EventListener() {
									@Override
									public void onEvent(Event event) throws Exception {
										/*
										 * Batal HANYA menutup window ini — data foto yang
										 * sudah ter-upload TETAP tersimpan seperti kondisi
										 * awal (halaman/keterangan default), TIDAK menerapkan
										 * perubahan halaman/keterangan yang baru diketik user
										 * di form ini.
										 */
										window.detach();
										loadDataDetail(galeriFoto);
									}
								});

								row.appendChild(batal);

								Center center = new Center();
								center.setParent(borderlayout);
								ais.ui.util.ZkCompat.setFlex(center, true);

								grid = new MyGrid();
								grid.setWidth("100%");
								grid.setParent(center);
								grid.setWidth("100%");
								grid.setHeight("100%");

								rows = new Rows();
								rows.setParent(grid);
								row = new Row();
								row.setParent(rows);

								Image image = new Image(CommonMedia.getGaleriFotoImage(galeriFoto.getId(),
										galeriFotoImage.getId(), null, null, false));
								image.setWidth("100%");
								image.setParent(row);

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

		toolbar.appendChild(new Space());
		toolbar.appendChild(new Space());
		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Tampilkan mulai foto ke :")));
		toolbar.appendChild(halamanMulai);
		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("s.d")));
		toolbar.appendChild(halamanSampai);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				loadDataDetail(galeriFoto);
			}
		});
		button.setParent(toolbar);

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		Common.clear(gridGaleriFotoImage);
		gridGaleriFotoImage.setParent(center);
		gridGaleriFotoImage.setWidth("100%");
		gridGaleriFotoImage.setHeight("100%");
		Columns columns = new Columns();
		columns.setParent(gridGaleriFotoImage);

		MyColumnConfig column = new MyColumnConfig("Tampilan");
		column.setParent(columns);
		column.setWidth("400px");

		// column = new MyColumnConfig("Tampil");
		// column.setParent(columns);
		// column.setWidth("5%");

		column = new MyColumnConfig("Keterangan");
		column.setParent(columns);

		// column = new MyColumnConfig("Foto ke");
		// column.setParent(columns);
		// column.setWidth("4%");
		//
		// column = new MyColumnConfig("");
		// column.setParent(columns);
		// column.setWidth("10%");

		loadDataDetail(galeriFoto);

		return borderlayout;
	}

	@SuppressWarnings("unchecked")
	private void loadDataDetail(final GaleriFoto galeriFoto) throws Exception {

		System.out.println("idsRand => " + idsRand);

		Session session = StreamingHibernateUtil.getInstance().currentSession();
		List<GaleriFotoImage> galeriFotoImages = session.createCriteria(GaleriFotoImage.class)
				.add(idsRand.isEmpty() ? Restrictions.sqlRestriction("false") : Restrictions.in("galeriFoto", idsRand))
				.add(Restrictions.between("halaman", halamanMulai.getValue() == null ? 0 : halamanMulai.getValue(),
						halamanSampai.getValue() == null ? 10 : halamanSampai.getValue()))
				.addOrder(Order.asc("halaman")).list();

		Common.clear(gridGaleriFotoImage);

		Rows rows = new Rows();
		rows.setParent(gridGaleriFotoImage);

		for (GaleriFotoImage galeriFotoImage : galeriFotoImages) {
			Row row = new Row();
			row.setValign("top");
			row.setParent(rows);
			initRow(row, galeriFotoImage);
		}
		StreamingHibernateUtil.getInstance().closeSession();

	}

	public void initRow(final Row row, final GaleriFotoImage galeriFotoImage) throws Exception {
		row.setValign("top");
		row.setAttribute("galeriFotoImage", galeriFotoImage);

		Vbox vbox = new Vbox();
		vbox.setParent(row);
		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				try {
					Session session = StreamingHibernateUtil.getInstance().currentSession();

					GaleriFotoImage mygaleriFotoImage = (GaleriFotoImage) session.createCriteria(GaleriFotoImage.class)
							.add(Restrictions.idEq(galeriFotoImage.getId())).uniqueResult();
					Filedownload.save(mygaleriFotoImage.ambilFile(), mygaleriFotoImage.getKeterangan());

					StreamingHibernateUtil.getInstance().closeSession();
				} catch (Exception e) {
					StreamingHibernateUtil.getInstance().rollbackTransaction();
					Common.tampilErrorJikaAdmin(e);
				}
			}
		};

		// A a = new A(galeriFotoImage.getNama());
		// a.setParent(row);
		// a.addEventListener("onClick", eventListener);

		Long galeriFoto = galeriFotoImage.getGaleriFoto();

		Image image = new Image(CommonMedia.getGaleriFotoImage(galeriFoto == null ? -1L : galeriFoto,
				galeriFotoImage.getId(), null, null, false));
		image.setWidth("400px");
		image.setParent(vbox);

		image.addEventListener("onClick", eventListener);

		final MyCheckboxConfig checkbox = new MyCheckboxConfig("Ditampilkan");
		final MyCkEditor keterangan = new MyCkEditor();
		keterangan.setValue(galeriFotoImage.getKeterangan());
		final MyIntbox halaman = new MyIntbox(galeriFotoImage.getHalaman());

		EventListener eventListenerUbah = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				galeriFotoImage.setDitampilkan(checkbox.isChecked());
				galeriFotoImage.setHalaman(halaman.getValue());
				galeriFotoImage.setKeterangan(keterangan.getValue());
				if (galeriFotoImage.getId() != null) {
					Session session = StreamingHibernateUtil.getInstance().currentSession();
					session.getTransaction().begin();
					Common.refreshUpdate(session, (galeriFotoImage));
					session.getTransaction().commit();
					StreamingHibernateUtil.getInstance().closeSession();
				}
				row.setValign("top");
				row.setAttribute("galeriFotoImage", galeriFotoImage);
			}
		};

		checkbox.setChecked(galeriFotoImage.getDitampilkan());
		checkbox.setParent(vbox);
		checkbox.addEventListener("onCheck", eventListenerUbah);

		keterangan.setParent(row);
		keterangan.setWidth("90%");
		keterangan.setHeight("150px");
		keterangan.addEventListener("onChange", eventListenerUbah);
		row.setValign("top");
		row.setAttribute("keterangan", keterangan);

		Hbox hbox = new Hbox();
		hbox.setParent(vbox);

		hbox.appendChild(new Label(ais.common.Common.getBahasaConfig("Foto ke :")));

		halaman.setParent(hbox);
		halaman.setCols(2);
		halaman.addEventListener("onChange", eventListenerUbah);

		hbox = new Hbox();
		hbox.setParent(vbox);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Download", "/img/upload.gif");
		button.setTooltiptext("Download");
		button.setParent(hbox);
		button.addEventListener("onClick", eventListener);

		button = new MyToolbarbuttonConfig("Hapus", "/img/svg/trash.svg");
		button.setTooltiptext("Hapus Data");
		button.setVisible(delete);
		button.setParent(hbox);

		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				MyMessageboxConfig.show("Apakah yakin ingin menghapus foto ini ?", "Pertanyaan",
						MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {

							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {
									if (galeriFotoImage.getId() != null) {
										Session session = StreamingHibernateUtil.getInstance().currentSession();
										session.getTransaction().begin();
										session.delete(galeriFotoImage);
										session.getTransaction().commit();
										StreamingHibernateUtil.getInstance().closeSession();

									}
									row.setVisible(false);
									row.detach();
								}

							}
						});

			}
		});
	}

}
