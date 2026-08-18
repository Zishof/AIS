package ais.action.master.sekolah.helper;

import java.awt.Color;
import java.io.File;
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
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
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
import ais.database.model.Konfigurasi;
import ais.database.model.ParameterTambahan;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranLain;
import ais.database.model.sekolah.DetailGrupKategoriItemPenilaianSiswa;
import ais.database.model.sekolah.DetailGrupPenilaian;
import ais.database.model.sekolah.DetailJenisPenilaian;
import ais.database.model.sekolah.GrupKategoriItemPenilaianSiswa;
import ais.database.model.sekolah.GrupPenilaian;
import ais.database.model.sekolah.JenisItemPenilaianSiswa;
import ais.database.model.sekolah.JenisPenilaian;
import ais.database.model.sekolah.KategoriItemPenilaianSiswa;
import ais.database.model.sekolah.KelasLesSiswa;
import ais.database.model.sekolah.KelasLesSiswaPunyaSiswa;
import ais.database.model.sekolah.Matapelajaran;
import ais.database.model.sekolah.NilaiHurufSekolah;
import ais.database.model.sekolah.Siswa;
import ais.ui.util.DataCriteria;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelAgakKecilBold;
import ais.ui.util.MyLabelBoldAja;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyTextboxAngka;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;

public class DetailPenilaianLesSiswaHelper {

	private KelasLesSiswa kelasLesSiswa;
	private static String[] contents = new String[] { "siswa.id", "siswa.nomorInduk", "siswa.nomorIndukNasional",
			"siswa.namaSiswa", "siswa.tahunMasuk", "siswa.sekolah.nama", "siswa.sekolah.yayasan" };

	public DetailPenilaianLesSiswaHelper() {
		// delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		// create = CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE);

	}

	public Criteria initCriteria(boolean order) {

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(KelasLesSiswaPunyaSiswa.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.eq("kelasLesSiswa", kelasLesSiswa))

				.createAlias("siswa", "siswa");

		if (order) {
			criteria.addOrder(Order.asc("nomorUrut")).addOrder(Order.asc("siswa.tahunMasuk"))
					.addOrder(Order.asc("siswa.namaSiswa")).addOrder(Order.desc("siswa.id"));
		}

		return criteria;
	}

	private static Rows createRows(GrupPenilaian grupPenilaian,
			GrupKategoriItemPenilaianSiswa grupKategoriItemPenilaianSiswa, KelasLesSiswa kelasLesSiswa,
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

		columnTotalGanjil.setWidth("5%");
		columnMinGanjil.setWidth("5%");
		columnMaxGanjil.setWidth("5%");

		Rows rows = new Rows();
		rows.setParent(grid);
		return rows;
	}

	private static Rows createRowsTanpaMinMax(GrupPenilaian grupPenilaian,
			GrupKategoriItemPenilaianSiswa grupKategoriItemPenilaianSiswa, KelasLesSiswa kelasLesSiswa,
			Matapelajaran matapelajaran, Tbmuser tbmuser, EventListener eventListener, Groupbox groupboxData,
			MyToolbarbuttonConfig upload) {
		MyGrid grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(50);
		grid.getPagingChild().setMold("os");
		grid.setParent(groupboxData);

		MyColumnConfig columnTotalGanjil = new MyColumnConfig();

		MyColumnConfig columnHurufGanjil = new MyColumnConfig();

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

		columnTotalGanjil.setWidth("5%");

		columnHurufGanjil.setWidth("5%");

		Rows rows = new Rows();
		rows.setParent(grid);
		return rows;
	}

