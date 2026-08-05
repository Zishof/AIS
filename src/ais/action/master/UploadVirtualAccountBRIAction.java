package ais.action.master;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.sql.Blob;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Fileupload;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;

import ais.action.master.helper.PembayaranUtilHelper;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.virtualaccount.DownloadNoRegistrasiCalonMahasiswa;
import ais.action.master.helper.virtualaccount.DownloadNoUjianCalonMahasiswa;
import ais.action.master.helper.virtualaccount.DownloadTagihanMahasiswa;
import ais.action.ws.util.PembayaranUtil;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.DetailBiaya;
import ais.database.model.DetailKegiatan;
import ais.database.model.JadwalPembayaran;
import ais.database.model.JenisKegiatan;
import ais.database.model.JenisSeleksi;
import ais.database.model.Jurusan;
import ais.database.model.Kegiatan;
import ais.database.model.Mahasiswa;
import ais.database.model.Perkuliahan;
import ais.database.model.UploadVirtualAccount;
import ais.database.model.file.FileFoto;
import ais.database.model.file.UploadVirtualAccountFileContent;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyFileUploadConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class UploadVirtualAccountBRIAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	protected Combobox searchTahunAjaran;
	protected Combobox jenisSemester;

	private Textbox kode;
	private Combobox tahunAkademik;
	private Combobox ganjilGenap;
	private Combobox jenisUpload;
	private Textbox keterangan;

	private boolean delete = false;

	private UploadVirtualAccount uploadVirtualAccount;
	private MyToolbarbuttonConfig add;


	private Tabpanel downloadTagihanMahasiswa;
	private Tabpanel downloadTagihanNoRegCalonMahasiswa;
	private Tabpanel downloadTagihanNoUjianCalonMahasiswa;

	public void onDownloadTagihanMahasiswa(Event event) {
		if (downloadTagihanMahasiswa.getChildren().size() == 0) {
			DownloadTagihanMahasiswa downloadTagihanMahasiswa = new DownloadTagihanMahasiswa();
			downloadTagihanMahasiswa.setHeight("100%");
			downloadTagihanMahasiswa.setWidth("100%");
			downloadTagihanMahasiswa.setParent(this.downloadTagihanMahasiswa);
		}
	}

	public void onDownloadTagihanNoRegCalonMahasiswa(Event event) {
		if (downloadTagihanNoRegCalonMahasiswa.getChildren().size() == 0) {
			DownloadNoRegistrasiCalonMahasiswa downloadTagihanMahasiswa = new DownloadNoRegistrasiCalonMahasiswa();
			downloadTagihanMahasiswa.setHeight("100%");
			downloadTagihanMahasiswa.setWidth("100%");
			downloadTagihanMahasiswa.setParent(this.downloadTagihanNoRegCalonMahasiswa);
		}
	}

	public void onDownloadTagihanNoUjianCalonMahasiswa(Event event) {
		if (downloadTagihanNoUjianCalonMahasiswa.getChildren().size() == 0) {
			DownloadNoUjianCalonMahasiswa downloadTagihanMahasiswa = new DownloadNoUjianCalonMahasiswa();
			downloadTagihanMahasiswa.setHeight("100%");
			downloadTagihanMahasiswa.setWidth("100%");
			downloadTagihanMahasiswa.setParent(this.downloadTagihanNoUjianCalonMahasiswa);
		}
	}

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		Common.initLaguage();
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		Common.generateTahunAjaranDanSemua(searchTahunAjaran);
		Common.selectComboItem(searchTahunAjaran, Common.getCurrentTahunAkademik());
		if (searchTahunAjaran != null) { searchTahunAjaran.setSelectedIndex(-1); }

		MyComboitemConfig comboitem = new MyComboitemConfig(Perkuliahan.GANJIL);
		if (comboitem != null) { comboitem.setValue(Perkuliahan.GANJIL); }
		jenisSemester.appendChild(comboitem);
		comboitem = new MyComboitemConfig(Perkuliahan.GENAP);
		if (comboitem != null) { comboitem.setValue(Perkuliahan.GENAP); }
		jenisSemester.appendChild(comboitem);

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});
	}

	class UploadVirtualAccountRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final UploadVirtualAccount uploadVirtualAccount = (UploadVirtualAccount) arg1;

			new Label(uploadVirtualAccount.getKode()).setParent(arg0);
			new Label(uploadVirtualAccount.getTahunAkademik()).setParent(arg0);
			new Label(uploadVirtualAccount.getGanjilGenap()).setParent(arg0);

			RevisiHelper
					.createNewRevisi(UploadVirtualAccount.class, uploadVirtualAccount, uploadVirtualAccount.getNama())
					.setParent(arg0);
			new Label(uploadVirtualAccount.getTipe()).setParent(arg0);
			new Label(uploadVirtualAccount.getJenisUpload()).setParent(arg0);
			new Label(uploadVirtualAccount.getTerupload() + "").setParent(arg0);
			new Label(Common.dateFormat.get().format(uploadVirtualAccount.getTanggal_dirubah())).setParent(arg0);

			new Label(uploadVirtualAccount.getOleh()).setParent(arg0);
			new Label(uploadVirtualAccount.getKeterangan()).setParent(arg0);

			Hbox toolbar = new Hbox();

			MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Download", FileFoto.icon(null));
			toolbarbutton.setOrient("vertical");
			toolbarbutton.setParent(toolbar);
			toolbarbutton.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					Session session = null;
					try {
						session = StreamingHibernateUtil.getInstance().getSessionFactory().openSession();

						UploadVirtualAccountFileContent myuploadVirtualAccountFileContent = (UploadVirtualAccountFileContent) session
								.createCriteria(UploadVirtualAccountFileContent.class)
								.add(Restrictions.eq("ref", uploadVirtualAccount.getId())).setMaxResults(1)
								.uniqueResult();
						Filedownload.save(myuploadVirtualAccountFileContent.ambilFile(),
								myuploadVirtualAccountFileContent.getFileMimeType());

						StreamingHibernateUtil.getInstance().closeSession();
					} catch (Exception e) {
						StreamingHibernateUtil.getInstance().rollbackTransaction();
						Common.tampilErrorJikaAdmin(e);
					} finally {
						// FIX bocor: session dedikasi (getSessionFactory().openSession(), TIDAK di MAP) tak ditutup
						// oleh StreamingHibernateUtil.closeSession() (yg menutup sesi MAP). Tutup manual di finally.
						if (session != null) {
							try { session.clear(); } catch (Exception eF) { ais.common.ErrorAuditUtil.record(eF, "auto-audit(empty-catch) src/ais/action/master/UploadVirtualAccountBRIAction.java:238");}
							try { session.disconnect(); } catch (Exception eF) { ais.common.ErrorAuditUtil.record(eF, "auto-audit(empty-catch) src/ais/action/master/UploadVirtualAccountBRIAction.java:239");}
							try { session.close(); } catch (Exception eF) { ais.common.ErrorAuditUtil.record(eF, "auto-audit(empty-catch) src/ais/action/master/UploadVirtualAccountBRIAction.java:240");}
						}
					}

				}

			});

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Re-Upload", "/img/settings_16x16.png");
			button.setTooltiptext("Re-Upload");
			button.setOrient("vertical");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							Session session = null;
							try {
								session = StreamingHibernateUtil.getInstance().getSessionFactory().openSession();

								UploadVirtualAccountFileContent myuploadVirtualAccountFileContent = (UploadVirtualAccountFileContent) session
										.createCriteria(UploadVirtualAccountFileContent.class)
										.add(Restrictions.eq("ref", uploadVirtualAccount.getId())).setMaxResults(1)
										.uniqueResult();
								File file = myuploadVirtualAccountFileContent.ambilFile();

								upload(file, uploadVirtualAccount);

								StreamingHibernateUtil.getInstance().closeSession();
							} catch (Exception e) {
								StreamingHibernateUtil.getInstance().rollbackTransaction();
								Common.tampilErrorJikaAdmin(e);
							} finally {
								// FIX bocor: session dedikasi (getSessionFactory().openSession(), TIDAK di MAP) tak ditutup
								// oleh StreamingHibernateUtil.closeSession() (yg menutup sesi MAP). Tutup manual di finally.
								if (session != null) {
									try { session.clear(); } catch (Exception eF) { ais.common.ErrorAuditUtil.record(eF, "auto-audit(empty-catch) src/ais/action/master/UploadVirtualAccountBRIAction.java:279");}
									try { session.disconnect(); } catch (Exception eF) { ais.common.ErrorAuditUtil.record(eF, "auto-audit(empty-catch) src/ais/action/master/UploadVirtualAccountBRIAction.java:280");}
									try { session.close(); } catch (Exception eF) { ais.common.ErrorAuditUtil.record(eF, "auto-audit(empty-catch) src/ais/action/master/UploadVirtualAccountBRIAction.java:281");}
								}
							}

							onSearchDefault(null);
						}
					});

					// final MyWindow window = new MyWindow("Log", "none",
					// true);
					// page.getFirstRoot().appendChild(window);
					// window.setHeight("95%");
					// window.setWidth("90%");
					//
					// Borderlayout borderlayout = new
					// ais.ui.util.MyBorderlayout();
					// borderlayout.setParent(window);
					// Center center = new Center();
					// center.setParent(borderlayout);
					// ais.ui.util.ZkCompat.setFlex(center, true);
					//
					// Html html = new ais.ui.util.MyHtml("<font>"
					// + org.apache.commons.lang3.StringUtils.replace(
					// uploadVirtualAccount.getPeringatan(), "\n",
					// "<br>") + "</font>");
					// html.setHeight("1500%");
					// center.appendChild(html);
					//
					// South south = new South();
					// ais.ui.util.ZkCompat.setFlex(south, true);
					// south.setParent(borderlayout);
					//
					// Toolbar toolbar = new Toolbar();
					// // toolbar.setHeight("25px");
					// toolbar.setParent(south);
					// MyToolbarbuttonConfig cancel = new
					// MyToolbarbuttonConfig("Tutup",
					// "/img/cancel.gif");
					// cancel.setTooltiptext("Tutup");
					// cancel.addEventListener("onClick", new EventListener() {
					// @Override
					// public void onEvent(Event event) throws Exception {
					// window.detach();
					// }
					// });
					// cancel.setParent(toolbar);
					//
					// window.onModal();
				}

			});
			button.setParent(toolbar);

			button = new MyToolbarbuttonConfig("Delete", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
			button.setVisible(delete);
			button.setOrient("vertical");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {
											Session session = HibernateUtil.currentSession();

											session.createSQLQuery(
													"delete from kegiatan where upload_virtual_account = "
															+ uploadVirtualAccount.getId())
													.executeUpdate();

											Common.refreshDelete(session, uploadVirtualAccount);
											onSearchDefault(event);
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											MyMessageboxConfig.show(
													"Data ini tidak dapat dihapus .., karena berelasi dengan data lainnya, error-nya adalah sbagai berikut:"
															+ e.getMessage());
										}

									}

								}
							});

				}
			});
			button.setParent(toolbar);
			toolbar.setParent(arg0);
		}
	}

	public void onAdd(Event event) throws Exception {
		init(new UploadVirtualAccount());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private String namaFile = null;
	private String typeFile = null;
	private File file = null;
	private Label labelNama;

	private void init(final UploadVirtualAccount uploadVirtualAccount) throws Exception {

		tahunAkademik = new Combobox();
		Common.generateTahunAjaranDanSemua(tahunAkademik);
		ganjilGenap = new Combobox();
		MyComboitemConfig comboitem = new MyComboitemConfig(Perkuliahan.GANJIL);
		comboitem.setValue(Perkuliahan.GANJIL);
		ganjilGenap.appendChild(comboitem);
		comboitem = new MyComboitemConfig(Perkuliahan.GENAP);
		comboitem.setValue(Perkuliahan.GENAP);
		ganjilGenap.appendChild(comboitem);

		this.uploadVirtualAccount = uploadVirtualAccount;
		addWindow.setTitle("Upload Data Virtual Account");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("35%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		if (uploadVirtualAccount.getKode() == null || uploadVirtualAccount.getKode().trim().isEmpty()) {

			String kode = (String) HibernateUtil.currentSession().createCriteria(UploadVirtualAccount.class)
					.setProjection(Projections.property("kode")).setMaxResults(1).addOrder(Order.desc("id"))
					.uniqueResult();

			if (kode != null) {
				uploadVirtualAccount.setKode(kode);
			}
		}

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode BRIVA"));
		row.appendChild(kode = new Textbox(uploadVirtualAccount.getKode()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("File"));
		Fileupload fileupload = new MyFileUploadConfig("Upload Virtual Account");
		row.appendChild(fileupload);
		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				try {
					UploadEvent uploadEvent = (UploadEvent) event;
					if (uploadEvent != null) {

						Media media = uploadEvent.getMedia();if(!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media))return;
						if (media.getName().toLowerCase().endsWith("xlsx")) {

							InputStream inputStream = media.getStreamData();
							namaFile = media.getName();
							typeFile = media.getContentType();

							file = new File(Common.REAL_PATH + "/tmp/upload_Virtual_Account_"
									+ ais.ui.util.WaktuUtil.getCalendar().getTimeInMillis() + namaFile);
							System.out.println("file upload -> " + file.getAbsolutePath());
							IOUtils.copyLarge(inputStream, new FileOutputStream(file));

							labelNama.setValue(namaFile);
						} else {
							MyMessageboxConfig.show(
									"File yang anda upload harus ber-format Excel Open XML Spreadsheet (xlsx). Jika masih menggunakan format lain, buka file excel tersebut, kemudian Save As Excel Open XML Spreadsheet (xlsx). "
											+ media,
									"Error", MyMessageboxConfig.OK, MyMessageboxConfig.ERROR);
						}
					}
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}

			}
		};
		fileupload.addEventListener("onUpload", eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama File"));
		labelNama = new Label(uploadVirtualAccount.getNama());
		row.appendChild(labelNama);

		jenisUpload = new Combobox();
		comboitem = new MyComboitemConfig(UploadVirtualAccount.JENIS_UPLOAD_NIM);
		comboitem.setValue(UploadVirtualAccount.JENIS_UPLOAD_NIM);
		jenisUpload.appendChild(comboitem);

		comboitem = new MyComboitemConfig(UploadVirtualAccount.JENIS_UPLOAD_NO_REG);
		comboitem.setValue(UploadVirtualAccount.JENIS_UPLOAD_NO_REG);
		jenisUpload.appendChild(comboitem);

		comboitem = new MyComboitemConfig(UploadVirtualAccount.JENIS_UPLOAD_NO_UJIAN);
		comboitem.setValue(UploadVirtualAccount.JENIS_UPLOAD_NO_UJIAN);
		jenisUpload.appendChild(comboitem);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Upload"));
		row.appendChild(jenisUpload);
		jenisUpload.setWidth("90%");

		Common.selectComboItem(jenisUpload, uploadVirtualAccount.getJenisUpload());
		jenisUpload.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
		row.appendChild(tahunAkademik);
		Common.selectComboItem(tahunAkademik, uploadVirtualAccount.getTahunAkademik());
		tahunAkademik.setWidth("90%");
		tahunAkademik.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester"));
		row.appendChild(ganjilGenap);
		Common.selectComboItem(ganjilGenap, uploadVirtualAccount.getGanjilGenap());
		ganjilGenap.setWidth("90%");
		ganjilGenap.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(
				uploadVirtualAccount.getKeterangan() == null ? "" : uploadVirtualAccount.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(south);
		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				addWindow.setVisible(false);
			}
		});
		cancel.setParent(toolbar);
		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (onSave(event)) {
					onSearchDefault(null);
					addWindow.setVisible(false);
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);

	}

	public boolean onSave(Event event) throws Exception {
		if (kode.getValue().trim().isEmpty()) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Kode BRIVA",
					"Kolom Kode BRIVA belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Kode BRIVA.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (file == null) {
			MyMessageboxConfig.show("Masukkan file yang di-upload ", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (jenisUpload.getSelectedItem() == null) {
			MyMessageboxConfig.show("Pilih salah satu jenis upload", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (tahunAkademik.getSelectedItem() == null) {
			MyMessageboxConfig.show("Pilih salah satu tahun akademik", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (ganjilGenap.getSelectedItem() == null) {
			MyMessageboxConfig.show("Pilih salah satu semester", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (uploadVirtualAccount.getId() != null) {
			uploadVirtualAccount = (UploadVirtualAccount) session.load(UploadVirtualAccount.class,
					uploadVirtualAccount.getId());
		}

		uploadVirtualAccount.setJenisUpload((String) jenisUpload.getSelectedItem().getValue());
		uploadVirtualAccount.setBank(UploadVirtualAccount.BRI);
		uploadVirtualAccount.setKode(kode.getValue());
		uploadVirtualAccount.setNama(namaFile);
		uploadVirtualAccount.setTipe(typeFile);
		uploadVirtualAccount.setKeterangan(keterangan.getValue());
		uploadVirtualAccount.setTahunAkademik((String) tahunAkademik.getSelectedItem().getValue());
		uploadVirtualAccount.setGanjilGenap((String) ganjilGenap.getSelectedItem().getValue());

		if (uploadVirtualAccount.getId() != null) {
			session.update(uploadVirtualAccount);
		} else {
			session.save(uploadVirtualAccount);
		}

		Common.createDefaultTimer(new EventListener() {

			@SuppressWarnings("deprecation")
			@Override
			public void onEvent(Event arg0) throws Exception {

				Session session = StreamingHibernateUtil.getInstance().currentSession();

				UploadVirtualAccountFileContent uploadVirtualAccountFileContent = new UploadVirtualAccountFileContent();
				Blob blob = new javax.sql.rowset.serial.SerialBlob(IOUtils.toByteArray(new FileInputStream(file)));
				uploadVirtualAccountFileContent.setFoto(blob);
				uploadVirtualAccountFileContent.setNama(uploadVirtualAccount.getNama());
				uploadVirtualAccountFileContent.setFileMimeType(uploadVirtualAccount.getTipe());

				uploadVirtualAccountFileContent.setRef(uploadVirtualAccount.getId());
				uploadVirtualAccountFileContent.setUploadDate(ais.ui.util.WaktuUtil.getDate());
				uploadVirtualAccountFileContent.setRef(uploadVirtualAccount.getId());

				session.getTransaction().begin();
				session.save(uploadVirtualAccountFileContent);
				session.getTransaction().commit();
				StreamingHibernateUtil.getInstance().closeSession();

				upload(file, uploadVirtualAccount);

				onSearchDefault(null);
			}
		}, "Harap tunggu, sedang melakukan proses upload data ..");

		return true;
	}

	private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yy");
	private SimpleDateFormat dateFormatTime = new SimpleDateFormat("dd/MM/yy HH:mm:ss");
	private SimpleDateFormat dateFormatTime1 = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

	public void upload(final File file, final UploadVirtualAccount uploadVirtualAccount) throws Exception {

		final List<String> peringatan = new ArrayList<String>();
		final Intbox jumlahBerhasil = new Intbox(0);

		final Label label = new Label(ais.common.Common.getBahasaConfig("Proses upload data data .."));
		final Label downloadPath = new Label("");
		Clients.showBusy(label.getValue());
		final Timer timer = new Timer(200);
		timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		timer.setRepeats(true);
		timer.addEventListener("onTimer", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Clients.showBusy(label.getValue());
				if (label.getValue().isEmpty()) {
					System.out.println("loading file " + file.getAbsolutePath());
					if (!downloadPath.getValue().isEmpty()) {
						try { Filedownload.save(new java.io.File(downloadPath.getValue()), "text/plain"); } catch (Exception eD) { ais.common.ErrorAuditUtil.record(eD, "auto-audit(empty-catch) UploadVirtualAccountBRIAction download-laporan"); }
					}

					uploadVirtualAccount.setPeringatan(peringatan.toString());
					uploadVirtualAccount.setTerupload(jumlahBerhasil.getValue());
					Common.refreshUpdate(uploadVirtualAccount);

					MyMessageboxConfig.show("Upload data selesai dilakukan.\n" + "Jumlah data ter-upload = "
							+ Common.numberFormat.get().format(jumlahBerhasil.getValue()) + ".\n\n" + (peringatan.toString()),
							"Pemberitahuan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
							new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									onSearchDefault(arg0);
								}
							});

					Clients.clearBusy();
					timer.detach();
				}

			}
		});
		timer.start();

		final ais.common.UploadReportHelper report = new ais.common.UploadReportHelper("Upload Virtual Account BRI");
		new Thread(new Runnable() {

			@SuppressWarnings("unchecked")
			@Override
			public void run() {
				try {

				try {

					XSSFWorkbook workbook = new XSSFWorkbook(file.getAbsolutePath());
					XSSFSheet sheet = workbook.getSheetAt(0);

					int rowCount = (sheet.getLastRowNum() + 1);
					for (int i = 1; i < rowCount; i++) {
						try {

							String nama = null;
							String nilai = null;
							Date tgl = null;
							try {
								String tangggal = Common.getSheetContentAsString(sheet, 0, i);
								tgl = dateFormat.parse(tangggal.trim());

								nama = Common.getSheetContentAsString(sheet, 3, i);
								nilai = Common.getSheetContentAsString(sheet, 20, i);
							} catch (Exception e) {
								try {

									String d = Common.getSheetContentAsString(sheet, 1, i).trim();
									String t = Common.getSheetContentAsString(sheet, 2, i).trim();

									try {
										tgl = dateFormatTime.parse(d + " " + t);
									} catch (Exception we) {
										tgl = dateFormat.parse(d);
									}

									nama = Common.getSheetContentAsString(sheet, 11, i);
									nilai = Common.getSheetContentAsString(sheet, 7, i);
								} catch (Exception ee) { ais.common.ErrorAuditUtil.record(ee, "auto-audit(empty-catch) src/ais/action/master/UploadVirtualAccountBRIAction.java:732");

								}
							}

							if (tgl == null || nama == null || nama.trim().isEmpty()) {
								try {

									String d = Common.getSheetContentAsString(sheet, 0, i).trim();
									String t = Common.getSheetContentAsString(sheet, 2, i).trim();

									try {
										tgl = dateFormatTime.parse(d + " " + t);
									} catch (Exception we) {
										tgl = dateFormat.parse(d);
									}

									nama = Common.getSheetContentAsString(sheet, 5, i).trim();
									nilai = Common.getSheetContentAsString(sheet, 15, i);
								} catch (Exception ee) { ais.common.ErrorAuditUtil.record(ee, "auto-audit(empty-catch) src/ais/action/master/UploadVirtualAccountBRIAction.java:751");

								}

							}

							if (tgl == null || nama == null || nama.trim().isEmpty()) {
								try {

									String d = Common.getSheetContentAsString(sheet, 1, i).trim();

									tgl = dateFormat.parse(d);

									nama = Common.getSheetContentAsString(sheet, 8, i).trim();
									nilai = Common.getSheetContentAsString(sheet, 35, i);
								} catch (Exception ee) { ais.common.ErrorAuditUtil.record(ee, "auto-audit(empty-catch) src/ais/action/master/UploadVirtualAccountBRIAction.java:766");

								}

							}

							if (tgl == null || nama == null || nama.trim().isEmpty()) {
								try {

									String d = Common.getSheetContentAsString(sheet, 0, i).trim();
									String t = Common.getSheetContentAsString(sheet, 1, i).trim();

									try {
										tgl = dateFormatTime.parse(d + " " + t);
									} catch (Exception we) {
										tgl = dateFormat.parse(d);
									}

									nama = Common.getSheetContentAsString(sheet, 2, i).trim();
									nilai = Common.getSheetContentAsString(sheet, 9, i);
								} catch (Exception ee) { ais.common.ErrorAuditUtil.record(ee, "auto-audit(empty-catch) src/ais/action/master/UploadVirtualAccountBRIAction.java:786");

								}

							}

							if (tgl == null || nama == null || nama.trim().isEmpty()) {
								try {

									String t = Common.getSheetContentAsString(sheet, 1, i).trim();

									tgl = dateFormatTime1.parse(t);

									nama = Common.getSheetContentAsString(sheet, 2, i).trim();
									nilai = Common.getSheetContentAsString(sheet, 5, i);
								} catch (Exception ee) { ais.common.ErrorAuditUtil.record(ee, "auto-audit(empty-catch) src/ais/action/master/UploadVirtualAccountBRIAction.java:801");

								}

							}

							if (tgl != null) {

								if (nilai == null || nilai.trim().isEmpty()) {
									nilai = Common.getSheetContentAsString(sheet, 34, i);
								}
								if (nilai == null || nilai.trim().isEmpty()) {
									nilai = Common.getSheetContentAsString(sheet, 35, i);
								}
								if (nilai == null || nilai.trim().isEmpty()) {
									nilai = Common.getSheetContentAsString(sheet, 36, i);
								}
								if (nilai == null || nilai.trim().isEmpty()) {
									nilai = Common.getSheetContentAsString(sheet, 5, i);
									nama = Common.getSheetContentAsString(sheet, 2, i);
								}

								String nim = "";

								System.out.println("tangggal = " + Common.dateFormat3.get().format(tgl) + ", nama = " + nama
										+ ", nilai = " + nilai);

								label.setValue("Upload data tangggal = " + Common.dateFormat3.get().format(tgl) + ", nama = "
										+ nama + ", nilai = " + nilai + " ("
										+ Common.numberFormat.get().format(i * 100.0 / rowCount) + " %)");

								if (nama != null && nilai != null && !nama.trim().isEmpty()
										&& !nilai.trim().isEmpty()) {

									JenisKegiatan jenisKegiatan = null;
									Mahasiswa mahasiswa = null;
									BiodataCalonMahasiswa biodataCalonMahasiswa = null;

									try {

										nim = StringUtils.substring(nama,
												StringUtils.indexOf(nama, uploadVirtualAccount.getKode()));

										nim = StringUtils.trim(nim);
										nim = StringUtils.removeStart(nim, uploadVirtualAccount.getKode());
										nim = StringUtils.split(nim, " ")[0].trim();

										nim = nim.replaceAll("[^\\d.]", "");
										nim = org.apache.commons.lang3.StringUtils.replace(nim, ".", "");
										nim = org.apache.commons.lang3.StringUtils.replace(nim, ",", "");
										nim = org.apache.commons.lang3.StringUtils.replace(nim, "-", "");
										nim = org.apache.commons.lang3.StringUtils.replace(nim, "+", "");

										System.out.println("NIM / NO REG / NO UJIAN " + nim);

										Session session = HibernateUtil.currentNativeSession();

										if (uploadVirtualAccount.getJenisUpload() != null && uploadVirtualAccount
												.getJenisUpload().equals(UploadVirtualAccount.JENIS_UPLOAD_NIM)) {
											mahasiswa = (Mahasiswa) session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
													.add(Restrictions.eq("nim", nim.trim())).setMaxResults(1)
													.uniqueResult();

											if (mahasiswa == null && nim.length() > 8) {
												mahasiswa = (Mahasiswa) session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
														.add(Restrictions.eq("nim",
																nim.trim().substring(0, nim.trim().length() - 1)))
														.setMaxResults(1).uniqueResult();
											}

											if (mahasiswa == null && nim.length() > 8) {
												mahasiswa = (Mahasiswa) session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
														.add(Restrictions.eq("nim",
																nim.trim().substring(0, nim.trim().length() - 2)))
														.setMaxResults(1).uniqueResult();
											}

											if (mahasiswa == null && nim.length() > 8) {
												mahasiswa = (Mahasiswa) session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
														.add(Restrictions.eq("nim",
																nim.trim().substring(0, nim.trim().length() - 3)))
														.setMaxResults(1).uniqueResult();
											}

											if (mahasiswa != null) {
												jenisKegiatan = ConstantValues.PENDAFTARAN_MAHASISWA_LAMA;
											}
										} else if (uploadVirtualAccount.getJenisUpload() != null && uploadVirtualAccount
												.getJenisUpload().equals(UploadVirtualAccount.JENIS_UPLOAD_NO_UJIAN)) {

											biodataCalonMahasiswa = (BiodataCalonMahasiswa) session
													.createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
													.add(Restrictions.eq("noUjian", nim.trim())).setMaxResults(1)
													.uniqueResult();

											if (biodataCalonMahasiswa == null && nim.length() > 8) {
												biodataCalonMahasiswa = (BiodataCalonMahasiswa) session
														.createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
														.add(Restrictions.eq("noUjian",
																nim.trim().substring(0, nim.trim().length() - 1)))
														.setMaxResults(1).uniqueResult();
											}

											if (biodataCalonMahasiswa == null && nim.length() > 8) {
												biodataCalonMahasiswa = (BiodataCalonMahasiswa) session
														.createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
														.add(Restrictions.eq("noUjian",
																nim.trim().substring(0, nim.trim().length() - 2)))
														.setMaxResults(1).uniqueResult();
											}

											if (biodataCalonMahasiswa == null && nim.length() > 8) {
												biodataCalonMahasiswa = (BiodataCalonMahasiswa) session
														.createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
														.add(Restrictions.eq("noUjian",
																nim.trim().substring(0, nim.trim().length() - 3)))
														.setMaxResults(1).uniqueResult();
											}

											if (biodataCalonMahasiswa != null) {
												jenisKegiatan = ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU;
											}
										} else if (uploadVirtualAccount.getJenisUpload() != null && uploadVirtualAccount
												.getJenisUpload().equals(UploadVirtualAccount.JENIS_UPLOAD_NO_REG)) {
											biodataCalonMahasiswa = (BiodataCalonMahasiswa) session
													.createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
													.add(Restrictions.eq("noRegistrasi", nim.trim())).setMaxResults(1)
													.uniqueResult();

											if (biodataCalonMahasiswa == null && nim.length() > 8) {
												biodataCalonMahasiswa = (BiodataCalonMahasiswa) session
														.createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
														.add(Restrictions.eq("noRegistrasi",
																nim.trim().substring(0, nim.trim().length() - 1)))
														.setMaxResults(1).uniqueResult();
											}

											if (biodataCalonMahasiswa == null && nim.length() > 8) {
												biodataCalonMahasiswa = (BiodataCalonMahasiswa) session
														.createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
														.add(Restrictions.eq("noRegistrasi",
																nim.trim().substring(0, nim.trim().length() - 2)))
														.setMaxResults(1).uniqueResult();
											}

											if (biodataCalonMahasiswa == null && nim.length() > 8) {
												biodataCalonMahasiswa = (BiodataCalonMahasiswa) session
														.createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
														.add(Restrictions.eq("noRegistrasi",
																nim.trim().substring(0, nim.trim().length() - 3)))
														.setMaxResults(1).uniqueResult();
											}

											if (biodataCalonMahasiswa != null) {
												jenisKegiatan = ConstantValues.PENDAFTARAN_CALON_MAHASISWA;
											}
										}

									} catch (Exception e) {
										Common.tampilErrorJikaAdmin(e);
									}
									HibernateUtil.closeSession();

									System.out.println("Upload data " + jenisKegiatan + ", mahasiswa = " + mahasiswa
											+ ", biodataCalonMahasiswa = " + biodataCalonMahasiswa);

									if (jenisKegiatan != null && (mahasiswa != null || biodataCalonMahasiswa != null )) {

										Double amount = 0.0;
										try {
											nilai = org.apache.commons.lang3.StringUtils.replace(nilai, "K", "").trim();
											boolean ends = nilai.endsWith(".00") || nilai.endsWith(",00");
											System.out.println("ends => " + ends + ", nilai => " + nilai);
											if (ends) {
												nilai = StringUtils.substring(nilai, 0, nilai.length() - 3).trim();
												System.out.println("nilai => " + nilai);
												amount = Double.parseDouble(org.apache.commons.lang3.StringUtils.replace(

														org.apache.commons.lang3.StringUtils.replace(nilai, ".", "").trim()

														, ",", "").trim()

												);
											} else {
												amount = Double.parseDouble(org.apache.commons.lang3.StringUtils.replace(

														org.apache.commons.lang3.StringUtils.replace(nilai, ".", "").trim()

														, ",", "").trim()

												);
											}
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											try {
												amount = Double.parseDouble(org.apache.commons.lang3.StringUtils.replace(

														StringUtils
																.replace(org.apache.commons.lang3.StringUtils.replace(nilai, ".00", "").trim(),
																		".", "")
																.trim()

														, ",", "").trim());

											} catch (Exception ee) {
												Common.tampilErrorJikaAdmin(e);
											}

										}

										if (amount > 0.01) {

											JenisSeleksi jenisSeleksi = null;
											if (mahasiswa != null) {
												jenisSeleksi = mahasiswa.getJenisSeleksi();
											}
											if (biodataCalonMahasiswa != null) {
												jenisSeleksi = biodataCalonMahasiswa.getJenisSeleksi();
											}
											Integer semester = 0;
											if (mahasiswa != null) {
												String ta = uploadVirtualAccount.getTahunAkademik();
												Integer tahun = Integer.parseInt(StringUtils.split(ta, "/")[0]);
												semester = Common.getSemester(mahasiswa.getTahunangkatan(),
														uploadVirtualAccount.getGanjilGenap(),
														mahasiswa.getPindahKeKampusIniMasukSemester(), tahun,
														mahasiswa.getSemesterMulai());
											}

											JadwalPembayaran jadwalPembayaran = PembayaranUtil.getInstance()
													.getJadwalPembayaranTanpaDibatasiWaktu(jenisKegiatan,
															uploadVirtualAccount.getTahunAkademik(),
															uploadVirtualAccount.getGanjilGenap().equals(
																	Perkuliahan.GANJIL),
															jenisSeleksi, null, null);

											Session session = HibernateUtil.currentNativeSession();
											try {
												Kegiatan kegiatan = (Kegiatan) session.createCriteria(Kegiatan.class)
														.addOrder(Order.asc("id"))

														.add(biodataCalonMahasiswa != null
																? Restrictions.eq("calonMahasiswa",
																		biodataCalonMahasiswa)
																: Restrictions.eq("mahasiswa", mahasiswa))
														.add(Restrictions.eq("jenisKegiatan", jenisKegiatan))
														.add(Restrictions.eq("semster", semester))

														.setMaxResults(1).uniqueResult();

												System.out.println("kegiatan => " + kegiatan);

												if (kegiatan == null || kegiatan.getId() == null) {
													kegiatan = new Kegiatan();
												}

												kegiatan.setJadwalPembayaran(jadwalPembayaran);
												kegiatan.setNama(nama);
												kegiatan.setUploadVirtualAccount(uploadVirtualAccount);
												kegiatan.setAmount(amount);
												kegiatan.setCalonMahasiswa(biodataCalonMahasiswa);
												kegiatan.setMahasiswa(mahasiswa);
												kegiatan.setTahunAkademik(uploadVirtualAccount.getTahunAkademik());
												kegiatan.setSemster(semester);
												kegiatan.setJenisKegiatan(jenisKegiatan);
												kegiatan.setTanggal(tgl);
												kegiatan.setValidated(1);
												kegiatan.setValidator(uploadVirtualAccount.getBank().toUpperCase()
														+ " (" + uploadVirtualAccount.getOleh() + ")");

												session.getTransaction().begin();
												Common.refreshSaveOrUpdate(session, kegiatan);
												session.getTransaction().commit();

												Collection<DetailBiaya> detailBiayas = null;

												if (mahasiswa != null) {

													detailBiayas = PembayaranUtilHelper.getDetailBiayaMahasiswa(mahasiswa,
															semester, jenisKegiatan, null, false);

												} else if (biodataCalonMahasiswa != null && jenisKegiatan.getId()
														.equals(ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU
																.getId())) {

													biodataCalonMahasiswa.setPembayaranDaftarUlang(kegiatan);
													session.getTransaction().begin();
													Common.refreshUpdate(session, biodataCalonMahasiswa);
													session.getTransaction().commit();

													Jurusan prodiLulus = biodataCalonMahasiswa.getProdiLulus();
													detailBiayas = PembayaranUtilHelper.getDetailBiayaCalonMahasiswa(
															biodataCalonMahasiswa, jenisKegiatan, prodiLulus, semester,
															false);
												} else if (biodataCalonMahasiswa != null && jenisKegiatan.getId()
														.equals(ConstantValues.PENDAFTARAN_CALON_MAHASISWA.getId())) {
													biodataCalonMahasiswa.setPembayaranRegistrasi(kegiatan);
													session.getTransaction().begin();
													Common.refreshUpdate(session, biodataCalonMahasiswa);
													session.getTransaction().commit();
													Jurusan prodiLulus = biodataCalonMahasiswa.getProdiLulus();
													detailBiayas = new ArrayList<DetailBiaya>();
													if (prodiLulus == null || prodiLulus.getId() == null) {
														Jurusan myjurusan1 = biodataCalonMahasiswa.getProdi1() == null
																? biodataCalonMahasiswa.getProdi2()
																: biodataCalonMahasiswa.getProdi1();
														java.util.Collection<DetailBiaya> detailBiayas1 = PembayaranUtilHelper
																.getDetailBiayaCalonMahasiswa(biodataCalonMahasiswa,
																		jenisKegiatan, myjurusan1, semester, false);
														detailBiayas.addAll(detailBiayas1);
													} else {
														java.util.Collection<DetailBiaya> detailBiayas1 = PembayaranUtilHelper
																.getDetailBiayaCalonMahasiswa(biodataCalonMahasiswa,
																		jenisKegiatan, prodiLulus, semester, false);
														detailBiayas.addAll(detailBiayas1);
													}
												}

												Double totalBiaya = 0.0;
												for (DetailBiaya detailBiaya : detailBiayas) {
													Double biaya = detailBiaya.hitungTotalKegiatan(kegiatan, session);

													DetailKegiatan detailKegiatan = kegiatan
															.ambilSatuDetailKegiatan(detailBiaya, true);
													if (detailKegiatan == null) {
														detailKegiatan = new DetailKegiatan();

														detailKegiatan.setBiaya(biaya);

														detailKegiatan.setDetailBiaya(detailBiaya);
														detailKegiatan.setKeterangan(detailBiaya.getKeterangan());
														detailKegiatan.setKegiatan(kegiatan);
														// detailKegiatan.setAkunKredit(detailBiaya.getItemBiaya()
														// == null ? null
														// :
														// detailBiaya.getItemBiaya().getAkunKredit());
														session.getTransaction().begin();
														session.save(detailKegiatan);
														session.getTransaction().commit();
													}

													totalBiaya += biaya;

												}

												session.getTransaction().begin();

												if (totalBiaya.intValue() == amount.intValue()) {
													Common.simpanCicilanTanpaMencicil(kegiatan, amount, tgl, nama,
															ConstantValues.TUNAI, detailBiayas, session);
												} else {
													Common.simpanCicilanTanpaMencicil(kegiatan, amount, tgl, nama,
															ConstantValues.TUNAI, null, session);
												}

												session.getTransaction().commit();

											} catch (Exception eee) {
												Common.tampilErrorJikaAdmin(eee);
											}

											HibernateUtil.closeSession();

											jumlahBerhasil.setValue(jumlahBerhasil.getValue() + 1);
											report.sukses(i, nim + " – " + nama, "");
										} else {
											String p = "GAGAL : Nilai \"" + nilai + "\" tidak sesuai, untuk NIM = "
													+ nim + ", nama  = " + nama + ", Tanggal = "
													+ Common.dateFormat3.get().format(tgl) + " \n";
											report.gagal(i, nim + " – " + nama, "Nilai tidak sesuai: " + nilai, "Periksa nominal pada baris " + i);
											peringatan.add(p);
										}
									} else {
										String p = "GAGAL : NIM / NO UJIAN / NO REG \"" + nim
												+ "\" tidak ditemukan, nama = " + nama + ", Tanggal = "
												+ Common.dateFormat3.get().format(tgl) + ", nilai = " + nilai + " \n";
										report.gagal(i, nim + " – " + nama, "NIM/NoUjian/NoReg tidak ditemukan", "Periksa identitas pada baris " + i);
										peringatan.add(p);
									}

								}

							}

						} catch (Exception e) {
							Common.tampilErrorJikaAdmin(e);
						}

					}

				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
					peringatan.add(
							"File excel tidak bisa terbaca. Coba buka dulu file tersebut ke Ms. Excel, kemudian save as ke excel 2003. \n\nKemudian upload ulang..");
				}

				try {
					java.io.File rptFile = report.simpanLaporan();
					downloadPath.setValue(rptFile.getAbsolutePath());
				} catch (Exception eR) { ais.common.ErrorAuditUtil.record(eR, "auto-audit(empty-catch) UploadVirtualAccountBRIAction laporan"); }
				label.setValue("");
							} finally {
					ais.database.hibernate.HibernateUtil.closeSession();
				}
			}
		}).start();

	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(UploadVirtualAccount.class);

		if (order)
			criteria.addOrder(Order.desc("id"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
				: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))
				.add(searchTahunAjaran.getSelectedItem() == null
						|| searchTahunAjaran.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("tahunAkademik", searchTahunAjaran.getSelectedItem().getValue()))

				.add(jenisSemester.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("ganjilGenap", jenisSemester.getSelectedItem().getValue()));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<UploadVirtualAccount> uploadVirtualAccount = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(uploadVirtualAccount);
		grid.setRowRenderer(new UploadVirtualAccountRenderer());
		grid.setModelCheckMobile(strset);

	}

}
