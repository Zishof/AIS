package ais.action.master.employ.helper;

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
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Fileupload;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.common.Common;
import ais.common.CommonMedia;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.file.FotoLampiranPegawai;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyFileUploadConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Helper ZK generik untuk lampiran file/foto ({@link FotoLampiranPegawai}) yang bisa ditempel ke
 * entitas apa pun — target diidentifikasi lewat pasangan polymorphic {@code (clazz, item)}, sama
 * seperti pola {@code KomunikasiPegawaiHelper}. Upload langsung menyimpan blob file ke database
 * (bukan filesystem) lewat sesi Hibernate terpisah {@link StreamingHibernateUtil}; setiap baris
 * menyediakan pratinjau ({@link CommonMedia#preview}), tautan unduh, dan tombol hapus.
 *
 * <p>
 * <b>Catatan:</b> bila entitas induk ({@code generalValueObject}) belum tersimpan (id
 * {@code null}), {@code item} lampiran diisi dari {@code new Random(Long.MIN_VALUE).nextLong()} —
 * karena {@link Random} diinisialisasi dengan seed TETAP setiap panggilan, nilai yang dihasilkan
 * SELALU SAMA pada tiap upload untuk entitas yang belum tersimpan (bukan benar-benar acak).
 * Perilaku ini dipertahankan apa adanya sesuai cakupan dokumentasi ini.
 * </p>
 */
public class FotoLampiranPegawaiHelper {

	private MyGrid gridFotoGambar;
	private boolean add = true;
	private boolean delete = true;

	/** Membuat helper untuk grid target {@code gridFotoGambar} (privilese tambah/hapus saat ini keduanya default aktif, pemeriksaan {@code CommonPrivilages} dikomentari/dinonaktifkan). */
	public FotoLampiranPegawaiHelper(MyGrid gridFotoGambar) {
		this.gridFotoGambar = gridFotoGambar;
		// add = CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE);
		// delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
	}

	@SuppressWarnings({ "rawtypes" })
	/**
	 * Membangun kerangka grid lampiran: toolbar upload "Tambah Lampiran" (menyimpan file yang
	 * diunggah langsung sebagai {@link FotoLampiranPegawai} baru terikat ke
	 * {@code (clazz, generalValueObject.getId())}) dan kolom (Lampiran, aksi), lalu memuat lampiran
	 * yang sudah ada lewat {@link #loadDataDetail}.
	 */
	public Borderlayout initDetail(final GeneralValueObject generalValueObject, final Class clazz) throws Exception {
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, true);

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("30px");
		toolbar.setParent(north);

		Fileupload fileupload = new MyFileUploadConfig("Tambah Lampiran", "/img/new.gif");
		fileupload.setVisible(FotoLampiranPegawaiHelper.this.add);
		fileupload.setParent(toolbar);
		fileupload.setTooltiptext("Tambah");
		
		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				try {
					UploadEvent uploadEvent = (UploadEvent) event;
					if (uploadEvent != null) {

						Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();

						FotoLampiranPegawai fotoLampiranPegawai = new FotoLampiranPegawai();
						fotoLampiranPegawai.setNama(uploadEvent.getMedia().getName());
						fotoLampiranPegawai.setKeterangan(uploadEvent.getMedia().getContentType());
						fotoLampiranPegawai
								.setItem(generalValueObject.getId() == null ? new Random(Long.MIN_VALUE).nextLong()
										: generalValueObject.getId());

						fotoLampiranPegawai.setClazz(clazz.getName());
						fotoLampiranPegawai.setFoto(Common.getBlobFromMedia(uploadEvent.getMedia()));

						streamingSession.getTransaction().begin();
						streamingSession.save(fotoLampiranPegawai);
						streamingSession.getTransaction().commit();

						StreamingHibernateUtil.getInstance().closeSession();

						Rows rows = gridFotoGambar.getRows() == null ? new Rows() : gridFotoGambar.getRows();
						rows.setParent(gridFotoGambar);
						Row row = new Row();row.setValign("top");
						row.setParent(rows);
						initRow(row, fotoLampiranPegawai);
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

		MyColumnConfig column = new MyColumnConfig("Lampiran");
		column.setParent(columns);

		column = new MyColumnConfig("");
		column.setParent(columns);
		column.setWidth("10%");

		loadDataDetail(generalValueObject, clazz);

		return borderlayout;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	/** Memuat ulang seluruh {@link FotoLampiranPegawai} milik pasangan {@code (clazz, generalValueObject.getId())}, terurut terbaru lebih dulu; kosong bila {@code generalValueObject} belum tersimpan. */
	private void loadDataDetail(final GeneralValueObject generalValueObject, final Class clazz) throws Exception {

		Session session = StreamingHibernateUtil.getInstance().currentSession();
		List<FotoLampiranPegawai> fotoLampiranPegawais = generalValueObject == null
				|| generalValueObject.getId() == null
						? new ArrayList<FotoLampiranPegawai>()
						: session.createCriteria(FotoLampiranPegawai.class)
								.add(Restrictions.eq("item", generalValueObject.getId()))
								.add(Restrictions.eq("clazz", clazz.getName())).addOrder(Order.desc("id")).list();

		Rows rows = gridFotoGambar.getRows() == null ? new Rows() : gridFotoGambar.getRows();
		rows.setParent(gridFotoGambar);

		for (FotoLampiranPegawai fotoLampiranPegawai : fotoLampiranPegawais) {
			Row row = new Row();row.setValign("top");
			row.setParent(rows);
			initRow(row, fotoLampiranPegawai);
		}
		StreamingHibernateUtil.getInstance().closeSession();
	}

	/** Merender satu baris grid: pratinjau media, nama+keterangan sebagai tautan unduh, tombol unduh eksplisit, dan tombol hapus. */
	public void initRow(final Row row, final FotoLampiranPegawai fotoLampiranPegawai) throws Exception {
		row.setValign("top");row.setAttribute("fotoLampiranPegawai", fotoLampiranPegawai);
		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				try {
					Session session = StreamingHibernateUtil.getInstance().currentSession();

					FotoLampiranPegawai myfotoLampiranPegawai = (FotoLampiranPegawai) session
							.createCriteria(FotoLampiranPegawai.class)
							.add(Restrictions.idEq(fotoLampiranPegawai.getId())).uniqueResult();
					Filedownload.save(myfotoLampiranPegawai.ambilFile(), myfotoLampiranPegawai.getKeterangan());

					StreamingHibernateUtil.getInstance().closeSession();
				} catch (Exception e) {
					StreamingHibernateUtil.getInstance().rollbackTransaction();
					Common.tampilErrorJikaAdmin(e);
				}
			}
		};

		Vbox vbox = new Vbox();
		vbox.setParent(row);

		CommonMedia.preview(fotoLampiranPegawai, vbox);

		A a = new A(fotoLampiranPegawai.getNama());
		a.setParent(vbox);
		a.addEventListener("onClick", eventListener);
		vbox.setWidth("100%");

		a = new A(fotoLampiranPegawai.getKeterangan());
		a.setParent(vbox);
		a.addEventListener("onClick", eventListener);

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
									if (fotoLampiranPegawai.getId() != null) {
										Session session = StreamingHibernateUtil.getInstance().currentSession();
										session.getTransaction().begin();
										session.delete(fotoLampiranPegawai);
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
