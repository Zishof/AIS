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
import ais.common.CommonPrivilages;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.file.FotoInformasiPerpustakaan;
import ais.database.model.library.InformasiPerpustakaan;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyFileUploadConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Helper UI untuk mengelola lampiran berkas (bukan khusus gambar, meski nama kelasnya menyiratkan
 * demikian — berkas apa pun bisa diunggah, lihat {@code Filedownload.save} yang mengunduh apa
 * adanya) pada satu {@link InformasiPerpustakaan}, ditampilkan sebagai grid unggah/hapus di dalam
 * layar detail informasi perpustakaan. Setiap lampiran memiliki flag "Tampil" yang menentukan
 * apakah berkas tersebut ditampilkan di halaman publik informasi perpustakaan, dapat diubah
 * langsung dari grid dan tersimpan seketika. Data BLOB disimpan/dibaca lewat sesi
 * {@link StreamingHibernateUtil} terpisah.
 */
public class InformasiPerpustakaanPunyaFotoHelper {

	private MyGrid gridFotoGambar;
	private boolean add = false;
	private boolean delete = false;

	/** Membuat helper terikat ke {@code gridFotoGambar}, menentukan visibilitas tombol tambah/hapus dari hak akses pengguna saat ini. */
	public InformasiPerpustakaanPunyaFotoHelper(MyGrid gridFotoGambar) {
		this.gridFotoGambar = gridFotoGambar;
		add = CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
	}

	/**
	 * Membangun tata letak grid lampiran lengkap dengan toolbar unggah, kolom nama/jenis/tampil/aksi,
	 * dan langsung memuat lampiran {@code informasiPerpustakaan} yang sudah ada. Unggahan baru
	 * disimpan sebagai BLOB pada baris {@link FotoInformasiPerpustakaan}.
	 *
	 * @param informasiPerpustakaan induk informasi perpustakaan yang lampirannya akan ditampilkan/dikelola
	 * @return tata letak {@link Borderlayout} siap ditempel ke komponen induk
	 */
	public Borderlayout initDetail(final InformasiPerpustakaan informasiPerpustakaan) throws Exception {
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, true);

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("30px");
		toolbar.setParent(north);

		Fileupload fileupload = new MyFileUploadConfig("Tambah File", "/img/new.gif");
		fileupload.setVisible(InformasiPerpustakaanPunyaFotoHelper.this.add);
		fileupload.setParent(toolbar);
		fileupload.setTooltiptext("Tambah");

