package ais.action.master.surat.helper;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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
import ais.database.model.file.FotoGambarTandaTanganPejabat;
import ais.database.model.rab.Pejabat;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyFileUploadConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Helper ZK untuk mengelola galeri gambar tanda tangan pejabat ({@link FotoGambarTandaTanganPejabat})
 * milik satu {@link Pejabat} (relasi "punya banyak") pada modul persuratan. Selain fitur
 * unggah/hapus yang serupa {@code KopSuratPunyaGambarFotoHelper}, setiap baris gambar di sini juga
 * punya koordinat penempatan tanda tangan pada dokumen ({@code posisiX}/{@code posisiY}, diedit
 * langsung dari grid lewat {@link MyDoublebox}) yang dipakai saat tanda tangan dibubuhkan otomatis
 * ke surat cetak. Toolbar unggah disembunyikan bila pengguna tidak punya hak
 * {@link CommonPrivilages#CREATE}; tombol hapus disembunyikan bila tidak punya hak
 * {@link CommonPrivilages#DELETE}. Catatan: saat pejabat induk belum tersimpan
 * ({@code pejabat.getId()} masih {@code null}), unggahan gambar disimpan dengan id pejabat acak
 * (placeholder) alih-alih menunggu id asli — lihat {@link #initDetail(Pejabat)}.
 */
public class FotoGambarTandaTanganPejabatHelper {

	private MyGrid gridGambar;
	private boolean add = false;
	private boolean delete = false;
	private Pejabat pejabat;

	/**
	 * Membuat helper terikat pada satu komponen grid target, sekaligus mengevaluasi hak akses
	 * tambah dan hapus pengguna saat ini.
	 *
	 * @param gridgambar komponen grid ZK tempat baris gambar tanda tangan dirender
	 */
	public FotoGambarTandaTanganPejabatHelper(MyGrid gridgambar) {
		this.gridGambar = gridgambar;
		add = CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
	}

	/**
	 * Membangun kerangka layout detail (toolbar unggah + kolom grid) dan langsung memuat data gambar
	 * tanda tangan untuk pejabat yang diberikan. Unggahan baru disimpan sebagai
	 * {@link FotoGambarTandaTanganPejabat} lewat sesi {@link StreamingHibernateUtil} tersendiri;
	 * bila pejabat belum memiliki id (belum tersimpan), dipakai id acak sementara sebagai penanda
	 * relasi.
	 *
	 * @param pejabat pejabat yang galeri tanda tangannya ditampilkan/dikelola
	 * @return komponen {@link Borderlayout} berisi toolbar dan grid gambar yang siap dipasang ke layar pemanggil
	 * @throws Exception diteruskan apa adanya dari kegagalan pemuatan data
	 */
	public Borderlayout initDetail(final Pejabat pejabat) throws Exception {
		this.pejabat = pejabat;
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, true);

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("30px");
		toolbar.setParent(north);

		Fileupload fileupload = new MyFileUploadConfig("Tambah Tanda Tangan", "/img/new.gif");
		fileupload.setVisible(FotoGambarTandaTanganPejabatHelper.this.add);
		fileupload.setParent(toolbar);
		fileupload.setTooltiptext("Tambah");

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				try {
					UploadEvent uploadEvent = (UploadEvent) event;
					if (uploadEvent != null) {

						Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();

						FotoGambarTandaTanganPejabat fotoGambarTandaTanganPejabat = new FotoGambarTandaTanganPejabat();
						fotoGambarTandaTanganPejabat.setNama(uploadEvent.getMedia().getName());
						fotoGambarTandaTanganPejabat.setKeterangan(uploadEvent.getMedia().getContentType());
						fotoGambarTandaTanganPejabat.setPejabat(
								pejabat.getId() == null ? new Random(Long.MIN_VALUE).nextLong() : pejabat.getId());

						fotoGambarTandaTanganPejabat
								.setFoto(Common.getBlobFromMedia(uploadEvent.getMedia()));

						streamingSession.getTransaction().begin();
						streamingSession.save(fotoGambarTandaTanganPejabat);
						streamingSession.getTransaction().commit();

						StreamingHibernateUtil.getInstance().closeSession();

						Rows rows = gridGambar.getRows() == null ? new Rows() : gridGambar.getRows();
						rows.setParent(gridGambar);
						Row row = new Row();row.setValign("top");
						row.setParent(rows);
						initRow(row, fotoGambarTandaTanganPejabat);
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

		Common.clear(gridGambar);
		gridGambar.setParent(center);
		gridGambar.setWidth("100%");
		gridGambar.setHeight("100%");
		Columns columns = new Columns();
		columns.setParent(gridGambar);

		MyColumnConfig column = new MyColumnConfig("Gambar");
		column.setParent(columns);

		column = new MyColumnConfig("X");
		column.setParent(columns);
		column.setWidth("0%");

		column = new MyColumnConfig("Y");
		column.setParent(columns);
		column.setWidth("0%");

		column = new MyColumnConfig("Hapus");
		column.setParent(columns);
		column.setWidth("8%");

		loadDataDetail(pejabat);

		return borderlayout;
	}

	@SuppressWarnings("unchecked")
	private void loadDataDetail(final Pejabat pejabat) throws Exception {

		Session session = StreamingHibernateUtil.getInstance().currentSession();
		List<FotoGambarTandaTanganPejabat> fotoGambarTandaTanganPejabats = pejabat == null || pejabat.getId() == null
				? new ArrayList<FotoGambarTandaTanganPejabat>()
				: session.createCriteria(FotoGambarTandaTanganPejabat.class)
						.add(Restrictions.eq("pejabat", pejabat.getId())).addOrder(Order.desc("id")).list();

		Rows rows = gridGambar.getRows() == null ? new Rows() : gridGambar.getRows();
		rows.setParent(gridGambar);

		for (FotoGambarTandaTanganPejabat fotoGambarTandaTanganPejabat : fotoGambarTandaTanganPejabats) {
			Row row = new Row();row.setValign("top");
			row.setParent(rows);
			initRow(row, fotoGambarTandaTanganPejabat);
		}
		StreamingHibernateUtil.getInstance().closeSession();
	}

	/**
	 * Mengisi satu baris grid dengan pratinjau gambar tanda tangan (dengan tautan buka gambar),
	 * kolom koordinat X/Y penempatan tanda tangan pada dokumen (masing-masing tersimpan otomatis
	 * saat berubah), dan tombol hapus beserta event handler-nya (dialog konfirmasi, hapus dari
	 * database dan dari grid bila dikonfirmasi).
	 *
	 * @param row                          baris grid yang diisi
	 * @param fotoGambarTandaTanganPejabat data gambar tanda tangan yang direpresentasikan baris ini
	 * @throws Exception diteruskan apa adanya dari kegagalan pembangunan komponen
	 */
	public void initRow(final Row row, final FotoGambarTandaTanganPejabat fotoGambarTandaTanganPejabat)
			throws Exception {
		row.setValign("top");row.setAttribute("fotoGambarTandaTanganPejabat", fotoGambarTandaTanganPejabat);

		Vbox vbox = new Vbox();
		vbox.setParent(row);

		String url = CommonMedia.getUrlFotoPejabat(fotoGambarTandaTanganPejabat.getId(),
				fotoGambarTandaTanganPejabat.getPejabat());
		Image image = new Image(url);
		image.setWidth("300px");
		image.setParent(vbox);

		A a = new A("ttd." + pejabat.getJenisJabatan().getKey());
		a.setHref(url);
		vbox.appendChild(a);

		final MyDoublebox posisiX = new MyDoublebox(fotoGambarTandaTanganPejabat.getPosisiX());
		posisiX.setWidth("90%");
		posisiX.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				fotoGambarTandaTanganPejabat.setPosisiX(posisiX.getValue() == null ? 0.0 : posisiX.getValue());
				row.setValign("top");row.setAttribute("fotoGambarTandaTanganPejabat", fotoGambarTandaTanganPejabat);
				if (fotoGambarTandaTanganPejabat.getId() != null) {
					Session session = StreamingHibernateUtil.getInstance().currentSession();
					session.getTransaction().begin();
					session.update(fotoGambarTandaTanganPejabat);
					session.getTransaction().commit();
					StreamingHibernateUtil.getInstance().closeSession();
				}
			}
		});
		posisiX.setParent(row);

		final MyDoublebox posisiY = new MyDoublebox(fotoGambarTandaTanganPejabat.getPosisiY());
		posisiY.setWidth("90%");
		posisiY.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				fotoGambarTandaTanganPejabat.setPosisiY(posisiY.getValue() == null ? 0.0 : posisiY.getValue());
				row.setValign("top");row.setAttribute("fotoGambarTandaTanganPejabat", fotoGambarTandaTanganPejabat);
				if (fotoGambarTandaTanganPejabat.getId() != null) {
					Session session = StreamingHibernateUtil.getInstance().currentSession();
					session.getTransaction().begin();
					session.update(fotoGambarTandaTanganPejabat);
					session.getTransaction().commit();
					StreamingHibernateUtil.getInstance().closeSession();
				}
			}
		});
		posisiY.setParent(row);

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
						MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {
									if (fotoGambarTandaTanganPejabat.getId() != null) {
										Session session = StreamingHibernateUtil.getInstance().currentSession();
										session.getTransaction().begin();
										session.delete(fotoGambarTandaTanganPejabat);
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
