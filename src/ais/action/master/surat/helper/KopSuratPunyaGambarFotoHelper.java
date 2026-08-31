package ais.action.master.surat.helper;

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
import org.zkoss.zul.Fileupload;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Image;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.CommonPrivilages;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.file.FotoGambarKopSurat;
import ais.database.model.surat.KopSurat;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyFileUploadConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Helper ZK untuk mengelola galeri gambar/foto kop surat ({@link FotoGambarKopSurat}) milik satu
 * {@link KopSurat} (relasi "punya banyak"). Menyediakan toolbar unggah (disembunyikan bila pengguna
 * tidak punya hak {@link CommonPrivilages#CREATE}) yang langsung menyimpan gambar terunggah sebagai
 * BLOB lewat {@link StreamingHibernateUtil} (sesi Hibernate terpisah khusus data biner besar), grid
 * pratinjau gambar dengan tautan buka gambar penuh, dan aksi hapus per baris (tombol hapus
 * disembunyikan bila tidak punya hak {@link CommonPrivilages#DELETE}) dengan dialog konfirmasi.
 * Catatan: saat kop surat induk belum tersimpan ({@code kopSurat.getId()} masih {@code null}),
 * unggahan gambar disimpan dengan id kop surat acak (placeholder) alih-alih menunggu id asli — lihat
 * {@link #initDetail(KopSurat)}.
 */
public class KopSuratPunyaGambarFotoHelper {

	private MyGrid gridPengarang;
	private boolean add = false;
	private boolean delete = false;

	/**
	 * Membuat helper terikat pada satu komponen grid target, sekaligus mengevaluasi hak akses
	 * tambah dan hapus pengguna saat ini.
	 *
	 * @param gridPengarang komponen grid ZK tempat baris gambar dirender
	 */
	public KopSuratPunyaGambarFotoHelper(MyGrid gridPengarang) {
		this.gridPengarang = gridPengarang;
		add = CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
	}

	/**
	 * Membangun kerangka layout detail (toolbar unggah + kolom grid) dan langsung memuat data gambar
	 * untuk kop surat yang diberikan. Unggahan baru disimpan sebagai {@link FotoGambarKopSurat} lewat
	 * sesi {@link StreamingHibernateUtil} tersendiri; bila kop surat belum memiliki id (belum
	 * tersimpan), dipakai id acak sementara sebagai penanda relasi.
	 *
	 * @param kopSurat kop surat yang galeri gambarnya ditampilkan/dikelola
	 * @return komponen {@link Borderlayout} berisi toolbar dan grid gambar yang siap dipasang ke layar pemanggil
	 * @throws Exception diteruskan apa adanya dari kegagalan pemuatan data
	 */
	public Borderlayout initDetail(final KopSurat kopSurat) throws Exception {
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, true);

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("30px");
		toolbar.setParent(north);

		Fileupload fileupload = new MyFileUploadConfig("Tambah Kop", "/img/new.gif");
		fileupload.setVisible(KopSuratPunyaGambarFotoHelper.this.add);
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

						FotoGambarKopSurat fotoGambarKopSurat = new FotoGambarKopSurat();
						fotoGambarKopSurat.setNama(uploadEvent.getMedia()
								.getName());
						fotoGambarKopSurat.setKeterangan(uploadEvent.getMedia()
								.getContentType());
						fotoGambarKopSurat
								.setKopSurat(kopSurat.getId() == null ? new Random(
										Long.MIN_VALUE).nextLong() : kopSurat
										.getId());

						fotoGambarKopSurat.setFoto(new SerialBlob(uploadEvent.getMedia().getByteData()));

						streamingSession.getTransaction().begin();
						streamingSession.save(fotoGambarKopSurat);
						streamingSession.getTransaction().commit();

						StreamingHibernateUtil.getInstance().closeSession();

						Rows rows = gridPengarang.getRows() == null ? new Rows()
								: gridPengarang.getRows();
						rows.setParent(gridPengarang);
						Row row = new Row();row.setValign("top");
						row.setParent(rows);
						initRow(row, fotoGambarKopSurat);
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

		Common.clear(gridPengarang);
		gridPengarang.setParent(center);
		gridPengarang.setWidth("100%");
		gridPengarang.setHeight("100%");
		Columns columns = new Columns();
		columns.setParent(gridPengarang);

		MyColumnConfig column = new MyColumnConfig("Gambar");
		column.setParent(columns);

		column = new MyColumnConfig("Hapus");
		column.setParent(columns);
		column.setWidth("8%");

		loadDataDetail(kopSurat);

		return borderlayout;
	}

	@SuppressWarnings("unchecked")
	private void loadDataDetail(final KopSurat kopSurat) throws Exception {

		Session session = StreamingHibernateUtil.getInstance().currentSession();
		List<FotoGambarKopSurat> fotoGambarKopSurats = kopSurat == null
				|| kopSurat.getId() == null ? new ArrayList<FotoGambarKopSurat>()
				: session.createCriteria(FotoGambarKopSurat.class)
						.add(Restrictions.eq("kopSurat", kopSurat.getId()))
						.addOrder(Order.desc("id")).list();

		Rows rows = gridPengarang.getRows() == null ? new Rows()
				: gridPengarang.getRows();
		rows.setParent(gridPengarang);

		for (FotoGambarKopSurat fotoGambarKopSurat : fotoGambarKopSurats) {
			Row row = new Row();row.setValign("top");
			row.setParent(rows);
			initRow(row, fotoGambarKopSurat);
		}
		StreamingHibernateUtil.getInstance().closeSession();
	}

	/**
	 * Mengisi satu baris grid dengan pratinjau gambar (memakai URL dari
	 * {@link CommonMedia#getUrlFotoKopSurat}), tautan buka gambar, dan tombol hapus beserta event
	 * handler-nya (dialog konfirmasi, hapus dari database dan dari grid bila dikonfirmasi).
	 *
	 * @param row                  baris grid yang diisi
	 * @param fotoGambarKopSurat   data gambar yang direpresentasikan baris ini
	 * @throws Exception diteruskan apa adanya dari kegagalan pembangunan komponen
	 */
	public void initRow(final Row row,
			final FotoGambarKopSurat fotoGambarKopSurat) throws Exception {
		row.setValign("top");row.setAttribute("fotoGambarKopSurat", fotoGambarKopSurat);
		

		Vbox vbox = new Vbox();
		vbox.setParent(row);

		String url = CommonMedia.getUrlFotoKopSurat(fotoGambarKopSurat.getId(),
				fotoGambarKopSurat.getKopSurat(), 70, 800);
		Image image = new Image(url);
		image.setWidth("800px");
		image.setHeight("70px");
		image.setParent(vbox);

		A a = new A(url);
		a.setHref(url);
		vbox.appendChild(a);

		Hbox hbox = new Hbox();
		hbox.setParent(row);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
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
									if (fotoGambarKopSurat.getId() != null) {
										Session session = StreamingHibernateUtil
												.getInstance().currentSession();
										session.getTransaction().begin();
										session.delete(fotoGambarKopSurat);
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
