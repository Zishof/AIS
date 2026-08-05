package ais.action.master.dashboard.sekolah;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zss.model.Worksheet;
import org.zkoss.zss.ui.Spreadsheet;
import org.zkoss.zss.ui.impl.Utils;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.West;

import ais.action.master.helper.AmbilDataTbmuserBanbox;
import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.report.Report;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.CommonVO;
import ais.database.model.ParameterTambahan;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranLain;
import ais.database.model.sekolah.KegiatanSiswa;
import ais.database.model.sekolah.KelompokKegiatanSiswa;
import ais.database.model.sekolah.ParameterTambahanKegiatanSiswa;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelBold;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class DashboardRekapKegiatanSiswaData extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 790038368339375113L;

	private Combobox searchyayasan = new Combobox();
	private Combobox searchsekolah = new Combobox();
	private Textbox searchsiswa = new Textbox();
	private AmbilDataTbmuserBanbox pembina = new AmbilDataTbmuserBanbox();
	private Spreadsheet spreadsheet = new ais.ui.util.MySpreadsheet();
	private Center center = new Center();

	private List<MyCheckboxConfig> kolom = new ArrayList<MyCheckboxConfig>();

	public DashboardRekapKegiatanSiswaData() {
		super();

		try {
			initYayasan();
			init();
			initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private void initYayasan() {

		Common.initYayasanDanSekolahDanSemua(null, null, searchyayasan, searchsekolah);

	}

	private void init() throws Exception {

		setHeight("100%");
		setWidth("100%");
		setBorder("none");
		setClosable(false);
		setPosition("center");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, true);

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(north);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Yayasan"));
		row.appendChild(searchyayasan);
		searchyayasan.setWidth("90%");
		searchyayasan.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sekolah"));
		row.appendChild(searchsekolah);
		searchsekolah.setWidth("90%");
		searchsekolah.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pembina"));
		row.appendChild(pembina);
		pembina.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Siswa"));
		row.appendChild(searchsiswa);
		searchsiswa.setWidth("90%");

		West west = new West();
		west.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(west, true);
		west.setWidth("250px");

		grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(west);
		grid.setWidth("100%");
		grid.setHeight("100%");

		final Rows rowsParams = new Rows();
		rowsParams.setParent(grid);

		EventListener eventListener = new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event arg0) throws Exception {

				Common.clear(rowsParams);
				kolom.clear();

				MyFormRow row = new MyFormRow();
		row.setValign("top");
				row.setParent(rowsParams);

				Hbox hbox = new Hbox();
				hbox.setParent(row);

				MyToolbarbuttonConfig refresh = new MyToolbarbuttonConfig("Tampilkan", "/img/print.png");
				refresh.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						initSpreadsheet();
					}
				});
				refresh.setParent(hbox);

				MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Ambil File", "/img/print.png");
				print.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						ByteArrayOutputStream bout = new ByteArrayOutputStream();
						spreadsheet.getBook().write(bout);
						bout.close();
						Filedownload.save(bout.toByteArray(),
								"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "data.xlsx");
					}
				});
				print.setParent(hbox);

				print = new MyToolbarbuttonConfig("Download", "/img/print.png");
				print.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						cetak();
					}
				});

				print.setParent(hbox);

				Session session = HibernateUtil.currentSession();
				List<KelompokKegiatanSiswa> kelompokKegiatanSiswas = session.createCriteria(KelompokKegiatanSiswa.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).list();
				Collections.sort(kelompokKegiatanSiswas);
				for (KelompokKegiatanSiswa kelompokKegiatanSiswa : kelompokKegiatanSiswas) {
					List<ParameterTambahanKegiatanSiswa> parameterTambahanKegiatanSiswas = session
							.createCriteria(ParameterTambahanKegiatanSiswa.class)
							.add(Restrictions.eq("kelompokKegiatanSiswa", kelompokKegiatanSiswa))

							.list();
					Collections.sort(parameterTambahanKegiatanSiswas);

					row = new MyFormRow();
					row.setParent(rowsParams);
					row.appendChild(new MyLabelBold(kelompokKegiatanSiswa.getNama()));

					for (ParameterTambahanKegiatanSiswa parameterTambahanKegiatanSiswa : parameterTambahanKegiatanSiswas) {
						row = new MyFormRow();
						row.setParent(rowsParams);
						MyCheckboxConfig checkboxConfig = new MyCheckboxConfig(
								parameterTambahanKegiatanSiswa.getParameterTambahan().getLabelInputan());
						checkboxConfig.setChecked(true);
						checkboxConfig.setAttribute("parameterTambahan",
								parameterTambahanKegiatanSiswa.getParameterTambahan());
						checkboxConfig.setAttribute("kelompokKegiatanSiswa", kelompokKegiatanSiswa);
						checkboxConfig.setParent(row);
						kolom.add(checkboxConfig);
					}
				}

				initSpreadsheet();
			}
		};

		eventListener.onEvent(null);

		searchyayasan.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				initSpreadsheet();
			}
		});
		searchsekolah.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				initSpreadsheet();
			}
		});
		searchsiswa.addEventListener("onOK", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				initSpreadsheet();
			}
		});

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void cetak() throws Exception {
		Session session = HibernateUtil.currentNativeSession();
		List<Long> kegiatanSiswasid = buatCriteria(session).addOrder(Order.asc("siswa.id")).addOrder(Order.asc("id"))
				.list();

		Map parameters = ais.common.HashMapGenerator.getRand();

		List<Map> maps = new ArrayList<Map>();

		for (Long idP : kegiatanSiswasid) {
			KegiatanSiswa kegiatanSiswa = (KegiatanSiswa) session.createCriteria(KegiatanSiswa.class)
					.add(Restrictions.idEq(idP)).uniqueResult();
			if (kegiatanSiswa != null) {

				Map map = new java.util.HashMap();

				Yayasan myYayasan = kegiatanSiswa.getSiswa().getYayasan();
				Sekolah mySekolah = kegiatanSiswa.getSiswa().getSekolah();

				map.put("header",
						mySekolah == null
								? (myYayasan == null ? Common.ambilREAL_PATH_REPORT() + "/wood.jpg"
										: Common.getRequestHostWithProtocol() + "/AmbilLampiran?ref="
												+ (myYayasan.getId()) + "&jenis=KOP+Yayasan&usingId=false")
								: Common.getRequestHostWithProtocol() + "/AmbilLampiran?ref=" + (mySekolah.getId())
										+ "&jenis=KOP+Sekolah&usingId=false");

				PerguruanTinggi perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();
				if (mySekolah != null && mySekolah.getId() != null) {
					LampiranLain lampiranLain = LampiranLain.ambil(mySekolah.getId(), LampiranLain.KOP_SEKOLAH);
					if (lampiranLain != null) {
						map.put("header", lampiranLain.ambilFile().getAbsolutePath());
					} else {
						if (perguruanTinggi != null) {
							lampiranLain = LampiranLain.ambil(perguruanTinggi.getId(), LampiranLain.KOP_PT);
							if (lampiranLain != null) {
								map.put("header", lampiranLain.ambilFile().getAbsolutePath());
							}
							lampiranLain = LampiranLain.ambil(perguruanTinggi.getId(), LampiranLain.KOP_BAWAH_PT);
							if (lampiranLain != null) {
								map.put("footer", lampiranLain.ambilFile().getAbsolutePath());
							}
						}
					}
				} else if (myYayasan != null && myYayasan.getId() != null) {
					LampiranLain lampiranLain = LampiranLain.ambil(myYayasan.getId(), LampiranLain.KOP_YAYASAN);
					if (lampiranLain != null) {
						map.put("header", lampiranLain.ambilFile().getAbsolutePath());
					} else {
						if (perguruanTinggi != null) {
							lampiranLain = LampiranLain.ambil(perguruanTinggi.getId(), LampiranLain.KOP_PT);
							if (lampiranLain != null) {
								map.put("header", lampiranLain.ambilFile().getAbsolutePath());
							}
							lampiranLain = LampiranLain.ambil(perguruanTinggi.getId(), LampiranLain.KOP_BAWAH_PT);
							if (lampiranLain != null) {
								map.put("footer", lampiranLain.ambilFile().getAbsolutePath());
							}
						}
					}
				} else {
					if (perguruanTinggi != null) {
						LampiranLain lampiranLain = LampiranLain.ambil(perguruanTinggi.getId(), LampiranLain.KOP_PT);
						if (lampiranLain != null) {
							map.put("header", lampiranLain.ambilFile().getAbsolutePath());
						}
						lampiranLain = LampiranLain.ambil(perguruanTinggi.getId(), LampiranLain.KOP_BAWAH_PT);
						if (lampiranLain != null) {
							map.put("footer", lampiranLain.ambilFile().getAbsolutePath());
						}
					}

				}

				map.put("id_siswa", kegiatanSiswa.getSiswa().getId());
				map.put("nis", kegiatanSiswa.getSiswa().getNomorInduk());
				map.put("nisn", kegiatanSiswa.getSiswa().getNomorIndukNasional());
				map.put("nama", kegiatanSiswa.getSiswa().getNama());
				map.put("sekolah", kegiatanSiswa.getSiswa().getSekolah() == null ? ""
						: kegiatanSiswa.getSiswa().getSekolah().getNama());

				map.put("kelas", kegiatanSiswa.getSiswa().getKelas() == null ? ""
						: kegiatanSiswa.getSiswa().getKelas().getNama());

				map.put("pembina",
						kegiatanSiswa.getPembina1() != null ? kegiatanSiswa.getPembina1().getUserNama()
								: (kegiatanSiswa.getSiswa().getGuruPembina() == null ? ""
										: kegiatanSiswa.getSiswa().getGuruPembina().getNama()));

				map.put("tanggal",
						kegiatanSiswa.getWaktu() == null ? "" : Common.dateFormat.get().format(kegiatanSiswa.getWaktu()));

				map.put("keterangan", kegiatanSiswa.getKeterangan());

				map.put("pembina1",
						kegiatanSiswa.getPembina1() == null ? "" : kegiatanSiswa.getPembina1().getUserNama());
				map.put("pembina2",
						kegiatanSiswa.getPembina2() == null ? "" : kegiatanSiswa.getPembina2().getUserNama());
				map.put("pembina3",
						kegiatanSiswa.getPembina3() == null ? "" : kegiatanSiswa.getPembina3().getUserNama());

				List<CommonVO> hasil = kegiatanSiswa.ambilDataParameterTambahan();
				for (MyCheckboxConfig checkboxConfig : kolom) {
					if (checkboxConfig.isChecked()) {
						ParameterTambahan parameterTambahan = (ParameterTambahan) checkboxConfig
								.getAttribute("parameterTambahan");
						for (CommonVO vo : hasil) {
							if (vo.getId() != null && vo.getId().equals(parameterTambahan.getId().toString())) {

								map.put(parameterTambahan.getLabelInputan().toLowerCase(),
										vo.getName1() + (vo.getName2() != null && !vo.getName2().trim().isEmpty()
												? ", link : " + vo.getName2()
												: ""));
								break;
							}
						}

					}
				}
				maps.add(map);

			}
			kegiatanSiswa = null;
		}
		kegiatanSiswasid = null;

		parameters.put("maps", maps);

		Report.generatePDFReport("pdf", parameters, "sekolah/kegiatan_siswa", ais.ui.util.WaktuUtil.getDate(), maps);
	}

	private Criteria buatCriteria(Session session) {
		List<KelompokKegiatanSiswa> kelompokKegiatanSiswas = new ArrayList<KelompokKegiatanSiswa>();

		for (MyCheckboxConfig checkboxConfig : kolom) {
			if (checkboxConfig.isChecked()) {
				kelompokKegiatanSiswas
						.add((KelompokKegiatanSiswa) checkboxConfig.getAttribute("kelompokKegiatanSiswa"));
			}
		}

		Tbmuser pembina = (Tbmuser) this.pembina.getAttribute("tbmuser");

		Yayasan yayasan = (Yayasan) (searchyayasan.getSelectedItem() == null
				|| searchyayasan.getSelectedItem().getValue() == null
				|| searchyayasan.getSelectedItem().getValue() == null ? null
						: searchyayasan.getSelectedItem().getValue());
		Sekolah sekolah = (Sekolah) (searchsekolah.getSelectedItem() == null
				|| searchsekolah.getSelectedItem().getValue() == null
				|| searchsekolah.getSelectedItem().getValue() == null ? null
						: searchsekolah.getSelectedItem().getValue());

		return session.createCriteria(KegiatanSiswa.class)

				.add(pembina == null ? Restrictions.sqlRestriction("true")
						: Restrictions.or(Restrictions.eq("pembina1", pembina),
								Restrictions.or(Restrictions.eq("pembina2", pembina),
										Restrictions.eq("pembina3", pembina))))

				.createAlias("siswa", "siswa")

				.add(searchsiswa.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.or(
								Restrictions.ilike("siswa.nama", searchsiswa.getValue().trim(), MatchMode.ANYWHERE),
								Restrictions.or(
										Restrictions.ilike("siswa.nomorIndukNasional", searchsiswa.getValue().trim(),
												MatchMode.ANYWHERE),
										Restrictions.ilike("siswa.nomorInduk", searchsiswa.getValue().trim(),
												MatchMode.ANYWHERE))))

				.add(yayasan == null || yayasan.getId() == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("siswa.yayasan", yayasan))
				.add(sekolah == null || sekolah.getId() == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("siswa.sekolah", sekolah))

				.setProjection(Projections.property("id"))
				.add(kelompokKegiatanSiswas.isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.in("kelompokKegiatanSiswa", kelompokKegiatanSiswas));
	}

	@SuppressWarnings({ "unchecked" })
	private void initSpreadsheet() {

		final List<List<Object>> sekolahsSemua = new ArrayList<List<Object>>();

		final Label label = Common.displayLoadBar(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(center);
				if (!sekolahsSemua.isEmpty()) {
					spreadsheet = new ais.ui.util.MySpreadsheet();
					Common.clear(center);
					spreadsheet.setParent(center);
					spreadsheet.setWidth("100%");
					spreadsheet.setHeight("100%");
					spreadsheet.setSrc("../../WEB-INF/rowcolumn.xlsx");
					spreadsheet.setMaxcolumns(sekolahsSemua.get(0).size() + 1);
					spreadsheet.setMaxrows(sekolahsSemua.size() + 1);

					Worksheet sheet = spreadsheet.getSelectedSheet();
					sheet.setDefaultColumnWidth(40);
					Utils.setColumnWidth(sheet, 0, 20);
					Utils.setColumnWidth(sheet, 1, 60);
					Utils.setColumnWidth(sheet, 2, 100);
					Utils.setColumnWidth(sheet, 3, 80);

					int rowIndex = 0;
					for (List<Object> objects : sekolahsSemua) {
						int rowIndexData = 0;
						for (Object object : objects) {
							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, rowIndexData, object);
							rowIndexData++;
						}
						rowIndex++;
					}
					// Excel mentah -> grid ringan (Book tetap hidup untuk tombol Download). Pola B PratinjauXlsxHelper.
					ais.ui.util.PratinjauXlsxHelper.gantiSpreadsheetDenganGrid(spreadsheet);
				}

			}
		});

		new Thread(new Runnable() {

			@Override
			public void run() {
				try {

				List<Object> namadata = new ArrayList<Object>();
				namadata.add("No.");
				namadata.add("NIS");
				namadata.add("Nama");
				namadata.add("Kelas");
				namadata.add("Sekolah");

				List<KelompokKegiatanSiswa> kelompokKegiatanSiswas = new ArrayList<KelompokKegiatanSiswa>();

				for (MyCheckboxConfig checkboxConfig : kolom) {
					if (checkboxConfig.isChecked()) {
						namadata.add(checkboxConfig.getLabel());
						kelompokKegiatanSiswas
								.add((KelompokKegiatanSiswa) checkboxConfig.getAttribute("kelompokKegiatanSiswa"));
					}
				}
				sekolahsSemua.add(namadata);

				Session session = HibernateUtil.currentNativeSession();
				List<Long> kegiatanSiswasid = buatCriteria(session).addOrder(Order.asc("id")).list();

				int size = kegiatanSiswasid.size();
				int index = 0;
				for (Long idP : kegiatanSiswasid) {
					KegiatanSiswa kegiatanSiswa = (KegiatanSiswa) session.createCriteria(KegiatanSiswa.class)
							.add(Restrictions.idEq(idP)).uniqueResult();
					if (kegiatanSiswa != null) {
						index++;

						List<Object> objects = new ArrayList<Object>();

						objects.add(index);

						if (kegiatanSiswa.getSiswa() != null) {
							objects.add(kegiatanSiswa.getSiswa().getNomorInduk());
							objects.add(kegiatanSiswa.getSiswa().getNama());
							objects.add(kegiatanSiswa.getSiswa().getKelas() == null ? ""
									: kegiatanSiswa.getSiswa().getKelas().getNama());
							objects.add(kegiatanSiswa.getSiswa().getSekolah() == null ? ""
									: kegiatanSiswa.getSiswa().getSekolah().getNama());
						} else {
							objects.add("");
							objects.add("");
							objects.add("");
							objects.add("");
						}

						label.setValue("Memproses data \"" + objects.toString() + "\" ("
								+ Common.numberFormat.get().format((index * 100.0) / size) + " %)");

						boolean adaSemua = false;
						List<CommonVO> hasil = kegiatanSiswa.ambilDataParameterTambahan();
						for (MyCheckboxConfig checkboxConfig : kolom) {
							if (checkboxConfig.isChecked()) {
								ParameterTambahan parameterTambahan = (ParameterTambahan) checkboxConfig
										.getAttribute("parameterTambahan");
								boolean ada = false;
								for (CommonVO vo : hasil) {
									if (vo.getId() != null && vo.getId().equals(parameterTambahan.getId().toString())) {
										objects.add(vo.getName1()
												+ (vo.getName2() != null && !vo.getName2().trim().isEmpty()
														? ", link : " + vo.getName2()
														: ""));
										adaSemua = true;
										ada = true;
										break;
									}
								}
								if (!ada) {
									objects.add("");
								}
							}
						}
						hasil = null;
						if (adaSemua) {
							sekolahsSemua.add(objects);
						}

					}
					kegiatanSiswa = null;
				}
				kegiatanSiswasid = null;
				label.setValue("");
				HibernateUtil.closeSession();
							} finally {
					ais.database.hibernate.HibernateUtil.closeSession();
				}
			}
		}).start();

	}
}
