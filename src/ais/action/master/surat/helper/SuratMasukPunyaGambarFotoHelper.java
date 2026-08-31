package ais.action.master.surat.helper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.commons.io.IOUtils;
import org.apache.pdfbox.util.PDFMergerUtility;
import javax.sql.rowset.serial.SerialBlob;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
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

import ais.action.master.surat.util.SuratUtil;
import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.CommonPrivilages;
import ais.common.gdrive.GDriveUtilPerPengguna;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.file.FotoGambarSuratKeluar;
import ais.database.model.file.FotoGambarSuratMasuk;
import ais.database.model.file.LampiranLain;
import ais.database.model.surat.SuratMasuk;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelBoldMerah;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Helper UI ZK modul persuratan untuk mengelola lampiran gambar/scan
 * ({@link FotoGambarSuratMasuk}) pada satu {@link SuratMasuk}, dengan pola penyimpanan ganda
 * yang sama seperti {@link SuratKeluarPunyaGambarFotoHelper} (unggah langsung ke database via
 * {@link StreamingHibernateUtil}, atau ke Google Drive via {@link GDriveUtilPerPengguna}).
 *
 * <p>
 * Berbeda dari surat masuk biasa, {@link #initDetail(SuratMasuk, boolean)} memiliki jalur khusus
 * bila {@code edit=false} dan surat masuk ini merupakan hasil tindak lanjut dari sebuah
 * {@code suratKeluar}: alih-alih menampilkan grid lampiran, panel menampilkan langsung lampiran
 * gambar surat keluar terkait bila ada, atau (bila belum ada) menyusun PDF surat keluar tersebut
 * on-the-fly dari template JRXML ({@link LampiranLain}, hingga 15 halaman lampiran berurutan)
 * dan menggabungkannya lewat {@link PDFMergerUtility} untuk pratinjau langsung. Untuk mode edit
 * biasa, panel juga menghormati status kunci ({@code suratMasuk.getDikunci()}) — bila surat
 * sedang dikunci pengguna lain, grid diganti pesan siapa yang menguncinya dan toolbar unggah
 * disembunyikan.
 * </p>
 */
public class SuratMasukPunyaGambarFotoHelper {

	private MyGrid gridPengarang;
	private boolean add = false;
	private boolean delete = false;

	/** Membangun helper terikat pada {@code gridPengarang} dan menghitung hak tambah/hapus pengguna saat ini. */
	public SuratMasukPunyaGambarFotoHelper(MyGrid gridPengarang) {
		this.gridPengarang = gridPengarang;
		add = CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
	}

	private com.google.api.services.drive.model.File fileUpload = null;

	/**
	 * Membangun panel detail lampiran untuk {@code suratMasuk}. Bila {@code edit=false} dan
	 * surat ini tindak lanjut dari surat keluar, menampilkan lampiran/PDF surat keluar tersebut
	 * (lihat javadoc kelas); selain itu menampilkan toolbar unggah (lokal/Google Drive, bila
	 * belum dikunci pengguna lain dan pengguna berhak tambah) dan grid daftar lampiran.
	 *
	 * @param suratMasuk surat masuk yang detail lampirannya ditampilkan/dikelola
	 * @param edit       {@code true} untuk mode edit lampiran biasa; {@code false} untuk mode
	 *                   lihat, yang memicu jalur khusus pratinjau surat keluar terkait
	 * @return border layout siap disisipkan sebagai konten panel detail
	 */
	public Borderlayout initDetail(final SuratMasuk suratMasuk, boolean edit) throws Exception {
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

		if (!edit && suratMasuk.getSuratKeluar() != null) {
			Center center = new Center();
			center.setParent(borderlayout);
			ais.ui.util.ZkCompat.setFlex(center, true);

			Session session = StreamingHibernateUtil.getInstance().currentSession();
			int number = ((Number) session.createCriteria(FotoGambarSuratKeluar.class)
					.add(Restrictions.eq("suratKeluar", suratMasuk.getSuratKeluar().getId()))
					.setProjection(Projections.rowCount()).uniqueResult()).intValue();
			StreamingHibernateUtil.getInstance().closeSession();

			if (number > 0) {
				MyGrid gridGambar = new MyGrid();
				center.appendChild(new SuratKeluarPunyaGambarFotoHelper(gridGambar)
						.initDetail(suratMasuk.getSuratKeluar(), false));
			} else {
				try {
					Map<String, Object> parameters = SuratUtil.ubahIsiSuratKeluar(suratMasuk.getSuratKeluar());

					PDFMergerUtility ut = new PDFMergerUtility();

					for (int index = 1; index <= 15; index++) {
						try {
							LampiranLain lampiranLain = LampiranLain.ambil(
									suratMasuk.getSuratKeluar().getKlasifikasiSuratKeluar().getId(),
									LampiranLain.FILE_JRXML_LAYOUT_SURAT + (index == 1 ? "" : "_" + index));
							if (lampiranLain != null && lampiranLain.getId() != null) {
								try {

									File file = Report.generateCompileFileReport(Report.PDF, parameters,
											lampiranLain.ambilFile().getAbsolutePath(), ais.ui.util.WaktuUtil.getDate(),
											false);
									ut.addSource(file);

								} catch (Exception e) {
									Common.tampilErrorJikaAdmin(e);
								}
							}
						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/surat/helper/SuratMasukPunyaGambarFotoHelper.java:112");
						}

					}
					try {
						File filePdfBaru = new File(
								Common.ambilREAL_PATH_REPORT() + "/" + Common.getGeneratedBarCode() + ".pdf");
						ut.setDestinationStream(new FileOutputStream(filePdfBaru));
						ut.mergeDocuments();
						CommonReport.tampilkanReportPDF(center, filePdfBaru);
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/surat/helper/SuratMasukPunyaGambarFotoHelper.java:125");

				}
			}

		} else {

			final Tbmuser tbmuser = Common.getCurrentUser();

			North north = new North();
			north.setParent(borderlayout);
			ais.ui.util.ZkCompat.setFlex(north, true);
			north.setVisible(edit && add && suratMasuk.getDikunci() == null);

			Toolbar toolbar = new Toolbar();
			toolbar.setHeight("30px");
			toolbar.setParent(north);

			MyToolbarbuttonConfig fileupload = new MyToolbarbuttonConfig(
					"Tambah Lampiran Surat Masuk " + Common.ukuranLabelFileUpload(null), "/img/File-Upload-icon.png");
			fileupload.setVisible(SuratMasukPunyaGambarFotoHelper.this.add);
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

							FotoGambarSuratMasuk fotoGambarSuratMasuk = new FotoGambarSuratMasuk();
							fotoGambarSuratMasuk.setNama(uploadEvent.getMedia().getName());
							fotoGambarSuratMasuk.setKeterangan(uploadEvent.getMedia().getContentType());
							fotoGambarSuratMasuk
									.setSuratMasuk(suratMasuk.getId() == null ? new Random(Long.MIN_VALUE).nextLong()
											: suratMasuk.getId());

							if (f != null && f.exists()) {
								fotoGambarSuratMasuk.setFoto(new SerialBlob(IOUtils.toByteArray(new FileInputStream(f.getAbsolutePath()))));
							}

							streamingSession.getTransaction().begin();
							streamingSession.save(fotoGambarSuratMasuk);
							streamingSession.getTransaction().commit();

							StreamingHibernateUtil.getInstance().closeSession();

							Rows rows = gridPengarang.getRows() == null ? new Rows() : gridPengarang.getRows();
							rows.setParent(gridPengarang);
							Row row = new Row();
							row.setValign("top");
							row.setParent(rows);
							initRow(row, fotoGambarSuratMasuk);
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
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/surat/helper/SuratMasukPunyaGambarFotoHelper.java:219");
				// TODO: handle exception
			}

			MyToolbarbuttonConfig upload = new MyToolbarbuttonConfig(
					"Upload Scan Surat Masuk ke Drive (maks " + maxDrive + " Mb)", "/img/Google-Drive-icon.png");
			upload.setParent(toolbar);
			upload.setTooltiptext("Upload Scan Surat Masuk ke Drive");
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

					GDriveUtilPerPengguna driveUtilPerPengguna = new GDriveUtilPerPengguna(tbmuser);

					driveUtilPerPengguna.prosesBackup(f, "Tata Kelola Surat", "Surat Masuk", new EventListener() {

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

								FotoGambarSuratMasuk fotoGambarSuratMasuk = new FotoGambarSuratMasuk();
								fotoGambarSuratMasuk.setNama(uploadEvent.getMedia().getName());
								fotoGambarSuratMasuk.setKeterangan(uploadEvent.getMedia().getContentType());
								fotoGambarSuratMasuk.setSuratMasuk(
										suratMasuk.getId() == null ? new Random(Long.MIN_VALUE).nextLong()
												: suratMasuk.getId());

								fotoGambarSuratMasuk.setGdrive(fileUpload.getId());
								fotoGambarSuratMasuk.setGdriveUsername(
										tbmuser == null ? Common.getCurrentSessionId() : tbmuser.getUserId());

								streamingSession.getTransaction().begin();
								streamingSession.save(fotoGambarSuratMasuk);
								streamingSession.getTransaction().commit();

								StreamingHibernateUtil.getInstance().closeSession();

								Rows rows = gridPengarang.getRows() == null ? new Rows() : gridPengarang.getRows();
								rows.setParent(gridPengarang);
								Row row = new Row();
								row.setValign("top");
								row.setParent(rows);
								initRow(row, fotoGambarSuratMasuk);

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

			if (suratMasuk.getDikunci() != null) {
				center.appendChild(new MyLabelBoldMerah("File dikunci oleh " + suratMasuk.getDikunci().getUserNama()));
			} else {

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

				loadDataDetail(suratMasuk);
			}
		}

		return borderlayout;
	}

	/** Memuat baris-baris lampiran tersimpan untuk {@code suratMasuk} dari database (terurut terbaru dulu) dan merendernya ke grid. */
	@SuppressWarnings("unchecked")
	private void loadDataDetail(final SuratMasuk suratMasuk) throws Exception {

		Session session = StreamingHibernateUtil.getInstance().currentSession();
		List<FotoGambarSuratMasuk> fotoGambarSuratMasuks = suratMasuk == null || suratMasuk.getId() == null
				? new ArrayList<FotoGambarSuratMasuk>()
				: session.createCriteria(FotoGambarSuratMasuk.class)
						.add(Restrictions.eq("suratMasuk", suratMasuk.getId())).addOrder(Order.desc("id")).list();

		Rows rows = gridPengarang.getRows() == null ? new Rows() : gridPengarang.getRows();
		rows.setParent(gridPengarang);

		for (FotoGambarSuratMasuk fotoGambarSuratMasuk : fotoGambarSuratMasuks) {
			Row row = new Row();
			row.setValign("top");
			row.setParent(rows);
			initRow(row, fotoGambarSuratMasuk);
		}
		StreamingHibernateUtil.getInstance().closeSession();
	}

	/**
	 * Mengisi {@code row} dengan tautan nama berkas + pratinjau ({@code CommonMedia#preview}),
	 * tombol unduh (mengarahkan ke URL Google Drive atau menyajikan unduhan blob lokal
	 * tergantung tempat penyimpanan), dan tombol hapus (bila pengguna berhak); tombol hapus
	 * meminta konfirmasi, lalu menghapus baris lampiran dari database dalam transaksi tersendiri.
	 */
	public void initRow(final Row row, final FotoGambarSuratMasuk fotoGambarSuratMasuk) throws Exception {
		row.setValign("top");
		row.setValign("top");
		row.setAttribute("fotoGambarSuratMasuk", fotoGambarSuratMasuk);

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				try {

					if (fotoGambarSuratMasuk.getGdrive() != null && !fotoGambarSuratMasuk.getGdrive().isEmpty()) {
						ExecutionsCtrl.getCurrent().sendRedirect(fotoGambarSuratMasuk.downloadGDriveUrl(), "_blank");
					} else {

						Session session = StreamingHibernateUtil.getInstance().currentSession();

						FotoGambarSuratMasuk myfotoGambarSuratMasuk = (FotoGambarSuratMasuk) session
								.createCriteria(FotoGambarSuratMasuk.class)
								.add(Restrictions.idEq(fotoGambarSuratMasuk.getId())).uniqueResult();

						Filedownload.save(myfotoGambarSuratMasuk.ambilFile(), myfotoGambarSuratMasuk.getKeterangan());

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
		A a = new A(fotoGambarSuratMasuk.getNama());
		a.addEventListener("onClick", eventListener);
		a.setParent(vbox);

		vbox.setWidth("100%");
		CommonMedia.preview(fotoGambarSuratMasuk, vbox);

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
									if (fotoGambarSuratMasuk.getId() != null) {
										Session session = StreamingHibernateUtil.getInstance().currentSession();
										session.getTransaction().begin();
										session.delete(fotoGambarSuratMasuk);
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
