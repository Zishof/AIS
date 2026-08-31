package ais.action.master.sekolah;


import ais.common.CommonSearchFilterHelper;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;
import org.zkoss.poi.xssf.usermodel.XSSFCell;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Textbox;

import ais.action.master.helper.AmbilDataRuangBanbox;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.sekolah.helper.DetailPenilaianSiswaHelper;
import ais.action.master.sekolah.helper.JadwalUtil;
import ais.action.master.sekolah.util.GrupPenilaianUtil;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Perkuliahan;
import ais.database.model.Ruang;
import ais.database.model.Tbmuser;
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
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Siswa;
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyIntbox;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;
import ais.action.master.helper.FilterLanjutHelper;

/**
 * Controller/action ZK untuk penilaian siswa. Tipe ini merupakan titik masuk UI yang menghubungkan
 * event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code Combobox searchta}, {@code Textbox searchnama}, {@code Textbox
 * searchsiswa}, {@code MyIntbox searchtingkat}, {@code Checkbox searchaktif}; inisialisasi/lifecycle ({@code
 * doBeforeCompose()}, {@code doAfterCompose()}, {@code initCriteria()}); pembacaan/pencarian ({@code
 * onSearchDefault()}); mutasi data ({@code onSave()}); operasi domain lain ({@code onJenisNilai()}). Bagian lain
 * dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 * <p><b>Lifecycle:</b> instance mengikuti lifecycle komponen ZK dan menyimpan state layar; jangan digunakan
 * sebagai singleton atau dibagikan antar desktop/session. Event handler harus tetap memakai konteks pengguna
 * serta session Hibernate milik request yang aktif.</p>
 *
 * @see GenericAutowireComposer
 */
public class PenilaianSiswaAction extends GenericAutowireComposer implements DataCriteria, DataSearchDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Combobox searchta;
	private Textbox searchnama;
	private Textbox searchsiswa;
	private MyIntbox searchtingkat;
	private Checkbox searchaktif;
	private Combobox searchyayasan;
	private Combobox searchsekolah;

	private Textbox nama;
	private AmbilDataRuangBanbox ruang;
	private Combobox sekolah;
	private Intbox tingkat;
	private Textbox keterangan;

