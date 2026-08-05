package ais.action.report.format1.akademik;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.West;

import ais.action.master.akunting.helper.AmbilDataPegawaiBanbox;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.JenisPengajuanPegawai;
import ais.database.model.KelompokParameterTambahanPengajuanPegawai;
import ais.database.model.ParameterTambahan;
import ais.database.model.ParameterTambahanPengajuanPegawai;
import ais.database.model.Pegawai;
import ais.database.model.PengajuanPegawai;
import ais.database.model.file.LampiranLain;
import ais.database.model.rab.SatuanKerja;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;

public class LaporanPengajuanPegawai extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = -397946194166101691L;

	private Center center;

	private JenisPengajuanPegawai jenis = null;

	public LaporanPengajuanPegawai() {
		super();
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Pengajuan Pegawai", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanPengajuanPegawai(JenisPengajuanPegawai jenis) {
		super();
		try {
			this.jenis = jenis;
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Pengajuan Pegawai", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanPengajuanPegawai(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);
		init();
	}

	private MyDatebox tanggal;
	private MyDatebox sampai;

	private Combobox jenisPengajuanPegawai;
	private AmbilDataSatuanKerjaBanbox searchparent;
	private AmbilDataPegawaiBanbox pegawai;

	private SatuanKerjaTreeModel satuanKerjaTreeModel;

	private void init() throws Exception {

		searchparent = new AmbilDataSatuanKerjaBanbox();
		searchparent.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onKHS(arg0);
			}
		});

		satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);

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
		column.setWidth("30%");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		jenisPengajuanPegawai = new Combobox();

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Pengajuan *"));

		if (jenis != null) {
			row.appendChild(new Label(jenis.getNama()));
		} else {
			row.appendChild(jenisPengajuanPegawai);
		}
		jenisPengajuanPegawai.setWidth("90%");
		jenisPengajuanPegawai.setReadonly(true);

		Common.insertCombo(jenisPengajuanPegawai, new String[] { "nama", "kode" }, "keterangan",
				JenisPengajuanPegawai.class, Restrictions.eq("aktif", true));
		if (jenis != null) {
			Common.selectComboItem(jenisPengajuanPegawai, jenis);
			jenisPengajuanPegawai.setReadonly(true);
		}

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR) - 1);

		jenisPengajuanPegawai.addEventListener("onChange", eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Satuan Kerja"));
		row.appendChild(searchparent);
		searchparent.setWidth("90%");
		searchparent.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pegawai"));
		row.appendChild(pegawai = new AmbilDataPegawaiBanbox());
		pegawai.setWidth("90%");
		pegawai.setReadonly(true);
		pegawai.setEventListener(eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Mulai"));
		tanggal = new MyDatebox(calendar.getTime());
		row.appendChild(tanggal);
		tanggal.setWidth("90%");
		tanggal.addEventListener("onChange", eventListener);
		tanggal.setReadonly(true);

		calendar = ais.ui.util.WaktuUtil.getCalendar();

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sampai"));
		sampai = new MyDatebox(ais.ui.util.WaktuUtil.getDate());
		row.appendChild(sampai);
		sampai.setWidth("90%");
		sampai.addEventListener("onChange", eventListener);
		sampai.setReadonly(true);

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

				if (jenisPengajuanPegawai.getSelectedItem() == null
						|| jenisPengajuanPegawai.getSelectedItem().getValue() == null) {
					MyMessageboxConfig.show("Jenis Pengajuan Pegawai", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return null;
				}

				JenisPengajuanPegawai j = (JenisPengajuanPegawai) (jenisPengajuanPegawai.getSelectedItem() == null
						? null
						: jenisPengajuanPegawai.getSelectedItem().getValue());

				SatuanKerja parent = (SatuanKerja) searchparent.getAttribute("satuanKerja");
				Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
				if (parent != null) {
					satuanKerjas.clear();
					satuanKerjas.add(parent);
					satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
				}

				Map parameters = generateParameter(j, tanggal.getValue(), sampai.getValue(),
						(Pegawai) pegawai.getAttribute("pegawai"), null, satuanKerjas);
				return parameters;
			}
		}, "Pengajuan_Pegawai", null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onKHS(arg0);

			}
		}));

		if (jenis != null) {
			Common.createDefaultTimer(eventListener);
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static Map generateParameter(JenisPengajuanPegawai j, Date tanggal, Date sampai, Pegawai pegawai,
			PengajuanPegawai pengajuanPegawaiData, Set<SatuanKerja> satuanKerjas) throws Exception {

		if (j == null) {
			return null;
		}

		Map parameters = ais.common.HashMapGenerator.getRandStringSerializable();
		// EKSPOR DINAMIS: jrxml terupload per jenis dipakai utk SEMUA format cetak (PDF/XLS/DOCX/PPTX).
		if (j != null && j.getId() != null) {
			try {
				LampiranLain layoutDinamis = LampiranLain.ambil(j.getId(),
						LampiranLain.FILE_JRXML_LAYOUT_JENIS_PENGAJUAN);
				if (layoutDinamis != null && layoutDinamis.ambilFile() != null
						&& layoutDinamis.ambilFile().exists()) {
					parameters.put("nama_laporan", layoutDinamis.ambilFile().getAbsolutePath());
				}
			} catch (Throwable abaikan) { ais.common.ErrorAuditUtil.record(abaikan, "auto-audit(empty-catch) src/ais/action/report/format1/akademik/LaporanPengajuanPegawai.java:281");
			}
		}

		parameters.put("tanggal", tanggal);
		parameters.put("sampai", sampai);

		parameters.put("tanggal.format", tanggal == null ? "" : Common.dateFormat2.get().format(tanggal));
		parameters.put("sampai.format", sampai == null ? "" : Common.dateFormat2.get().format(sampai));

		parameters.put("jenisPengajuanPegawai", j.getNama());

		List<Map> maps = new ArrayList<Map>();

		List<PengajuanPegawai> pengajuanPegawais = new ArrayList<PengajuanPegawai>();

		if (pengajuanPegawaiData == null) {
			pengajuanPegawais = HibernateUtil.currentSession().createCriteria(PengajuanPegawai.class)
					.add(Restrictions.eq("setujui", true))
					.add(pegawai == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("pegawai", pegawai))
					.add(satuanKerjas == null || satuanKerjas.isEmpty() ? Restrictions.sqlRestriction("true")
							: Restrictions.or(Restrictions.isNull("satuanKerja"),
									Restrictions.in("satuanKerja", satuanKerjas)))
					.add(Restrictions.or(
							Restrictions.sqlRestriction("date('" + Common.databaseDateFormat.get().format(WaktuUtil.getDate())
									+ "') between date(this_.waktu) and date(this_.waktusampai)"),
							Restrictions.sqlRestriction(
									"date(waktu) between date('" + Common.databaseDateFormat.get().format(tanggal)
											+ "') and date('" + Common.databaseDateFormat.get().format(sampai) + "')")))
					.add(Restrictions.eq("jenisPengajuanPegawai", j)).addOrder(Order.asc("waktu")).list();
		} else {
			pengajuanPegawais.add(pengajuanPegawaiData);
		}

		for (PengajuanPegawai pengajuanPegawai : pengajuanPegawais) {
			Map map = new HashMap();
			JenisPengajuanPegawai ja = pengajuanPegawai.getJenisPengajuanPegawai();
			Session session = HibernateUtil.currentSession();
			session.refresh(ja);

			map.put("id", pengajuanPegawai.getId());
			map.put("nama", pengajuanPegawai.getNama());
			map.put("waktu", pengajuanPegawai.getWaktu());
			map.put("keterangan", pengajuanPegawai.getKeterangan());

			Common.insertProperty(PengajuanPegawai.class, pengajuanPegawai, map, "pengajuan");
			Common.insertProperty(Pegawai.class, pengajuanPegawai.getPegawai(), map, "pegawai");
			Common.insertProperty(JenisPengajuanPegawai.class, ja, map, "jenis");

			for (KelompokParameterTambahanPengajuanPegawai kelompokParameterTambahanPengajuanPegawai : ja
					.getKelompokParameterTambahanPengajuanPegawais()) {
				map.put("kelompok_id", kelompokParameterTambahanPengajuanPegawai.getId());
				map.put("kelompok", kelompokParameterTambahanPengajuanPegawai.getNama());

				List<ParameterTambahan> parameterTambahans = ConstantValues.simpleList(
						session.createCriteria(ParameterTambahanPengajuanPegawai.class)
								.add(Restrictions.eq("kelompokParameterTambahanPengajuanPegawai",
										kelompokParameterTambahanPengajuanPegawai))
								.createAlias("parameterTambahan", "parameterTambahan")
								.createAlias("kelompokParameterTambahanPengajuanPegawai",
										"kelompokParameterTambahanPengajuanPegawai")
								.add(Restrictions.eq("parameterTambahan.aktif", true))
								.add(Restrictions.eq("kelompokParameterTambahanPengajuanPegawai.aktif", true))
								.setProjection(Projections.groupProperty("parameterTambahan.id")),
						ParameterTambahan.class, false);
				Collections.sort(parameterTambahans);

				for (ParameterTambahan parameterTambahan : parameterTambahans) {
					String jenis = kelompokParameterTambahanPengajuanPegawai.getId() + "->" + parameterTambahan.getId();
					String jenis_id = kelompokParameterTambahanPengajuanPegawai.getId() + "_"
							+ parameterTambahan.getId();

					String val = "";
					String[] spl = pengajuanPegawai.getParameterTambahanInds().split("\n");
					for (String d : spl) {
						String[] value = d.split("<=>");
						if (value[0].trim().equalsIgnoreCase(jenis)) {
							val = value.length > 1 ? value[1].trim() : "";
						}
					}

					LampiranLain lampiranLain = LampiranLain.ambil(pengajuanPegawai.getId(), jenis);

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
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/akademik/LaporanPengajuanPegawai.java:376");
							PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Pengajuan Pegawai", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
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
						map.put("param.file.", lampiranLain.ambilFile().getAbsolutePath());
						map.put(jenis_id + "_url", lampiranLain.ambilFile().getAbsolutePath());
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

						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/akademik/LaporanPengajuanPegawai.java:403");
							PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Pengajuan Pegawai", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
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

						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/akademik/LaporanPengajuanPegawai.java:424");
							PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Pengajuan Pegawai", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
								new String[] {
									"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
									"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
									"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
								});

						}
					}
				}

			}

			maps.add(map);
		}

		parameters.put("maps", maps);