	@SuppressWarnings({ "unchecked", "deprecation" })
	public static void displayPenilaian(final Component detail, final KelasLesSiswa kelasLesSiswa,
			List<KelasLesSiswaPunyaSiswa> siswasTemp) throws Exception {

		final Tbmuser tbmuser = Common.getCurrentUser();
		final Boolean hanyaValid = tbmuser != null && (tbmuser.getSiswa() != null || tbmuser.getCalonSiswa() != null)
				? true
				: null;

		final List<KelasLesSiswaPunyaSiswa> siswas = new ArrayList<KelasLesSiswaPunyaSiswa>();

		for (KelasLesSiswaPunyaSiswa kelasLesSiswaPunyaSiswa : siswasTemp) {
			if (kelasLesSiswaPunyaSiswa.getAktif()) {
				siswas.add(kelasLesSiswaPunyaSiswa);
			}
		}

		Session session = HibernateUtil.currentSession();

		JenisPenilaian jenisPenilaian = kelasLesSiswa.getMatapelajaran().getJenisPenilaian();

		List<GrupPenilaian> grupPenilaians = ConstantValues.simpleList(session
				.createCriteria(DetailJenisPenilaian.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.eq("jenisPenilaian", jenisPenilaian)).add(Restrictions.isNotNull("grupPenilaian.id"))
				.setProjection(Projections.groupProperty("grupPenilaian.id")), GrupPenilaian.class, false);

		final int tinggi = siswas.size();

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

			if (grupPenilaian != null && kelasLesSiswa.getTingkat() > 0 && grupPenilaian.getKhususTingkat() != null
					&& !grupPenilaian.getKhususTingkat().equals(kelasLesSiswa.getTingkat())) {
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

							if (grupKategoriItemPenilaianSiswa != null && kelasLesSiswa.getTingkat() > 0
									&& grupKategoriItemPenilaianSiswa.getKhususTingkat() != null
									&& !grupKategoriItemPenilaianSiswa.getKhususTingkat()
											.equals(kelasLesSiswa.getTingkat())) {
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

									@Override
									public void onEvent(Event arg0) throws Exception {
										Common.clear(tabpanel1);

										Groupbox groupboxData = new Groupbox();
										groupboxData.setParent(tabpanel1);

										Toolbar toolbar = new Toolbar();
										toolbar.setParent(groupboxData);

										toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Siswa : ")));
										toolbar.appendChild(nama = new Textbox());
										MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Cari",
												"/img/svg/search.svg");
										button.addEventListener("onClick", this);
										button.setParent(toolbar);

										nama.addEventListener("onOK", this);

										int[] smts = new int[] { 1 };

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

												int[] smts = new int[] { 1 };

												if (grupPenilaian != null
														&& grupPenilaian.getKhususSemester() != null) {
													smts = new int[] { grupPenilaian.getKhususSemester() };
												}

												Object[] objects = (Object[]) arg0.getData();
												KelasLesSiswaPunyaSiswa kelasLesSiswaPunyaSiswa = (KelasLesSiswaPunyaSiswa) objects[0];
												XSSFRow row = (XSSFRow) objects[2];
												row.getCell(0).setCellValue(kelasLesSiswaPunyaSiswa.getSiswa().getId());

												Siswa siswa = kelasLesSiswaPunyaSiswa.getSiswa();

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

														total = kelasLesSiswaPunyaSiswa.retreiveTotalNilaiTotal(target,
																kelasLesSiswa.getMatapelajaran(), grupPenilaian, smt,
																grupKategoriItemPenilaianSiswas);

														target = GrupPenilaianUtil.ambilTargetMin(formula, sekarang);

														min = kelasLesSiswaPunyaSiswa.retreiveTotalNilaiTotal(target,
																kelasLesSiswa.getMatapelajaran(), grupPenilaian, smt,
																grupKategoriItemPenilaianSiswas);

														target = GrupPenilaianUtil.ambilTargetMax(formula, sekarang);

														max = kelasLesSiswaPunyaSiswa.retreiveTotalNilaiTotal(target,
																kelasLesSiswa.getMatapelajaran(), grupPenilaian, smt,
																grupKategoriItemPenilaianSiswas);

													} catch (Exception e) {
														e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/helper/DetailPenilaianLesSiswaHelper.java:466");
													}

													JSONObject js = new JSONObject();
													try {
														js = new JSONObject(
																smt == 1 ? kelasLesSiswaPunyaSiswa.getKeterangan1()
																		: kelasLesSiswaPunyaSiswa.getKeterangan2());
													} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/DetailPenilaianLesSiswaHelper.java:474");
													}

													String keyKet = kelasLesSiswa.getMatapelajaran().getId() + "_"
															+ grupPenilaian.getId();

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
																	siswa.getSekolah(), siswa.getYayasan(), null,
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

//										a

										MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakDataCustomButton(
												KelasLesSiswaPunyaSiswa.class, new DataCriteria() {

													@Override
													public Object initCriteria(boolean order) {
														// TODO Auto-generated method stub
														return siswas;
													}
												}, "Download Nilai", "/img/print.png", columnHeadersAdding, dataAdding,
												true, null, "Nilai", contents);
										toolbar.appendChild(cetakToolbarbutton);

										Rows rows = DetailPenilaianLesSiswaHelper.createRows(grupPenilaian,
												grupKategoriItemPenilaianSiswa, kelasLesSiswa,
												kelasLesSiswa.getMatapelajaran(), tbmuser, new EventListener() {

													@Override
													public void onEvent(Event arg0) throws Exception {
														Common.clear(detail);

														DetailPenilaianLesSiswaHelper.displayPenilaian(detail,
																kelasLesSiswa, siswas);
													}
												}, groupboxData);

										for (final KelasLesSiswaPunyaSiswa kelasLesSiswaPunyaSiswa : siswas) {

											if (nama.getValue().trim().isEmpty() ||

													(!nama.getValue().trim().toLowerCase().isEmpty()
															&& !kelasLesSiswaPunyaSiswa.getSiswa().getNama().isEmpty()
															&& kelasLesSiswaPunyaSiswa.getSiswa().getNama()
																	.toLowerCase()
																	.contains(nama.getValue().trim().toLowerCase()))

													||

													(!nama.getValue().trim().toLowerCase().isEmpty()
															&& !kelasLesSiswaPunyaSiswa.getSiswa().getNomorInduk()
																	.isEmpty()
															&& kelasLesSiswaPunyaSiswa.getSiswa().getNomorInduk()
																	.toLowerCase()
																	.contains(nama.getValue().trim().toLowerCase()))

													||

													(!nama.getValue().trim().toLowerCase().isEmpty()
															&& !kelasLesSiswaPunyaSiswa.getSiswa()
																	.getNomorIndukNasional().isEmpty()
															&& kelasLesSiswaPunyaSiswa.getSiswa()
																	.getNomorIndukNasional().toLowerCase()
																	.contains(nama.getValue().trim().toLowerCase()))

													||

													(!nama.getValue().trim().toLowerCase().isEmpty()
															&& !kelasLesSiswaPunyaSiswa.getSiswa().getNomorIndukSantri()
																	.isEmpty()
															&& kelasLesSiswaPunyaSiswa.getSiswa().getNomorIndukSantri()
																	.toLowerCase()
																	.contains(nama.getValue().trim().toLowerCase()))

											) {

												Row row = new Row();
												row.setValign("top");
												row.setValign("top");
												row.setParent(rows);

												Siswa siswa = kelasLesSiswaPunyaSiswa.getSiswa();
												CommonMedia.tampilkanGambarKecil(siswa).setParent(row);
												Vbox aa;
												(aa = RevisiHelper.createNewRevisi(KelasLesSiswaPunyaSiswa.class,
														kelasLesSiswaPunyaSiswa, siswa.getNomorInduk())).setParent(row);
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

														total = kelasLesSiswaPunyaSiswa.retreiveTotalNilaiTotal(target,
																kelasLesSiswa.getMatapelajaran(), grupPenilaian, smt,
																grupKategoriItemPenilaianSiswas);

														target = GrupPenilaianUtil.ambilTargetMin(formula, sekarang);

														min = kelasLesSiswaPunyaSiswa.retreiveTotalNilaiTotal(target,
																kelasLesSiswa.getMatapelajaran(), grupPenilaian, smt,
																grupKategoriItemPenilaianSiswas);

														target = GrupPenilaianUtil.ambilTargetMax(formula, sekarang);

														max = kelasLesSiswaPunyaSiswa.retreiveTotalNilaiTotal(target,
																kelasLesSiswa.getMatapelajaran(), grupPenilaian, smt,
																grupKategoriItemPenilaianSiswas);

													} catch (Exception e) {
														e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/helper/DetailPenilaianLesSiswaHelper.java:636");
													}

													JSONObject js = new JSONObject();
													try {
														js = new JSONObject(
																smt == 1 ? kelasLesSiswaPunyaSiswa.getKeterangan1()
																		: kelasLesSiswaPunyaSiswa.getKeterangan2());
													} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/DetailPenilaianLesSiswaHelper.java:644");
													}

													String keyKet = kelasLesSiswa.getMatapelajaran().getId() + "_"
															+ grupPenilaian.getId();

													String ket = js.isNull(keyKet) ? "" : js.getString(keyKet);

													MyTextbox myTextbox = new MyTextbox(ket);
													myTextbox.addEventListener("onChange", new EventListener() {

														@Override
														public void onEvent(Event arg0) throws Exception {
															String jumlah = ((Textbox) arg0.getTarget()).getValue()
																	.trim();

															JSONObject js = new JSONObject();
															try {
																js = new JSONObject(smt == 1
																		? kelasLesSiswaPunyaSiswa.getKeterangan1()
																		: kelasLesSiswaPunyaSiswa.getKeterangan2());
															} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/DetailPenilaianLesSiswaHelper.java:665");
															}

															String keyKet = kelasLesSiswa.getMatapelajaran().getId()
																	+ "_" + grupPenilaian.getId();

															js.put(keyKet, jumlah);

															if (smt == 1) {
																kelasLesSiswaPunyaSiswa.setKeterangan1(js.toString());
															} else {
																kelasLesSiswaPunyaSiswa.setKeterangan2(js.toString());
															}
															Common.refreshUpdate(kelasLesSiswaPunyaSiswa);
														}
													});
													myTextbox.setParent(row);
													myTextbox.setWidth("95%");
													myTextbox.setRows(2);

													RevisiHelper.createNewRevisi(GrupPenilaian.class, grupPenilaian,
															Common.numberFormat.get().format(total)).setParent(row);

													RevisiHelper.createNewRevisi(GrupPenilaian.class, grupPenilaian,
															Common.numberFormat.get().format(min)).setParent(row);

													RevisiHelper.createNewRevisi(GrupPenilaian.class, grupPenilaian,
															Common.numberFormat.get().format(max)).setParent(row);

													NilaiHurufSekolah nilaiHurufSekolah = NilaiHurufSekolah
															.getNilaiHurufSekolah(total, siswa.getTahunMasuk(),
																	siswa.getSekolah(), siswa.getYayasan(), null,
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
										private String textNama = "";

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

												toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Siswa : ")));
												toolbar.appendChild(nama = new Textbox(textNama));
												MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Cari",
														"/img/svg/search.svg");
												button.addEventListener("onClick", new EventListener() {

													@Override
													public void onEvent(Event arg0) throws Exception {
														textNama = nama.getValue().trim();
														Common.clear(tabpanel1);
														getThis().onEvent(arg0);
													}
												});
												button.setParent(toolbar);