		EventListener eventListener = new EventListener() {

			@SuppressWarnings("deprecation")
			@Override
			public void onEvent(Event event) throws Exception {
				try {
					UploadEvent uploadEvent = (UploadEvent) event;
					if (uploadEvent != null) {

						Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();

						FotoInformasiPerpustakaan fotoInformasiPerpustakaan = new FotoInformasiPerpustakaan();
						fotoInformasiPerpustakaan.setNama(uploadEvent.getMedia().getName());
						fotoInformasiPerpustakaan.setKeterangan(uploadEvent.getMedia().getContentType());
						fotoInformasiPerpustakaan.setInformasiPerpustakaan(
								informasiPerpustakaan.getId() == null ? new Random(Long.MIN_VALUE).nextLong()
										: informasiPerpustakaan.getId());

						fotoInformasiPerpustakaan.setFoto(new javax.sql.rowset.serial.SerialBlob(uploadEvent.getMedia().getByteData()));

						streamingSession.getTransaction().begin();
						streamingSession.save(fotoInformasiPerpustakaan);
						streamingSession.getTransaction().commit();

						StreamingHibernateUtil.getInstance().closeSession();

						Rows rows = gridFotoGambar.getRows() == null ? new Rows() : gridFotoGambar.getRows();
						rows.setParent(gridFotoGambar);
						Row row = new Row();row.setValign("top");
						row.setParent(rows);
						initRow(row, fotoInformasiPerpustakaan);
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

		loadDataDetail(informasiPerpustakaan);

		return borderlayout;
	}

	/** Memuat seluruh lampiran tersimpan milik {@code informasiPerpustakaan} (diurutkan id terbaru dulu) ke grid, atau tidak menambah baris apa pun bila induk belum tersimpan. */
	@SuppressWarnings("unchecked")
	private void loadDataDetail(final InformasiPerpustakaan informasiPerpustakaan) throws Exception {

		Session session = StreamingHibernateUtil.getInstance().currentSession();
		List<FotoInformasiPerpustakaan> fotoInformasiPerpustakaans = informasiPerpustakaan == null
				|| informasiPerpustakaan.getId() == null
						? new ArrayList<FotoInformasiPerpustakaan>()
						: session.createCriteria(FotoInformasiPerpustakaan.class)
								.add(Restrictions.eq("informasiPerpustakaan", informasiPerpustakaan.getId()))
								.addOrder(Order.desc("id")).list();

		Rows rows = gridFotoGambar.getRows() == null ? new Rows() : gridFotoGambar.getRows();
		rows.setParent(gridFotoGambar);

		for (FotoInformasiPerpustakaan fotoInformasiPerpustakaan : fotoInformasiPerpustakaans) {
			Row row = new Row();row.setValign("top");
			row.setParent(rows);
			initRow(row, fotoInformasiPerpustakaan);
		}
		StreamingHibernateUtil.getInstance().closeSession();
	}

	/**
	 * Mengisi satu baris grid dengan nama dan jenis berkas (keduanya berupa tautan yang memicu
	 * unduhan lewat {@link Filedownload#save}), checkbox "Tampil" (langsung tersimpan ke database
	 * saat diubah, menentukan tampil-tidaknya di halaman publik), tombol unduh, dan tombol hapus
	 * (dengan konfirmasi).
	 */
	public void initRow(final Row row, final FotoInformasiPerpustakaan fotoInformasiPerpustakaan) throws Exception {
		row.setValign("top");row.setAttribute("fotoInformasiPerpustakaan", fotoInformasiPerpustakaan);
		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				try {
					Session session = StreamingHibernateUtil.getInstance().currentSession();

					FotoInformasiPerpustakaan myfotoInformasiPerpustakaan = (FotoInformasiPerpustakaan) session
							.createCriteria(FotoInformasiPerpustakaan.class)
							.add(Restrictions.idEq(fotoInformasiPerpustakaan.getId())).uniqueResult();
					Filedownload.save(myfotoInformasiPerpustakaan.ambilFile(),
							myfotoInformasiPerpustakaan.getKeterangan());

					StreamingHibernateUtil.getInstance().closeSession();
				} catch (Exception e) {
					StreamingHibernateUtil.getInstance().rollbackTransaction();
					Common.tampilErrorJikaAdmin(e);
				}
			}
		};

		A a = new A(fotoInformasiPerpustakaan.getNama());
		a.setParent(row);
		a.addEventListener("onClick", eventListener);

		a = new A(fotoInformasiPerpustakaan.getKeterangan());
		a.setParent(row);
		a.addEventListener("onClick", eventListener);

		final MyCheckboxConfig checkbox = new MyCheckboxConfig();
		checkbox.setDisabled(add && delete);
		checkbox.setChecked(fotoInformasiPerpustakaan.getDitampilkan());
		checkbox.setParent(row);
		row.setValign("top");row.setAttribute("checkbox", checkbox);
		checkbox.addEventListener("onCheck", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				fotoInformasiPerpustakaan.setDitampilkan(checkbox.isChecked());
				if (fotoInformasiPerpustakaan.getId() != null) {
					Session session = StreamingHibernateUtil.getInstance().currentSession();
					session.getTransaction().begin();
					Common.refreshUpdate(session, (fotoInformasiPerpustakaan));
					session.getTransaction().commit();
					StreamingHibernateUtil.getInstance().closeSession();
				}
				row.setValign("top");row.setAttribute("fotoInformasiPerpustakaan", fotoInformasiPerpustakaan);
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
									if (fotoInformasiPerpustakaan.getId() != null) {
										Session session = StreamingHibernateUtil.getInstance().currentSession();
										session.getTransaction().begin();
										session.delete(fotoInformasiPerpustakaan);
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