//		System.out.println("parameters => " + parameters);

		return parameters;
	}

	@SuppressWarnings({ "unchecked" })
	public void onKHS(Event event) throws Exception {

		try {

			JenisPengajuanPegawai j = (JenisPengajuanPegawai) (jenisPengajuanPegawai.getSelectedItem() == null ? null
					: jenisPengajuanPegawai.getSelectedItem().getValue());
			if (j == null) {
				return;
			}

			LampiranLain lainMahaadministrasi = LampiranLain.ambil(j.getId(),
					LampiranLain.FILE_JRXML_LAYOUT_JENIS_PENGAJUAN);

			if (lainMahaadministrasi == null) {
				MyMessageboxConfig.show("File laporan Pengajuan Pegawai belum diupload", "Peringatan",
						MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
				return;
			}

			SatuanKerja parent = (SatuanKerja) searchparent.getAttribute("satuanKerja");
			Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
			if (parent != null) {
				satuanKerjas.clear();
				satuanKerjas.add(parent);
				satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
			}

			File file = Report.generateCompileFileReport(Report.PDF,
					generateParameter(j, tanggal.getValue(), sampai.getValue(),
							(Pegawai) pegawai.getAttribute("pegawai"), null, satuanKerjas),
					lainMahaadministrasi.ambilFile().getAbsolutePath(), ais.ui.util.WaktuUtil.getDate());
			CommonReport.tampilkanReportPDF(center, file);

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Pengajuan Pegawai", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini (mis. data akademik/keuangan/pegawai terkait) sudah lengkap dan benar, kemudian coba cetak ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}

	}

}
