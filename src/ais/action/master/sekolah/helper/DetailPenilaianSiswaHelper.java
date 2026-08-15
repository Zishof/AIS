package ais.action.master.sekolah.helper;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;
import org.zkoss.poi.xssf.usermodel.XSSFCell;
import org.zkoss.poi.xssf.usermodel.XSSFCellStyle;
import org.zkoss.poi.xssf.usermodel.XSSFColor;
import org.zkoss.poi.xssf.usermodel.XSSFFont;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Auxhead;
import org.zkoss.zul.Auxheader;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Button;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Foot;
import org.zkoss.zul.Footer;
import org.zkoss.zul.Group;
import org.zkoss.zul.Groupbox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Space;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.sekolah.util.GrupPenilaianUtil;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Agama;
import ais.database.model.Konfigurasi;
import ais.database.model.ParameterTambahan;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranLain;
import ais.database.model.sekolah.DetailGrupKategoriItemPenilaianSiswa;
import ais.database.model.sekolah.DetailGrupPenilaian;
import ais.database.model.sekolah.DetailJenisPenilaian;
import ais.database.model.sekolah.GrupKategoriItemPenilaianSiswa;
import ais.database.model.sekolah.GrupPenilaian;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.JadwalPelajaran;
import ais.database.model.sekolah.JenisItemPenilaianSiswa;
import ais.database.model.sekolah.JenisPenilaian;
import ais.database.model.sekolah.KategoriItemPenilaianSiswa;
import ais.database.model.sekolah.KelasSiswa;
import ais.database.model.sekolah.KelasSiswaPunyaSiswa;
import ais.database.model.sekolah.KurikulumPunyaMatapelajaran;
import ais.database.model.sekolah.Matapelajaran;
import ais.database.model.sekolah.NilaiHurufSekolah;
import ais.database.model.sekolah.Siswa;
import ais.database.model.sekolah.VoKelasPunyaSiswa;
import ais.ui.util.DataCriteria;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelAgakKecilBold;
import ais.ui.util.MyLabelBoldAja;
import ais.ui.util.MyLabelBoldMerah;
import ais.ui.util.MyLabelBolder;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyTextboxAngka;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;

public class DetailPenilaianSiswaHelper {

	private KelasSiswa kelasSiswa;
	private Rows rowsData;
	private List<? extends VoKelasPunyaSiswa> siswas;

	private static String[] contents = new String[] { "siswa.id", "siswa.nomorInduk", "siswa.nomorIndukNasional",
			"siswa.namaSiswa", "siswa.tahunMasuk", "siswa.jenisKelamin", "siswa.agama.nama" };

	public DetailPenilaianSiswaHelper() {
		// delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		// create = CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE);

	}

	public Criteria initCriteria(boolean order) {

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(KelasSiswaPunyaSiswa.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.eq("kelasSiswa", kelasSiswa))

				.createAlias("siswa", "siswa");

		if (order) {
			criteria.addOrder(Order.asc("nomorUrut")).addOrder(Order.asc("siswa.namaSiswa"))
					.addOrder(Order.desc("siswa.id"));
		}

		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser != null && tbmuser.getOrangTua() != null && !tbmuser.getOrangTua().ambilAnakSiswa().isEmpty()) {
			criteria.add(Restrictions.in("siswa.id", tbmuser.getOrangTua().ambilAnakSiswa()));
		}

