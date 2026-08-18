package ais.action.master.library.helper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URLEncoder;
import java.sql.Blob;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.commons.io.IOUtils;
import org.hibernate.Hibernate;
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

import ais.action.master.library.util.LibraryUtil;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.file.FotoItem;
import ais.database.model.library.Item;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyFileUploadConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;

public class ItemPunyaFotoHelper {

	private MyGrid gridFotoGambar;
	private boolean add = false;
	private boolean delete = false;
	private Tbmuser tbmuser;

	public ItemPunyaFotoHelper(MyGrid gridFotoGambar) {
		this.gridFotoGambar = gridFotoGambar;
		add = CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
	}

	public Borderlayout initDetail(final Item item) throws Exception {
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, true);
		tbmuser = Common.getCurrentUser();
		north.setVisible(tbmuser.getMahasiswa() == null && tbmuser.ambilDosen() == null);

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("30px");
		toolbar.setParent(north);

		Fileupload fileupload = new MyFileUploadConfig("Tambah File (PDF)", "/img/new.gif");
		fileupload.setVisible(ItemPunyaFotoHelper.this.add);
		fileupload.setParent(toolbar);
		fileupload.setTooltiptext("Tambah");

		EventListener eventListener = new EventListener() {

			@SuppressWarnings("deprecation")
			@Override
			public void onEvent(Event event) throws Exception {
				try {
					UploadEvent uploadEvent = (UploadEvent) event;
					if (uploadEvent != null) {

						if (uploadEvent.getMedia().getName().toLowerCase().endsWith("pdf")) {

							File folder = new File(Common
									.getKonfigurasi("lokasi_penyimpanan_lampiran_perpustakaan", "/opt/gambar_perpus")
									.getNilai() + "/pdf/");
							if (!folder.exists()) {
								folder.mkdirs();
							}

							final File f = new File(folder.getAbsolutePath() + "/"
									+ URLEncoder.encode(ais.ui.util.WaktuUtil.getCalendar().getTimeInMillis() + "_"
											+ uploadEvent.getMedia().getName(), "UTF-8"));

							f.createNewFile();
							FileOutputStream fileOutputStream = new FileOutputStream(f);
							try {
								IOUtils.copyLarge(uploadEvent.getMedia().getStreamData(), fileOutputStream);
							} catch (Exception e) {
								try {
									IOUtils.write(uploadEvent.getMedia().getStringData(), fileOutputStream);
								} catch (Exception ee) {
									IOUtils.write(uploadEvent.getMedia().getByteData(), fileOutputStream);
								}
							}

							fileOutputStream.close();

							Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();

							FotoItem fotoItem = new FotoItem();
							fotoItem.setNama(uploadEvent.getMedia().getName());
							fotoItem.setKeterangan(uploadEvent.getMedia().getContentType());
							fotoItem.setItem(
									item.getId() == null ? new Random(Long.MIN_VALUE).nextLong() : item.getId());

							fotoItem.setPath(f.getAbsolutePath());

							streamingSession.getTransaction().begin();
							streamingSession.save(fotoItem);
							streamingSession.getTransaction().commit();

							try {
								Blob blob = new javax.sql.rowset.serial.SerialBlob(IOUtils.toByteArray(new FileInputStream(f)));
								fotoItem.setFoto(blob);
								streamingSession.getTransaction().begin();
								streamingSession.update(fotoItem);
								streamingSession.getTransaction().commit();

							} catch (Exception e) {
								Common.tampilErrorJikaAdmin(e);
							}

							StreamingHibernateUtil.getInstance().closeSession();

							if (item.getId() != null && fotoItem.getPath() != null
									&& !fotoItem.getPath().trim().isEmpty() && new File(fotoItem.getPath()).exists()) {
								Session session = HibernateUtil.currentSession();
								session.refresh(item);
								item.setImagePath(fotoItem.getPath());
								Common.refreshUpdate(session, item);
							}

							LibraryUtil.convertLampiranToText(fotoItem);

							Rows rows = gridFotoGambar.getRows() == null ? new Rows() : gridFotoGambar.getRows();
							rows.setParent(gridFotoGambar);
							Row row = new Row();row.setValign("top");
							row.setParent(rows);
							initRow(row, fotoItem);
						} else {
							MyMessageboxConfig.show("File yang anda upload harus ber-format PDF.", "Error",
									MyMessageboxConfig.OK, MyMessageboxConfig.ERROR);
						}
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
		column.setVisible(tbmuser.getMahasiswa() == null && tbmuser.ambilDosen() == null);
		column.setWidth("20%");

		loadDataDetail(item);

		return borderlayout;
	}

	@SuppressWarnings("unchecked")
	private void loadDataDetail(final Item item) throws Exception {

		Session session = StreamingHibernateUtil.getInstance().currentSession();
		List<FotoItem> fotoItems = item == null || item.getId() == null ? new ArrayList<FotoItem>()
				: session.createCriteria(FotoItem.class).add(Restrictions.eq("item", item.getId()))
						.addOrder(Order.desc("id")).list();

		Rows rows = gridFotoGambar.getRows() == null ? new Rows() : gridFotoGambar.getRows();
		rows.setParent(gridFotoGambar);

		for (FotoItem fotoItem : fotoItems) {
			Row row = new Row();row.setValign("top");
			row.setParent(rows);
			initRow(row, fotoItem);
		}
		StreamingHibernateUtil.getInstance().closeSession();
	}

	public void initRow(final Row row, final FotoItem fotoItem) throws Exception {
		row.setValign("top");row.setAttribute("fotoItem", fotoItem);
		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				try {
					Session session = StreamingHibernateUtil.getInstance().currentSession();

					FotoItem myfotoItem = (FotoItem) session.createCriteria(FotoItem.class)
							.add(Restrictions.idEq(fotoItem.getId())).uniqueResult();
					Filedownload.save(myfotoItem.ambilFile(), myfotoItem.getKeterangan());

					StreamingHibernateUtil.getInstance().closeSession();
				} catch (Exception e) {
					StreamingHibernateUtil.getInstance().rollbackTransaction();
					Common.tampilErrorJikaAdmin(e);
				}
			}
		};

		A a = new A(fotoItem.getNama());
		a.setParent(row);
		// a.addEventListener("onClick", eventListener);

		a = new A(fotoItem.getKeterangan());
		a.setParent(row);
		// a.addEventListener("onClick", eventListener);

		final MyCheckboxConfig checkbox = new MyCheckboxConfig();
		checkbox.setDisabled(add && delete);
		checkbox.setChecked(fotoItem.getDitampilkan());
		checkbox.setParent(row);
		row.setValign("top");row.setAttribute("checkbox", checkbox);
		checkbox.addEventListener("onCheck", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				fotoItem.setDitampilkan(checkbox.isChecked());
				if (fotoItem.getId() != null) {
					Session session = StreamingHibernateUtil.getInstance().currentSession();
					session.getTransaction().begin();
					Common.refreshUpdate(session, (fotoItem));
					session.getTransaction().commit();
					StreamingHibernateUtil.getInstance().closeSession();
				}
				row.setValign("top");row.setAttribute("fotoItem", fotoItem);
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
									if (fotoItem.getId() != null) {

										HibernateUtil.currentSession().createSQLQuery(
												"delete from library.lampiran_item where ref = " + fotoItem.getId())
												.executeUpdate();

										Session session = StreamingHibernateUtil.getInstance().currentSession();
										session.getTransaction().begin();
										session.delete(fotoItem);
										session.getTransaction().commit();
										StreamingHibernateUtil.getInstance().closeSession();
									}
	row.setVisible(false);row.detach();
								}

							}
						});

			}
		});
	}

}
