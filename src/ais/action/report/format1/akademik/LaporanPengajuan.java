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
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.West;

import ais.action.master.helper.AmbilDataMahasiswaBanbox;
import ais.action.master.sekolah.helper.AmbilDataSiswaBanbox;
import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.JenisPengajuan;
import ais.database.model.KelompokParameterTambahanPengajuan;
import ais.database.model.Mahasiswa;
import ais.database.model.ParameterTambahan;
import ais.database.model.ParameterTambahanPengajuan;
import ais.database.model.PengajuanMahasiswa;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranLain;
import ais.database.model.sekolah.PengajuanSiswa;
import ais.database.model.sekolah.Siswa;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyWindow;

public class LaporanPengajuan extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = -397946194166101691L;

	private Center center;

	public LaporanPengajuan() {
		super();
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Pengajuan", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanPengajuan(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);
		init();
	}

	private MyDatebox tanggal;

	private MyDatebox sampai;

	private Combobox jenisPengajuan;

	private AmbilDataMahasiswaBanbox mahasiswa;
	private AmbilDataSiswaBanbox siswa;

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
		column.setWidth("35%");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Pengajuan *"));
		row.appendChild(jenisPengajuan = new Combobox());
		jenisPengajuan.setWidth("90%");
		jenisPengajuan.setReadonly(true);

		Common.insertCombo(jenisPengajuan, new String[] { "nama", "kode" }, "keterangan", JenisPengajuan.class,
				Restrictions.eq("aktif", true));

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR) - 1);

		jenisPengajuan.addEventListener("onChange", eventListener);

		Tbmuser tbmuser = Common.getCurrentUser();

		boolean[] ptYa = Common.chekPtAtauSekolah();
		boolean pt = ptYa[0];
		boolean ya = ptYa[1];

		row = new MyFormRow();
		row.setVisible(pt);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Mahasiswa (*)"));
		row.appendChild(mahasiswa = new AmbilDataMahasiswaBanbox());
		mahasiswa.setWidth("90%");

		Mahasiswa mahasiswaTerpilih = tbmuser == null ? null : tbmuser.getMahasiswa();
		if (mahasiswaTerpilih != null) {
			mahasiswa.setAttribute("myValue", mahasiswaTerpilih);
			mahasiswa.setAttribute("mahasiswa", mahasiswaTerpilih);
			mahasiswa.setValue(mahasiswaTerpilih.getNama());
			mahasiswa.setDisabled(true);
		}

		row = new MyFormRow();
		row.setVisible(ya);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Siswa (*)"));
		row.appendChild(siswa = new AmbilDataSiswaBanbox());
		siswa.setWidth("90%");

		Siswa siswaTerpilih = tbmuser == null ? null : tbmuser.getSiswa();
		if (siswaTerpilih != null) {
			siswa.setAttribute("myValue", siswaTerpilih);
			siswa.setAttribute("siswa", siswaTerpilih);
			siswa.setValue(siswaTerpilih.getNama());
			siswa.setDisabled(true);
		}

		EventListener eventListenerData = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Mahasiswa m = (Mahasiswa) mahasiswa.getAttribute("mahasiswa");
				Siswa s = (Siswa) siswa.getAttribute("siswa");
				mahasiswa.getParent().setVisible(true);
				siswa.getParent().setVisible(true);
				if (m != null) {
					siswa.getParent().setVisible(false);
				}
				if (s != null) {
					mahasiswa.getParent().setVisible(false);

				}

				onKHS(arg0);
			}
		};

		eventListenerData.onEvent(null);
		mahasiswa.setEventListener(eventListenerData);
		siswa.setEventListener(eventListenerData);

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
				
				if (mahasiswa.getParent().isVisible() && siswa.getParent().isVisible()) {
					MyMessageboxConfig.show("Siswa dan mahasiswa belum dipilih", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return null;
				}


				if (jenisPengajuan.getSelectedItem() == null || jenisPengajuan.getSelectedItem().getValue() == null) {
					MyMessageboxConfig.show("Jenis Pengajuan", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return null;
				}

				JenisPengajuan j = (JenisPengajuan) (jenisPengajuan.getSelectedItem() == null ? null
						: jenisPengajuan.getSelectedItem().getValue());

				Map parameters = mahasiswa.getParent().isVisible()
						? generateParameter(j, tanggal.getValue(), sampai.getValue(),
								(Mahasiswa) mahasiswa.getAttribute("mahasiswa"), null)
						: generateParameter(j, tanggal.getValue(), sampai.getValue(),
								(Siswa) siswa.getAttribute("siswa"), null);
				return parameters;
			}
		}, "Pengajuan_Pegawai", null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onKHS(arg0);

			}
		}));

	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static Map generateParameter(JenisPengajuan j, Date tanggal, Date sampai, Mahasiswa mahasiswa,
			PengajuanMahasiswa pengajuanData) throws Exception {

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
			} catch (Throwable abaikan) { ais.common.ErrorAuditUtil.record(abaikan, "auto-audit(empty-catch) src/ais/action/report/format1/akademik/LaporanPengajuan.java:285");
			}
		}

		parameters.put("tanggal", tanggal);
		parameters.put("sampai", sampai);

		parameters.put("jenisPengajuan", j.getNama());

		List<Map> maps = new ArrayList<Map>();

		List<PengajuanMahasiswa> pengajuans = new ArrayList<PengajuanMahasiswa>();

		if (pengajuanData == null) {
			pengajuans = HibernateUtil.currentSession().createCriteria(PengajuanMahasiswa.class)

					.add(mahasiswa == null ? Restrictions.sqlRestriction("true")
							: Restrictions.eq("mahasiswa", mahasiswa))
					.add(Restrictions
							.sqlRestriction("date(tanggal) between date('" + Common.databaseDateFormat.get().format(tanggal)
									+ "') and date('" + Common.databaseDateFormat.get().format(sampai) + "')"))
					.add(Restrictions.eq("jenisPengajuan", j)).addOrder(Order.asc("tanggal")).list();
		} else {
			pengajuans.add(pengajuanData);
		}

		for (PengajuanMahasiswa pengajuan : pengajuans) {
			Map map = new HashMap();
			JenisPengajuan ja = pengajuan.getJenisPengajuan();
			Session session = HibernateUtil.currentSession();
			session.refresh(ja);

			map.put("id", pengajuan.getId());
			map.put("nama", pengajuan.getNama());
			map.put("mulai", pengajuan.getWaktuMulai());
			map.put("selesai", pengajuan.getWaktuSelesai());
			map.put("keterangan", pengajuan.getKeterangan());

			Common.insertProperty(PengajuanMahasiswa.class, pengajuan, map, "pengajuan");
			Common.insertProperty(Mahasiswa.class, pengajuan.getMahasiswa(), map, "mahasiswa");
			Common.insertProperty(JenisPengajuan.class, ja, map, "jenis");

			for (KelompokParameterTambahanPengajuan kelompokParameterTambahanPengajuan : ja
					.getKelompokParameterTambahanPengajuans()) {
				map.put("kelompok_id", kelompokParameterTambahanPengajuan.getId());
				map.put("kelompok", kelompokParameterTambahanPengajuan.getNama());

				List<ParameterTambahan> parameterTambahans = ConstantValues.simpleList(
						session.createCriteria(ParameterTambahanPengajuan.class)
								.add(Restrictions.eq("kelompokParameterTambahanPengajuan",
										kelompokParameterTambahanPengajuan))
								.createAlias("parameterTambahan", "parameterTambahan")
								.createAlias("kelompokParameterTambahanPengajuan", "kelompokParameterTambahanPengajuan")
								.add(Restrictions.eq("parameterTambahan.aktif", true))
								.add(Restrictions.eq("kelompokParameterTambahanPengajuan.aktif", true))
								.setProjection(Projections.groupProperty("parameterTambahan.id")),
						ParameterTambahan.class, false);
				Collections.sort(parameterTambahans);

				for (ParameterTambahan parameterTambahan : parameterTambahans) {
					String jenis = kelompokParameterTambahanPengajuan.getId() + "->" + parameterTambahan.getId();
					String jenis_id = kelompokParameterTambahanPengajuan.getId() + "_" + parameterTambahan.getId();

					String val = "";
					String[] spl = pengajuan.getParameterTambahanInds().split("\n");
					for (String d : spl) {
						String[] value = d.split("<=>");
						if (value[0].trim().equalsIgnoreCase(jenis)) {
							val = value.length > 1 ? value[1].trim() : "";
						}
					}

					LampiranLain lampiranLain = LampiranLain.ambil(pengajuan.getId(), jenis);

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
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/akademik/LaporanPengajuan.java:371");
							PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Pengajuan", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
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

						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/akademik/LaporanPengajuan.java:398");
							PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Pengajuan", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
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

						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/akademik/LaporanPengajuan.java:419");
							PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Pengajuan", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
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

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static Map generateParameter(JenisPengajuan j, Date tanggal, Date sampai, Siswa siswa,
			PengajuanSiswa pengajuanData) throws Exception {

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
			} catch (Throwable abaikan) { ais.common.ErrorAuditUtil.record(abaikan, "auto-audit(empty-catch) src/ais/action/report/format1/akademik/LaporanPengajuan.java:455");
			}
		}

		parameters.put("tanggal", tanggal);
		parameters.put("sampai", sampai);

		parameters.put("jenisPengajuan", j.getNama());

		List<Map> maps = new ArrayList<Map>();

		List<PengajuanSiswa> pengajuans = new ArrayList<PengajuanSiswa>();

		if (pengajuanData == null) {
			pengajuans = HibernateUtil.currentSession().createCriteria(PengajuanSiswa.class)

					.add(siswa == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("siswa", siswa))
					.add(Restrictions
							.sqlRestriction("date(tanggal) between date('" + Common.databaseDateFormat.get().format(tanggal)
									+ "') and date('" + Common.databaseDateFormat.get().format(sampai) + "')"))
					.add(Restrictions.eq("jenisPengajuan", j)).addOrder(Order.asc("tanggal")).list();
		} else {
			pengajuans.add(pengajuanData);
		}

		for (PengajuanSiswa pengajuan : pengajuans) {
			Map map = new HashMap();
			JenisPengajuan ja = pengajuan.getJenisPengajuan();
			Session session = HibernateUtil.currentSession();
			session.refresh(ja);

			map.put("id", pengajuan.getId());
			map.put("nama", pengajuan.getNama());
			map.put("mulai", pengajuan.getWaktuMulai());
			map.put("selesai", pengajuan.getWaktuSelesai());
			map.put("keterangan", pengajuan.getKeterangan());

			Common.insertProperty(PengajuanSiswa.class, pengajuan, map, "pengajuan");
			Common.insertProperty(Siswa.class, pengajuan.getSiswa(), map, "siswa");
			Common.insertProperty(JenisPengajuan.class, ja, map, "jenis");

			for (KelompokParameterTambahanPengajuan kelompokParameterTambahanPengajuan : ja
					.getKelompokParameterTambahanPengajuans()) {
				map.put("kelompok_id", kelompokParameterTambahanPengajuan.getId());
				map.put("kelompok", kelompokParameterTambahanPengajuan.getNama());

				List<ParameterTambahan> parameterTambahans = ConstantValues.simpleList(
						session.createCriteria(ParameterTambahanPengajuan.class)
								.add(Restrictions.eq("kelompokParameterTambahanPengajuan",
										kelompokParameterTambahanPengajuan))
								.createAlias("parameterTambahan", "parameterTambahan")
								.createAlias("kelompokParameterTambahanPengajuan", "kelompokParameterTambahanPengajuan")
								.add(Restrictions.eq("parameterTambahan.aktif", true))
								.add(Restrictions.eq("kelompokParameterTambahanPengajuan.aktif", true))
								.setProjection(Projections.groupProperty("parameterTambahan.id")),
						ParameterTambahan.class, false);
				Collections.sort(parameterTambahans);

				for (ParameterTambahan parameterTambahan : parameterTambahans) {
					String jenis = kelompokParameterTambahanPengajuan.getId() + "->" + parameterTambahan.getId();
					String jenis_id = kelompokParameterTambahanPengajuan.getId() + "_" + parameterTambahan.getId();

					String val = "";
					String[] spl = pengajuan.getParameterTambahanInds().split("\n");
					for (String d : spl) {
						String[] value = d.split("<=>");
						if (value[0].trim().equalsIgnoreCase(jenis)) {
							val = value.length > 1 ? value[1].trim() : "";
						}
					}

					LampiranLain lampiranLain = LampiranLain.ambil(pengajuan.getId(), jenis);

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
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/akademik/LaporanPengajuan.java:540");
							PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Pengajuan", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
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

						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/akademik/LaporanPengajuan.java:567");
							PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Pengajuan", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
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

						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/akademik/LaporanPengajuan.java:588");
							PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Pengajuan", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
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

			if (mahasiswa.getParent().isVisible() && siswa.getParent().isVisible()) {
				MyMessageboxConfig.show("Siswa dan mahasiswa belum dipilih", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.INFORMATION);
				return;
			}

			JenisPengajuan j = (JenisPengajuan) (jenisPengajuan.getSelectedItem() == null ? null
					: jenisPengajuan.getSelectedItem().getValue());
			if (j == null) {
				return;
			}

			LampiranLain lainMahaadministrasi = LampiranLain.ambil(j.getId(),
					LampiranLain.FILE_JRXML_LAYOUT_JENIS_PENGAJUAN_MHS);

			if (lainMahaadministrasi == null) {
				MyMessageboxConfig.show("File laporan Pengajuan belum diupload", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.INFORMATION);
				return;
			}

			File file = mahasiswa.getParent().isVisible()
					? Report.generateCompileFileReport(Report.PDF,
							generateParameter(j, tanggal.getValue(), sampai.getValue(),
									(Mahasiswa) mahasiswa.getAttribute("mahasiswa"), null),
							lainMahaadministrasi.ambilFile().getAbsolutePath(), ais.ui.util.WaktuUtil.getDate())
					: Report.generateCompileFileReport(Report.PDF,
							generateParameter(j, tanggal.getValue(), sampai.getValue(),
									(Siswa) siswa.getAttribute("siswa"), null),
							lainMahaadministrasi.ambilFile().getAbsolutePath(), ais.ui.util.WaktuUtil.getDate());
			CommonReport.tampilkanReportPDF(center, file);

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Pengajuan", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini (mis. data akademik/keuangan/pegawai terkait) sudah lengkap dan benar, kemudian coba cetak ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}

	}

}