		return criteria;
	}

	public static void displayPenilaian(KurikulumPunyaMatapelajaran kurikulumPunyaMatapelajaran, Component detail,
			KelasSiswa kelasSiswa, List<? extends VoKelasPunyaSiswa> siswas) throws Exception {
		displayPenilaian(null, kurikulumPunyaMatapelajaran, detail, kelasSiswa, siswas);
	}

	private static Rows createRows(JadwalPelajaran jadwalPelajaran, GrupPenilaian grupPenilaian,
			GrupKategoriItemPenilaianSiswa grupKategoriItemPenilaianSiswa, KelasSiswa kelasSiswa,
			Matapelajaran matapelajaran, Tbmuser tbmuser, EventListener eventListener, Groupbox groupboxData) {
		MyGrid grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(50);
		grid.getPagingChild().setMold("os");
		grid.setParent(groupboxData);

		MyColumnConfig columnTotalGanjil = new MyColumnConfig();

		MyColumnConfig columnMinGanjil = new MyColumnConfig();
		MyColumnConfig columnMaxGanjil = new MyColumnConfig();

		MyColumnConfig columnHurufGanjil = new MyColumnConfig();

		MyColumnConfig columnTotalGenap = new MyColumnConfig();

		MyColumnConfig columnMinGenap = new MyColumnConfig();
		MyColumnConfig columnMaxGenap = new MyColumnConfig();

		MyColumnConfig columnHurufGenap = new MyColumnConfig();

		if (jadwalPelajaran != null
				|| (grupKategoriItemPenilaianSiswa != null
						&& grupKategoriItemPenilaianSiswa.getKhususSemester() != null)
				|| (grupPenilaian != null && grupPenilaian.getKhususSemester() != null)) {

			Columns columns = new Columns();

			columns.setParent(grid);

			MyColumnConfig column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Foto");
			column.setWidth("70px");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Siswa");
			column.setWidth("25%");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Penilian");

			columnTotalGanjil.setParent(columns);
			columnTotalGanjil.setLabel("Total");
			columnTotalGanjil.setWidth("0%");

			columnMinGanjil.setParent(columns);
			columnMinGanjil.setLabel("Min");
			columnMinGanjil.setWidth("0%");

			columnMaxGanjil.setParent(columns);
			columnMaxGanjil.setLabel("Max");
			columnMaxGanjil.setWidth("0%");

			columnHurufGanjil.setParent(columns);
			columnHurufGanjil.setLabel("Huruf");
			columnHurufGanjil.setWidth("0%");

		} else {

			Auxhead auxhead = new Auxhead();
			auxhead.setParent(grid);

			Auxheader auxheader = new Auxheader("Siswa");
			auxheader.setColspan(2);
			auxheader.setParent(auxhead);

			Auxheader auxheaderGanjil = new Auxheader();
			auxheaderGanjil.setColspan(5);
			auxheaderGanjil.setParent(auxhead);

			Hbox hbox = new Hbox();
			auxheaderGanjil.appendChild(hbox);
			hbox.appendChild(new MyLabelBoldAja("Semester Ganjil"));

			Auxheader auxheaderGenap = new Auxheader();
			auxheaderGenap.setColspan(5);
			auxheaderGenap.setParent(auxhead);

			hbox = new Hbox();
			auxheaderGenap.appendChild(hbox);
			hbox.appendChild(new MyLabelBoldAja("Semester Genap"));

			Columns columns = new Columns();

			columns.setParent(grid);

			MyColumnConfig column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Foto");
			column.setWidth("70px");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Siswa");
			column.setWidth("25%");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Penilian");

			columnTotalGanjil.setParent(columns);
			columnTotalGanjil.setLabel("Total");
			columnTotalGanjil.setWidth("0%");

			columnMinGanjil.setParent(columns);
			columnMinGanjil.setLabel("Min");
			columnMinGanjil.setWidth("0%");

			columnMaxGanjil.setParent(columns);
			columnMaxGanjil.setLabel("Max");
			columnMaxGanjil.setWidth("0%");

			columnHurufGanjil.setParent(columns);
			columnHurufGanjil.setLabel("Huruf");
			columnHurufGanjil.setWidth("0%");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Penilian");

			columnTotalGenap.setParent(columns);
			columnTotalGenap.setLabel("Total");
			columnTotalGenap.setWidth("0%");

			columnMinGenap.setParent(columns);
			columnMinGenap.setLabel("Min");
			columnMinGenap.setWidth("0%");

			columnMaxGenap.setParent(columns);
			columnMaxGenap.setLabel("Max");
			columnMaxGenap.setWidth("0%");

			columnHurufGenap.setParent(columns);
			columnHurufGenap.setLabel("Huruf");
			columnHurufGenap.setWidth("0%");
		}

		columnTotalGanjil.setWidth("5%");

		columnMinGanjil.setWidth("5%");
		columnMaxGanjil.setWidth("5%");

		columnHurufGanjil.setWidth("5%");
		columnTotalGenap.setWidth("5%");

		columnMinGenap.setWidth("5%");
		columnMaxGenap.setWidth("5%");

		columnHurufGenap.setWidth("5%");

		Rows rows = new Rows();
		rows.setParent(grid);
		return rows;
	}

	private static Button[] buatDownloadDanUpload(final JadwalPelajaran jadwalPelajaran,
			final Matapelajaran matapelajaran, final Boolean hanyaValid,
			final GrupKategoriItemPenilaianSiswa grupKategoriItemPenilaianSiswa, final GrupPenilaian grupPenilaian,
			final List<JenisItemPenilaianSiswa> jenisItemPenilaianSiswas, final List<? extends VoKelasPunyaSiswa> siswas,
			final EventListener eventSetelahUpload, final int smtTunggal, final Textbox searchnama,
			final Combobox searchagama, final Combobox jenisKelamin) {
		int[] smts = new int[] { smtTunggal };

		if (jadwalPelajaran != null) {
			smts = new int[] { jadwalPelajaran.getSemester() };
		}

		if (grupKategoriItemPenilaianSiswa != null && grupKategoriItemPenilaianSiswa.getKhususSemester() != null) {
			smts = new int[] { grupKategoriItemPenilaianSiswa.getKhususSemester() };
		}

		if (grupPenilaian != null && grupPenilaian.getKhususSemester() != null) {
			smts = new int[] { grupPenilaian.getKhususSemester() };
		}

		List<String> columnHeadersAdding = new ArrayList<String>();
		for (int smt : smts) {
			for (final JenisItemPenilaianSiswa jenisItemPenilaianSiswa : jenisItemPenilaianSiswas) {
				columnHeadersAdding.add(jenisItemPenilaianSiswa.getNama() + "|" + smt);

				if (!jenisItemPenilaianSiswa.getTipeDataInputan().equals(JenisItemPenilaianSiswa.FORMULA)) {
					columnHeadersAdding.add(jenisItemPenilaianSiswa.getNama() + "|" + smt + "|verif");
				}

			}
			columnHeadersAdding.add("Total|" + smt);
		}

		EventListener dataAdding = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				int[] smts = new int[] { smtTunggal };

				if (jadwalPelajaran != null) {
					smts = new int[] { jadwalPelajaran.getSemester() };
				}

				if (grupPenilaian != null && grupPenilaian.getKhususSemester() != null) {
					smts = new int[] { grupPenilaian.getKhususSemester() };
				}

				Object[] objects = (Object[]) arg0.getData();
				KelasSiswaPunyaSiswa kelasSiswaPunyaSiswa = (KelasSiswaPunyaSiswa) objects[0];
				XSSFRow row = (XSSFRow) objects[2];

				row.getCell(0).setCellValue(kelasSiswaPunyaSiswa.getSiswa().getId());

				XSSFWorkbook workbook = (XSSFWorkbook) objects[3];

				XSSFFont hlink_font = workbook.createFont();
				XSSFCellStyle hlink_style = workbook.createCellStyle();
				hlink_style.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
				hlink_style.setFillForegroundColor(new XSSFColor(Color.LIGHT_GRAY));
				hlink_style.setFont(hlink_font);

				XSSFCellStyle hlink_style1 = workbook.createCellStyle();
				hlink_style1.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
				hlink_style1.setFillForegroundColor(new XSSFColor(Color.CYAN));
				hlink_style1.setFont(hlink_font);

				XSSFCellStyle hlink_style2 = workbook.createCellStyle();
				hlink_style2.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
				hlink_style2.setFillForegroundColor(new XSSFColor(Color.YELLOW));
				hlink_style2.setFont(hlink_font);

				int idex = 0;
				for (int smt : smts) {

					Double total = 0.0;

					try {
						Date sekarang = WaktuUtil.getDate();
						String formula = grupKategoriItemPenilaianSiswa.getFormula();
						String target = GrupPenilaianUtil.ambilTarget(formula, sekarang);
						total = kelasSiswaPunyaSiswa.retreiveTotalNilai(jenisItemPenilaianSiswas, target, matapelajaran,
								grupPenilaian, grupKategoriItemPenilaianSiswa, smt, hanyaValid);
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/DetailPenilaianSiswaHelper.java:404");
					}

					for (JenisItemPenilaianSiswa jenisItemPenilaianSiswa : jenisItemPenilaianSiswas) {

						String val = kelasSiswaPunyaSiswa.retreiveDetailNilai(jenisItemPenilaianSiswa,
								grupKategoriItemPenilaianSiswa, matapelajaran, smt, hanyaValid);
						XSSFCell cellTambahan = row.createCell(contents.length + idex);

						if (jenisItemPenilaianSiswa.getTipeDataInputan().equals(JenisItemPenilaianSiswa.ANGKA)
								|| jenisItemPenilaianSiswa.getTipeDataInputan().equals(JenisItemPenilaianSiswa.FORMULA)
								|| jenisItemPenilaianSiswa.getTipeDataInputan()
										.equals(JenisItemPenilaianSiswa.TEXT_ANGKA)) {
							try {
								cellTambahan.setCellValue(val == null || val.isEmpty() ? 0.0 : Double.parseDouble(val));
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/DetailPenilaianSiswaHelper.java:419");
							}
						} else {
							cellTambahan.setCellValue(val);
						}

						if (jenisItemPenilaianSiswa.getTipeDataInputan().equals(JenisItemPenilaianSiswa.FORMULA)) {
							cellTambahan.setCellStyle(hlink_style1);
						} else {
							cellTambahan.setCellStyle(hlink_style);

							idex++;

							Boolean sesuai = kelasSiswaPunyaSiswa.retreiveDetailVerify(jenisItemPenilaianSiswa,
									grupKategoriItemPenilaianSiswa, matapelajaran, smt);

							XSSFCell cellTambahanVerif = row.createCell(contents.length + idex);
							cellTambahanVerif.setCellValue(sesuai);
							cellTambahanVerif.setCellStyle(hlink_style2);
						}

						idex++;
					}

					XSSFCell cellTambahan = row.createCell(contents.length + idex);
					cellTambahan.setCellValue(total);
					cellTambahan.setCellStyle(hlink_style1);

					idex++;
				}

			}
		};

		final int[] smtss = smts;

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakDataCustomButton(KelasSiswaPunyaSiswa.class,
				new DataCriteria() {

					@Override
					public Object initCriteria(boolean order) {
						List< VoKelasPunyaSiswa> siswasTemp = new ArrayList<VoKelasPunyaSiswa>();

						String nama = searchnama.getValue().trim();
						Agama agama = (Agama) (searchagama.getSelectedItem() == null ? null
								: searchagama.getSelectedItem().getValue());

						String textJk = (jenisKelamin.getSelectedItem() == null ? null
								: (String) jenisKelamin.getSelectedItem().getValue());

						for (VoKelasPunyaSiswa kelasSiswaPunyaSiswa : siswas) {

							if (nama.trim().isEmpty() ||

									(!nama.trim().toLowerCase().isEmpty()
											&& !kelasSiswaPunyaSiswa.getSiswa().getNama().isEmpty()
											&& kelasSiswaPunyaSiswa.getSiswa().getNama().toLowerCase()
													.contains(nama.trim().toLowerCase()))

									||

									(!nama.trim().toLowerCase().isEmpty()
											&& !kelasSiswaPunyaSiswa.getSiswa().getNomorInduk().isEmpty()
											&& kelasSiswaPunyaSiswa.getSiswa().getNomorInduk().toLowerCase()
													.contains(nama.trim().toLowerCase()))

									||

									(!nama.trim().toLowerCase().isEmpty()
											&& !kelasSiswaPunyaSiswa.getSiswa().getNomorIndukNasional().isEmpty()
											&& kelasSiswaPunyaSiswa.getSiswa().getNomorIndukNasional().toLowerCase()
													.contains(nama.trim().toLowerCase()))

									||

									(!nama.trim().toLowerCase().isEmpty()
											&& !kelasSiswaPunyaSiswa.getSiswa().getNomorIndukSantri().isEmpty()
											&& kelasSiswaPunyaSiswa.getSiswa().getNomorIndukSantri().toLowerCase()
													.contains(nama.trim().toLowerCase()))

				) {

								String jk = kelasSiswaPunyaSiswa.getSiswa() == null ? ""
										: kelasSiswaPunyaSiswa.getSiswa().getJenisKelamin();

								Agama agm = kelasSiswaPunyaSiswa.getSiswa() == null ? null
										: kelasSiswaPunyaSiswa.getSiswa().getAgama();

								if (agama == null || (agm != null && agm.getId().equals(agama.getId()))) {

									if ((textJk == null || textJk.trim().isEmpty())
											|| (textJk != null && jk.trim().equals(textJk))) {
										siswasTemp.add(kelasSiswaPunyaSiswa);
									}
								}
							}

						}

						return siswasTemp;
					}
				}, "Download", "/img/print.png", columnHeadersAdding, dataAdding, true, null, "Nilai", contents);

		MyToolbarbuttonConfig upload = new MyToolbarbuttonConfig("Upload " + Common.ukuranLabelFileUpload(),
				"/img/excel.png");
		upload.setUpload(Common.ukuranFileUpload());
		upload.addEventListener("onUpload", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				UploadEvent uploadEvent = (UploadEvent) event;
				Media media = uploadEvent.getMedia();
				if (!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media))
					return;
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
							uploadDataNilai(file, eventSetelahUpload, contents, smtss, jenisItemPenilaianSiswas, siswas,
									matapelajaran, grupKategoriItemPenilaianSiswa, grupPenilaian);
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

		return new Button[] { cetakToolbarbutton, upload };
	}

	private static Rows createRowsTanpaMinMax(JadwalPelajaran jadwalPelajaran, GrupPenilaian grupPenilaian,
			GrupKategoriItemPenilaianSiswa grupKategoriItemPenilaianSiswa, KelasSiswa kelasSiswa,
			Matapelajaran matapelajaran, Tbmuser tbmuser, EventListener eventListener, Groupbox groupboxData,
			Boolean hanyaValid, List<JenisItemPenilaianSiswa> jenisItemPenilaianSiswas, List<? extends VoKelasPunyaSiswa> siswas,
			final Textbox nama, final Combobox searchagama, final Combobox jenisKelamin, final Integer smtPilih,
			boolean langsung) {
		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(langsung ? 5 : 50);
		grid.getPagingChild().setMold("os");
		grid.setParent(groupboxData);

		MyColumnConfig columnTotalGanjil = new MyColumnConfig();

		MyColumnConfig columnHurufGanjil = new MyColumnConfig();

		MyColumnConfig columnTotalGenap = new MyColumnConfig();

		MyColumnConfig columnHurufGenap = new MyColumnConfig();

		if (smtPilih != null || jadwalPelajaran != null
				|| (grupKategoriItemPenilaianSiswa != null
						&& grupKategoriItemPenilaianSiswa.getKhususSemester() != null)
				|| (grupPenilaian != null && grupPenilaian.getKhususSemester() != null)) {

			Auxhead auxhead = new Auxhead();
			auxhead.setParent(grid);

			Auxheader auxheader = new Auxheader("Siswa");
			auxheader.setColspan(2);
			auxheader.setParent(auxhead);

			Auxheader auxheaderData = new Auxheader();
			auxheaderData.setColspan(3);
			auxheaderData.setParent(auxhead);

			if ((kelasSiswa == null || kelasSiswa.getDikunci() == null)

					&& (jadwalPelajaran == null || jadwalPelajaran.getDikunci() == null)

					&& (jadwalPelajaran == null || jadwalPelajaran.getMasaJadwalPelajaran() == null
							|| jadwalPelajaran.getMasaJadwalPelajaran().getDikunci() == null)) {

				Hbox hbox = new Hbox();
				auxheaderData.appendChild(hbox);

				if (grupKategoriItemPenilaianSiswa != null) {

					int smt = 0;

					if (jadwalPelajaran != null) {
						smt = jadwalPelajaran.getSemester();
					}

					if (grupKategoriItemPenilaianSiswa != null
							&& grupKategoriItemPenilaianSiswa.getKhususSemester() != null) {
						smt = grupKategoriItemPenilaianSiswa.getKhususSemester();
					}

					if (grupPenilaian != null && grupPenilaian.getKhususSemester() != null) {
						smt = grupPenilaian.getKhususSemester();
					}

					if (smtPilih != null) {
						smt = smtPilih;
					}

					Konfigurasi ganjil = Common.getKonfigurasi(
							"kunci_nilai_sekolah_" + matapelajaran.getId() + "_" + grupPenilaian.getId() + "_"
									+ grupKategoriItemPenilaianSiswa.getId() + "_" + kelasSiswa.getId() + "_" + smt,
							Konfigurasi.AKTIF);

					hbox.appendChild(new Space());
					hbox.appendChild(new Space());
					hbox.appendChild(new Space());
					DetailPenilaianSiswaHelper.tampilKunci(hbox, ganjil, tbmuser, eventListener);

					Button[] buttons = DetailPenilaianSiswaHelper.buatDownloadDanUpload(jadwalPelajaran, matapelajaran,
							hanyaValid, grupKategoriItemPenilaianSiswa, grupPenilaian, jenisItemPenilaianSiswas, siswas,
							eventListener, smt, nama, searchagama, jenisKelamin);

					Button download = buttons[0];
					Button upload = buttons[1];

					Vbox vbox = new Vbox();
					hbox.appendChild(vbox);

					vbox.appendChild(upload);
					vbox.appendChild(download);

					if ((ganjil != null && ganjil.getDikunci() != null)) {
						upload.setVisible(false);
						download.setVisible(false);
					}
				}
			}

			Columns columns = new Columns();

			columns.setParent(grid);

			MyColumnConfig column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Foto");
			column.setWidth("70px");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Siswa");
			column.setWidth("25%");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Penilian");

			columnTotalGanjil.setParent(columns);
			columnTotalGanjil.setLabel("Total");
			columnTotalGanjil.setWidth("0%");

			columnHurufGanjil.setParent(columns);
			columnHurufGanjil.setLabel("Huruf");
			columnHurufGanjil.setWidth("0%");

		} else {

			Konfigurasi genap = null;
			Konfigurasi ganjil = null;

			Auxhead auxhead = new Auxhead();
			auxhead.setParent(grid);

			Auxheader auxheader = new Auxheader("Siswa");
			auxheader.setColspan(2);
			auxheader.setParent(auxhead);

			Auxheader auxheaderGanjil = new Auxheader();
			auxheaderGanjil.setColspan(3);
			auxheaderGanjil.setParent(auxhead);

			Hbox hbox = new Hbox();
			auxheaderGanjil.appendChild(hbox);
			hbox.appendChild(new MyLabelBoldMerah("Ganjil"));

			if ((kelasSiswa == null || kelasSiswa.getDikunci() == null)

					&& (jadwalPelajaran == null || jadwalPelajaran.getDikunci() == null)

					&& (jadwalPelajaran == null || jadwalPelajaran.getMasaJadwalPelajaran() == null
							|| jadwalPelajaran.getMasaJadwalPelajaran().getDikunci() == null)) {
				if (grupKategoriItemPenilaianSiswa != null) {
					ganjil = Common.getKonfigurasi(
							"kunci_nilai_sekolah_" + matapelajaran.getId() + "_" + grupPenilaian.getId() + "_"
									+ grupKategoriItemPenilaianSiswa.getId() + "_" + kelasSiswa.getId() + "_1",
							Konfigurasi.AKTIF);

					hbox.appendChild(new Space());
					hbox.appendChild(new Space());
					hbox.appendChild(new Space());
					DetailPenilaianSiswaHelper.tampilKunci(hbox, ganjil, tbmuser, eventListener);

					Button[] buttons = DetailPenilaianSiswaHelper.buatDownloadDanUpload(jadwalPelajaran, matapelajaran,
							hanyaValid, grupKategoriItemPenilaianSiswa, grupPenilaian, jenisItemPenilaianSiswas, siswas,
							eventListener, 1, nama, searchagama, jenisKelamin);

					Button download = buttons[0];
					Button upload = buttons[1];

					Vbox vbox = new Vbox();
					hbox.appendChild(vbox);

					vbox.appendChild(upload);
					vbox.appendChild(download);

					if ((ganjil != null && ganjil.getDikunci() != null)) {
						upload.setVisible(false);
						download.setVisible(false);
					}
				}
			}

			Auxheader auxheaderGenap = new Auxheader();
			auxheaderGenap.setColspan(3);
			auxheaderGenap.setParent(auxhead);

			hbox = new Hbox();
			auxheaderGenap.appendChild(hbox);
			hbox.appendChild(new MyLabelBoldMerah("Genap"));

			if ((kelasSiswa == null || kelasSiswa.getDikunci() == null)

					&& (jadwalPelajaran == null || jadwalPelajaran.getDikunci() == null)

					&& (jadwalPelajaran == null || jadwalPelajaran.getMasaJadwalPelajaran() == null
							|| jadwalPelajaran.getMasaJadwalPelajaran().getDikunci() == null)) {

				if (grupKategoriItemPenilaianSiswa != null) {
					genap = Common.getKonfigurasi(
							"kunci_nilai_sekolah_" + matapelajaran.getId() + "_" + grupPenilaian.getId() + "_"
									+ grupKategoriItemPenilaianSiswa.getId() + "_" + kelasSiswa.getId() + "_2",
							Konfigurasi.AKTIF);

					hbox.appendChild(new Space());
					hbox.appendChild(new Space());
					hbox.appendChild(new Space());
					DetailPenilaianSiswaHelper.tampilKunci(hbox, genap, tbmuser, eventListener);

					Button[] buttons = DetailPenilaianSiswaHelper.buatDownloadDanUpload(jadwalPelajaran, matapelajaran,
							hanyaValid, grupKategoriItemPenilaianSiswa, grupPenilaian, jenisItemPenilaianSiswas, siswas,
							eventListener, 2, nama, searchagama, jenisKelamin);

					Button download = buttons[0];
					Button upload = buttons[1];

					Vbox vbox = new Vbox();
					hbox.appendChild(vbox);

					vbox.appendChild(upload);
					vbox.appendChild(download);

					if ((genap != null && genap.getDikunci() != null)) {
						upload.setVisible(false);
						download.setVisible(false);
					}

				}
			}

			Columns columns = new Columns();

			columns.setParent(grid);

			MyColumnConfig column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Foto");
			column.setWidth("70px");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Siswa");
			column.setWidth("25%");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Penilian");

			columnTotalGanjil.setParent(columns);
			columnTotalGanjil.setLabel("Total");
			columnTotalGanjil.setWidth("0%");

			columnHurufGanjil.setParent(columns);
			columnHurufGanjil.setLabel("Huruf");
			columnHurufGanjil.setWidth("0%");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Penilian");

			columnTotalGenap.setParent(columns);
			columnTotalGenap.setLabel("Total");
			columnTotalGenap.setWidth("0%");

			columnHurufGenap.setParent(columns);
			columnHurufGenap.setLabel("Huruf");
			columnHurufGenap.setWidth("0%");

		}

		columnTotalGanjil.setWidth("5%");

		columnHurufGanjil.setWidth("5%");
		columnTotalGenap.setWidth("5%");

		columnHurufGenap.setWidth("5%");

		Rows rows = new Rows();
		rows.setParent(grid);
		return rows;
	}

	@SuppressWarnings({ "unchecked", "deprecation" })
	public static void displayPenilaian(final JadwalPelajaran jadwalPelajaran, final KurikulumPunyaMatapelajaran kur,
			final Component detail, final KelasSiswa kelasSiswa, List<? extends VoKelasPunyaSiswa> siswasTemp) throws Exception {

		final Tbmuser tbmuser = Common.getCurrentUser();
		final Boolean hanyaValid = tbmuser != null && (tbmuser.getSiswa() != null || tbmuser.getCalonSiswa() != null)
				? true
				: null;
		final Matapelajaran matapelajaran = jadwalPelajaran != null && jadwalPelajaran.getMatapelajaran() != null
				? jadwalPelajaran.getMatapelajaran()
				: (kur == null ? null : kur.getMatapelajaran());

		final List<? extends VoKelasPunyaSiswa> siswas = siswasTemp != null
				? KelasSiswaPunyaSiswa.filterMk(siswasTemp, matapelajaran)
				: null;

		System.out.println("siswasTemp -> " + (siswasTemp == null ? "" : siswasTemp.size()) + ", siswas -> "
				+ (siswas == null ? "" : siswas.size()) + ", matapelajaran -> " + matapelajaran);

		Session session = HibernateUtil.currentSession();

		JenisPenilaian jenisPenilaian = matapelajaran == null ? null : matapelajaran.getJenisPenilaian();
		if (kur != null && kur.getKurikulumSekolah() != null && kur.getKurikulumSekolah().getJenisPenilaian() != null) {
			jenisPenilaian = kur.getKurikulumSekolah().getJenisPenilaian();
		}

		List<GrupPenilaian> grupPenilaians = ConstantValues.simpleList(session
				.createCriteria(DetailJenisPenilaian.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.eq("jenisPenilaian", jenisPenilaian)).add(Restrictions.isNotNull("grupPenilaian.id"))
				.setProjection(Projections.groupProperty("grupPenilaian.id")), GrupPenilaian.class, false);

		final int tinggi = siswas != null ? siswas.size() : 0;

		ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
		groupbox.setStyle("min-height: " + (170 + (250 * tinggi)) + "px;");
		groupbox.setParent(detail);

		Tabbox tabbox = new Tabbox();
		tabbox.setParent(groupbox);
		tabbox.setHeight("100%");
		tabbox.setWidth("100%");

		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		Collections.sort(grupPenilaians);

		int indexTab = 0;
		for (final GrupPenilaian grupPenilaian : grupPenilaians) {

			if (grupPenilaian != null && kelasSiswa.getTingkat() > 0 && grupPenilaian.getKhususTingkat() != null
					&& !grupPenilaian.getKhususTingkat().equals(kelasSiswa.getTingkat())) {
				continue;
			}

			MyTabConfig tabSoal = new MyTabConfig(grupPenilaian.getNama());
			tabSoal.setParent(tabs);

			final Tabpanel tabpanel = new ais.ui.util.MyTabpanel();
			tabpanel.setStyle("min-height: 250px;");
			tabpanel.setHeight((170 + (250 * tinggi)) + "px");
			tabpanel.setParent(tabpanels);

			EventListener tabTabbox = new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					if (tabpanel.getChildren().isEmpty()) {

						Tabbox tabbox1 = new Tabbox();
						tabbox1.setParent(tabpanel);
						tabbox1.setHeight("100%");
						tabbox1.setWidth("100%");

						Tabs tabs1 = new Tabs();
						tabs1.setParent(tabbox1);

						Tabpanels tabpanels1 = new Tabpanels();
						tabpanels1.setParent(tabbox1);

						Session session = HibernateUtil.currentSession();
						final List<GrupKategoriItemPenilaianSiswa> grupKategoriItemPenilaianSiswas = ConstantValues
								.simpleList(
										session.createCriteria(DetailGrupPenilaian.class)
												.add(Restrictions.or(Restrictions.isNull("aktif"),
														Restrictions.eq("aktif", true)))
												.add(Restrictions.isNotNull("grupKategoriItemPenilaianSiswa"))
												.setProjection(
														Projections.groupProperty("grupKategoriItemPenilaianSiswa.id"))
												.add(Restrictions.eq("grupPenilaian", grupPenilaian)),
										GrupKategoriItemPenilaianSiswa.class, false);

						if (grupKategoriItemPenilaianSiswas.isEmpty()) {
							return;
						}

						Collections.sort(grupKategoriItemPenilaianSiswas);

						grupKategoriItemPenilaianSiswas.add(null);
						int indexTab = 0;
						for (final GrupKategoriItemPenilaianSiswa grupKategoriItemPenilaianSiswa : grupKategoriItemPenilaianSiswas) {

							if (grupKategoriItemPenilaianSiswa != null && kelasSiswa.getTingkat() > 0
									&& grupKategoriItemPenilaianSiswa.getKhususTingkat() != null
									&& !grupKategoriItemPenilaianSiswa.getKhususTingkat()
											.equals(kelasSiswa.getTingkat())) {
								continue;
							}

							if (grupKategoriItemPenilaianSiswa == null && grupPenilaian.getAdaTotal()) {

								MyTabConfig tabSoal1 = new MyTabConfig("Total");
								tabSoal1.setParent(tabs1);

								final Tabpanel tabpanel1 = new ais.ui.util.MyTabpanel();
								tabpanel1.setStyle("min-height: 250px;");
								tabpanel1.setHeight((170 + (250 * tinggi)) + "px");
								tabpanel1.setParent(tabpanels1);

								tabSoal1.addEventListener("onClick", new EventListener() {

									private Textbox nama;
									private Combobox searchagama;
									private String textNama = "";
									private String textJk = null;
									private Agama agama = null;
									private Combobox jenisKelamin = null;

									EventListener getThis() {
										return this;
									}

									@Override
									public void onEvent(Event arg0) throws Exception {
										Common.clear(tabpanel1);

										Groupbox groupboxData = new Groupbox();
										groupboxData.setParent(tabpanel1);

										Toolbar toolbar = new Toolbar();
										toolbar.setParent(groupboxData);

										nama = new Textbox(textNama);
										searchagama = new Combobox();
										Common.insertComboDanSemua(searchagama, new String[] { "nama" }, "keterangan",
												Agama.class, "Semua Agama", Restrictions.eq("aktif", true));
										Common.selectComboItem(searchagama, agama);

										jenisKelamin = new Combobox();
										MyComboitemConfig comboitem = new MyComboitemConfig();
										comboitem.setLabel("Laki-laki");
										comboitem.setValue("Laki-laki");
										jenisKelamin.appendChild(comboitem);
										comboitem = new MyComboitemConfig();
										comboitem.setLabel("Perempuan");
										comboitem.setValue("Perempuan");
										jenisKelamin.appendChild(comboitem);
										comboitem = new MyComboitemConfig();
										comboitem.setLabel("Semua Kelamin");
										comboitem.setValue(null);
										jenisKelamin.appendChild(comboitem);
										jenisKelamin.setReadonly(true);

										Common.selectComboItem(jenisKelamin, textJk);

										toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Siswa : ")));
										toolbar.appendChild(nama);
										MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Cari",
												"/img/svg/search.svg");
										button.addEventListener("onClick", new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {
												textNama = nama.getValue().trim();
												agama = (Agama) (searchagama.getSelectedItem() == null ? null
														: searchagama.getSelectedItem().getValue());

												textJk = (jenisKelamin.getSelectedItem() == null ? null
														: (String) jenisKelamin.getSelectedItem().getValue());
												Common.clear(tabpanel1);
												getThis().onEvent(arg0);
											}
										});
										button.setParent(toolbar);
										nama.setCols(10);
										nama.addEventListener("onOK", new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {
												textNama = nama.getValue().trim();
												agama = (Agama) (searchagama.getSelectedItem() == null ? null
														: searchagama.getSelectedItem().getValue());
												textJk = (jenisKelamin.getSelectedItem() == null ? null
														: (String) jenisKelamin.getSelectedItem().getValue());
												Common.clear(tabpanel1);
												getThis().onEvent(arg0);
											}
										});
										searchagama.setParent(toolbar);
										searchagama.setCols(10);
										searchagama.addEventListener("onChange", new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {
												textNama = nama.getValue().trim();
												agama = (Agama) (searchagama.getSelectedItem() == null ? null
														: searchagama.getSelectedItem().getValue());
												textJk = (jenisKelamin.getSelectedItem() == null ? null
														: (String) jenisKelamin.getSelectedItem().getValue());
												Common.clear(tabpanel1);
												getThis().onEvent(arg0);
											}
										});

										jenisKelamin.setParent(toolbar);
										jenisKelamin.setCols(10);
										jenisKelamin.addEventListener("onChange", new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {
												textNama = nama.getValue().trim();
												agama = (Agama) (searchagama.getSelectedItem() == null ? null
														: searchagama.getSelectedItem().getValue());
												textJk = (jenisKelamin.getSelectedItem() == null ? null
														: (String) jenisKelamin.getSelectedItem().getValue());
												Common.clear(tabpanel1);
												getThis().onEvent(arg0);
											}
										});

										int[] smts = new int[] { 1, 2 };

										if (jadwalPelajaran != null) {
											smts = new int[] { jadwalPelajaran.getSemester() };
										}

										if (grupPenilaian != null && grupPenilaian.getKhususSemester() != null) {
											smts = new int[] { grupPenilaian.getKhususSemester() };
										}

										List<String> columnHeadersAdding = new ArrayList<String>();
										for (int smt : smts) {

											columnHeadersAdding.add("Penilaian|" + smt);
											columnHeadersAdding.add("Total|" + smt);
											columnHeadersAdding.add("Min|" + smt);
											columnHeadersAdding.add("Max|" + smt);
											columnHeadersAdding.add("Huruf|" + smt);
										}

										EventListener dataAdding = new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {

												int[] smts = new int[] { 1, 2 };

												if (jadwalPelajaran != null) {
													smts = new int[] { jadwalPelajaran.getSemester() };
												}

												if (grupPenilaian != null
														&& grupPenilaian.getKhususSemester() != null) {
													smts = new int[] { grupPenilaian.getKhususSemester() };
												}

												Object[] objects = (Object[]) arg0.getData();
												KelasSiswaPunyaSiswa kelasSiswaPunyaSiswa = (KelasSiswaPunyaSiswa) objects[0];
												XSSFRow row = (XSSFRow) objects[2];
												row.getCell(0).setCellValue(kelasSiswaPunyaSiswa.getSiswa().getId());

												Siswa siswa = kelasSiswaPunyaSiswa.getSiswa();

												XSSFWorkbook workbook = (XSSFWorkbook) objects[3];

												XSSFFont hlink_font = workbook.createFont();
												XSSFCellStyle hlink_style = workbook.createCellStyle();
												hlink_style.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
												hlink_style.setFillForegroundColor(new XSSFColor(Color.LIGHT_GRAY));
												hlink_style.setFont(hlink_font);

												XSSFCellStyle hlink_style1 = workbook.createCellStyle();
												hlink_style1.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
												hlink_style1.setFillForegroundColor(new XSSFColor(Color.CYAN));
												hlink_style1.setFont(hlink_font);

												int idex = 0;
												for (int smt : smts) {

													Double total = 0.0;

													Double min = 0.0;

													Double max = 0.0;

													try {
														Date sekarang = WaktuUtil.getDate();
														String formula = grupPenilaian.getFormula();

														String target = GrupPenilaianUtil.ambilTarget(formula,
																sekarang);

														total = kelasSiswaPunyaSiswa.retreiveTotalNilaiTotal(target,
																matapelajaran, grupPenilaian, smt,
																grupKategoriItemPenilaianSiswas);

														target = GrupPenilaianUtil.ambilTargetMin(formula, sekarang);

														min = kelasSiswaPunyaSiswa.retreiveTotalNilaiTotal(target,
																matapelajaran, grupPenilaian, smt,
																grupKategoriItemPenilaianSiswas);

														target = GrupPenilaianUtil.ambilTargetMax(formula, sekarang);

														max = kelasSiswaPunyaSiswa.retreiveTotalNilaiTotal(target,
																matapelajaran, grupPenilaian, smt,
																grupKategoriItemPenilaianSiswas);

													} catch (Exception e) {
														e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/helper/DetailPenilaianSiswaHelper.java:1173");
													}

													JSONObject js = new JSONObject();
													try {
														js = new JSONObject(
																smt == 1 ? kelasSiswaPunyaSiswa.getKeterangan1()
																		: kelasSiswaPunyaSiswa.getKeterangan2());
													} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/DetailPenilaianSiswaHelper.java:1181");
													}

													String keyKet = matapelajaran.getId() + "_" + grupPenilaian.getId();

													String ket = js.isNull(keyKet) ? "" : js.getString(keyKet);

													XSSFCell cellTambahan = row.createCell(contents.length + idex);
													cellTambahan.setCellValue(ket);

													cellTambahan.setCellStyle(hlink_style);

													idex++;

													cellTambahan = row.createCell(contents.length + idex);
													cellTambahan.setCellValue(Common.numberFormat.get().format(total));

													cellTambahan.setCellStyle(hlink_style);

													idex++;

													cellTambahan = row.createCell(contents.length + idex);
													cellTambahan.setCellValue(Common.numberFormat.get().format(min));

													cellTambahan.setCellStyle(hlink_style);

													idex++;

													cellTambahan = row.createCell(contents.length + idex);
													cellTambahan.setCellValue(Common.numberFormat.get().format(max));

													cellTambahan.setCellStyle(hlink_style);

													idex++;

													NilaiHurufSekolah nilaiHurufSekolah = NilaiHurufSekolah
															.getNilaiHurufSekolah(total, siswa.getTahunMasuk(),
																	siswa.getSekolah(), siswa.getYayasan(),
																	kelasSiswaPunyaSiswa.getKelasSiswa()
																			.getTahunAjaran(),
																	smt % 2 == 0 ? Perkuliahan.GENAP
																			: Perkuliahan.GANJIL,
																	grupPenilaian.getJenisNilaiHuruf());

													cellTambahan = row.createCell(contents.length + idex);
													cellTambahan.setCellValue(nilaiHurufSekolah == null ? ""
															: nilaiHurufSekolah.getNilaiHuruf());
													cellTambahan.setCellStyle(hlink_style1);
													idex++;
												}
											}
										};

										MyToolbarbuttonConfig cetakToolbarbutton = Common
												.cetakDataCustomButton(KelasSiswaPunyaSiswa.class, new DataCriteria() {

													@Override
													public Object initCriteria(boolean order) {

														List< VoKelasPunyaSiswa> siswasTemp = new ArrayList<VoKelasPunyaSiswa>();

														for (VoKelasPunyaSiswa kelasSiswaPunyaSiswa : siswas) {

															if (nama.getValue().trim().isEmpty() ||

																	(!nama.getValue().trim().toLowerCase().isEmpty()
																			&& !kelasSiswaPunyaSiswa.getSiswa()
																					.getNama().isEmpty()
																			&& kelasSiswaPunyaSiswa.getSiswa().getNama()
																					.toLowerCase()
																					.contains(nama.getValue().trim()
																							.toLowerCase()))

																	||

																	(!nama.getValue().trim().toLowerCase().isEmpty()
																			&& !kelasSiswaPunyaSiswa.getSiswa()
																					.getNomorInduk().isEmpty()
																			&& kelasSiswaPunyaSiswa.getSiswa()
																					.getNomorInduk().toLowerCase()
																					.contains(nama.getValue().trim()
																							.toLowerCase()))

																	||

																	(!nama.getValue().trim().toLowerCase().isEmpty()
																			&& !kelasSiswaPunyaSiswa.getSiswa()
																					.getNomorIndukNasional().isEmpty()
																			&& kelasSiswaPunyaSiswa.getSiswa()
																					.getNomorIndukNasional()
																					.toLowerCase()
																					.contains(nama.getValue().trim()
																							.toLowerCase()))

																	||

																	(!nama.getValue().trim().toLowerCase().isEmpty()
																			&& !kelasSiswaPunyaSiswa.getSiswa()
																					.getNomorIndukSantri().isEmpty()
																			&& kelasSiswaPunyaSiswa.getSiswa()
																					.getNomorIndukSantri().toLowerCase()
																					.contains(nama.getValue().trim()
																							.toLowerCase()))

												) {

																String jk = kelasSiswaPunyaSiswa.getSiswa() == null ? ""
																		: kelasSiswaPunyaSiswa.getSiswa()
																				.getJenisKelamin();

																Agama agm = kelasSiswaPunyaSiswa.getSiswa() == null
																		? null
																		: kelasSiswaPunyaSiswa.getSiswa().getAgama();

																if (agama == null || (agm != null
																		&& agm.getId().equals(agama.getId()))) {

																	if ((textJk == null || textJk.trim().isEmpty())
																			|| (textJk != null
																					&& jk.trim().equals(textJk))) {
																		siswasTemp.add(kelasSiswaPunyaSiswa);
																	}
																}
															}

														}

														return siswasTemp;
													}
												}, "Download Nilai", "/img/print.png", columnHeadersAdding, dataAdding,
														true, null, "Nilai", contents);
										toolbar.appendChild(cetakToolbarbutton);

										Rows rows = DetailPenilaianSiswaHelper.createRows(jadwalPelajaran,
												grupPenilaian, grupKategoriItemPenilaianSiswa, kelasSiswa,
												matapelajaran, tbmuser, new EventListener() {

													@Override
													public void onEvent(Event arg0) throws Exception {
														Common.clear(detail);

														DetailPenilaianSiswaHelper.displayPenilaian(jadwalPelajaran,
																kur, detail, kelasSiswa, siswas);
													}
												}, groupboxData);

										for (final VoKelasPunyaSiswa kelasSiswaPunyaSiswa : siswas) {

											if (nama.getValue().trim().isEmpty() ||

													(!nama.getValue().trim().toLowerCase().isEmpty()
															&& !kelasSiswaPunyaSiswa.getSiswa().getNama().isEmpty()
															&& kelasSiswaPunyaSiswa.getSiswa().getNama().toLowerCase()
																	.contains(nama.getValue().trim().toLowerCase()))

													||

													(!nama.getValue().trim().toLowerCase().isEmpty()
															&& !kelasSiswaPunyaSiswa.getSiswa().getNomorInduk()
																	.isEmpty()
															&& kelasSiswaPunyaSiswa.getSiswa().getNomorInduk()
																	.toLowerCase()
																	.contains(nama.getValue().trim().toLowerCase()))

													||

													(!nama.getValue().trim().toLowerCase().isEmpty()
															&& !kelasSiswaPunyaSiswa.getSiswa().getNomorIndukNasional()
																	.isEmpty()
															&& kelasSiswaPunyaSiswa.getSiswa().getNomorIndukNasional()
																	.toLowerCase()
																	.contains(nama.getValue().trim().toLowerCase()))

													||

													(!nama.getValue().trim().toLowerCase().isEmpty()
															&& !kelasSiswaPunyaSiswa.getSiswa().getNomorIndukSantri()
																	.isEmpty()
															&& kelasSiswaPunyaSiswa.getSiswa().getNomorIndukSantri()
																	.toLowerCase()
																	.contains(nama.getValue().trim().toLowerCase()))

											) {

												String jk = kelasSiswaPunyaSiswa.getSiswa() == null ? ""
														: kelasSiswaPunyaSiswa.getSiswa().getJenisKelamin();

												Agama agm = kelasSiswaPunyaSiswa.getSiswa() == null ? null
														: kelasSiswaPunyaSiswa.getSiswa().getAgama();

												if (agama == null
														|| (agm != null && agm.getId().equals(agama.getId()))) {

													if ((textJk == null || textJk.trim().isEmpty())
															|| (textJk != null && jk.trim().equals(textJk))) {

														Row row = new Row();
														row.setValign("top");
														row.setValign("top");
														row.setParent(rows);

														Siswa siswa = kelasSiswaPunyaSiswa.getSiswa();
														CommonMedia.tampilkanGambarKecil(siswa).setParent(row);
														Vbox aa;
														(aa = RevisiHelper.createNewRevisi(KelasSiswaPunyaSiswa.class,
																kelasSiswaPunyaSiswa, siswa.getNomorInduk()))
																.setParent(row);
														new Label(siswa.getNomorIndukNasional()).setParent(aa);
														new Label(siswa.getNama()).setParent(aa);

														for (final int smt : smts) {
															Double total = 0.0;

															Double min = 0.0;

															Double max = 0.0;

															try {
																Date sekarang = WaktuUtil.getDate();
																String formula = grupPenilaian.getFormula();

																String target = GrupPenilaianUtil.ambilTarget(formula,
																		sekarang);

																total = kelasSiswaPunyaSiswa.retreiveTotalNilaiTotal(
																		target, matapelajaran, grupPenilaian, smt,
																		grupKategoriItemPenilaianSiswas);

																target = GrupPenilaianUtil.ambilTargetMin(formula,
																		sekarang);

																min = kelasSiswaPunyaSiswa.retreiveTotalNilaiTotal(
																		target, matapelajaran, grupPenilaian, smt,
																		grupKategoriItemPenilaianSiswas);

																target = GrupPenilaianUtil.ambilTargetMax(formula,
																		sekarang);

																max = kelasSiswaPunyaSiswa.retreiveTotalNilaiTotal(
																		target, matapelajaran, grupPenilaian, smt,
																		grupKategoriItemPenilaianSiswas);

															} catch (Exception e) {
																e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/helper/DetailPenilaianSiswaHelper.java:1424");
															}

															JSONObject js = new JSONObject();
															try {
																js = new JSONObject(smt == 1
																		? kelasSiswaPunyaSiswa.getKeterangan1()
																		: kelasSiswaPunyaSiswa.getKeterangan2());
															} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/DetailPenilaianSiswaHelper.java:1432");
															}

															String keyKet = matapelajaran.getId() + "_"
																	+ grupPenilaian.getId();

															String ket = js.isNull(keyKet) ? "" : js.getString(keyKet);

															MyTextbox myTextbox = new MyTextbox(ket);
															myTextbox.addEventListener("onChange", new EventListener() {

																@Override
																public void onEvent(Event arg0) throws Exception {
																	String jumlah = ((Textbox) arg0.getTarget())
																			.getValue().trim();

																	JSONObject js = new JSONObject();
																	try {
																		js = new JSONObject(smt == 1
																				? kelasSiswaPunyaSiswa.getKeterangan1()
																				: kelasSiswaPunyaSiswa
																						.getKeterangan2());
																	} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/DetailPenilaianSiswaHelper.java:1454");
																	}

																	String keyKet = matapelajaran.getId() + "_"
																			+ grupPenilaian.getId();

																	js.put(keyKet, jumlah);

																	if (smt == 1) {
																		kelasSiswaPunyaSiswa
																				.setKeterangan1(js.toString());
																	} else {
																		kelasSiswaPunyaSiswa
																				.setKeterangan2(js.toString());
																	}
																	Common.refreshUpdate(kelasSiswaPunyaSiswa);
																}
															});
															myTextbox.setParent(row);
															myTextbox.setWidth("95%");
															myTextbox.setRows(2);

															RevisiHelper
																	.createNewRevisi(GrupPenilaian.class, grupPenilaian,
																			Common.numberFormat.get().format(total))
																	.setParent(row);

															RevisiHelper
																	.createNewRevisi(GrupPenilaian.class, grupPenilaian,
																			Common.numberFormat.get().format(min))
																	.setParent(row);

															RevisiHelper
																	.createNewRevisi(GrupPenilaian.class, grupPenilaian,
																			Common.numberFormat.get().format(max))
																	.setParent(row);

															NilaiHurufSekolah nilaiHurufSekolah = NilaiHurufSekolah
																	.getNilaiHurufSekolah(total, siswa.getTahunMasuk(),
																			siswa.getSekolah(), siswa.getYayasan(),
																			kelasSiswaPunyaSiswa
																					.ambilKelasSiswa() == null
																							? ""
																							: kelasSiswaPunyaSiswa
																									.ambilKelasSiswa()
																									.getTahunAjaran(),
																			smt % 2 == 0 ? Perkuliahan.GENAP
																					: Perkuliahan.GANJIL,
																			grupPenilaian.getJenisNilaiHuruf());

															MyLabelBoldAja labelNilaiHuruf = new MyLabelBoldAja();
															labelNilaiHuruf.setValue(nilaiHurufSekolah == null ? ""
																	: nilaiHurufSekolah.getNilaiHuruf());
															labelNilaiHuruf.setParent(row);

														}
													}
												}
											}
										}
									}
								});

							} else {

								List<KategoriItemPenilaianSiswa> kategoriItemPenilaianSiswasId = ConstantValues
										.simpleList(session.createCriteria(DetailGrupKategoriItemPenilaianSiswa.class)

												.add(Restrictions.eq("grupKategoriItemPenilaianSiswa",
														grupKategoriItemPenilaianSiswa))
												.add(Restrictions.or(Restrictions.isNull("aktif"),
														Restrictions.eq("aktif", true)))

												.setProjection(
														Projections.groupProperty("kategoriItemPenilaianSiswa.id")),
												KategoriItemPenilaianSiswa.class, false);

								System.out.println("kategoriItemPenilaianSiswas -> " + kategoriItemPenilaianSiswasId);

								if (!kategoriItemPenilaianSiswasId.isEmpty()) {

									final List<JenisItemPenilaianSiswa> jenisItemPenilaianSiswas = ConstantValues
											.simpleList(
													session.createCriteria(JenisItemPenilaianSiswa.class)
															.createAlias("kategoriItemPenilaianSiswa",
																	"kategoriItemPenilaianSiswa")
															.addOrder(Order.asc("kategoriItemPenilaianSiswa.kode"))
															.addOrder(Order.asc("nomorUrut"))
															.add(Restrictions.in("kategoriItemPenilaianSiswa",
																	kategoriItemPenilaianSiswasId))
															.add(Restrictions.or(Restrictions.isNull("aktif"),
																	Restrictions.eq("aktif", true))),
													JenisItemPenilaianSiswa.class);

									MyTabConfig tabSoal1 = new MyTabConfig(grupKategoriItemPenilaianSiswa.getNama());
									tabSoal1.setParent(tabs1);

									final Tabpanel tabpanel1 = new ais.ui.util.MyTabpanel();
									tabpanel1.setStyle("min-height: 250px;");
									tabpanel1.setHeight((170 + (250 * tinggi)) + "px");
									tabpanel1.setParent(tabpanels1);

									EventListener tabTabboxLagi = new EventListener() {

										private Textbox nama;
										private Combobox searchagama;
										private String textNama = "";
										private String textJk = null;
										private Integer smtPilih = null;
										private Boolean langsung = false;
										private Agama agama = null;
										private Combobox jenisKelamin = null;
										private Combobox semuaSemester;
										private MyCheckboxConfig entryLangsung;

										EventListener getThis() {
											return this;
										}

										@Override
										public void onEvent(Event arg0) throws Exception {

											if (tabpanel1.getChildren().isEmpty()) {

												Groupbox groupboxData = new Groupbox();
												groupboxData.setParent(tabpanel1);

												Toolbar toolbar = new Toolbar();
												toolbar.setParent(groupboxData);

												nama = new Textbox(textNama);
												searchagama = new Combobox();
												Common.insertComboDanSemua(searchagama, new String[] { "nama" },
														"keterangan", Agama.class, "Semua Agama",
														Restrictions.eq("aktif", true));
												Common.selectComboItem(searchagama, agama);

												jenisKelamin = new Combobox();
												MyComboitemConfig comboitem = new MyComboitemConfig();
												comboitem.setLabel("Laki-laki");
												comboitem.setValue("Laki-laki");
												jenisKelamin.appendChild(comboitem);
												comboitem = new MyComboitemConfig();
												comboitem.setLabel("Perempuan");
												comboitem.setValue("Perempuan");
												jenisKelamin.appendChild(comboitem);
												comboitem = new MyComboitemConfig();
												comboitem.setLabel("Semua Kelamin");
												comboitem.setValue(null);
												jenisKelamin.appendChild(comboitem);
												jenisKelamin.setReadonly(true);

												Common.selectComboItem(jenisKelamin, textJk);

												semuaSemester = new Combobox();
												comboitem = new MyComboitemConfig();
												comboitem.setLabel("Ganjil");
												comboitem.setValue(1);
												semuaSemester.appendChild(comboitem);
												comboitem = new MyComboitemConfig();
												comboitem.setLabel("Genap");
												comboitem.setValue(2);
												semuaSemester.appendChild(comboitem);
												comboitem = new MyComboitemConfig();
												comboitem.setLabel("Semua Semester");
												comboitem.setValue(null);
												semuaSemester.appendChild(comboitem);
												semuaSemester.setReadonly(true);

												Common.selectComboItem(semuaSemester, smtPilih);

												entryLangsung = new MyCheckboxConfig("Langsung entry nilai");
												entryLangsung.setChecked(langsung);

												if (jadwalPelajaran != null && jadwalPelajaran.getSemester() != null) {
													Common.selectComboItem(semuaSemester,
															jadwalPelajaran.getSemester());
													semuaSemester.setDisabled(true);
												}

												else if (grupKategoriItemPenilaianSiswa != null
														&& grupKategoriItemPenilaianSiswa.getKhususSemester() != null) {

													Common.selectComboItem(semuaSemester,
															grupKategoriItemPenilaianSiswa.getKhususSemester());
													semuaSemester.setDisabled(true);
												}

												else if (grupPenilaian != null
														&& grupPenilaian.getKhususSemester() != null) {

													Common.selectComboItem(semuaSemester,
															grupPenilaian.getKhususSemester());
													semuaSemester.setDisabled(true);

												}

												else {
													Common.selectComboItem(semuaSemester, smtPilih);
												}

												toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Siswa : ")));
												toolbar.appendChild(nama);
												MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Cari",
														"/img/svg/search.svg");
												button.addEventListener("onClick", new EventListener() {

													@Override
													public void onEvent(Event arg0) throws Exception {
														textNama = nama.getValue().trim();
														agama = (Agama) (searchagama.getSelectedItem() == null ? null
																: searchagama.getSelectedItem().getValue());

														textJk = (jenisKelamin.getSelectedItem() == null ? null
																: (String) jenisKelamin.getSelectedItem().getValue());

														smtPilih = (semuaSemester.getSelectedItem() == null ? null
																: (Integer) semuaSemester.getSelectedItem().getValue());
														langsung = entryLangsung.isChecked();

														Common.clear(tabpanel1);
														getThis().onEvent(arg0);
													}
												});
												button.setParent(toolbar);
												nama.setCols(10);
												nama.addEventListener("onOK", new EventListener() {

													@Override
													public void onEvent(Event arg0) throws Exception {
														textNama = nama.getValue().trim();
														agama = (Agama) (searchagama.getSelectedItem() == null ? null
																: searchagama.getSelectedItem().getValue());
														textJk = (jenisKelamin.getSelectedItem() == null ? null
																: (String) jenisKelamin.getSelectedItem().getValue());
														smtPilih = (semuaSemester.getSelectedItem() == null ? null
																: (Integer) semuaSemester.getSelectedItem().getValue());
														langsung = entryLangsung.isChecked();
														Common.clear(tabpanel1);
														getThis().onEvent(arg0);
													}
												});
												searchagama.setParent(toolbar);
												searchagama.setCols(10);
												searchagama.addEventListener("onChange", new EventListener() {

													@Override
													public void onEvent(Event arg0) throws Exception {
														textNama = nama.getValue().trim();
														agama = (Agama) (searchagama.getSelectedItem() == null ? null
																: searchagama.getSelectedItem().getValue());
														textJk = (jenisKelamin.getSelectedItem() == null ? null
																: (String) jenisKelamin.getSelectedItem().getValue());
														smtPilih = (semuaSemester.getSelectedItem() == null ? null
																: (Integer) semuaSemester.getSelectedItem().getValue());
														langsung = entryLangsung.isChecked();
														Common.clear(tabpanel1);
														getThis().onEvent(arg0);
													}
												});

												jenisKelamin.setParent(toolbar);
												jenisKelamin.setCols(10);
												jenisKelamin.addEventListener("onChange", new EventListener() {

													@Override
													public void onEvent(Event arg0) throws Exception {
														textNama = nama.getValue().trim();
														agama = (Agama) (searchagama.getSelectedItem() == null ? null
																: searchagama.getSelectedItem().getValue());
														textJk = (jenisKelamin.getSelectedItem() == null ? null
																: (String) jenisKelamin.getSelectedItem().getValue());
														smtPilih = (semuaSemester.getSelectedItem() == null ? null
																: (Integer) semuaSemester.getSelectedItem().getValue());
														langsung = entryLangsung.isChecked();
														Common.clear(tabpanel1);
														getThis().onEvent(arg0);
													}
												});

												semuaSemester.setParent(toolbar);
												semuaSemester.setCols(10);
												semuaSemester.addEventListener("onChange", new EventListener() {

													@Override
													public void onEvent(Event arg0) throws Exception {
														textNama = nama.getValue().trim();
														agama = (Agama) (searchagama.getSelectedItem() == null ? null
																: searchagama.getSelectedItem().getValue());
														textJk = (jenisKelamin.getSelectedItem() == null ? null
																: (String) jenisKelamin.getSelectedItem().getValue());
														smtPilih = (semuaSemester.getSelectedItem() == null ? null
																: (Integer) semuaSemester.getSelectedItem().getValue());
														langsung = entryLangsung.isChecked();
														Common.clear(tabpanel1);
														getThis().onEvent(arg0);
													}
												});

												Tbmuser kunci = null;
												if (jadwalPelajaran != null && jadwalPelajaran.getDikunci() != null) {
													kunci = jadwalPelajaran.getDikunci();
												}
												if (kunci == null && jadwalPelajaran != null
														&& jadwalPelajaran.getMasaJadwalPelajaran() != null
														&& jadwalPelajaran.getMasaJadwalPelajaran()
																.getDikunci() != null) {
													kunci = jadwalPelajaran.getDikunci();
												}
												if (kunci == null && kelasSiswa != null
														&& kelasSiswa.getDikunci() != null) {
													kunci = kelasSiswa.getDikunci();
												}

												if (tbmuser != null && (tbmuser.getSiswa() == null
														&& tbmuser.getCalonSiswa() == null) && kunci == null) {
													entryLangsung.setParent(toolbar);
												}
												entryLangsung.addEventListener("onClick", new EventListener() {

													@Override
													public void onEvent(Event arg0) throws Exception {
														textNama = nama.getValue().trim();
														agama = (Agama) (searchagama.getSelectedItem() == null ? null
																: searchagama.getSelectedItem().getValue());
														textJk = (jenisKelamin.getSelectedItem() == null ? null
																: (String) jenisKelamin.getSelectedItem().getValue());
														smtPilih = (semuaSemester.getSelectedItem() == null ? null
																: (Integer) semuaSemester.getSelectedItem().getValue());

														langsung = entryLangsung.isChecked();

														Common.clear(tabpanel1);

														getThis().onEvent(arg0);
													}
												});

												Rows rows = DetailPenilaianSiswaHelper.createRowsTanpaMinMax(
														jadwalPelajaran, grupPenilaian, grupKategoriItemPenilaianSiswa,
														kelasSiswa, matapelajaran, tbmuser, new EventListener() {

															@Override
															public void onEvent(Event arg0) throws Exception {
																Common.clear(tabpanel1);
																getThis().onEvent(arg0);
															}
														},

														groupboxData, hanyaValid, jenisItemPenilaianSiswas, siswas,
														nama, searchagama, jenisKelamin, smtPilih, langsung);

												for (final VoKelasPunyaSiswa kelasSiswaPunyaSiswa : siswas) {

													String jk = kelasSiswaPunyaSiswa.getSiswa() == null ? ""
															: kelasSiswaPunyaSiswa.getSiswa().getJenisKelamin();

													Agama agm = kelasSiswaPunyaSiswa.getSiswa() == null ? null
															: kelasSiswaPunyaSiswa.getSiswa().getAgama();

													if (agama == null
															|| (agm != null && agm.getId().equals(agama.getId()))) {

														if ((textJk == null || textJk.trim().isEmpty())
																|| (textJk != null && jk.trim().equals(textJk))) {

															if (nama.getValue().trim().isEmpty() ||

																	(!nama.getValue().trim().toLowerCase().isEmpty()
																			&& !kelasSiswaPunyaSiswa.getSiswa()
																					.getNama().isEmpty()
																			&& kelasSiswaPunyaSiswa.getSiswa().getNama()
																					.toLowerCase()
																					.contains(nama.getValue().trim()
																							.toLowerCase()))

																	||

																	(!nama.getValue().trim().toLowerCase().isEmpty()
																			&& !kelasSiswaPunyaSiswa.getSiswa()
																					.getNomorInduk().isEmpty()
																			&& kelasSiswaPunyaSiswa.getSiswa()
																					.getNomorInduk().toLowerCase()
																					.contains(nama.getValue().trim()
																							.toLowerCase()))

																	||

																	(!nama.getValue().trim().toLowerCase().isEmpty()
																			&& !kelasSiswaPunyaSiswa.getSiswa()
																					.getNomorIndukNasional().isEmpty()
																			&& kelasSiswaPunyaSiswa.getSiswa()
																					.getNomorIndukNasional()
																					.toLowerCase()
																					.contains(nama.getValue().trim()
																							.toLowerCase()))

																	||

																	(!nama.getValue().trim().toLowerCase().isEmpty()
																			&& !kelasSiswaPunyaSiswa.getSiswa()
																					.getNomorIndukSantri().isEmpty()
																			&& kelasSiswaPunyaSiswa.getSiswa()
																					.getNomorIndukSantri().toLowerCase()
																					.contains(nama.getValue().trim()
																							.toLowerCase()))

															) {

																Row row = new Row();
																row.setValign("top");
																row.setValign("top");
																row.setParent(rows);

																final Siswa siswa = kelasSiswaPunyaSiswa.getSiswa();
																CommonMedia.tampilkanGambarKecil(siswa).setParent(row);
																Vbox aa;
																(aa = RevisiHelper.createNewRevisi(
																		KelasSiswaPunyaSiswa.class,
																		kelasSiswaPunyaSiswa, siswa.getNomorInduk()))
																		.setParent(row);
																new Label(siswa.getNomorIndukNasional()).setParent(aa);
																new Label(siswa.getNama()).setParent(aa);

																int[] smts = new int[] { 1, 2 };

																if (smtPilih != null) {
																	smts = new int[] { smtPilih };
																}

																if (jadwalPelajaran != null) {
																	smts = new int[] { jadwalPelajaran.getSemester() };
																}

																if (grupKategoriItemPenilaianSiswa != null
																		&& grupKategoriItemPenilaianSiswa
																				.getKhususSemester() != null) {
																	smts = new int[] { grupKategoriItemPenilaianSiswa
																			.getKhususSemester() };
																}

																if (grupPenilaian != null
																		&& grupPenilaian.getKhususSemester() != null) {
																	smts = new int[] {
																			grupPenilaian.getKhususSemester() };
																}

																for (final int smt : smts) {
																	Double total = 0.0;

																	try {
																		Date sekarang = WaktuUtil.getDate();
																		String formula = grupKategoriItemPenilaianSiswa
																				.getFormula();
																		String target = GrupPenilaianUtil
																				.ambilTarget(formula, sekarang);
																		total = kelasSiswaPunyaSiswa.retreiveTotalNilai(
																				jenisItemPenilaianSiswas, target,
																				matapelajaran, grupPenilaian,
																				grupKategoriItemPenilaianSiswa, smt,
																				hanyaValid);
																	} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/DetailPenilaianSiswaHelper.java:1915");
																	}

																	final MyLabelBoldAja labelNilai = new MyLabelBoldAja();
																	labelNilai.setValue(
																			Common.numberFormat.get().format(total));

																	NilaiHurufSekolah nilaiHurufSekolah = NilaiHurufSekolah
																			.getNilaiHurufSekolah(total,
																					siswa.getTahunMasuk(),
																					siswa.getSekolah(),
																					siswa.getYayasan(),
																					kelasSiswaPunyaSiswa
																							.ambilKelasSiswa() == null
																									? null
																									: kelasSiswaPunyaSiswa
																											.ambilKelasSiswa()
																											.getTahunAjaran(),
																					smt % 2 == 0 ? Perkuliahan.GENAP
																							: Perkuliahan.GANJIL,
																					grupPenilaian.getJenisNilaiHuruf());

																	final MyLabelBoldAja labelNilaiHuruf = new MyLabelBoldAja();
																	labelNilaiHuruf.setValue(nilaiHurufSekolah == null
																			? ""
																			: nilaiHurufSekolah.getNilaiHuruf());

																	final MyWindow myWindow = new MyWindow();
																	final MyGrid subGrid = new MyGrid();
																	final MyLabelKecil agakKecil = new MyLabelKecil();

																	Columns columns = new Columns();
																	columns.setParent(subGrid);

																	MyColumnConfig column = new MyColumnConfig(
																			"Komponen");
																	column.setParent(columns);
																	column.setWidth("30%");

																	column = new MyColumnConfig("Nilai");
																	column.setParent(columns);
																	column.setAlign("right");

																	column = new MyColumnConfig("Sesuai");
																	column.setParent(columns);
																	column.setWidth("15%");

																	if (langsung) {
																		subGrid.setParent(row);
																		subGrid.setWidth("95%");
																	}

																	else {

																		Vbox vbData = new Vbox();
																		vbData.setParent(row);

																		if (tbmuser != null && (tbmuser
																				.getSiswa() == null
																				&& tbmuser.getCalonSiswa() == null)
																				&& kunci == null) {

																			Toolbarbutton ubahNilai = new MyToolbarbuttonConfig(
																					"Ubah Nilai",
																					"/img/svg/edit-box-line.svg");

																			if (tbmuser != null
																					&& tbmuser.ambilGuru() != null
																					&& !grupKategoriItemPenilaianSiswa
																							.getNilaiBolehDinputOlehGuru()) {

																			} else {
																				ubahNilai.setParent(vbData);
																			}

																			if (tbmuser != null
																					&& tbmuser.ambilGuru() != null
																					&& !grupPenilaian
																							.getNilaiBolehDinputOlehGuru()) {

																			} else {
																				ubahNilai.setParent(vbData);
																			}

																			ubahNilai.addEventListener("onClick",
																					new EventListener() {

																						@Override
																						public void onEvent(Event arg0)
																								throws Exception {

																							if (myWindow.getChildren()
																									.isEmpty()) {
																								myWindow.setParent(
																										ExecutionsCtrl
																												.getCurrentCtrl()
																												.getCurrentPage()
																												.getFirstRoot());
																								myWindow.setHeight(
																										"95%");
																								myWindow.setWidth(
																										"500px");

																								Borderlayout borderlayout = new Borderlayout();
																								borderlayout.setParent(
																										myWindow);
																								Center center = new Center();
																								center.setParent(
																										borderlayout);
																								ais.ui.util.ZkCompat.setFlex(center, true);

																								center.appendChild(
																										subGrid);

																								South south = new South();
																								ais.ui.util.ZkCompat.setFlex(south, true);
																								south.setParent(
																										borderlayout);

																								Toolbar toolbar = new Toolbar();
																								toolbar.setParent(
																										south);
																								MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig(
																										"Tutup",
																										"/img/cancel.gif");
																								cancel.setTooltiptext(
																										"Tutup");
																								cancel.addEventListener(
																										"onClick",
																										new EventListener() {
																											@Override
																											public void onEvent(
																													Event event)
																													throws Exception {

																												Common.createDefaultTimer(
																														new EventListener() {

																															@Override
																															public void onEvent(
																																	Event arg0)
																																	throws Exception {
																																Common.clear(
																																		tabpanel1);
																																getThis()
																																		.onEvent(
																																				arg0);
																															}
																														});

																												myWindow.setVisible(
																														false);
																											}
																										});
																								cancel.setParent(
																										toolbar);
																							}
																							myWindow.setVisible(true);
																							myWindow.onModal();
																						}
																					});
																		}

																		vbData.appendChild(agakKecil);
																	}

																	final Rows subRows = new Rows();
																	subRows.setParent(subGrid);

																	Foot foot = new Foot();
																	foot.setParent(subGrid);

																	Footer footer = new Footer("Total");
																	footer.setParent(foot);

																	footer.setStyle(
																			"font-size:16px;font-weight: bolder;");

																	Footer footerTotalData = new Footer();
																	footerTotalData.setParent(foot);
																	Date sekarang = WaktuUtil.getDate();
																	String formula = grupKategoriItemPenilaianSiswa
																			.getFormula();
																	String target = GrupPenilaianUtil
																			.ambilTarget(formula, sekarang);

																	Vbox vboxD = new Vbox();
																	vboxD.setParent(footerTotalData);
																	vboxD.setAlign("right");
																	vboxD.setPack("end");
																	vboxD.setWidth("95%");
																	final MyLabelAgakKecilBold footerTotal;
																	final MyLabelKecil footerTarget;
																	vboxD.appendChild(
																			footerTotal = new MyLabelAgakKecilBold(
																					labelNilai.getValue()));
																	vboxD.appendChild(
																			footerTarget = new MyLabelKecil(target));

																	EventListener tampilDataPenilaian = new EventListener() {

																		private EventListener getThisEventLocal() {
																			return this;
																		}

																		@Override
																		public void onEvent(Event arg0)
																				throws Exception {

																			if (arg0 != null && arg0.getTarget() != null
																					&& arg0.getTarget() instanceof Checkbox) {
																				((Checkbox) arg0.getTarget()).focus();
																			}

																			Common.clear(subRows);

																			KategoriItemPenilaianSiswa kategoriItemPenilaianSiswa = new KategoriItemPenilaianSiswa();
																			kategoriItemPenilaianSiswa.setId(-1L);

																			final List<EventListener> eventListenersFormula = new ArrayList<EventListener>();

																			String valdata = "";

																			for (final JenisItemPenilaianSiswa jenisItemPenilaianSiswa : jenisItemPenilaianSiswas) {

																				try {
																					KategoriItemPenilaianSiswa kategoriItemPenilaianSiswaTemporari = (KategoriItemPenilaianSiswa) jenisItemPenilaianSiswa
																							.getKategoriItemPenilaianSiswa();
																					if (kategoriItemPenilaianSiswaTemporari != null
																							&& (kategoriItemPenilaianSiswa == null
																									|| !kategoriItemPenilaianSiswa
																											.getId()
																											.equals(kategoriItemPenilaianSiswaTemporari
																													.getId()))) {
																						kategoriItemPenilaianSiswa = kategoriItemPenilaianSiswaTemporari;
																						Group group = new ais.ui.util.MyGroupConfig(
																								kategoriItemPenilaianSiswa
																										.getNama());
																						group.setParent(subRows);

																					} else if (kategoriItemPenilaianSiswaTemporari == null
																							&& kategoriItemPenilaianSiswa != null) {
																						kategoriItemPenilaianSiswa = null;
																						Group group = new ais.ui.util.MyGroupConfig(
																								"Item Penilaian");
																						group.setParent(subRows);

																					}
																				} catch (Exception e) {
																					kategoriItemPenilaianSiswa = null;
																					Group group = new ais.ui.util.MyGroupConfig(
																							"Item Penilaian");
																					group.setParent(subRows);
																				}

																				Row subRow = new Row();
																				subRow.setValign("top");
																				subRow.setParent(subRows);

																				final Component component;
																				final MyCheckboxConfig verify = new MyCheckboxConfig();

																				// FIX "popup Ubah Nilai tampil blank": SEBELUMNYA baris ini TIDAK
																				// dibungkus try/catch -- bila data nilai satu item saja rusak/tak
																				// terduga (lihat VoKelasPunyaSiswa.retreiveDetailNilai/Verify),
																				// exception merambat ke luar SELURUH loop tampilDataPenilaian,
																				// meninggalkan myWindow yang sudah terlanjur tampil (setVisible di
																				// atas) TANPA isi sama sekali -- persis gejala popup kosong putih.
																				// Satu item bermasalah kini pakai nilai fallback & loop lanjut,
																				// bukan menggagalkan seluruh daftar item penilaian.
																				String val = "";
																				Boolean sesuaiTemp = false;
																				try {
																					val = kelasSiswaPunyaSiswa.retreiveDetailNilai(jenisItemPenilaianSiswa,
																							grupKategoriItemPenilaianSiswa, matapelajaran, smt, hanyaValid);
																					sesuaiTemp = kelasSiswaPunyaSiswa.retreiveDetailVerify(jenisItemPenilaianSiswa,
																							grupKategoriItemPenilaianSiswa, matapelajaran, smt);
																				} catch (Exception eNilaiItem) {
																					ais.common.ErrorAuditUtil.record(eNilaiItem,
																							"auto-guard(ubah-nilai-item) src/ais/action/master/sekolah/helper/DetailPenilaianSiswaHelper.java:tampilDataPenilaian");
																				}
																				final Boolean sesuai = sesuaiTemp;

																				verify.setChecked(sesuai);

																				valdata += valdata.isEmpty()
																						? jenisItemPenilaianSiswa
																								.getNama() + "=" + val
																						: ";" + jenisItemPenilaianSiswa
																								.getNama() + "=" + val;

																				Konfigurasi kunci = grupKategoriItemPenilaianSiswa == null
																						? null
																						: Common.getKonfigurasi(
																								"kunci_nilai_sekolah_"
																										+ matapelajaran
																												.getId()
																										+ "_"
																										+ grupPenilaian
																												.getId()
																										+ "_"
																										+ grupKategoriItemPenilaianSiswa
																												.getId()
																										+ "_"
																										+ kelasSiswa
																												.getId()
																										+ "_" + smt,
																								Konfigurasi.AKTIF);

																				if (tbmuser != null && (tbmuser
																						.getSiswa() != null
																						|| tbmuser
																								.getCalonSiswa() != null
																						|| (kunci != null && kunci
																								.getDikunci() != null)

																						|| (jadwalPelajaran != null
																								&& jadwalPelajaran
																										.getDikunci() != null)

																						|| (jadwalPelajaran != null
																								&& jadwalPelajaran
																										.getMasaJadwalPelajaran() != null
																								&& jadwalPelajaran
																										.getMasaJadwalPelajaran()
																										.getDikunci() != null)

																						|| (kelasSiswa != null
																								&& kelasSiswa
																										.getDikunci() != null))) {

																					subRow.appendChild(new Label(
																							jenisItemPenilaianSiswa
																									.getKode() + " - "
																									+ jenisItemPenilaianSiswa
																											.getNama()));
																					subRow.appendChild(new Label(val));

																					subRow.appendChild(sesuai
																							? new Image(
																									"/img/svg/check2.svg")
																							: new Label());

																					if (jenisItemPenilaianSiswa
																							.getHarusMenyertakanLampiran()) {

																						Hbox hbox = new Hbox();
																						hbox.setWidth("100%");
																						hbox.setStyle(
																								"border:0px;background: transparent;");

																						LampiranLain
																								.createDownloadUploadFileLain(
																										hbox,
																										siswa.getId(),
																										KelasSiswaPunyaSiswa.class
																												.getName()
																												+ "-"
																												+ kelasSiswaPunyaSiswa
																														.getId(),
																										jenisItemPenilaianSiswa
																												.getLabelInputan()
																												+ (jenisItemPenilaianSiswa
																														.getLampiranWajibDiisi()
																																? " (*)"
																																: " "),
																										false,
																										new EventListener() {

																											@Override
																											public void onEvent(
																													Event arg0)
																													throws Exception {
																												// LampiranLain
																												// lainMahasiswa
																												// =
																												// (LampiranLain)
																												// arg0.getData();

																											}
																										}, null, false,
																										false, false,
																										false, null);

																						subRow = new Row();
																						ais.ui.util.ZkCompat.setSpans(subRow, "2");
																						subRow.setParent(subRows);
																						hbox.setParent(subRow);
																					}

																				} else {

																					EventListener eventListener = null;

																					Boolean udahDiisiDanDisable = false;

																					if (jenisItemPenilaianSiswa
																							.getTipeDataInputan()
																							.equals(JenisItemPenilaianSiswa.FORMULA)) {
																						component = new Vbox();

																						EventListener formulaeventListener = new EventListener() {

																							@Override
																							public void onEvent(
																									Event arg0)
																									throws Exception {
																								Date sekarang = WaktuUtil
																										.getDate();
																								String formula = jenisItemPenilaianSiswa
																										.getFormula();
																								String target = GrupPenilaianUtil
																										.ambilTarget(
																												formula,
																												sekarang);

																								Double total = kelasSiswaPunyaSiswa
																										.retreiveTotalNilai(
																												jenisItemPenilaianSiswas,
																												target,
																												matapelajaran,
																												grupPenilaian,
																												grupKategoriItemPenilaianSiswa,
																												smt,
																												hanyaValid);

//																						System.out.println("formula -> "
//																								+ formula
//																								+ ", target -> "
//																								+ target + ", total -> "
//																								+ total);

																								Vbox vboxD = (Vbox) component;
																								Common.clear(vboxD);
																								vboxD.setAlign("right");
																								vboxD.setPack("end");
																								vboxD.setWidth("95%");
																								Vbox footerTarget1 = RevisiHelper
																										.createNewRevisi(
																												JenisItemPenilaianSiswa.class,
																												jenisItemPenilaianSiswa,
																												target,
																												"font-size:7px;");
																								Vbox footerTotal1 = RevisiHelper
																										.createNewRevisi(
																												KelasSiswaPunyaSiswa.class,
																												kelasSiswaPunyaSiswa,
																												Common.numberFormat.get()
																														.format(total),
																												"font-size:11px;font-weight: bolder;");

																								vboxD.appendChild(
																										footerTotal1);
																								vboxD.appendChild(
																										footerTarget1);

																								kelasSiswaPunyaSiswa
																										.populateDetailNilai(
																												jenisItemPenilaianSiswa,
																												matapelajaran,
																												grupKategoriItemPenilaianSiswa,
																												total + "",
																												verify.isChecked(),
																												smt);

																								formula = grupKategoriItemPenilaianSiswa
																										.getFormula();
																								target = GrupPenilaianUtil
																										.ambilTarget(
																												formula,
																												sekarang);

																								Double tot = kelasSiswaPunyaSiswa
																										.retreiveTotalNilai(
																												jenisItemPenilaianSiswas,
																												target,
																												matapelajaran,
																												grupPenilaian,
																												grupKategoriItemPenilaianSiswa,
																												smt,
																												hanyaValid);

																								labelNilai.setValue(
																										Common.numberFormat.get()
																												.format(tot));

																								kelasSiswaPunyaSiswa
																										.populateDetailNilaiTotal(
																												matapelajaran,
																												grupKategoriItemPenilaianSiswa,
																												tot,
																												verify.isChecked(),
																												smt);
																								Common.refreshUpdate(
																										kelasSiswaPunyaSiswa);

																								footerTotal.setValue(
																										labelNilai
																												.getValue());
																								footerTarget.setValue(
																										target);

																							}

																						};

																						eventListenersFormula.add(
																								formulaeventListener);
																						Common.createDefaultTimer(
																								formulaeventListener);

																					}

																					else if (jenisItemPenilaianSiswa
																							.getTipeDataInputan()
																							.equals(JenisItemPenilaianSiswa.TEXT)) {
																						component = new Textbox(val);
																						((Textbox) component)
																								.setWidth("85%");
																						((Textbox) component).setRows(
																								jenisItemPenilaianSiswa
																										.getJumlahBaris());
																						((Textbox) component)
																								.setMaxlength(
																										jenisItemPenilaianSiswa
																												.getJumlahText());
																						((Textbox) component).focus();

																						eventListener = new EventListener() {

																							@Override
																							public void onEvent(
																									Event arg0)
																									throws Exception {
																								String jumlah = ((Textbox) component)
																										.getValue()
																										.trim();
																								kelasSiswaPunyaSiswa
																										.populateDetailNilai(
																												jenisItemPenilaianSiswa,
																												matapelajaran,
																												grupKategoriItemPenilaianSiswa,
																												jumlah,
																												verify.isChecked(),
																												smt);
																								Common.refreshUpdate(
																										kelasSiswaPunyaSiswa);

																								Date sekarang = WaktuUtil
																										.getDate();
																								String formula = grupKategoriItemPenilaianSiswa
																										.getFormula();
																								String target = GrupPenilaianUtil
																										.ambilTarget(
																												formula,
																												sekarang);
																								labelNilai.setValue(
																										Common.numberFormat.get()
																												.format(kelasSiswaPunyaSiswa
																														.retreiveTotalNilai(
																																jenisItemPenilaianSiswas,
																																target,
																																matapelajaran,
																																grupPenilaian,
																																grupKategoriItemPenilaianSiswa,
																																smt,
																																hanyaValid)));

																								footerTotal.setValue(
																										labelNilai
																												.getValue());
																								footerTarget.setValue(
																										target);

																								String valdata = "";
																								for (JenisItemPenilaianSiswa f : jenisItemPenilaianSiswas) {

																									String val = kelasSiswaPunyaSiswa
																											.retreiveDetailNilai(
																													f,
																													grupKategoriItemPenilaianSiswa,
																													matapelajaran,
																													smt,
																													hanyaValid);

																									valdata += valdata
																											.isEmpty()
																													? jenisItemPenilaianSiswa
																															.getNama()
																															+ "="
																															+ val
																													: ";" + jenisItemPenilaianSiswa
																															.getNama()
																															+ "="
																															+ val;
																								}
																								agakKecil.setValue(
																										valdata);

																								Common.createDefaultTimerNoBusy(
																										new EventListener() {

																											@Override
																											public void onEvent(
																													Event arg0)
																													throws Exception {
																												for (EventListener e : eventListenersFormula) {
																													e.onEvent(
																															arg0);
																												}
																											}
																										});

																							}
																						};

																					} else if (jenisItemPenilaianSiswa
																							.getTipeDataInputan()
																							.equals(JenisItemPenilaianSiswa.TANGGAL)) {
																						Date nilai = null;
																						try {
																							nilai = val.trim().isEmpty()
																									? null
																									: Common.dateFormat1.get()
																											.parse(val);
																						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/DetailPenilaianSiswaHelper.java:2532");

																						}
																						component = new MyDatebox(
																								nilai);
																						((MyDatebox) component)
																								.setWidth("85%");
																						((MyDatebox) component).focus();

																						eventListener = new EventListener() {

																							@Override
																							public void onEvent(
																									Event arg0)
																									throws Exception {
																								Date jumlah = ((MyDatebox) component)
																										.getValue();
																								kelasSiswaPunyaSiswa
																										.populateDetailNilai(
																												jenisItemPenilaianSiswa,
																												matapelajaran,
																												grupKategoriItemPenilaianSiswa,
																												jumlah == null
																														? ""
																														: Common.dateFormat1.get()
																																.format(jumlah),
																												verify.isChecked(),
																												smt);
																								Common.refreshUpdate(
																										kelasSiswaPunyaSiswa);

																								Date sekarang = WaktuUtil
																										.getDate();
																								String formula = grupKategoriItemPenilaianSiswa
																										.getFormula();
																								String target = GrupPenilaianUtil
																										.ambilTarget(
																												formula,
																												sekarang);

																								labelNilai.setValue(
																										Common.numberFormat.get()
																												.format(kelasSiswaPunyaSiswa
																														.retreiveTotalNilai(
																																jenisItemPenilaianSiswas,
																																target,
																																matapelajaran,
																																grupPenilaian,
																																grupKategoriItemPenilaianSiswa,
																																smt,
																																hanyaValid)));

																								footerTotal.setValue(
																										labelNilai
																												.getValue());
																								footerTarget.setValue(
																										target);

																								String valdata = "";
																								for (JenisItemPenilaianSiswa f : jenisItemPenilaianSiswas) {

																									String val = kelasSiswaPunyaSiswa
																											.retreiveDetailNilai(
																													f,
																													grupKategoriItemPenilaianSiswa,
																													matapelajaran,
																													smt,
																													hanyaValid);

																									valdata += valdata
																											.isEmpty()
																													? jenisItemPenilaianSiswa
																															.getNama()
																															+ "="
																															+ val
																													: ";" + jenisItemPenilaianSiswa
																															.getNama()
																															+ "="
																															+ val;
																								}
																								agakKecil.setValue(
																										valdata);

																								Common.createDefaultTimerNoBusy(
																										new EventListener() {

																											@Override
																											public void onEvent(
																													Event arg0)
																													throws Exception {
																												for (EventListener e : eventListenersFormula) {
																													e.onEvent(
																															arg0);
																												}
																											}
																										});

																							}
																						};

																					} else if (jenisItemPenilaianSiswa
																							.getTipeDataInputan()
																							.equals(JenisItemPenilaianSiswa.ANGKA)) {

																						Double nilai = null;
																						try {
																							nilai = val.trim().isEmpty()
																									? 0.0
																									: Double.parseDouble(
																											val);
																						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/DetailPenilaianSiswaHelper.java:2642");

																						}
																						component = new MyDoublebox(
																								nilai);
																						((MyDoublebox) component)
																								.setWidth("85%");

																						final Double nilailama = nilai;

																						eventListener = new EventListener() {

																							@Override
																							public void onEvent(
																									Event arg0)
																									throws Exception {

																								Double valData = ((MyDoublebox) arg0
																										.getTarget())
																										.getValue();

																								if (valData != null
																										&& jenisItemPenilaianSiswa
																												.getNilaiMin() > valData) {
																									MyMessageboxConfig.showFormat(
																											"Nilai yang dimasukkan tidak boleh lebih kecil dari {V1}. Langkah yang dapat dilakukan: (1) periksa kembali nilai yang Anda masukkan; (2) masukkan nilai yang tidak kurang dari batas minimum yang ditentukan; (3) simpan kembali data penilaian.",
																											"Peringatan",
																											MyMessageboxConfig.OK,
																											MyMessageboxConfig.INFORMATION,
																											Common.numberFormat.get()
																													.format(jenisItemPenilaianSiswa
																															.getNilaiMin()));

																									((MyDoublebox) arg0
																											.getTarget())
																											.setValue(
																													nilailama == null
																															? jenisItemPenilaianSiswa
																																	.getNilaiMin()
																															: nilailama);
																									return;
																								} else if (valData != null
																										&& jenisItemPenilaianSiswa
																												.getNilaiMax() < valData) {
																									MyMessageboxConfig.showFormat(
																											"Nilai yang dimasukkan tidak boleh lebih besar dari {V1}. Langkah yang dapat dilakukan: (1) periksa kembali nilai yang Anda masukkan; (2) masukkan nilai yang tidak melebihi batas maksimum yang ditentukan; (3) simpan kembali data penilaian.",
																											"Peringatan",
																											MyMessageboxConfig.OK,
																											MyMessageboxConfig.INFORMATION,
																											Common.numberFormat.get()
																													.format(jenisItemPenilaianSiswa
																															.getNilaiMax()));

																									((MyDoublebox) arg0
																											.getTarget())
																											.setValue(
																													nilailama == null
																															? jenisItemPenilaianSiswa
																																	.getNilaiMax()
																															: nilailama);
																									return;
																								}

																								Double jumlah = ((MyDoublebox) component)
																										.getValue();

																								kelasSiswaPunyaSiswa
																										.populateDetailNilai(
																												jenisItemPenilaianSiswa,
																												matapelajaran,
																												grupKategoriItemPenilaianSiswa,
																												jumlah == null
																														? ""
																														: jumlah.toString(),
																												verify.isChecked(),
																												smt);

																								Date sekarang = WaktuUtil
																										.getDate();
																								String formula = grupKategoriItemPenilaianSiswa
																										.getFormula();
																								String target = GrupPenilaianUtil
																										.ambilTarget(
																												formula,
																												sekarang);

																								Double total = kelasSiswaPunyaSiswa
																										.retreiveTotalNilai(
																												jenisItemPenilaianSiswas,
																												target,
																												matapelajaran,
																												grupPenilaian,
																												grupKategoriItemPenilaianSiswa,
																												smt,
																												hanyaValid);

																								kelasSiswaPunyaSiswa
																										.populateDetailNilaiTotal(
																												matapelajaran,
																												grupKategoriItemPenilaianSiswa,
																												total,
																												verify.isChecked(),
																												smt);
																								Common.refreshUpdate(
																										kelasSiswaPunyaSiswa);

																								NilaiHurufSekolah nilaiHurufSekolah = NilaiHurufSekolah
																										.getNilaiHurufSekolah(
																												total,
																												siswa.getTahunMasuk(),
																												siswa.getSekolah(),
																												siswa.getYayasan(),
																												kelasSiswaPunyaSiswa
																														.ambilKelasSiswa() == null
																																? null
																																: kelasSiswaPunyaSiswa
																																		.ambilKelasSiswa()
																																		.getTahunAjaran(),
																												smt % 2 == 0
																														? Perkuliahan.GENAP
																														: Perkuliahan.GANJIL,
																												grupPenilaian
																														.getJenisNilaiHuruf());
																								labelNilaiHuruf
																										.setValue(
																												nilaiHurufSekolah == null
																														? ""
																														: nilaiHurufSekolah
																																.getNilaiHuruf());

																								labelNilai.setValue(
																										Common.numberFormat.get()
																												.format(total));
																								footerTotal.setValue(
																										labelNilai
																												.getValue());
																								footerTarget.setValue(
																										target);

																								String valdata = "";
																								for (JenisItemPenilaianSiswa f : jenisItemPenilaianSiswas) {

																									String val = kelasSiswaPunyaSiswa
																											.retreiveDetailNilai(
																													f,
																													grupKategoriItemPenilaianSiswa,
																													matapelajaran,
																													smt,
																													hanyaValid);

																									valdata += valdata
																											.isEmpty()
																													? jenisItemPenilaianSiswa
																															.getNama()
																															+ "="
																															+ val
																													: ";" + jenisItemPenilaianSiswa
																															.getNama()
																															+ "="
																															+ val;
																								}
																								agakKecil.setValue(
																										valdata);

																								Common.createDefaultTimerNoBusy(
																										new EventListener() {

																											@Override
																											public void onEvent(
																													Event arg0)
																													throws Exception {
																												for (EventListener e : eventListenersFormula) {
																													e.onEvent(
																															arg0);
																												}
																											}
																										});

																							}
																						};
																					} else if (jenisItemPenilaianSiswa
																							.getTipeDataInputan()
																							.equals(JenisItemPenilaianSiswa.TEXT_ANGKA)) {

																						component = new MyTextboxAngka(
																								val == null
																										|| val.isEmpty()
																												? "0"
																												: val);
																						((Textbox) component)
																								.setWidth("85%");
																						((Textbox) component).focus();
																						eventListener = new EventListener() {

																							@Override
																							public void onEvent(
																									Event arg0)
																									throws Exception {
																								String jumlah = ((Textbox) component)
																										.getValue()
																										.trim();
																								kelasSiswaPunyaSiswa
																										.populateDetailNilai(
																												jenisItemPenilaianSiswa,
																												matapelajaran,
																												grupKategoriItemPenilaianSiswa,
																												jumlah,
																												verify.isChecked(),
																												smt);

																								Date sekarang = WaktuUtil
																										.getDate();
																								String formula = grupKategoriItemPenilaianSiswa
																										.getFormula();
																								String target = GrupPenilaianUtil
																										.ambilTarget(
																												formula,
																												sekarang);

																								Double total = kelasSiswaPunyaSiswa
																										.retreiveTotalNilai(
																												jenisItemPenilaianSiswas,
																												target,
																												matapelajaran,
																												grupPenilaian,
																												grupKategoriItemPenilaianSiswa,
																												smt,
																												hanyaValid);

																								kelasSiswaPunyaSiswa
																										.populateDetailNilaiTotal(
																												matapelajaran,
																												grupKategoriItemPenilaianSiswa,
																												total,
																												verify.isChecked(),
																												smt);
																								Common.refreshUpdate(
																										kelasSiswaPunyaSiswa);

																								labelNilai.setValue(
																										Common.numberFormat.get()
																												.format(total));
																								footerTotal.setValue(
																										labelNilai
																												.getValue());
																								footerTarget.setValue(
																										target);

																								String valdata = "";
																								for (JenisItemPenilaianSiswa f : jenisItemPenilaianSiswas) {

																									String val = kelasSiswaPunyaSiswa
																											.retreiveDetailNilai(
																													f,
																													grupKategoriItemPenilaianSiswa,
																													matapelajaran,
																													smt,
																													hanyaValid);

																									valdata += valdata
																											.isEmpty()
																													? jenisItemPenilaianSiswa
																															.getNama()
																															+ "="
																															+ val
																													: ";" + jenisItemPenilaianSiswa
																															.getNama()
																															+ "="
																															+ val;
																								}
																								agakKecil.setValue(
																										valdata);

																								Common.createDefaultTimerNoBusy(
																										new EventListener() {

																											@Override
																											public void onEvent(
																													Event arg0)
																													throws Exception {
																												for (EventListener e : eventListenersFormula) {
																													e.onEvent(
																															arg0);
																												}
																											}
																										});
																							}

																						};
																					} else if (jenisItemPenilaianSiswa
																							.getTipeDataInputan()
																							.equals(JenisItemPenilaianSiswa.PILIHAN_YA_TIDAK)) {
																						Boolean nilai = null;
																						try {
																							nilai = val.trim().isEmpty()
																									? null
																									: Boolean
																											.parseBoolean(
																													val);
																						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/DetailPenilaianSiswaHelper.java:2941");

																						}

																						component = new Combobox();
																						MyComboitemConfig comboitem = new MyComboitemConfig(
																								"Ya");
																						comboitem.setValue(true);
																						component
																								.appendChild(comboitem);
																						comboitem = new MyComboitemConfig(
																								"Tidak");
																						comboitem.setValue(false);
																						component
																								.appendChild(comboitem);

																						((Combobox) component)
																								.setReadonly(true);
																						Common.selectComboItem(
																								((Combobox) component),
																								nilai);
																						((Combobox) component)
																								.setWidth("85%");

																						eventListener = new EventListener() {

																							@Override
																							public void onEvent(
																									Event arg0)
																									throws Exception {
																								Object jumlah = ((Combobox) component)
																										.getSelectedItem() == null
																												? ""
																												: ((Combobox) component)
																														.getSelectedItem()
																														.getValue();
																								kelasSiswaPunyaSiswa
																										.populateDetailNilai(
																												jenisItemPenilaianSiswa,
																												matapelajaran,
																												grupKategoriItemPenilaianSiswa,
																												jumlah == null
																														? ""
																														: jumlah.toString(),
																												verify.isChecked(),
																												smt);
																								Common.refreshUpdate(
																										kelasSiswaPunyaSiswa);

																								Date sekarang = WaktuUtil
																										.getDate();
																								String formula = grupKategoriItemPenilaianSiswa
																										.getFormula();
																								String target = GrupPenilaianUtil
																										.ambilTarget(
																												formula,
																												sekarang);

																								labelNilai.setValue(
																										Common.numberFormat.get()
																												.format(kelasSiswaPunyaSiswa
																														.retreiveTotalNilai(
																																jenisItemPenilaianSiswas,
																																target,
																																matapelajaran,
																																grupPenilaian,
																																grupKategoriItemPenilaianSiswa,
																																smt,
																																hanyaValid)));
																								footerTotal.setValue(
																										labelNilai
																												.getValue());
																								footerTarget.setValue(
																										target);

																								String valdata = "";
																								for (JenisItemPenilaianSiswa f : jenisItemPenilaianSiswas) {

																									String val = kelasSiswaPunyaSiswa
																											.retreiveDetailNilai(
																													f,
																													grupKategoriItemPenilaianSiswa,
																													matapelajaran,
																													smt,
																													hanyaValid);

																									valdata += valdata
																											.isEmpty()
																													? jenisItemPenilaianSiswa
																															.getNama()
																															+ "="
																															+ val
																													: ";" + jenisItemPenilaianSiswa
																															.getNama()
																															+ "="
																															+ val;
																								}
																								agakKecil.setValue(
																										valdata);

																								Common.createDefaultTimerNoBusy(
																										new EventListener() {

																											@Override
																											public void onEvent(
																													Event arg0)
																													throws Exception {
																												for (EventListener e : eventListenersFormula) {
																													e.onEvent(
																															arg0);
																												}
																											}
																										});
																							}

																						};
																					} else if (jenisItemPenilaianSiswa
																							.getTipeDataInputan()
																							.equals(JenisItemPenilaianSiswa.PILIHAN_CUSTOM)) {
																						component = new Combobox();
																						String[] ss = StringUtils.split(
																								jenisItemPenilaianSiswa
																										.getNilaiDataInputan(),
																								";");
																						Arrays.sort(ss);
																						for (String s : ss) {

																							String[] kol = StringUtils
																									.split(s, ":");
																							String a = kol[0];
																							Integer skor = 0;
																							try {
																								skor = Integer.parseInt(
																										kol[1].trim());
																							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/DetailPenilaianSiswaHelper.java:3075");

																							}
																							MyComboitemConfig comboitem = new MyComboitemConfig(
																									a);
																							comboitem.setValue(s);
																							comboitem.setAttribute(
																									"skor", skor);
																							component.appendChild(
																									comboitem);
																						}

																						((Combobox) component)
																								.setReadonly(true);
																						Common.selectComboItem(
																								((Combobox) component),
																								val);
																						((Combobox) component)
																								.setWidth("85%");

																						eventListener = new EventListener() {

																							@Override
																							public void onEvent(
																									Event arg0)
																									throws Exception {
																								Object jumlah = ((Combobox) component)
																										.getSelectedItem() == null
																												? ""
																												: ((Combobox) component)
																														.getSelectedItem()
																														.getValue();
																								kelasSiswaPunyaSiswa
																										.populateDetailNilai(
																												jenisItemPenilaianSiswa,
																												matapelajaran,
																												grupKategoriItemPenilaianSiswa,
																												jumlah == null
																														? ""
																														: jumlah.toString(),
																												verify.isChecked(),
																												smt);
																								Common.refreshUpdate(
																										kelasSiswaPunyaSiswa);

																								Date sekarang = WaktuUtil
																										.getDate();
																								String formula = grupKategoriItemPenilaianSiswa
																										.getFormula();
																								String target = GrupPenilaianUtil
																										.ambilTarget(
																												formula,
																												sekarang);

																								labelNilai.setValue(
																										Common.numberFormat.get()
																												.format(kelasSiswaPunyaSiswa
																														.retreiveTotalNilai(
																																jenisItemPenilaianSiswas,
																																target,
																																matapelajaran,
																																grupPenilaian,
																																grupKategoriItemPenilaianSiswa,
																																smt,
																																hanyaValid)));

																								footerTotal.setValue(
																										labelNilai
																												.getValue());
																								footerTarget.setValue(
																										target);

																								String valdata = "";
																								for (JenisItemPenilaianSiswa f : jenisItemPenilaianSiswas) {

																									String val = kelasSiswaPunyaSiswa
																											.retreiveDetailNilai(
																													f,
																													grupKategoriItemPenilaianSiswa,
																													matapelajaran,
																													smt,
																													hanyaValid);

																									valdata += valdata
																											.isEmpty()
																													? jenisItemPenilaianSiswa
																															.getNama()
																															+ "="
																															+ val
																													: ";" + jenisItemPenilaianSiswa
																															.getNama()
																															+ "="
																															+ val;
																								}
																								agakKecil.setValue(
																										valdata);

																								Common.createDefaultTimerNoBusy(
																										new EventListener() {

																											@Override
																											public void onEvent(
																													Event arg0)
																													throws Exception {
																												for (EventListener e : eventListenersFormula) {
																													e.onEvent(
																															arg0);
																												}
																											}
																										});

																							}
																						};

																					} else if (jenisItemPenilaianSiswa
																							.getTipeDataInputan()
																							.equals(ParameterTambahan.PILIHAN_BANYAK)) {
																						component = new Vbox();
																						String[] ss = StringUtils.split(
																								jenisItemPenilaianSiswa
																										.getNilaiDataInputan(),
																								";");
																						Arrays.sort(ss);
																						for (String s : ss) {
																							MyCheckboxConfig comboitem = new MyCheckboxConfig(
																									s);
																							comboitem.setValue(s);
																							component.appendChild(
																									comboitem);

																							for (String g : val
																									.split(";")) {
																								if (g.trim()
																										.equalsIgnoreCase(
																												s.trim())) {
																									comboitem
																											.setChecked(
																													true);
																								}
																							}

																						}

																						eventListener = new EventListener() {

																							@Override
																							public void onEvent(
																									Event arg0)
																									throws Exception {
																								String jumlah = "";

																								List<MyCheckboxConfig> checkboxConfigs = component
																										.getChildren();
																								for (MyCheckboxConfig c : checkboxConfigs) {
																									if (c.isChecked()) {
																										jumlah += jumlah
																												.isEmpty()
																														? c.getValue()
																														: ";" + c
																																.getValue();
																									}
																								}

																								kelasSiswaPunyaSiswa
																										.populateDetailNilai(
																												jenisItemPenilaianSiswa,
																												matapelajaran,
																												grupKategoriItemPenilaianSiswa,
																												jumlah,
																												verify.isChecked(),
																												smt);
																								Common.refreshUpdate(
																										kelasSiswaPunyaSiswa);

																								Date sekarang = WaktuUtil
																										.getDate();
																								String formula = grupKategoriItemPenilaianSiswa
																										.getFormula();
																								String target = GrupPenilaianUtil
																										.ambilTarget(
																												formula,
																												sekarang);

																								labelNilai.setValue(
																										Common.numberFormat.get()
																												.format(kelasSiswaPunyaSiswa
																														.retreiveTotalNilai(
																																jenisItemPenilaianSiswas,
																																target,
																																matapelajaran,
																																grupPenilaian,
																																grupKategoriItemPenilaianSiswa,
																																smt,
																																hanyaValid)));

																								footerTotal.setValue(
																										labelNilai
																												.getValue());
																								footerTarget.setValue(
																										target);

																								String valdata = "";
																								for (JenisItemPenilaianSiswa f : jenisItemPenilaianSiswas) {

																									String val = kelasSiswaPunyaSiswa
																											.retreiveDetailNilai(
																													f,
																													grupKategoriItemPenilaianSiswa,
																													matapelajaran,
																													smt,
																													hanyaValid);

																									valdata += valdata
																											.isEmpty()
																													? jenisItemPenilaianSiswa
																															.getNama()
																															+ "="
																															+ val
																													: ";" + jenisItemPenilaianSiswa
																															.getNama()
																															+ "="
																															+ val;
																								}
																								agakKecil.setValue(
																										valdata);

																								Common.createDefaultTimerNoBusy(
																										new EventListener() {

																											@Override
																											public void onEvent(
																													Event arg0)
																													throws Exception {
																												for (EventListener e : eventListenersFormula) {
																													e.onEvent(
																															arg0);
																												}
																											}
																										});

																							}
																						};

																					} else {
																						component = null;
																					}

																					if (component != null) {

																						if (eventListener != null) {
																							component.addEventListener(
																									"onChange",
																									eventListener);

																							final EventListener ev = eventListener;

																							verify.addEventListener(
																									"onCheck",
																									new EventListener() {

																										@Override
																										public void onEvent(
																												Event arg0)
																												throws Exception {

																											ev.onEvent(
																													new Event(
																															"",
																															component));

																											getThisEventLocal()
																													.onEvent(

																															new Event(
																																	"",
																																	verify));
																										}
																									});
																						}

																						subRow.appendChild(new Label(
																								jenisItemPenilaianSiswa
																										.getKode()
																										+ " - "
																										+ jenisItemPenilaianSiswa
																												.getNama()));

																						if (sesuai
																								&& eventListener != null) {
																							subRow.appendChild(
																									new Label(val));
																						} else {
																							subRow.appendChild(
																									component);
																						}

																						if (component instanceof Label
																								|| component instanceof Vbox) {
																							subRow.appendChild(
																									new Label());
																						}

																						else if (tbmuser
																								.ambilGuru() != null) {

																							if (kelasSiswa
																									.getGuruBolehMemverifikasiSendiri()) {
																								subRow.appendChild(
																										verify);
																							} else {
																								subRow.appendChild(
																										sesuai ? new Image(
																												"/img/svg/check2.svg")
																												: new Label());
																							}

																						} else if (tbmuser != null
																								&& (tbmuser
																										.getSiswa() != null
																										|| tbmuser
																												.getCalonSiswa() != null)) {
																							subRow.appendChild(sesuai
																									? new Image(
																											"/img/svg/check2.svg")
																									: new Label());
																						} else {
																							subRow.appendChild(verify);
																						}

																						if (jenisItemPenilaianSiswa
																								.getHarusMenyertakanLampiran()) {
																							if (!udahDiisiDanDisable) {

																								Hbox hbox = new Hbox();
																								hbox.setWidth("100%");
																								hbox.setStyle(
																										"border:0px;background: transparent;");

																								LampiranLain
																										.createDownloadUploadFileLain(
																												hbox,
																												siswa.getId(),
																												KelasSiswaPunyaSiswa.class
																														.getName()
																														+ "-"
																														+ kelasSiswaPunyaSiswa
																																.getId(),
																												jenisItemPenilaianSiswa
																														.getLabelInputan()
																														+ (jenisItemPenilaianSiswa
																																.getLampiranWajibDiisi()
																																		? " (*)"
																																		: " "),
																												false,
																												new EventListener() {

																													@Override
																													public void onEvent(
																															Event arg0)
																															throws Exception {
																														// LampiranLain
																														// lainMahasiswa
																														// =
																														// (LampiranLain)
																														// arg0.getData();

																													}
																												});

																								subRow = new Row();
																								ais.ui.util.ZkCompat.setSpans(subRow, "2");
																								subRow.setParent(
																										subRows);
																								hbox.setParent(subRow);
																							}
																						}
																					}
																					if (jenisItemPenilaianSiswa
																							.getHanyaTampilDiAdmin()) {
																						Common.freeze(subRows,
																								!Common.getApakahAdmin(
																										jenisItemPenilaianSiswa
																												.getKodeAdminYgBoleh()));
																					}
																				}

																			}
																			agakKecil.setValue(valdata);
																		}
																	};

																	tampilDataPenilaian.onEvent(arg0);

																	labelNilai.setParent(row);
																	labelNilaiHuruf.setParent(row);

																}
															}
														}
													}
												}
											}
										}

									};

									if (indexTab == 0) {
										tabTabboxLagi.onEvent(null);
									} else {
										tabSoal1.addEventListener("onClick", tabTabboxLagi);
									}
									indexTab++;
								}
							}
						}
					}
				}
			};

			if (indexTab == 0) {
				tabTabbox.onEvent(null);
			} else {
				tabSoal.addEventListener("onClick", tabTabbox);
			}

			indexTab++;
		}
	}

	public void displayDetailPA(KelasSiswa kelasSiswa, final Component component, final MyWindow window) {

		this.kelasSiswa = kelasSiswa;
		Common.clear(component);

		if (kelasSiswa.getKurikulumSekolah() == null) {
			new MyLabelBolder("Kurikulum belum di setting").setParent(component);
			return;
		}

		ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
		groupbox.setStyle("min-height: 200px;");
		groupbox.setParent(component);

		MyGrid grid = new MyGrid();
		grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		grid.setMold("paging");
		grid.setParent(groupbox);

		rowsData = new Rows();
		rowsData.setParent(grid);

		loadData(null);
	}

	public static void uploadDataNilai(final File file, final EventListener eventListener, final String[] contents,
			final int[] smts, final List<JenisItemPenilaianSiswa> jenisItemPenilaianSiswas,
			final List<? extends VoKelasPunyaSiswa> siswas, final Matapelajaran matapelajaran,
			final GrupKategoriItemPenilaianSiswa grupKategoriItemPenilaianSiswa, final GrupPenilaian grupPenilaian)
			throws Exception {

		final Label peringatan = new Label("");
		final Label label = new Label(ais.common.Common.getBahasaConfig("Proses upload data nilai .."));
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
					MyMessageboxConfig.show(
							"Upload data nilai berhasil dilakukan."
									+ (peringatan.getValue().isEmpty() ? "" : "\n" + peringatan.getValue()),
							"Pemberitahuan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, eventListener);
					Clients.clearBusy();
					timer.detach();
				}
			}
		});
		timer.start();

		new Thread(new Runnable() {
			@Override
			public void run() {
				Session session = null;
				XSSFWorkbook workbook = null;
				try {
					if (file == null || !file.exists()) {
						peringatan.setValue("File upload tidak ditemukan.");
						return;
					}
					// Gunakan FileInputStream agar path dengan spasi tidak gagal di ZSS ZipPackage
					FileInputStream xlsFis = null;
					try {
						xlsFis = new FileInputStream(file);
						workbook = new XSSFWorkbook(xlsFis);
					} finally {
						if (xlsFis != null) { try { xlsFis.close(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/DetailPenilaianSiswaHelper.java:3576");} }
					}
					XSSFSheet sheet = workbook.getSheetAt(0);
					session = HibernateUtil.getSessionFactory().openSession();
					Boolean hanyaValid = null;
					int rowCount = (sheet.getLastRowNum() + 1);
					int sukses = 0;
					int gagal = 0;

					for (int i = 1; i < rowCount; i++) {
						@SuppressWarnings("rawtypes")
						Map datum = null;
						org.hibernate.Transaction tx = null;
						try {
							if (session == null || !session.isOpen()) {
								session = HibernateUtil.getSessionFactory().openSession();
							}

							Long id = Common.getSheetContentAsLong(sheet, 0, i);
							Siswa siswa = id == null || id.equals(-1L) ? null
									: (Siswa) session.createCriteria(Siswa.class)
											.add(Restrictions.isNotNull("namaSiswa"))
											.add(Restrictions.ne("namaSiswa", ""))
											.add(Restrictions.isNotNull("sekolah")).add(Restrictions.idEq(id))
											.uniqueResult();
							String nim = Common.getSheetContentAsString(sheet, 1, i);

							if (siswa == null) {
								siswa = nim == null || nim.equals("-----") || nim.trim().isEmpty() ? null
										: (Siswa) session.createCriteria(Siswa.class)
												.add(Restrictions.isNotNull("namaSiswa"))
												.add(Restrictions.ne("namaSiswa", ""))
												.add(Restrictions.isNotNull("sekolah"))
												.add(Restrictions.eq("nomorIndukNasional", nim))
												.addOrder(Order.desc("id")).setMaxResults(1).uniqueResult();

								if (siswa != null) {
									continue;
								}
							}

							if (siswa != null && siswa.getId() != null) {
								VoKelasPunyaSiswa kelasSiswaPunyaSiswa = null;
								for (VoKelasPunyaSiswa temp : siswas) {
									if (temp.getSiswa() != null && temp.getSiswa().getId() != null
											&& siswa.getId().equals(temp.getSiswa().getId())) {
										kelasSiswaPunyaSiswa = temp;
										break;
									}
								}
								if (kelasSiswaPunyaSiswa != null) {
									Date sekarang = WaktuUtil.getDate();
									session.refresh(kelasSiswaPunyaSiswa);
									int idex = 0;
									for (int smt : smts) {
										for (JenisItemPenilaianSiswa jenisItemPenilaianSiswa : jenisItemPenilaianSiswas) {
											String tot = "0.0";
											Boolean verif = null;

											System.out.println("jenisItemPenilaianSiswa.getTipeDataInputan() -> "
													+ jenisItemPenilaianSiswa.getTipeDataInputan());

											if (jenisItemPenilaianSiswa.getTipeDataInputan()
													.equals(JenisItemPenilaianSiswa.FORMULA)) {

												String formula = jenisItemPenilaianSiswa.getFormula();
												String target = GrupPenilaianUtil.ambilTarget(formula, sekarang);

												Double total = kelasSiswaPunyaSiswa.retreiveTotalNilai(
														jenisItemPenilaianSiswas, target, matapelajaran, grupPenilaian,
														grupKategoriItemPenilaianSiswa, smt, hanyaValid);

												tot = total + "";
												verif = true;
											} else {
												tot = Common.getSheetContentAsString(sheet, contents.length + idex, i);
												idex++;
												verif = Common.getSheetContentAsBoolean(sheet, contents.length + idex, i);
											}

											System.out.println(
													"jenisItemPenilaianSiswa -> " + jenisItemPenilaianSiswa.getNama()
															+ ", tot -> " + tot + ", verif -> " + verif);

											kelasSiswaPunyaSiswa.populateDetailNilai(jenisItemPenilaianSiswa,
													matapelajaran, grupKategoriItemPenilaianSiswa, tot, verif, smt);

											idex++;
										}

										String formula = grupKategoriItemPenilaianSiswa.getFormula();
										String target = GrupPenilaianUtil.ambilTarget(formula, sekarang);

										Double tot = kelasSiswaPunyaSiswa.retreiveTotalNilai(jenisItemPenilaianSiswas,
												target, matapelajaran, grupPenilaian, grupKategoriItemPenilaianSiswa,
												smt, hanyaValid);

										System.out.println("tot -> " + tot + " matapelajaran -> " + matapelajaran + ", "
												+ kelasSiswaPunyaSiswa + ", smt " + smt
												+ ", grupKategoriItemPenilaianSiswa " + grupKategoriItemPenilaianSiswa);

										kelasSiswaPunyaSiswa.populateDetailNilaiTotal(matapelajaran,
												grupKategoriItemPenilaianSiswa, tot, true, smt);

										idex++;
									}

									tx = session.beginTransaction();
									Common.refreshUpdate(session, kelasSiswaPunyaSiswa);
									tx.commit();
									tx = null;
									sukses++;
								}
							}
						} catch (Exception e) {
							gagal++;
							System.out.println("error upload nilai baris excel ke-" + (i + 1) + " --> datum=>" + datum);
							try {
								if (tx != null) {
									tx.rollback();
								}
							} catch (Exception ignoreRollback) { ais.common.ErrorAuditUtil.record(ignoreRollback, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/DetailPenilaianSiswaHelper.java:3697");
							}
							Common.tampilErrorJikaAdmin(e);
							if (session == null || !session.isOpen()) {
								try {
									if (session != null) {
										session.close();
									}
								} catch (Exception ignoreClose) { ais.common.ErrorAuditUtil.record(ignoreClose, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/DetailPenilaianSiswaHelper.java:3705");
								}
								session = HibernateUtil.getSessionFactory().openSession();
							}
						}
					}
					if (gagal > 0) {
						peringatan.setValue("Sebagian baris gagal diproses. Berhasil: " + sukses + ", gagal: " + gagal + ". Silakan cek log untuk detail baris yang gagal.");
					}
				} catch (Exception e1) {
					Common.tampilErrorJikaAdmin(e1);
					peringatan.setValue("Upload data nilai belum selesai karena terjadi error: " + e1.getMessage());
				} finally {
					if (session != null) {
						try {
							session.clear();
						} catch (Exception ignoreClear) { ais.common.ErrorAuditUtil.record(ignoreClear, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/DetailPenilaianSiswaHelper.java:3721");
						}
						try {
							session.disconnect();
						} catch (Exception ignoreDisconnect) { ais.common.ErrorAuditUtil.record(ignoreDisconnect, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/DetailPenilaianSiswaHelper.java:3725");
						}
						try {
							session.close();
						} catch (Exception ignoreClose) { ais.common.ErrorAuditUtil.record(ignoreClose, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/DetailPenilaianSiswaHelper.java:3729");
						}
					}
					try {
						if (workbook != null && workbook.getPackage() != null) {
							workbook.getPackage().close();
						}
					} catch (Exception ignoreWorkbook) { ais.common.ErrorAuditUtil.record(ignoreWorkbook, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/DetailPenilaianSiswaHelper.java:3736");
					}
					label.setValue("");
				}
			}
		}).start();
	}

	public static void tampilKunci(Component toolbar, final Konfigurasi konfigurasi, Tbmuser tbmuser,
			final EventListener eventListener) {
		final MyToolbarbuttonConfig bukaKunci = new MyToolbarbuttonConfig("Buka", "/img/svg/unlock.svg");
		final MyToolbarbuttonConfig kunci = new MyToolbarbuttonConfig("Kunci", "/img/Lock-Lock-icon.png");

		bukaKunci.setStyle("font-size:11px;");
		kunci.setStyle("font-size:11px;");

		if (tbmuser.getSiswa() == null && konfigurasi != null) {

			toolbar.appendChild(bukaKunci);
			toolbar.appendChild(kunci);

			kunci.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					MyMessageboxConfig.show(
							"Apakah yakin ingin mengunci nilai ini ?\n\nCatatan : Nilai akan terkunci dan tidak bisa dirubah oleh orang lain kecuali jika anda membuka kunci penilain kembali.",
							"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
							MyMessageboxConfig.QUESTION, new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {

										konfigurasi.setDikunci(Common.getCurrentUser());
										Common.refreshUpdate(konfigurasi);

										kunci.setVisible(konfigurasi.getDikunci() == null);
										bukaKunci.setVisible(konfigurasi.getDikunci() != null);
										if (konfigurasi.getDikunci() != null) {
											bukaKunci.setLabel(
													"Buka Kunci (" + konfigurasi.getDikunci().getUserNama() + ")");
										}

										Common.createDefaultTimer(eventListener);
									}

								}
							});
				}
			});

			kunci.setVisible(konfigurasi.getDikunci() == null);
			kunci.setDisabled(!Common.getApakahAdminBolehKunci());

			kunci.setParent(toolbar);
//			kunci.setOrient("vertical");

			bukaKunci.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					MyMessageboxConfig.show(
							"Apakah yakin ingin membuka kunci nilai ini ?\n\nCatatan : Nilai akan terbuka dan bisa dirubah oleh orang lain yang berhak mengakses penilaian anda (misalnya: admin).",
							"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
							MyMessageboxConfig.QUESTION, new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {

										konfigurasi.setDikunci(null);
										Common.refreshUpdate(konfigurasi);

										kunci.setVisible(konfigurasi.getDikunci() == null);
										bukaKunci.setVisible(konfigurasi.getDikunci() != null);

										Common.createDefaultTimer(eventListener);
									}

								}
							});
				}
			});
			bukaKunci.setVisible(konfigurasi.getDikunci() != null);
			if (konfigurasi.getDikunci() != null) {
				bukaKunci.setLabel("Buka Kunci (" + konfigurasi.getDikunci().getUserNama() + ")");
			}
			bukaKunci.setDisabled((konfigurasi.getDikunci() != null && Common.getCurrentUser().getUserId() != null
					&& !konfigurasi.getDikunci().getUserId().equals(Common.getCurrentUser().getUserId()))

					|| !Common.getApakahAdminBolehKunci());

			bukaKunci.setParent(toolbar);
