package ais.action.master.surat.helper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.commons.io.IOUtils;
import javax.sql.rowset.serial.SerialBlob;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.CommonPrivilages;
import ais.common.gdrive.GDriveUtilPerPengguna;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.file.FotoGambarSuratKeluar;
import ais.database.model.surat.SuratKeluar;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Helper UI ZK modul persuratan untuk mengelola lampiran gambar/scan
 * ({@link FotoGambarSuratKeluar}) pada satu {@link SuratKeluar}, dengan dua jalur penyimpanan:
 * unggah langsung ke database (blob) via {@link StreamingHibernateUtil}, atau unggah ke Google
 * Drive pengguna via {@link GDriveUtilPerPengguna}. Dipasang pada panel detail satu surat
 * keluar, menampilkan daftar lampiran sebagai grid dengan pratinjau, tombol unduh, dan hapus.
 *
 * <p>
 * Tombol "Tambah Lampiran" menyimpan berkas ke direktori media lokal lalu langsung menyimpan
 * baris {@link FotoGambarSuratKeluar} berisi blob isi berkas ({@code SerialBlob}); tombol
 * "Upload Scan ... ke Drive" (dibatasi ukuran lewat konfigurasi
 * {@code max_upload_via_drive_baru}, default 300 MB) mengunggah ke Google Drive secara
 * asinkron dan menggunakan {@link org.zkoss.zul.Timer} untuk menunggu proses backup selesai
 * sebelum baris {@link FotoGambarSuratKeluar} (berisi id Google Drive, bukan blob) disimpan.
 * Bila {@code suratKeluar} belum memiliki id (surat baru, belum tersimpan), lampiran sementara
 * dikaitkan ke id acak negatif ({@code new Random(Long.MIN_VALUE).nextLong()}) — pola umum di
 * kode ini untuk menghindari nilai {@code null} pada kolom id relasi sebelum induk tersimpan.
 * Mengunduh lampiran mengarahkan ke URL Google Drive bila tersimpan di Drive, atau menyajikan
 * unduhan langsung dari blob bila tersimpan lokal. Kegagalan di tengah proses unggah memicu
 * rollback transaksi {@link StreamingHibernateUtil}. Tombol hapus per baris meminta konfirmasi;
 * visibilitasnya mengikuti privilese hapus pengguna saat ini.
 * </p>
 */
public class SuratKeluarPunyaGambarFotoHelper {

	private MyGrid gridPengarang;
	private boolean delete = false;

	private com.google.api.services.drive.model.File fileUpload = null;

	/** Membangun helper terikat pada {@code gridPengarang} dan menghitung hak hapus pengguna saat ini. */
	public SuratKeluarPunyaGambarFotoHelper(MyGrid gridPengarang) {
		this.gridPengarang = gridPengarang;
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
	}

	/**
	 * Membangun panel (border layout) berisi toolbar unggah (lokal dan Google Drive) dan grid
	 * daftar lampiran untuk {@code suratKeluar}, lalu memuat data lampiran yang sudah tersimpan.
	 *
	 * @param suratKeluar surat keluar yang detail lampirannya ditampilkan/dikelola
	 * @param tampilEdit  bila {@code true}, toolbar unggah ditampilkan; bila {@code false}, hanya grid (mode lihat saja)
	 * @return border layout siap disisipkan sebagai konten panel detail
	 */
	public Borderlayout initDetail(final SuratKeluar suratKeluar, boolean tampilEdit) throws Exception {
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, true);
		north.setVisible(tampilEdit);

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("30px");
		toolbar.setParent(north);

		MyToolbarbuttonConfig fileupload = new MyToolbarbuttonConfig(
				"Tambah Lampiran Surat Keluar " + Common.ukuranLabelFileUpload(null), "/img/File-Upload-icon.png");
		fileupload.setParent(toolbar);
		fileupload.setUpload(Common.ukuranFileUpload(null));
		fileupload.setTooltiptext("Tambah");

