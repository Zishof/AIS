package ais.action.master.helper;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.pdfbox.util.PDFMergerUtility;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;

import ais.action.master.helper.generic.AmbilDataMahasiswaBanyak;
import ais.action.report.Report;
import ais.action.report.format1.akademik.LaporanIjazahAkademik;
import ais.action.report.format1.akademik.LaporanTranskipAkademik;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.KelompokStatusKeluarMahasiswa;
import ais.database.model.Mahasiswa;
import ais.ui.util.DataCriteria;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;

public class KelompokStatusKeluarMahasiswaDetailAction extends MyDetail implements DataCriteria {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5086031585928643232L;

	private KelompokStatusKeluarMahasiswa kelompokStatusKeluarMahasiswa;
	private MyGrid grid;

	private Textbox pencarian;

	public KelompokStatusKeluarMahasiswaDetailAction(KelompokStatusKeluarMahasiswa kelompokStatusKeluarMahasiswa) {
		super();
		this.kelompokStatusKeluarMahasiswa = kelompokStatusKeluarMahasiswa;
		this.addEventListener(Events.ON_OPEN, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(KelompokStatusKeluarMahasiswaDetailAction.this);
				if (isOpen()) {
					display();
				}
			}
		});
	}

	class MahasiswaRenderer extends ais.ui.util.MyRowRenderer {

		public MahasiswaRenderer() {

		}

		@Override
		public void render(final Row arg0, Object data) throws Exception {
			// TODO Auto-generated method stub
			final Mahasiswa mahasiswa = (Mahasiswa) data;

			CommonMedia.tampilkanGambarKecil(mahasiswa).setParent(arg0);
			new Label(mahasiswa.getNim()).setParent(arg0);

			RevisiHelper.createNewRevisi(Mahasiswa.class, mahasiswa, mahasiswa.getNama()).setParent(arg0);

			new Label(mahasiswa.getTahunangkatan() + "").setParent(arg0);

			new Label(mahasiswa.getJurusan() == null ? "" : mahasiswa.getJurusan().getNama()).setParent(arg0);

			new Label(mahasiswa.getSemesterLulus() + "").setParent(arg0);

			final MyTextbox noIjazah1 = new MyTextbox(mahasiswa.getNoIjazah1());
			noIjazah1.setWidth("95%");

			final MyTextbox noIjazah2 = new MyTextbox(mahasiswa.getNoIjazah2());
			noIjazah2.setWidth("95%");

			final MyTextbox noAkta1 = new MyTextbox(mahasiswa.getNoAkta1());
			noAkta1.setWidth("95%");

			final MyTextbox noAkta2 = new MyTextbox(mahasiswa.getNoAkta2());
			noAkta2.setWidth("95%");

			final MyDatebox tanggalWisuda = new MyDatebox(mahasiswa.getTanggalWisuda());
			tanggalWisuda.setWidth("95%");

			final MyDatebox tanggalSkRektor = new MyDatebox(mahasiswa.getTanggalSkRektor());
			tanggalSkRektor.setWidth("95%");

			final MyDatebox tanggalYudisium = new MyDatebox(mahasiswa.getTanggalYudisium());
			tanggalYudisium.setWidth("95%");

			final MyTextbox skDo = new MyTextbox(mahasiswa.getSkDo());
			skDo.setWidth("95%");

			final MyTextbox nomorSkpi = new MyTextbox(mahasiswa.getNomorSkpi());
			nomorSkpi.setWidth("95%");

			EventListener eventListener = new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					mahasiswa.setNoIjazah1(noIjazah1.getValue().trim());
					mahasiswa.setNoIjazah2(noIjazah2.getValue().trim());
					mahasiswa.setNoAkta1(noAkta1.getValue().trim());
					mahasiswa.setNoAkta2(noAkta2.getValue().trim());
					mahasiswa.setTanggalWisuda(tanggalWisuda.getValue());
					mahasiswa.setTanggalSkRektor(tanggalSkRektor.getValue());
					mahasiswa.setTanggalYudisium(tanggalYudisium.getValue());
					mahasiswa.setSkDo(skDo.getValue().trim());
					mahasiswa.setNomorSkpi(nomorSkpi.getValue().trim());
					Common.refreshUpdate(mahasiswa);
				}
			};

			noIjazah1.addEventListener("onChange", eventListener);
			noIjazah2.addEventListener("onChange", eventListener);
			noAkta1.addEventListener("onChange", eventListener);
			noAkta2.addEventListener("onChange", eventListener);

			tanggalWisuda.addEventListener("onChange", eventListener);
			tanggalYudisium.addEventListener("onChange", eventListener);

			tanggalSkRektor.addEventListener("onChange", eventListener);
			tanggalYudisium.addEventListener("onChange", eventListener);
			skDo.addEventListener("onChange", eventListener);
			nomorSkpi.addEventListener("onChange", eventListener);

			noIjazah1.setParent(arg0);
			noIjazah2.setParent(arg0);
			noAkta1.setParent(arg0);
			noAkta2.setParent(arg0);

			tanggalWisuda.setParent(arg0);
			tanggalYudisium.setParent(arg0);

			tanggalSkRektor.setParent(arg0);

			tanggalYudisium.setParent(arg0);

			skDo.setParent(arg0);

			nomorSkpi.setParent(arg0);

			Hbox hbox = new Hbox();
			hbox.setParent(arg0);

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Transkrip", "/img/svg/printer.svg");
			button.setVisible(mahasiswa.getKelompokStatusKeluarMahasiswa() != null);
			button.setOrient("vertical");
			button.setTooltiptext("Hapus Data");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					LaporanTranskipAkademik laporanTranskipAkademik = new LaporanTranskipAkademik(mahasiswa);
					laporanTranskipAkademik.setTitle("Transkrip");
					laporanTranskipAkademik.setClosable(true);
					laporanTranskipAkademik.setBorder("none");
					laporanTranskipAkademik.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
					laporanTranskipAkademik.setHeight("95%");
					laporanTranskipAkademik.setWidth("90%");
					laporanTranskipAkademik.onModal();

				}
			});
			button.setParent(hbox);

			button = new MyToolbarbuttonConfig("Hapus", "/img/svg/trash.svg");
			button.setVisible(mahasiswa.getKelompokStatusKeluarMahasiswa() != null);
			button.setOrient("vertical");
			button.setTooltiptext("Hapus Data");
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
											mahasiswa.setKelompokStatusKeluarMahasiswa(null);
											Common.refreshSaveOrUpdate(mahasiswa);
											Common.createDefaultTimer(new EventListener() {

												@Override
												public void onEvent(Event arg0) throws Exception {
													loadData(null);
												}
											});
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											PesanFormalHelper.tampilkanGagalException("Menghapus data", "Data yang Bapak/Ibu coba hapus kemungkinan besar masih memiliki keterkaitan/relasi dengan data lain pada tabel terkait (misalnya digunakan sebagai referensi oleh transaksi, detail, atau riwayat lain), sehingga sistem basis data menolak proses penghapusan ini demi menjaga integritas data secara keseluruhan.", e, new String[]{"Periksa kembali apakah data ini masih digunakan atau direferensikan oleh data lain yang berelasi.", "Hapus atau lepaskan terlebih dahulu keterkaitan/relasi data tersebut sebelum mencoba menghapus data ini kembali.", "Jika Bapak/Ibu yakin data ini seharusnya sudah tidak digunakan lagi, hubungi Administrator untuk pengecekan lebih lanjut."});
										}

									}

								}
							});

				}
			});
			button.setParent(hbox);
		}
	}

	private List<Mahasiswa> mahasiswas = null;

	@SuppressWarnings("unchecked")
	public void loadData(Object value) {

		mahasiswas = ConstantValues.simpleList(initCriteria(true).setMaxResults(1500), Mahasiswa.class);

		ListModel strset = new SimpleListModel(mahasiswas);
		grid.setRowRenderer(new MahasiswaRenderer());
		grid.setModelCheckMobile(strset);

	}

	public void display() {

		ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
		groupbox.setStyle("min-height: 200px;");
		groupbox.setParent(this);
		groupbox.appendChild(
				new MyCaptionStyled("Daftar mahasiswa yang masuk kelompok " + kelompokStatusKeluarMahasiswa.getNama()));
		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(groupbox);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Ambil Data Mahasiswa", "/img/add_item.png");
		button.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {

				AmbilDataMahasiswaBanyak window = new AmbilDataMahasiswaBanyak(mahasiswas);

				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);
				window.setWidth("90%");
				window.setHeight("90%");

				window.setEventListener(new EventListener() {

					@Override
					public void onEvent(final Event dataCalonMhs) throws Exception {

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								List<Mahasiswa> mahasiswas = (List<Mahasiswa>) dataCalonMhs.getData();

								if (mahasiswas != null) {
									Session session = HibernateUtil.currentSession();
									for (Mahasiswa mahasiswa : mahasiswas) {
										mahasiswa.setKelompokStatusKeluarMahasiswa(kelompokStatusKeluarMahasiswa);
										Common.refreshUpdate(session, mahasiswa);
									}

									loadData(null);
								}
							}
						});

					}
				});

				window.onModal();

			}

		});
		button.setParent(toolbar);

		pencarian = new Textbox();
		pencarian.setCols(8);
		pencarian.setParent(toolbar);
		pencarian.addEventListener("onOK", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});

		MyToolbarbuttonConfig cari = new MyToolbarbuttonConfig("Refresh", "/img/Button-Refresh-icon.png");
		cari.setParent(toolbar);
		cari.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});

		button = new MyToolbarbuttonConfig("Ijazah", "/img/svg/file-pdf.svg");
		button.setTooltiptext("Hapus Data");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				final PDFMergerUtility ut = new PDFMergerUtility();
				final File filePdfBaru = new File(Common.ambilREAL_PATH_REPORT() + "/" + Common.getGeneratedBarCode() + ".pdf");
				final Label label = Common.displayLoadBar(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						ut.setDestinationStream(new FileOutputStream(filePdfBaru));
						ut.mergeDocuments();

						Report.tampil(filePdfBaru);
					}
				});

				new Thread(new Runnable() {

					@SuppressWarnings("rawtypes")
					@Override
					public void run() {
						int index = 0;
						int size = mahasiswas.size();
						for (Mahasiswa mahasiswa : mahasiswas) {
							index++;
							label.setValue("Memperoses data " + mahasiswa.getNama() + " ("
									+ Common.numberFormat.get().format(((index * 1.0 )/ (size * 1.0 )) * 100.0) + "%)");
							try {
								Map parameters = LaporanIjazahAkademik.parameterIjazah(mahasiswa, null, null, false);
								File file = Report.generateFileReport(Report.PDF, parameters, "Ijazah",
										ais.ui.util.WaktuUtil.getDate(), new Toolbar());
								ut.addSource(file);
							} catch (Exception e) {
								// TODO Auto-generated catch block
								e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/KelompokStatusKeluarMahasiswaDetailAction.java:382");
							}

						}

						label.setValue("");
					}
				}).start();

			}
		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Transkrip", "/img/svg/journal-bookmark.svg");
		button.setTooltiptext("Hapus Data");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				final MyWindow window = new MyWindow("Pilih Jenis Transkrip", "none", true);
				window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				window.setHeight("250px");
				window.setWidth("500px");

				Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
				borderlayout.setParent(window);

				Center a = new Center();
				a.setParent(borderlayout);

				MyGrid grid = new MyGrid();
				grid.setWidth("100%");
				grid.setParent(a);
				grid.setHeight("100%");

				Columns columns = new Columns();
				columns.setParent(grid);
				MyColumnConfig column = new MyColumnConfig();
				column.setWidth("30%");
				column.setParent(columns);
				column = new MyColumnConfig();
				column.setParent(columns);

				Rows rows = new Rows();
				rows.setParent(grid);

				final Combobox jenis = new Combobox();
				Comboitem comboitem = new MyComboitemConfig("Transkrip Akademik");
				comboitem.setValue("Transkrip_Akademik");
				jenis.appendChild(comboitem);

				comboitem = new MyComboitemConfig("Transkrip 2 Kolom");
				comboitem.setValue("Rekaman_Nilai_2_Kolom");
				jenis.appendChild(comboitem);

				comboitem = new MyComboitemConfig("Transkrip 2 Halaman");
				comboitem.setValue("report1");
				jenis.appendChild(comboitem);

				comboitem = new MyComboitemConfig("Transkrip 4 Kolom");
				comboitem.setValue("Report6");
				jenis.appendChild(comboitem);

				comboitem = new MyComboitemConfig("Transkrip IPK");
				comboitem.setValue("Rekaman_Nilai");
				jenis.appendChild(comboitem);

				comboitem = new MyComboitemConfig("IPK berdasar Kelompok");
				comboitem.setValue("Rekaman_Nilai_Kelompok");
				jenis.appendChild(comboitem);

				MyFormRow row = new MyFormRow();
				row.setValign("top");
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Jenis *"));
				row.appendChild(jenis);

				jenis.setWidth("95%");
				jenis.setSelectedIndex(0);
				jenis.setReadonly(true);

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
						window.detach();
					}
				});
				cancel.setParent(toolbar);
				MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Cetak Transkrip",
						"/img/svg/journal-bookmark.svg");
				save.setTooltiptext("Lanjut Cetak Transkrip");
				save.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						final String namaFile = (String) jenis.getSelectedItem().getValue();
						final PDFMergerUtility ut = new PDFMergerUtility();
						final File filePdfBaru = new File(
								Common.ambilREAL_PATH_REPORT() + "/" + Common.getGeneratedBarCode() + ".pdf");
						final Label label = Common.displayLoadBar(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								ut.setDestinationStream(new FileOutputStream(filePdfBaru));
								ut.mergeDocuments();

								Report.tampil(filePdfBaru);
							}
						});

						new Thread(new Runnable() {

							@SuppressWarnings("rawtypes")
							@Override
							public void run() {
								int index = 0;
								int size = mahasiswas.size();
								for (Mahasiswa mahasiswa : mahasiswas) {
									index++;
									label.setValue("Memperoses data " + mahasiswa.getNama() + " ("
											+ Common.numberFormat.get().format(((index * 1.0 )/ (size * 1.0 )) * 100.0) + "%)");
									try {
										Map parameters = LaporanTranskipAkademik.generateParameter(mahasiswa,
												mahasiswa.currentSemester(), false, false, WaktuUtil.getDate(),
												WaktuUtil.getDate());
										File file = Report.generateFileReport(Report.PDF, parameters, namaFile,
												ais.ui.util.WaktuUtil.getDate(), new Toolbar());
										ut.addSource(file);
									} catch (Exception e) {
										// TODO Auto-generated catch block
										e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/KelompokStatusKeluarMahasiswaDetailAction.java:522");
									}

								}

								label.setValue("");
							}
						}).start();

					}
				});
				save.setParent(toolbar);

				window.onModal();

			}
		});
		button.setParent(toolbar);

		String[] contents = new String[] { "nim", "nama", "jurusan.nama", "tahunangkatan", "noIjazah1", "noIjazah2",
				"noAkta1", "noAkta2", "tanggalWisuda", "tanggalSkRektor", "tanggalYudisium", "skDo", "nomorSkpi" };

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(Mahasiswa.class, this, contents);
		toolbar.appendChild(cetakToolbarbutton);

		MyToolbarbuttonConfig upload = new MyToolbarbuttonConfig("Upload " + Common.ukuranLabelFileUpload(),
				"/img/excel.png");
		upload.setUpload(Common.ukuranFileUpload());
		upload.addEventListener("onUpload", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				UploadEvent uploadEvent = (UploadEvent) event;
				Media media = uploadEvent.getMedia();if(!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media))return;
				if (media.getName().toLowerCase().endsWith("xlsx")) {

					InputStream inputStream = media.getStreamData();
					// System.out.println("media = " + media);
					final File file = new File(
							Sessions.getCurrent().getWebApp().getRealPath("/temp/" + media.getName()));
					// System.out.println("file = " + file.getAbsolutePath());
					file.getParentFile().mkdirs();
					FileOutputStream fileOutputStream = new FileOutputStream(file);
					int c;
					while ((c = inputStream.read()) != -1) {
						fileOutputStream.write(c);
					}
					fileOutputStream.close();
					inputStream.close();

					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							uploadDataMahasiswa(file, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									loadData(arg0);
									Clients.clearBusy();
								}
							});
						}
					}, "Harap tunggu.. sedang melakukan proses upload data..");

				} else {
					MyMessageboxConfig.show(
							"File yang anda upload harus ber-format Excel Open XML Spreadsheet (xlsx). Jika masih menggunakan format lain, buka file excel tersebut, kemudian Save As Excel Open XML Spreadsheet (xlsx). "
									+ media,
							"Error", MyMessageboxConfig.OK, MyMessageboxConfig.ERROR);
				}
			}
		});
		toolbar.appendChild(upload);

		button = new MyToolbarbuttonConfig("Hapus Semua", "/img/svg/trash.svg");
		button.setTooltiptext("Hapus Data");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
						MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {

							@SuppressWarnings("unchecked")
							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {
									try {
										List<Mahasiswa> mahasiswas = ConstantValues
												.simpleList(initCriteria(true).setMaxResults(5000), Mahasiswa.class);
										for (Mahasiswa mahasiswa : mahasiswas) {
											mahasiswa.setKelompokStatusKeluarMahasiswa(null);
											Common.refreshSaveOrUpdate(mahasiswa);
										}
										Common.createDefaultTimer(new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {
												loadData(null);
											}
										});
									} catch (Exception e) {
										Common.tampilErrorJikaAdmin(e);
										PesanFormalHelper.tampilkanGagalException("Menghapus data", "Data yang Bapak/Ibu coba hapus kemungkinan besar masih memiliki keterkaitan/relasi dengan data lain pada tabel terkait (misalnya digunakan sebagai referensi oleh transaksi, detail, atau riwayat lain), sehingga sistem basis data menolak proses penghapusan ini demi menjaga integritas data secara keseluruhan.", e, new String[]{"Periksa kembali apakah data ini masih digunakan atau direferensikan oleh data lain yang berelasi.", "Hapus atau lepaskan terlebih dahulu keterkaitan/relasi data tersebut sebelum mencoba menghapus data ini kembali.", "Jika Bapak/Ibu yakin data ini seharusnya sudah tidak digunakan lagi, hubungi Administrator untuk pengecekan lebih lanjut."});
									}

								}

							}
						});

			}
		});
		button.setParent(toolbar);

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(50);
		grid.getPagingChild().setMold("os");
		grid.setParent(groupbox);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Foto");
		column.setWidth("70px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("NIM");
		column.setWidth("7%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");
		column.setWidth("9%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Angkatan");
		column.setWidth("5%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Prodi");
		column.setWidth("8%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Smt Lulus");
		column.setWidth("5%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("No.Ijazah I");
		column.setWidth("8%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("No.Ijazah II");
		column.setWidth("8%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("No.Transkrip");
		column.setWidth("8%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("No.SK");
		column.setWidth("8%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Tgl.Wisuda");
		column.setWidth("8%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Tgl.SK");
		column.setWidth("8%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Tgl.Yudisium");
		column.setWidth("8%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("No.DO");
		column.setWidth("8%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("No.SKPI");
		column.setWidth("8%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("10%");

		loadData(null);
	}

	@Override
	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		return session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

				.add(pencarian.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.or(Restrictions.ilike("nama", pencarian.getValue().trim(), MatchMode.ANYWHERE),
								Restrictions.ilike("nim", pencarian.getValue().trim(), MatchMode.ANYWHERE)))

				.addOrder(Order.desc("id"))
				.add(Restrictions.eq("kelompokStatusKeluarMahasiswa", kelompokStatusKeluarMahasiswa));
	}

	public void uploadDataMahasiswa(final File file, final EventListener eventListener) throws Exception {

		/*
		 * LAPORAN UPLOAD. Sebelumnya baris yang NIM-nya tidak dikenali dilewati DIAM-DIAM
		 * (blok "if (mahasiswa != null ...)" tanpa else) sementara kotak "berhasil dilakukan"
		 * tetap tampil tanpa syarat -- itulah sebabnya pengguna melihat notifikasi berhasil
		 * padahal tidak satu pun baris tersimpan. Kini hasil TIAP baris dicatat, lalu di akhir
		 * proses jumlah berhasil/gagal/dilewati ditampilkan dan rincian per baris otomatis diunduh.
		 */
		final ais.common.LaporanUpload laporan = new ais.common.LaporanUpload(
				"Upload Data Mahasiswa Status Keluar");
		laporan.setNamaBerkasSumber(file.getName());

		final Label label = new Label(ais.common.Common.getBahasaConfig("Proses upload data data .."));
		Clients.showBusy(label.getValue());
		final Timer timer = new Timer(200);
		timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		timer.setRepeats(true);
		timer.addEventListener("onTimer", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Clients.showBusy(label.getValue());
				if (label.getValue().isEmpty()) {
					Clients.clearBusy();
					timer.detach();
					laporan.selesaikan(eventListener);
				}

			}
		});
		timer.start();

		new Thread(new Runnable() {

			@Override
			public void run() {
				Session session = null;
				try {

					XSSFWorkbook workbook = new XSSFWorkbook(file.getAbsolutePath());
					XSSFSheet sheet = workbook.getSheetAt(0);

					/*
					 * WAJIB openSession(), BUKAN currentNativeSession(). Common.getSheetContentAsObject()
					 * dan getSheetContentAsString() di dalam loop menutup native session ThreadLocal
					 * (HibernateUtil.closeSession()), sehingga session hasil currentNativeSession()
					 * sudah TERTUTUP saat getTransaction().begin() dipanggil -> "Session is closed!"
					 * di SETIAP baris -> seluruh baris tercatat gagal.
					 */
					session = HibernateUtil.openSession();

					int rowCount = (sheet.getLastRowNum() + 1);
					for (int i = 1; i < rowCount; i++) {
						String nimBaris = "";
						try {

							nimBaris = Common.getSheetContentAsString(sheet, 0, i);

							Mahasiswa mahasiswa = (Mahasiswa) Common.getSheetContentAsObject(sheet, 0, i,
									Mahasiswa.class);
							if (mahasiswa == null) {
								mahasiswa = ConstantValues.ambilByNim(nimBaris);
							}

							String noIjazah1 = Common.getSheetContentAsString(sheet, 4, i);
							String noIjazah2 = Common.getSheetContentAsString(sheet, 5, i);
							String noAkta1 = Common.getSheetContentAsString(sheet, 6, i);
							String noAkta2 = Common.getSheetContentAsString(sheet, 7, i);
							Date tanggalWisuda = Common.getSheetContentAsDate(sheet, 8, i);
							Date tanggalSkRektor = Common.getSheetContentAsDate(sheet, 9, i);
							Date tanggalYudisium = Common.getSheetContentAsDate(sheet, 10, i);
							String skDo = Common.getSheetContentAsString(sheet, 11, i);
							String nomorSkpi = Common.getSheetContentAsString(sheet, 12, i);

							if (mahasiswa != null && mahasiswa.getId() != null) {

								// Reload ke session khusus thread ini agar entitas managed (bukan
								// detached dari cache/session lain) -> update pasti ter-flush.
								Mahasiswa mahasiswaSafe = (Mahasiswa) session.get(Mahasiswa.class, mahasiswa.getId());
								KelompokStatusKeluarMahasiswa kelompokSafe = (KelompokStatusKeluarMahasiswa) session
										.get(KelompokStatusKeluarMahasiswa.class, kelompokStatusKeluarMahasiswa.getId());
								if (mahasiswaSafe == null || kelompokSafe == null) {
									laporan.catatDilewati(i, nimBaris, "Data mahasiswa/kelompok tidak ditemukan di database");
									continue;
								}

								mahasiswaSafe.setNoAkta1(noAkta1);
								mahasiswaSafe.setNoAkta2(noAkta2);
								mahasiswaSafe.setNoIjazah1(noIjazah1);
								mahasiswaSafe.setNoIjazah2(noIjazah2);
								mahasiswaSafe.setSkDo(skDo);
								mahasiswaSafe.setNomorSkpi(nomorSkpi);
								mahasiswaSafe.setTanggalWisuda(tanggalWisuda);
								mahasiswaSafe.setTanggalSkRektor(tanggalSkRektor);
								mahasiswaSafe.setTanggalYudisium(tanggalYudisium);
								mahasiswaSafe.setKelompokStatusKeluarMahasiswa(kelompokSafe);

								session.getTransaction().begin();
								try {
									Common.refreshUpdate(session, mahasiswaSafe);
									session.getTransaction().commit();
								} catch (Exception eSimpan) {
									/*
									 * WAJIB rollback. Tanpa ini transaksi tetap AKTIF, sehingga begin() pada
									 * baris berikutnya melempar "Transaction already active" -- satu baris
									 * bermasalah membuat SELURUH baris sesudahnya ikut gagal tanpa jejak,
									 * sementara notifikasi tetap berbunyi berhasil.
									 */
									try {
										session.getTransaction().rollback();
									} catch (Exception eRoll) {
										ais.common.ErrorAuditUtil.record(eRoll, "rollback-gagal-upload "
											+ "src/ais/action/master/helper/KelompokStatusKeluarMahasiswaDetailAction.java");
									}
									throw eSimpan;
								}

								laporan.catatBerhasil(i, mahasiswaSafe.getNim(), mahasiswaSafe.getNama());

								label.setValue("Upload data \"" + mahasiswaSafe.getNim() + " - " + mahasiswaSafe.getNama()
										+ "\" (" + Common.numberFormat.get().format(i * 100.0 / rowCount) + " %)");
							} else if (nimBaris == null || nimBaris.trim().isEmpty()) {
								laporan.catatDilewati(i, "", "Kolom NIM/NPM kosong");
							} else {
								laporan.catatDilewati(i, nimBaris,
									"NIM/NPM tidak ditemukan pada data mahasiswa -- periksa penulisannya, "
										+ "atau mahasiswa memang belum terdaftar");
							}

						} catch (Exception e) {
							laporan.catatGagal(i, nimBaris, e);
							Common.tampilErrorJikaAdmin(e);
						}

					}
				} catch (Exception e1) {
					e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/helper/KelompokStatusKeluarMahasiswaDetailAction.java:836");
				} finally {
					// Tutup session khusus thread ini + bersihkan ThreadLocal sisa helper Excel.
					HibernateUtil.closeSessionQuietly(session);
					HibernateUtil.closeSession();
				}

				label.setValue("");
			}
		}).start();
	}
}