//			bukaKunci.setOrient("vertical");

			Konfigurasi konfigurasiKunci = Common.getKonfigurasi("kunci_nilai_untuk_admin", Konfigurasi.TIDAK_AKTIF);

			if (konfigurasiKunci.getNilai().equals(Konfigurasi.AKTIF)) {
				if (Common.getCurrentUser().getRoot() != null && Common.getCurrentUser().getRoot()
						&& Common.getCurrentUser() != null && Common.getCurrentUser().hakAkses() != null
						&& Common.getCurrentUser().hakAkses() != null && Common.getCurrentUser() != null
						&& Common.getCurrentUser().hakAkses() != null
						&& Common.getCurrentUser().hakAkses().getRoleId().equals(Tbmrole.ADMINISTRATOR)) {
					bukaKunci.setDisabled(false);
				}
			}

		}
	}

	private void loadData(Object object) {

		Tbmuser tbmuser = Common.getCurrentUser();
		Guru gur = tbmuser == null ? null : tbmuser.ambilGuru();
		List<Long> mt;
		if (gur != null) {
			mt = JadwalUtil.ambilJadwal(gur, kelasSiswa);
		} else {
			mt = new ArrayList<Long>();
		}

		List<Long> mk = kelasSiswa.ambilMk();

		Session session = HibernateUtil.currentSession();
		List<JenisPenilaian> jenisPenilaians = ConstantValues.simpleList(session
				.createCriteria(KurikulumPunyaMatapelajaran.class).createAlias("matapelajaran", "matapelajaran")

				.add(mk == null || mk.isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.not(Restrictions.in("matapelajaran.id", mk)))

				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(mt.isEmpty()
						? (gur == null ? Restrictions.sqlRestriction("true") : Restrictions.sqlRestriction("false"))
						: Restrictions.in("matapelajaran.id", mt))

				.add(Restrictions.eq("kurikulumSekolah", kelasSiswa.getKurikulumSekolah()))
				.setProjection(Projections.groupProperty("matapelajaran.jenisPenilaian.id"))
				.addOrder(Order.asc("matapelajaran.jenisPenilaian")), JenisPenilaian.class, false);

		siswas = ConstantValues.simpleList(initCriteria(true), KelasSiswaPunyaSiswa.class);

		System.out.println("jenisPenilaians -> " + jenisPenilaians + ", siswas -> " + siswas);
		Common.clear(rowsData);
		for (JenisPenilaian jenisPenilaian : jenisPenilaians) {

			Row rowData = new Row();
			rowData.setValign("top");
			rowData.setParent(rowsData);

			new MyLabelBolder(jenisPenilaian.getNama()).setParent(rowData);

			rowData = new Row();
			rowData.setParent(rowsData);

			MyGrid grid = new MyGrid();
			grid.setOddRowSclass("non-odd");
			grid.setWidth("100%");
			grid.setMold("paging");
			grid.setPageSize(50);
			grid.getPagingChild().setMold("os");
			grid.setParent(rowData);

			Columns columns = new Columns();

			columns.setParent(grid);

			MyColumnConfig column = new MyColumnConfig();
			column.setParent(columns);
			column.setWidth("40px");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("No.");
			column.setWidth("3%");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Mata Pelajaran");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Jenis Penilaian");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Guru");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("keterangan");
			column.setWidth("15%");

			List<KurikulumPunyaMatapelajaran> jenisItemPenilaianSiswa = ConstantValues.simpleList(
					session.createCriteria(KurikulumPunyaMatapelajaran.class)
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
							.add(Restrictions.eq("kurikulumSekolah", kelasSiswa.getKurikulumSekolah()))
							.createAlias("matapelajaran", "matapelajaran")

							.add(mk == null || mk.isEmpty() ? Restrictions.sqlRestriction("true")
									: Restrictions.not(Restrictions.in("matapelajaran.id", mk)))

							.createAlias("matapelajaran.kelompokMatapelajaran", "kelompokMatapelajaran")

							.addOrder(Order.asc("kelompokMatapelajaran.nomorUrut"))
							.addOrder(Order.asc("matapelajaran.urutan"))

							.add(mt.isEmpty()
									? (gur == null ? Restrictions.sqlRestriction("true")
											: Restrictions.sqlRestriction("false"))
									: Restrictions.in("matapelajaran.id", mt))

							.add(Restrictions.eq("matapelajaran.jenisPenilaian", jenisPenilaian)),
					KurikulumPunyaMatapelajaran.class);

			System.out.println("jenisItemPenilaianSiswa -> " + jenisItemPenilaianSiswa);

			Rows rows = new Rows();
			rows.setParent(grid);
			int nomor = 1;
			for (final KurikulumPunyaMatapelajaran kurikulumPunyaMatapelajaran : jenisItemPenilaianSiswa) {

				Matapelajaran matapelajaran = kurikulumPunyaMatapelajaran.getMatapelajaran();
				Row row = new Row();
				row.setValign("top");
				row.setParent(rows);

				final MyDetail detail = new MyDetail();
				detail.setParent(row);
				detail.addEventListener("onOpen", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						if (detail.getChildren().isEmpty()) {

							DetailPenilaianSiswaHelper.displayPenilaian(kurikulumPunyaMatapelajaran, detail, kelasSiswa,
									siswas);
						}

					}

				});

				Guru guru = (Guru) ConstantValues.simpleObject(
						session.createCriteria(JadwalPelajaran.class).setProjection(Projections.property("guru.id"))
								.add(Restrictions.eq("kelas", kelasSiswa))
								.add(Restrictions.eq("matapelajaran", matapelajaran)).setMaxResults(1),
						Guru.class, false);

				new Label(Common.numberFormat.get().format(nomor)).setParent(row);
				row.appendChild(new Label(matapelajaran.getNama()));

				JenisPenilaian jenisPenilaian1 = matapelajaran.getJenisPenilaian() == null ? null
						: matapelajaran.getJenisPenilaian();
				if (kurikulumPunyaMatapelajaran != null && kurikulumPunyaMatapelajaran.getKurikulumSekolah() != null
						&& kurikulumPunyaMatapelajaran.getKurikulumSekolah().getJenisPenilaian() != null) {
					jenisPenilaian1 = kurikulumPunyaMatapelajaran.getKurikulumSekolah().getJenisPenilaian();
				}

				new Label(jenisPenilaian1 == null ? "" : jenisPenilaian1.getJenis()).setParent(row);
				new Label(guru == null ? "" : guru.getNama()).setParent(row);
				new Label(matapelajaran.getKeterangan()).setParent(row);

				nomor++;

			}
		}

	}

}
