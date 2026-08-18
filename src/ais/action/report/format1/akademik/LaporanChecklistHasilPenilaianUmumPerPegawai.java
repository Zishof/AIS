package ais.action.report.format1.akademik;
import ais.common.PesanFormalHelper;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
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
import org.zkoss.zul.LayoutRegion;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.West;

import ais.common.Common;
import ais.common.CommonSearchFilterHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.ChecklistHasilPenilaianUmum;
import ais.database.model.ChecklistPenilaianUmum;
import ais.database.model.GrupChecklistPenilaianUmum;
import ais.database.model.Pegawai;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmuser;
import ais.ui.util.EcampusUtil;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class LaporanChecklistHasilPenilaianUmumPerPegawai extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3331244819198611604L;

	private Spreadsheet excelku;

	private Center center;

	private MyToolbarbuttonConfig printAmbil;

	private Combobox fakultas;

	private Combobox jurusan;

	private Combobox searchta;

	private Combobox searchsmt;

	private Tbmuser tbmuser;

	public LaporanChecklistHasilPenilaianUmumPerPegawai() throws Exception {
		super();
		init();
	}

	public LaporanChecklistHasilPenilaianUmumPerPegawai(String title, String border, boolean closable)
			throws Exception {
		super(title, border, closable);
		init();
	}

	private void init() throws Exception {
		setHeight("100%");
		setWidth("100%");
		setBorder("none");
		setClosable(false);
		setPosition("center");

		tbmuser = Common.getCurrentUser();

		fakultas = new Combobox();
		jurusan = new Combobox();

		searchsmt = new Combobox();
		searchta = new Combobox();

		Common.generateTahunAjaran(searchta);

		Comboitem comboitem = new Comboitem(Perkuliahan.GANJIL);
		comboitem.setValue(Perkuliahan.GANJIL);
		searchsmt.appendChild(comboitem);
		comboitem = new Comboitem(Perkuliahan.GENAP);
		comboitem.setValue(Perkuliahan.GENAP);
		searchsmt.appendChild(comboitem);
		searchsmt.setCols(2);

		Common.selectComboItem(searchsmt, Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP);
		searchsmt.setReadonly(true);

		Common.initFakultasDanJurusanDanSemua(fakultas, jurusan, null, null);

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
		vbox.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		vbox.appendChild(fakultas);
		fakultas.setCols(6);

		row = new MyFormRow();
		row.setParent(rows);

		vbox = new Vbox();
		vbox.setParent(row);
		vbox.setWidth("100%");
		vbox.appendChild(new ais.ui.util.MyLabelConfig("Jurusan"));
		vbox.appendChild(jurusan);
		jurusan.setCols(6);

		row = new MyFormRow();
		row.setParent(rows);
		vbox = new Vbox();
		vbox.setParent(row);
		vbox.setWidth("100%");
		vbox.appendChild(new ais.ui.util.MyLabelConfig("TA"));
		vbox.appendChild(searchta);
		searchta.setCols(6);

		row = new MyFormRow();
		row.setParent(rows);
		vbox = new Vbox();
		vbox.setParent(row);
		vbox.setWidth("100%");
		vbox.appendChild(new ais.ui.util.MyLabelConfig("Semester"));
		vbox.appendChild(searchsmt);
		searchsmt.setCols(6);

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
							"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "data.xlsx");
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/akademik/LaporanChecklistHasilPenilaianUmumPerPegawai.java:201");
					PesanFormalHelper.tampilkanGagalException("pembuatan berkas Excel Laporan Checklist Hasil Penilaian Umum Per Pegawai", "Sistem mengalami kendala teknis saat menyusun berkas Excel laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap atau format datanya tidak sesuai dengan yang diharapkan oleh template ekspor.", e,
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

		if (tbmuser != null && tbmuser.getSiswa() != null) {
			onCetak(null);
		}

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

					datas = new ArrayList<List>();

					Session session = HibernateUtil.currentSession();
					List<ChecklistHasilPenilaianUmum> checklistHasilPenilaianUmums = session
							.createCriteria(ChecklistHasilPenilaianUmum.class).createAlias("tbmuser", "tbmuser")
							.createAlias("tbmuser.pegawai", "pegawai")

							.add(Restrictions.isNull("tbmuser.dosen"))

							.add(searchsmt.getSelectedItem() == null || searchsmt.getSelectedItem().getValue() == null
									|| searchsmt.getSelectedItem().getValue() == null
											? Restrictions.sqlRestriction("1=1")
											: Restrictions.eq("semesterStr", searchsmt.getSelectedItem().getValue()))

							.add(searchta.getSelectedItem() == null || searchta.getSelectedItem().getValue() == null
									|| searchta.getSelectedItem().getValue() == null
											? Restrictions.sqlRestriction("1=1")
											: Restrictions.eq("tahunAkademik", searchta.getSelectedItem().getValue()))

							.add(jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null
									|| jurusan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
											: CommonSearchFilterHelper.eqSelectedWithId("tbmuser.jurusan", jurusan, false))

							.add(fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null
									|| fakultas.getSelectedItem().getValue() == null
											? Restrictions.sqlRestriction("1=1")
											: CommonSearchFilterHelper.eqSelectedWithId("tbmuser.fakultas", fakultas, false))

							.list();

					TreeMap<String, List<ChecklistHasilPenilaianUmum>> myItems = new TreeMap<String, List<ChecklistHasilPenilaianUmum>>();
					TreeMap<Long, TreeMap<Long, ChecklistPenilaianUmum>> checklistPenilaianUmums = new TreeMap<Long, TreeMap<Long, ChecklistPenilaianUmum>>();

					for (ChecklistHasilPenilaianUmum checklistHasilPenilaianUmum : checklistHasilPenilaianUmums) {
						try {
							List<ChecklistHasilPenilaianUmum> baruPenilaianDosenOlehDosens = myItems
									.get(checklistHasilPenilaianUmum.getTbmuser().getUserId());
							if (baruPenilaianDosenOlehDosens == null) {
								baruPenilaianDosenOlehDosens = new ArrayList<ChecklistHasilPenilaianUmum>();
								myItems.put(checklistHasilPenilaianUmum.getTbmuser().getUserId(),
										baruPenilaianDosenOlehDosens);
							}
							baruPenilaianDosenOlehDosens.add(checklistHasilPenilaianUmum);

							ChecklistPenilaianUmum checklistPenilaianUmum = checklistHasilPenilaianUmum
									.getChecklistPenilaianUmum();
							if (checklistPenilaianUmum != null) {
								GrupChecklistPenilaianUmum grupChecklistPenilaianUmum = checklistPenilaianUmum
										.getGrupChecklistPenilaianUmum();
								if (grupChecklistPenilaianUmum != null) {
									TreeMap<Long, ChecklistPenilaianUmum> subchecklistPenilaianUmums = checklistPenilaianUmums
											.get(grupChecklistPenilaianUmum.getId());
									if (subchecklistPenilaianUmums == null) {
										subchecklistPenilaianUmums = new TreeMap<Long, ChecklistPenilaianUmum>();
										checklistPenilaianUmums.put(grupChecklistPenilaianUmum.getId(),
												subchecklistPenilaianUmums);
									}
									subchecklistPenilaianUmums.put(checklistPenilaianUmum.getId(),
											checklistPenilaianUmum);
								}
							}
						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/akademik/LaporanChecklistHasilPenilaianUmumPerPegawai.java:297");
							PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Checklist Hasil Penilaian Umum Per Pegawai", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
								new String[] {
									"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
									"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
									"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
								});
						}
					}

					System.out.println("myItems => " + myItems.size());
					System.out.println("checklistHasilPenilaianUmums => " + checklistHasilPenilaianUmums.size());

					try {

						ArrayList sub = new ArrayList();
						sub.add("**No.");
						sub.add("**Kode");
						sub.add("**Nama");
						sub.add("**Prodi");
						sub.add("**Program");
						sub.add("**Status");

						for (Long key : checklistPenilaianUmums.keySet()) {
							TreeMap<Long, ChecklistPenilaianUmum> treeMap = checklistPenilaianUmums.get(key);
							int indexD = 0;
							for (ChecklistPenilaianUmum checklistPenilaianUmum : treeMap.values()) {
								if (indexD == 0) {
									sub.add("**" + checklistPenilaianUmum.getGrupChecklistPenilaianUmum().getIsi());
								} else {
									sub.add("**");
								}

								indexD++;
							}
						}

						sub.add("**Total");
						sub.add("**Rata-Rata");

						datas.add(sub);

						sub = new ArrayList();
						sub.add("**");
						sub.add("**");
						sub.add("**");
						sub.add("**");
						sub.add("**");
						sub.add("**");

						for (Long key : checklistPenilaianUmums.keySet()) {
							TreeMap<Long, ChecklistPenilaianUmum> treeMap = checklistPenilaianUmums.get(key);
							for (ChecklistPenilaianUmum checklistPenilaianUmum : treeMap.values()) {
								sub.add("**" + checklistPenilaianUmum.getIsi());
							}
						}

						sub.add("**");
						sub.add("**");

						datas.add(sub);

					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/akademik/LaporanChecklistHasilPenilaianUmumPerPegawai.java:353");
						// TODO: handle exception
					}

					Integer jumlahSemua = 0;
					Integer totalSemua = 0;
					Map<Long, Integer> jumlahMasingMasing = new HashMap<Long, Integer>();

					int nomor = 1;
					for (List<ChecklistHasilPenilaianUmum> aa : myItems.values()) {

						try {
							Tbmuser tbmuser = aa.get(0).getTbmuser();
							Pegawai pegawai = tbmuser.getPegawai();
							label.setValue("Sedang memproses pegawai " + pegawai.toString());

							ArrayList sub = new ArrayList();
							sub.add(nomor);
							sub.add(pegawai.getCode());
							sub.add(pegawai.getNama());
							sub.add(tbmuser.getJurusan() == null ? "" : tbmuser.getJurusan().getNama());
							sub.add(tbmuser.getFakultas() == null ? "" : tbmuser.getFakultas().getNama());
							sub.add(pegawai.getStatusKepegawaian() == null ? ""
									: pegawai.getStatusKepegawaian().getNama());
							Integer jumlah = 0;
							Integer total = 0;
							for (Long keyD : checklistPenilaianUmums.keySet()) {
								TreeMap<Long, ChecklistPenilaianUmum> treeMap = checklistPenilaianUmums.get(keyD);
								for (ChecklistPenilaianUmum checklistPenilaianUmum : treeMap.values()) {

									Integer val = 0;
									for (ChecklistHasilPenilaianUmum checklistHasilPenilaianUmum : aa) {
										if (checklistHasilPenilaianUmum.getChecklistPenilaianUmum().getId()
												.equals(checklistPenilaianUmum.getId())) {
											val = checklistHasilPenilaianUmum.getNilai();
											break;
										}
									}

									sub.add(val);

									totalSemua += val;
									jumlahSemua++;

									total += val;
									jumlah++;

									Integer sebelumnya = jumlahMasingMasing.get(checklistPenilaianUmum.getId());
									if (sebelumnya == null) {
										sebelumnya = 0;
									}
									sebelumnya += val;
									jumlahMasingMasing.put(checklistPenilaianUmum.getId(), sebelumnya);
								}
							}
							sub.add("**" + Common.numberFormat.get().format(total));
							sub.add("**" + Common.numberFormat.get().format(total.doubleValue() / jumlah.doubleValue()));
							datas.add(sub);

							nomor++;
						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/akademik/LaporanChecklistHasilPenilaianUmumPerPegawai.java:414");
							PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Checklist Hasil Penilaian Umum Per Pegawai", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
								new String[] {
									"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
									"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
									"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
								});
						}

					}

					ArrayList sub = new ArrayList();
					sub.add("**");
					sub.add("**Total");
					sub.add("**");
					sub.add("**");
					sub.add("**");
					sub.add("**");
					for (Long keyD : checklistPenilaianUmums.keySet()) {
						TreeMap<Long, ChecklistPenilaianUmum> treeMap = checklistPenilaianUmums.get(keyD);
						for (ChecklistPenilaianUmum checklistPenilaianUmum : treeMap.values()) {
							Integer sebelumnya = jumlahMasingMasing.get(checklistPenilaianUmum.getId());
							if (sebelumnya == null) {
								sebelumnya = 0;
							}
							sub.add("**" + Common.numberFormat.get().format(sebelumnya));
						}
					}

					sub.add("**" + Common.numberFormat.get().format(totalSemua));
					sub.add("**" + Common.numberFormat.get().format(totalSemua.doubleValue() / jumlahSemua.doubleValue()));
					datas.add(sub);

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

//						System.out.println("Tampilkan data --> " + datas);

						ais.action.report.helper.LoadingReportUtil.clearBusy();
						excelku = new ais.ui.util.MySpreadsheet();
						Common.clear(center);
						center.appendChild(excelku);
						EcampusUtil.tampilkan(datas, excelku, false);
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
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas Excel Laporan Checklist Hasil Penilaian Umum Per Pegawai", "Sistem mengalami kendala teknis saat menyusun berkas Excel laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap atau format datanya tidak sesuai dengan yang diharapkan oleh template ekspor.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mengekspor laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini sudah lengkap dan benar, kemudian coba ekspor ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

}