//	private boolean edit = false;
//	private boolean delete = false;

	private KelasSiswa kelasSiswa;
	private MyToolbarbuttonConfig add;
	private Combobox yayasan;
	private Combobox tahunAjaran;

	private Tabpanel tabPanelSiswa;

	public void onJenisNilai(Event event) {
		if (tabPanelSiswa.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(tabPanelSiswa);
			MyInclude iframe = new MyInclude("/pages/master/sekolah/jenis_nilai_siswa.zul");
			iframe.setParent(window);
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
		Common.generateTahunAjaran(searchta);

		Common.initYayasanDanSekolahDanSemua(null, null, searchyayasan, searchsekolah);

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

//		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
//		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});



		MyToolbarbuttonConfig singkronDenganMhs = new MyToolbarbuttonConfig("Singkronkan Nilai", "/img/svg/check2.svg");

		singkronDenganMhs.addEventListener("onClick", new EventListener() {

			private File file;

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event arg0) throws Exception {

				Tbmuser tbmuser = Common.getCurrentUser();
				final Guru gur = tbmuser == null ? null : tbmuser.ambilGuru();

				final List<Long> mt;
				if (gur != null) {
					mt = JadwalUtil.ambilJadwal(gur, kelasSiswa);
				} else {
					mt = new ArrayList<Long>();
				}

				final String filename = Sessions.getCurrent().getWebApp().getRealPath("/tmp/data_nilai_"
						+ URLEncoder.encode(Common.datetimeFormat2s.get().format(ais.ui.util.WaktuUtil.getDate()), "UTF-8")
						+ ".xlsx");
				(file = new File(filename)).createNewFile();

				final ais.common.LaporanUpload laporan = new ais.common.LaporanUpload("Sinkronisasi Data Nilai");

				final Label label = Common.displayLoadBar(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						try {
							Filedownload.save(new FileInputStream(file),
									"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
									"data_nilai.xlsx");
						} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

						laporan.selesaikan(new EventListener() {
							@Override
							public void onEvent(Event event) throws Exception {
								onSearchDefault(null);
							}
						});
					}
				});

				new Thread(new Runnable() {

					@Override
					public void run() {
						try {

						XSSFWorkbook workbook = new XSSFWorkbook();

						XSSFSheet sheet = workbook.createSheet("Data Nilai");
						sheet.setDefaultColumnWidth(18);

						XSSFRow rowhead = sheet.createRow((short) 0);

						rowhead.createCell(0).setCellValue("NIS");
						rowhead.createCell(1).setCellValue("Nama Siswa");
						rowhead.createCell(2).setCellValue("Matapelajaran");
						rowhead.createCell(3).setCellValue("Tahun Akademik");
						rowhead.createCell(4).setCellValue("Semester");
						rowhead.createCell(5).setCellValue("Penilaian");
						rowhead.createCell(6).setCellValue("Nilai");
						rowhead.createCell(7).setCellValue("Min");
						rowhead.createCell(8).setCellValue("Max");
						rowhead.createCell(9).setCellValue("Keterangan");

						List<KelasSiswa> kelasSiswas = initCriteria(true).list();
						int size = kelasSiswas.size();
						int index = 0;
						int rowIndex = 0;
						for (KelasSiswa kelasSiswa : kelasSiswas) {

							String kunciKelas = kelasSiswa.getNama();
							try {

								index++;
								Session session = HibernateUtil.currentNativeSession();
								try {

									label.setValue("Singkronkan nilai kelas " + kelasSiswa.getNama() + " ("
											+ Common.numberFormat.get().format((index * 100.0) / size) + "%)");

									if (kelasSiswa.getKurikulumSekolah() != null) {

										List<Long> mk = kelasSiswa.ambilMk();

										List<KelasSiswaPunyaSiswa> siswas = ConstantValues
												.simpleList(
														session.createCriteria(KelasSiswaPunyaSiswa.class)
																.add(Restrictions.or(Restrictions.isNull("aktif"),
																		Restrictions.eq("aktif", true)))
																.add(Restrictions.eq("kelasSiswa", kelasSiswa))
																.createAlias("siswa", "siswa")
																.addOrder(Order.asc("siswa.nama")),
														KelasSiswaPunyaSiswa.class);

										List<JenisPenilaian> jenisPenilaians = ConstantValues.simpleList(
												session.createCriteria(KurikulumPunyaMatapelajaran.class)
														.createAlias("matapelajaran", "matapelajaran")
														.add(Restrictions.or(Restrictions.isNull("aktif"),
																Restrictions.eq("aktif", true)))
														.add(mt.isEmpty()
																? (gur == null ? Restrictions.sqlRestriction("true")
																		: Restrictions.sqlRestriction("false"))
																: Restrictions.in("matapelajaran.id", mt))
														.add(Restrictions.eq("kurikulumSekolah",
																kelasSiswa.getKurikulumSekolah()))
														.setProjection(Projections
																.groupProperty("matapelajaran.jenisPenilaian.id"))
														.addOrder(Order.asc("matapelajaran.jenisPenilaian")),
												JenisPenilaian.class, false);

										for (JenisPenilaian jenisPenilaian : jenisPenilaians) {

											List<KurikulumPunyaMatapelajaran> kurikulumPunyaMatapelajarans = ConstantValues
													.simpleList(
															session.createCriteria(KurikulumPunyaMatapelajaran.class)
																	.add(Restrictions.or(Restrictions.isNull("aktif"),
																			Restrictions.eq("aktif", true)))
																	.add(Restrictions.eq("kurikulumSekolah",
																			kelasSiswa.getKurikulumSekolah()))
																	.createAlias("matapelajaran", "matapelajaran")

																	.add(mk == null || mk.isEmpty()
																			? Restrictions.sqlRestriction("true")
																			: Restrictions.not(Restrictions
																					.in("matapelajaran.id", mk)))

																	.createAlias("matapelajaran.kelompokMatapelajaran",
																			"kelompokMatapelajaran")

																	.addOrder(Order
																			.asc("kelompokMatapelajaran.nomorUrut"))
																	.addOrder(Order.asc("matapelajaran.urutan"))
																	.add(mt.isEmpty() ? (gur == null
																			? Restrictions.sqlRestriction("true")
																			: Restrictions.sqlRestriction("false"))
																			: Restrictions.in("matapelajaran.id", mt))
																	.add(Restrictions.eq("matapelajaran.jenisPenilaian",
																			jenisPenilaian)),
															KurikulumPunyaMatapelajaran.class);

											List<GrupPenilaian> grupPenilaians = ConstantValues.simpleList(
													session.createCriteria(DetailJenisPenilaian.class)
															.add(Restrictions.or(Restrictions.isNull("aktif"),
																	Restrictions.eq("aktif", true)))
															.add(Restrictions.eq("jenisPenilaian", jenisPenilaian))
															.setProjection(
																	Projections.groupProperty("grupPenilaian.id")),
													GrupPenilaian.class, false);
											Collections.sort(grupPenilaians);

											System.out.println(
													"jenisItemPenilaianSiswa -> " + kurikulumPunyaMatapelajarans);

											for (KurikulumPunyaMatapelajaran kurikulumPunyaMatapelajaran : kurikulumPunyaMatapelajarans) {

												label.setValue("Singkronkan nilai kelas " + kelasSiswa.getNama() + " "
														+ kurikulumPunyaMatapelajaran.getMatapelajaran().getNama()
														+ " (" + Common.numberFormat.get().format((index * 100.0) / size)
														+ "%)");

												for (GrupPenilaian grupPenilaian : grupPenilaians) {

													if (grupPenilaian != null && kelasSiswa.getTingkat() > 0
															&& grupPenilaian.getKhususTingkat() != null
															&& !grupPenilaian.getKhususTingkat()
																	.equals(kelasSiswa.getTingkat())) {
														continue;
													}

													List<GrupKategoriItemPenilaianSiswa> grupKategoriItemPenilaianSiswas = ConstantValues
															.simpleList(
																	session.createCriteria(DetailGrupPenilaian.class)
																			.add(Restrictions.or(
																					Restrictions.isNull("aktif"),
																					Restrictions.eq("aktif", true)))
																			.add(Restrictions.isNotNull(
																					"grupKategoriItemPenilaianSiswa"))
																			.setProjection(Projections.groupProperty(
																					"grupKategoriItemPenilaianSiswa.id"))
																			.add(Restrictions.eq("grupPenilaian",
																					grupPenilaian)),
																	GrupKategoriItemPenilaianSiswa.class, false);

													if (!grupKategoriItemPenilaianSiswas.isEmpty()) {

														Collections.sort(grupKategoriItemPenilaianSiswas);
														grupKategoriItemPenilaianSiswas.add(null);

														for (GrupKategoriItemPenilaianSiswa grupKategoriItemPenilaianSiswa : grupKategoriItemPenilaianSiswas) {

															if (grupKategoriItemPenilaianSiswa != null
																	&& kelasSiswa.getTingkat() > 0
																	&& grupKategoriItemPenilaianSiswa
																			.getKhususTingkat() != null
																	&& !grupKategoriItemPenilaianSiswa
																			.getKhususTingkat()
																			.equals(kelasSiswa.getTingkat())) {
																continue;
															}

															if (grupKategoriItemPenilaianSiswa == null) {

																int[] smts = new int[] { 1, 2 };

																if (grupPenilaian != null
																		&& grupPenilaian.getKhususSemester() != null) {
																	smts = new int[] {
																			grupPenilaian.getKhususSemester() };
																}

																Matapelajaran matapelajaran = kurikulumPunyaMatapelajaran
																		.getMatapelajaran();
																for (KelasSiswaPunyaSiswa kelasSiswaPunyaSiswa : siswas) {

																	List<Long> longs = kelasSiswaPunyaSiswa.ambilMk();
																	if (!longs.contains(matapelajaran.getId())) {

																		label.setValue(
																				"Singkronkan total nilai kelas \""
																						+ kelasSiswa.getNama() + "\" \""
																						+ kurikulumPunyaMatapelajaran
																								.getMatapelajaran()
																								.getNama()
																						+ "\" \""
																						+ kelasSiswaPunyaSiswa
																								.getSiswa().getNama()
																						+ "\" ("
																						+ Common.numberFormat.get().format(
																								(index * 100.0) / size)
																						+ "%)");

																		for (int smt : smts) {
																			Double total = 0.0;

																			Double min = 0.0;

																			Double max = 0.0;

																			try {
																				Date sekarang = WaktuUtil.getDate();
																				String formula = grupPenilaian
																						.getFormula();

																				String target = GrupPenilaianUtil
																						.ambilTarget(formula, sekarang);

																				total = kelasSiswaPunyaSiswa
																						.retreiveTotalNilaiTotal(target,
																								kurikulumPunyaMatapelajaran
																										.getMatapelajaran(),
																								grupPenilaian, smt,
																								grupKategoriItemPenilaianSiswas);

																				target = GrupPenilaianUtil
																						.ambilTargetMin(formula,
																								sekarang);

																				min = kelasSiswaPunyaSiswa
																						.retreiveTotalNilaiTotal(target,
																								kurikulumPunyaMatapelajaran
																										.getMatapelajaran(),
																								grupPenilaian, smt,
																								grupKategoriItemPenilaianSiswas);

																				target = GrupPenilaianUtil
																						.ambilTargetMax(formula,
																								sekarang);

																				max = kelasSiswaPunyaSiswa
																						.retreiveTotalNilaiTotal(target,
																								kurikulumPunyaMatapelajaran
																										.getMatapelajaran(),
																								grupPenilaian, smt,
																								grupKategoriItemPenilaianSiswas);

																			} catch (Exception e) {
																				ais.common.Common.tampilErrorJikaAdmin(e);
																			}

																			JSONObject js = new JSONObject();
																			try {
																				js = new JSONObject(smt == 1
																						? kelasSiswaPunyaSiswa
																								.getKeterangan1()
																						: kelasSiswaPunyaSiswa
																								.getKeterangan2());
																			} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

																			String keyKet = kurikulumPunyaMatapelajaran
																					.getMatapelajaran().getId() + "_"
																					+ grupPenilaian.getId();

																			String ket = js.isNull(keyKet) ? ""
																					: js.getString(keyKet);

																			rowIndex++;
																			XSSFRow row = sheet.createRow(rowIndex);

																			XSSFCell cell = row.createCell(0);
																			cell.setCellValue(kelasSiswaPunyaSiswa
																					.getSiswa().getNomorInduk());
																			cell = row.createCell(1);
																			cell.setCellValue(kelasSiswaPunyaSiswa
																					.getSiswa().getNama());

																			cell = row.createCell(2);
																			cell.setCellValue(
																					kurikulumPunyaMatapelajaran
																							.getMatapelajaran()
																							.getNama());

																			cell = row.createCell(3);
																			cell.setCellValue(
																					kelasSiswa.getTahunAjaran());

																			cell = row.createCell(4);
																			cell.setCellValue(smt);

																			cell = row.createCell(5);
																			cell.setCellValue("TOTAL");

																			cell = row.createCell(6);
																			cell.setCellValue(total);

																			cell = row.createCell(7);
																			cell.setCellValue(min);

																			cell = row.createCell(8);
																			cell.setCellValue(max);

																			cell = row.createCell(9);
																			cell.setCellValue(ket);
																		}

																	}
																}

															} else {

																List<KategoriItemPenilaianSiswa> kategoriItemPenilaianSiswasId = ConstantValues
																		.simpleList(session.createCriteria(
																				DetailGrupKategoriItemPenilaianSiswa.class)

																				.add(Restrictions.eq(
																						"grupKategoriItemPenilaianSiswa",
																						grupKategoriItemPenilaianSiswa))
																				.add(Restrictions.or(
																						Restrictions.isNull("aktif"),
																						Restrictions.eq("aktif", true)))

																				.setProjection(
																						Projections.groupProperty(
																								"kategoriItemPenilaianSiswa.id")),
																				KategoriItemPenilaianSiswa.class,
																				false);

																List<JenisItemPenilaianSiswa> jenisItemPenilaianSiswas = ConstantValues
																		.simpleList(session
																				.createCriteria(
																						JenisItemPenilaianSiswa.class)
																				.createAlias(
																						"kategoriItemPenilaianSiswa",
																						"kategoriItemPenilaianSiswa")
																				.addOrder(Order.asc(
																						"kategoriItemPenilaianSiswa.kode"))
																				.addOrder(Order.asc("nomorUrut"))
																				.add(Restrictions.in(
																						"kategoriItemPenilaianSiswa",
																						kategoriItemPenilaianSiswasId))
																				.add(Restrictions.or(
																						Restrictions.isNull("aktif"),
																						Restrictions.eq("aktif",
																								true))),
																				JenisItemPenilaianSiswa.class);

																int[] smts = new int[] { 1, 2 };

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

																Matapelajaran matapelajaran = kurikulumPunyaMatapelajaran
																		.getMatapelajaran();

																for (KelasSiswaPunyaSiswa kelasSiswaPunyaSiswa : siswas) {

																	List<Long> longs = kelasSiswaPunyaSiswa.ambilMk();
																	if (!longs.contains(matapelajaran.getId())) {

																		label.setValue("Singkronkan nilai kelas \""
																				+ kelasSiswa.getNama() + "\" \""
																				+ kurikulumPunyaMatapelajaran
																						.getMatapelajaran().getNama()
																				+ "\" \""
																				+ kelasSiswaPunyaSiswa.getSiswa()
																						.getNama()
																				+ "\" (" + Common.numberFormat.get().format(
																						(index * 100.0) / size)
																				+ "%)");

																		Siswa siswa = kelasSiswaPunyaSiswa.getSiswa();
																		for (int smt : smts) {

																			for (JenisItemPenilaianSiswa jenisItemPenilaianSiswa : jenisItemPenilaianSiswas) {

																				if (jenisItemPenilaianSiswa
																						.getTipeDataInputan()
																						.equals(JenisItemPenilaianSiswa.FORMULA)) {

																					Date sekarang = WaktuUtil.getDate();
																					String formula = jenisItemPenilaianSiswa
																							.getFormula();
																					String target1 = GrupPenilaianUtil
																							.ambilTarget(formula,
																									sekarang);

																					Double total = kelasSiswaPunyaSiswa
																							.retreiveTotalNilai(
																									jenisItemPenilaianSiswas,
																									target1,
																									kurikulumPunyaMatapelajaran
																											.getMatapelajaran(),
																									grupPenilaian,
																									grupKategoriItemPenilaianSiswa,
																									smt, null);

																					kelasSiswaPunyaSiswa
																							.populateDetailNilai(
																									jenisItemPenilaianSiswa,
																									kurikulumPunyaMatapelajaran
																											.getMatapelajaran(),
																									grupKategoriItemPenilaianSiswa,
																									total + "", true,
																									smt);

																					formula = grupKategoriItemPenilaianSiswa
																							.getFormula();
																					String target = GrupPenilaianUtil
																							.ambilTarget(formula,
																									sekarang);

																					Double tot = kelasSiswaPunyaSiswa
																							.retreiveTotalNilai(
																									jenisItemPenilaianSiswas,
																									target,
																									kurikulumPunyaMatapelajaran
																											.getMatapelajaran(),
																									grupPenilaian,
																									grupKategoriItemPenilaianSiswa,
																									smt, null);

																					kelasSiswaPunyaSiswa
																							.populateDetailNilaiTotal(
																									kurikulumPunyaMatapelajaran
																											.getMatapelajaran(),
																									grupKategoriItemPenilaianSiswa,
																									tot, true, smt);

																					session.getTransaction().begin();
																					Common.refreshUpdate(session,
																							kelasSiswaPunyaSiswa);
																					session.getTransaction().commit();

																					rowIndex++;
																					XSSFRow row = sheet
																							.createRow(rowIndex);

																					XSSFCell cell = row.createCell(0);
																					cell.setCellValue(
																							kelasSiswaPunyaSiswa
																									.getSiswa()
																									.getNomorInduk());
																					cell = row.createCell(1);
																					cell.setCellValue(
																							kelasSiswaPunyaSiswa
																									.getSiswa()
																									.getNama());

																					cell = row.createCell(2);
																					cell.setCellValue(
																							kurikulumPunyaMatapelajaran
																									.getMatapelajaran()
																									.getNama());

																					cell = row.createCell(3);
																					cell.setCellValue(kelasSiswa
																							.getTahunAjaran());

																					cell = row.createCell(4);
																					cell.setCellValue(smt);

																					cell = row.createCell(5);
																					cell.setCellValue(
																							jenisItemPenilaianSiswa
																									.getNama());

																					cell = row.createCell(6);
																					cell.setCellValue(total);

																					cell = row.createCell(7);
																					cell.setCellValue(tot);

																					cell = row.createCell(8);
																					cell.setCellValue(target1);

																					cell = row.createCell(9);
																					cell.setCellValue(target);

																				}
																			}

																			Double total = 0.0;

																			try {
																				Date sekarang = WaktuUtil.getDate();
																				String formula = grupKategoriItemPenilaianSiswa
																						.getFormula();
																				String target = GrupPenilaianUtil
																						.ambilTarget(formula, sekarang);
																				total = kelasSiswaPunyaSiswa
																						.retreiveTotalNilai(
																								jenisItemPenilaianSiswas,
																								target,
																								kurikulumPunyaMatapelajaran
																										.getMatapelajaran(),
																								grupPenilaian,
																								grupKategoriItemPenilaianSiswa,
																								smt, null);
																			} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

																			NilaiHurufSekolah nilaiHurufSekolah = NilaiHurufSekolah
																					.getNilaiHurufSekolah(total,
																							siswa.getTahunMasuk(),
																							siswa.getSekolah(),
																							siswa.getYayasan(),
																							kelasSiswaPunyaSiswa
																									.getKelasSiswa()
																									.getTahunAjaran(),
																							smt % 2 == 0
																									? Perkuliahan.GENAP
																									: Perkuliahan.GANJIL,
																							grupPenilaian
																									.getJenisNilaiHuruf());

																			rowIndex++;
																			XSSFRow row = sheet.createRow(rowIndex);

																			XSSFCell cell = row.createCell(0);
																			cell.setCellValue(kelasSiswaPunyaSiswa
																					.getSiswa().getNomorInduk());
																			cell = row.createCell(1);
																			cell.setCellValue(kelasSiswaPunyaSiswa
																					.getSiswa().getNama());

																			cell = row.createCell(2);
																			cell.setCellValue(
																					kurikulumPunyaMatapelajaran
																							.getMatapelajaran()
																							.getNama());

																			cell = row.createCell(3);
																			cell.setCellValue(
																					kelasSiswa.getTahunAjaran());

																			cell = row.createCell(4);
																			cell.setCellValue(smt);

																			cell = row.createCell(5);
																			cell.setCellValue(
																					grupKategoriItemPenilaianSiswa
																							.getNama());

																			cell = row.createCell(6);
																			cell.setCellValue(total);

																			cell = row.createCell(7);
																			cell.setCellValue("");

																			cell = row.createCell(8);
																			cell.setCellValue("");

																			cell = row.createCell(9);
																			cell.setCellValue(nilaiHurufSekolah == null
																					? ""
																					: nilaiHurufSekolah.getNama());

																		}
																	}
																}

															}

														}
													}

												}

											}
										}

									}

								} catch (Exception e) {
									ais.common.Common.tampilErrorJikaAdmin(e);
								}

								// session.disconnect();
								if (session.isOpen()) {session.disconnect();session.close();}
								HibernateUtil.closeSession();

								try {
									FileOutputStream fileOut = new FileOutputStream(filename);
									workbook.write(fileOut);
									fileOut.close();
								} catch (IOException e) {
									// TODO Auto-generated catch block
									Common.tampilErrorJikaAdmin(e);
								}
								laporan.catatBerhasil(index - 1, kunciKelas, "Sinkronisasi berhasil");
							} catch (Exception e) {
								ais.common.Common.tampilErrorJikaAdmin(e);
								laporan.catatGagalDetail(index - 1, kunciKelas, e);
							}
						}
											} finally {
							label.setValue("");
							ais.database.hibernate.HibernateUtil.closeSession();
						}
					}
				}).start();

			}
		});
		Common.appendKeToolbar(singkronDenganMhs, add, comp);
	        FilterLanjutHelper.setup(comp);
}

	/**
	 * Renderer lokal untuk layar/komponen {@link PenilaianSiswaAction}. Kelas ini menerjemahkan satu item data
	 * menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link PenilaianSiswaAction} dan dapat mengakses
	 * state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code DetailPenilaianSiswaHelper
	 * detailPenilaianSiswaHelper}; operasi lokal: {@code render}(). Aturan bisnis bersama tetap berada pada kelas
	 * induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see PenilaianSiswaAction
	 */
	class KelasSiswaRenderer extends ais.ui.util.MyRowRenderer {

		private DetailPenilaianSiswaHelper detailPenilaianSiswaHelper = new DetailPenilaianSiswaHelper();

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final KelasSiswa kelasSiswa = (KelasSiswa) arg1;

			final MyDetail detail = new MyDetail();
			detail.setParent(arg0);
			detail.addEventListener("onOpen", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					if (detail.getChildren().isEmpty() && detail.isOpen()) {
						detailPenilaianSiswaHelper.displayDetailPA(kelasSiswa, detail, addWindow);
					}
				}

			});

			RevisiHelper.createNewRevisi(KelasSiswa.class, kelasSiswa, kelasSiswa.getNama()).setParent(arg0);
			new Label(kelasSiswa.getKurikulumSekolah() == null ? "" : kelasSiswa.getKurikulumSekolah().getNama())
					.setParent(arg0);
			new Label(kelasSiswa.getRuang() == null ? ""
					: kelasSiswa.getRuang().getKodeRuangan() + "-" + kelasSiswa.getRuang().getNama()).setParent(arg0);
			new Label(kelasSiswa.getSekolah() == null ? "" : kelasSiswa.getSekolah().getNama()).setParent(arg0);
			new Label(kelasSiswa.getTingkat().toString()).setParent(arg0);
			new Label(kelasSiswa.getTahunAjaran()).setParent(arg0);
			int count = ((Number) HibernateUtil.currentSession().createCriteria(KelasSiswaPunyaSiswa.class)
					.add(Restrictions.eq("kelasSiswa", kelasSiswa)).setProjection(Projections.rowCount())
					.uniqueResult()).intValue();
			new Label(Common.numberFormat.get().format(count)).setParent(arg0);
			new Label(kelasSiswa.getKeterangan()).setParent(arg0);

		}

	}

	public boolean onSave(Event event) throws Exception {
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Nama Kelas belum diisi. Langkah yang dapat dilakukan: (1) isi kolom Nama Kelas dengan nama kelas yang tepat; (2) pastikan nama tidak kosong atau hanya spasi; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (yayasan.getSelectedItem() == null || yayasan.getSelectedItem().getValue() == null) {
			MyMessageboxConfig.show("Mohon maaf, Yayasan belum dipilih. Langkah yang dapat dilakukan: (1) klik kolom Yayasan dan pilih yayasan yang sesuai; (2) pastikan yayasan terpilih sebelum menyimpan; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (sekolah.getSelectedItem() == null || sekolah.getSelectedItem().getValue() == null) {
			MyMessageboxConfig.show("Mohon maaf, Sekolah belum dipilih. Langkah yang dapat dilakukan: (1) klik kolom Sekolah dan pilih sekolah yang sesuai; (2) pastikan yayasan sudah dipilih agar daftar sekolah tersedia; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (kelasSiswa.getId() != null) {
			kelasSiswa = (KelasSiswa) session.load(KelasSiswa.class, kelasSiswa.getId());

		}
		kelasSiswa.setTahunAjaran((String) tahunAjaran.getSelectedItem().getValue());
		kelasSiswa.setSekolah((Sekolah) sekolah.getSelectedItem().getValue());
		kelasSiswa.setNama(nama.getValue());
		kelasSiswa.setRuang((Ruang) ruang.getAttribute("ruang"));
		kelasSiswa.setTingkat(tingkat.getValue());
		kelasSiswa.setYayasan((Yayasan) yayasan.getSelectedItem().getValue());
		kelasSiswa.setKeterangan(keterangan.getValue());

		Common.refreshSaveOrUpdate(session, kelasSiswa);

		return true;
	}

	@SuppressWarnings("unchecked")
	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();

		List<Long> idsiswas = new ArrayList<Long>();
		if (!searchsiswa.getValue().trim().isEmpty()) {

			idsiswas = session.createCriteria(KelasSiswaPunyaSiswa.class)
					.setProjection(Projections.groupProperty("kelasSiswa.id"))
					.createAlias("siswa", "siswa", Criteria.LEFT_JOIN)
					.createAlias("calonSiswa", "calonSiswa", Criteria.LEFT_JOIN)

					.add(Restrictions.or(
							Restrictions.or(
									Restrictions.ilike("calonSiswa.nomorIndukNasional", searchsiswa.getValue().trim(),
											MatchMode.ANYWHERE),

									Restrictions.or(
											Restrictions.ilike("calonSiswa.namaSiswa", searchsiswa.getValue().trim(),
													MatchMode.ANYWHERE),
											Restrictions.ilike("calonSiswa.nomorInduk", searchsiswa.getValue().trim(),
													MatchMode.ANYWHERE))),

							Restrictions.or(
									Restrictions.ilike("siswa.nomorIndukNasional", searchsiswa.getValue().trim(),
											MatchMode.ANYWHERE),
									Restrictions.or(
											Restrictions.ilike("siswa.nomorIndukSantri", searchsiswa.getValue().trim(),
													MatchMode.ANYWHERE),

											Restrictions.or(
													Restrictions.ilike("siswa.namaSiswa", searchsiswa.getValue().trim(),
															MatchMode.ANYWHERE),
													Restrictions.ilike("siswa.nomorInduk",
															searchsiswa.getValue().trim(), MatchMode.ANYWHERE))))))

					.list();

		}

		System.out.println("idsiswas -> " + idsiswas);

		Criteria criteria = session.createCriteria(KelasSiswa.class)
				.add(!searchsiswa.getValue().trim().isEmpty() && idsiswas.isEmpty()
						? Restrictions.sqlRestriction("false")
						: idsiswas.isEmpty() ? Restrictions.sqlRestriction("true") : Restrictions.in("id", idsiswas))
				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"));

		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser != null && tbmuser.getOrangTua() != null && !tbmuser.getOrangTua().ambilAnakSiswa().isEmpty()) {

			List<Long> kelas = session.createCriteria(KelasSiswaPunyaSiswa.class)
					.setProjection(Projections.property("kelasSiswa.id"))
					.add(Restrictions.in("siswa.id", tbmuser.getOrangTua().ambilAnakSiswa())).list();

			if (!kelas.isEmpty()) {
				criteria.add(Restrictions.in("id", kelas));
			}

		}

		Guru guruD = tbmuser != null ? tbmuser.ambilGuru() : null;

		if (guruD != null) {

			Criterion criterionDsn = Restrictions.eq("guru", guruD);
			criterionDsn = Restrictions.or(criterionDsn, Restrictions.eq("guru2", guruD));
			criterionDsn = Restrictions.or(criterionDsn, Restrictions.eq("guru3", guruD));
			criterionDsn = Restrictions.or(criterionDsn, Restrictions.eq("guru4", guruD));
			criterionDsn = Restrictions.or(criterionDsn, Restrictions.eq("guru5", guruD));
			criterionDsn = Restrictions.or(criterionDsn, Restrictions.eq("guru6", guruD));
			criterionDsn = Restrictions.or(criterionDsn, Restrictions.eq("guru7", guruD));
			criterionDsn = Restrictions.or(criterionDsn, Restrictions.eq("guru8", guruD));
			criterionDsn = Restrictions.or(criterionDsn, Restrictions.eq("guru9", guruD));
			criterionDsn = Restrictions.or(criterionDsn, Restrictions.eq("guru10", guruD));
			criterionDsn = Restrictions.or(criterionDsn, Restrictions.eq("guru11", guruD));
			criterionDsn = Restrictions.or(criterionDsn, Restrictions.eq("guru12", guruD));

			List<Long> kelasSiswas = session.createCriteria(JadwalPelajaran.class)

					.add(searchta.getSelectedItem() == null || searchta.getSelectedItem().getValue() == null
							|| searchta.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
									: Restrictions.eq("tahunAjaran", searchta.getSelectedItem().getValue()))

					.add(criterionDsn).add(Restrictions.isNotNull("kelas"))
					.setProjection(Projections.groupProperty("kelas.id")).list();

			if (!kelasSiswas.isEmpty()) {
				criteria.add(Restrictions.in("id", kelasSiswas));
			}
		}

		if (order)
			criteria.addOrder(Order.asc("nama"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))

				.add((searchtingkat == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchtingkat.getValue() == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("tingkat", searchtingkat.getValue())))

				.add(searchsekolah.getSelectedItem() == null || searchsekolah.getSelectedItem().getValue() == null
						|| searchsekolah.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("sekolah", searchsekolah, false))

				.add(searchta.getSelectedItem() == null || searchta.getSelectedItem().getValue() == null
						|| searchta.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("tahunAjaran", searchta.getSelectedItem().getValue()))

				.add(searchyayasan.getSelectedItem() == null || searchyayasan.getSelectedItem().getValue() == null
						|| searchyayasan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("yayasan", searchyayasan, false));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<KelasSiswa> kelasSiswa = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(kelasSiswa);
		grid.setRowRenderer(new KelasSiswaRenderer());
		grid.setModelCheckMobile(strset);

	}

}