		EventListener eventListener = new EventListener() {

			@SuppressWarnings("deprecation")
			@Override
			public void onEvent(Event event) throws Exception {
				try {
					UploadEvent uploadEvent = (UploadEvent) event;
					if (uploadEvent != null) {
						Media media = uploadEvent.getMedia();if(!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media))return;
						File folder = CommonMedia.getMediaDirectory();
						if (!folder.exists()) {
							folder.mkdirs();
						}
						File f = new File(folder.getAbsolutePath() + "/"
								+ URLEncoder.encode(ais.ui.util.WaktuUtil.getCalendar().getTimeInMillis() + "_"
										+ uploadEvent.getMedia().getName(), "UTF-8"));

						f.createNewFile();
						FileOutputStream fileOutputStream = new FileOutputStream(f);
						try {
							IOUtils.copyLarge(media.getStreamData(), fileOutputStream);
						} catch (Exception e) {
							try {
								IOUtils.write(media.getStringData(), fileOutputStream);
							} catch (Exception ee) {
								IOUtils.write(media.getByteData(), fileOutputStream);
							}
						}

						fileOutputStream.close();

						Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();

						FotoGambarSuratKeluar fotoGambarSuratKeluar = new FotoGambarSuratKeluar();
						fotoGambarSuratKeluar.setNama(uploadEvent.getMedia().getName());
						fotoGambarSuratKeluar.setKeterangan(uploadEvent.getMedia().getContentType());
						fotoGambarSuratKeluar
								.setSuratKeluar(suratKeluar.getId() == null ? new Random(Long.MIN_VALUE).nextLong()
										: suratKeluar.getId());

						if (f != null && f.exists()) {
							fotoGambarSuratKeluar.setFoto(new SerialBlob(IOUtils.toByteArray(new FileInputStream(f.getAbsolutePath()))));
						}

						streamingSession.getTransaction().begin();
						streamingSession.save(fotoGambarSuratKeluar);
						streamingSession.getTransaction().commit();

						StreamingHibernateUtil.getInstance().closeSession();

						Rows rows = gridPengarang.getRows() == null ? new Rows() : gridPengarang.getRows();
						rows.setParent(gridPengarang);
						Row row = new Row();
						row.setValign("top");
						row.setParent(rows);
						initRow(row, fotoGambarSuratKeluar);
					}
				} catch (Exception e) {
					StreamingHibernateUtil.getInstance().rollbackTransaction();
				}

			}
		};
		fileupload.addEventListener("onUpload", eventListener);

		int maxDrive = 300;
		try {
			maxDrive = Integer.parseInt(Common.getKonfigurasi("max_upload_via_drive_baru", "300").getNilai());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/surat/helper/SuratKeluarPunyaGambarFotoHelper.java:145");
			// TODO: handle exception
		}

		MyToolbarbuttonConfig upload = new MyToolbarbuttonConfig(
				"Upload Scan Surat Keluar ke Drive (maks " + maxDrive + " Mb)", "/img/Google-Drive-icon.png");
		upload.setParent(toolbar);
		upload.setTooltiptext("Upload Scan Surat Keluar ke Drive");
		upload.setAttribute("janganDisabled", true);
		upload.setUpload("true,maxsize=" + (1024 * maxDrive));
		upload.addEventListener("onUpload", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {

				fileUpload = null;
				File folder = CommonMedia.getMediaDirectory();
				if (!folder.exists()) {
					folder.mkdirs();
				}
				final UploadEvent uploadEvent = (UploadEvent) event;
				final Media media = uploadEvent.getMedia();if(!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media))return;

				File f = new File(folder.getAbsolutePath() + "/" + URLEncoder.encode(
						ais.ui.util.WaktuUtil.getCalendar().getTimeInMillis() + "_" + uploadEvent.getMedia().getName(),
						"UTF-8"));

				f.createNewFile();
				FileOutputStream fileOutputStream = new FileOutputStream(f);
				try {
					IOUtils.copyLarge(media.getStreamData(), fileOutputStream);
				} catch (Exception e) {
					try {
						IOUtils.write(media.getStringData(), fileOutputStream);
					} catch (Exception ee) {
						IOUtils.write(media.getByteData(), fileOutputStream);
					}
				}

				fileOutputStream.close();
				Tbmuser tbmuser = Common.getCurrentUser();
				GDriveUtilPerPengguna driveUtilPerPengguna = new GDriveUtilPerPengguna(tbmuser);

				driveUtilPerPengguna.prosesBackup(f, "Tata Kelola Surat", "Surat Keluar", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						fileUpload = (com.google.api.services.drive.model.File) arg0.getData();

					}
				});

				final Timer timer = new Timer(1000);
				timer.setRepeats(true);
				timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				timer.addEventListener("onTimer", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						if (fileUpload != null && fileUpload.getId() != null) {

							Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();

							FotoGambarSuratKeluar fotoGambarSuratKeluar = new FotoGambarSuratKeluar();
							fotoGambarSuratKeluar.setNama(uploadEvent.getMedia().getName());
							fotoGambarSuratKeluar.setKeterangan(uploadEvent.getMedia().getContentType());
							fotoGambarSuratKeluar
									.setSuratKeluar(suratKeluar.getId() == null ? new Random(Long.MIN_VALUE).nextLong()
											: suratKeluar.getId());
							Tbmuser tbmuser = Common.getCurrentUser();
							fotoGambarSuratKeluar.setGdrive(fileUpload.getId());
							fotoGambarSuratKeluar.setGdriveUsername(
									tbmuser == null ? Common.getCurrentSessionId() : tbmuser.getUserId());

							streamingSession.getTransaction().begin();
							streamingSession.save(fotoGambarSuratKeluar);
							streamingSession.getTransaction().commit();

							StreamingHibernateUtil.getInstance().closeSession();

							Rows rows = gridPengarang.getRows() == null ? new Rows() : gridPengarang.getRows();
							rows.setParent(gridPengarang);
							Row row = new Row();
							row.setValign("top");
							row.setParent(rows);
							initRow(row, fotoGambarSuratKeluar);

							timer.stop();
							timer.detach();
						}
					}
				});
				timer.start();

			}
		});

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
		column.setWidth("10%");

		loadDataDetail(suratKeluar);

		return borderlayout;
	}

	/** Memuat baris-baris lampiran tersimpan untuk {@code suratKeluar} dari database (terurut terbaru dulu) dan merendernya ke grid. */
	@SuppressWarnings("unchecked")
	private void loadDataDetail(SuratKeluar suratKeluar) throws Exception {

		Session session = StreamingHibernateUtil.getInstance().currentSession();
		List<FotoGambarSuratKeluar> fotoGambarSuratKeluars = suratKeluar == null || suratKeluar.getId() == null
				? new ArrayList<FotoGambarSuratKeluar>()
				: session.createCriteria(FotoGambarSuratKeluar.class)
						.add(Restrictions.eq("suratKeluar", suratKeluar.getId())).addOrder(Order.desc("id")).list();

		Rows rows = gridPengarang.getRows() == null ? new Rows() : gridPengarang.getRows();
		rows.setParent(gridPengarang);

		for (FotoGambarSuratKeluar fotoGambarSuratKeluar : fotoGambarSuratKeluars) {
			Row row = new Row();
			row.setValign("top");
			row.setParent(rows);
			initRow(row, fotoGambarSuratKeluar);
		}
		StreamingHibernateUtil.getInstance().closeSession();
	}

	/**
	 * Mengisi {@code row} dengan tautan nama berkas + pratinjau ({@code CommonMedia#preview}),
	 * tombol unduh (mengarahkan ke URL Google Drive atau menyajikan unduhan blob lokal
	 * tergantung tempat penyimpanan), dan tombol hapus (bila pengguna berhak); tombol hapus
	 * meminta konfirmasi, lalu menghapus baris lampiran dari database dalam transaksi tersendiri.
	 */
	public void initRow(final Row row, final FotoGambarSuratKeluar fotoGambarSuratKeluar) throws Exception {
		row.setValign("top");
		row.setValign("top");
		row.setAttribute("fotoGambarSuratKeluar", fotoGambarSuratKeluar);

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				try {

					if (fotoGambarSuratKeluar.getGdrive() != null && !fotoGambarSuratKeluar.getGdrive().isEmpty()) {
						ExecutionsCtrl.getCurrent().sendRedirect(fotoGambarSuratKeluar.downloadGDriveUrl(), "_blank");
					} else {

						Session session = StreamingHibernateUtil.getInstance().currentSession();

						FotoGambarSuratKeluar myfotoGambarSuratKeluar = (FotoGambarSuratKeluar) session
								.createCriteria(FotoGambarSuratKeluar.class)
								.add(Restrictions.idEq(fotoGambarSuratKeluar.getId())).uniqueResult();

						Filedownload.save(myfotoGambarSuratKeluar.ambilFile(), myfotoGambarSuratKeluar.getKeterangan());

						StreamingHibernateUtil.getInstance().closeSession();
					}
				} catch (Exception e) {
					StreamingHibernateUtil.getInstance().rollbackTransaction();
					Common.tampilErrorJikaAdmin(e);
				}
			}
		};

		Vbox vbox = new Vbox();
		vbox.setParent(row);
		A a = new A(fotoGambarSuratKeluar.getNama());
		a.addEventListener("onClick", eventListener);
		a.setParent(vbox);

		vbox.setWidth("100%");
		CommonMedia.preview(fotoGambarSuratKeluar, vbox);

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
									if (fotoGambarSuratKeluar.getId() != null) {
										Session session = StreamingHibernateUtil.getInstance().currentSession();
										session.getTransaction().begin();
										session.delete(fotoGambarSuratKeluar);
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
