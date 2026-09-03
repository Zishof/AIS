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
import ais.action.report.format1.payroll.LaporanCutiPegawai;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.CutiBersama;
import ais.database.model.ParameterTambahan;
import ais.database.model.Pegawai;
import ais.database.model.employ.JenisCutiDanIzin;
import ais.database.model.employ.KelompokParameterTambahanCutiDanIzin;
import ais.database.model.employ.ParameterTambahanCutiDanIzin;
import ais.database.model.file.LampiranLain;
import ais.database.model.payroll.CutiDanIzin;
import ais.database.model.sop.DisposisiAlurSop;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyWindow;

/**
 * Penyusun/penyaji laporan untuk laporan cuti dan izin. Kelas ini mengubah data domain menjadi
 * bentuk laporan yang dipakai UI, ekspor, atau proses cetak tanpa memindahkan aturan transaksi ke
 * lapisan report.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Center center}, {@code MyDatebox
 * tanggal}, {@code MyDatebox sampai}, {@code Combobox jenisCutiDanIzin}, {@code AmbilDataPegawaiBanbox pegawai};
 * inisialisasi/lifecycle ({@code init()}); pelaporan/ekspor ({@code cetak()}); operasi domain lain ({@code
 * generateParameter()}, {@code onKHS()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface
 * yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class LaporanCutiDanIzin extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = -397946194166101691L;

	private Center center;

	public LaporanCutiDanIzin() {
		super();
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Cuti Dan Izin", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanCutiDanIzin(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);
		init();
	}

	private MyDatebox tanggal;

	private MyDatebox sampai;

	private Combobox jenisCutiDanIzin;

	private AmbilDataPegawaiBanbox pegawai;

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
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Pengajuan *"));
		row.appendChild(jenisCutiDanIzin = new Combobox());
		jenisCutiDanIzin.setWidth("90%");
		jenisCutiDanIzin.setReadonly(true);

		Common.insertCombo(jenisCutiDanIzin, new String[] { "nama", "kode" }, "keterangan", JenisCutiDanIzin.class,
				Restrictions.eq("aktif", true));

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR) - 1);

		jenisCutiDanIzin.addEventListener("onChange", eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pegawai *"));
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

				if (jenisCutiDanIzin.getSelectedItem() == null
						|| jenisCutiDanIzin.getSelectedItem().getValue() == null) {
					MyMessageboxConfig.show("Mohon maaf, Jenis Pengajuan Pegawai belum dipilih. Langkah yang dapat dilakukan: (1) Pilih Jenis Pengajuan (Cuti/Izin) dari daftar dropdown; (2) Pastikan data jenis pengajuan sudah dikonfigurasi di sistem; (3) Ulangi proses cetak laporan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return null;
				}

				JenisCutiDanIzin j = (JenisCutiDanIzin) (jenisCutiDanIzin.getSelectedItem() == null ? null
						: jenisCutiDanIzin.getSelectedItem().getValue());

				Map parameters = generateParameter(j, tanggal.getValue(), sampai.getValue(),
						(Pegawai) pegawai.getAttribute("pegawai"), null);
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
	public static Map generateParameter(JenisCutiDanIzin j, Date tanggal, Date sampai, Pegawai pegawai,
			CutiDanIzin cutiDanIzinData) throws Exception {

		if (j == null) {
			return null;
		}

		Map parameters = ais.common.HashMapGenerator.getRandStringSerializable();
		// EKSPOR DINAMIS: jrxml terupload per jenis dipakai utk SEMUA format cetak (PDF/XLS/DOCX/PPTX).
		if (j != null && j.getId() != null) {
			try {
				LampiranLain layoutDinamis = LampiranLain.ambil(j.getId(),
						LampiranLain.FILE_JRXML_LAYOUT_JENIS_CUTI_DAN_IZIN);
				if (layoutDinamis != null && layoutDinamis.ambilFile() != null
						&& layoutDinamis.ambilFile().exists()) {
					parameters.put("nama_laporan", layoutDinamis.ambilFile().getAbsolutePath());
				}
			} catch (Throwable abaikan) { ais.common.ErrorAuditUtil.record(abaikan, "auto-audit(empty-catch) src/ais/action/report/format1/akademik/LaporanCutiDanIzin.java:223");
			}
		}

		parameters.put("tanggal", tanggal);
		parameters.put("sampai", sampai);

		parameters.put("jenisCutiDanIzin", j.getNama());

		if (cutiDanIzinData != null) {
			DisposisiAlurSop.parameterMap(cutiDanIzinData.getDisposisiSop(), parameters);

			int jumlahCuti = Common.getWorkingDaysBetweenTwoDates(cutiDanIzinData.getMulai(),
					cutiDanIzinData.getSampai()) + 1;
			parameters.put("jumlah_cuti", jumlahCuti);
		}

		List<Map> maps = new ArrayList<Map>();

		List<CutiDanIzin> cutiDanIzins = new ArrayList<CutiDanIzin>();

		if (cutiDanIzinData == null) {
			cutiDanIzins = HibernateUtil.currentSession().createCriteria(CutiDanIzin.class)

					.add(Restrictions.eq("setujui", true))

					.add(pegawai == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("pegawai", pegawai))

					.add(Restrictions.or(
							Restrictions.sqlRestriction(
									"date(mulai) between date('" + Common.databaseDateFormat.get().format(tanggal)
											+ "') and date('" + Common.databaseDateFormat.get().format(sampai) + "')"),
							Restrictions.sqlRestriction(
									"date(sampai) between date('" + Common.databaseDateFormat.get().format(tanggal)
											+ "') and date('" + Common.databaseDateFormat.get().format(sampai) + "')"))

					).add(Restrictions.eq("jenisCutiDanIzin", j)).addOrder(Order.asc("mulai")).list();
		} else {
			cutiDanIzins.add(cutiDanIzinData);

		}

		Map mapdata = new HashMap();

		if (!cutiDanIzins.isEmpty()) {

			cutiDanIzinData = cutiDanIzins.get(0);
			Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
			calendar.setTime(cutiDanIzinData.getMulai());
			int tahun = calendar.get(Calendar.YEAR);

			Session session = ais.action.report.Report.openNativeSession();

			CutiBersama cutiBersama = (CutiBersama) session.createCriteria(CutiBersama.class)
					.add(Restrictions.eq("tahun", tahun)).setMaxResults(1).uniqueResult();
			if (cutiBersama == null) {
				cutiBersama = new CutiBersama();
			}

			int jumlahCuti = pegawai.getJatahCutiTahunan() == null ? cutiBersama.getJumlahCuti()
					: pegawai.getJatahCutiTahunan();
			int jumlahCutiYangBisaDiambil = jumlahCuti - cutiBersama.getJumlahCutiBersama();

			mapdata.put("jumlahCutiTotal", jumlahCuti);
			mapdata.put("jumlahCutiBersama", cutiBersama.getJumlahCutiBersama());
			mapdata.put("jumlahCutiYangBisaDiambil", jumlahCutiYangBisaDiambil);

			mapdata.put("jumlah_cuti", jumlahCuti);
			mapdata.put("cuti_bersama", cutiBersama.getJumlahCutiBersama());
			mapdata.put("cuti_bisa_diambil", jumlahCutiYangBisaDiambil);

			calendar = ais.ui.util.WaktuUtil.getCalendar();
			calendar.set(Calendar.YEAR, tahun);
			calendar.set(Calendar.DATE, 1);
			calendar.set(Calendar.MONTH, 0);

			Date m = calendar.getTime();

			calendar = ais.ui.util.WaktuUtil.getCalendar();
			calendar.set(Calendar.YEAR, tahun);
			calendar.set(Calendar.DATE, 31);
			calendar.set(Calendar.MONTH, 11);

			Date s = calendar.getTime();

			List<CutiDanIzin> cutiDanIzinsD = session.createCriteria(CutiDanIzin.class)
					.add(Restrictions.or(Restrictions.between("mulai", m, s), Restrictions.between("sampai", m, s)))
					.add(Restrictions.eq("pegawai", pegawai)).add(Restrictions.eq("setujui", true)).list();
			// session.disconnect();
			ais.action.report.Report.closeNativeSession(session);
			ais.action.report.Report.closeCurrentSessionQuietly();
			LaporanCutiPegawai.generateCutiDanIzinParameter(mapdata, cutiDanIzinsD, tahun, null, cutiBersama,
					jumlahCutiYangBisaDiambil);
			cutiDanIzinsD = null;

		}

		for (CutiDanIzin cutiDanIzin : cutiDanIzins) {
			Map map = new HashMap();
			map.putAll(mapdata);
			JenisCutiDanIzin ja = cutiDanIzin.getJenisCutiDanIzin();
			Session session = HibernateUtil.currentSession();
			session.refresh(ja);

			map.put("id", cutiDanIzin.getId());
			map.put("nama", cutiDanIzin.getNama());
			map.put("mulai", cutiDanIzin.getMulai());
			map.put("sampai", cutiDanIzin.getSampai());
			map.put("keterangan", cutiDanIzin.getKeterangan());

			DisposisiAlurSop.parameterMap(cutiDanIzin.getDisposisiSop(), map);

			int jumlahCuti = Common.getWorkingDaysBetweenTwoDates(cutiDanIzin.getMulai(), cutiDanIzin.getSampai()) + 1;
			map.put("jumlah_cuti", jumlahCuti);

			Common.insertProperty(CutiDanIzin.class, cutiDanIzin, map, "pengajuan");
			Common.insertProperty(Pegawai.class, cutiDanIzin.getPegawai(), map, "pegawai");
			Common.insertProperty(JenisCutiDanIzin.class, ja, map, "jenis");

			for (KelompokParameterTambahanCutiDanIzin kelompokParameterTambahanCutiDanIzin : ja
					.getKelompokParameterTambahanCutiDanIzins()) {
				map.put("kelompok_id", kelompokParameterTambahanCutiDanIzin.getId());
				map.put("kelompok", kelompokParameterTambahanCutiDanIzin.getNama());

				List<ParameterTambahan> parameterTambahans = ConstantValues.simpleList(
						session.createCriteria(ParameterTambahanCutiDanIzin.class)
								.add(Restrictions.eq("kelompokParameterTambahanCutiDanIzin",
										kelompokParameterTambahanCutiDanIzin))
								.createAlias("parameterTambahan", "parameterTambahan")
								.createAlias("kelompokParameterTambahanCutiDanIzin",
										"kelompokParameterTambahanCutiDanIzin")
								.add(Restrictions.eq("parameterTambahan.aktif", true))
								.add(Restrictions.eq("kelompokParameterTambahanCutiDanIzin.aktif", true))
								.setProjection(Projections.groupProperty("parameterTambahan.id")),
						ParameterTambahan.class, false);
				Collections.sort(parameterTambahans);

				for (ParameterTambahan parameterTambahan : parameterTambahans) {
					String jenis = LampiranLain.resolveJenisParameterTambahan(CutiDanIzin.class, cutiDanIzin.getId(),
							kelompokParameterTambahanCutiDanIzin.getId() + "->" + parameterTambahan.getId());
					String jenis_id = kelompokParameterTambahanCutiDanIzin.getId() + "_" + parameterTambahan.getId();

					String val = "";
					String[] spl = cutiDanIzin.getParameterTambahanInds().split("\n");
					for (String d : spl) {
						String[] value = d.split("<=>");
						if (value[0].trim().equalsIgnoreCase(jenis)) {
							val = value.length > 1 ? value[1].trim() : "";
						}
					}

					LampiranLain lampiranLain = LampiranLain.ambil(cutiDanIzin.getId(), jenis);

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
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/akademik/LaporanCutiDanIzin.java:387");
							PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Cuti Dan Izin", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
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

						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/akademik/LaporanCutiDanIzin.java:414");
							PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Cuti Dan Izin", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
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

						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/akademik/LaporanCutiDanIzin.java:435");
							PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Cuti Dan Izin", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
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

			JenisCutiDanIzin j = (JenisCutiDanIzin) (jenisCutiDanIzin.getSelectedItem() == null ? null
					: jenisCutiDanIzin.getSelectedItem().getValue());
			if (j == null) {
				return;
			}

			LampiranLain lainMahaadministrasi = LampiranLain.ambil(j.getId(),
					LampiranLain.FILE_JRXML_LAYOUT_JENIS_CUTI_DAN_IZIN);

			if (lainMahaadministrasi == null) {
				MyMessageboxConfig.show("Mohon maaf, file template laporan pengajuan cuti/izin belum diupload. Langkah yang dapat dilakukan: (1) Buka menu Konfigurasi Laporan dan upload file JRXML untuk jenis cuti/izin ini; (2) Pastikan file template sudah sesuai format yang didukung sistem; (3) Ulangi proses cetak setelah file diupload. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
						MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
				return;
			}

			File file = Report.generateCompileFileReport(Report.PDF,
					generateParameter(j, tanggal.getValue(), sampai.getValue(),
							(Pegawai) pegawai.getAttribute("pegawai"), null),
					lainMahaadministrasi.ambilFile().getAbsolutePath(), ais.ui.util.WaktuUtil.getDate());
			CommonReport.tampilkanReportPDF(center, file);

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Cuti Dan Izin", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini (mis. data akademik/keuangan/pegawai terkait) sudah lengkap dan benar, kemudian coba cetak ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void cetak(final CutiDanIzin cutiDanIzin) throws Exception {

		try {

			final JenisCutiDanIzin j = cutiDanIzin.getJenisCutiDanIzin();
			if (j == null) {
				return;
			}

			LampiranLain lainMahaadministrasi = LampiranLain.ambil(j.getId(),
					LampiranLain.FILE_JRXML_LAYOUT_JENIS_CUTI_DAN_IZIN);

			if (lainMahaadministrasi == null) {
				MyMessageboxConfig.show("Mohon maaf, file template form cuti dan izin belum diupload. Langkah yang dapat dilakukan: (1) Buka menu Konfigurasi Laporan dan upload file template form cuti/izin yang sesuai; (2) Pastikan file template sudah sesuai format yang didukung sistem; (3) Ulangi proses cetak setelah file diupload. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
						MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
				return;
			}

			Map parameters = generateParameter(j, cutiDanIzin.getTanggal_dirubah(), cutiDanIzin.getTanggal_dirubah(),
					cutiDanIzin.getPegawai(), cutiDanIzin);

			File file = Report.generateCompileFileReport(Report.PDF, parameters,
					lainMahaadministrasi.ambilFile().getAbsolutePath(), ais.ui.util.WaktuUtil.getDate());

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
						Map parameters = generateParameter(j, cutiDanIzin.getTanggal_dirubah(),
								cutiDanIzin.getTanggal_dirubah(), cutiDanIzin.getPegawai(), cutiDanIzin);
						return parameters;
					}
				}, LampiranLain.FILE_JRXML_LAYOUT_JENIS_CUTI_DAN_IZIN, null, null));
			}

			window.setVisible(true);
			window.onModal();

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Cuti Dan Izin", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
					new String[] {
						"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
						"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}

	}

}
