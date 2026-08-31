package ais.action.report.format1.kpi;
import ais.common.PesanFormalHelper;

import java.io.ByteArrayOutputStream;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zss.ui.Spreadsheet;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Label;
import org.zkoss.zul.LayoutRegion;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.West;

import ais.action.master.kpi.helper.AmbilDataFormatKpiBanbox;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Pegawai;
import ais.database.model.Tbmuser;
import ais.database.model.kpi.FormatKpi;
import ais.database.model.kpi.FormatKpiDetail;
import ais.database.model.kpi.ItemKpi;
import ais.database.model.kpi.NilaiKpi;
import ais.database.model.rab.SatuanKerja;
import ais.ui.util.EcampusUtil;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Penyusun/penyaji laporan untuk laporan rekap kpi. Kelas ini mengubah data domain menjadi bentuk
 * laporan yang dipakai UI, ekspor, atau proses cetak tanpa memindahkan aturan transaksi ke lapisan
 * report.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Spreadsheet excelku}, {@code
 * AmbilDataFormatKpiBanbox searchFormatKpi}, {@code MyTextbox searchnama}, {@code Combobox ta}, {@code
 * AmbilDataSatuanKerjaBanbox searchparent}, {@code SatuanKerjaTreeModel satuanKerjaTreeModel}, {@code Center
 * center}, {@code MyToolbarbuttonConfig printAmbil}; inisialisasi/lifecycle ({@code init()}); pelaporan/ekspor
 * ({@code onCetak()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di
 * atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class LaporanRekapKpi extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3331244819198611604L;

	private Spreadsheet excelku;
	private AmbilDataFormatKpiBanbox searchFormatKpi = new AmbilDataFormatKpiBanbox();
	private MyTextbox searchnama = new MyTextbox();

	private Combobox ta = new Combobox();
	private AmbilDataSatuanKerjaBanbox searchparent;

	private SatuanKerjaTreeModel satuanKerjaTreeModel;

	private Center center;

	private MyToolbarbuttonConfig printAmbil;

	public LaporanRekapKpi() throws Exception {
		super();
		init();
	}

	public LaporanRekapKpi(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);
		init();
	}

	private void init() throws Exception {

		searchparent = new AmbilDataSatuanKerjaBanbox();
		searchparent.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onCetak(arg0);
			}
		});

		satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);

		Common.generateTahunAjaran(ta);
		ta.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onCetak(arg0);
			}
		});

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);

		LayoutRegion west = Common.isMobile() ? new North() : new West();
		west.setTitle("Menu");
		west.setCollapsible(true);
		west.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(west, true);
		if (Common.isMobile()) {
			west.setHeight("250px");
			west.setOpen(false);
		} else {
			west.setWidth("150px");
		}

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
		vbox.appendChild(new ais.ui.util.MyLabelConfig("Pegawai"));
		vbox.appendChild(searchnama);
		searchnama.setCols(5);

		row = new MyFormRow();
		row.setParent(rows);
		vbox = new Vbox();
		vbox.setParent(row);
		vbox.setWidth("100%");
		vbox.appendChild(new ais.ui.util.MyLabelConfig("Tahun"));
		vbox.appendChild(ta);
		ta.setCols(5);

		row = new MyFormRow();
		row.setParent(rows);
		vbox = new Vbox();
		vbox.setParent(row);
		vbox.setWidth("100%");
		vbox.appendChild(new ais.ui.util.MyLabelConfig("Format"));
		vbox.appendChild(searchFormatKpi);
		searchFormatKpi.setCols(5);
		searchFormatKpi.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		vbox = new Vbox();
		vbox.setParent(row);
		vbox.setWidth("100%");
		vbox.appendChild(new ais.ui.util.MyLabelConfig("Satuan Kerja"));
		vbox.appendChild(searchparent);
		searchparent.setCols(5);
		searchparent.setReadonly(true);

		SatuanKerja satuanKerja = Common.getSatuanKerja();

		SatuanKerja satuanKerjaData = satuanKerja;
		Tbmuser tbmuser = Common.getCurrentUser();
		if (satuanKerjaData != null && tbmuser != null && tbmuser.hakAkses() != null
				&& !tbmuser.hakAkses().getMelihatDataSatkerLain()) {
			searchparent.setValue(satuanKerjaData.getNama());
			searchparent.setAttribute("satuanKerja", satuanKerjaData);
			searchparent.setAttribute("myValue", satuanKerjaData);
			searchparent.setDisabled(true);
		}

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
							"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "kpi.xlsx");
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/kpi/LaporanRekapKpi.java:212");
					PesanFormalHelper.tampilkanGagalException("pembuatan berkas Excel Laporan Rekap Kpi", "Sistem mengalami kendala teknis saat menyusun berkas Excel laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap atau format datanya tidak sesuai dengan yang diharapkan oleh template ekspor.", e,
						new String[] {
							"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mengekspor laporan ini.",
							"Pastikan data yang menjadi sumber laporan ini sudah lengkap dan benar, kemudian coba ekspor ulang.",
							"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
						});

				}
			}
		});
		printAmbil.setParent(toolbar);

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

			final Label label = new Label(ais.common.Common.getBahasaConfig("Proses load data .."));

			new Thread(new Runnable() {

				@Override
				public void run() {

					String nama = searchnama.getValue().trim();
					String t = (String) ta.getSelectedItem().getValue();
					FormatKpi formatKpi = (FormatKpi) searchFormatKpi.getAttribute("formatKpi");

					datas = new ArrayList<List>();

					SatuanKerja parent = (SatuanKerja) searchparent.getAttribute("satuanKerja");
					Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
					if (parent != null) {
						satuanKerjas.clear();
						satuanKerjas.add(parent);
						satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
					}

					Session session = HibernateUtil.currentSession();

					List<Object[]> formatKpiDetail = session.createCriteria(FormatKpiDetail.class)
							.setProjection(Projections.projectionList().add(Projections.property("formatKpi.id"))
									.add(Projections.property("pegawai.id")))
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).list();
					Set<String> ds = new HashSet<String>();
					for (Object[] objects : formatKpiDetail) {
						String key = objects[0] + "_" + objects[1];
						ds.add(key);
					}

					List<NilaiKpi> nilaiKpis = session.createCriteria(NilaiKpi.class)

							.add(formatKpi == null ? Restrictions.sqlRestriction("true")
									: Restrictions.eq("itemKpi.formatKpi", formatKpi))

							.createAlias("itemKpi", "itemKpi").createAlias("itemKpi.formatKpi", "formatKpi")
							.createAlias("penilaianKpi", "penilaianKpi").createAlias("penilaianKpi.pegawai", "pegawai")

							.add(satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
									: Restrictions.in("pegawai.satuanKerja", satuanKerjas))

							.add(t == null ? Restrictions.sqlRestriction("true")
									: Restrictions.eq("penilaianKpi.ta", t))

							.addOrder(Order.asc("formatKpi.nama")).addOrder(Order.asc("penilaianKpi.ta"))
							.addOrder(Order.asc("pegawai.nama"))

							.add(nama.isEmpty() ? Restrictions.sqlRestriction("true")
									: Restrictions.or(Restrictions.ilike("pegawai.nama", nama, MatchMode.ANYWHERE),
											Restrictions.ilike("pegawai.code", nama, MatchMode.ANYWHERE)))

							.list();

					Map<String, Map<Long, List<NilaiKpi>>> treeMap = new TreeMap<String, Map<Long, List<NilaiKpi>>>();

					for (NilaiKpi nilaiKpi : nilaiKpis) {
						try {
							FormatKpi formatKpiData = nilaiKpi.getItemKpi().getFormatKpi();
							Pegawai pegawai = nilaiKpi.getPenilaianKpi().getPegawai();
							if (ds.contains(formatKpiData.getId() + "_" + pegawai.getId())) {
								String key = formatKpiData.getId() + "_" + nilaiKpi.getPenilaianKpi().getTa();

								Map<Long, List<NilaiKpi>> map = treeMap.get(key);

								if (map == null) {
									map = new HashMap<Long, List<NilaiKpi>>();
									treeMap.put(key, map);
								}

								List<NilaiKpi> nilaiKpisData = map.get(pegawai.getId());
								if (nilaiKpisData == null) {
									nilaiKpisData = new ArrayList<NilaiKpi>();
									map.put(pegawai.getId(), nilaiKpisData);
								}
								nilaiKpisData.add(nilaiKpi);
							}
						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/kpi/LaporanRekapKpi.java:315");
							PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Rekap Kpi", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
								new String[] {
									"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
									"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
									"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
								});
						}

					}

					String currentCaraPembayaranGaji = null;
					Map<Long, Double> mapsTotal = new HashMap<Long, Double>();
					Map<Long, Double> mapsTotalRealisasi = new HashMap<Long, Double>();
					Map<Long, Double> mapsTotalPerJenis = null;
					Map<Long, Double> mapsTotalPerJenisRealisasi = null;
					int nomor = 1;
					for (String key : treeMap.keySet()) {
						String[] formats = key.split("_");
						FormatKpi formatKpiData = (FormatKpi) ConstantValues.ambil(FormatKpi.class.getName(),
								Long.parseLong(formats[0]));
						String n = formatKpiData.getNama() + " tahun " + formats[1];

						Map<Long, List<NilaiKpi>> nilaiKpisData = treeMap.get(key);

						TreeMap<String, ItemKpi> myItems = new TreeMap<String, ItemKpi>();
						NumberFormat nf = new DecimalFormat("000");

						if (currentCaraPembayaranGaji == null || !currentCaraPembayaranGaji.equals(n)) {

							try {

								if (mapsTotalPerJenis != null) {

									try {

										ArrayList sub = new ArrayList();
										sub.add("");
										sub.add("Total");
										sub.add("");
										sub.add("");

										for (ItemKpi itemKpi : myItems.values()) {
											Double totalSemua = mapsTotalPerJenis.get(itemKpi.getId());
											if (totalSemua == null) {
												totalSemua = 0.0;
											}
											Double totalSemuaRealisasi = mapsTotalPerJenisRealisasi
													.get(itemKpi.getId());
											if (totalSemuaRealisasi == null) {
												totalSemuaRealisasi = 0.0;
											}

											sub.add(Common.numberFormat.get().format(totalSemua) + "/"
													+ Common.numberFormat.get().format(totalSemuaRealisasi) + "/"
													+ Common.numberFormat.get().format(totalSemua.intValue() == 0 ? 0.0
															: ((totalSemuaRealisasi * 100.0) / totalSemua))
													+ "%");
										}

										datas.add(sub);

									} catch (Exception e) {
										Common.tampilErrorJikaAdmin(e);
										PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Rekap Kpi", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
												new String[] {
													"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
													"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
													"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
												});
									}

								}

								for (Long pegId : nilaiKpisData.keySet()) {
									List<NilaiKpi> nilaiKpisDataOke = nilaiKpisData.get(pegId);
									for (NilaiKpi nilaiKpi : nilaiKpisDataOke) {
										ItemKpi itemKpi = nilaiKpi.getItemKpi();
										if (itemKpi != null) {
											String kode = nf.format(itemKpi.getNomorUrut()) + "-" + itemKpi.getId();
											myItems.put(kode, itemKpi);
										}

									}
								}

								List sub = new ArrayList();

								sub.add("");
								sub.add("");
								sub.add("");
								sub.add("");
								for (@SuppressWarnings("unused")
								ItemKpi itemKpi : myItems.values()) {
									sub.add("");
								}

								datas.add(sub);

								sub = new ArrayList();

								sub.add("");
								sub.add(n);
								sub.add("");
								sub.add("");
								for (@SuppressWarnings("unused")
								ItemKpi itemKpi : myItems.values()) {
									sub.add("");
								}

								datas.add(sub);

								sub = new ArrayList();
								sub.add("**No.");
								sub.add("**Kode");
								sub.add("**Nama");
								sub.add("**NPWP");

								for (ItemKpi itemKpi : myItems.values()) {
									sub.add("**" + itemKpi.getNama());
								}

								datas.add(sub);

							} catch (Exception e) {
								e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/kpi/LaporanRekapKpi.java:428");
							}

							currentCaraPembayaranGaji = n;
							mapsTotalPerJenis = new HashMap<Long, Double>();
							mapsTotalPerJenisRealisasi = new HashMap<Long, Double>();
							nomor = 1;
						}

						for (Long pegId : nilaiKpisData.keySet()) {

							List sub = new ArrayList();

							List<NilaiKpi> nilaiKpisDataOke = nilaiKpisData.get(pegId);

							try {

								Pegawai pegawai = (Pegawai) ConstantValues.ambil(Pegawai.class.getName(), pegId);

								label.setValue("Sedang memproses data " + pegawai.toString());

								sub.add(nomor);
								sub.add(pegawai.getCode().isEmpty() ? pegawai.getMycode() : pegawai.getCode());
								sub.add(pegawai.getNama());
								sub.add(pegawai.getNpwp());

								for (ItemKpi itemKpi : myItems.values()) {

									Double target = 0.0;
									Double realisasi = 0.0;

									for (NilaiKpi nilaiKpi : nilaiKpisDataOke) {
										if (itemKpi.getId().equals(nilaiKpi.getItemKpi().getId())) {
											target = nilaiKpi.getItemKpi().getTarget();
											realisasi = nilaiKpi.getRealisasi();
										}
									}

									sub.add(Common.numberFormat.get().format(target) + "/"
											+ Common.numberFormat.get().format(realisasi) + "/"
											+ Common.numberFormat.get().format(
													target.intValue() == 0 ? 0.0 : ((realisasi * 100.0) / target))
											+ "%");

									Double totalSemua = mapsTotal.get(itemKpi.getId());
									if (totalSemua == null) {
										totalSemua = 0.0;
									}

									totalSemua += target;
									mapsTotal.put(itemKpi.getId(), totalSemua);

									totalSemua = mapsTotalRealisasi.get(itemKpi.getId());
									if (totalSemua == null) {
										totalSemua = 0.0;
									}

									totalSemua += target;
									mapsTotalRealisasi.put(itemKpi.getId(), totalSemua);

									if (mapsTotalPerJenis != null) {
										Double totalSemuaPerJenis = mapsTotalPerJenis.get(itemKpi.getId());
										if (totalSemuaPerJenis == null) {
											totalSemuaPerJenis = 0.0;
										}

										totalSemuaPerJenis += target;
										mapsTotalPerJenis.put(itemKpi.getId(), totalSemuaPerJenis);

										totalSemuaPerJenis = mapsTotalPerJenisRealisasi.get(itemKpi.getId());
										if (totalSemuaPerJenis == null) {
											totalSemuaPerJenis = 0.0;
										}

										totalSemuaPerJenis += realisasi;
										mapsTotalPerJenisRealisasi.put(itemKpi.getId(), totalSemuaPerJenis);
									}

								}

								System.out.println("sub =>" + sub);
								datas.add(sub);

								nomor++;

							} catch (Exception e) {
								Common.tampilErrorJikaAdmin(e);
								PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Rekap Kpi", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
										new String[] {
											"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
											"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
											"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
										});
							}
						}

						if (mapsTotalPerJenis != null) {

							try {

								ArrayList sub = new ArrayList();
								sub.add("");
								sub.add("");
								sub.add("Total");
								sub.add("");

								for (ItemKpi itemKpi : myItems.values()) {
									Double totalSemua = mapsTotalPerJenis.get(itemKpi.getId());
									if (totalSemua == null) {
										totalSemua = 0.0;
									}
									Double totalSemuaRealisasi = mapsTotalPerJenisRealisasi.get(itemKpi.getId());
									if (totalSemuaRealisasi == null) {
										totalSemuaRealisasi = 0.0;
									}

									sub.add(Common.numberFormat.get().format(totalSemua) + "/"
											+ Common.numberFormat.get().format(totalSemuaRealisasi) + "/"
											+ Common.numberFormat.get().format(totalSemua.intValue() == 0 ? 0.0
													: ((totalSemuaRealisasi * 100.0) / totalSemua))
											+ "%");
								}

								datas.add(sub);

							} catch (Exception e) {
								Common.tampilErrorJikaAdmin(e);
								PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Rekap Kpi", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
										new String[] {
											"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
											"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
											"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
										});
							}

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
						excelku = new ais.ui.util.MySpreadsheet();
						center.appendChild(excelku);
						EcampusUtil.tampilkan(datas, excelku);
						printAmbil.setVisible(true);
						ais.action.report.helper.LoadingReportUtil.stopAndDetach(timer);
					}

				}
			});
			timer.start();

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas Excel Laporan Rekap Kpi", "Sistem mengalami kendala teknis saat menyusun berkas Excel laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap atau format datanya tidak sesuai dengan yang diharapkan oleh template ekspor.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mengekspor laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini sudah lengkap dan benar, kemudian coba ekspor ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

}
