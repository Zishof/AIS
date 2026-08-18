package ais.action.master;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.sql.Blob;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.hibernate.Criteria;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.common.Common;
import ais.common.CommonPMB;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.GelombangPendaftaran;
import ais.database.model.JenisSekolahMahasiswaBaru;
import ais.database.model.Jurusan;
import ais.database.model.JurusanSekolahMahasiswaBaru;
import ais.database.model.Paket;
import ais.database.model.PerguruanTinggi;
import ais.database.model.UploadBiodataCalonMahasiswa;
import ais.database.model.file.FileFoto;
import ais.database.model.file.UploadBiodataCalonMahasiswaFileContent;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class UploadBiodataCalonMahasiswaSPANPTKINAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Textbox keterangan;

	private boolean delete = false;

	private MyCheckboxConfig generateNoRegistrasiOlehSistem;
	private UploadBiodataCalonMahasiswa uploadBiodataCalonMahasiswa;
	private MyToolbarbuttonConfig add;
	private Combobox gelombangPendaftaran;
	private Combobox searchTahunAjaran;

	private PerguruanTinggi selectedPerguruanTinggi;

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
		selectedPerguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();
		if (searchTahunAjaran != null) { searchTahunAjaran.setReadonly(true); }
		Common.generateTahunAjaranDanSemua(searchTahunAjaran);
		Common.selectComboItem(searchTahunAjaran, Common.getCurrentTahunAkademik());
		String tahunAkademikPenerimaanMahasiswaBaru = Common
				.getKonfigurasi("tahunAkademikPenerimaanMahasiswaBaru", Common.getCurrentTahunAkademik()).getNilai();

		Common.selectComboItem(searchTahunAjaran, tahunAkademikPenerimaanMahasiswaBaru);

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

	class UploadBiodataCalonMahasiswaRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final UploadBiodataCalonMahasiswa uploadBiodataCalonMahasiswa = (UploadBiodataCalonMahasiswa) arg1;

			RevisiHelper.createNewRevisi(UploadBiodataCalonMahasiswa.class, uploadBiodataCalonMahasiswa,
					uploadBiodataCalonMahasiswa.getNama()).setParent(arg0);
			new Label(uploadBiodataCalonMahasiswa.getTipe()).setParent(arg0);
			new Label(uploadBiodataCalonMahasiswa.getGelombangPendaftaran() == null ? ""
					: uploadBiodataCalonMahasiswa.getGelombangPendaftaran().toString()).setParent(arg0);
			new Label(uploadBiodataCalonMahasiswa.getPaket() == null ? ""
					: uploadBiodataCalonMahasiswa.getPaket().getNama()).setParent(arg0);
			new Label(Common.dateFormat.get().format(uploadBiodataCalonMahasiswa.getTanggal_dirubah())).setParent(arg0);

			new Label(uploadBiodataCalonMahasiswa.getOleh()).setParent(arg0);

			new Label(uploadBiodataCalonMahasiswa.getGenerateNoRegistrasiOlehSistem() ? "Ya" : "Tidak").setParent(arg0);

			new Label(uploadBiodataCalonMahasiswa.getKeterangan()).setParent(arg0);

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

						UploadBiodataCalonMahasiswaFileContent myuploadBiodataCalonMahasiswaFileContent = (UploadBiodataCalonMahasiswaFileContent) session
								.createCriteria(UploadBiodataCalonMahasiswaFileContent.class).setMaxResults(1)
								.addOrder(Order.desc("id"))
								.add(Restrictions.eq("ref", uploadBiodataCalonMahasiswa.getId())).uniqueResult();
						Filedownload.save(myuploadBiodataCalonMahasiswaFileContent.ambilFile(),
								myuploadBiodataCalonMahasiswaFileContent.getFileMimeType());

						StreamingHibernateUtil.getInstance().closeSession();
					} catch (Exception e) {
						StreamingHibernateUtil.getInstance().rollbackTransaction();
						Common.tampilErrorJikaAdmin(e);
					} finally {
						// FIX bocor: session dedikasi (getSessionFactory().openSession(), TIDAK di MAP) tak ditutup
						// oleh StreamingHibernateUtil.closeSession() (yg menutup sesi MAP). Tutup manual di finally.
						if (session != null) {
							try { session.clear(); } catch (Exception eF) { ais.common.ErrorAuditUtil.record(eF, "auto-audit(empty-catch) src/ais/action/master/UploadBiodataCalonMahasiswaSPANPTKINAction.java:179");}
							try { session.disconnect(); } catch (Exception eF) { ais.common.ErrorAuditUtil.record(eF, "auto-audit(empty-catch) src/ais/action/master/UploadBiodataCalonMahasiswaSPANPTKINAction.java:180");}
							try { session.close(); } catch (Exception eF) { ais.common.ErrorAuditUtil.record(eF, "auto-audit(empty-catch) src/ais/action/master/UploadBiodataCalonMahasiswaSPANPTKINAction.java:181");}
						}
					}

				}

			});

			toolbarbutton = new MyToolbarbuttonConfig("Ulangi", "/img/options.png");
			toolbarbutton.setOrient("vertical");
			toolbarbutton.setParent(toolbar);
			toolbarbutton.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							Session session = StreamingHibernateUtil.getInstance().currentSession();

							UploadBiodataCalonMahasiswaFileContent uploadBiodataCalonMahasiswaFileContent = (UploadBiodataCalonMahasiswaFileContent) session
									.createCriteria(UploadBiodataCalonMahasiswaFileContent.class)
									.addOrder(Order.desc("id"))
									.add(Restrictions.eq("ref", uploadBiodataCalonMahasiswa.getId())).setMaxResults(1)
									.uniqueResult();
							if (uploadBiodataCalonMahasiswaFileContent != null) {
								File file = new File(
										Common.REAL_PATH + "/" + uploadBiodataCalonMahasiswaFileContent.getId() + "__"
												+ uploadBiodataCalonMahasiswaFileContent.getClass().getName() + "_"
												+ (uploadBiodataCalonMahasiswaFileContent.getNama() == null ? ""
														: uploadBiodataCalonMahasiswaFileContent.getNama()
																.replaceAll(" ", "_")));
								Common.writeBlobToFile(uploadBiodataCalonMahasiswaFileContent.getFoto(), file);
								// System.out.println("file => " + file);
								String peringatan = uploadFormat1(file, uploadBiodataCalonMahasiswa);

								uploadBiodataCalonMahasiswa.setPeringatan(peringatan);
								Common.refreshUpdate(uploadBiodataCalonMahasiswa);
								Clients.clearBusy();
							} else {
								MyMessageboxConfig.show("File tidak ditemukan");
							}

							StreamingHibernateUtil.getInstance().closeSession();
						}
					}, "Harap tunggu, sedang melakukan proses upload data ..");

				}

			});

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Log", "/img/settings_16x16.png");
			button.setOrient("vertical");
			button.setTooltiptext("Lihat Log");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					final MyWindow window = new MyWindow("Log", "none", true);
					page.getFirstRoot().appendChild(window);
					window.setHeight("95%");
					window.setWidth("90%");

					Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
					borderlayout.setParent(window);
					Center center = new Center();
					center.setParent(borderlayout);
					ais.ui.util.ZkCompat.setFlex(center, true);

					Html html = new ais.ui.util.MyHtml("<font>"
							+ org.apache.commons.lang3.StringUtils.replace(uploadBiodataCalonMahasiswa.getPeringatan(), "\n", "<br>")
							+ "</font>");
					html.setHeight("1500%");
					center.appendChild(html);

					South south = new South();
					ais.ui.util.ZkCompat.setFlex(south, true);
					south.setParent(borderlayout);

					Toolbar toolbar = new Toolbar();
					// toolbar.setHeight("25px");
					toolbar.setParent(south);
					MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
					cancel.setTooltiptext("Tutup");
					cancel.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {
							window.detach();
						}
					});
					cancel.setParent(toolbar);

					window.onModal();
				}

			});
			button.setParent(toolbar);

			button = new MyToolbarbuttonConfig("Edit", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setOrient("vertical");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(uploadBiodataCalonMahasiswa);
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			button.setParent(toolbar);

			button = new MyToolbarbuttonConfig("Hapus", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
			button.setOrient("vertical");
			button.setVisible(delete);
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
													"delete from biodata_calon_mahasiswa where upload_biodata_calon_mahasiswa = "
															+ uploadBiodataCalonMahasiswa.getId())
													.executeUpdate();

											Common.refreshDelete(session, uploadBiodataCalonMahasiswa);
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
		init(new UploadBiodataCalonMahasiswa());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private String namaFile = null;
	private String typeFile = null;
	private File file = null;
	private Label labelNama;
	private Combobox paket;

	private void init(final UploadBiodataCalonMahasiswa uploadBiodataCalonMahasiswa) throws Exception {
		this.uploadBiodataCalonMahasiswa = uploadBiodataCalonMahasiswa;
		addWindow.setTitle("Upload Data Calon Mahasiswa");
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

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("File"));
		MyToolbarbuttonConfig fileupload = new MyToolbarbuttonConfig(
				"Upload Data Calon Mahasiswa" + Common.ukuranLabelFileUpload(), "/img/File-Upload-icon.png");
		fileupload.setUpload(Common.ukuranFileUpload());
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

							file = new File(Common.REAL_PATH + "/tmp/upload_biodata_"
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
		labelNama = new Label(uploadBiodataCalonMahasiswa.getNama());
		row.appendChild(labelNama);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Gelombang Pendaftaran *"));
		gelombangPendaftaran = new Combobox();
		gelombangPendaftaran.setReadonly(true);
		EventListener gelombangEventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Criterion criterion = Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true));

				Common.insertCombo(gelombangPendaftaran, new String[] { "nama", "mulai", "sampai", "jenisSeleksi" },
						"tahunAkademik", GelombangPendaftaran.class, criterion);

				if (uploadBiodataCalonMahasiswa.getGelombangPendaftaran() != null) {
					Common.selectComboItem(gelombangPendaftaran, uploadBiodataCalonMahasiswa.getGelombangPendaftaran());
				} else {
					if (!gelombangPendaftaran.getChildren().isEmpty()) {
						gelombangPendaftaran.setSelectedIndex(0);
					}
				}
			}
		};

		row.appendChild(gelombangPendaftaran);
		gelombangPendaftaran.setWidth("90%");

		gelombangEventListener.onEvent(null);

		if (uploadBiodataCalonMahasiswa.getGelombangPendaftaran() != null) {
			Common.selectComboItem(gelombangPendaftaran, uploadBiodataCalonMahasiswa.getGelombangPendaftaran());
		} else {
			if (!gelombangPendaftaran.getChildren().isEmpty()) {
				gelombangPendaftaran.setSelectedIndex(0);
			}
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Paket"));
		paket = new Combobox();
		paket.setReadonly(true);
		row.appendChild(paket);
		paket.setWidth("90%");
		Common.insertCombo(paket, "nama", Paket.class,
				selectedPerguruanTinggi == null || selectedPerguruanTinggi.getId() == null
						? Restrictions.sqlRestriction("true")
						: Restrictions.or(Restrictions.eq("perguruanTinggi", selectedPerguruanTinggi),
								Restrictions.isNull("perguruanTinggi")),
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.selectComboItem(true, paket, uploadBiodataCalonMahasiswa.getPaket());

		Common.initKeterangan(rows, "Kosongkan paket jika berlaku tidak hanya salah satu paket");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(
				generateNoRegistrasiOlehSistem = new MyCheckboxConfig("Generate No. Registrasi otomatis oleh sistem"));
		generateNoRegistrasiOlehSistem.setChecked(uploadBiodataCalonMahasiswa.getGenerateNoRegistrasiOlehSistem());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(uploadBiodataCalonMahasiswa.getKeterangan() == null ? ""
				: uploadBiodataCalonMahasiswa.getKeterangan()));
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

		if (gelombangPendaftaran.getSelectedItem() == null) {
			MyMessageboxConfig.show("Pilih salah satu gelombang", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		// GelombangPendaftaran gel = (GelombangPendaftaran)
		// gelombangPendaftaran.getSelectedItem().getValue();
		// if (gel.getJenjang() == null) {
		// MyMessageboxConfig.show("Tidak boleh memilih gelombang yang
		// jenjang-nya kosong..", "Peringatan",
		// MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
		// return false;
		// }

		Session session = HibernateUtil.currentSession();
		if (uploadBiodataCalonMahasiswa.getId() != null) {
			uploadBiodataCalonMahasiswa = (UploadBiodataCalonMahasiswa) session.load(UploadBiodataCalonMahasiswa.class,
					uploadBiodataCalonMahasiswa.getId());
		}

		uploadBiodataCalonMahasiswa.setGenerateNoRegistrasiOlehSistem(generateNoRegistrasiOlehSistem.isChecked());
		uploadBiodataCalonMahasiswa.setNama(namaFile);
		uploadBiodataCalonMahasiswa.setTipe(typeFile);
		uploadBiodataCalonMahasiswa.setKeterangan(keterangan.getValue());
		uploadBiodataCalonMahasiswa
				.setGelombangPendaftaran((GelombangPendaftaran) gelombangPendaftaran.getSelectedItem().getValue());
		uploadBiodataCalonMahasiswa
				.setPaket((Paket) (paket.getSelectedItem() == null ? null : paket.getSelectedItem().getValue()));

		if (uploadBiodataCalonMahasiswa.getId() != null) {
			session.update(uploadBiodataCalonMahasiswa);
		} else {
			session.save(uploadBiodataCalonMahasiswa);
		}

		if (file != null) {
			Common.createDefaultTimer(new EventListener() {

				@SuppressWarnings("deprecation")
				@Override
				public void onEvent(Event arg0) throws Exception {

					Session session = StreamingHibernateUtil.getInstance().currentSession();

					UploadBiodataCalonMahasiswaFileContent uploadBiodataCalonMahasiswaFileContent = new UploadBiodataCalonMahasiswaFileContent();

					Blob blob = new javax.sql.rowset.serial.SerialBlob(IOUtils.toByteArray(new FileInputStream(file)));
					uploadBiodataCalonMahasiswaFileContent.setFoto(blob);
					uploadBiodataCalonMahasiswaFileContent.setNama(uploadBiodataCalonMahasiswa.getNama());
					uploadBiodataCalonMahasiswaFileContent.setFileMimeType(uploadBiodataCalonMahasiswa.getTipe());

					uploadBiodataCalonMahasiswaFileContent.setRef(uploadBiodataCalonMahasiswa.getId());
					uploadBiodataCalonMahasiswaFileContent.setUploadDate(ais.ui.util.WaktuUtil.getDate());
					uploadBiodataCalonMahasiswaFileContent.setRef(uploadBiodataCalonMahasiswa.getId());

					session.getTransaction().begin();
					session.save(uploadBiodataCalonMahasiswaFileContent);
					session.getTransaction().commit();
					StreamingHibernateUtil.getInstance().closeSession();

					String peringatan = uploadFormat1(file, uploadBiodataCalonMahasiswa);

					uploadBiodataCalonMahasiswa.setPeringatan(peringatan);
					Common.refreshUpdate(uploadBiodataCalonMahasiswa);
					Clients.clearBusy();
				}
			}, "Harap tunggu, sedang melakukan proses upload data ..");
		}
		return true;
	}

	public void onDownload(Event event) throws Exception {
		Filedownload.save(new File(application.getRealPath("/help/format_upload_calon_mahasiswa.xlsx")),
				"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
	}

	public String upload(File file, UploadBiodataCalonMahasiswa uploadBiodataCalonMahasiswa) throws Exception {

		XSSFWorkbook workbook = new XSSFWorkbook(file.getAbsolutePath());
		XSSFSheet sheet = workbook.getSheetAt(0);

		String prefix = Common.getKonfigurasi("prefix_no_registrasi_upload", "").getNilai();

		String peringatan = "";
		int jumlahBerhasil = 0;
		ais.common.UploadReportHelper report = new ais.common.UploadReportHelper("Upload Biodata Calon Mahasiswa SPANPTKIN");
		for (int i = 1; i < (sheet.getLastRowNum() + 1); i++) {
			try {

				String s = Common.getSheetContentAsString(sheet, 0, i);
				if (s == null || s.trim().isEmpty()) {
					break;
				}
				String noRegistrasi = Common.getSheetContentAsString(sheet, 1, i);

				String nama = Common.getSheetContentAsString(sheet, 2, i);
				// String kodeProdi = Common.getSheetContentAsString(sheet, 3,
				// i);
				String namaProdi = Common.getSheetContentAsString(sheet, 4, i);
				// String kodeSekolah = Common
				// .getSheetContentAsString(sheet, 5, i);
				String namaSekolah = Common.getSheetContentAsString(sheet, 6, i);

				if (noRegistrasi != null && nama != null && !noRegistrasi.trim().isEmpty() && !nama.trim().isEmpty()) {

					Session session = HibernateUtil.currentNativeSession();
					Jurusan jurusan = (Jurusan) session.createCriteria(Jurusan.class)
							.add(Restrictions.eq("jenjang",
									uploadBiodataCalonMahasiswa.getGelombangPendaftaran().getJenjang() == null
											? Restrictions.sqlRestriction("true")
											: uploadBiodataCalonMahasiswa.getGelombangPendaftaran().getJenjang()))
							.add(Restrictions.ilike("nama", namaProdi.trim(), MatchMode.EXACT)).setMaxResults(1)
							.uniqueResult();

					if (jurusan != null) {

						BiodataCalonMahasiswa biodataCalonMahasiswa = (BiodataCalonMahasiswa) session
								.createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
								.add(Restrictions.eq("noRegistrasi", noRegistrasi))
								.add(Restrictions.eq("gelombangPendaftaran",
										uploadBiodataCalonMahasiswa.getGelombangPendaftaran()))
								.setMaxResults(1).uniqueResult();
						if (biodataCalonMahasiswa == null) {
							biodataCalonMahasiswa = new BiodataCalonMahasiswa();
						}

						biodataCalonMahasiswa
								.setGelombangPendaftaran(uploadBiodataCalonMahasiswa.getGelombangPendaftaran());
						biodataCalonMahasiswa.setNama(nama);
						biodataCalonMahasiswa.setAsalSma(namaSekolah);
						biodataCalonMahasiswa.setProdi1(jurusan);
						biodataCalonMahasiswa.setProdiLulus(jurusan);
						biodataCalonMahasiswa.setNoUjian(noRegistrasi);
						biodataCalonMahasiswa.setUploadBiodataCalonMahasiswa(uploadBiodataCalonMahasiswa);

						if (uploadBiodataCalonMahasiswa.getGenerateNoRegistrasiOlehSistem()) {
							noRegistrasi = CommonPMB.generateNoRegistrasi(biodataCalonMahasiswa);
						}
						biodataCalonMahasiswa.setNoRegistrasi(prefix + noRegistrasi);

						session.getTransaction().begin();
						session.saveOrUpdate(biodataCalonMahasiswa);
						session.getTransaction().commit();

						jumlahBerhasil++;
						report.sukses(i, noRegistrasi + "/" + nama, "jumlahBerhasil=" + jumlahBerhasil);

					} else {
						peringatan += "Prodi \"" + namaProdi + "\", jenjang \""
								+ uploadBiodataCalonMahasiswa.getGelombangPendaftaran().getJenjang().getNama()
								+ "\" tidak ditemukan untuk No Pendaftaran " + noRegistrasi
								+ ", nama calon mahasiswa = " + nama + ", asal sekolah = " + namaSekolah + "\n";
						report.gagal(i, noRegistrasi + "/" + nama, "Prodi tidak ditemukan: " + namaProdi, "Periksa kode/nama prodi baris " + i);
					}

					HibernateUtil.closeSession();
				}

			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
				report.gagal(i, "row " + i, e, "Periksa data calon mahasiswa baris " + i);
			}

		}

		try { Filedownload.save(report.simpanLaporan(), "text/plain"); }
		catch (Exception eDl) { ais.common.ErrorAuditUtil.record(eDl, "auto-audit(empty-catch) download laporan upload()"); }

		MyMessageboxConfig.show(
				"Upload data calon mahasiswa selesai dilakukan.\n" + "Jumlah data ter-upload = "
						+ Common.numberFormat.get().format(jumlahBerhasil) + ".\n\n"
						+ (peringatan.isEmpty() ? "" : "\n" + peringatan)
						+ report.getRingkasan(),
				"Pemberitahuan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);

		return peringatan;
	}

	public String uploadFormat1(File file, UploadBiodataCalonMahasiswa uploadBiodataCalonMahasiswa) throws Exception {

		// FIX defensif: bila writeBlobToFile gagal menulis file secara lengkap (mis. karena
		// PSQLException "Large Objects may not be used in auto-commit mode" saat membaca Blob),
		// file di sini bisa jadi tidak ada atau berukuran 0 byte. XSSFWorkbook akan melempar
		// InvalidOperationException teknis yang membingungkan user; tampilkan pesan yang jelas
		// alih-alih itu.
		if (file == null || !file.exists() || file.length() == 0) {
			MyMessageboxConfig.show("Berkas format upload gagal disiapkan, silakan coba upload ulang.");
			return "Berkas format upload gagal disiapkan (file tidak ditemukan/kosong): "
					+ (file == null ? "-" : file.getAbsolutePath());
		}

		XSSFWorkbook workbook = new XSSFWorkbook(file.getAbsolutePath());
		XSSFSheet sheet = workbook.getSheetAt(0);
		String prefix = Common.getKonfigurasi("prefix_no_registrasi_upload", "").getNilai();
		String peringatan = "";
		int jumlahBerhasil = 0;
		ais.common.UploadReportHelper report = new ais.common.UploadReportHelper("Upload Biodata Calon Mahasiswa Format1 SPANPTKIN");
		for (int i = 1; i < (sheet.getLastRowNum() + 1); i++) {
			try {

				String s = Common.getSheetContentAsString(sheet, 1, i);
				if (s == null || s.trim().isEmpty() || !Common.isNumber(s)) {
					continue;
				}
				String noRegistrasi = Common.getSheetContentAsString(sheet, 2, i);

				String nama = Common.getSheetContentAsString(sheet, 3, i);
				String jk = Common.getSheetContentAsString(sheet, 4, i);
				String kodeProdi = Common.getSheetContentAsString(sheet, 5, i);
				String namaProdi = Common.getSheetContentAsString(sheet, 6, i);
				String tempatLahir = Common.getSheetContentAsString(sheet, 7, i);
				String tglLahir = Common.getSheetContentAsString(sheet, 8, i);
				String jenisSekolah = Common.getSheetContentAsString(sheet, 9, i);
				String namaSekolah = Common.getSheetContentAsString(sheet, 11, i);
				String jurusanSekolah = Common.getSheetContentAsString(sheet, 12, i);
				if (noRegistrasi != null && nama != null && !noRegistrasi.trim().isEmpty() && !nama.trim().isEmpty()) {

					Session session = HibernateUtil.currentNativeSession();
					Jurusan jurusan = (Jurusan) session.createCriteria(Jurusan.class)
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
							.add(Restrictions.ilike("kodeEpsbed", kodeProdi.trim(), MatchMode.EXACT)).setMaxResults(1)
							.uniqueResult();
					if (jurusan == null) {
						jurusan = (Jurusan) session.createCriteria(Jurusan.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
								.add(uploadBiodataCalonMahasiswa.getGelombangPendaftaran().getJenjang() == null
										? Restrictions.sqlRestriction("true")
										: Restrictions.eq("jenjang",
												uploadBiodataCalonMahasiswa.getGelombangPendaftaran().getJenjang()))
								.add(Restrictions.ilike("nama", namaProdi.trim(), MatchMode.EXACT)).setMaxResults(1)
								.uniqueResult();
					}

					if (jurusan != null) {

						BiodataCalonMahasiswa biodataCalonMahasiswa = (BiodataCalonMahasiswa) session
								.createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
								.add(Restrictions.eq("noRegistrasi", noRegistrasi))
								.add(Restrictions.eq("gelombangPendaftaran",
										uploadBiodataCalonMahasiswa.getGelombangPendaftaran()))
								.setMaxResults(1).uniqueResult();
						if (biodataCalonMahasiswa == null) {
							biodataCalonMahasiswa = new BiodataCalonMahasiswa();
						}

						biodataCalonMahasiswa
								.setGelombangPendaftaran(uploadBiodataCalonMahasiswa.getGelombangPendaftaran());
						biodataCalonMahasiswa.setNama(nama);
						biodataCalonMahasiswa.setJurusanSekolah((JurusanSekolahMahasiswaBaru) session
								.createCriteria(JurusanSekolahMahasiswaBaru.class)
								.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
								.add(Restrictions.ilike("nama", jurusanSekolah, MatchMode.EXACT)).setMaxResults(1)
								.uniqueResult());
						biodataCalonMahasiswa.setJenisSekolah((JenisSekolahMahasiswaBaru) session
								.createCriteria(JenisSekolahMahasiswaBaru.class)
								.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
								.add(Restrictions.ilike("nama", jenisSekolah, MatchMode.EXACT)).setMaxResults(1)
								.uniqueResult());
						biodataCalonMahasiswa.setTempatLahir(tempatLahir);
						try {
							biodataCalonMahasiswa.setTanggalLahir(Common.databaseDateFormat.get().parse(tglLahir));
						} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

						biodataCalonMahasiswa.setAsalSma(namaSekolah);
						biodataCalonMahasiswa.setProdi1(jurusan);
						biodataCalonMahasiswa.setProdiLulus(jurusan);

						biodataCalonMahasiswa
								.setJenisKelamin(jk.trim().equalsIgnoreCase("L") ? "Laki-laki" : "Perempuan");
						biodataCalonMahasiswa.setNoUjian(noRegistrasi);
						biodataCalonMahasiswa.setUploadBiodataCalonMahasiswa(uploadBiodataCalonMahasiswa);

						if (uploadBiodataCalonMahasiswa.getGenerateNoRegistrasiOlehSistem()) {
							noRegistrasi = CommonPMB.generateNoRegistrasi(biodataCalonMahasiswa);
						}
						biodataCalonMahasiswa.setNoRegistrasi(prefix + noRegistrasi);

						session.getTransaction().begin();
						session.saveOrUpdate(biodataCalonMahasiswa);
						session.getTransaction().commit();

						jumlahBerhasil++;
						report.sukses(i, noRegistrasi + "/" + nama, "jumlahBerhasil=" + jumlahBerhasil);

					} else {
						peringatan += "Prodi \"" + namaProdi + "\", "
								+ (uploadBiodataCalonMahasiswa.getGelombangPendaftaran().getJenjang() == null ? ""
										: ", jenjang \"" + uploadBiodataCalonMahasiswa.getGelombangPendaftaran()
												.getJenjang().getNama() + "\" ")
								+ " tidak ditemukan untuk No Pendaftaran " + noRegistrasi + ", nama calon mahasiswa = "
								+ nama + ", asal sekolah = " + namaSekolah + "\n";
						report.gagal(i, noRegistrasi + "/" + nama, "Prodi tidak ditemukan: " + namaProdi, "Periksa kode/nama prodi baris " + i);
					}

					HibernateUtil.closeSession();
				}

			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
				report.gagal(i, "row " + i, e, "Periksa data calon mahasiswa baris " + i);
			}

		}

		try { Filedownload.save(report.simpanLaporan(), "text/plain"); }
		catch (Exception eDl) { ais.common.ErrorAuditUtil.record(eDl, "auto-audit(empty-catch) download laporan uploadFormat1()"); }

		MyMessageboxConfig.show(
				"Upload data calon mahasiswa selesai dilakukan.\n" + "Jumlah data ter-upload = "
						+ Common.numberFormat.get().format(jumlahBerhasil) + ".\n\n"
						+ (peringatan.isEmpty() ? "" : "\n" + peringatan)
						+ report.getRingkasan(),
				"Pemberitahuan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);

		return peringatan;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(UploadBiodataCalonMahasiswa.class)
				.createAlias("gelombangPendaftaran", "gelombangPendaftaran");

		if (order)
			criteria.addOrder(Order.desc("id"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("nama", searchnama.getValue(), MatchMode.ANYWHERE))
				.add(searchTahunAjaran.getSelectedItem() == null
						|| searchTahunAjaran.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("true")
								: Restrictions.eq("gelombangPendaftaran.tahunAkademik",
										searchTahunAjaran.getSelectedItem().getValue()));

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<UploadBiodataCalonMahasiswa> uploadBiodataCalonMahasiswa = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(uploadBiodataCalonMahasiswa);
		grid.setRowRenderer(new UploadBiodataCalonMahasiswaRenderer());
		grid.setModelCheckMobile(strset);

	}

}
