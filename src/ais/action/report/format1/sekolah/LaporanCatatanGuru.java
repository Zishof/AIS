package ais.action.report.format1.sekolah;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.West;

import ais.action.master.sekolah.helper.AmbilDataGuruBanbox;
import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.ParameterTambahan;
import ais.database.model.Perkuliahan;
import ais.database.model.file.LampiranLain;
import ais.database.model.sekolah.CatatanGuru;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.JenisCatatanGuru;
import ais.database.model.sekolah.KelompokParameterTambahanCatatanGuru;
import ais.database.model.sekolah.ParameterTambahanCatatanGuru;
import ais.database.model.sekolah.Sekolah;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class LaporanCatatanGuru extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = -397946194166101691L;

	private AmbilDataGuruBanbox bandboxGuru;
	private Center center;

	public LaporanCatatanGuru() {
		super();
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Catatan Guru", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanCatatanGuru(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);
		init();
	}

	private MyDatebox tanggal;

	private MyDatebox sampai;

	private Combobox jenisCatatanGuru;

	private Combobox tahunAkademik;

	private Combobox searchsmt;

	private void init() throws Exception {

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				onKHS(event);

			}
		};

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);

		West west = new West();
		west.setTitle("Menu");
		west.setCollapsible(true);
		west.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(west, true);
		west.setWidth("350px");

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(west);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);
		MyColumnConfig column = new MyColumnConfig();
		column.setWidth("25%");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(Common.getBahasa("label_guru")));
		row.appendChild(bandboxGuru = new AmbilDataGuruBanbox());
		bandboxGuru.setWidth("90%");

		if (Common.getCurrentUser() != null && Common.getCurrentUser().getGuru() != null) {
			Guru guru = Common.getCurrentUser().getGuru();
			bandboxGuru.setAttribute("guru", guru);
			bandboxGuru.setAttribute("myValue", guru);
			bandboxGuru.setValue(guru.getNim() + " - " + guru.getNama());
			bandboxGuru.setId("mhs_" + guru.getId());
			bandboxGuru.setDisabled(true);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Catatan *"));
		row.appendChild(jenisCatatanGuru = new Combobox());
		jenisCatatanGuru.setWidth("90%");
		jenisCatatanGuru.setReadonly(true);

		EventListener eventListenerJenis = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Guru guru = (Guru) bandboxGuru.getAttribute("guru");

				if (guru != null) {
					Common.insertCombo(jenisCatatanGuru, new String[] { "nama", "kode" }, "keterangan",
							JenisCatatanGuru.class, Restrictions.and(Restrictions.eq("sekolah", guru.getSekolah()),
									Restrictions.eq("aktif", true)));
				}

			}

		};
		jenisCatatanGuru.addEventListener("onChange", eventListener);
		bandboxGuru.setEventListener(eventListenerJenis);

		Common.createDefaultTimer(eventListenerJenis);

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR) - 1);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Mulai"));
		tanggal = new MyDatebox(calendar.getTime());
		row.appendChild(tanggal);
		tanggal.setWidth("90%");
		tanggal.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sampai"));
		sampai = new MyDatebox(ais.ui.util.WaktuUtil.getDate());
		row.appendChild(sampai);
		sampai.setWidth("90%");
		sampai.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new MyLabelConfig("TA : "));
		tahunAkademik = new Combobox();
		Common.generateTahunAjaran(tahunAkademik);
		row.appendChild(tahunAkademik);
		tahunAkademik.setWidth("90%");

		tahunAkademik.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onKHS(arg0);
			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new MyLabelConfig("Smt : "));
		Comboitem comboitem = new Comboitem(Perkuliahan.GANJIL);
		comboitem.setValue(1);
		searchsmt = new Combobox();
		searchsmt.appendChild(comboitem);
		comboitem = new Comboitem(Perkuliahan.GENAP);
		comboitem.setValue(2);
		searchsmt.appendChild(comboitem);
		searchsmt.setWidth("90%");

		Common.selectComboItem(searchsmt, Common.isNowSemensterGanjil() ? 1 : 2);
		searchsmt.setReadonly(true);
		row.appendChild(searchsmt);
		searchsmt.setWidth("90%");

		searchsmt.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onKHS(arg0);
			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Tampilkan", "/img/print.png");
		print.addEventListener("onClick", eventListener);
		print.setParent(row);

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		// row = new MyFormRow();
		//		// row.setParent(rows);

		org.zkoss.zul.North north = new org.zkoss.zul.North();
		north.setParent(borderlayout);
		north.appendChild(CommonReport.exportReport(new ParameterListener() {

			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			public Map<String, Serializable> generateParameters() throws Exception {

				if (bandboxGuru.getAttribute("guru") == null) {
					MyMessageboxConfig.show("Pilih Guru", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return null;
				}

				if (jenisCatatanGuru.getSelectedItem() == null
						|| jenisCatatanGuru.getSelectedItem().getValue() == null) {
					MyMessageboxConfig.show("Jenis Catatan Guru", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return null;
				}

				if (jenisCatatanGuru.getSelectedItem() == null
						|| jenisCatatanGuru.getSelectedItem().getValue() == null) {
					return null;
				}

				JenisCatatanGuru j = (JenisCatatanGuru) jenisCatatanGuru.getSelectedItem().getValue();

				Map parameters = generateParameter((Guru) bandboxGuru.getAttribute("guru"), tanggal.getValue(),
						sampai.getValue(),
						tahunAkademik.getSelectedItem() == null ? null
								: tahunAkademik.getSelectedItem().getValue().toString(),
						(Integer) (searchsmt.getSelectedItem() == null ? null : searchsmt.getSelectedItem().getValue()),
						null, j);
				return parameters;
			}
		}, "Catatan_Guru", null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onKHS(arg0);

			}
		}, false));

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static Map initData(CatatanGuru catatanGuru) {
		Map map = new HashMap();
		JenisCatatanGuru ja = catatanGuru.getJenisCatatanGuru();
		Session session = HibernateUtil.currentSession();
		session.refresh(ja);

		map.put("id", catatanGuru.getId());
		map.put("nama", catatanGuru.getNama());
		map.put("waktu", catatanGuru.getWaktu());
		map.put("tahunAjaran", catatanGuru.getTahunAjaran());
		map.put("semester", catatanGuru.getSemester());

		if (catatanGuru.getWaktu() != null) {
			map.put("waktu.formated1", Common.dateFormat6.get().format(catatanGuru.getWaktu()));
			map.put("waktu.formated2", Common.dateFormat2.get().format(catatanGuru.getWaktu()));
			map.put("waktu.formated3", Common.dateFormat51.get().format(catatanGuru.getWaktu()));
			map.put("waktu.formated4", Common.timeFormat.get().format(catatanGuru.getWaktu()));
			map.put("waktu.formated5", Common.dateFormat1.get().format(catatanGuru.getWaktu()));
		}

		map.put("keterangan", catatanGuru.getKeterangan());

		for (KelompokParameterTambahanCatatanGuru kelompokParameterTambahanCatatanGuru : ja
				.getKelompokParameterTambahanCatatanGurus()) {
			map.put("kelompok_id", kelompokParameterTambahanCatatanGuru.getId());
			map.put("kelompok", kelompokParameterTambahanCatatanGuru.getNama());

			List<ParameterTambahan> parameterTambahans = ConstantValues
					.simpleList(
							session.createCriteria(ParameterTambahanCatatanGuru.class)
									.add(Restrictions.eq("kelompokParameterTambahanCatatanGuru",
											kelompokParameterTambahanCatatanGuru))
									.createAlias("parameterTambahan", "parameterTambahan")
									.createAlias("kelompokParameterTambahanCatatanGuru",
											"kelompokParameterTambahanCatatanGuru")
									.add(Restrictions.eq("parameterTambahan.aktif", true))
									.add(Restrictions.eq("kelompokParameterTambahanCatatanGuru.aktif", true))
									.setProjection(Projections.groupProperty("parameterTambahan.id")),
							ParameterTambahan.class, false);
			Collections.sort(parameterTambahans);

			for (ParameterTambahan parameterTambahan : parameterTambahans) {
				String jenis = kelompokParameterTambahanCatatanGuru.getId() + "->" + parameterTambahan.getId();
				String jenis_id = kelompokParameterTambahanCatatanGuru.getId() + "_" + parameterTambahan.getId();

				String val = "";
				String ket = "";
				String[] spl = catatanGuru.getParameterTambahanInds().split("\n");
				for (String d : spl) {
					String[] value = d.split("<=>");
					if (value[0].trim().equalsIgnoreCase(jenis)) {
						val = value.length > 1 ? value[1].trim() : "";
						try {
							ket = value.length > 0 ? value[value.length - 1] : "";
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/sekolah/LaporanCatatanGuru.java:349");
							PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Catatan Guru", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
								new String[] {
									"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
									"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
									"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
								});

						}
					}
				}

				LampiranLain lampiranLain = LampiranLain.ambil(catatanGuru.getId(), jenis);

				String vall = val;
				map.put("param.id." + parameterTambahan.getId(), vall);
				map.put("param.nama." + parameterTambahan.getNama().toLowerCase(), vall);
				map.put("param.kode." + parameterTambahan.getKode(), vall);
				map.put("param.keterangan." + parameterTambahan.getKode(), ket);
				if (parameterTambahan.getTipeDataInputan().equals(ParameterTambahan.ANGKA)
						|| parameterTambahan.getTipeDataInputan().equals(ParameterTambahan.TEXT_ANGKA)) {
					try {
						Double nilai = val.trim().isEmpty() || val.trim().equalsIgnoreCase("null") ? null
								: Double.parseDouble(val);
						map.put("param.id." + parameterTambahan.getId(), nilai);
						map.put("param.nama." + parameterTambahan.getNama().toLowerCase(), nilai);
						map.put("param.kode." + parameterTambahan.getKode(), nilai);
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/sekolah/LaporanCatatanGuru.java:370");
						PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Catatan Guru", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
							new String[] {
								"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
								"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
								"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
							});
					}
				}

				if (parameterTambahan.getTipeDataInputan().equals(ParameterTambahan.PILIHAN_MATRIX_BANYAK_COMBO)) {

					try {
						JSONObject temporary = val == null || val.isEmpty() ? new JSONObject() : new JSONObject(val);

						Iterator<String> keys = temporary.keys();

						while (keys.hasNext()) {
							String key = keys.next();
							String dd = jenis_id.trim() + "." + key.trim();
							map.put(dd, temporary.get(key));
							map.put(dd.replaceAll("[^\\sa-zA-Z0-9]", "").replaceAll(" ", ""), temporary.get(key));
						}

					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/sekolah/LaporanCatatanGuru.java:388");
						PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Catatan Guru", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
							new String[] {
								"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
								"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
								"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
							});

					}

				}

				if (parameterTambahan.getTipeDataInputan().equals(ParameterTambahan.PILIHAN_MATRIX_BANYAK_NILAI)) {

					try {
						JSONObject temporary = val == null || val.isEmpty() ? new JSONObject() : new JSONObject(val);

						Iterator<String> keys = temporary.keys();

						while (keys.hasNext()) {
							String key = keys.next();

							try {

								JSONObject jsonObject = temporary.getJSONObject(key);
								Iterator<String> keysSub = jsonObject.keys();
								while (keysSub.hasNext()) {
									try {
										String keySub = keysSub.next();

										String dd = jenis_id.trim() + "." + key.trim() + "." + keySub.trim();

										map.put(dd, jsonObject.get(key));
										map.put(dd.replaceAll("[^\\sa-zA-Z0-9]", "").replaceAll(" ", ""), jsonObject.get(key));

									} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/sekolah/LaporanCatatanGuru.java:417");
										PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Catatan Guru", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
											new String[] {
												"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
												"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
												"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
											});

									}
								}

							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/sekolah/LaporanCatatanGuru.java:422");
								PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Catatan Guru", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
									new String[] {
										"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
										"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
										"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
									});

							}
						}

					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/sekolah/LaporanCatatanGuru.java:427");
						PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Catatan Guru", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
							new String[] {
								"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
								"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
								"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
							});

					}

				}

				if (parameterTambahan.getTipeDataInputan().equals(ParameterTambahan.TANGGAL)) {
					Date nilai = null;
					try {
						nilai = val.trim().isEmpty() ? null : Common.dateFormat1.get().parse(val);
						map.put("param.id.formated1", Common.dateFormat6.get().format(nilai));
						map.put("param.id.formated2", Common.dateFormat2.get().format(nilai));
						map.put("param.id.formated3", Common.dateFormat51.get().format(nilai));
						map.put("param.id.formated4", Common.timeFormat.get().format(nilai));
						map.put("param.id.formated5", Common.dateFormat1.get().format(nilai));

						map.put(jenis_id + ".formated1", Common.dateFormat6.get().format(nilai));
						map.put(jenis_id + ".formated2", Common.dateFormat2.get().format(nilai));
						map.put(jenis_id + ".formated3", Common.dateFormat51.get().format(nilai));
						map.put(jenis_id + ".formated4", Common.timeFormat.get().format(nilai));
						map.put(jenis_id + ".formated5", Common.dateFormat1.get().format(nilai));

					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/sekolah/LaporanCatatanGuru.java:449");
						PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Catatan Guru", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
							new String[] {
								"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
								"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
								"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
							});

					}
				}

				if (parameterTambahan.getTipeDataInputan().equals(ParameterTambahan.TANGGAL_DAN_WAKTU)) {
					Date nilai = null;
					try {
						nilai = val.trim().isEmpty() ? null : Common.dateFormat.get().parse(val);
						map.put("param.id.formated1", Common.dateFormat6.get().format(nilai));
						map.put("param.id.formated2", Common.dateFormat2.get().format(nilai));
						map.put("param.id.formated3", Common.dateFormat51.get().format(nilai));
						map.put("param.id.formated4", Common.timeFormat.get().format(nilai));
						map.put("param.id.formated5", Common.dateFormat1.get().format(nilai));

						map.put(jenis_id + ".formated1", Common.dateFormat6.get().format(nilai));
						map.put(jenis_id + ".formated2", Common.dateFormat2.get().format(nilai));
						map.put(jenis_id + ".formated3", Common.dateFormat51.get().format(nilai));
						map.put(jenis_id + ".formated4", Common.timeFormat.get().format(nilai));
						map.put(jenis_id + ".formated5", Common.dateFormat1.get().format(nilai));

					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/sekolah/LaporanCatatanGuru.java:470");
						PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Catatan Guru", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
							new String[] {
								"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
								"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
								"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
							});

					}
				}

				map.put(jenis_id, vall);

				parameterTambahan.masukkanData(vall, jenis_id, map);

				if (lampiranLain != null) {
					map.put(jenis_id + "_url", lampiranLain.ambilFile().getAbsolutePath());
				}
			}

		}

		return map;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static Map generateParameter(Guru guru, Date tanggal, Date sampai, String ta, Integer smt,
			CatatanGuru catatanGurua, JenisCatatanGuru j) throws Exception {

		if (guru == null) {
			return null;
		}

		Map parameters = ais.common.HashMapGenerator.getRandStringSerializable();
		// EKSPOR DINAMIS: jrxml terupload per jenis dipakai utk SEMUA format cetak (PDF/XLS/DOCX/PPTX).
		if (j != null && j.getId() != null) {
			try {
				LampiranLain layoutDinamis = LampiranLain.ambil(j.getId(),
						LampiranLain.FILE_JRXML_LAYOUT_JENIS_CATATAN_GURU);
				if (layoutDinamis != null && layoutDinamis.ambilFile() != null
						&& layoutDinamis.ambilFile().exists()) {
					parameters.put("nama_laporan", layoutDinamis.ambilFile().getAbsolutePath());
				}
			} catch (Throwable abaikan) { ais.common.ErrorAuditUtil.record(abaikan, "auto-audit(empty-catch) src/ais/action/report/format1/sekolah/LaporanCatatanGuru.java:507");
			}
		}

		if (j.getId() != null) {
			try {
				Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();
				List<LampiranLain> lampiranLains = streamingSession.createCriteria(LampiranLain.class)
						.addOrder(Order.asc("id")).add(Restrictions.eq("ref", j.getId()))
						.add(Restrictions.ilike("jenis", "Catatan_Guru_", MatchMode.START)).list();
				int index = 0;
				for (LampiranLain lampiran : lampiranLains) {
					File f = lampiran.ambilFile();
					if (f != null & f.exists()) {
						parameters.put("file_" + (++index), f.getAbsolutePath());
					}
				}

				StreamingHibernateUtil.getInstance().closeSession();

			} catch (Exception e1) {
				StreamingHibernateUtil.getInstance().rollbackTransaction();
				e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/report/format1/sekolah/LaporanCatatanGuru.java:529");
			}
		}

		guru.putPhoto(parameters);

		parameters.put("sekolah", (guru.getSekolah() == null ? "" : guru.getSekolah().getNama()));
		parameters.put("nama", (guru.getNama()));
		parameters.put("guru_id", guru.getId());
		parameters.put("tanggal", tanggal);
		parameters.put("sampai", sampai);

		if (sampai != null) {
			parameters.put("sampai.formated1", Common.dateFormat6.get().format(sampai));
			parameters.put("sampai.formated2", Common.dateFormat2.get().format(sampai));
			parameters.put("sampai.formated3", Common.dateFormat51.get().format(sampai));
			parameters.put("sampai.formated4", Common.timeFormat.get().format(sampai));
			parameters.put("sampai.formated5", Common.dateFormat1.get().format(sampai));
		}

		parameters.put("tanggal.formated1", Common.dateFormat6.get().format(tanggal));
		parameters.put("tanggal.formated2", Common.dateFormat2.get().format(tanggal));
		parameters.put("tanggal.formated3", Common.dateFormat51.get().format(tanggal));
		parameters.put("tanggal.formated4", Common.timeFormat.get().format(tanggal));
		parameters.put("tanggal.formated5", Common.dateFormat1.get().format(tanggal));

		Common.insertProperty(Guru.class, guru, parameters, "guru");

		if (guru.getSekolah() != null) {
			Common.insertProperty(Sekolah.class, guru.getSekolah(), parameters, "sekolah");
		}

		parameters.put("jenisCatatanGuru", j.getNama());

		List<Map> maps = new ArrayList<Map>();

		if (catatanGurua != null) {
			maps.add(initData(catatanGurua));
		} else {
			List<CatatanGuru> catatanGurus = HibernateUtil.currentSession().createCriteria(CatatanGuru.class)

					.add(ta == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("tahunAjaran", ta))
					.add(smt == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("semester", smt))

					.add(Restrictions
							.sqlRestriction("date(waktu) between date('" + Common.databaseDateFormat.get().format(tanggal)
									+ "') and date('" + Common.databaseDateFormat.get().format(sampai) + "')"))
					.add(Restrictions.eq("guru", guru)).add(Restrictions.eq("jenisCatatanGuru", j))
					.addOrder(Order.asc("waktu")).list();

			for (CatatanGuru catatanGuru : catatanGurus) {
				maps.add(initData(catatanGuru));
			}
		}

		parameters.put("maps", maps);

		return parameters;
	}

	@SuppressWarnings({ "unchecked" })
	public void onKHS(Event event) throws Exception {

		try {

			JenisCatatanGuru j = (JenisCatatanGuru) (jenisCatatanGuru.getSelectedItem() == null ? null
					: jenisCatatanGuru.getSelectedItem().getValue());
			if (j == null) {
				return;
			}

			LampiranLain lainMahaguru = LampiranLain.ambil(j.getId(),
					LampiranLain.FILE_JRXML_LAYOUT_JENIS_CATATAN_GURU);

			if (lainMahaguru == null) {
				MyMessageboxConfig.show("File laporan catatan guru belum diupload", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.INFORMATION);
				return;
			}

			File file = Report.generateCompileFileReport(Report.PDF, generateParameter(
					(Guru) bandboxGuru.getAttribute("guru"), tanggal.getValue(), sampai.getValue(),
					tahunAkademik.getSelectedItem() == null ? null
							: tahunAkademik.getSelectedItem().getValue().toString(),
					(Integer) (searchsmt.getSelectedItem() == null ? null : searchsmt.getSelectedItem().getValue()),
					null, j), lainMahaguru.ambilFile().getAbsolutePath(), ais.ui.util.WaktuUtil.getDate());
			CommonReport.tampilkanReportPDF(center, file);

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Catatan Guru", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini (mis. data akademik/keuangan/pegawai terkait) sudah lengkap dan benar, kemudian coba cetak ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void cetak(final CatatanGuru catatanGuru) throws Exception {

		try {

			JenisCatatanGuru j = catatanGuru.getJenisCatatanGuru();
			if (j == null) {

				return;
			}

			LampiranLain lainMahaguru = LampiranLain.ambil(j.getId(),
					LampiranLain.FILE_JRXML_LAYOUT_JENIS_FORM_CATATAN_GURU);

			if (lainMahaguru == null) {
//				MyMessageboxConfig.show("File template form catatan guru belum diupload", "Peringatan",
//						MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
				return;
			}

			Map parameters = generateParameter(catatanGuru.getGuru(), catatanGuru.getTanggal_dirubah(),
					catatanGuru.getTanggal_dirubah(), catatanGuru.getTahunAjaran(), catatanGuru.getSemester(),
					catatanGuru, catatanGuru.getJenisCatatanGuru());

			File file = Report.generateCompileFileReport(Report.PDF, parameters,
					lainMahaguru.ambilFile().getAbsolutePath(), ais.ui.util.WaktuUtil.getDate());

			MyWindow window = new MyWindow("Laporan", "none", true);
			window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
			window.setHeight("90%");
			window.setWidth("900px");

			Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
			borderlayout.setParent(window);

			Center center = new Center();
			ais.ui.util.ZkCompat.setFlex(center, true);
			center.setParent(borderlayout);
			CommonReport.tampilkanReportPDF(center, file);

			if (parameters == null || parameters.get("tidak_tampil_pilihan_export") == null) {
				org.zkoss.zul.North north = new org.zkoss.zul.North();
				north.setParent(borderlayout);
				north.appendChild(CommonReport.exportReport(new ParameterListener() {
					@Override
					public Map generateParameters() throws Exception {
						Map parameters = generateParameter(catatanGuru.getGuru(), catatanGuru.getTanggal_dirubah(),
								catatanGuru.getTanggal_dirubah(), catatanGuru.getTahunAjaran(),
								catatanGuru.getSemester(), catatanGuru, catatanGuru.getJenisCatatanGuru());
						return parameters;
					}
				}, LampiranLain.FILE_JRXML_LAYOUT_JENIS_FORM_CATATAN_GURU, null, null));
			}

			window.setVisible(true);
			window.onModal();

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Catatan Guru", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
					new String[] {
						"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
						"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}

	}
}
