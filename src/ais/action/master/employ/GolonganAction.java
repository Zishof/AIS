package ais.action.master.employ;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Caption;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
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
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.employ.helper.GolonganUtil;
import ais.action.master.helper.RevisiHelper;
import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.dao.DaoFactory;
import ais.database.dao.employ.GolonganDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.ParameterTambahan;
import ais.database.model.employ.GajiPokok;
import ais.database.model.employ.Golongan;
import ais.database.model.employ.Insentif;
import ais.database.model.employ.Makan;
import ais.database.model.employ.Peraturan;
import ais.database.model.employ.SkorGolongan;
import ais.database.model.employ.Transport;
import ais.database.model.employ.UnitGolongan;
import ais.database.model.file.FileFotoLain;
import ais.database.model.file.LampiranLain;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyGroupboxStyled;
import ais.ui.util.MyInclude;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;

/**
 * Controller/action ZK untuk golongan. Tipe ini merupakan titik masuk UI yang menghubungkan event
 * layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code Textbox searchnama}, {@code Textbox searchkode}, {@code Combobox
 * searchunitgolongan}, {@code Checkbox searchaktif}, {@code Textbox nama}; inisialisasi/lifecycle ({@code
 * doBeforeCompose()}, {@code doAfterCompose()}, {@code init()}, {@code init()}, {@code initCriteria()});
 * pembacaan/pencarian ({@code reloadDataFormula()}, {@code reloadFormula()}, {@code onSearchDefault()}); mutasi
 * data ({@code onSave()}); operasi domain lain ({@code onUnit()}, {@code onSkor()}, {@code onKonstanta()},
 * {@code onManajemenParameter()}, {@code onAdd()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau
 * interface yang disebut di atas.</p>
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
public class GolonganAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Textbox searchkode;
	private Combobox searchunitgolongan;
	private Checkbox searchaktif;

	private Textbox nama;
	private Textbox pangkat;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private Golongan golongan;
	private MyToolbarbuttonConfig add;
	private Textbox kode;
	private Combobox unitGolongan;

	private Tabpanel unit;

	public void onUnit(Event event) {
		if (unit.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(unit);
			MyInclude iframe = new MyInclude("/pages/master/employ/unit_golongan.zul");
			iframe.setParent(window);
		}
	}

	private Tabpanel skor;
	private JSONArray array;

	public void onSkor(Event event) {
		if (skor.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(skor);
			MyInclude iframe = new MyInclude("/pages/master/employ/skor_golongan.zul");
			iframe.setParent(window);
		}
	}

	private Tabpanel konstanta;

	public void onKonstanta(Event event) {
		if (konstanta.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(konstanta);
			MyInclude iframe = new MyInclude("/pages/master/konstanta.zul");
			iframe.setParent(window);
		}
	}

	private Tabpanel manajemenParameter;
	private Textbox formulaGajiPokok;
	private Textbox formulaInsentif;
	private Textbox formulaMakan;
	private Textbox formulaTransport;
	private Textbox formulaLain;
	private Label gpLabel;
	private Label insentifLabel;
	private Label makanLabel;
	private Label transportLabel;
	private Label lainLabel;
	private Combobox peraturan;

	public void onManajemenParameter(Event event) {
		if (manajemenParameter.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(manajemenParameter);
			MyInclude iframe = new MyInclude("/pages/master/parameter_tambahan.zul");
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
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		Common.insertComboDanSemua(searchunitgolongan, "nama", "keterangan", UnitGolongan.class,
				Restrictions.eq("aktif", true));

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		MyToolbarbuttonConfig cetakSksDosen = new MyToolbarbuttonConfig("Generate Gaji Pokok", "/img/jadwal.png");
		if (cetakSksDosen != null) { cetakSksDosen.setParent(add.getParent()); }
		cetakSksDosen.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				final MyWindow window = new MyWindow("Pilih Tahun Masa Kerja dan Tanggal Efektif", "none", true);
				window.setParent(page.getFirstRoot());
				window.setHeight("400px");
				window.setWidth("600px");
				window.onModal();

				Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
				borderlayout.setParent(window);

				Center center = new Center();
				center.setParent(borderlayout);

				MyGrid grid = new MyGrid();
				grid.setWidth("100%");
				grid.setParent(center);
				grid.setHeight("100%");

				Columns columns = new Columns();
				columns.setParent(grid);
				MyColumnConfig column = new MyColumnConfig();
				column.setWidth("20%");
				column.setParent(columns);
				column = new MyColumnConfig();
				column.setParent(columns);

				Rows rows = new Rows();
				rows.setParent(grid);

				MyFormRow row = new MyFormRow();
				row.setValign("top");
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Mulai Tahun Masa Kerja"));
				final Intbox mulai;
				row.appendChild(mulai = new Intbox(0));
				mulai.setWidth("90%");

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Sampai Tahun Masa Kerja"));
				final Intbox sampai;
				row.appendChild(sampai = new Intbox(50));
				sampai.setWidth("90%");

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Efektif"));
				final MyDatebox tanggalEfektif;
				row.appendChild(tanggalEfektif = new MyDatebox());

				Common.initKeterangan(rows, "Kosongkan tanggal jika  tanggal efektif tidak berubah");

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

				MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Proses", "/img/save.gif");
				save.setTooltiptext("Proses");
				save.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						window.detach();

						final Label label = Common.displayLoadBar(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								onSearchDefault(arg0);
							}
						});

						new Thread(new Runnable() {

							@SuppressWarnings("unchecked")
							@Override
							public void run() {
								try {

								List<Golongan> golongans = initCriteria(true).list();

								for (Golongan golongan : golongans) {

									Map<String, Double> data = new HashMap<String, Double>();
									Date sekarang = WaktuUtil.getDate();

									try {

										String s = Common.dateFormat1.get().format(sekarang);
										JSONArray jsonArray = new JSONArray(golongan.getFormula());
										JSONObject jsonObjectDicari = null;
										for (int i = 0; i < jsonArray.length(); i++) {
											JSONObject jsonObject = jsonArray.getJSONObject(i);
											if (!jsonObject.isNull("tgl")) {
												Date tgl = Common.dateFormat1.get().parse(jsonObject.get("tgl").toString());
												if (tgl.before(sekarang) || Common.dateFormat1.get().format(tgl).equals(s)) {
													jsonObjectDicari = jsonObject;
													break;
												}
											}
										}

										if (jsonObjectDicari != null) {
											for (Object o : ConstantValues.ambilBerdasarClass(SkorGolongan.class)
													.values()) {
												SkorGolongan skorGolongan = (SkorGolongan) o;
												if (skorGolongan.getAktif()) {
													if (!jsonObjectDicari.isNull(skorGolongan.getKode())) {
														data.put(skorGolongan.getKode(),
																jsonObjectDicari.getDouble(skorGolongan.getKode()));
													} else {
														data.put(skorGolongan.getKode(), 0.0);
													}
												}
											}
										}
									} catch (Exception e) {
										ais.common.Common.tampilErrorJikaAdmin(e);
									}

									Session session = HibernateUtil.currentNativeSession();

									int m = mulai.getValue() == null ? 0 : mulai.getValue();
									int ss = sampai.getValue() == null ? 50 : sampai.getValue();
									int size = ss - m;
									int index = 0;
									for (int i = m; i <= ss; i++) {

										label.setValue("Memproses data " + golongan.getNama() + "  ("
												+ Common.numberFormat.get().format((index * 100.0) / size) + "%)");
										index++;

										try {
											GajiPokok gajiPokok = (GajiPokok) session.createCriteria(GajiPokok.class)
													.add(Restrictions.eq("golongan", golongan))
													.add(Restrictions.or(Restrictions.isNull("peraturan"),
															Restrictions.eq("peraturan", golongan.getPeraturan())))
													.add(Restrictions.eq("masaKerja", i)).setMaxResults(1)
													.uniqueResult();
											if (gajiPokok == null) {
												gajiPokok = new GajiPokok();

											}

											if (tanggalEfektif.getValue() != null) {
												gajiPokok.setTanggalEfektif(tanggalEfektif.getValue());
											}

											gajiPokok.setPeraturan(golongan.getPeraturan());
											gajiPokok.setGolongan(golongan);
											gajiPokok.setMasaKerja(i);

											Insentif insentif = (Insentif) session.createCriteria(Insentif.class)
													.add(Restrictions.eq("golongan", golongan))
													.add(Restrictions.or(Restrictions.isNull("peraturan"),
															Restrictions.eq("peraturan", golongan.getPeraturan())))
													.add(Restrictions.eq("masaKerja", i)).setMaxResults(1)
													.uniqueResult();
											if (insentif == null) {
												insentif = new Insentif();

											}
											if (tanggalEfektif.getValue() != null) {
												insentif.setTanggalEfektif(tanggalEfektif.getValue());
											}

											insentif.setPeraturan(golongan.getPeraturan());
											insentif.setGolongan(golongan);
											insentif.setMasaKerja(i);

											Makan makan = (Makan) session.createCriteria(Makan.class)
													.add(Restrictions.eq("golongan", golongan))
													.add(Restrictions.or(Restrictions.isNull("peraturan"),
															Restrictions.eq("peraturan", golongan.getPeraturan())))
													.add(Restrictions.eq("masaKerja", i)).setMaxResults(1)
													.uniqueResult();
											if (makan == null) {
												makan = new Makan();

											}
											if (tanggalEfektif.getValue() != null) {
												makan.setTanggalEfektif(tanggalEfektif.getValue());
											}

											makan.setPeraturan(golongan.getPeraturan());
											makan.setGolongan(golongan);
											makan.setMasaKerja(i);

											Transport transport = (Transport) session.createCriteria(Transport.class)
													.add(Restrictions.eq("golongan", golongan))
													.add(Restrictions.or(Restrictions.isNull("peraturan"),
															Restrictions.eq("peraturan", golongan.getPeraturan())))
													.add(Restrictions.eq("masaKerja", i)).setMaxResults(1)
													.uniqueResult();
											if (transport == null) {
												transport = new Transport();

											}
											if (tanggalEfektif.getValue() != null) {
												transport.setTanggalEfektif(tanggalEfektif.getValue());
											}

											transport.setPeraturan(golongan.getPeraturan());
											transport.setGolongan(golongan);
											transport.setMasaKerja(i);

											String target = golongan.getFormulaGajiPokok();
											target = target.replaceAll("\\(", " ( ");
											target = target.replaceAll("\\)", " ) ");
											target = target.replaceAll("\\+", " + ");
											target = target.replaceAll("\\-", " - ");
											target = target.replaceAll("\\*", " * ");
											target = target.replaceAll("/", " / ");
											target = target.replaceAll("%", " % ");
											target = " " + target + " ";
											target = org.apache.commons.lang3.StringUtils.replace(target, " MK ", " " + i + " ");
											Double gp = GolonganUtil.hitung(data, target, sekarang, 0);
											gajiPokok.setGaji(gp);

											target = golongan.getFormulaInsentif();
											target = target.replaceAll("\\(", " ( ");
											target = target.replaceAll("\\)", " ) ");
											target = target.replaceAll("\\+", " + ");
											target = target.replaceAll("\\-", " - ");
											target = target.replaceAll("\\*", " * ");
											target = target.replaceAll("/", " / ");
											target = target.replaceAll("%", " % ");
											target = " " + target + " ";
											target = org.apache.commons.lang3.StringUtils.replace(target, " MK ", " " + i + " ");
											Double in = GolonganUtil.hitung(data, target, sekarang, 0);
											insentif.setInsentif(in);

											target = golongan.getFormulaMakan();
											target = target.replaceAll("\\(", " ( ");
											target = target.replaceAll("\\)", " ) ");
											target = target.replaceAll("\\+", " + ");
											target = target.replaceAll("\\-", " - ");
											target = target.replaceAll("\\*", " * ");
											target = target.replaceAll("/", " / ");
											target = target.replaceAll("%", " % ");
											target = " " + target + " ";
											target = org.apache.commons.lang3.StringUtils.replace(target, " MK ", " " + i + " ");
											Double mak = GolonganUtil.hitung(data, target, sekarang, 0);
											makan.setMakan(mak);

											target = golongan.getFormulaTransport();
											target = target.replaceAll("\\(", " ( ");
											target = target.replaceAll("\\)", " ) ");
											target = target.replaceAll("\\+", " + ");
											target = target.replaceAll("\\-", " - ");
											target = target.replaceAll("\\*", " * ");
											target = target.replaceAll("/", " / ");
											target = target.replaceAll("%", " % ");
											target = " " + target + " ";
											target = org.apache.commons.lang3.StringUtils.replace(target, " MK ", " " + i + " ");
											Double trns = GolonganUtil.hitung(data, target, sekarang, 0);
											transport.setTransport(trns);

											target = golongan.getFormulaLain();
											target = target.replaceAll("\\(", " ( ");
											target = target.replaceAll("\\)", " ) ");
											target = target.replaceAll("\\+", " + ");
											target = target.replaceAll("\\-", " - ");
											target = target.replaceAll("\\*", " * ");
											target = target.replaceAll("/", " / ");
											target = target.replaceAll("%", " % ");
											target = " " + target + " ";
											target = org.apache.commons.lang3.StringUtils.replace(target, " MK ", " " + i + " ");
											Double lain = GolonganUtil.hitung(data, target, sekarang, 0);
											gajiPokok.setLain(lain);

											session.getTransaction().begin();
											Common.refreshSaveOrUpdate(session, gajiPokok);
											Common.refreshSaveOrUpdate(session, insentif);
											Common.refreshSaveOrUpdate(session, makan);
											Common.refreshSaveOrUpdate(session, transport);
											session.getTransaction().commit();
										} catch (Exception e) {
											ais.common.Common.tampilErrorJikaAdmin(e);
										}
									}
								}
								HibernateUtil.closeSession();
								label.setValue("");
															} finally {
									ais.database.hibernate.HibernateUtil.closeSession();
								}
							}
						}).start();
					}
				});
				save.setParent(toolbar);
			}

		});

		String[] contents = new String[] { "id", "kode", "nama", "peraturan", "unitGolongan", "pangkat", "formula",
				"formulaGajiPokok", "formulaInsentif", "formulaMakan", "formulaTransport", "formulaLain", "keterangan",
				"aktif" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(Golongan.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);
//
//		MyToolbarbuttonConfig upload = Common.uploadData(this, Golongan.class, contents);
//		upload.setVisible(add.isVisible() && edit && delete);
//		add.getParent().appendChild(upload);
	}

	private List<Map<String, Object>> maps = new ArrayList<Map<String, Object>>();

	class GolonganRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Golongan golongan = (Golongan) arg1;

			final MyDetail detail = new MyDetail();
			detail.setParent(arg0);
			detail.addEventListener("onOpen", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Common.clear(detail);
					if (detail.isOpen()) {
						Map<String, Double> data = new HashMap<String, Double>();
						Date sekarang = WaktuUtil.getDate();

						String s = Common.dateFormat1.get().format(sekarang);
						JSONArray jsonArray = new JSONArray(golongan.getFormula());
						JSONObject jsonObjectDicari = null;
						for (int i = 0; i < jsonArray.length(); i++) {
							JSONObject jsonObject = jsonArray.getJSONObject(i);
							if (!jsonObject.isNull("tgl")) {
								Date tgl = Common.dateFormat1.get().parse(jsonObject.get("tgl").toString());
								if (tgl.before(sekarang) || Common.dateFormat1.get().format(tgl).equals(s)) {
									jsonObjectDicari = jsonObject;
									break;
								}
							}
						}

						if (jsonObjectDicari != null) {
							for (Object o : ConstantValues.ambilBerdasarClass(SkorGolongan.class).values()) {
								try {
									SkorGolongan skorGolongan = (SkorGolongan) o;
									if (skorGolongan.getAktif()) {
										if (!jsonObjectDicari.isNull(skorGolongan.getKode())) {
											data.put(skorGolongan.getKode(),
													jsonObjectDicari.getDouble(skorGolongan.getKode()));
										} else {
											data.put(skorGolongan.getKode(), 0.0);
										}
									}
								} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/employ/GolonganAction.java:586");
									// TODO: handle exception
								}
							}
						}

						MyGroupboxStyled groupboxStyled = new MyGroupboxStyled();
						groupboxStyled.setParent(detail);
						groupboxStyled.appendChild(
								new Caption("Simulasi penghitungan gaji pokok selama 40 tahun masa kerja"));

						Toolbar toolbar = new Toolbar();
						toolbar.setParent(groupboxStyled);

						MyToolbarbuttonConfig cetakSksDosen = new MyToolbarbuttonConfig("Cetak Draf Gaji Pokok",
								"/img/svg/printer.svg");
						cetakSksDosen.setParent(toolbar);
						cetakSksDosen.addEventListener("onClick", new EventListener() {

							@SuppressWarnings({ "unchecked", "rawtypes" })
							@Override
							public void onEvent(Event arg0) throws Exception {
								try {
									Map parameters = ais.common.HashMapGenerator.getRand();
									parameters.put("golongan", golongan.getNama());
									parameters.put("maps", maps);
									File file = Report.generateFileReport(Report.PDF, parameters,
											"employ/Draft_Gaji_Pokok", ais.ui.util.WaktuUtil.getDate(), new Toolbar());

									MyWindow window = new MyWindow("Draf Gaji Pokok", "none", true);
									window.setParent(page.getFirstRoot());
									window.setHeight("95%");
									window.setWidth("95%");
									window.onModal();
									Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
									borderlayout.setParent(window);

									Center center = new Center();
									center.setParent(borderlayout);
									ais.ui.util.ZkCompat.setFlex(center, true);

									CommonReport.tampilkanReportPDF(center, file);

								} catch (Exception e) {
									Common.tampilErrorJikaAdmin(e);
								}
							}

						});

						cetakSksDosen = new MyToolbarbuttonConfig("Generate Gaji Pokok", "/img/jadwal.png");
						cetakSksDosen.setParent(toolbar);
						cetakSksDosen.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {

								final MyWindow window = new MyWindow("Pilih Tahun Masa Kerja dan Tanggal Efektif",
										"none", true);
								window.setParent(page.getFirstRoot());
								window.setHeight("400px");
								window.setWidth("600px");
								window.onModal();

								Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
								borderlayout.setParent(window);

								Center center = new Center();
								center.setParent(borderlayout);

								MyGrid grid = new MyGrid();
								grid.setWidth("100%");
								grid.setParent(center);
								grid.setHeight("100%");

								Columns columns = new Columns();
								columns.setParent(grid);
								MyColumnConfig column = new MyColumnConfig();
								column.setWidth("20%");
								column.setParent(columns);
								column = new MyColumnConfig();
								column.setParent(columns);

								Rows rows = new Rows();
								rows.setParent(grid);

								MyFormRow row = new MyFormRow();
								row.setValign("top");
								row.setParent(rows);
								row.appendChild(new ais.ui.util.MyLabelConfig("Mulai Tahun Masa Kerja"));
								final Intbox mulai;
								row.appendChild(mulai = new Intbox(0));
								mulai.setWidth("90%");

								row = new MyFormRow();
								row.setParent(rows);
								row.appendChild(new ais.ui.util.MyLabelConfig("Sampai Tahun Masa Kerja"));
								final Intbox sampai;
								row.appendChild(sampai = new Intbox(50));
								sampai.setWidth("90%");

								row = new MyFormRow();
								row.setParent(rows);
								row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Efektif"));
								final MyDatebox tanggalEfektif;
								row.appendChild(tanggalEfektif = new MyDatebox());

								Common.initKeterangan(rows, "Kosongkan tanggal jika  tanggal efektif tidak berubah");

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

								MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Proses", "/img/save.gif");
								save.setTooltiptext("Proses");
								save.addEventListener("onClick", new EventListener() {
									@Override
									public void onEvent(Event event) throws Exception {
										window.detach();

										final Label label = Common.displayLoadBar(new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {
												onSearchDefault(arg0);
											}
										});

										new Thread(new Runnable() {

											@Override
											public void run() {
												try {

												Map<String, Double> data = new HashMap<String, Double>();
												Date sekarang = WaktuUtil.getDate();

												try {

													String s = Common.dateFormat1.get().format(sekarang);
													JSONArray jsonArray = new JSONArray(golongan.getFormula());
													JSONObject jsonObjectDicari = null;
													for (int i = 0; i < jsonArray.length(); i++) {
														JSONObject jsonObject = jsonArray.getJSONObject(i);
														if (!jsonObject.isNull("tgl")) {
															Date tgl = Common.dateFormat1.get()
																	.parse(jsonObject.get("tgl").toString());
															if (tgl.before(sekarang)
																	|| Common.dateFormat1.get().format(tgl).equals(s)) {
																jsonObjectDicari = jsonObject;
																break;
															}
														}
													}

													if (jsonObjectDicari != null) {
														for (Object o : ConstantValues
																.ambilBerdasarClass(SkorGolongan.class).values()) {
															SkorGolongan skorGolongan = (SkorGolongan) o;
															if (skorGolongan.getAktif()) {
																if (!jsonObjectDicari.isNull(skorGolongan.getKode())) {
																	data.put(skorGolongan.getKode(), jsonObjectDicari
																			.getDouble(skorGolongan.getKode()));
																} else {
																	data.put(skorGolongan.getKode(), 0.0);
																}
															}
														}
													}
												} catch (Exception e) {
													ais.common.Common.tampilErrorJikaAdmin(e);
												}

												Session session = HibernateUtil.currentNativeSession();

												int m = mulai.getValue() == null ? 0 : mulai.getValue();
												int ss = sampai.getValue() == null ? 50 : sampai.getValue();
												int size = ss - m;
												int index = 0;
												for (int i = m; i <= ss; i++) {

													label.setValue("Memproses data  ("
															+ Common.numberFormat.get().format((index * 100.0) / size)
															+ "%)");
													index++;

													try {
														GajiPokok gajiPokok = (GajiPokok) session
																.createCriteria(GajiPokok.class)
																.add(Restrictions.eq("golongan", golongan))
																.add(Restrictions.or(Restrictions.isNull("peraturan"),
																		Restrictions.eq("peraturan",
																				golongan.getPeraturan())))
																.add(Restrictions.eq("masaKerja", i)).setMaxResults(1)
																.uniqueResult();
														if (gajiPokok == null) {
															gajiPokok = new GajiPokok();

														}
														if (tanggalEfektif.getValue() != null) {
															gajiPokok.setTanggalEfektif(tanggalEfektif.getValue());
														}

														gajiPokok.setPeraturan(golongan.getPeraturan());
														gajiPokok.setGolongan(golongan);
														gajiPokok.setMasaKerja(i);

														Insentif insentif = (Insentif) session
																.createCriteria(Insentif.class)
																.add(Restrictions.eq("golongan", golongan))
																.add(Restrictions.or(Restrictions.isNull("peraturan"),
																		Restrictions.eq("peraturan",
																				golongan.getPeraturan())))
																.add(Restrictions.eq("masaKerja", i)).setMaxResults(1)
																.uniqueResult();
														if (insentif == null) {
															insentif = new Insentif();

														}
														if (tanggalEfektif.getValue() != null) {
															insentif.setTanggalEfektif(tanggalEfektif.getValue());
														}

														insentif.setPeraturan(golongan.getPeraturan());
														insentif.setGolongan(golongan);
														insentif.setMasaKerja(i);

														Makan makan = (Makan) session.createCriteria(Makan.class)
																.add(Restrictions.eq("golongan", golongan))
																.add(Restrictions.or(Restrictions.isNull("peraturan"),
																		Restrictions.eq("peraturan",
																				golongan.getPeraturan())))
																.add(Restrictions.eq("masaKerja", i)).setMaxResults(1)
																.uniqueResult();
														if (makan == null) {
															makan = new Makan();

														}
														if (tanggalEfektif.getValue() != null) {
															makan.setTanggalEfektif(tanggalEfektif.getValue());
														}

														makan.setPeraturan(golongan.getPeraturan());
														makan.setGolongan(golongan);
														makan.setMasaKerja(i);

														Transport transport = (Transport) session
																.createCriteria(Transport.class)
																.add(Restrictions.eq("golongan", golongan))
																.add(Restrictions.or(Restrictions.isNull("peraturan"),
																		Restrictions.eq("peraturan",
																				golongan.getPeraturan())))
																.add(Restrictions.eq("masaKerja", i)).setMaxResults(1)
																.uniqueResult();
														if (transport == null) {
															transport = new Transport();

														}
														if (tanggalEfektif.getValue() != null) {
															transport.setTanggalEfektif(tanggalEfektif.getValue());
														}

														transport.setPeraturan(golongan.getPeraturan());
														transport.setGolongan(golongan);
														transport.setMasaKerja(i);

														String target = golongan.getFormulaGajiPokok();
														target = target.replaceAll("\\(", " ( ");
														target = target.replaceAll("\\)", " ) ");
														target = target.replaceAll("\\+", " + ");
														target = target.replaceAll("\\-", " - ");
														target = target.replaceAll("\\*", " * ");
														target = target.replaceAll("/", " / ");
														target = target.replaceAll("%", " % ");
														target = " " + target + " ";
														target = org.apache.commons.lang3.StringUtils.replace(target, " MK ", " " + i + " ");
														Double gp = GolonganUtil.hitung(data, target, sekarang, 0);
														gajiPokok.setGaji(gp);

														target = golongan.getFormulaInsentif();
														target = target.replaceAll("\\(", " ( ");
														target = target.replaceAll("\\)", " ) ");
														target = target.replaceAll("\\+", " + ");
														target = target.replaceAll("\\-", " - ");
														target = target.replaceAll("\\*", " * ");
														target = target.replaceAll("/", " / ");
														target = target.replaceAll("%", " % ");
														target = " " + target + " ";
														target = org.apache.commons.lang3.StringUtils.replace(target, " MK ", " " + i + " ");
														Double in = GolonganUtil.hitung(data, target, sekarang, 0);
														insentif.setInsentif(in);

														target = golongan.getFormulaMakan();
														target = target.replaceAll("\\(", " ( ");
														target = target.replaceAll("\\)", " ) ");
														target = target.replaceAll("\\+", " + ");
														target = target.replaceAll("\\-", " - ");
														target = target.replaceAll("\\*", " * ");
														target = target.replaceAll("/", " / ");
														target = target.replaceAll("%", " % ");
														target = " " + target + " ";
														target = org.apache.commons.lang3.StringUtils.replace(target, " MK ", " " + i + " ");
														Double mak = GolonganUtil.hitung(data, target, sekarang, 0);
														makan.setMakan(mak);

														target = golongan.getFormulaTransport();
														target = target.replaceAll("\\(", " ( ");
														target = target.replaceAll("\\)", " ) ");
														target = target.replaceAll("\\+", " + ");
														target = target.replaceAll("\\-", " - ");
														target = target.replaceAll("\\*", " * ");
														target = target.replaceAll("/", " / ");
														target = target.replaceAll("%", " % ");
														target = " " + target + " ";
														target = org.apache.commons.lang3.StringUtils.replace(target, " MK ", " " + i + " ");
														Double trns = GolonganUtil.hitung(data, target, sekarang, 0);
														transport.setTransport(trns);

														target = golongan.getFormulaLain();
														target = target.replaceAll("\\(", " ( ");
														target = target.replaceAll("\\)", " ) ");
														target = target.replaceAll("\\+", " + ");
														target = target.replaceAll("\\-", " - ");
														target = target.replaceAll("\\*", " * ");
														target = target.replaceAll("/", " / ");
														target = target.replaceAll("%", " % ");
														target = " " + target + " ";
														target = org.apache.commons.lang3.StringUtils.replace(target, " MK ", " " + i + " ");
														Double lain = GolonganUtil.hitung(data, target, sekarang, 0);
														gajiPokok.setLain(lain);

														session.getTransaction().begin();
														Common.refreshSaveOrUpdate(session, gajiPokok);
														Common.refreshSaveOrUpdate(session, insentif);
														Common.refreshSaveOrUpdate(session, makan);
														Common.refreshSaveOrUpdate(session, transport);

														session.getTransaction().commit();
													} catch (Exception e) {
														ais.common.Common.tampilErrorJikaAdmin(e);
													}
												}

												HibernateUtil.closeSession();
												label.setValue("");
																							} finally {
													ais.database.hibernate.HibernateUtil.closeSession();
												}
											}
										}).start();
									}
								});
								save.setParent(toolbar);
							}

						});

						Grid grid = new Grid();
						grid.setSclass("dgrid");
						grid.setParent(groupboxStyled);
						grid.setWidth("100%");
						grid.setHeight("100%");

						Columns columns = new Columns();
						columns.setParent(grid);

						MyColumnConfig column = new MyColumnConfig("Masa Kerja (MK)");
						column.setParent(columns);
						column.setWidth("10%");

						column = new MyColumnConfig("Gaji Pokok (GAPOK)");
						column.setParent(columns);
						column.setWidth("15%");
						column.setAlign("right");

						column = new MyColumnConfig("Insentif (INSENTIF)");
						column.setParent(columns);
						column.setWidth("15%");
						column.setAlign("right");

						column = new MyColumnConfig("Makan (MAKAN)");
						column.setParent(columns);
						column.setWidth("15%");
						column.setAlign("right");

						column = new MyColumnConfig("Transport (TRANSPORT)");
						column.setParent(columns);
						column.setWidth("15%");
						column.setAlign("right");

						column = new MyColumnConfig("Lain-lain (LAIN_LAIN)");
						column.setParent(columns);
						column.setWidth("15%");
						column.setAlign("right");

						column = new MyColumnConfig("Total");
						column.setParent(columns);
						column.setWidth("15%");
						column.setAlign("right");

						Rows rows = new Rows();
						rows.setParent(grid);
						maps.clear();
						for (int i = 0; i <= 40; i++) {
							MyFormRow row = new MyFormRow();
							row.setValign("top");
							row.setParent(rows);
							row.appendChild(new Label(i + " tahun"));

							String target = golongan.getFormulaGajiPokok();
							target = target.replaceAll("\\(", " ( ");
							target = target.replaceAll("\\)", " ) ");
							target = target.replaceAll("\\+", " + ");
							target = target.replaceAll("\\-", " - ");
							target = target.replaceAll("\\*", " * ");
							target = target.replaceAll("/", " / ");
							target = target.replaceAll("%", " % ");
							target = " " + target + " ";
							target = org.apache.commons.lang3.StringUtils.replace(target, " MK ", " " + i + " ");
							Double gp = GolonganUtil.hitung(data, target, sekarang, 0);
							row.appendChild(new Label(Common.numberFormat.get().format(gp)));

							target = golongan.getFormulaInsentif();
							target = target.replaceAll("\\(", " ( ");
							target = target.replaceAll("\\)", " ) ");
							target = target.replaceAll("\\+", " + ");
							target = target.replaceAll("\\-", " - ");
							target = target.replaceAll("\\*", " * ");
							target = target.replaceAll("/", " / ");
							target = target.replaceAll("%", " % ");
							target = " " + target + " ";
							target = org.apache.commons.lang3.StringUtils.replace(target, " MK ", " " + i + " ");
							Double in = GolonganUtil.hitung(data, target, sekarang, 0);
							row.appendChild(new Label(Common.numberFormat.get().format(in)));

							target = golongan.getFormulaMakan();
							target = target.replaceAll("\\(", " ( ");
							target = target.replaceAll("\\)", " ) ");
							target = target.replaceAll("\\+", " + ");
							target = target.replaceAll("\\-", " - ");
							target = target.replaceAll("\\*", " * ");
							target = target.replaceAll("/", " / ");
							target = target.replaceAll("%", " % ");
							target = " " + target + " ";
							target = org.apache.commons.lang3.StringUtils.replace(target, " MK ", " " + i + " ");
							Double mak = GolonganUtil.hitung(data, target, sekarang, 0);
							row.appendChild(new Label(Common.numberFormat.get().format(mak)));

							target = golongan.getFormulaTransport();
							target = target.replaceAll("\\(", " ( ");
							target = target.replaceAll("\\)", " ) ");
							target = target.replaceAll("\\+", " + ");
							target = target.replaceAll("\\-", " - ");
							target = target.replaceAll("\\*", " * ");
							target = target.replaceAll("/", " / ");
							target = target.replaceAll("%", " % ");
							target = " " + target + " ";
							target = org.apache.commons.lang3.StringUtils.replace(target, " MK ", " " + i + " ");
							Double trns = GolonganUtil.hitung(data, target, sekarang, 0);
							row.appendChild(new Label(Common.numberFormat.get().format(trns)));

							target = golongan.getFormulaLain();
							target = target.replaceAll("\\(", " ( ");
							target = target.replaceAll("\\)", " ) ");
							target = target.replaceAll("\\+", " + ");
							target = target.replaceAll("\\-", " - ");
							target = target.replaceAll("\\*", " * ");
							target = target.replaceAll("/", " / ");
							target = target.replaceAll("%", " % ");
							target = " " + target + " ";
							target = org.apache.commons.lang3.StringUtils.replace(target, " MK ", " " + i + " ");
							Double lain = GolonganUtil.hitung(data, target, sekarang, 0);
							row.appendChild(new Label(Common.numberFormat.get().format(lain)));

							Double total = gp + in + mak + trns + lain;
							row.appendChild(new Label(Common.numberFormat.get().format(total)));

							Map<String, Object> map = new java.util.HashMap<String, Object>();
							map.put("gp", gp);
							map.put("in", in);
							map.put("mak", mak);
							map.put("trns", trns);
							map.put("lain", lain);
							map.put("total", total);
							map.put("kode", i + " tahun");
							maps.add(map);
						}
					}
				}
			});

			new Label(golongan.getKode()).setParent(arg0);
			RevisiHelper.createNewRevisi(Golongan.class, golongan, golongan.getNama()).setParent(arg0);

			Vbox a = new Vbox();
			a.setParent(arg0);
			new Label(golongan.getPeraturan() == null ? "" : golongan.getPeraturan().getNama()).setParent(a);
			if (golongan.getPeraturan() != null) {
				Vbox myvbox = new Vbox();
				myvbox.setParent(a);

				Hbox hbox = new Hbox();
				hbox.setParent(myvbox);
				LampiranLain.createDownloadUploadFileLain(hbox, golongan.getPeraturan().getId(),
						Peraturan.class.getName(), "Peraturan Dokumen", false, null, null, false, false, false, false);
			}

			new Label(golongan.getUnitGolongan() == null ? "" : golongan.getUnitGolongan().getNama()).setParent(arg0);

			new Html(GolonganUtil.ambilDeskripsi(golongan.getFormula())).setParent(arg0);

			new Label(Common.numberFormat.get().format(GolonganUtil.ambilPoint(golongan.getFormula(), WaktuUtil.getDate())))
					.setParent(arg0);

			new Label(golongan.getKeterangan()).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(golongan.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					golongan.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(golongan);
				}
			});

			Common.copyEditDeleteButtons(edit, delete, golongan, GolonganAction.this).setParent(arg0);
		}

	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		golongan = (Golongan) obj;
		init(golongan);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	public void onAdd(Event event) throws Exception {
		init(new Golongan());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@SuppressWarnings("deprecation")
	private void init(final Golongan golongan) throws Exception {
		this.golongan = golongan;
		addWindow.setTitle(golongan.getId() == null ? "Tambah Golongan" : "Ubah Golongan");
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

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Peraturan *"));
		row.appendChild(peraturan = new Combobox());
		Common.insertCombo(peraturan, "nama", Peraturan.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.selectComboItem(peraturan, golongan.getPeraturan());
		peraturan.setWidth("90%");
		peraturan.setReadonly(true);

		final MyFormRow rowFile = new MyFormRow();

		rowFile.setParent(rows);

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(rowFile);
				rowFile.appendChild(new ais.ui.util.MyLabelConfig("Lampiran Dokumen Peraturan"));
				rowFile.setVisible(false);
				Peraturan jp = (Peraturan) (peraturan.getSelectedItem() == null ? null
						: peraturan.getSelectedItem().getValue());
				if (jp != null) {

					FileFotoLain fileFotoLain = FileFotoLain.ambil(false, jp.getId(), Peraturan.class.getName(),
							LampiranLain.class);

					rowFile.setVisible(fileFotoLain != null);
					Vbox myvbox = new Vbox();
					myvbox.setParent(rowFile);

					Hbox hbox = new Hbox();
					hbox.setParent(myvbox);
					LampiranLain.createDownloadUploadFileLain(hbox, jp.getId(), Peraturan.class.getName(),
							"Peraturan Dokumen", false, null, null, false, false, false, false);
				}
			}
		};
		peraturan.addEventListener("onChange", eventListener);
		eventListener.onEvent(null);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Golongan *"));
		row.appendChild(kode = new Textbox(golongan.getKode()));
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Golongan *"));
		row.appendChild(nama = new Textbox(golongan.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Unit Golongan"));
		row.appendChild(unitGolongan = new Combobox());
		Common.insertComboDanSemua(unitGolongan, new String[] { "kode", "nama" }, "keterangan", UnitGolongan.class,
				"=Tanpa Unit Golongan=", Restrictions.eq("aktif", true));
		Common.selectComboItem(unitGolongan, golongan.getUnitGolongan());
		unitGolongan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pangkat"));
		row.appendChild(pangkat = new Textbox(golongan.getPangkat() == null ? "" : golongan.getPangkat()));
		pangkat.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(golongan.getKeterangan() == null ? "" : golongan.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		row = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Formula"));

		row = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.setParent(rows);
		array = new JSONArray(golongan.getFormula());
		Row rowFormula = Common.tampilanScroll1(row);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Formula Gaji Pokok"));
		row.appendChild(formulaGajiPokok = new Textbox(golongan.getFormulaGajiPokok()));
		formulaGajiPokok.setWidth("90%");
		formulaGajiPokok.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(gpLabel = new Label());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Formula Insentif"));
		row.appendChild(formulaInsentif = new Textbox(golongan.getFormulaInsentif()));
		formulaInsentif.setWidth("90%");
		formulaInsentif.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(insentifLabel = new Label());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Formula Uang Makan"));
		row.appendChild(formulaMakan = new Textbox(golongan.getFormulaMakan()));
		formulaMakan.setWidth("90%");
		formulaMakan.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(makanLabel = new Label());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Formula Uang Transport"));
		row.appendChild(formulaTransport = new Textbox(golongan.getFormulaTransport()));
		formulaTransport.setWidth("90%");
		formulaTransport.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(transportLabel = new Label());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Formula lain-lain"));
		row.appendChild(formulaLain = new Textbox(golongan.getFormulaLain()));
		formulaLain.setWidth("90%");
		formulaLain.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(lainLabel = new Label());

		EventListener eventListenerUbah = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Date sekarang = WaktuUtil.getDate();
				Map<String, Double> data = new HashMap<String, Double>();
				String s = Common.dateFormat1.get().format(sekarang);

				JSONObject jsonObjectDicari = null;
				for (int i = 0; i < array.length(); i++) {
					JSONObject jsonObject = array.getJSONObject(i);
					if (!jsonObject.isNull("tgl")) {
						Date tgl = Common.dateFormat1.get().parse(jsonObject.get("tgl").toString());
						if (tgl.before(sekarang) || Common.dateFormat1.get().format(tgl).equals(s)) {
							jsonObjectDicari = jsonObject;
							break;
						}
					}
				}

				if (jsonObjectDicari != null) {
					for (Object o : ConstantValues.ambilBerdasarClass(SkorGolongan.class).values()) {
						try {
							SkorGolongan skorGolongan = (SkorGolongan) o;
							if (skorGolongan.getAktif()) {
								if (!jsonObjectDicari.isNull(skorGolongan.getKode())) {
									data.put(skorGolongan.getKode(),
											jsonObjectDicari.getDouble(skorGolongan.getKode()));
								} else {
									data.put(skorGolongan.getKode(), 0.0);
								}
							}
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/employ/GolonganAction.java:1357");
							// TODO: handle exception
						}
					}
				}

				golongan.setFormulaGajiPokok(formulaGajiPokok.getValue().trim());
				golongan.setFormulaInsentif(formulaInsentif.getValue().trim());
				golongan.setFormulaLain(formulaLain.getValue().trim());
				golongan.setFormulaMakan(formulaMakan.getValue().trim());
				golongan.setFormulaTransport(formulaTransport.getValue().trim());

				int i = 1;

				String target = golongan.getFormulaGajiPokok();

				target = target.replaceAll("\\(", " ( ");
				target = target.replaceAll("\\)", " ) ");
				target = target.replaceAll("\\+", " + ");
				target = target.replaceAll("\\-", " - ");
				target = target.replaceAll("\\*", " * ");
				target = target.replaceAll("/", " / ");
				target = target.replaceAll("%", " % ");
				target = " " + target + " ";
				target = org.apache.commons.lang3.StringUtils.replace(target, " MK ", " " + i + " ");
				Double gp = GolonganUtil.hitung(data, target, sekarang, 0);

//				System.out.println("target -> " + target + " data -> " + data + " gp -> " + gp);

				gpLabel.setValue("Simulasi gaji pokok jika masa kerja=1 tahun = " + (Common.numberFormat.get().format(gp)));

				target = golongan.getFormulaInsentif();
				target = target.replaceAll("\\(", " ( ");
				target = target.replaceAll("\\)", " ) ");
				target = target.replaceAll("\\+", " + ");
				target = target.replaceAll("\\-", " - ");
				target = target.replaceAll("\\*", " * ");
				target = target.replaceAll("/", " / ");
				target = target.replaceAll("%", " % ");
				target = " " + target + " ";
				target = org.apache.commons.lang3.StringUtils.replace(target, " MK ", " " + i + " ");
				Double in = GolonganUtil.hitung(data, target, sekarang, 0);
				insentifLabel
						.setValue("Simulasi insentif jika masa kerja=1 tahun = " + (Common.numberFormat.get().format(in)));

				target = golongan.getFormulaMakan();
				target = target.replaceAll("\\(", " ( ");
				target = target.replaceAll("\\)", " ) ");
				target = target.replaceAll("\\+", " + ");
				target = target.replaceAll("\\-", " - ");
				target = target.replaceAll("\\*", " * ");
				target = target.replaceAll("/", " / ");
				target = target.replaceAll("%", " % ");
				target = " " + target + " ";
				target = org.apache.commons.lang3.StringUtils.replace(target, " MK ", " " + i + " ");
				Double mak = GolonganUtil.hitung(data, target, sekarang, 0);
				makanLabel
						.setValue("Simulasi uang makan jika masa kerja=1 tahun = " + (Common.numberFormat.get().format(mak)));

				target = golongan.getFormulaTransport();
				target = target.replaceAll("\\(", " ( ");
				target = target.replaceAll("\\)", " ) ");
				target = target.replaceAll("\\+", " + ");
				target = target.replaceAll("\\-", " - ");
				target = target.replaceAll("\\*", " * ");
				target = target.replaceAll("/", " / ");
				target = target.replaceAll("%", " % ");
				target = " " + target + " ";
				target = org.apache.commons.lang3.StringUtils.replace(target, " MK ", " " + i + " ");
				Double trns = GolonganUtil.hitung(data, target, sekarang, 0);
				transportLabel.setValue(
						"Simulasi uang transport jika masa kerja=1 tahun = " + (Common.numberFormat.get().format(trns)));

				target = golongan.getFormulaLain();
				target = target.replaceAll("\\(", " ( ");
				target = target.replaceAll("\\)", " ) ");
				target = target.replaceAll("\\+", " + ");
				target = target.replaceAll("\\-", " - ");
				target = target.replaceAll("\\*", " * ");
				target = target.replaceAll("/", " / ");
				target = target.replaceAll("%", " % ");
				target = " " + target + " ";
				target = org.apache.commons.lang3.StringUtils.replace(target, " MK ", " " + i + " ");
				Double lain = GolonganUtil.hitung(data, target, sekarang, 0);
				lainLabel.setValue(
						"Simulasi uang lain-lain jika masa kerja=1 tahun = " + (Common.numberFormat.get().format(lain)));

			}
		};

		formulaGajiPokok.addEventListener("onChange", eventListenerUbah);
		formulaInsentif.addEventListener("onChange", eventListenerUbah);
		formulaMakan.addEventListener("onChange", eventListenerUbah);
		formulaTransport.addEventListener("onChange", eventListenerUbah);
		formulaLain.addEventListener("onChange", eventListenerUbah);

		eventListenerUbah.onEvent(null);

		reloadFormula(rowFormula, unitGolongan, array, eventListenerUbah);

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

	public static void reloadDataFormula(final Row rowU, final UnitGolongan u, final JSONArray array,
			final EventListener eventListenerUbah) throws Exception {
		Common.clear(rowU);

		Grid grid = new Grid();
		grid.setSclass("dgrid");
		grid.setWidth("100%");
		grid.setParent(rowU);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig("Tanggal Efektif");
		column.setParent(columns);
		column.setWidth("12%");

		List<SkorGolongan> skorGolongans = new ArrayList<SkorGolongan>();

		if (u == null || u.getSkor().isEmpty()) {
			for (Object o : ConstantValues.ambilBerdasarClass(SkorGolongan.class).values()) {
				SkorGolongan skorGolongan = (SkorGolongan) o;
				if (skorGolongan.getAktif()) {
					skorGolongans.add(skorGolongan);
				}
			}
		} else {
			String[] ss = u.getSkor().split(",");
			for (String s : ss) {
				if (!s.trim().isEmpty()) {
					SkorGolongan skorGolongan = (SkorGolongan) ConstantValues.ambil(SkorGolongan.class.getName(),
							Long.parseLong(s));
					if (skorGolongan != null) {
						skorGolongans.add(skorGolongan);
					}
				}
			}
		}
		Collections.sort(skorGolongans);

		int lebar = skorGolongans.size() == 0 ? 60 : 60 / skorGolongans.size();

		for (SkorGolongan skorGolongan : skorGolongans) {
			column = new MyColumnConfig(skorGolongan.getKode());
			column.setTooltiptext(skorGolongan.getNama());
			column.setParent(columns);
			column.setWidth(lebar + "%");
		}

		column = new MyColumnConfig("Formula");
		column.setParent(columns);
		column.setWidth("20%");

		column = new MyColumnConfig("Nilai");
		column.setAlign("right");
		column.setParent(columns);
		column.setWidth("5%");

		column = new MyColumnConfig();
		column.setParent(columns);
		Rows rows = new Rows();
		rows.setParent(grid);

		for (int i = 0; i < array.length(); i++) {
			final int index = i;
			final JSONObject jsonObject = array.getJSONObject(i);

			if (!jsonObject.isNull("tgl")) {

				Date tgl = new Date();

				String target = "";

				if (!jsonObject.isNull("tgl")) {
					tgl = Common.dateFormat1.get().parse(jsonObject.get("tgl").toString());
				}

				if (!jsonObject.isNull("target")) {
					target = jsonObject.get("target") + "";
				}

				MyFormRow row = new MyFormRow();
				row.setValign("top");
				row.setParent(rows);

				final Label nilai = new Label(Common.numberFormat.get().format(GolonganUtil.ambilPoint(jsonObject)));

				final MyTextbox targetText = new MyTextbox(target);
				final MyDatebox datebox = new MyDatebox(tgl);
				datebox.setWidth("90%");
				row.appendChild(datebox);

				EventListener eventListener = new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						if (arg0 != null && arg0.getTarget() instanceof MyDoublebox
								&& arg0.getTarget().getAttribute("skorGolongan") != null) {
							SkorGolongan skorGolongan = (SkorGolongan) arg0.getTarget().getAttribute("skorGolongan");
							MyDoublebox doublebox = (MyDoublebox) arg0.getTarget();
							jsonObject.put(skorGolongan.getKode(),
									doublebox.getValue() == null ? "" : doublebox.getValue());
						}

						jsonObject.put("tgl",
								datebox.getValue() == null ? "" : Common.dateFormat1.get().format(datebox.getValue()));

						String target = targetText.getValue() == null ? "" : targetText.getValue();
						jsonObject.put("target", target);

						nilai.setValue(Common.numberFormat.get().format(GolonganUtil.ambilPoint(jsonObject)));

						eventListenerUbah.onEvent(arg0);
					}
				};

				for (final SkorGolongan skorGolongan : skorGolongans) {

					String val = "";
					if (!jsonObject.isNull(skorGolongan.getKode())) {
						val = jsonObject.get(skorGolongan.getKode()).toString();
					}

					if (skorGolongan.getParameterTambahan() != null) {
						Component component = ParameterTambahan.ambilComponent(val, skorGolongan.getParameterTambahan(),
								new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										String val = ParameterTambahan.ambilValComponent(arg0.getTarget(),
												skorGolongan.getParameterTambahan());

										try {
											val = val.split(":")[1];
										} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/employ/GolonganAction.java:1625");
											// TODO: handle exception
										}

										jsonObject.put(skorGolongan.getKode(), val);
										jsonObject.put("tgl", datebox.getValue() == null ? ""
												: Common.dateFormat1.get().format(datebox.getValue()));

										String target = targetText.getValue() == null ? "" : targetText.getValue();
										jsonObject.put("target", target);

										System.out.println("jsonObject -> " + jsonObject);

										nilai.setValue(Common.numberFormat.get().format(GolonganUtil.ambilPoint(jsonObject)));
										eventListenerUbah.onEvent(arg0);
									}
								});
						component.setAttribute("skorGolongan", skorGolongan);
						row.appendChild(component);
					} else {
						Double point = 0.0;
						if (!jsonObject.isNull(skorGolongan.getKode())) {
							point = jsonObject.getDouble(skorGolongan.getKode());
						}
						MyDoublebox doublebox = new MyDoublebox(point);
						doublebox.setAttribute("skorGolongan", skorGolongan);
						doublebox.setWidth("85%");
						row.appendChild(doublebox);
						doublebox.addEventListener("onChange", eventListener);
					}
				}

				targetText.setWidth("90%");
				targetText.setRows(3);
				row.appendChild(targetText);

				datebox.addEventListener("onChange", eventListener);
				targetText.addEventListener("onChange", eventListener);

				nilai.setParent(row);

				MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
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
												array.put(index, new JSONObject());

												reloadDataFormula(rowU, u, array, eventListenerUbah);

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
				button.setParent(row);
			}
		}
	}

	public static void reloadFormula(final Row rowFormula, final Combobox unitGolongan, final JSONArray array,
			final EventListener eventListenerUbah) throws Exception {
		final MyFormRow rowU = new MyFormRow();

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Tambah Formula", "/img/svg/addthis.svg");
		button.setTooltiptext("Hapus Data");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				JSONObject jsonObject = new JSONObject();
				jsonObject.put("tgl", Common.dateFormat1.get().format(new Date()));
				jsonObject.put("target", "");
				array.put(jsonObject);

				UnitGolongan u = (UnitGolongan) (unitGolongan.getSelectedItem() == null ? null
						: unitGolongan.getSelectedItem().getValue());

				reloadDataFormula(rowU, u, array, eventListenerUbah);
			}
		});
		button.setParent(rowFormula);

		rowU.setParent(rowFormula.getParent());

		UnitGolongan u = (UnitGolongan) (unitGolongan.getSelectedItem() == null ? null
				: unitGolongan.getSelectedItem().getValue());

		reloadDataFormula(rowU, u, array, eventListenerUbah);

		unitGolongan.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				UnitGolongan u = (UnitGolongan) (unitGolongan.getSelectedItem() == null ? null
						: unitGolongan.getSelectedItem().getValue());
				reloadDataFormula(rowU, u, array, eventListenerUbah);
			}
		});
	}

	public boolean onSave(Event event) throws Exception {
		if (peraturan.getSelectedItem() == null || peraturan.getSelectedItem().getValue() == null) {
			MyMessageboxConfig.show("Mohon maaf, Peraturan belum dipilih. Langkah yang dapat dilakukan: (1) pilih Peraturan dari dropdown pada form; (2) pastikan data peraturan sudah tersedia di master; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (kode.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Kode Golongan belum diisi. Langkah yang dapat dilakukan: (1) isi kolom Kode Golongan pada form; (2) pastikan kode tidak kosong atau hanya spasi; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Nama Golongan belum diisi. Langkah yang dapat dilakukan: (1) isi kolom Nama Golongan pada form; (2) pastikan nama tidak kosong atau hanya spasi; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		GolonganDao golonganDao = DaoFactory.getInstance().getGolonganDao();
		if (golongan.getId() != null) {
			golongan = golonganDao.load(golongan.getId());

		}
		golongan.setPeraturan(
				(Peraturan) (peraturan.getSelectedItem() == null ? null : peraturan.getSelectedItem().getValue()));
		golongan.setFormula(array.toString());
		golongan.setKode(kode.getValue());
		golongan.setNama(nama.getValue());
		golongan.setPangkat(pangkat.getValue());
		golongan.setUnitGolongan((UnitGolongan) (unitGolongan.getSelectedItem() == null ? null
				: unitGolongan.getSelectedItem().getValue()));
		golongan.setKeterangan(keterangan.getValue());

		golongan.setFormulaGajiPokok(formulaGajiPokok.getValue().trim());
		golongan.setFormulaInsentif(formulaInsentif.getValue().trim());
		golongan.setFormulaLain(formulaLain.getValue().trim());
		golongan.setFormulaMakan(formulaMakan.getValue().trim());
		golongan.setFormulaTransport(formulaTransport.getValue().trim());

		if (golongan.getId() != null) {
			golonganDao.update(golongan);
		} else {
			golonganDao.save(golongan);
		}

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(Golongan.class)
				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"));
		if (order)
			criteria.addOrder(Order.asc("nama"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))

				.add(searchkode.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("kode", searchkode.getValue().trim(), MatchMode.ANYWHERE))

				.add(searchunitgolongan.getSelectedItem() == null
						|| searchunitgolongan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("true")
								: Restrictions.eq("unitGolongan", searchunitgolongan.getSelectedItem().getValue()))

		;
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<Golongan> golongan = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(golongan);
		grid.setRowRenderer(new GolonganRenderer());
		grid.setModelCheckMobile(strset);

	}

}
