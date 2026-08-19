package ais.action.master.sekolah.helper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Calendar;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.West;

import ais.common.Common;
import ais.common.CommonEmail;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.FormatNilai;
import ais.database.model.Pertemuan;
import ais.database.model.Tbmuser;
import ais.database.model.file.FileFoto;
import ais.database.model.file.LampiranLain;
import ais.database.model.file.TugasFileContent;
import ais.database.model.sekolah.CalonSiswa;
import ais.database.model.sekolah.Siswa;
import ais.ui.util.MyCkEditor;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelBold;
import ais.ui.util.MyLabelBoldConfig;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyPanel;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class TugasMandiriSiswaHelper {

	private MyGrid uploadTugasGrid;
	private Tabpanel tabpanelFileTugasPertemuan;
	private Siswa siswa;
	private CalonSiswa calonSiswa;
	private Pertemuan pertemuan;

	public TugasMandiriSiswaHelper(final Siswa siswa, final CalonSiswa calonSiswa) {
		this.siswa = siswa;
		this.calonSiswa = calonSiswa;
	}

	private MyToolbarbuttonConfig buttonMasukkanNilai;

	public void onUbahPerintahTugas(final EventListener eventListener) throws Exception {
		final MyWindow addWindow = new MyWindow();
		addWindow.setHeight("95%");
		addWindow.setWidth("950px");
		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(addWindow);

		addWindow.setTitle("Perintah Tugas");
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

		Rows rows = new Rows();
		rows.setParent(grid);

		final MyDatebox mulaiWaktuMengumpulkanTugas = new MyDatebox(pertemuan.getMulai());
		mulaiWaktuMengumpulkanTugas.setFormat(Common.dateFormat.get().toPattern());

		// mulaiWaktuMengumpulkanTugas.setWidth("90%");
		final MyDatebox batasWaktuMengumpulkanTugas = new MyDatebox(pertemuan.getSelesai());
		batasWaktuMengumpulkanTugas.setFormat(Common.dateFormat.get().toPattern());
		// batasWaktuMengumpulkanTugas.setWidth("90%");

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelBoldConfig("Tugas Mulai"));

		row = new MyFormRow();
		row.setParent(rows);
		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser.getSiswa() == null) {
			row.appendChild(mulaiWaktuMengumpulkanTugas);
		} else {
			Html html = new ais.ui.util.MyHtml("<strong><font style='color:red'>"
					+ (pertemuan.getMulai() == null ? "Tidak Ada" : Common.dateFormat5.get().format(pertemuan.getMulai()))
					+ "</font></strong>");
			row.appendChild(html);
		}

		Common.initKeteranganSatuKolom(rows,
				"Kosongkan tanggal tugas mulai jika tugas ini tidak ada batas waktu mulai-nya");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelBoldConfig("Tugas Selesai"));

		row = new MyFormRow();
		row.setParent(rows);
		if (tbmuser.getSiswa() == null) {
			row.appendChild(batasWaktuMengumpulkanTugas);
		} else {
			Html html = new ais.ui.util.MyHtml("<strong><font style='color:red'>"
					+ (pertemuan.getSelesai() == null ? "Tidak Ada" : Common.dateFormat5.get().format(pertemuan.getSelesai()))
					+ "</font></strong>");
			row.appendChild(html);
		}

		Common.initKeteranganSatuKolom(rows,
				"Kosongkan tanggal tugas selesai jika tugas ini tidak ada batas waktu selesai-nya");

		if (pertemuan.getPerkuliahan() != null) {

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new MyLabelBoldConfig("Nilai masuk ke format penilaian :"));

			row = new MyFormRow();
			row.setParent(rows);
			Vbox vbox = new Vbox();
			vbox.setParent(row);

			Hbox hboxP = new Hbox();
			final Combobox formatNilai = new Combobox();

			formatNilai.setWidth("92px");
			MyComboitemConfig comboitemTidakAda = new MyComboitemConfig("Tidak Ada");
			comboitemTidakAda.setValue(null);
			formatNilai.appendChild(comboitemTidakAda);
			List<FormatNilai> formatNilais = Common.getFormatNilais(HibernateUtil.currentSession(),
					pertemuan.getPerkuliahan());
			for (FormatNilai nilai : formatNilais) {
				if (nilai.getStatusPertemuan() != null) {
					org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
					comboitem.setValue(nilai);
					comboitem.setLabel(nilai.getNama() + " (" + Common.numberFormat.get().format(nilai.getPersen()) + "%)");
					formatNilai.appendChild(comboitem);
				}
			}
			formatNilai.setParent(hboxP);
			if (pertemuan.getFormatNilai() == null) {
				formatNilai.setSelectedItem(comboitemTidakAda);
			} else {
				Common.selectComboItem(formatNilai, pertemuan.getFormatNilai());
			}
			formatNilai.setReadonly(true);
			formatNilai.setDisabled(pertemuan.getPerkuliahan().getDikunci() != null);
			if (pertemuan.getPerkuliahan().getDikunci() != null) {
				new MyLabelKecil("Penilaian sudah dikunci").setParent(vbox);
				if (pertemuan.getFormatNilai() != null) {
					new MyLabelKecil("Nilai otomatis masuk ke " + pertemuan.getFormatNilai().getNama()).setParent(vbox);
				}
			}

			hboxP.setParent(vbox);
			hboxP.appendChild(new Label(ais.common.Common.getBahasaConfig(" dengan bobot sebesar ")));
			final MyDoublebox prosentase = new MyDoublebox(pertemuan.getProsentase());
			prosentase.setDisabled(pertemuan.getPerkuliahan().getDikunci() != null);
			prosentase.setCols(2);
			prosentase.setParent(hboxP);
			hboxP.appendChild(new Label(" "));

			prosentase.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					pertemuan.setProsentase(prosentase.getValue());
					Common.refreshUpdate(pertemuan);
				}
			});

			formatNilai.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					final FormatNilai fn = (FormatNilai) (formatNilai.getSelectedItem() == null ? null
							: formatNilai.getSelectedItem().getValue());

					Session session = HibernateUtil.currentSession();
					pertemuan.setFormatNilai(fn);
					Common.refreshUpdate(session, (pertemuan));
					prosentase.getParent().setVisible(pertemuan.getFormatNilai() != null);
				}

			});
		}

		final MyCkEditor isiTugas;

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new MyLabelBoldConfig("Judul tugas mandiri (*):"));

		row = new MyFormRow();
		row.setParent(rows);
		final Textbox judul;
		row.appendChild(judul = new Textbox(pertemuan.getJudultugas()));
		judul.setWidth("90%");
		judul.setRows(2);
		judul.setMaxlength(255);

		Common.initKeteranganSatuKolom(rows,
				"Judul tugas harus diisi, karena sebagai penanda terdapat tugas atau tidak.. Jika judul tidak diisi, maka mahasiswa tidak bisa mengupload tugas..");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new MyLabelBoldConfig("Masukkan instruksi tugas mandiri :"));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(isiTugas = new MyCkEditor());
		isiTugas.setValue(pertemuan.getIsitugas());
		isiTugas.setWidth("97%");
		isiTugas.setHeight("200px");

		row = new MyFormRow();
		row.setParent(rows);

		Hbox hbox = new Hbox();
		hbox.setParent(row);
		LampiranLain.createDownloadUploadFileLain(hbox, pertemuan.getId(), LampiranLain.TUGAS_MANDIRI_PERKULIAHAN,
				"Tugas Individu", false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

					}
				}, null, false, false, false, true);

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
				addWindow.detach();
			}
		});
		cancel.setParent(toolbar);
		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				if (judul.getValue().trim().equals("")) {
					MyMessageboxConfig.show("Judul tugas harus diisi", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return;
				}

				Session session = HibernateUtil.currentSession();
				pertemuan.setJudultugas(judul.getValue());
				pertemuan.setIsitugas(isiTugas.getValue());
				pertemuan.setMulai(mulaiWaktuMengumpulkanTugas.getValue());
				pertemuan.setSelesai(batasWaktuMengumpulkanTugas.getValue());
				Common.refreshUpdate(session, (pertemuan));

				eventListener.onEvent(new Event("", addWindow, pertemuan));

				CommonEmail.infoAdaTugasPerkuliahan(pertemuan);
				ais.common.CommonNotifikasi.infoTugasBaru(pertemuan);

				addWindow.detach();
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);

		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@SuppressWarnings("deprecation")
	public void createTugas(final Pertemuan pertemuan, final Tabpanel tabpanelFileTugasPertemuan) throws Exception {
		this.pertemuan = pertemuan;
		this.tabpanelFileTugasPertemuan = tabpanelFileTugasPertemuan;

		MyPanel panel = new MyPanel();
		panel.setParent(tabpanelFileTugasPertemuan);
		panel.setWidth("100%");
		panel.setHeight("100%");

		panel.setBorder("none");
		panel.setStyle("border:0px;");

		Panelchildren panelchildren = new Panelchildren();
		panelchildren.setParent(panel);

		/* Portal 2 kolom responsif (menumpuk di HP) menggantikan Borderlayout West+Center.
		 * Kiri = instruksi/syarat tugas (40%), kanan = pengumpulan & rekap (60%). */
		ais.ui.util.MyPortallayout borderlayout = new ais.ui.util.MyPortallayout();
		borderlayout.setWidth("100%");
		borderlayout.setParent(panelchildren);

		ais.ui.util.MyPortalchildren west = new ais.ui.util.MyPortalchildren();
		west.setWidth("40%");
		west.setParent(borderlayout);

		// Div div = new Div();
		// div.setParent(west);

		MyGrid gridPerintah = new MyGrid();
		gridPerintah.setParent(west);
		gridPerintah.setWidth("100%");
		// gridPerintah.setHeight("200px");

		Columns columns = new Columns();

		columns.setParent(gridPerintah);
		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");

		// column = new MyColumnConfig();
		// column.setParent(columns);
		// column.setLabel("");
		// column.setWidth("10%");

		Rows rows = new Rows();
		rows.setParent(gridPerintah);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tugas Mulai"));
		Tbmuser tbmuser = Common.getCurrentUser();

		final Html htmlMulai = new ais.ui.util.MyHtml("<strong><font style='color:red'>"
				+ (pertemuan.getMulai() == null ? "Tidak Ada" : Common.dateFormat5.get().format(pertemuan.getMulai()))
				+ "</font></strong>");
		row.appendChild(htmlMulai);

		Common.initKeterangan(rows, "Jika tanggal tugas mulai tidak diisi, maka tugas ini tidak ada waktu mulai-nya");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tugas Selesai"));

		final Html htmlSampai = new ais.ui.util.MyHtml("<strong><font style='color:red'>"
				+ (pertemuan.getSelesai() == null ? "Tidak Ada" : Common.dateFormat5.get().format(pertemuan.getSelesai()))
				+ "</font></strong>");
		row.appendChild(htmlSampai);

		Common.initKeterangan(rows,
				"Jika tanggal tugas selesai tidak diisi, maka tugas ini tidak ada batas waktu selesai-nya");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Isi Tugas"));

		MyToolbarbuttonConfig rubah = new MyToolbarbuttonConfig("Masukkan Instruksi Tugas", "/img/svg/edit-box-line.svg");
		rubah.setOrient("vertical");
		rubah.setVisible(siswa == null && calonSiswa == null);
		row.appendChild(rubah);

		row = new MyFormRow();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		final MyLabelBold judul = new MyLabelBold(pertemuan.getJudultugas());
		row.appendChild(judul);

		row = new MyFormRow();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		final Html isi = new ais.ui.util.MyHtml(pertemuan.getIsitugas());
		row.appendChild(isi);

		row = new MyFormRow();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		final Hbox hbox = new Hbox();
		hbox.setParent(row);
		LampiranLain.createDownloadUploadFileLain(hbox, pertemuan.getId(), LampiranLain.TUGAS_MANDIRI_PERKULIAHAN,
				LampiranLain.TUGAS_MANDIRI_PERKULIAHAN, false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

					}
				}, null, false, false, false, false);

		row = new MyFormRow();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.setVisible(pertemuan.getFormatNilai() != null);

		final Html nilaiMasuk = new ais.ui.util.MyHtml(pertemuan.getFormatNilai() == null ? ""
				: "<strong><font style='color:blue'>" + ("Nilai akan masuk ke " + pertemuan.getFormatNilai().getNama())
						+ " dengan bobot sebesar " + Common.numberFormat.get().format(pertemuan.getProsentase())
						+ " </font></strong>");
		row.appendChild(nilaiMasuk);

		final Toolbar vbox = new Toolbar();

		rubah.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				TugasMandiriSiswaHelper.this.pertemuan = pertemuan;
				onUbahPerintahTugas(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						final Pertemuan pertemuan = (Pertemuan) arg0.getData();
						TugasMandiriSiswaHelper.this.pertemuan = pertemuan;
						isi.setContent(pertemuan.getIsitugas());
						judul.setValue(pertemuan.getJudultugas());
						htmlMulai
								.setContent("<strong><font style='color:red'>"
										+ (pertemuan.getMulai() == null ? "Tidak Ada"
												: Common.dateFormat5.get().format(pertemuan.getMulai()))
										+ "</font></strong>");
						htmlSampai
								.setContent("<strong><font style='color:red'>"
										+ (pertemuan.getSelesai() == null ? "Tidak Ada"
												: Common.dateFormat5.get().format(pertemuan.getSelesai()))
										+ "</font></strong>");

						Common.clear(hbox);
						LampiranLain.createDownloadUploadFileLain(hbox, pertemuan.getId(), "Tugas Individu",
								LampiranLain.TUGAS_MANDIRI_PERKULIAHAN, false, new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {

									}
								}, null, false, false, false, false);

						nilaiMasuk.getParent().setVisible(pertemuan.getFormatNilai() != null);
						nilaiMasuk.setContent(pertemuan.getFormatNilai() == null ? ""
								: pertemuan.getFormatNilai() == null ? ""
										: "<strong><font style='color:blue'>"
												+ ("Nilai akan masuk ke " + pertemuan.getFormatNilai().getNama())
												+ " dengan bobot sebesar "
												+ Common.numberFormat.get().format(pertemuan.getProsentase())
												+ " </font></strong>");

						if (buttonMasukkanNilai != null) {
							buttonMasukkanNilai.setVisible(false);
							buttonMasukkanNilai.detach();
						}
						if (pertemuan.getFormatNilai() != null && siswa == null && calonSiswa == null) {

							buttonMasukkanNilai = new MyToolbarbuttonConfig(
									"Masukkan Nilai ke " + pertemuan.getFormatNilai().getNama(), "/img/Configure.gif");
							buttonMasukkanNilai.setParent(vbox);
							buttonMasukkanNilai.addEventListener("onClick", new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									TugasMandiriSiswaHelper.this.pertemuan = pertemuan;
									Common.createDefaultTimer(new EventListener() {

										@Override
										public void onEvent(Event arg0) throws Exception {
											ais.common.GradingHelper.hitungNilaiBerdasarkanFormatNilai(pertemuan.getPerkuliahan(),
													pertemuan.getFormatNilai());
										}
									});
								}
							});
						}
					}
				});

			}
		});

		final MyToolbarbuttonConfig upload = new MyToolbarbuttonConfig("Upload Tugas Siswa", "/img/new.gif");

		// EventListener batasWaktuMengumpulkanTugasEventListener = new
		// EventListener() {
		//
		// @Override
		// public void onEvent(Event arg0) throws Exception {
		//
		//
		//
		// Calendar kemarin = ais.ui.util.WaktuUtil.getCalendar();
		//
		// upload.setVisible((pertemuan.getSelesai() == null ||
		// pertemuan.getSelesai().after(kemarin.getTime()))
		// && (pertemuan.getMulai() == null ||
		// pertemuan.getMulai().before(kemarin.getTime())));
		// }
		// };

		final MyToolbarbuttonConfig download = new MyToolbarbuttonConfig("Download Semua Tugas", FileFoto.icon(null));

		// batasWaktuMengumpulkanTugas.addEventListener("onChange",
		// batasWaktuMengumpulkanTugasEventListener);

		Calendar kemarin = ais.ui.util.WaktuUtil.getCalendar();
		upload.setVisible((pertemuan.getSelesai() == null || pertemuan.getSelesai().after(kemarin.getTime()))
				&& (pertemuan.getMulai() == null || pertemuan.getMulai().before(kemarin.getTime())));

		download.setVisible(tbmuser != null && tbmuser.getSiswa() == null);

		if (!(pertemuan.getMulai() == null || pertemuan.getMulai().before(kemarin.getTime()))) {
			isi.setContent("<strong><font style='color:red'>Tugas belum mulai</font></strong>");
		} else {
			isi.setContent(pertemuan.getIsitugas());
		}

		upload.addEventListener(Events.ON_CLICK, new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {

				Common.uploadTugas(pertemuan, siswa, calonSiswa, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						reloadTugasFileContent();
					}
				});
			}
		});

		download.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {

				try {

					Session session = StreamingHibernateUtil.getInstance().currentSession();

					List<TugasFileContent> mypertemuanFileContents = session.createCriteria(TugasFileContent.class)
							.addOrder(Order.asc("id")).add(Restrictions.eq("pertemuan", pertemuan.getId())).list();

					System.out.println("size " + mypertemuanFileContents.size());

					// Create a buffer for reading the files
					byte[] buf = new byte[1024];
					File fileOut = new File(
							Common.REAL_PATH + "/tmp/Tugas_untuk_pertemuan_ke_" + pertemuan.getPertemuanKe() + ".zip");

					try {

						ZipOutputStream out = new ZipOutputStream(new FileOutputStream(fileOut));

						// Compress the files
						for (TugasFileContent content : mypertemuanFileContents) {

							try {
								File file = content.ambilFile();
								FileInputStream in = new FileInputStream(file);
								// Add ZIP entry to output stream.
								content.ubahRealNameSesuaiDenganNIM(siswa);
								out.putNextEntry(new ZipEntry(content.getNama()));

								// Transfer bytes from the file to the ZIP file
								int len;
								while ((len = in.read(buf)) > 0) {
									out.write(buf, 0, len);
								}

								// Complete the entry
								out.closeEntry();
								in.close();
							} catch (Exception e) {
								Common.tampilErrorJikaAdmin(e);
							}
						}

						// Complete the ZIP file
						out.close();

					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);
					}

					Filedownload.save(fileOut, "application/zip");

					StreamingHibernateUtil.getInstance().closeSession();
				} catch (Exception e) {
					StreamingHibernateUtil.getInstance().rollbackTransaction();
					Common.tampilErrorJikaAdmin(e);
				}

			}
		});

		ais.ui.util.MyPortalchildren center = new ais.ui.util.MyPortalchildren();
		center.setWidth("60%");
		center.setParent(borderlayout);

		Borderlayout myborderlayout = new ais.ui.util.MyBorderlayout();
		myborderlayout.setParent(center);
		/* Tinggi konkret: grid di dalamnya height:100% — di kolom portal (Div natural-height)
		 * Borderlayout tanpa tinggi pasti kolaps. Beri tinggi viewport + min-height pengaman. */
		myborderlayout.setHeight("74vh");
		myborderlayout.setStyle("min-height:480px;");

		vbox.setWidth("100%");
		vbox.setHeight("25px");
		vbox.appendChild(upload);
		vbox.appendChild(download);

		if (pertemuan.getFormatNilai() != null && siswa == null && calonSiswa == null) {
			if (buttonMasukkanNilai != null) {
				buttonMasukkanNilai.setVisible(false);
				buttonMasukkanNilai.detach();
			}
			buttonMasukkanNilai = new MyToolbarbuttonConfig("Masukkan Nilai ke " + pertemuan.getFormatNilai().getNama(),
					"/img/Configure.gif");
			buttonMasukkanNilai.setParent(vbox);
			buttonMasukkanNilai.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					TugasMandiriSiswaHelper.this.pertemuan = pertemuan;
					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							ais.common.GradingHelper.hitungNilaiBerdasarkanFormatNilai(pertemuan.getPerkuliahan(),
									pertemuan.getFormatNilai());
						}
					});
				}
			});
		}

		North myNorth = new North();
		myNorth.setParent(myborderlayout);
		ais.ui.util.ZkCompat.setFlex(myNorth, true);
		myNorth.appendChild(vbox);

		Center mycenter = new Center();
		mycenter.setParent(myborderlayout);
		ais.ui.util.ZkCompat.setFlex(mycenter, true);

		uploadTugasGrid = new MyGrid();
		uploadTugasGrid.setParent(mycenter);
		uploadTugasGrid.setWidth("100%");
		uploadTugasGrid.setHeight("100%");

		columns = new Columns();

		columns.setParent(uploadTugasGrid);

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("NIM");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("File");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Tanggal");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Keterangan");
		column.setWidth("25%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nilai");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("7%");

		reloadTugasFileContent();
	}

	@SuppressWarnings("unchecked")
	private void reloadTugasFileContent() {
		Session session = StreamingHibernateUtil.getInstance().currentSession();

		List<TugasFileContent> pertemuanFileContent = session.createCriteria(TugasFileContent.class)
				.addOrder(Order.desc("id")).add(Restrictions.eq("pertemuan", pertemuan.getId())).list();

		tabpanelFileTugasPertemuan.getLinkedTab().setLabel("Tugas (" + pertemuanFileContent.size() + ")");

		ListModel strset = new SimpleListModel(pertemuanFileContent);
		uploadTugasGrid.setRowRenderer(new DetailTugasFileContentRenderer());
		uploadTugasGrid.setModelCheckMobile(strset);
		uploadTugasGrid.renderAll();
		uploadTugasGrid.setOddRowSclass("non-odd");

		StreamingHibernateUtil.getInstance().closeSession();
	}

	class DetailTugasFileContentRenderer extends ais.ui.util.MyRowRenderer {

		private Tbmuser tbmuser = Common.getCurrentUser();

		public DetailTugasFileContentRenderer() {

		}

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final TugasFileContent tugasFileContent = (TugasFileContent) arg1;

			Session session = HibernateUtil.currentSession();
			Siswa siswa = (Siswa) session.createCriteria(Siswa.class).add(Restrictions.isNotNull("namaSiswa")).add(Restrictions.ne("namaSiswa","")).add(Restrictions.isNotNull("sekolah"))
					.add(Restrictions.idEq(tugasFileContent.getSiswa())).uniqueResult();
			if (siswa != null) {
				new Label(siswa == null ? "" : siswa.getNim()).setParent(arg0);
				new Label(siswa == null ? "" : siswa.getNama()).setParent(arg0);
				tugasFileContent.ubahRealNameSesuaiDenganNIM(siswa);
			} else {
				CalonSiswa calonSiswa = (CalonSiswa) session.createCriteria(CalonSiswa.class).add(Restrictions.isNotNull("gelombangPendaftaranPsb"))
						.add(Restrictions.idEq(tugasFileContent.getCalonSiswa())).uniqueResult();
				new Label(calonSiswa == null ? "" : calonSiswa.getNim()).setParent(arg0);
				new Label(calonSiswa == null ? "" : calonSiswa.getNama()).setParent(arg0);
				tugasFileContent.ubahRealNameSesuaiDenganNIM(calonSiswa);
			}
			new Label(tugasFileContent.getNama()).setParent(arg0);
			new Label(Common.dateFormat.get().format(tugasFileContent.getUploadDate())).setParent(arg0);
			if (TugasMandiriSiswaHelper.this.siswa == null && TugasMandiriSiswaHelper.this.calonSiswa == null) {
				final Textbox ketarangan = new Textbox(tugasFileContent.getKeterangan());
				ketarangan.setWidth("90%");
				ketarangan.setRows(2);
				ketarangan.setParent(arg0);
				ketarangan.setMaxlength(255);

				ketarangan.addEventListener("onChange", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Session session = StreamingHibernateUtil.getInstance().currentSession();
						session.createSQLQuery("update tugas_file_content set keterangan= :keterangan where id="
								+ tugasFileContent.getId()).setParameter("keterangan", ketarangan.getValue().trim())
								.executeUpdate();
						StreamingHibernateUtil.getInstance().closeSession();
					}
				});
			} else {
				new Label(tugasFileContent.getKeterangan()).setParent(arg0);
			}

			if (TugasMandiriSiswaHelper.this.siswa == null && TugasMandiriSiswaHelper.this.calonSiswa == null) {
				final MyDoublebox nilai = new MyDoublebox(tugasFileContent.getNilai());
				nilai.setWidth("90%");
				nilai.setParent(arg0);
				nilai.addEventListener("onChange", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						try {
							Session session = StreamingHibernateUtil.getInstance().currentSession();

							String sql = "update tugas_file_content set nilai = " + nilai.getValue() + " where id = "
									+ tugasFileContent.getId();
							System.out.println(sql);

							session.getTransaction().begin();
							session.createSQLQuery(sql).executeUpdate();
							session.getTransaction().commit();

						} catch (Exception e) {
							StreamingHibernateUtil.getInstance().rollbackTransaction();
							Common.tampilErrorJikaAdmin(e);
						}
					}
				});
			} else {

				if (tbmuser != null && tbmuser.getSiswa() == null || (tbmuser.getSiswa() != null
						&& tbmuser.getSiswa().getId().equals(tugasFileContent.getSiswa()))) {
					new Label(Common.numberFormat.get().format(tugasFileContent.getNilai())).setParent(arg0);
				} else {
					new Label("").setParent(arg0);
				}

			}

			final java.util.List<org.zkoss.zk.ui.Component> aksiButtons =
					new java.util.ArrayList<org.zkoss.zk.ui.Component>();

			MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("", tugasFileContent.iconDonwload());
			toolbarbutton.setVisible(tbmuser != null && tbmuser.getSiswa() == null
					|| (tbmuser.getSiswa() != null && tbmuser.getSiswa().getId().equals(tugasFileContent.getSiswa())));
			toolbarbutton.setOrient("vertical");
			aksiButtons.add(toolbarbutton);
			toolbarbutton.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					try {
						Session session = StreamingHibernateUtil.getInstance().currentSession();

						TugasFileContent mypertemuanFileContent = (TugasFileContent) session
								.createCriteria(TugasFileContent.class).add(Restrictions.idEq(tugasFileContent.getId()))
								.uniqueResult();

						Filedownload.save(mypertemuanFileContent.ambilFile(), mypertemuanFileContent.getFileMimeType());

						// Filedownload.save(
						// Common.createInputStreamFromFile(mypertemuanFileContent
						// .getFileContent()),
						// mypertemuanFileContent.getFileMimeType(),
						// mypertemuanFileContent.getRealFile());

						StreamingHibernateUtil.getInstance().closeSession();
					} catch (Exception e) {
						StreamingHibernateUtil.getInstance().rollbackTransaction();
						Common.tampilErrorJikaAdmin(e);
					}

				}

			});

			toolbarbutton = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			toolbarbutton.setOrient("vertical");
			toolbarbutton.setTooltiptext("Hapus Data");
			toolbarbutton.setVisible(tugasFileContent.getNilai() < 0.01 && (tbmuser != null && tbmuser.getSiswa() == null
					|| (tbmuser.getSiswa() != null && tbmuser.getSiswa().getId().equals(tugasFileContent.getSiswa()))));

			if (tugasFileContent.getNilai() != null && tugasFileContent.getNilai() > 1.0) {
				toolbarbutton.setVisible(false);
			}

			aksiButtons.add(toolbarbutton);
			toolbarbutton.addEventListener("onClick", new EventListener() {
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
											Session session = StreamingHibernateUtil.getInstance().currentSession();

											session.getTransaction().begin();
											Common.refreshDelete((tugasFileContent));
											session.getTransaction().commit();

											reloadTugasFileContent();
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											StreamingHibernateUtil.getInstance().rollbackTransaction();
											MyMessageboxConfig.show(
													"Data ini tidak dapat dihapus .., karena berelasi dengan data lainnya, error-nya adalah sbagai berikut:"
															+ e.getMessage());
										}

									}

								}
							});

				}

			});

			Vbox aksiBox = ais.ui.util.UIHelper.buatBarisAksi(arg0, 3, aksiButtons);
			aksiBox.setVisible(siswa == null || siswa.getId().equals(tugasFileContent.getSiswa()));
		}
	}
}
