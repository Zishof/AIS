package ais.action.report.format1.employ;
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
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.West;

import ais.action.master.akunting.helper.AmbilDataPegawaiBanbox;
import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.CatatanPegawai;
import ais.database.model.JenisCatatanPegawai;
import ais.database.model.KelompokParameterTambahanCatatanPegawai;
import ais.database.model.ParameterTambahan;
import ais.database.model.ParameterTambahanCatatanPegawai;
import ais.database.model.Pegawai;
import ais.database.model.file.LampiranLain;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Penyusun/penyaji laporan untuk laporan catatan pegawai. Kelas ini mengubah data domain menjadi
 * bentuk laporan yang dipakai UI, ekspor, atau proses cetak tanpa memindahkan aturan transaksi ke
 * lapisan report.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code AmbilDataPegawaiBanbox bandboxPegawai},
 * {@code Center center}, {@code MyDatebox tanggal}, {@code MyDatebox sampai}, {@code Combobox
 * jenisCatatanPegawai}, {@code String fileData}; inisialisasi/lifecycle ({@code init()}, {@code initData()});
 * pelaporan/ekspor ({@code cetak()}); operasi domain lain ({@code generateParameter()}, {@code onKHS()}). Bagian
 * lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class LaporanCatatanPegawai extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = -397946194166101691L;

	private AmbilDataPegawaiBanbox bandboxPegawai;
	private Center center;

	public LaporanCatatanPegawai() {
		super();
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Catatan Pegawai", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanCatatanPegawai(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);
		init();
	}

	private MyDatebox tanggal;

	private MyDatebox sampai;

	private Combobox jenisCatatanPegawai;

	private String fileData;

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

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(Common.getBahasa("label_pegawai")));
		row.appendChild(bandboxPegawai = new AmbilDataPegawaiBanbox());
		bandboxPegawai.setWidth("90%");

		if (Common.getCurrentUser() != null && Common.getCurrentUser().ambilPegawai() != null) {
			Pegawai pegawai = Common.getCurrentUser().ambilPegawai();
			bandboxPegawai.setAttribute("pegawai", pegawai);
			bandboxPegawai.setAttribute("myValue", pegawai);
			bandboxPegawai.setValue(pegawai.getNim() + " - " + pegawai.getNama());
			bandboxPegawai.setId("mhs_" + pegawai.getId());
			bandboxPegawai.setDisabled(true);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Catatan *"));
		row.appendChild(jenisCatatanPegawai = new Combobox());
		jenisCatatanPegawai.setWidth("90%");
		jenisCatatanPegawai.setReadonly(true);

		jenisCatatanPegawai.addEventListener("onChange", eventListener);

		Common.insertCombo(jenisCatatanPegawai, new String[] { "nama", "kode" }, "keterangan",
				JenisCatatanPegawai.class, Restrictions.eq("aktif", true));

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

				if (jenisCatatanPegawai.getSelectedItem() == null
						|| jenisCatatanPegawai.getSelectedItem().getValue() == null) {
					MyMessageboxConfig.show("Jenis Catatan Pegawai", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return null;
				}

				if (jenisCatatanPegawai.getSelectedItem() == null
						|| jenisCatatanPegawai.getSelectedItem().getValue() == null) {
					return null;
				}

				JenisCatatanPegawai j = (JenisCatatanPegawai) jenisCatatanPegawai.getSelectedItem().getValue();

				Map parameters = generateParameter((Pegawai) bandboxPegawai.getAttribute("pegawai"), tanggal.getValue(),
						sampai.getValue(), null, j);
				if (fileData != null) {
					parameters.put("nama_laporan", fileData);
				}
				return parameters;
			}
		}, "Catatan_Pegawai", null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onKHS(arg0);

			}
		}, false));

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static Map initData(CatatanPegawai catatanPegawai) {
		Map map = new HashMap();
		JenisCatatanPegawai ja = catatanPegawai.getJenisCatatanPegawai();
		Pegawai pegawai = catatanPegawai.getPegawai();
		Session session = HibernateUtil.currentSession();
		session.refresh(ja);

		map.put("id", catatanPegawai.getId());
		map.put("nama", catatanPegawai.getNama());
		map.put("waktu", catatanPegawai.getWaktu());
		map.put("tahunAjaran", catatanPegawai.getTahunAjaran());
		map.put("semester", catatanPegawai.getSemester());

		map.put("nama_peg", (pegawai.getNama()));
		map.put("pegawai_id", pegawai.getId());

		pegawai.putPhoto(map);

		Common.insertProperty(Pegawai.class, pegawai, map, "pegawai");

		if (catatanPegawai.getWaktu() != null) {
			map.put("waktu.formated1", Common.dateFormat6.get().format(catatanPegawai.getWaktu()));
			map.put("waktu.formated2", Common.dateFormat2.get().format(catatanPegawai.getWaktu()));
			map.put("waktu.formated3", Common.dateFormat51.get().format(catatanPegawai.getWaktu()));
			map.put("waktu.formated4", Common.timeFormat.get().format(catatanPegawai.getWaktu()));
			map.put("waktu.formated5", Common.dateFormat1.get().format(catatanPegawai.getWaktu()));
		}

		map.put("keterangan", catatanPegawai.getKeterangan());

		for (KelompokParameterTambahanCatatanPegawai kelompokParameterTambahanCatatanPegawai : ja
				.getKelompokParameterTambahanCatatanPegawais()) {
			map.put("kelompok_id", kelompokParameterTambahanCatatanPegawai.getId());
			map.put("kelompok", kelompokParameterTambahanCatatanPegawai.getNama());

			List<ParameterTambahan> parameterTambahans = ConstantValues
					.simpleList(
							session.createCriteria(ParameterTambahanCatatanPegawai.class)
									.add(Restrictions.eq("kelompokParameterTambahanCatatanPegawai",
											kelompokParameterTambahanCatatanPegawai))
									.createAlias("parameterTambahan", "parameterTambahan")
									.createAlias("kelompokParameterTambahanCatatanPegawai",
											"kelompokParameterTambahanCatatanPegawai")
									.add(Restrictions.eq("parameterTambahan.aktif", true))
									.add(Restrictions.eq("kelompokParameterTambahanCatatanPegawai.aktif", true))
									.setProjection(Projections.groupProperty("parameterTambahan.id")),
							ParameterTambahan.class, false);
			Collections.sort(parameterTambahans);

			for (ParameterTambahan parameterTambahan : parameterTambahans) {
				String jenis = kelompokParameterTambahanCatatanPegawai.getId() + "->" + parameterTambahan.getId();
				String jenis_id = kelompokParameterTambahanCatatanPegawai.getId() + "_" + parameterTambahan.getId();

				String val = "";
				String[] spl = catatanPegawai.getParameterTambahanInds().split("\n");
				for (String d : spl) {
					String[] value = d.split("<=>");
					if (value[0].trim().equalsIgnoreCase(jenis)) {
						val = value.length > 1 ? value[1].trim() : "";
					}
				}

				LampiranLain lampiranLain = LampiranLain.ambil(catatanPegawai.getId(), jenis);

				String vall = val;
				map.put("param.id." + parameterTambahan.getId(), vall);
				map.put("param.nama." + parameterTambahan.getNama().toLowerCase(), vall);
				map.put("param.kode." + parameterTambahan.getKode(), vall);
				if (parameterTambahan.getTipeDataInputan().equals(ParameterTambahan.ANGKA)
						|| parameterTambahan.getTipeDataInputan().equals(ParameterTambahan.TEXT_ANGKA)) {
					try {
						Double nilai = val.trim().isEmpty() || val.trim().equalsIgnoreCase("null") ? null
								: Double.parseDouble(val);
						map.put("param.id." + parameterTambahan.getId(), nilai);
						map.put("param.nama." + parameterTambahan.getNama().toLowerCase(), nilai);
						map.put("param.kode." + parameterTambahan.getKode(), nilai);
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/employ/LaporanCatatanPegawai.java:302");
						PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Catatan Pegawai", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
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

					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/employ/LaporanCatatanPegawai.java:320");
						PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Catatan Pegawai", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
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

									} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/employ/LaporanCatatanPegawai.java:349");
										PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Catatan Pegawai", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
											new String[] {
												"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
												"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
												"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
											});

									}
								}

							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/employ/LaporanCatatanPegawai.java:354");
								PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Catatan Pegawai", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
									new String[] {
										"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
										"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
										"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
									});

							}
						}

					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/employ/LaporanCatatanPegawai.java:359");
						PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Catatan Pegawai", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
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

					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/employ/LaporanCatatanPegawai.java:381");
						PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Catatan Pegawai", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
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

					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/employ/LaporanCatatanPegawai.java:402");
						PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Catatan Pegawai", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
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
	public static Map generateParameter(Pegawai pegawaiData, Date tanggal, Date sampai, CatatanPegawai catatanPegawaia,
			JenisCatatanPegawai j) throws Exception {

		Map parameters = ais.common.HashMapGenerator.getRandStringSerializable();

		if (j.getId() != null) {
			try {
				Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();
				List<LampiranLain> lampiranLains = streamingSession.createCriteria(LampiranLain.class)
						.addOrder(Order.asc("id")).add(Restrictions.eq("ref", j.getId()))
						.add(Restrictions.ilike("jenis", "Catatan_Pegawai_", MatchMode.START)).list();
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
				e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/report/format1/employ/LaporanCatatanPegawai.java:445");
			}
		}

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

		parameters.put("jenisCatatanPegawai", j.getNama());

		List<Map> maps = new ArrayList<Map>();

		if (catatanPegawaia != null) {
			maps.add(initData(catatanPegawaia));
		} else {
			List<CatatanPegawai> catatanPegawais = HibernateUtil.currentSession().createCriteria(CatatanPegawai.class)
					.add(Restrictions
							.sqlRestriction("date(waktu) between date('" + Common.databaseDateFormat.get().format(tanggal)
									+ "') and date('" + Common.databaseDateFormat.get().format(sampai) + "')"))
					.add(pegawaiData == null ? Restrictions.sqlRestriction("true")
							: Restrictions.eq("pegawai", pegawaiData))
					.add(Restrictions.eq("jenisCatatanPegawai", j)).addOrder(Order.asc("waktu")).list();

			for (CatatanPegawai catatanPegawai : catatanPegawais) {
				maps.add(initData(catatanPegawai));
			}
		}

		parameters.put("maps", maps);

		return parameters;
	}

	@SuppressWarnings({ "unchecked" })
	public void onKHS(Event event) throws Exception {

		try {

			JenisCatatanPegawai j = (JenisCatatanPegawai) (jenisCatatanPegawai.getSelectedItem() == null ? null
					: jenisCatatanPegawai.getSelectedItem().getValue());
			if (j == null) {
				return;
			}

			LampiranLain lainMahapegawai = LampiranLain.ambil(j.getId(),
					LampiranLain.FILE_JRXML_LAYOUT_JENIS_CATATAN_PEGAWAI);

			if (lainMahapegawai == null) {
				MyMessageboxConfig.show("File laporan catatan pegawai belum diupload", "Peringatan",
						MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
				return;
			}

			File file = Report.generateCompileFileReport(Report.PDF,
					generateParameter((Pegawai) bandboxPegawai.getAttribute("pegawai"), tanggal.getValue(),
							sampai.getValue(), null, j),
					fileData = lainMahapegawai.ambilFile().getAbsolutePath(), ais.ui.util.WaktuUtil.getDate());
			CommonReport.tampilkanReportPDF(center, file);

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Catatan Pegawai", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini (mis. data akademik/keuangan/pegawai terkait) sudah lengkap dan benar, kemudian coba cetak ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void cetak(final CatatanPegawai catatanPegawai) throws Exception {

		try {

			JenisCatatanPegawai j = catatanPegawai.getJenisCatatanPegawai();
			if (j == null) {

				return;
			}

			LampiranLain lainMahapegawai = LampiranLain.ambil(j.getId(),
					LampiranLain.FILE_JRXML_LAYOUT_JENIS_FORM_CATATAN_PEGAWAI);

			if (lainMahapegawai == null) {
//				MyMessageboxConfig.show("File template form catatan pegawai belum diupload", "Peringatan",
//						MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
				return;
			}

			Map parameters = generateParameter(catatanPegawai.getPegawai(), catatanPegawai.getTanggal_dirubah(),
					catatanPegawai.getTanggal_dirubah(), catatanPegawai, catatanPegawai.getJenisCatatanPegawai());
			final String fileData;
			File file = Report.generateCompileFileReport(Report.PDF, parameters,
					fileData = lainMahapegawai.ambilFile().getAbsolutePath(), ais.ui.util.WaktuUtil.getDate());

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
						Map parameters = generateParameter(catatanPegawai.getPegawai(),
								catatanPegawai.getTanggal_dirubah(), catatanPegawai.getTanggal_dirubah(),
								catatanPegawai, catatanPegawai.getJenisCatatanPegawai());
						if (fileData != null) {
							parameters.put("nama_laporan", fileData);
						}
						return parameters;
					}
				}, LampiranLain.FILE_JRXML_LAYOUT_JENIS_FORM_CATATAN_PEGAWAI, null, null));
			}

			window.setVisible(true);
			window.onModal();

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Catatan Pegawai", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
					new String[] {
						"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
						"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}

	}
}
