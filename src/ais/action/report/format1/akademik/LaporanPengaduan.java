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

import ais.action.master.akunting.helper.AmbilDataPegawaiBanbox;
import ais.action.master.helper.AmbilDataMahasiswaBanbox;
import ais.action.master.sekolah.helper.AmbilDataSiswaBanbox;
import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.JenisPengaduan;
import ais.database.model.KelompokParameterTambahanPengaduan;
import ais.database.model.Mahasiswa;
import ais.database.model.ParameterTambahan;
import ais.database.model.ParameterTambahanPengaduan;
import ais.database.model.Pegawai;
import ais.database.model.Pengaduan;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranLain;
import ais.database.model.sekolah.Siswa;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyWindow;

/**
 * Penyusun/penyaji laporan untuk laporan pengaduan. Kelas ini mengubah data domain menjadi bentuk
 * laporan yang dipakai UI, ekspor, atau proses cetak tanpa memindahkan aturan transaksi ke lapisan
 * report.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Center center}, {@code MyDatebox
 * tanggal}, {@code MyDatebox sampai}, {@code Combobox jenisPengaduan}, {@code AmbilDataPegawaiBanbox pegawai},
 * {@code AmbilDataMahasiswaBanbox mahasiswa}, {@code AmbilDataSiswaBanbox siswa}; inisialisasi/lifecycle ({@code
 * init()}); operasi domain lain ({@code generateParameter()}, {@code onKHS()}). Bagian lain dari kontrak tetap
 * mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class LaporanPengaduan extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = -397946194166101691L;

	private Center center;

	public LaporanPengaduan() {
		super();
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Pengaduan", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanPengaduan(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);
		init();
	}

	private MyDatebox tanggal;

	private MyDatebox sampai;

	private Combobox jenisPengaduan;

	private AmbilDataPegawaiBanbox pegawai;

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

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Pengaduan *"));
		row.appendChild(jenisPengaduan = new Combobox());
		jenisPengaduan.setWidth("90%");
		jenisPengaduan.setReadonly(true);

		Common.insertCombo(jenisPengaduan, new String[] { "nama", "kode" }, "keterangan", JenisPengaduan.class,
				Restrictions.eq("aktif", true));

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR) - 1);

		jenisPengaduan.addEventListener("onChange", eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pegawai (*)"));
		row.appendChild(pegawai = new AmbilDataPegawaiBanbox());
		pegawai.setWidth("90%");

		Tbmuser tbmuser = Common.getCurrentUser();

		Pegawai pegawaiTerpilih = tbmuser == null ? null : tbmuser.ambilPegawai();
		if (pegawaiTerpilih != null) {
			pegawai.setAttribute("myValue", pegawaiTerpilih);
			pegawai.setAttribute("pegawai", pegawaiTerpilih);
			pegawai.setValue(pegawaiTerpilih.getNama());
			pegawai.setDisabled(true);
		}

		row = new MyFormRow();
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
				Pegawai p = (Pegawai) pegawai.getAttribute("pegawai");

				mahasiswa.getParent().setVisible(true);
				siswa.getParent().setVisible(true);
				pegawai.getParent().setVisible(true);

				if (m != null) {
					mahasiswa.getParent().setVisible(true);
					siswa.getParent().setVisible(false);
					pegawai.getParent().setVisible(false);
				} else if (s != null) {
					mahasiswa.getParent().setVisible(false);
					siswa.getParent().setVisible(true);
					pegawai.getParent().setVisible(false);
				} else if (p != null) {
					mahasiswa.getParent().setVisible(false);
					siswa.getParent().setVisible(false);
					pegawai.getParent().setVisible(true);
				}

				onKHS(arg0);
			}
		};

		eventListenerData.onEvent(null);
		pegawai.setEventListener(eventListenerData);
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

				if (jenisPengaduan.getSelectedItem() == null || jenisPengaduan.getSelectedItem().getValue() == null) {
					MyMessageboxConfig.show("Jenis Pengaduan", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return null;
				}

				JenisPengaduan j = (JenisPengaduan) (jenisPengaduan.getSelectedItem() == null ? null
						: jenisPengaduan.getSelectedItem().getValue());

				Map parameters = generateParameter(j, tanggal.getValue(), sampai.getValue(),
						(Pegawai) pegawai.getAttribute("pegawai"), (Mahasiswa) mahasiswa.getAttribute("mahasiswa"),
						(Siswa) siswa.getAttribute("siswa"), null);
				return parameters;
			}
		}, "Pengaduan_Pegawai", null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onKHS(arg0);

			}
		}));

	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static Map generateParameter(JenisPengaduan j, Date tanggal, Date sampai, Pegawai pegawai,
			Mahasiswa mahasiswa, Siswa siswa, Pengaduan pengaduanData) throws Exception {

		if (j == null) {
			return null;
		}

		Map parameters = ais.common.HashMapGenerator.getRandStringSerializable();
		// EKSPOR DINAMIS: jrxml terupload per jenis dipakai utk SEMUA format cetak (PDF/XLS/DOCX/PPTX).
		if (j != null && j.getId() != null) {
			try {
				LampiranLain layoutDinamis = LampiranLain.ambil(j.getId(),
						LampiranLain.FILE_JRXML_LAYOUT_JENIS_PENGADUAN);
				if (layoutDinamis != null && layoutDinamis.ambilFile() != null
						&& layoutDinamis.ambilFile().exists()) {
					parameters.put("nama_laporan", layoutDinamis.ambilFile().getAbsolutePath());
				}
			} catch (Throwable abaikan) { ais.common.ErrorAuditUtil.record(abaikan, "auto-audit(empty-catch) src/ais/action/report/format1/akademik/LaporanPengaduan.java:298");
			}
		}

		parameters.put("tanggal", tanggal);
		parameters.put("sampai", sampai);

		parameters.put("jenisPengaduan", j.getNama());

		List<Map> maps = new ArrayList<Map>();

		List<Pengaduan> pengaduans = new ArrayList<Pengaduan>();

		if (pengaduanData == null) {
			pengaduans = HibernateUtil.currentSession().createCriteria(Pengaduan.class)

					.add(pegawai == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("pegawai", pegawai))
					.add(mahasiswa == null ? Restrictions.sqlRestriction("true")
							: Restrictions.eq("mahasiswa", mahasiswa))
					.add(siswa == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("siswa", siswa))

					.add(Restrictions
							.sqlRestriction("date(waktu) between date('" + Common.databaseDateFormat.get().format(tanggal)
									+ "') and date('" + Common.databaseDateFormat.get().format(sampai) + "')"))
					.add(Restrictions.eq("jenisPengaduan", j)).addOrder(Order.asc("waktu")).list();
		} else {
			pengaduans.add(pengaduanData);
		}

		for (Pengaduan pengaduan : pengaduans) {
			Map map = new HashMap();
			JenisPengaduan ja = pengaduan.getJenisPengaduan();
			Session session = HibernateUtil.currentSession();
			session.refresh(ja);

			map.put("id", pengaduan.getId());
			map.put("nama", pengaduan.getNama());
			map.put("waktu", pengaduan.getWaktu());
			map.put("keterangan", pengaduan.getKeterangan());

			Common.insertProperty(Pengaduan.class, pengaduan, map, "pengajuan");
			Common.insertProperty(Pegawai.class, pengaduan.getPegawai(), map, "pegawai");
			Common.insertProperty(JenisPengaduan.class, ja, map, "jenis");

			for (KelompokParameterTambahanPengaduan kelompokParameterTambahanPengaduan : ja
					.getKelompokParameterTambahanPengaduans()) {
				map.put("kelompok_id", kelompokParameterTambahanPengaduan.getId());
				map.put("kelompok", kelompokParameterTambahanPengaduan.getNama());

				List<ParameterTambahan> parameterTambahans = ConstantValues.simpleList(
						session.createCriteria(ParameterTambahanPengaduan.class)
								.add(Restrictions.eq("kelompokParameterTambahanPengaduan",
										kelompokParameterTambahanPengaduan))
								.createAlias("parameterTambahan", "parameterTambahan")
								.createAlias("kelompokParameterTambahanPengaduan", "kelompokParameterTambahanPengaduan")
								.add(Restrictions.eq("parameterTambahan.aktif", true))
								.add(Restrictions.eq("kelompokParameterTambahanPengaduan.aktif", true))
								.setProjection(Projections.groupProperty("parameterTambahan.id")),
						ParameterTambahan.class, false);
				Collections.sort(parameterTambahans);

				for (ParameterTambahan parameterTambahan : parameterTambahans) {
					String jenis = LampiranLain.resolveJenisParameterTambahan(Pengaduan.class, pengaduan.getId(),
							kelompokParameterTambahanPengaduan.getId() + "->" + parameterTambahan.getId());
					String jenis_id = kelompokParameterTambahanPengaduan.getId() + "_" + parameterTambahan.getId();

					String val = "";
					String[] spl = pengaduan.getParameterTambahanInds().split("\n");
					for (String d : spl) {
						String[] value = d.split("<=>");
						if (value[0].trim().equalsIgnoreCase(jenis)) {
							val = value.length > 1 ? value[1].trim() : "";
						}
					}

					LampiranLain lampiranLain = LampiranLain.ambil(pengaduan.getId(), jenis);

					String vall = val;map.put("param.id." + parameterTambahan.getId(), vall);map.put("param.nama." + parameterTambahan.getNama().toLowerCase(), vall);map.put("param.kode." + parameterTambahan.getKode(), vall);if (parameterTambahan.getTipeDataInputan().equals(ParameterTambahan.ANGKA)||parameterTambahan.getTipeDataInputan().equals(ParameterTambahan.TEXT_ANGKA)) {try {Double	nilai = val.trim().isEmpty() || val.trim().equalsIgnoreCase("null") ? null : Double.parseDouble(val);map.put("param.id." + parameterTambahan.getId(), nilai);map.put("param.nama." + parameterTambahan.getNama().toLowerCase(), nilai);map.put("param.kode." + parameterTambahan.getKode(), nilai);} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/akademik/LaporanPengaduan.java:374");}}

					map.put(jenis_id, vall);
					parameterTambahan.masukkanData(vall, jenis_id, map);

					if (lampiranLain != null) {
						map.put(jenis_id + "_url", lampiranLain.ambilFile().getAbsolutePath());
					}

					if (parameterTambahan.getTipeDataInputan().equals(ParameterTambahan.TANGGAL)) {
						Date nilai = null;
						try {
							nilai = val.trim().isEmpty() ? null : Common.dateFormat1.get().parse(val);map.put("param.id.formated1", Common.dateFormat6.get().format(nilai));map.put("param.id.formated2", Common.dateFormat2.get().format(nilai));map.put("param.id.formated3", Common.dateFormat51.get().format(nilai));map.put("param.id.formated4", Common.timeFormat.get().format(nilai));map.put("param.id.formated5", Common.dateFormat1.get().format(nilai));

							map.put(jenis_id + ".formated1", Common.dateFormat6.get().format(nilai));
							map.put(jenis_id + ".formated2", Common.dateFormat2.get().format(nilai));
							map.put(jenis_id + ".formated3", Common.dateFormat51.get().format(nilai));
							map.put(jenis_id + ".formated4", Common.timeFormat.get().format(nilai));
							map.put(jenis_id + ".formated5", Common.dateFormat1.get().format(nilai));

						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/akademik/LaporanPengaduan.java:394");
							PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Pengaduan", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
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
							nilai = val.trim().isEmpty() ? null : Common.dateFormat.get().parse(val);map.put("param.id.formated1", Common.dateFormat6.get().format(nilai));map.put("param.id.formated2", Common.dateFormat2.get().format(nilai));map.put("param.id.formated3", Common.dateFormat51.get().format(nilai));map.put("param.id.formated4", Common.timeFormat.get().format(nilai));map.put("param.id.formated5", Common.dateFormat1.get().format(nilai));

							map.put(jenis_id + ".formated1", Common.dateFormat6.get().format(nilai));
							map.put(jenis_id + ".formated2", Common.dateFormat2.get().format(nilai));
							map.put(jenis_id + ".formated3", Common.dateFormat51.get().format(nilai));
							map.put(jenis_id + ".formated4", Common.timeFormat.get().format(nilai));
							map.put(jenis_id + ".formated5", Common.dateFormat1.get().format(nilai));

						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/akademik/LaporanPengaduan.java:410");
							PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Pengaduan", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
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

			JenisPengaduan j = (JenisPengaduan) (jenisPengaduan.getSelectedItem() == null ? null
					: jenisPengaduan.getSelectedItem().getValue());
			if (j == null) {
				return;
			}

			LampiranLain lainMahaadministrasi = LampiranLain.ambil(j.getId(),
					LampiranLain.FILE_JRXML_LAYOUT_JENIS_PENGADUAN);

			if (lainMahaadministrasi == null) {
				MyMessageboxConfig.show("File laporan Pengaduan belum diupload", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.INFORMATION);
				return;
			}

			File file = Report.generateCompileFileReport(Report.PDF,
					generateParameter(j, tanggal.getValue(), sampai.getValue(),
							(Pegawai) pegawai.getAttribute("pegawai"), (Mahasiswa) mahasiswa.getAttribute("mahasiswa"),
							(Siswa) siswa.getAttribute("siswa"), null),
					lainMahaadministrasi.ambilFile().getAbsolutePath(), ais.ui.util.WaktuUtil.getDate());
			CommonReport.tampilkanReportPDF(center, file);

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Pengaduan", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini (mis. data akademik/keuangan/pegawai terkait) sudah lengkap dan benar, kemudian coba cetak ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}

	}

}
