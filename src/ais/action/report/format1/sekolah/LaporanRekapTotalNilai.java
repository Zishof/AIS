package ais.action.report.format1.sekolah;
import ais.common.PesanFormalHelper;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zss.ui.Spreadsheet;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.West;

import ais.action.master.sekolah.helper.AmbilDataKelasSiswaSemuaBanbox;
import ais.action.master.sekolah.util.GrupPenilaianUtil;
import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Perkuliahan;
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
import ais.database.model.sekolah.JenisNilaiSiswa;
import ais.database.model.sekolah.JenisPenilaian;
import ais.database.model.sekolah.KategoriItemPenilaianSiswa;
import ais.database.model.sekolah.KelasSiswa;
import ais.database.model.sekolah.KelasSiswaPunyaSiswa;
import ais.database.model.sekolah.KurikulumPunyaMatapelajaran;
import ais.database.model.sekolah.Matapelajaran;
import ais.database.model.sekolah.NilaiHurufSekolah;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Siswa;
import ais.ui.util.EcampusUtil;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyLabelStyled;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyVboxStyled;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;

public class LaporanRekapTotalNilai extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3331244819198611604L;

	private Spreadsheet excelku;

	private AmbilDataKelasSiswaSemuaBanbox searchnama = new AmbilDataKelasSiswaSemuaBanbox();

	private Center center;

	private MyToolbarbuttonConfig printAmbil;
	private Set<Long> masukGrup = new HashSet<Long>();
	private Set<Long> masukSubgrup = new HashSet<Long>();

	private Combobox searchsmt;

	private Rows rowsData;

	private MyCheckboxConfig searchtotal;

	private MyCheckboxConfig searchmax;

	private MyCheckboxConfig searchmin;

	private Combobox jenisNilaiSiswa;

	public LaporanRekapTotalNilai() throws Exception {
		super();
		init();
	}

	public LaporanRekapTotalNilai(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);
		init();
	}

	private void init() throws Exception {

		searchsmt = new Combobox();

		Comboitem comboitem = new Comboitem(Perkuliahan.GANJIL);
		comboitem.setValue(1);
		searchsmt.appendChild(comboitem);
		comboitem = new Comboitem(Perkuliahan.GENAP);
		comboitem.setValue(2);
		searchsmt.appendChild(comboitem);
		searchsmt.setCols(2);

		Common.selectComboItem(searchsmt, Common.isNowSemensterGanjil() ? 1 : 2);
		searchsmt.setReadonly(true);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);

		West west = new West();
		west.setTitle("Menu");
		west.setCollapsible(true);
		west.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(west, true);

		west.setWidth("200px");

		Grid grid = new Grid();
		grid.setSclass("dgrid");
		grid.setWidth("100%");
		grid.setParent(west);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		Vbox vbox = new Vbox();
		vbox.setParent(row);
		vbox.setWidth("100%");
		vbox.appendChild(new ais.ui.util.MyLabelConfig("Semester"));
		vbox.appendChild(searchsmt);
		searchsmt.setCols(6);

		row = new MyFormRow();
		row.setParent(rows);
		vbox = new Vbox();
		vbox.setParent(row);
		vbox.setWidth("100%");
		vbox.appendChild(new ais.ui.util.MyLabelConfig("Kelas *"));
		vbox.appendChild(searchnama);
		searchnama.setCols(6);

		row = new MyFormRow();
		row.setParent(rows);
		vbox = new Vbox();
		vbox.setParent(row);
		vbox.setWidth("100%");
		vbox.appendChild(new ais.ui.util.MyLabelConfig(""));
		vbox.appendChild(searchtotal = new MyCheckboxConfig("Tampilkan Total"));
		searchtotal.setChecked(true);

		row = new MyFormRow();
		row.setParent(rows);
		vbox = new Vbox();
		vbox.setParent(row);
		vbox.setWidth("100%");
		vbox.appendChild(new ais.ui.util.MyLabelConfig(""));
		vbox.appendChild(searchmax = new MyCheckboxConfig("Tampilkan Max"));
		searchmax.setChecked(false);

		row = new MyFormRow();
		row.setParent(rows);
		vbox = new Vbox();
		vbox.setParent(row);
		vbox.setWidth("100%");
		vbox.appendChild(new ais.ui.util.MyLabelConfig(""));
		vbox.appendChild(searchmin = new MyCheckboxConfig("Tampilkan Min"));
		searchmin.setChecked(false);

		row = new MyFormRow();
		row.setParent(rows);

		vbox = new Vbox();
		vbox.setParent(row);
		vbox.setWidth("100%");

		vbox.appendChild(new ais.ui.util.MyLabelConfig("Jenis Nilai"));
		vbox.appendChild(jenisNilaiSiswa = new Combobox());
		jenisNilaiSiswa.setWidth("90%");
		jenisNilaiSiswa.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);

		Vbox toolbar = new Vbox();
		toolbar.setParent(row);

		MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Tampilkan", "/img/print.png");
		print.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onCetak(null);
			}
		});
		print.setParent(toolbar);

		printAmbil = new MyToolbarbuttonConfig("Ambil File", "/img/excel.png");
		printAmbil.setVisible(false);
		printAmbil.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				try {
					ByteArrayOutputStream bout = new ByteArrayOutputStream();
					excelku.getBook().write(bout);
					bout.close();
					Filedownload.save(bout.toByteArray(),
							"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "rekap_nilai.xlsx");
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/sekolah/LaporanRekapTotalNilai.java:238");
					PesanFormalHelper.tampilkanGagalException("pembuatan berkas Excel Laporan Rekap Total Nilai", "Sistem mengalami kendala teknis saat menyusun berkas Excel laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap atau format datanya tidak sesuai dengan yang diharapkan oleh template ekspor.", e,
						new String[] {
							"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mengekspor laporan ini.",
							"Pastikan data yang menjadi sumber laporan ini sudah lengkap dan benar, kemudian coba ekspor ulang.",
							"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
						});

				}
			}
		});
		printAmbil.setParent(toolbar);

		row = new MyFormRow();
		row.setParent(rows);

		rowsData = (Rows) Common.tampilanScroll1(row).getParent();

		final EventListener eventListener = new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(rowsData);
				KelasSiswa kelasSiswa = (KelasSiswa) searchnama.getAttribute("kelas");

				if (kelasSiswa != null) {

//					kurikulumSekolah

					Sekolah s = kelasSiswa.getSekolah();
					Common.insertComboDanSemua(jenisNilaiSiswa, new String[] { "nama", "kode" }, "keterangan",
							JenisNilaiSiswa.class, "= Tanpa Jenis Penilaian =",
							Restrictions.and(
									Restrictions.or(Restrictions.isNull("kurikulumSekolah"),
											Restrictions.eq("kurikulumSekolah", kelasSiswa.getKurikulumSekolah())),
									Restrictions.and(Restrictions.eq("sekolah", s), Restrictions.eq("aktif", true))));

					Session session = HibernateUtil.currentSession();

					List<Long> longs = kelasSiswa.ambilMk();
					List<KurikulumPunyaMatapelajaran> kurikulumPunyaMatapelajarans = ConstantValues
							.simpleList(
									session.createCriteria(KurikulumPunyaMatapelajaran.class)
											.add(Restrictions.or(Restrictions.isNull("aktif"),
													Restrictions.eq("aktif", true)))
											.add(Restrictions.eq("kurikulumSekolah", kelasSiswa.getKurikulumSekolah()))
											.createAlias("matapelajaran", "matapelajaran")

											.add(longs == null || longs.isEmpty() ? Restrictions.sqlRestriction("true")
													: Restrictions.not(Restrictions.in("matapelajaran.id", longs)))
											.addOrder(Order.asc("matapelajaran.urutan")),
									KurikulumPunyaMatapelajaran.class);

					System.out.println("kurikulumPunyaMatapelajarans -> " + kurikulumPunyaMatapelajarans.size());

					Set<Long> mapGrup = new HashSet<Long>();

					for (KurikulumPunyaMatapelajaran kurikulumPunyaMatapelajaran : kurikulumPunyaMatapelajarans) {
						JenisPenilaian jenisPenilaian = kurikulumPunyaMatapelajaran.getMatapelajaran()
								.getJenisPenilaian();

						if (kurikulumPunyaMatapelajaran != null
								&& kurikulumPunyaMatapelajaran.getKurikulumSekolah() != null
								&& kurikulumPunyaMatapelajaran.getKurikulumSekolah().getJenisPenilaian() != null) {
							jenisPenilaian = kurikulumPunyaMatapelajaran.getKurikulumSekolah().getJenisPenilaian();
						}

						if (!mapGrup.contains(jenisPenilaian.getId())) {

							MyFormRow rowData = new MyFormRow();
							rowData.setValign("top");
							rowData.setParent(rowsData);

							rowData.appendChild(new MyLabelStyled(jenisPenilaian.getNama()));

							List<GrupPenilaian> grupPenilaians = ConstantValues.simpleList(
									session.createCriteria(DetailJenisPenilaian.class)
											.add(Restrictions.or(Restrictions.isNull("aktif"),
													Restrictions.eq("aktif", true)))
											.createAlias("grupPenilaian", "grupPenilaian")
											.add(Restrictions.or(Restrictions.isNull("grupPenilaian.tampilDirekap"),
													Restrictions.eq("grupPenilaian.tampilDirekap", true)))
											.add(Restrictions.eq("jenisPenilaian", jenisPenilaian))
											.setProjection(Projections.groupProperty("grupPenilaian.id")),
									GrupPenilaian.class, false);

							System.out.println("grupPenilaians -> " + grupPenilaians.size());

							mapGrup.add(jenisPenilaian.getId());

							Collections.sort(grupPenilaians);

							for (final GrupPenilaian grupPenilaian : grupPenilaians) {

								rowData = new MyFormRow();
								rowData.setParent(rowsData);

								final MyCheckboxConfig checkboxgrupPenilaian = new MyCheckboxConfig(
										grupPenilaian.getNama());
								checkboxgrupPenilaian.setStyle("font-size:14px;");
								checkboxgrupPenilaian.setAttribute("grupPenilaian", grupPenilaian);
								checkboxgrupPenilaian.addEventListener("onClick", new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										if (!checkboxgrupPenilaian.isChecked()) {
											masukGrup.remove(grupPenilaian.getId());
										} else {
											masukGrup.add(grupPenilaian.getId());
										}

									}
								});

//								if (baru && !grupPenilaian.getTampilDirekap()) {
//									blmMasukGrup.add(grupPenilaian.getId());
//								}

								checkboxgrupPenilaian.setChecked(masukGrup.contains(grupPenilaian.getId()));
								rowData.appendChild(checkboxgrupPenilaian);

								List<GrupKategoriItemPenilaianSiswa> grupKategoriItemPenilaianSiswas = ConstantValues
										.simpleList(
												session.createCriteria(DetailGrupPenilaian.class)
														.add(Restrictions.or(Restrictions.isNull("aktif"),
																Restrictions.eq("aktif", true)))
														.add(Restrictions.isNotNull("grupKategoriItemPenilaianSiswa"))
														.setProjection(Projections
																.groupProperty("grupKategoriItemPenilaianSiswa.id"))

														.add(Restrictions.eq("grupPenilaian", grupPenilaian)),
												GrupKategoriItemPenilaianSiswa.class, false);

								System.out.println(
										"grupKategoriItemPenilaianSiswas -> " + grupKategoriItemPenilaianSiswas.size());

								rowData = new MyFormRow();
								rowData.setParent(rowsData);

								MyVboxStyled groupboxStyled = new MyVboxStyled();
								groupboxStyled.setParent(rowData);

								Collections.sort(grupKategoriItemPenilaianSiswas);

								for (final GrupKategoriItemPenilaianSiswa grupKategoriItemPenilaianSiswa : grupKategoriItemPenilaianSiswas) {

									final MyCheckboxConfig checkboxConfig = new MyCheckboxConfig(
											grupKategoriItemPenilaianSiswa.getNama());
									checkboxConfig.setStyle("font-size:10px;");
									checkboxConfig.setAttribute("grupKategoriItemPenilaianSiswa",
											grupKategoriItemPenilaianSiswa);
									checkboxConfig.addEventListener("onClick", new EventListener() {

										@Override
										public void onEvent(Event arg0) throws Exception {
											if (!checkboxConfig.isChecked()) {
												masukSubgrup.remove(grupKategoriItemPenilaianSiswa.getId());
											} else {
												masukSubgrup.add(grupKategoriItemPenilaianSiswa.getId());
												
												checkboxgrupPenilaian.setChecked(true);
												masukGrup.add(grupPenilaian.getId());
											}

										}
									});
									checkboxConfig
											.setChecked(masukSubgrup.contains(grupKategoriItemPenilaianSiswa.getId()));
									groupboxStyled.appendChild(checkboxConfig);

								}

							}
						}
					}
				}
			}
		};

		searchnama.setEventListener(eventListener);

		searchtotal.addEventListener("onClick", eventListener);
		searchmax.addEventListener("onClick", eventListener);
		searchmin.addEventListener("onClick", eventListener);

		jenisNilaiSiswa.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				JenisNilaiSiswa jenis = (JenisNilaiSiswa) (jenisNilaiSiswa.getSelectedItem() == null ? null
						: jenisNilaiSiswa.getSelectedItem().getValue());

				LampiranLain lainMahasiswa = jenis == null ? null
						: LampiranLain.ambil(jenis.getId(), LampiranLain.FILE_JRXML_LAYOUT_JENIS_NILAI);

				printAmbil.setVisible(lainMahasiswa == null);
				rowsData.getParent().setVisible(lainMahasiswa == null);

				searchmin.getParent().getParent().setVisible(lainMahasiswa == null);
				searchmax.getParent().getParent().setVisible(lainMahasiswa == null);
				searchtotal.getParent().getParent().setVisible(lainMahasiswa == null);
			}
		});

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

	}

	@SuppressWarnings("rawtypes")
	private List<List> datas = null;

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void onCetak(Event event) {

		try {

			Common.clear(center);

			final KelasSiswa kelasSiswa = (KelasSiswa) searchnama.getAttribute("kelas");
			if (kelasSiswa == null) {
				MyMessageboxConfig.show("Kelas harus dipilih", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.INFORMATION);
				return;
			}
			final Integer smt = (Integer) searchsmt.getSelectedItem().getValue();
			Tbmuser tbmuser = Common.getCurrentUser();
			final List<Long> anak = tbmuser != null && tbmuser.getOrangTua() != null
					? tbmuser.getOrangTua().ambilAnakSiswa()
					: new ArrayList<Long>();
			final Label label = new Label(ais.common.Common.getBahasaConfig("Proses load data .."));

			final JenisNilaiSiswa j = (JenisNilaiSiswa) (jenisNilaiSiswa.getSelectedItem() == null ? null
					: jenisNilaiSiswa.getSelectedItem().getValue());
			final String fileReport;
			final Map parameters = new HashMap();

			Common.insertProperty(KelasSiswa.class, kelasSiswa, parameters, "kelas");

			Guru guru = kelasSiswa.getGuruPembina();
			if (guru != null) {
				Common.insertProperty(Guru.class, guru, parameters, "wali");
			}
			if (j != null) {

				LampiranLain lainMahasiswa = LampiranLain.ambil(j.getId(), LampiranLain.FILE_JRXML_LAYOUT_JENIS_NILAI);

				if (lainMahasiswa == null) {
					MyMessageboxConfig.show("File template jenis nilai siswa belum diupload", "Peringatan",
							MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
					return;
				}

				fileReport = lainMahasiswa.ambilFile().getAbsolutePath();
				// EKSPOR DINAMIS: jrxml terupload per jenis dipakai utk SEMUA format cetak (PDF/XLS/DOCX/PPTX).
				parameters.put("nama_laporan", fileReport);

			} else {
				fileReport = null;
			}

			new Thread(new Runnable() {

				@SuppressWarnings("unused")
				@Override
				public void run() {

					datas = new ArrayList<List>();

					Map<String, Double> mapsMinPerJenis;
					Map<String, Double> mapsMaxPerJenis;
					Map<String, Double> mapsTotalPerJenis;
					Map<String, Integer> mapsJmlPerJenis;
					int nomor = 1;
					Session session = null;
					if (kelasSiswa != null) {
						label.setValue("Sedang memproses kelas " + kelasSiswa.toString());
						String n = kelasSiswa.getNama() + " "
								+ (kelasSiswa.getGuruPembina() == null ? "" : kelasSiswa.getGuruPembina().getNama());

						session = HibernateUtil.openSession();
						try {
						Date sekarang = WaktuUtil.getDate();
						List<Long> longs = kelasSiswa.ambilMk();
						List<KurikulumPunyaMatapelajaran> kurikulumPunyaMatapelajarans = ConstantValues.simpleList(
								session.createCriteria(KurikulumPunyaMatapelajaran.class)
										.add(Restrictions.or(Restrictions.isNull("aktif"),
												Restrictions.eq("aktif", true)))
										.add(Restrictions.eq("kurikulumSekolah", kelasSiswa.getKurikulumSekolah()))
										.createAlias("matapelajaran", "matapelajaran")

										.add(longs == null || longs.isEmpty() ? Restrictions.sqlRestriction("true")
												: Restrictions.not(Restrictions.in("matapelajaran.id", longs)))

										.addOrder(Order.asc("matapelajaran.urutan")),
								KurikulumPunyaMatapelajaran.class);

						Map<Long, List<GrupPenilaian>> mapGrup = new HashMap<Long, List<GrupPenilaian>>();
						Map<Long, List<GrupKategoriItemPenilaianSiswa>> mapGrupKat = new HashMap<Long, List<GrupKategoriItemPenilaianSiswa>>();

						Map<Long, List<GrupKategoriItemPenilaianSiswa>> mapGrupKatAll = new HashMap<Long, List<GrupKategoriItemPenilaianSiswa>>();

						for (KurikulumPunyaMatapelajaran kurikulumPunyaMatapelajaran : kurikulumPunyaMatapelajarans) {
							JenisPenilaian jenisPenilaian = kurikulumPunyaMatapelajaran.getMatapelajaran()
									.getJenisPenilaian();

							if (kurikulumPunyaMatapelajaran != null
									&& kurikulumPunyaMatapelajaran.getKurikulumSekolah() != null
									&& kurikulumPunyaMatapelajaran.getKurikulumSekolah().getJenisPenilaian() != null) {
								jenisPenilaian = kurikulumPunyaMatapelajaran.getKurikulumSekolah().getJenisPenilaian();
							}

							if (!mapGrup.containsKey(jenisPenilaian.getId())) {
								List<GrupPenilaian> grupPenilaians = ConstantValues.simpleList(

										session.createCriteria(DetailJenisPenilaian.class)
												.add(Restrictions.or(Restrictions.isNull("aktif"),
														Restrictions.eq("aktif", true)))
												.createAlias("grupPenilaian", "grupPenilaian")
												.add(Restrictions.or(Restrictions.isNull("grupPenilaian.tampilDirekap"),
														Restrictions.eq("grupPenilaian.tampilDirekap", true)))
												.add(Restrictions.eq("jenisPenilaian", jenisPenilaian))

												.add(fileReport != null || masukGrup.isEmpty()
														? Restrictions.sqlRestriction("true")
														: Restrictions.in("grupPenilaian.id", masukGrup))

												.setProjection(Projections.groupProperty("grupPenilaian.id")),
										GrupPenilaian.class, false);
								Collections.sort(grupPenilaians);
								mapGrup.put(jenisPenilaian.getId(), grupPenilaians);

								for (GrupPenilaian grupPenilaian : grupPenilaians) {

									if (!mapGrupKat.containsKey(grupPenilaian.getId())) {

										List<GrupKategoriItemPenilaianSiswa> grupKategoriItemPenilaianSiswas = ConstantValues
												.simpleList(
														session.createCriteria(DetailGrupPenilaian.class)
																.add(Restrictions.or(
																		Restrictions.isNull("aktif"),
																		Restrictions.eq("aktif", true)))
																.add(Restrictions
																		.isNotNull("grupKategoriItemPenilaianSiswa"))

																.add(fileReport != null
																		? Restrictions.sqlRestriction("true")
																		: masukSubgrup.isEmpty()
																				? Restrictions.sqlRestriction("false")
																				: Restrictions.in(
																						"grupKategoriItemPenilaianSiswa.id",
																						masukSubgrup))

																.setProjection(Projections.groupProperty(
																		"grupKategoriItemPenilaianSiswa.id"))
																.add(Restrictions.eq("grupPenilaian", grupPenilaian)),
														GrupKategoriItemPenilaianSiswa.class, false);

										Collections.sort(grupKategoriItemPenilaianSiswas);

										mapGrupKat.put(grupPenilaian.getId(), grupKategoriItemPenilaianSiswas);
									}

									if (!mapGrupKatAll.containsKey(grupPenilaian.getId())) {

										List<GrupKategoriItemPenilaianSiswa> grupKategoriItemPenilaianSiswas = ConstantValues
												.simpleList(
														session.createCriteria(DetailGrupPenilaian.class)
																.add(Restrictions.or(Restrictions.isNull("aktif"),
																		Restrictions.eq("aktif", true)))
																.add(Restrictions
																		.isNotNull("grupKategoriItemPenilaianSiswa"))

																.setProjection(Projections.groupProperty(
																		"grupKategoriItemPenilaianSiswa.id"))
																.add(Restrictions.eq("grupPenilaian", grupPenilaian)),
														GrupKategoriItemPenilaianSiswa.class, false);

										Collections.sort(grupKategoriItemPenilaianSiswas);

										mapGrupKatAll.put(grupPenilaian.getId(), grupKategoriItemPenilaianSiswas);
									}

								}
							}
						}

						try {

							List sub = new ArrayList();

							sub.add("");
							sub.add("");
							sub.add("");
							sub.add("");
							sub.add("");
							sub.add("");
							for (KurikulumPunyaMatapelajaran kurikulumPunyaMatapelajaran : kurikulumPunyaMatapelajarans) {

								JenisPenilaian jenisPenilaian = kurikulumPunyaMatapelajaran.getMatapelajaran()
										.getJenisPenilaian();

								parameters.put("jenisPenilaian_" + jenisPenilaian.getId(), jenisPenilaian.getNama());
								parameters.put(
										"matapelajaran_" + kurikulumPunyaMatapelajaran.getMatapelajaran().getId(),
										kurikulumPunyaMatapelajaran.getMatapelajaran().getNama());
								parameters.put("kode_mk_" + kurikulumPunyaMatapelajaran.getMatapelajaran().getId(),
										kurikulumPunyaMatapelajaran.getMatapelajaran().getKode());
								parameters.put("kkm_mk_" + kurikulumPunyaMatapelajaran.getMatapelajaran().getId(),
										kurikulumPunyaMatapelajaran.getMatapelajaran().getKkm());

								if (kurikulumPunyaMatapelajaran != null
										&& kurikulumPunyaMatapelajaran.getKurikulumSekolah() != null
										&& kurikulumPunyaMatapelajaran.getKurikulumSekolah()
												.getJenisPenilaian() != null) {
									jenisPenilaian = kurikulumPunyaMatapelajaran.getKurikulumSekolah()
											.getJenisPenilaian();
								}

								for (GrupPenilaian grupPenilaian : mapGrup.get(jenisPenilaian.getId())) {
									sub.add("");
									sub.add("");
								}
							}
							sub.add("");
							sub.add("");
							sub.add("");

							datas.add(sub);

							sub = new ArrayList();

							sub.add("");
							sub.add(n);
							sub.add("");
							sub.add("");
							sub.add("");
							sub.add("");
							for (KurikulumPunyaMatapelajaran kurikulumPunyaMatapelajaran : kurikulumPunyaMatapelajarans) {

								JenisPenilaian jenisPenilaian = kurikulumPunyaMatapelajaran.getMatapelajaran()
										.getJenisPenilaian();

								if (kurikulumPunyaMatapelajaran != null
										&& kurikulumPunyaMatapelajaran.getKurikulumSekolah() != null
										&& kurikulumPunyaMatapelajaran.getKurikulumSekolah()
												.getJenisPenilaian() != null) {
									jenisPenilaian = kurikulumPunyaMatapelajaran.getKurikulumSekolah()
											.getJenisPenilaian();
								}

								for (GrupPenilaian grupPenilaian : mapGrup.get(jenisPenilaian.getId())) {
									sub.add("");
									sub.add("");
								}
							}
							sub.add("");
							sub.add("");
							sub.add("");

							datas.add(sub);

							sub = new ArrayList();
							sub.add("**No.");
							sub.add("**NIS");
							sub.add("**Nama");
							sub.add("**Semester");

							for (KurikulumPunyaMatapelajaran kurikulumPunyaMatapelajaran : kurikulumPunyaMatapelajarans) {

								JenisPenilaian jenisPenilaian = kurikulumPunyaMatapelajaran.getMatapelajaran()
										.getJenisPenilaian();

								if (kurikulumPunyaMatapelajaran != null
										&& kurikulumPunyaMatapelajaran.getKurikulumSekolah() != null
										&& kurikulumPunyaMatapelajaran.getKurikulumSekolah()
												.getJenisPenilaian() != null) {
									jenisPenilaian = kurikulumPunyaMatapelajaran.getKurikulumSekolah()
											.getJenisPenilaian();
								}

								int s = 0;
								for (GrupPenilaian grupPenilaian : mapGrup.get(jenisPenilaian.getId())) {

									parameters.put("grupPenilaian_" + grupPenilaian.getId(), grupPenilaian.getNama());

									List<GrupKategoriItemPenilaianSiswa> grupKategoriItemPenilaianSiswas = mapGrupKat
											.get(grupPenilaian.getId());

									for (GrupKategoriItemPenilaianSiswa grupKategoriItemPenilaianSiswa : grupKategoriItemPenilaianSiswas) {

										parameters.put(
												"grupKategoriItemPenilaianSiswa_"
														+ grupKategoriItemPenilaianSiswa.getId(),
												grupKategoriItemPenilaianSiswa.getNama());

										if (s == 0) {
											sub.add("**" + kurikulumPunyaMatapelajaran.getMatapelajaran().getNama());
											sub.add("**");
										} else {
											sub.add("**");
											sub.add("**");
										}
										s++;
									}

									if (grupKategoriItemPenilaianSiswas.isEmpty() && searchtotal.isChecked())
										sub.add("**" + kurikulumPunyaMatapelajaran.getMatapelajaran().getNama());
									else if (searchtotal.isChecked())
										sub.add("**");

									if (searchmin.isChecked())
										sub.add("**");
									if (searchmax.isChecked())
										sub.add("**");
									if (searchtotal.isChecked())
										sub.add("**");
								}
							}

							sub.add("**Total");
							sub.add("**Rata-Rata");
							sub.add("**Rangking");

							datas.add(sub);

							sub = new ArrayList();
							sub.add("**");
							sub.add("**");
							sub.add("**");
							sub.add("**");

							for (KurikulumPunyaMatapelajaran kurikulumPunyaMatapelajaran : kurikulumPunyaMatapelajarans) {

								JenisPenilaian jenisPenilaian = kurikulumPunyaMatapelajaran.getMatapelajaran()
										.getJenisPenilaian();

								if (kurikulumPunyaMatapelajaran != null
										&& kurikulumPunyaMatapelajaran.getKurikulumSekolah() != null
										&& kurikulumPunyaMatapelajaran.getKurikulumSekolah()
												.getJenisPenilaian() != null) {
									jenisPenilaian = kurikulumPunyaMatapelajaran.getKurikulumSekolah()
											.getJenisPenilaian();
								}

								for (GrupPenilaian grupPenilaian : mapGrup.get(jenisPenilaian.getId())) {

									List<GrupKategoriItemPenilaianSiswa> grupKategoriItemPenilaianSiswas = mapGrupKat
											.get(grupPenilaian.getId());
									int index = 0;
									for (GrupKategoriItemPenilaianSiswa grupKategoriItemPenilaianSiswa : grupKategoriItemPenilaianSiswas) {
										if (index == 0) {
											sub.add("**" + grupPenilaian.getNama());
											sub.add("**");
										} else {
											sub.add("**");
											sub.add("**");
										}

										index++;
									}

									if (grupKategoriItemPenilaianSiswas.isEmpty() && searchtotal.isChecked())
										sub.add("**" + grupPenilaian.getNama());
									else if (searchtotal.isChecked())
										sub.add("**");

									if (searchmin.isChecked())
										sub.add("**");
									if (searchmax.isChecked())
										sub.add("**");
									if (searchtotal.isChecked())
										sub.add("**");
								}
							}

							sub.add("**");
							sub.add("**");
							sub.add("**");

							datas.add(sub);

							sub = new ArrayList();
							sub.add("**");
							sub.add("**");
							sub.add("**");
							sub.add("**");

							for (KurikulumPunyaMatapelajaran kurikulumPunyaMatapelajaran : kurikulumPunyaMatapelajarans) {

								JenisPenilaian jenisPenilaian = kurikulumPunyaMatapelajaran.getMatapelajaran()
										.getJenisPenilaian();

								if (kurikulumPunyaMatapelajaran != null
										&& kurikulumPunyaMatapelajaran.getKurikulumSekolah() != null
										&& kurikulumPunyaMatapelajaran.getKurikulumSekolah()
												.getJenisPenilaian() != null) {
									jenisPenilaian = kurikulumPunyaMatapelajaran.getKurikulumSekolah()
											.getJenisPenilaian();
								}

								for (GrupPenilaian grupPenilaian : mapGrup.get(jenisPenilaian.getId())) {

									List<GrupKategoriItemPenilaianSiswa> grupKategoriItemPenilaianSiswas = mapGrupKat
											.get(grupPenilaian.getId());

									for (GrupKategoriItemPenilaianSiswa grupKategoriItemPenilaianSiswa : grupKategoriItemPenilaianSiswas) {
										sub.add("**" + grupKategoriItemPenilaianSiswa.getNama());
										sub.add("**");
									}

									if (searchtotal.isChecked())
										sub.add("**Total");
									if (searchmin.isChecked())
										sub.add("**Min");
									if (searchmax.isChecked())
										sub.add("**Max");
									if (searchtotal.isChecked())
										sub.add("**Hrf");
								}
							}

							sub.add("**");
							sub.add("**");
							sub.add("**");

							datas.add(sub);

						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/sekolah/LaporanRekapTotalNilai.java:862");
							// TODO: handle exception
						}
						mapsMinPerJenis = new HashMap<String, Double>();
						mapsMaxPerJenis = new HashMap<String, Double>();
						mapsTotalPerJenis = new HashMap<String, Double>();
						mapsJmlPerJenis = new HashMap<String, Integer>();
						nomor = 1;
						int jmlSiswa = 0;

						TreeSet<Double> treeMapRangking = new TreeSet<Double>(Collections.reverseOrder());

						try {

							List<KelasSiswaPunyaSiswa> kelasSiswaPunyaSiswas = ConstantValues.simpleList(
									session.createCriteria(KelasSiswaPunyaSiswa.class)
											.add(Restrictions.eq("kelasSiswa", kelasSiswa))
											.createAlias("siswa", "siswa")
											.add(anak.isEmpty() ? Restrictions.sqlRestriction("true")
													: Restrictions.in("siswa.id", anak))
											.addOrder(Order.asc("nomorUrut")).addOrder(Order.asc("siswa.nama")),
									KelasSiswaPunyaSiswa.class);
							jmlSiswa = kelasSiswaPunyaSiswas.size();

							parameters.put("jmlSiswa", jmlSiswa);

							for (KelasSiswaPunyaSiswa kelasSiswaPunyaSiswa : kelasSiswaPunyaSiswas) {
								Double jml = 0.0;
								Double totalsemua = 0.0;

								for (KurikulumPunyaMatapelajaran kurikulumPunyaMatapelajaran : kurikulumPunyaMatapelajarans) {

									JenisPenilaian jenisPenilaian = kurikulumPunyaMatapelajaran.getMatapelajaran()
											.getJenisPenilaian();

									if (kurikulumPunyaMatapelajaran != null
											&& kurikulumPunyaMatapelajaran.getKurikulumSekolah() != null
											&& kurikulumPunyaMatapelajaran.getKurikulumSekolah()
													.getJenisPenilaian() != null) {
										jenisPenilaian = kurikulumPunyaMatapelajaran.getKurikulumSekolah()
												.getJenisPenilaian();
									}

									for (GrupPenilaian grupPenilaian : mapGrup.get(jenisPenilaian.getId())) {

										Double total = 0.0;

										try {

											List<GrupKategoriItemPenilaianSiswa> grupKategoriItemPenilaianSiswasAll = mapGrupKatAll
													.get(grupPenilaian.getId());

											String formula = grupPenilaian.getFormula();

											String target = GrupPenilaianUtil.ambilTarget(formula, sekarang);

											total = kelasSiswaPunyaSiswa.retreiveTotalNilaiTotal(target,
													kurikulumPunyaMatapelajaran.getMatapelajaran(), grupPenilaian, smt,
													grupKategoriItemPenilaianSiswasAll);

											totalsemua += total;
											jml++;

										} catch (Exception e) {
											e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/sekolah/LaporanRekapTotalNilai.java:926");
											PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Rekap Total Nilai", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
												new String[] {
													"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
													"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
													"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
												});
										}

									}
								}

								treeMapRangking.add(totalsemua);
							}

							List<Map> maps = new ArrayList<Map>();

							if (j != null && j.getBerdasarkanMk()) {

								for (KurikulumPunyaMatapelajaran kurikulumPunyaMatapelajaran : kurikulumPunyaMatapelajarans) {

									Matapelajaran matapelajaran = kurikulumPunyaMatapelajaran.getMatapelajaran();
									label.setValue("Sedang memproses data " + matapelajaran.toString());

									List sub = new ArrayList();
									sub.add(nomor);
									sub.add(matapelajaran.getKode());
									sub.add(matapelajaran.getNama());

									sub.add(smt);

									for (KelasSiswaPunyaSiswa kelasSiswaPunyaSiswa : kelasSiswaPunyaSiswas) {

										Map map = new HashMap();
										map.put("nomor", nomor);
										map.put("smt", smt);
										Common.insertProperty(Matapelajaran.class, matapelajaran, map, "mk");

										JSONObject js = new JSONObject();
										try {
											js = new JSONObject(smt == 1 ? kelasSiswaPunyaSiswa.getKeterangan1()
													: kelasSiswaPunyaSiswa.getKeterangan2());
										} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/sekolah/LaporanRekapTotalNilai.java:962");
											PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Rekap Total Nilai", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
												new String[] {
													"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
													"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
													"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
												});
										}

										siswaMk(session, kurikulumPunyaMatapelajaran, smt, kelasSiswaPunyaSiswa, map,
												sub, js, mapGrup, mapGrupKat, sekarang, mapsMinPerJenis,
												mapsMaxPerJenis, mapsTotalPerJenis, mapsJmlPerJenis, mapGrupKatAll);

										Double totalsemua = mapsTotalPerJenis
												.get("totalsemua_" + kelasSiswaPunyaSiswa.getSiswa().getId());
										totalsemua = totalsemua == null ? 0.0 : totalsemua;

										Double jml = mapsTotalPerJenis
												.get("jml_" + kelasSiswaPunyaSiswa.getSiswa().getId());
										jml = jml == null ? 0.0 : jml;
										map.put("totalsemua", totalsemua);
										map.put("rata_rata_semua", totalsemua / jml);

										int rangking = 0;
										for (Double nilai : treeMapRangking) {
											rangking++;
											if (Common.numberFormat.get().format(nilai)
													.equals(Common.numberFormat.get().format(totalsemua))) {
												break;
											}
										}
										sub.add("**" + Common.numberFormat.get().format(rangking));
										datas.add(sub);

										map.put("rangking_semua", rangking);

										maps.add(map);

										nomor++;
									}

									Double totalsemua = mapsTotalPerJenis.get("totalsemua");
									totalsemua = totalsemua == null ? 0.0 : totalsemua;

									Double jml = mapsTotalPerJenis.get("jml");
									jml = jml == null ? 0.0 : jml;

									sub.add("**" + Common.numberFormat.get().format(totalsemua));
									sub.add("**" + Common.numberFormat.get().format(totalsemua / jml));

								}

							} else {

								for (KelasSiswaPunyaSiswa kelasSiswaPunyaSiswa : kelasSiswaPunyaSiswas) {

									JSONObject js = new JSONObject();
									try {
										js = new JSONObject(smt == 1 ? kelasSiswaPunyaSiswa.getKeterangan1()
												: kelasSiswaPunyaSiswa.getKeterangan2());
									} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/sekolah/LaporanRekapTotalNilai.java:1016");
										PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Rekap Total Nilai", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
											new String[] {
												"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
												"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
												"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
											});
									}

									Map map = new HashMap();

									Siswa siswa = kelasSiswaPunyaSiswa.getSiswa();
									label.setValue("Sedang memproses data " + siswa.toString());

									Common.insertProperty(KelasSiswaPunyaSiswa.class, kelasSiswaPunyaSiswa, map, "");

									map.put("nomor", nomor);
									map.put("smt", smt);

									List sub = new ArrayList();
									sub.add(nomor);
									sub.add(siswa.getNomorInduk());
									sub.add(siswa.getNama());

									sub.add(smt);

									for (KurikulumPunyaMatapelajaran kurikulumPunyaMatapelajaran : kurikulumPunyaMatapelajarans) {

										siswaMk(session, kurikulumPunyaMatapelajaran, smt, kelasSiswaPunyaSiswa, map,
												sub, js, mapGrup, mapGrupKat, sekarang, mapsMinPerJenis,
												mapsMaxPerJenis, mapsTotalPerJenis, mapsJmlPerJenis, mapGrupKatAll);

									}

									Double totalsemua = mapsTotalPerJenis
											.get("totalsemua_" + kelasSiswaPunyaSiswa.getSiswa().getId());
									totalsemua = totalsemua == null ? 0.0 : totalsemua;

									Double jml = mapsTotalPerJenis
											.get("jml_" + kelasSiswaPunyaSiswa.getSiswa().getId());
									jml = jml == null ? 0.0 : jml;

									sub.add("**" + Common.numberFormat.get().format(totalsemua));
									sub.add("**" + Common.numberFormat.get().format(totalsemua / jml));

									map.put("totalsemua", totalsemua);
									map.put("rata_rata_semua", totalsemua / jml);

									int rangking = 0;
									for (Double nilai : treeMapRangking) {
										rangking++;
										if (Common.numberFormat.get().format(nilai)
												.equals(Common.numberFormat.get().format(totalsemua))) {
											break;
										}
									}
									sub.add("**" + Common.numberFormat.get().format(rangking));
									datas.add(sub);

									map.put("rangking_semua", rangking);

									maps.add(map);

									nomor++;
								}
							}
							parameters.put("maps", maps);

							try {

								ArrayList sub = new ArrayList();
								sub.add("**");
								sub.add("**Total");
								sub.add("**");
								sub.add("**");
								Double jml = 0.0;
								Double totalsemua = 0.0;
								for (KurikulumPunyaMatapelajaran kurikulumPunyaMatapelajaran : kurikulumPunyaMatapelajarans) {

									JenisPenilaian jenisPenilaian = kurikulumPunyaMatapelajaran.getMatapelajaran()
											.getJenisPenilaian();

									if (kurikulumPunyaMatapelajaran != null
											&& kurikulumPunyaMatapelajaran.getKurikulumSekolah() != null
											&& kurikulumPunyaMatapelajaran.getKurikulumSekolah()
													.getJenisPenilaian() != null) {
										jenisPenilaian = kurikulumPunyaMatapelajaran.getKurikulumSekolah()
												.getJenisPenilaian();
									}

									for (GrupPenilaian grupPenilaian : mapGrup.get(jenisPenilaian.getId())) {

										List<GrupKategoriItemPenilaianSiswa> grupKategoriItemPenilaianSiswas = mapGrupKat
												.get(grupPenilaian.getId());

										for (GrupKategoriItemPenilaianSiswa grupKategoriItemPenilaianSiswa : grupKategoriItemPenilaianSiswas) {
											String key = grupKategoriItemPenilaianSiswa.getId() + "_"
													+ kurikulumPunyaMatapelajaran.getId() + "_" + grupPenilaian.getId();
											Double o = mapsTotalPerJenis.get(key);

											sub.add("**" + Common.numberFormat.get().format(o));
											sub.add("**");

											totalsemua += o == null ? 0.0 : o;
											jml++;
										}

										String key = "total_" + kurikulumPunyaMatapelajaran.getId() + "_"
												+ grupPenilaian.getId();
										Double o = mapsTotalPerJenis.get(key);
										o = o == null ? 0.0 : o;

										if (searchtotal.isChecked())
											sub.add("**" + Common.numberFormat.get().format(o));

										key = "min_" + kurikulumPunyaMatapelajaran.getId() + "_"
												+ grupPenilaian.getId();
										o = mapsTotalPerJenis.get(key);
										o = o == null ? 0.0 : o;

										if (searchmin.isChecked())
											sub.add("**" + Common.numberFormat.get().format(o));

										key = "max_" + kurikulumPunyaMatapelajaran.getId() + "_"
												+ grupPenilaian.getId();
										o = mapsTotalPerJenis.get(key);
										o = o == null ? 0.0 : o;
										if (searchmax.isChecked())
											sub.add("**" + Common.numberFormat.get().format(o));

										if (searchtotal.isChecked())
											sub.add("**");
									}

								}

								sub.add("**" + Common.numberFormat.get().format(totalsemua));
								sub.add("**" + Common.numberFormat.get().format(totalsemua / jml));
								sub.add("**");

								datas.add(sub);

								sub = new ArrayList();
								sub.add("**");
								sub.add("**Rata-rata");
								sub.add("**");
								sub.add("**");
								jml = 0.0;
								totalsemua = 0.0;
								for (KurikulumPunyaMatapelajaran kurikulumPunyaMatapelajaran : kurikulumPunyaMatapelajarans) {

									JenisPenilaian jenisPenilaian = kurikulumPunyaMatapelajaran.getMatapelajaran()
											.getJenisPenilaian();

									if (kurikulumPunyaMatapelajaran != null
											&& kurikulumPunyaMatapelajaran.getKurikulumSekolah() != null
											&& kurikulumPunyaMatapelajaran.getKurikulumSekolah()
													.getJenisPenilaian() != null) {
										jenisPenilaian = kurikulumPunyaMatapelajaran.getKurikulumSekolah()
												.getJenisPenilaian();
									}

									for (GrupPenilaian grupPenilaian : mapGrup.get(jenisPenilaian.getId())) {

										List<GrupKategoriItemPenilaianSiswa> grupKategoriItemPenilaianSiswas = mapGrupKat
												.get(grupPenilaian.getId());

										for (GrupKategoriItemPenilaianSiswa grupKategoriItemPenilaianSiswa : grupKategoriItemPenilaianSiswas) {
											String key = grupKategoriItemPenilaianSiswa.getId() + "_"
													+ kurikulumPunyaMatapelajaran.getId() + "_" + grupPenilaian.getId();
											Double o = mapsTotalPerJenis.get(key);
											o = (o == null) ? 0.0 : o / jmlSiswa;

											sub.add("**" + Common.numberFormat.get().format(o));
											sub.add("**");

											totalsemua += o == null ? 0.0 : o;
											jml++;
										}

										String key = "total_" + kurikulumPunyaMatapelajaran.getId() + "_"
												+ grupPenilaian.getId();
										Double o = mapsTotalPerJenis.get(key);
										o = o == null ? 0.0 : o / jmlSiswa;

										if (searchtotal.isChecked())
											sub.add("**" + Common.numberFormat.get().format(o));

										key = "min_" + kurikulumPunyaMatapelajaran.getId() + "_"
												+ grupPenilaian.getId();
										o = mapsTotalPerJenis.get(key);
										o = o == null ? 0.0 : o / jmlSiswa;
										if (searchmin.isChecked())
											sub.add("**" + Common.numberFormat.get().format(o));

										key = "max_" + kurikulumPunyaMatapelajaran.getId() + "_"
												+ grupPenilaian.getId();
										o = mapsTotalPerJenis.get(key);
										o = o == null ? 0.0 : o / jmlSiswa;
										if (searchmax.isChecked())
											sub.add("**" + Common.numberFormat.get().format(o));

										if (searchtotal.isChecked())
											sub.add("**");
									}

								}

								sub.add("**" + Common.numberFormat.get().format(totalsemua));
								sub.add("**" + Common.numberFormat.get().format(totalsemua / jml));
								sub.add("**");

								datas.add(sub);

								sub = new ArrayList();
								sub.add("**");
								sub.add("**Minimal");
								sub.add("**");
								sub.add("**");

								for (KurikulumPunyaMatapelajaran kurikulumPunyaMatapelajaran : kurikulumPunyaMatapelajarans) {

									JenisPenilaian jenisPenilaian = kurikulumPunyaMatapelajaran.getMatapelajaran()
											.getJenisPenilaian();

									if (kurikulumPunyaMatapelajaran != null
											&& kurikulumPunyaMatapelajaran.getKurikulumSekolah() != null
											&& kurikulumPunyaMatapelajaran.getKurikulumSekolah()
													.getJenisPenilaian() != null) {
										jenisPenilaian = kurikulumPunyaMatapelajaran.getKurikulumSekolah()
												.getJenisPenilaian();
									}

									for (GrupPenilaian grupPenilaian : mapGrup.get(jenisPenilaian.getId())) {

										List<GrupKategoriItemPenilaianSiswa> grupKategoriItemPenilaianSiswas = mapGrupKat
												.get(grupPenilaian.getId());

										for (GrupKategoriItemPenilaianSiswa grupKategoriItemPenilaianSiswa : grupKategoriItemPenilaianSiswas) {
											String key = grupKategoriItemPenilaianSiswa.getId() + "_"
													+ kurikulumPunyaMatapelajaran.getId() + "_" + grupPenilaian.getId();
											Double o = mapsMinPerJenis.get(key);

											sub.add("**" + Common.numberFormat.get().format(o));
											sub.add("**");

										}

										String key = "total_" + kurikulumPunyaMatapelajaran.getId() + "_"
												+ grupPenilaian.getId();
										Double o = mapsMinPerJenis.get(key);
										o = o == null ? 0.0 : o;

										if (searchtotal.isChecked())
											sub.add("**" + Common.numberFormat.get().format(o));

										key = "min_" + kurikulumPunyaMatapelajaran.getId() + "_"
												+ grupPenilaian.getId();
										o = mapsMinPerJenis.get(key);
										o = o == null ? 0.0 : o;

										if (searchmin.isChecked())
											sub.add("**" + Common.numberFormat.get().format(o));

										key = "max_" + kurikulumPunyaMatapelajaran.getId() + "_"
												+ grupPenilaian.getId();
										o = mapsMinPerJenis.get(key);
										o = o == null ? 0.0 : o;
										if (searchmax.isChecked())
											sub.add("**" + Common.numberFormat.get().format(o));

										if (searchtotal.isChecked())
											sub.add("**");

									}

								}

								sub.add("**");
								sub.add("**");
								sub.add("**");

								datas.add(sub);

								sub = new ArrayList();
								sub.add("**");
								sub.add("**Maksimal");
								sub.add("**");
								sub.add("**");

								for (KurikulumPunyaMatapelajaran kurikulumPunyaMatapelajaran : kurikulumPunyaMatapelajarans) {

									JenisPenilaian jenisPenilaian = kurikulumPunyaMatapelajaran.getMatapelajaran()
											.getJenisPenilaian();

									if (kurikulumPunyaMatapelajaran != null
											&& kurikulumPunyaMatapelajaran.getKurikulumSekolah() != null
											&& kurikulumPunyaMatapelajaran.getKurikulumSekolah()
													.getJenisPenilaian() != null) {
										jenisPenilaian = kurikulumPunyaMatapelajaran.getKurikulumSekolah()
												.getJenisPenilaian();
									}

									for (GrupPenilaian grupPenilaian : mapGrup.get(jenisPenilaian.getId())) {

										List<GrupKategoriItemPenilaianSiswa> grupKategoriItemPenilaianSiswas = mapGrupKat
												.get(grupPenilaian.getId());

										for (GrupKategoriItemPenilaianSiswa grupKategoriItemPenilaianSiswa : grupKategoriItemPenilaianSiswas) {
											String key = grupKategoriItemPenilaianSiswa.getId() + "_"
													+ kurikulumPunyaMatapelajaran.getId() + "_" + grupPenilaian.getId();
											Double o = mapsMaxPerJenis.get(key);

											sub.add("**" + Common.numberFormat.get().format(o));
											sub.add("**");

										}

										String key = "total_" + kurikulumPunyaMatapelajaran.getId() + "_"
												+ grupPenilaian.getId();
										Double o = mapsMaxPerJenis.get(key);
										o = o == null ? 0.0 : o;

										if (searchtotal.isChecked())
											sub.add("**" + Common.numberFormat.get().format(o));

										key = "min_" + kurikulumPunyaMatapelajaran.getId() + "_"
												+ grupPenilaian.getId();
										o = mapsMaxPerJenis.get(key);
										o = o == null ? 0.0 : o;
										if (searchmin.isChecked())
											sub.add("**" + Common.numberFormat.get().format(o));

										key = "max_" + kurikulumPunyaMatapelajaran.getId() + "_"
												+ grupPenilaian.getId();
										o = mapsMaxPerJenis.get(key);
										o = o == null ? 0.0 : o;
										if (searchmax.isChecked())
											sub.add("**" + Common.numberFormat.get().format(o));

										if (searchtotal.isChecked())
											sub.add("**");
									}

								}

								sub.add("**");
								sub.add("**");
								sub.add("**");

								datas.add(sub);

							} catch (Exception e) {
								Common.tampilErrorJikaAdmin(e);
								PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Rekap Total Nilai", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
										new String[] {
											"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
											"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
											"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
										});
							}

						} catch (Exception e) {
							Common.tampilErrorJikaAdmin(e);
							PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Rekap Total Nilai", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
									new String[] {
										"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
										"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
										"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
									});
						}
						} finally {
							// Tutup session yang DIBUKA di thread ini pada FINALLY (jalur sukses & exception).
							Common.closeOpenedSession(session);
						}
					}

					ais.action.report.helper.LoadingReportUtil.selesai(label);

				}
			}).start();

			final Timer timer = new Timer(1000);
			timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
			timer.setRepeats(true);
			ais.action.report.helper.LoadingReportUtil.showBusy(label);
			timer.addEventListener("onTimer", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					ais.action.report.helper.LoadingReportUtil.showBusy(label);
					if (ais.action.report.helper.LoadingReportUtil.isSelesai(label)) {
						ais.action.report.helper.LoadingReportUtil.clearBusy();
						Common.clear(center);
						if (fileReport != null) {

							File file = Report.generateCompileFileReport(Report.PDF, parameters, fileReport,
									ais.ui.util.WaktuUtil.getDate());

							CommonReport.tampilkanReportPDF(center, file);

							org.zkoss.zul.North north = ((Borderlayout) center.getParent()).getNorth() == null
									? new org.zkoss.zul.North()
									: ((Borderlayout) center.getParent()).getNorth();
							Common.clear(north);
							north.setParent(center.getParent());
							north.appendChild(CommonReport.exportReport(new ParameterListener() {
								@Override
								public Map generateParameters() throws Exception {
									return parameters;
								}
							}, LampiranLain.FILE_JRXML_LAYOUT_JENIS_NILAI, null, null));

						} else {
							printAmbil.setVisible(true);
							excelku = new ais.ui.util.MySpreadsheet();

							center.appendChild(excelku);
							EcampusUtil.tampilkan(datas, excelku);
							// Tampilkan sebagai grid ringan; Excel tetap utuh saat tombol Download diklik.
							ais.ui.util.PratinjauXlsxHelper.gantiSpreadsheetDenganGrid(excelku);

						}

						ais.action.report.helper.LoadingReportUtil.stopAndDetach(timer);
					}

				}
			});
			timer.start();

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas Excel Laporan Rekap Total Nilai", "Sistem mengalami kendala teknis saat menyusun berkas Excel laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap atau format datanya tidak sesuai dengan yang diharapkan oleh template ekspor.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mengekspor laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini sudah lengkap dan benar, kemudian coba ekspor ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void siswaMk(Session session, KurikulumPunyaMatapelajaran kurikulumPunyaMatapelajaran, Integer smt,
			KelasSiswaPunyaSiswa kelasSiswaPunyaSiswa, Map map, List sub, JSONObject js,
			Map<Long, List<GrupPenilaian>> mapGrup, Map<Long, List<GrupKategoriItemPenilaianSiswa>> mapGrupKat,
			Date sekarang, Map<String, Double> mapsMinPerJenis, Map<String, Double> mapsMaxPerJenis,
			Map<String, Double> mapsTotalPerJenis, Map<String, Integer> mapsJmlPerJenis,
			Map<Long, List<GrupKategoriItemPenilaianSiswa>> mapGrupKatAll) throws Exception {
		JenisPenilaian jenisPenilaian = kurikulumPunyaMatapelajaran.getMatapelajaran().getJenisPenilaian();

		if (kurikulumPunyaMatapelajaran != null && kurikulumPunyaMatapelajaran.getKurikulumSekolah() != null
				&& kurikulumPunyaMatapelajaran.getKurikulumSekolah().getJenisPenilaian() != null) {
			jenisPenilaian = kurikulumPunyaMatapelajaran.getKurikulumSekolah().getJenisPenilaian();
		}

		Siswa siswa = kelasSiswaPunyaSiswa.getSiswa();

		Common.insertProperty(Siswa.class, siswa, map, "siswa");

		Guru guru = (Guru) ConstantValues.simpleObject(session.createCriteria(JadwalPelajaran.class)
				.setProjection(Projections.property("guru.id")).add(Restrictions.eq("semester", smt))
				.add(Restrictions.eq("kelas", kelasSiswaPunyaSiswa.getKelasSiswa()))
				.add(Restrictions.eq("matapelajaran", kurikulumPunyaMatapelajaran.getMatapelajaran())).setMaxResults(1),
				Guru.class, false);

		if (guru != null) {
			String url = CommonMedia.getUrlFotoPengguna(new Tbmuser(guru));
			map.put("url_foto_guru", url);
			map.put("url_foto_guru_" + kurikulumPunyaMatapelajaran.getMatapelajaran().getId(), url);
		}

		map.put("kode_guru", guru == null ? "" : guru.getKode());
		map.put("nip_guru", guru == null ? "" : guru.getNip());
		map.put("nama_guru", guru == null ? "" : guru.getNama());

		map.put("sub_jenis", kurikulumPunyaMatapelajaran.getMatapelajaran().getKelompokMatapelajaran().getNama());

		map.put("matapelajaran", kurikulumPunyaMatapelajaran.getMatapelajaran().getNama());

		map.put("mk", kurikulumPunyaMatapelajaran.getMatapelajaran().getNama());
		map.put("kode_mk", kurikulumPunyaMatapelajaran.getMatapelajaran().getKode());
		map.put("singkatan_mk", kurikulumPunyaMatapelajaran.getMatapelajaran().getSingkatan());
		map.put("jenis_mk_" + jenisPenilaian.getId(), jenisPenilaian.getNama());

		map.put("kode_guru_" + kurikulumPunyaMatapelajaran.getMatapelajaran().getId(),
				guru == null ? "" : guru.getKode());
		map.put("nip_guru_" + kurikulumPunyaMatapelajaran.getMatapelajaran().getId(),
				guru == null ? "" : guru.getNip());
		map.put("nama_guru_" + kurikulumPunyaMatapelajaran.getMatapelajaran().getId(),
				guru == null ? "" : guru.getNama());

		map.put("sub_jenis_" + kurikulumPunyaMatapelajaran.getMatapelajaran().getId(),
				kurikulumPunyaMatapelajaran.getMatapelajaran().getKelompokMatapelajaran().getNama());

		map.put("matapelajaran_" + kurikulumPunyaMatapelajaran.getMatapelajaran().getId(),
				kurikulumPunyaMatapelajaran.getMatapelajaran().getNama());

		map.put("mk_" + kurikulumPunyaMatapelajaran.getMatapelajaran().getId(),
				kurikulumPunyaMatapelajaran.getMatapelajaran().getNama());
		map.put("kode_mk_" + kurikulumPunyaMatapelajaran.getMatapelajaran().getId(),
				kurikulumPunyaMatapelajaran.getMatapelajaran().getKode());
		map.put("singkatan_mk_" + kurikulumPunyaMatapelajaran.getMatapelajaran().getId(),
				kurikulumPunyaMatapelajaran.getMatapelajaran().getSingkatan());
		map.put("jenis_mk_" + jenisPenilaian.getId(), jenisPenilaian.getNama());

		for (GrupPenilaian grupPenilaian : mapGrup.get(jenisPenilaian.getId())) {

			Double total = 0.0;

			Double min = 0.0;

			Double max = 0.0;

			try {

				List<GrupKategoriItemPenilaianSiswa> grupKategoriItemPenilaianSiswasAll = mapGrupKatAll
						.get(grupPenilaian.getId());

				String formula = grupPenilaian.getFormula();

				String target = GrupPenilaianUtil.ambilTarget(formula, sekarang);

				total = kelasSiswaPunyaSiswa.retreiveTotalNilaiTotal(target,
						kurikulumPunyaMatapelajaran.getMatapelajaran(), grupPenilaian, smt,
						grupKategoriItemPenilaianSiswasAll);

				target = GrupPenilaianUtil.ambilTargetMin(formula, sekarang);

				min = kelasSiswaPunyaSiswa.retreiveTotalNilaiTotal(target,
						kurikulumPunyaMatapelajaran.getMatapelajaran(), grupPenilaian, smt,
						grupKategoriItemPenilaianSiswasAll);

				target = GrupPenilaianUtil.ambilTargetMax(formula, sekarang);

				max = kelasSiswaPunyaSiswa.retreiveTotalNilaiTotal(target,
						kurikulumPunyaMatapelajaran.getMatapelajaran(), grupPenilaian, smt,
						grupKategoriItemPenilaianSiswasAll);

			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/sekolah/LaporanRekapTotalNilai.java:1531");
				PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Rekap Total Nilai", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
					new String[] {
						"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
						"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
			}

			map.put("min_" + grupPenilaian.getNama(), min);
			map.put("max_" + grupPenilaian.getNama(), max);

			map.put("grup_" + grupPenilaian.getId(), grupPenilaian.getNama());

			String keyKet = kurikulumPunyaMatapelajaran.getMatapelajaran().getId() + "_" + grupPenilaian.getId();

			String ket = js.isNull(keyKet) ? "" : js.getString(keyKet);

			map.put("keterangan_nilai_" + grupPenilaian.getId(), ket);

			List<GrupKategoriItemPenilaianSiswa> grupKategoriItemPenilaianSiswas = mapGrupKat
					.get(grupPenilaian.getId());

			for (GrupKategoriItemPenilaianSiswa grupKategoriItemPenilaianSiswa : grupKategoriItemPenilaianSiswas) {

				map.put("grup_kategori_" + grupKategoriItemPenilaianSiswa.getId(),
						grupKategoriItemPenilaianSiswa.getNama());

				List<KategoriItemPenilaianSiswa> kategoriItemPenilaianSiswasId = ConstantValues.simpleList(
						session.createCriteria(DetailGrupKategoriItemPenilaianSiswa.class)

								.add(Restrictions.eq("grupKategoriItemPenilaianSiswa", grupKategoriItemPenilaianSiswa))
								.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

								.setProjection(Projections.groupProperty("kategoriItemPenilaianSiswa.id")),
						KategoriItemPenilaianSiswa.class, false);

				List<JenisItemPenilaianSiswa> jenisItemPenilaianSiswas = ConstantValues.simpleList(
						session.createCriteria(JenisItemPenilaianSiswa.class)
								.createAlias("kategoriItemPenilaianSiswa", "kategoriItemPenilaianSiswa")
								.addOrder(Order.asc("kategoriItemPenilaianSiswa.kode")).addOrder(Order.asc("nomorUrut"))
								.add(Restrictions.in("kategoriItemPenilaianSiswa", kategoriItemPenilaianSiswasId))
								.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
						JenisItemPenilaianSiswa.class);
				Double totalLocal = 0.0;

				try {

					String formula = grupKategoriItemPenilaianSiswa.getFormula();
					String target = GrupPenilaianUtil.ambilTarget(formula, sekarang);
					totalLocal = kelasSiswaPunyaSiswa.retreiveTotalNilai(jenisItemPenilaianSiswas, target,
							kurikulumPunyaMatapelajaran.getMatapelajaran(), grupPenilaian,
							grupKategoriItemPenilaianSiswa, smt, true);

				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/sekolah/LaporanRekapTotalNilai.java:1580");
					PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Rekap Total Nilai", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
						new String[] {
							"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
							"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
							"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
						});
				}

				sub.add(totalLocal);

				NilaiHurufSekolah nilaiHurufSekolah = NilaiHurufSekolah.getNilaiHurufSekolah(totalLocal,
						siswa.getTahunMasuk(), siswa.getSekolah(), siswa.getYayasan(),
						kelasSiswaPunyaSiswa.getKelasSiswa().getTahunAjaran(),
						smt % 2 == 0 ? Perkuliahan.GENAP : Perkuliahan.GANJIL, grupPenilaian.getJenisNilaiHuruf());

				sub.add(nilaiHurufSekolah == null ? "" : nilaiHurufSekolah.getNilaiHuruf());

				map.put("total_" + grupKategoriItemPenilaianSiswa.getKode(), totalLocal);
				map.put("huruf_" + grupKategoriItemPenilaianSiswa.getKode(),
						nilaiHurufSekolah == null ? "" : nilaiHurufSekolah.getNilaiHuruf());

				String key = grupKategoriItemPenilaianSiswa.getId() + "_" + kurikulumPunyaMatapelajaran.getId() + "_"
						+ grupPenilaian.getId();
				Double o = mapsTotalPerJenis.get(key);
				o = o == null ? 0.0 : o;
				o += totalLocal;
				mapsTotalPerJenis.put(key, o);

				map.put("nilai_" + key, totalLocal);

				map.put("nilai_mk_" + grupKategoriItemPenilaianSiswa.getId() + "_"
						+ kurikulumPunyaMatapelajaran.getMatapelajaran().getId() + "_" + jenisPenilaian.getId(),
						totalLocal);

				o = mapsMinPerJenis.get(key);
				o = o == null ? 100.0 : o;

				if (o >= totalLocal) {
					mapsMinPerJenis.put(key, totalLocal);
				}

				map.put("min_" + key, totalLocal);

				map.put("min_mk_" + grupKategoriItemPenilaianSiswa.getId() + "_"
						+ kurikulumPunyaMatapelajaran.getMatapelajaran().getId() + "_" + jenisPenilaian.getId(),
						totalLocal);

				o = mapsMaxPerJenis.get(key);
				o = o == null ? 0.0 : o;

				if (o <= totalLocal) {
					mapsMaxPerJenis.put(key, totalLocal);
				}

				map.put("max_" + key, totalLocal);

				map.put("max_mk_" + grupKategoriItemPenilaianSiswa.getId() + "_"
						+ kurikulumPunyaMatapelajaran.getMatapelajaran().getId() + "_" + jenisPenilaian.getId(),
						totalLocal);

				Integer p = mapsJmlPerJenis.get(key);
				p = p == null ? 0 : p;
				p += 1;
				mapsJmlPerJenis.put(key, p);

				map.put("jml_" + key, p);

				map.put("jml_mk_" + grupKategoriItemPenilaianSiswa.getId() + "_"
						+ kurikulumPunyaMatapelajaran.getMatapelajaran().getId() + "_" + jenisPenilaian.getId(), p);

				for (JenisItemPenilaianSiswa jenisItemPenilaianSiswa : jenisItemPenilaianSiswas) {

					Boolean sesuai = kelasSiswaPunyaSiswa.retreiveDetailVerify(jenisItemPenilaianSiswa,
							grupKategoriItemPenilaianSiswa, kurikulumPunyaMatapelajaran.getMatapelajaran(), smt);

					key = grupKategoriItemPenilaianSiswa.getKode() + "_" + jenisItemPenilaianSiswa.getKode();
					String val = kelasSiswaPunyaSiswa.retreiveDetailNilai(jenisItemPenilaianSiswa,
							grupKategoriItemPenilaianSiswa, kurikulumPunyaMatapelajaran.getMatapelajaran(), smt, null);

					if (val == null || val.trim().isEmpty()) {
						val = "0.0";
					}

					map.put(key + "_ok", sesuai);

					Double nilaiAngka = 0.0;
					try {
						if (Common.isNumber(val)) {
							nilaiAngka = Double.parseDouble(val);
						}
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/sekolah/LaporanRekapTotalNilai.java:1665");
						// TODO: handle exception
					}

					map.put(key, val);
					map.put(key + "_angka", nilaiAngka);

					if (totalLocal.intValue() == min.intValue()) {
						map.put(jenisItemPenilaianSiswa.getKode() + "_min_" + grupPenilaian.getId(), val);
					}
					if (totalLocal.intValue() == max.intValue()) {
						map.put(jenisItemPenilaianSiswa.getKode() + "_max_" + grupPenilaian.getId(), val);
					}

				}

			}

			Double totalsemua = mapsTotalPerJenis.get("totalsemua");
			totalsemua = totalsemua == null ? 0.0 : totalsemua;
			totalsemua += total;
			mapsTotalPerJenis.put("totalsemua", totalsemua);

			totalsemua = mapsTotalPerJenis.get("totalsemua_" + kelasSiswaPunyaSiswa.getSiswa().getId());
			totalsemua = totalsemua == null ? 0.0 : totalsemua;
			totalsemua += total;
			mapsTotalPerJenis.put("totalsemua_" + kelasSiswaPunyaSiswa.getSiswa().getId(), totalsemua);

			Double jml = mapsTotalPerJenis.get("jml");
			jml = jml == null ? 0.0 : jml;
			jml += 1.0;
			mapsTotalPerJenis.put("jml", jml);

			jml = mapsTotalPerJenis.get("jml_" + kelasSiswaPunyaSiswa.getSiswa().getId());
			jml = jml == null ? 0.0 : jml;
			jml += 1.0;
			mapsTotalPerJenis.put("jml_" + kelasSiswaPunyaSiswa.getSiswa().getId(), jml);

			NilaiHurufSekolah nilaiHurufSekolah = NilaiHurufSekolah.getNilaiHurufSekolah(total, siswa.getTahunMasuk(),
					siswa.getSekolah(), siswa.getYayasan(), kelasSiswaPunyaSiswa.getKelasSiswa().getTahunAjaran(),
					smt % 2 == 0 ? Perkuliahan.GENAP : Perkuliahan.GANJIL, grupPenilaian.getJenisNilaiHuruf());

			if (searchtotal.isChecked())
				sub.add("**" + Common.numberFormat.get().format(total));
			if (searchmin.isChecked())
				sub.add("**" + Common.numberFormat.get().format(min));
			if (searchmax.isChecked())
				sub.add("**" + Common.numberFormat.get().format(max));

			if (searchtotal.isChecked())
				sub.add("**" + (nilaiHurufSekolah == null ? "" : nilaiHurufSekolah.getNilaiHuruf()));

			String key = "total_" + kurikulumPunyaMatapelajaran.getId() + "_" + grupPenilaian.getId();
			Double o = mapsTotalPerJenis.get(key);
			o = o == null ? 0.0 : o;
			o += total;
			mapsTotalPerJenis.put(key, o);

			map.put(key, total);

			map.put("total_mk_" + kurikulumPunyaMatapelajaran.getMatapelajaran().getId() + "_" + grupPenilaian.getId(),
					total);
			map.put("total_huruf_mk_" + kurikulumPunyaMatapelajaran.getMatapelajaran().getId() + "_"
					+ grupPenilaian.getId(), nilaiHurufSekolah == null ? "" : nilaiHurufSekolah.getNilaiHuruf());

			map.put("total_" + grupPenilaian.getId(), total);
			map.put("total_huruf_" + grupPenilaian.getId(),
					nilaiHurufSekolah == null ? "" : nilaiHurufSekolah.getNilaiHuruf());

			o = mapsMinPerJenis.get(key);
			o = o == null ? 100.0 : o;

			if (o >= total) {
				mapsMinPerJenis.put(key, total);
			}

			o = mapsMaxPerJenis.get(key);
			o = o == null ? 0.0 : o;

			if (o <= total) {
				mapsMaxPerJenis.put(key, total);
			}

			key = "min_" + kurikulumPunyaMatapelajaran.getId() + "_" + grupPenilaian.getId();
			o = mapsTotalPerJenis.get(key);
			o = o == null ? 0.0 : o;
			o += min;
			mapsTotalPerJenis.put(key, o);

			map.put("total_" + key, min);

			map.put("min_" + grupPenilaian.getId(), min);

			map.put("min_mk_" + kurikulumPunyaMatapelajaran.getMatapelajaran().getId() + "_" + grupPenilaian.getId(),
					min);

			o = mapsMinPerJenis.get(key);
			o = o == null ? 100.0 : o;

			if (o >= min) {
				mapsMinPerJenis.put(key, min);
			}

			o = mapsMaxPerJenis.get(key);
			o = o == null ? 0.0 : o;

			if (o <= min) {
				mapsMaxPerJenis.put(key, min);
			}

			key = "max_" + kurikulumPunyaMatapelajaran.getId() + "_" + grupPenilaian.getId();
			o = mapsTotalPerJenis.get(key);
			o = o == null ? 0.0 : o;
			o += max;
			mapsTotalPerJenis.put(key, o);

			if (o >= max) {
				mapsMinPerJenis.put(key, max);
			}
			map.put("max_" + grupPenilaian.getId(), max);
			map.put("max_mk_" + kurikulumPunyaMatapelajaran.getMatapelajaran().getId() + "_" + grupPenilaian.getId(),
					max);

			map.put("total_" + key, min);

			o = mapsMaxPerJenis.get(key);
			o = o == null ? 0.0 : o;

			if (o <= max) {
				mapsMaxPerJenis.put(key, max);
			}

		}
	}
}
