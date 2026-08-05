package ais.action.report.format1.payroll;
import ais.common.PesanFormalHelper;

import java.io.ByteArrayOutputStream;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
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
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.LayoutRegion;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.West;

import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Pegawai;
import ais.database.model.Tbmuser;
import ais.database.model.payroll.FormatItemGaji;
import ais.database.model.payroll.ItemGaji;
import ais.database.model.payroll.RencanaGajiPunyaPegawai;
import ais.database.model.rab.SatuanKerja;
import ais.ui.util.EcampusUtil;
import ais.ui.util.MyJSONObject;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class LaporanRekapRencanaGaji extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3331244819198611604L;

	private Spreadsheet excelku;

	private MyTextbox searchnama = new MyTextbox();

	private Intbox tahun = new Intbox(Calendar.getInstance().get(Calendar.YEAR));
	private AmbilDataSatuanKerjaBanbox searchparent;

	private SatuanKerjaTreeModel satuanKerjaTreeModel;

	private Center center;

	private MyToolbarbuttonConfig printAmbil;

	public LaporanRekapRencanaGaji() throws Exception {
		super();
		init();
	}

	public LaporanRekapRencanaGaji(String title, String border, boolean closable) throws Exception {
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

		SatuanKerja satuanKerja = Common.getSatuanKerja();
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

		MyFormRow row = new MyFormRow();row.setValign("top");
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
		vbox.appendChild(tahun);
		tahun.setCols(5);

		row = new MyFormRow();
		row.setParent(rows);
		vbox = new Vbox();
		vbox.setParent(row);
		vbox.setWidth("100%");
		vbox.appendChild(new ais.ui.util.MyLabelConfig("Satuan Kerja"));
		vbox.appendChild(searchparent);
		searchparent.setCols(5);
		searchparent.setReadonly(true);

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
					Filedownload.save(bout.toByteArray(), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "rencana_gaji.xlsx");
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/payroll/LaporanRekapRencanaGaji.java:189");
					PesanFormalHelper.tampilkanGagalException("pembuatan berkas Excel Laporan Rekap Rencana Gaji", "Sistem mengalami kendala teknis saat menyusun berkas Excel laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap atau format datanya tidak sesuai dengan yang diharapkan oleh template ekspor.", e,
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

					Integer thn = tahun.getValue();

					datas = new ArrayList<List>();

					SatuanKerja parent = (SatuanKerja) searchparent.getAttribute("satuanKerja");
					Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
					if (parent != null) {
						satuanKerjas.clear(); satuanKerjas.add(parent);
						satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
					}

					Session session = HibernateUtil.currentSession();
					List<RencanaGajiPunyaPegawai> rencanaGajiPunyaPegawais = session
							.createCriteria(RencanaGajiPunyaPegawai.class).createAlias("rencanaGaji", "rencanaGaji")

							.createAlias("pegawai", "pegawai")
							
							.add(Restrictions.isNotNull("pegawai.formatItemGaji")).add(Restrictions.eq("pegawai.aktif",true))

							.add(satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
									: Restrictions.in("pegawai.satuanKerja", satuanKerjas))

							.add(thn == null ? Restrictions.sqlRestriction("true")
									: Restrictions.eq("rencanaGaji.tahun", thn))

							.addOrder(Order.asc("rencanaGaji.tahun")).addOrder(Order.asc("pegawai.nama"))

							.add(nama.isEmpty() ? Restrictions.sqlRestriction("true")
									: Restrictions.or(Restrictions.ilike("pegawai.nama", nama, MatchMode.ANYWHERE),
											Restrictions.ilike("pegawai.code", nama, MatchMode.ANYWHERE)))

							.list();

					List<FormatItemGaji> formatItemGajis = session.createCriteria(RencanaGajiPunyaPegawai.class)

							.createAlias("rencanaGaji", "rencanaGaji")

							.createAlias("pegawai", "pegawai")
							
							.add(Restrictions.isNotNull("pegawai.formatItemGaji")).add(Restrictions.eq("pegawai.aktif",true))

							.add(satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
									: Restrictions.in("pegawai.satuanKerja", satuanKerjas))

							.add(thn == null ? Restrictions.sqlRestriction("true")
									: Restrictions.eq("rencanaGaji.tahun", thn))

							.add(nama.isEmpty() ? Restrictions.sqlRestriction("true")
									: Restrictions.or(Restrictions.ilike("pegawai.nama", nama, MatchMode.ANYWHERE),
											Restrictions.ilike("pegawai.code", nama, MatchMode.ANYWHERE)))

							.add(Restrictions.isNotNull("pegawai.formatItemGaji")).add(Restrictions.eq("pegawai.aktif",true))
							.setProjection(Projections.groupProperty("pegawai.formatItemGaji")).list();

					List<ItemGaji> itemGajis = session.createCriteria(ItemGaji.class)
							.add(formatItemGajis.isEmpty() ? Restrictions.sqlRestriction("false")
									: Restrictions.in("formatItemGaji", formatItemGajis))
							.list();

					TreeMap<String, ItemGaji> myItems = new TreeMap<String, ItemGaji>();
					NumberFormat nf = new DecimalFormat("000");
					for (ItemGaji itemGaji : itemGajis) {
						String kode = nf.format(itemGaji.getNomorUrut()) + "-" + itemGaji.getKode();
						myItems.put(kode, itemGaji);
					}

					String currentCaraRencanaGaji = null;
					Map<Integer, Double> mapsTotal = new HashMap<Integer, Double>();
					Map<Integer, Double> mapsTotalPerJenis = null;
					int nomor = 1;
					for (ItemGaji itemGaji : myItems.values()) {

						String n = itemGaji.getKode() + " " + itemGaji.getNama();

						if (currentCaraRencanaGaji == null || !currentCaraRencanaGaji.equals(n)) {

							try {

								if (mapsTotalPerJenis != null) {

									try {

										ArrayList sub = new ArrayList();
										sub.add("");
										sub.add("Total");
										sub.add("");
										sub.add("");
										Double tott = 0.0;
										for (int bulan = 1; bulan <= 12; bulan++) {
											Double totalSemua = mapsTotalPerJenis.get(bulan);
											if (totalSemua == null) {
												totalSemua = 0.0;
											}
											tott += totalSemua;
											sub.add(totalSemua);
										}
										sub.add(tott);
										datas.add(sub);

									} catch (Exception e) {
										Common.tampilErrorJikaAdmin(e);
										PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Rekap Rencana Gaji", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
												new String[] {
													"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
													"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
													"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
												});
									}

								}

								List sub = new ArrayList();

								sub.add("");
								sub.add("");
								sub.add("");
								sub.add("");
								for (int bulan = 1; bulan <= 12; bulan++) {
									sub.add("");
								}
								sub.add("");

								datas.add(sub);

								sub = new ArrayList();

								sub.add("");
								sub.add(n);
								sub.add("");
								sub.add("");
								for (int bulan = 1; bulan <= 12; bulan++) {
									sub.add("");
								}
								sub.add("");

								datas.add(sub);

								sub = new ArrayList();
								sub.add("**No.");
								sub.add("**Kode");
								sub.add("**Nama");
								sub.add("**NPWP");

								for (int bulan = 1; bulan <= 12; bulan++) {
									sub.add("**"+Common.BULAN[bulan - 1]);
								}
								sub.add("Total");

								datas.add(sub);

							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/payroll/LaporanRekapRencanaGaji.java:365");
								// TODO: handle exception
							}

							currentCaraRencanaGaji = n;
							mapsTotalPerJenis = new HashMap<Integer, Double>();
							nomor = 1;
						}

						for (RencanaGajiPunyaPegawai rencanaGajiPunyaPegawai : rencanaGajiPunyaPegawais) {
							List sub = new ArrayList();

							try {
								MyJSONObject jsonObject = new MyJSONObject(rencanaGajiPunyaPegawai.getKomponenGaji());
								Pegawai pegawai = rencanaGajiPunyaPegawai.getPegawai();

								label.setValue("Sedang memproses data " + pegawai.toString());

								sub.add(nomor);
								sub.add(pegawai.getCode().isEmpty() ? pegawai.getMycode() : pegawai.getCode());
								sub.add(pegawai.getNama());
								sub.add(pegawai.getNpwp());

								Double tott = 0.0;
								for (int bulan = 1; bulan <= 12; bulan++) {
									if (!jsonObject.isNull(itemGaji.getKode() + "_" + bulan)) {
										Double nilai = Double.parseDouble(
												jsonObject.get(itemGaji.getKode() + "_" + bulan).toString());
										sub.add(nilai);

										Double totalSemua = mapsTotal.get(bulan);
										if (totalSemua == null) {
											totalSemua = 0.0;
										}
										tott += nilai;
										totalSemua += nilai;
										mapsTotal.put(bulan, totalSemua);

										if (mapsTotalPerJenis != null) {
											Double totalSemuaPerJenis = mapsTotalPerJenis.get(bulan);
											if (totalSemuaPerJenis == null) {
												totalSemuaPerJenis = 0.0;
											}

											totalSemuaPerJenis += nilai;
											mapsTotalPerJenis.put(bulan, totalSemuaPerJenis);
										}

									} else {
										sub.add(0.0);
									}
								}
								sub.add(tott);
								System.out.println("sub =>" + sub);
								datas.add(sub);

								nomor++;

							} catch (Exception e) {
								Common.tampilErrorJikaAdmin(e);
								PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Rekap Rencana Gaji", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
										new String[] {
											"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
											"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
											"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
										});
							}
						}
					}

					if (mapsTotalPerJenis != null) {

						try {

							ArrayList sub = new ArrayList();
							sub.add("");
							sub.add("");
							sub.add("Total");
							sub.add("");
							Double tott = 0.0;
							for (int bulan = 1; bulan <= 12; bulan++) {
								Double totalSemua = mapsTotalPerJenis.get(bulan);
								if (totalSemua == null) {
									totalSemua = 0.0;
								}
								tott += totalSemua;
								sub.add(totalSemua);
							}
							sub.add(tott);
							datas.add(sub);

						} catch (Exception e) {
							Common.tampilErrorJikaAdmin(e);
							PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Rekap Rencana Gaji", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
									new String[] {
										"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
										"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
										"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
									});
						}

					}

					try {

						ArrayList sub = new ArrayList();
						sub.add("");
						sub.add("");
						sub.add("Total Keseluruhan");
						sub.add("");
						Double tott = 0.0;
						for (int bulan = 1; bulan <= 12; bulan++) {
							Double totalSemua = mapsTotal.get(bulan);
							if (totalSemua == null) {
								totalSemua = 0.0;
							}
							tott += totalSemua;
							sub.add(totalSemua);
						}
						sub.add(tott);
						datas.add(sub);

					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);
						PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Rekap Rencana Gaji", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
								new String[] {
									"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
									"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
									"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
								});
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
						// Tampilkan sebagai grid ringan; Excel tetap utuh saat tombol Download diklik.
						ais.ui.util.PratinjauXlsxHelper.gantiSpreadsheetDenganGrid(excelku);
						printAmbil.setVisible(true);
						ais.action.report.helper.LoadingReportUtil.stopAndDetach(timer);
					}

				}
			});
			timer.start();

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas Excel Laporan Rekap Rencana Gaji", "Sistem mengalami kendala teknis saat menyusun berkas Excel laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap atau format datanya tidak sesuai dengan yang diharapkan oleh template ekspor.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mengekspor laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini sudah lengkap dan benar, kemudian coba ekspor ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

}