												nama.addEventListener("onOK", new EventListener() {

													@Override
													public void onEvent(Event arg0) throws Exception {
														textNama = nama.getValue().trim();
														Common.clear(tabpanel1);
														getThis().onEvent(arg0);
													}
												});

												int[] smts = new int[] { 1 };

												if (grupKategoriItemPenilaianSiswa != null
														&& grupKategoriItemPenilaianSiswa.getKhususSemester() != null) {
													smts = new int[] {
															grupKategoriItemPenilaianSiswa.getKhususSemester() };
												}

												if (grupPenilaian != null
														&& grupPenilaian.getKhususSemester() != null) {
													smts = new int[] { grupPenilaian.getKhususSemester() };
												}

												List<String> columnHeadersAdding = new ArrayList<String>();
												for (int smt : smts) {
													for (final JenisItemPenilaianSiswa jenisItemPenilaianSiswa : jenisItemPenilaianSiswas) {
														columnHeadersAdding
																.add(jenisItemPenilaianSiswa.getNama() + "|" + smt);

														if (!jenisItemPenilaianSiswa.getTipeDataInputan()
																.equals(JenisItemPenilaianSiswa.FORMULA)) {
															columnHeadersAdding.add(jenisItemPenilaianSiswa.getNama()
																	+ "|" + smt + "|verif");
														}

													}
													columnHeadersAdding.add("Total|" + smt);
												}

												EventListener dataAdding = new EventListener() {

													@Override
													public void onEvent(Event arg0) throws Exception {

														int[] smts = new int[] { 1 };

														if (grupPenilaian != null
																&& grupPenilaian.getKhususSemester() != null) {
															smts = new int[] { grupPenilaian.getKhususSemester() };
														}

														Object[] objects = (Object[]) arg0.getData();
														KelasLesSiswaPunyaSiswa kelasLesSiswaPunyaSiswa = (KelasLesSiswaPunyaSiswa) objects[0];
														XSSFRow row = (XSSFRow) objects[2];

														row.getCell(0).setCellValue(
																kelasLesSiswaPunyaSiswa.getSiswa().getId());

														XSSFWorkbook workbook = (XSSFWorkbook) objects[3];

														XSSFFont hlink_font = workbook.createFont();
														XSSFCellStyle hlink_style = workbook.createCellStyle();
														hlink_style.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
														hlink_style.setFillForegroundColor(
																new XSSFColor(Color.LIGHT_GRAY));
														hlink_style.setFont(hlink_font);

														XSSFCellStyle hlink_style1 = workbook.createCellStyle();
														hlink_style1.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
														hlink_style1.setFillForegroundColor(new XSSFColor(Color.CYAN));
														hlink_style1.setFont(hlink_font);

														XSSFCellStyle hlink_style2 = workbook.createCellStyle();
														hlink_style2.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
														hlink_style2
																.setFillForegroundColor(new XSSFColor(Color.YELLOW));
														hlink_style2.setFont(hlink_font);

														int idex = 0;
														for (int smt : smts) {

															Double total = 0.0;

															try {
																Date sekarang = WaktuUtil.getDate();
																String formula = grupKategoriItemPenilaianSiswa
																		.getFormula();
																String target = GrupPenilaianUtil.ambilTarget(formula,
																		sekarang);
																total = kelasLesSiswaPunyaSiswa.retreiveTotalNilai(
																		jenisItemPenilaianSiswas, target,
																		kelasLesSiswa.getMatapelajaran(), grupPenilaian,
																		grupKategoriItemPenilaianSiswa, smt,
																		hanyaValid);
															} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/DetailPenilaianLesSiswaHelper.java:880");
															}

															for (JenisItemPenilaianSiswa jenisItemPenilaianSiswa : jenisItemPenilaianSiswas) {

																String val = kelasLesSiswaPunyaSiswa
																		.retreiveDetailNilai(jenisItemPenilaianSiswa,
																				grupKategoriItemPenilaianSiswa,
																				kelasLesSiswa.getMatapelajaran(), smt,
																				hanyaValid);
																XSSFCell cellTambahan = row
																		.createCell(contents.length + idex);

																if (jenisItemPenilaianSiswa.getTipeDataInputan()
																		.equals(JenisItemPenilaianSiswa.ANGKA)
																		|| jenisItemPenilaianSiswa.getTipeDataInputan()
																				.equals(JenisItemPenilaianSiswa.FORMULA)
																		|| jenisItemPenilaianSiswa.getTipeDataInputan()
																				.equals(JenisItemPenilaianSiswa.TEXT_ANGKA)) {
																	try {
																		cellTambahan.setCellValue(
																				val == null || val.isEmpty() ? 0.0
																						: Double.parseDouble(val));
																	} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/DetailPenilaianLesSiswaHelper.java:903");
																	}
																} else {
																	cellTambahan.setCellValue(val);
																}

																if (jenisItemPenilaianSiswa.getTipeDataInputan()
																		.equals(JenisItemPenilaianSiswa.FORMULA)) {
																	cellTambahan.setCellStyle(hlink_style1);
																} else {
																	cellTambahan.setCellStyle(hlink_style);

																	idex++;

																	Boolean sesuai = kelasLesSiswaPunyaSiswa
																			.retreiveDetailVerify(
																					jenisItemPenilaianSiswa,
																					grupKategoriItemPenilaianSiswa,
																					kelasLesSiswa.getMatapelajaran(),
																					smt);

																	XSSFCell cellTambahanVerif = row
																			.createCell(contents.length + idex);
																	cellTambahanVerif.setCellValue(sesuai);
																	cellTambahanVerif.setCellStyle(hlink_style2);
																}

																idex++;
															}

															XSSFCell cellTambahan = row
																	.createCell(contents.length + idex);
															cellTambahan.setCellValue(total);
															cellTambahan.setCellStyle(hlink_style1);

															idex++;
														}

													}
												};

												MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakDataCustomButton(
														KelasLesSiswaPunyaSiswa.class, new DataCriteria() {

															@Override
															public Object initCriteria(boolean order) {
																// TODO Auto-generated method stub
																return siswas;
															}
														}, "Download Nilai", "/img/print.png", columnHeadersAdding,
														dataAdding, true, null, "Nilai", contents);
												toolbar.appendChild(cetakToolbarbutton);

												MyToolbarbuttonConfig upload = new MyToolbarbuttonConfig(
														"Upload Nilai " + Common.ukuranLabelFileUpload(),
														"/img/excel.png");
												upload.setUpload(Common.ukuranFileUpload());
												upload.addEventListener("onUpload", new EventListener() {
													@Override
													public void onEvent(Event event) throws Exception {
														UploadEvent uploadEvent = (UploadEvent) event;
														Media media = uploadEvent.getMedia();
														if (!ais.action.master.helper.generic.AmbilDataTugasFileContent
																.checkFile(media))
															return;
														if (media.getName().toLowerCase().endsWith("xlsx")) {

															InputStream inputStream = media.getStreamData();
															// System.out.println("media = " + media);
															final File file = new File(Sessions.getCurrent().getWebApp()
																	.getRealPath("/temp/" + media.getName()));
															// System.out.println("file = " + file.getAbsolutePath());
															file.getParentFile().mkdirs();
															FileOutputStream fileOutputStream = new FileOutputStream(
																	file);
															int c;
															while ((c = inputStream.read()) != -1) {
																fileOutputStream.write(c);
															}
															fileOutputStream.close();
															inputStream.close();

															Common.createDefaultTimer(new EventListener() {

																@Override
																public void onEvent(Event arg0) throws Exception {

																	int[] smts = new int[] { 1 };

																	if (grupPenilaian != null && grupPenilaian
																			.getKhususSemester() != null) {
																		smts = new int[] {
																				grupPenilaian.getKhususSemester() };
																	}

																	uploadDataNilai(file, new EventListener() {

																		@Override
																		public void onEvent(Event arg0)
																				throws Exception {
																			Common.clear(tabpanel1);
																			getThis().onEvent(arg0);
																		}
																	}, contents, smts, jenisItemPenilaianSiswas, siswas,
																			kelasLesSiswa,
																			grupKategoriItemPenilaianSiswa,
																			grupPenilaian);
																}
															}, "Harap tunggu.. sedang melakukan proses upload data..");

														} else {
															MyMessageboxConfig.show(
																	"File yang anda upload harus ber-format Excel Open XML Spreadsheet (xlsx). Jika masih menggunakan format lain, buka file excel tersebut, kemudian Save As Excel Open XML Spreadsheet (xlsx). "
																			+ media,
																	"Error", MyMessageboxConfig.OK,
																	MyMessageboxConfig.ERROR);
														}
													}
												});
												toolbar.appendChild(upload);

												Rows rows = DetailPenilaianLesSiswaHelper.createRowsTanpaMinMax(
														grupPenilaian, grupKategoriItemPenilaianSiswa, kelasLesSiswa,
														kelasLesSiswa.getMatapelajaran(), tbmuser, new EventListener() {

															@Override
															public void onEvent(Event arg0) throws Exception {
																Common.clear(tabpanel1);
																getThis().onEvent(arg0);
															}
														},

														groupboxData, upload);

												for (final KelasLesSiswaPunyaSiswa kelasLesSiswaPunyaSiswa : siswas) {

													if (nama.getValue().trim().isEmpty() ||

															(!nama.getValue().trim().toLowerCase().isEmpty()
																	&& !kelasLesSiswaPunyaSiswa.getSiswa().getNama()
																			.isEmpty()
																	&& kelasLesSiswaPunyaSiswa.getSiswa().getNama()
																			.toLowerCase()
																			.contains(nama.getValue().trim()
																					.toLowerCase()))

															||

															(!nama.getValue().trim().toLowerCase().isEmpty()
																	&& !kelasLesSiswaPunyaSiswa.getSiswa()
																			.getNomorInduk().isEmpty()
																	&& kelasLesSiswaPunyaSiswa.getSiswa()
																			.getNomorInduk().toLowerCase()
																			.contains(nama.getValue().trim()
																					.toLowerCase()))

															||

															(!nama.getValue().trim().toLowerCase().isEmpty()
																	&& !kelasLesSiswaPunyaSiswa.getSiswa()
																			.getNomorIndukNasional().isEmpty()
																	&& kelasLesSiswaPunyaSiswa.getSiswa()
																			.getNomorIndukNasional().toLowerCase()
																			.contains(nama.getValue().trim()
																					.toLowerCase()))

															||

															(!nama.getValue().trim().toLowerCase().isEmpty()
																	&& !kelasLesSiswaPunyaSiswa.getSiswa()
																			.getNomorIndukSantri().isEmpty()
																	&& kelasLesSiswaPunyaSiswa.getSiswa()
																			.getNomorIndukSantri().toLowerCase()
																			.contains(nama.getValue().trim()
																					.toLowerCase()))

													) {

														Row row = new Row();
														row.setValign("top");
														row.setValign("top");
														row.setParent(rows);

														final Siswa siswa = kelasLesSiswaPunyaSiswa.getSiswa();
														CommonMedia.tampilkanGambarKecil(siswa).setParent(row);
														Vbox aa;
														(aa = RevisiHelper.createNewRevisi(
																KelasLesSiswaPunyaSiswa.class, kelasLesSiswaPunyaSiswa,
																siswa.getNomorInduk())).setParent(row);
														new Label(siswa.getNomorIndukNasional()).setParent(aa);
														new Label(siswa.getNama()).setParent(aa);

														for (final int smt : smts) {
															Double total = 0.0;

															try {
																Date sekarang = WaktuUtil.getDate();
																String formula = grupKategoriItemPenilaianSiswa
																		.getFormula();
																String target = GrupPenilaianUtil.ambilTarget(formula,
																		sekarang);
																total = kelasLesSiswaPunyaSiswa.retreiveTotalNilai(
																		jenisItemPenilaianSiswas, target,
																		kelasLesSiswa.getMatapelajaran(), grupPenilaian,
																		grupKategoriItemPenilaianSiswa, smt,
																		hanyaValid);
															} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/DetailPenilaianLesSiswaHelper.java:1109");
															}

															final MyLabelBoldAja labelNilai = new MyLabelBoldAja();
															labelNilai.setValue(Common.numberFormat.get().format(total));

															NilaiHurufSekolah nilaiHurufSekolah = NilaiHurufSekolah
																	.getNilaiHurufSekolah(total, siswa.getTahunMasuk(),
																			siswa.getSekolah(), siswa.getYayasan(),
																			null,
																			smt % 2 == 0 ? Perkuliahan.GENAP
																					: Perkuliahan.GANJIL,
																			grupPenilaian.getJenisNilaiHuruf());

															final MyLabelBoldAja labelNilaiHuruf = new MyLabelBoldAja();
															labelNilaiHuruf.setValue(nilaiHurufSekolah == null ? ""
																	: nilaiHurufSekolah.getNilaiHuruf());

															final MyWindow myWindow = new MyWindow();
															final MyGrid subGrid = new MyGrid();

															Columns columns = new Columns();
															columns.setParent(subGrid);

															MyColumnConfig column = new MyColumnConfig("Komponen");
															column.setParent(columns);
															column.setWidth("30%");

															column = new MyColumnConfig("Nilai");
															column.setParent(columns);
															column.setAlign("right");

															column = new MyColumnConfig("Sesuai");
															column.setParent(columns);
															column.setWidth("15%");

															Vbox vbData = new Vbox();
															vbData.setParent(row);

															Toolbarbutton toolbarbutton = new MyToolbarbuttonConfig(
																	"Ubah Nilai", "/img/svg/edit-box-line.svg");

															if (tbmuser != null && tbmuser.ambilGuru() != null
																	&& !grupKategoriItemPenilaianSiswa
																			.getNilaiBolehDinputOlehGuru()) {

															} else {
																toolbarbutton.setParent(vbData);
															}

															if (tbmuser != null && tbmuser.ambilGuru() != null
																	&& !grupPenilaian.getNilaiBolehDinputOlehGuru()) {

															} else {
																toolbarbutton.setParent(vbData);
															}

															final MyLabelKecil agakKecil = new MyLabelKecil();

															vbData.appendChild(agakKecil);

															toolbarbutton.addEventListener("onClick",
																	new EventListener() {

																		@Override
																		public void onEvent(Event arg0)
																				throws Exception {

																			if (myWindow.getChildren().isEmpty()) {
																				myWindow.setParent(
																						ExecutionsCtrl.getCurrentCtrl()
																								.getCurrentPage()
																								.getFirstRoot());
																				myWindow.setHeight("95%");
																				myWindow.setWidth("500px");

																				Borderlayout borderlayout = new Borderlayout();
																				borderlayout.setParent(myWindow);
																				Center center = new Center();
																				center.setParent(borderlayout);
																				ais.ui.util.ZkCompat.setFlex(center, true);

																				center.appendChild(subGrid);

																				South south = new South();
																				ais.ui.util.ZkCompat.setFlex(south, true);
																				south.setParent(borderlayout);

																				Toolbar toolbar = new Toolbar();
																				toolbar.setParent(south);
																				MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig(
																						"Tutup", "/img/cancel.gif");
																				cancel.setTooltiptext("Tutup");
																				cancel.addEventListener("onClick",
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
																				cancel.setParent(toolbar);
																			}
																			myWindow.setVisible(true);
																			myWindow.onModal();
																		}
																	});

															final Rows subRows = new Rows();
															subRows.setParent(subGrid);

															Foot foot = new Foot();
															foot.setParent(subGrid);

															Footer footer = new Footer("Total");
															footer.setParent(foot);

															footer.setStyle("font-size:16px;font-weight: bolder;");

															Footer footerTotalData = new Footer();
															footerTotalData.setParent(foot);
															Date sekarang = WaktuUtil.getDate();
															String formula = grupKategoriItemPenilaianSiswa
																	.getFormula();
															String target = GrupPenilaianUtil.ambilTarget(formula,
																	sekarang);

															Vbox vboxD = new Vbox();
															vboxD.setParent(footerTotalData);
															vboxD.setAlign("right");
															vboxD.setPack("end");
															vboxD.setWidth("95%");
															final MyLabelAgakKecilBold footerTotal;
															final MyLabelKecil footerTarget;
															vboxD.appendChild(footerTotal = new MyLabelAgakKecilBold(
																	labelNilai.getValue()));
															vboxD.appendChild(footerTarget = new MyLabelKecil(target));

															EventListener tampilDataPenilaian = new EventListener() {

																private EventListener getThisEventLocal() {
																	return this;
																}

																@Override
																public void onEvent(Event arg0) throws Exception {

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

																		String val = kelasLesSiswaPunyaSiswa
																				.retreiveDetailNilai(
																						jenisItemPenilaianSiswa,
																						grupKategoriItemPenilaianSiswa,
																						kelasLesSiswa
																								.getMatapelajaran(),
																						smt, hanyaValid);

																		final Boolean sesuai = kelasLesSiswaPunyaSiswa
																				.retreiveDetailVerify(
																						jenisItemPenilaianSiswa,
																						grupKategoriItemPenilaianSiswa,
																						kelasLesSiswa
																								.getMatapelajaran(),
																						smt);

																		verify.setChecked(sesuai);

																		valdata += valdata.isEmpty()
																				? jenisItemPenilaianSiswa.getNama()
																						+ "=" + val
																				: ";" + jenisItemPenilaianSiswa
																						.getNama() + "=" + val;

																		Konfigurasi kunci = grupKategoriItemPenilaianSiswa == null
																				? null
																				: Common.getKonfigurasi(
																						"kunci_nilai_sekolah_"
																								+ kelasLesSiswa
																										.getMatapelajaran()
																										.getId()
																								+ "_"
																								+ grupPenilaian.getId()
																								+ "_"
																								+ grupKategoriItemPenilaianSiswa
																										.getId()
																								+ "_"
																								+ kelasLesSiswa.getId()
																								+ "_" + smt,
																						Konfigurasi.AKTIF);

																		if (tbmuser != null && (tbmuser
																				.getSiswa() != null
																				|| tbmuser.getCalonSiswa() != null
																				|| (kunci != null && kunci
																						.getDikunci() != null))) {

																			subRow.appendChild(new Label(
																					jenisItemPenilaianSiswa.getKode()
																							+ " - "
																							+ jenisItemPenilaianSiswa
																									.getNama()));
																			subRow.appendChild(new Label(val));

																			subRow.appendChild(sesuai
																					? new Image("/img/svg/check2.svg")
																					: new Label());

																			if (jenisItemPenilaianSiswa
																					.getHarusMenyertakanLampiran()) {

																				Hbox hbox = new Hbox();
																				hbox.setWidth("100%");
																				hbox.setStyle(
																						"border:0px;background: transparent;");

																				LampiranLain
																						.createDownloadUploadFileLain(
																								hbox, siswa.getId(),
																								KelasLesSiswaPunyaSiswa.class
																										.getName()
																										+ "-"
																										+ kelasLesSiswaPunyaSiswa
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
																								}, null, false, false,
																								false, false, null);

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
																					public void onEvent(Event arg0)
																							throws Exception {
																						Date sekarang = WaktuUtil
																								.getDate();
																						String formula = jenisItemPenilaianSiswa
																								.getFormula();
																						String target = GrupPenilaianUtil
																								.ambilTarget(formula,
																										sekarang);

																						Double total = kelasLesSiswaPunyaSiswa
																								.retreiveTotalNilai(
																										jenisItemPenilaianSiswas,
																										target,
																										kelasLesSiswa
																												.getMatapelajaran(),
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
																										KelasLesSiswaPunyaSiswa.class,
																										kelasLesSiswaPunyaSiswa,
																										Common.numberFormat.get()
																												.format(total),
																										"font-size:11px;font-weight: bolder;");

																						vboxD.appendChild(footerTotal1);
																						vboxD.appendChild(
																								footerTarget1);

																						kelasLesSiswaPunyaSiswa
																								.populateDetailNilai(
																										jenisItemPenilaianSiswa,
																										kelasLesSiswa
																												.getMatapelajaran(),
																										grupKategoriItemPenilaianSiswa,
																										total + "",
																										verify.isChecked(),
																										smt);

																						formula = grupKategoriItemPenilaianSiswa
																								.getFormula();
																						target = GrupPenilaianUtil
																								.ambilTarget(formula,
																										sekarang);

																						Double tot = kelasLesSiswaPunyaSiswa
																								.retreiveTotalNilai(
																										jenisItemPenilaianSiswas,
																										target,
																										kelasLesSiswa
																												.getMatapelajaran(),
																										grupPenilaian,
																										grupKategoriItemPenilaianSiswa,
																										smt,
																										hanyaValid);

																						labelNilai.setValue(
																								Common.numberFormat.get()
																										.format(tot));

																						kelasLesSiswaPunyaSiswa
																								.populateDetailNilaiTotal(
																										kelasLesSiswa
																												.getMatapelajaran(),
																										grupKategoriItemPenilaianSiswa,
																										tot,
																										verify.isChecked(),
																										smt);
																						Common.refreshUpdate(
																								kelasLesSiswaPunyaSiswa);

																						footerTotal.setValue(
																								labelNilai.getValue());
																						footerTarget.setValue(target);

																					}

																				};

																				eventListenersFormula
																						.add(formulaeventListener);
																				Common.createDefaultTimer(
																						formulaeventListener);

																			}

																			else if (jenisItemPenilaianSiswa
																					.getTipeDataInputan()
																					.equals(JenisItemPenilaianSiswa.TEXT)) {
																				component = new Textbox(val);
																				((Textbox) component).setWidth("85%");
																				((Textbox) component)
																						.setRows(jenisItemPenilaianSiswa
																								.getJumlahBaris());
																				((Textbox) component).focus();

																				eventListener = new EventListener() {

																					@Override
																					public void onEvent(Event arg0)
																							throws Exception {
																						String jumlah = ((Textbox) component)
																								.getValue().trim();
																						kelasLesSiswaPunyaSiswa
																								.populateDetailNilai(
																										jenisItemPenilaianSiswa,
																										kelasLesSiswa
																												.getMatapelajaran(),
																										grupKategoriItemPenilaianSiswa,
																										jumlah,
																										verify.isChecked(),
																										smt);
																						Common.refreshUpdate(
																								kelasLesSiswaPunyaSiswa);

																						Date sekarang = WaktuUtil
																								.getDate();
																						String formula = grupKategoriItemPenilaianSiswa
																								.getFormula();
																						String target = GrupPenilaianUtil
																								.ambilTarget(formula,
																										sekarang);
																						labelNilai.setValue(
																								Common.numberFormat.get()
																										.format(kelasLesSiswaPunyaSiswa
																												.retreiveTotalNilai(
																														jenisItemPenilaianSiswas,
																														target,
																														kelasLesSiswa
																																.getMatapelajaran(),
																														grupPenilaian,
																														grupKategoriItemPenilaianSiswa,
																														smt,
																														hanyaValid)));

																						footerTotal.setValue(
																								labelNilai.getValue());
																						footerTarget.setValue(target);

																						String valdata = "";
																						for (JenisItemPenilaianSiswa f : jenisItemPenilaianSiswas) {

																							String val = kelasLesSiswaPunyaSiswa
																									.retreiveDetailNilai(
																											f,
																											grupKategoriItemPenilaianSiswa,
																											kelasLesSiswa
																													.getMatapelajaran(),
																											smt,
																											hanyaValid);

																							valdata += valdata.isEmpty()
																									? jenisItemPenilaianSiswa
																											.getNama()
																											+ "=" + val
																									: ";" + jenisItemPenilaianSiswa
																											.getNama()
																											+ "=" + val;
																						}
																						agakKecil.setValue(valdata);

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
																					nilai = val.trim().isEmpty() ? null
																							: Common.dateFormat1.get()
																									.parse(val);
																				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/DetailPenilaianLesSiswaHelper.java:1651");

																				}
																				component = new MyDatebox(nilai);
																				((MyDatebox) component).setWidth("85%");
																				((MyDatebox) component).focus();

																				eventListener = new EventListener() {

																					@Override
																					public void onEvent(Event arg0)
																							throws Exception {
																						Date jumlah = ((MyDatebox) component)
																								.getValue();
																						kelasLesSiswaPunyaSiswa
																								.populateDetailNilai(
																										jenisItemPenilaianSiswa,
																										kelasLesSiswa
																												.getMatapelajaran(),
																										grupKategoriItemPenilaianSiswa,
																										jumlah == null
																												? ""
																												: Common.dateFormat1.get()
																														.format(jumlah),
																										verify.isChecked(),
																										smt);
																						Common.refreshUpdate(
																								kelasLesSiswaPunyaSiswa);

																						Date sekarang = WaktuUtil
																								.getDate();
																						String formula = grupKategoriItemPenilaianSiswa
																								.getFormula();
																						String target = GrupPenilaianUtil
																								.ambilTarget(formula,
																										sekarang);

																						labelNilai.setValue(
																								Common.numberFormat.get()
																										.format(kelasLesSiswaPunyaSiswa
																												.retreiveTotalNilai(
																														jenisItemPenilaianSiswas,
																														target,
																														kelasLesSiswa
																																.getMatapelajaran(),
																														grupPenilaian,
																														grupKategoriItemPenilaianSiswa,
																														smt,
																														hanyaValid)));

																						footerTotal.setValue(
																								labelNilai.getValue());
																						footerTarget.setValue(target);

																						String valdata = "";
																						for (JenisItemPenilaianSiswa f : jenisItemPenilaianSiswas) {

																							String val = kelasLesSiswaPunyaSiswa
																									.retreiveDetailNilai(
																											f,
																											grupKategoriItemPenilaianSiswa,
																											kelasLesSiswa
																													.getMatapelajaran(),
																											smt,
																											hanyaValid);

																							valdata += valdata.isEmpty()
																									? jenisItemPenilaianSiswa
																											.getNama()
																											+ "=" + val
																									: ";" + jenisItemPenilaianSiswa
																											.getNama()
																											+ "=" + val;
																						}
																						agakKecil.setValue(valdata);

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
																					nilai = val.trim().isEmpty() ? 0.0
																							: Double.parseDouble(val);
																				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/DetailPenilaianLesSiswaHelper.java:1752");

																				}
																				component = new MyDoublebox(nilai);
																				((MyDoublebox) component)
																						.setWidth("85%");

																				final Double nilailama = nilai;

																				eventListener = new EventListener() {

																					@Override
																					public void onEvent(Event arg0)
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

																						kelasLesSiswaPunyaSiswa
																								.populateDetailNilai(
																										jenisItemPenilaianSiswa,
																										kelasLesSiswa
																												.getMatapelajaran(),
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
																								.ambilTarget(formula,
																										sekarang);

																						Double total = kelasLesSiswaPunyaSiswa
																								.retreiveTotalNilai(
																										jenisItemPenilaianSiswas,
																										target,
																										kelasLesSiswa
																												.getMatapelajaran(),
																										grupPenilaian,
																										grupKategoriItemPenilaianSiswa,
																										smt,
																										hanyaValid);

																						kelasLesSiswaPunyaSiswa
																								.populateDetailNilaiTotal(
																										kelasLesSiswa
																												.getMatapelajaran(),
																										grupKategoriItemPenilaianSiswa,
																										total,
																										verify.isChecked(),
																										smt);
																						Common.refreshUpdate(
																								kelasLesSiswaPunyaSiswa);

																						NilaiHurufSekolah nilaiHurufSekolah = NilaiHurufSekolah
																								.getNilaiHurufSekolah(
																										total,
																										siswa.getTahunMasuk(),
																										siswa.getSekolah(),
																										siswa.getYayasan(),
																										null,
																										smt % 2 == 0
																												? Perkuliahan.GENAP
																												: Perkuliahan.GANJIL,
																										grupPenilaian
																												.getJenisNilaiHuruf());
																						labelNilaiHuruf.setValue(
																								nilaiHurufSekolah == null
																										? ""
																										: nilaiHurufSekolah
																												.getNilaiHuruf());

																						labelNilai.setValue(
																								Common.numberFormat.get()
																										.format(total));
																						footerTotal.setValue(
																								labelNilai.getValue());
																						footerTarget.setValue(target);

																						String valdata = "";
																						for (JenisItemPenilaianSiswa f : jenisItemPenilaianSiswas) {

																							String val = kelasLesSiswaPunyaSiswa
																									.retreiveDetailNilai(
																											f,
																											grupKategoriItemPenilaianSiswa,
																											kelasLesSiswa
																													.getMatapelajaran(),
																											smt,
																											hanyaValid);

																							valdata += valdata.isEmpty()
																									? jenisItemPenilaianSiswa
																											.getNama()
																											+ "=" + val
																									: ";" + jenisItemPenilaianSiswa
																											.getNama()
																											+ "=" + val;
																						}
																						agakKecil.setValue(valdata);

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
																						val == null || val.isEmpty()
																								? "0"
																								: val);
																				((Textbox) component).setWidth("85%");
																				((Textbox) component).focus();
																				eventListener = new EventListener() {

																					@Override
																					public void onEvent(Event arg0)
																							throws Exception {
																						String jumlah = ((Textbox) component)
																								.getValue().trim();
																						kelasLesSiswaPunyaSiswa
																								.populateDetailNilai(
																										jenisItemPenilaianSiswa,
																										kelasLesSiswa
																												.getMatapelajaran(),
																										grupKategoriItemPenilaianSiswa,
																										jumlah,
																										verify.isChecked(),
																										smt);

																						Date sekarang = WaktuUtil
																								.getDate();
																						String formula = grupKategoriItemPenilaianSiswa
																								.getFormula();
																						String target = GrupPenilaianUtil
																								.ambilTarget(formula,
																										sekarang);

																						Double total = kelasLesSiswaPunyaSiswa
																								.retreiveTotalNilai(
																										jenisItemPenilaianSiswas,
																										target,
																										kelasLesSiswa
																												.getMatapelajaran(),
																										grupPenilaian,
																										grupKategoriItemPenilaianSiswa,
																										smt,
																										hanyaValid);

																						kelasLesSiswaPunyaSiswa
																								.populateDetailNilaiTotal(
																										kelasLesSiswa
																												.getMatapelajaran(),
																										grupKategoriItemPenilaianSiswa,
																										total,
																										verify.isChecked(),
																										smt);
																						Common.refreshUpdate(
																								kelasLesSiswaPunyaSiswa);

																						labelNilai.setValue(
																								Common.numberFormat.get()
																										.format(total));
																						footerTotal.setValue(
																								labelNilai.getValue());
																						footerTarget.setValue(target);

																						String valdata = "";
																						for (JenisItemPenilaianSiswa f : jenisItemPenilaianSiswas) {

																							String val = kelasLesSiswaPunyaSiswa
																									.retreiveDetailNilai(
																											f,
																											grupKategoriItemPenilaianSiswa,
																											kelasLesSiswa
																													.getMatapelajaran(),
																											smt,
																											hanyaValid);

																							valdata += valdata.isEmpty()
																									? jenisItemPenilaianSiswa
																											.getNama()
																											+ "=" + val
																									: ";" + jenisItemPenilaianSiswa
																											.getNama()
																											+ "=" + val;
																						}
																						agakKecil.setValue(valdata);

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
																					nilai = val.trim().isEmpty() ? null
																							: Boolean.parseBoolean(val);
																				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/DetailPenilaianLesSiswaHelper.java:2030");

																				}

																				component = new Combobox();
																				MyComboitemConfig comboitem = new MyComboitemConfig(
																						"Ya");
																				comboitem.setValue(true);
																				component.appendChild(comboitem);
																				comboitem = new MyComboitemConfig(
																						"Tidak");
																				comboitem.setValue(false);
																				component.appendChild(comboitem);

																				((Combobox) component)
																						.setReadonly(true);
																				Common.selectComboItem(
																						((Combobox) component), nilai);
																				((Combobox) component).setWidth("85%");

																				eventListener = new EventListener() {

																					@Override
																					public void onEvent(Event arg0)
																							throws Exception {
																						Object jumlah = ((Combobox) component)
																								.getSelectedItem() == null
																										? ""
																										: ((Combobox) component)
																												.getSelectedItem()
																												.getValue();
																						kelasLesSiswaPunyaSiswa
																								.populateDetailNilai(
																										jenisItemPenilaianSiswa,
																										kelasLesSiswa
																												.getMatapelajaran(),
																										grupKategoriItemPenilaianSiswa,
																										jumlah == null
																												? ""
																												: jumlah.toString(),
																										verify.isChecked(),
																										smt);
																						Common.refreshUpdate(
																								kelasLesSiswaPunyaSiswa);

																						Date sekarang = WaktuUtil
																								.getDate();
																						String formula = grupKategoriItemPenilaianSiswa
																								.getFormula();
																						String target = GrupPenilaianUtil
																								.ambilTarget(formula,
																										sekarang);

																						labelNilai.setValue(
																								Common.numberFormat.get()
																										.format(kelasLesSiswaPunyaSiswa
																												.retreiveTotalNilai(
																														jenisItemPenilaianSiswas,
																														target,
																														kelasLesSiswa
																																.getMatapelajaran(),
																														grupPenilaian,
																														grupKategoriItemPenilaianSiswa,
																														smt,
																														hanyaValid)));
																						footerTotal.setValue(
																								labelNilai.getValue());
																						footerTarget.setValue(target);

																						String valdata = "";
																						for (JenisItemPenilaianSiswa f : jenisItemPenilaianSiswas) {

																							String val = kelasLesSiswaPunyaSiswa
																									.retreiveDetailNilai(
																											f,
																											grupKategoriItemPenilaianSiswa,
																											kelasLesSiswa
																													.getMatapelajaran(),
																											smt,
																											hanyaValid);

																							valdata += valdata.isEmpty()
																									? jenisItemPenilaianSiswa
																											.getNama()
																											+ "=" + val
																									: ";" + jenisItemPenilaianSiswa
																											.getNama()
																											+ "=" + val;
																						}
																						agakKecil.setValue(valdata);

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

																					String[] kol = StringUtils.split(s,
																							":");
																					String a = kol[0];
																					Integer skor = 0;
																					try {
																						skor = Integer.parseInt(
																								kol[1].trim());
																					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/DetailPenilaianLesSiswaHelper.java:2155");

																					}
																					MyComboitemConfig comboitem = new MyComboitemConfig(
																							a);
																					comboitem.setValue(s);
																					comboitem.setAttribute("skor",
																							skor);
																					component.appendChild(comboitem);
																				}

																				((Combobox) component)
																						.setReadonly(true);
																				Common.selectComboItem(
																						((Combobox) component), val);
																				((Combobox) component).setWidth("85%");

																				eventListener = new EventListener() {

																					@Override
																					public void onEvent(Event arg0)
																							throws Exception {
																						Object jumlah = ((Combobox) component)
																								.getSelectedItem() == null
																										? ""
																										: ((Combobox) component)
																												.getSelectedItem()
																												.getValue();
																						kelasLesSiswaPunyaSiswa
																								.populateDetailNilai(
																										jenisItemPenilaianSiswa,
																										kelasLesSiswa
																												.getMatapelajaran(),
																										grupKategoriItemPenilaianSiswa,
																										jumlah == null
																												? ""
																												: jumlah.toString(),
																										verify.isChecked(),
																										smt);
																						Common.refreshUpdate(
																								kelasLesSiswaPunyaSiswa);

																						Date sekarang = WaktuUtil
																								.getDate();
																						String formula = grupKategoriItemPenilaianSiswa
																								.getFormula();
																						String target = GrupPenilaianUtil
																								.ambilTarget(formula,
																										sekarang);

																						labelNilai.setValue(
																								Common.numberFormat.get()
																										.format(kelasLesSiswaPunyaSiswa
																												.retreiveTotalNilai(
																														jenisItemPenilaianSiswas,
																														target,
																														kelasLesSiswa
																																.getMatapelajaran(),
																														grupPenilaian,
																														grupKategoriItemPenilaianSiswa,
																														smt,
																														hanyaValid)));

																						footerTotal.setValue(
																								labelNilai.getValue());
																						footerTarget.setValue(target);

																						String valdata = "";
																						for (JenisItemPenilaianSiswa f : jenisItemPenilaianSiswas) {

																							String val = kelasLesSiswaPunyaSiswa
																									.retreiveDetailNilai(
																											f,
																											grupKategoriItemPenilaianSiswa,
																											kelasLesSiswa
																													.getMatapelajaran(),
																											smt,
																											hanyaValid);

																							valdata += valdata.isEmpty()
																									? jenisItemPenilaianSiswa
																											.getNama()
																											+ "=" + val
																									: ";" + jenisItemPenilaianSiswa
																											.getNama()
																											+ "=" + val;
																						}
																						agakKecil.setValue(valdata);

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
																					component.appendChild(comboitem);

																					for (String g : val.split(";")) {
																						if (g.trim().equalsIgnoreCase(
																								s.trim())) {
																							comboitem.setChecked(true);
																						}
																					}

																				}

																				eventListener = new EventListener() {

																					@Override
																					public void onEvent(Event arg0)
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

																						kelasLesSiswaPunyaSiswa
																								.populateDetailNilai(
																										jenisItemPenilaianSiswa,
																										kelasLesSiswa
																												.getMatapelajaran(),
																										grupKategoriItemPenilaianSiswa,
																										jumlah,
																										verify.isChecked(),
																										smt);
																						Common.refreshUpdate(
																								kelasLesSiswaPunyaSiswa);

																						Date sekarang = WaktuUtil
																								.getDate();
																						String formula = grupKategoriItemPenilaianSiswa
																								.getFormula();
																						String target = GrupPenilaianUtil
																								.ambilTarget(formula,
																										sekarang);

																						labelNilai.setValue(
																								Common.numberFormat.get()
																										.format(kelasLesSiswaPunyaSiswa
																												.retreiveTotalNilai(
																														jenisItemPenilaianSiswas,
																														target,
																														kelasLesSiswa
																																.getMatapelajaran(),
																														grupPenilaian,
																														grupKategoriItemPenilaianSiswa,
																														smt,
																														hanyaValid)));

																						footerTotal.setValue(
																								labelNilai.getValue());
																						footerTarget.setValue(target);

																						String valdata = "";
																						for (JenisItemPenilaianSiswa f : jenisItemPenilaianSiswas) {

																							String val = kelasLesSiswaPunyaSiswa
																									.retreiveDetailNilai(
																											f,
																											grupKategoriItemPenilaianSiswa,
																											kelasLesSiswa
																													.getMatapelajaran(),
																											smt,
																											hanyaValid);

																							valdata += valdata.isEmpty()
																									? jenisItemPenilaianSiswa
																											.getNama()
																											+ "=" + val
																									: ";" + jenisItemPenilaianSiswa
																											.getNama()
																											+ "=" + val;
																						}
																						agakKecil.setValue(valdata);

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
																							"onChange", eventListener);

																					final EventListener ev = eventListener;

																					verify.addEventListener("onCheck",
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
																								.getKode() + " - "
																								+ jenisItemPenilaianSiswa
																										.getNama()));

																				if (sesuai && eventListener != null) {
																					subRow.appendChild(new Label(val));
																				} else {
																					subRow.appendChild(component);
																				}

																				if (component instanceof Label
																						|| component instanceof Vbox) {
																					subRow.appendChild(new Label());
																				}

																				else if (tbmuser.ambilGuru() != null) {

																					if (kelasLesSiswa
																							.getGuruBolehMemverifikasiSendiri()) {
																						subRow.appendChild(verify);
																					} else {
																						subRow.appendChild(sesuai
																								? new Image(
																										"/img/svg/check2.svg")
																								: new Label());
																					}

																				} else if (tbmuser != null && (tbmuser
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
																										KelasLesSiswaPunyaSiswa.class
																												.getName()
																												+ "-"
																												+ kelasLesSiswaPunyaSiswa
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
																						subRow.setParent(subRows);
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

	public static void uploadDataNilai(final File file, final EventListener eventListener, final String[] contents,
			final int[] smts, final List<JenisItemPenilaianSiswa> jenisItemPenilaianSiswas,
			final List<KelasLesSiswaPunyaSiswa> siswas, final KelasLesSiswa kelasLesSiswa,
			final GrupKategoriItemPenilaianSiswa grupKategoriItemPenilaianSiswa, final GrupPenilaian grupPenilaian)
			throws Exception {

		final Label peringatan = new Label("");

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
				try {

				try {

					XSSFWorkbook workbook = new XSSFWorkbook(file.getAbsolutePath());
					XSSFSheet sheet = workbook.getSheetAt(0);

					Session session = HibernateUtil.currentNativeSession();
					Boolean hanyaValid = null;
					int rowCount = (sheet.getLastRowNum() + 1);
					for (int i = 1; i < rowCount; i++) {
						@SuppressWarnings("rawtypes")
						Map datum = null;
						try {

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
								KelasLesSiswaPunyaSiswa kelasLesSiswaPunyaSiswa = null;
								for (KelasLesSiswaPunyaSiswa temp : siswas) {
									if (temp.getSiswa() != null && temp.getSiswa().getId() != null
											&& siswa.getId().equals(temp.getSiswa().getId())) {
										kelasLesSiswaPunyaSiswa = temp;
										break;
									}
								}
								if (kelasLesSiswaPunyaSiswa != null) {
									Date sekarang = WaktuUtil.getDate();
									session.refresh(kelasLesSiswaPunyaSiswa);
									int idex = 0;
									for (int smt : smts) {
										for (JenisItemPenilaianSiswa jenisItemPenilaianSiswa : jenisItemPenilaianSiswas) {
											String tot = "0.0";
											Boolean verif = null;
											if (jenisItemPenilaianSiswa.getTipeDataInputan()
													.equals(JenisItemPenilaianSiswa.FORMULA)) {

												String formula = jenisItemPenilaianSiswa.getFormula();
												String target = GrupPenilaianUtil.ambilTarget(formula, sekarang);

												Double total = kelasLesSiswaPunyaSiswa.retreiveTotalNilai(
														jenisItemPenilaianSiswas, target,
														kelasLesSiswa.getMatapelajaran(), grupPenilaian,
														grupKategoriItemPenilaianSiswa, smt, hanyaValid);

												tot = total + "";
												verif = true;
											} else {
												tot = Common.getSheetContentAsString(sheet, contents.length + idex, i);
												idex++;
												verif = Common.getSheetContentAsBoolean(sheet, contents.length + idex,
														i);
											}

											kelasLesSiswaPunyaSiswa.populateDetailNilai(jenisItemPenilaianSiswa,
													kelasLesSiswa.getMatapelajaran(), grupKategoriItemPenilaianSiswa,
													tot, verif, smt);

											idex++;
										}

										String formula = grupKategoriItemPenilaianSiswa.getFormula();
										String target = GrupPenilaianUtil.ambilTarget(formula, sekarang);

										Double tot = kelasLesSiswaPunyaSiswa.retreiveTotalNilai(
												jenisItemPenilaianSiswas, target, kelasLesSiswa.getMatapelajaran(),
												grupPenilaian, grupKategoriItemPenilaianSiswa, smt, hanyaValid);

										kelasLesSiswaPunyaSiswa.populateDetailNilaiTotal(
												kelasLesSiswa.getMatapelajaran(), grupKategoriItemPenilaianSiswa, tot,
												true, smt);

										idex++;
									}

									session.getTransaction().begin();
									Common.refreshUpdate(session, kelasLesSiswaPunyaSiswa);
									session.getTransaction().commit();

								}
							}
						} catch (Exception e) {
							System.out.println("error --> datum=>" + datum);
							Common.tampilErrorJikaAdmin(e);
							try {
								HibernateUtil.rollbackTransaction();
							} catch (Exception ee) { ais.common.ErrorAuditUtil.record(ee, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/DetailPenilaianLesSiswaHelper.java:2696");

							}
						}
					}
					// session.disconnect();
					if (session.isOpen()) {
						session.disconnect();
						session.close();
					}
					HibernateUtil.closeSession();
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/sekolah/helper/DetailPenilaianLesSiswaHelper.java:2709");
				}

				label.setValue("");
							} finally {
					ais.database.hibernate.HibernateUtil.closeSession();
				}
			}
		}).start();
	}

}
