package ais.action.report.helper.asset;
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

import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.ParameterTambahan;
import ais.database.model.asset.JenisPerbaikanAsset;
import ais.database.model.asset.KelompokParameterTambahanPerbaikanAsset;
import ais.database.model.asset.ParameterTambahanPerbaikanAsset;
import ais.database.model.asset.PerbaikanAsset;
import ais.database.model.file.LampiranLain;
import ais.database.model.sop.DisposisiAlurSop;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyWindow;

/**
 * Penyusun/penyaji laporan untuk laporan perbaikan asset. Kelas ini mengubah data domain menjadi
 * bentuk laporan yang dipakai UI, ekspor, atau proses cetak tanpa memindahkan aturan transaksi ke
 * lapisan report.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Center center}, {@code MyDatebox
 * tanggal}, {@code MyDatebox sampai}, {@code Combobox jenisPerbaikanAsset}; inisialisasi/lifecycle ({@code
 * init()}, {@code initData()}); pelaporan/ekspor ({@code cetak()}); operasi domain lain ({@code
 * generateParameter()}, {@code onKHS()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface
 * yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class LaporanPerbaikanAsset extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = -397946194166101691L;

	private Center center;

	public LaporanPerbaikanAsset() {
		super();
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Perbaikan Asset", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanPerbaikanAsset(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);
		init();
	}

	private MyDatebox tanggal;

	private MyDatebox sampai;

	private Combobox jenisPerbaikanAsset;

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
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Perbaikan *"));
		row.appendChild(jenisPerbaikanAsset = new Combobox());
		jenisPerbaikanAsset.setWidth("90%");
		jenisPerbaikanAsset.setReadonly(true);

		Common.insertCombo(jenisPerbaikanAsset, new String[] { "nama", "kode" }, "keterangan",
				JenisPerbaikanAsset.class, Restrictions.eq("aktif", true));

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR) - 1);

		jenisPerbaikanAsset.addEventListener("onChange", eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Mulai"));
		tanggal = new MyDatebox(calendar.getTime());
		row.appendChild(tanggal);
		tanggal.setWidth("90%");
		tanggal.addEventListener("onChange", eventListener);
		tanggal.setReadonly(true);

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

				if (jenisPerbaikanAsset.getSelectedItem() == null
						|| jenisPerbaikanAsset.getSelectedItem().getValue() == null) {
					MyMessageboxConfig.show("Jenis Perbaikan Administrasi", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return null;
				}

				JenisPerbaikanAsset j = (JenisPerbaikanAsset) jenisPerbaikanAsset.getSelectedItem().getValue();

				Map parameters = generateParameter(tanggal.getValue(), sampai.getValue(), null, j);
				return parameters;
			}
		}, "Perbaikan_Administrasi", null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onKHS(arg0);

			}
		}));

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static Map initData(PerbaikanAsset perbaikanAsset) {
		Map map = new HashMap();
		JenisPerbaikanAsset ja = perbaikanAsset.getJenisPerbaikanAsset();
		Session session = HibernateUtil.currentSession();
		session.refresh(ja);

		Common.insertProperty(PerbaikanAsset.class, perbaikanAsset, map, "perbaikanAsset");

		DisposisiAlurSop.parameterMap(perbaikanAsset.getDisposisiSop(), map);

		map.put("id", perbaikanAsset.getId());
		map.put("nama", perbaikanAsset.getNama());
		map.put("waktu", perbaikanAsset.getWaktu());
		map.put("keterangan", perbaikanAsset.getKeterangan());

		for (KelompokParameterTambahanPerbaikanAsset kelompokParameterTambahanPerbaikanAsset : ja
				.getKelompokParameterTambahanPerbaikanAssets()) {
			map.put("kelompok_id", kelompokParameterTambahanPerbaikanAsset.getId());
			map.put("kelompok", kelompokParameterTambahanPerbaikanAsset.getNama());

			List<ParameterTambahan> parameterTambahans = ConstantValues
					.simpleList(
							session.createCriteria(ParameterTambahanPerbaikanAsset.class)
									.add(Restrictions.eq("kelompokParameterTambahanPerbaikanAsset",
											kelompokParameterTambahanPerbaikanAsset))
									.createAlias("parameterTambahan", "parameterTambahan")
									.createAlias("kelompokParameterTambahanPerbaikanAsset",
											"kelompokParameterTambahanPerbaikanAsset")
									.add(Restrictions.eq("parameterTambahan.aktif", true))
									.add(Restrictions.eq("kelompokParameterTambahanPerbaikanAsset.aktif", true))
									.setProjection(Projections.groupProperty("parameterTambahan.id")),
							ParameterTambahan.class, false);
			Collections.sort(parameterTambahans);

			for (ParameterTambahan parameterTambahan : parameterTambahans) {
				String jenis = kelompokParameterTambahanPerbaikanAsset.getId() + "->" + parameterTambahan.getId();
				String jenis_id = kelompokParameterTambahanPerbaikanAsset.getId() + "_" + parameterTambahan.getId();

				String val = "";
				String[] spl = perbaikanAsset.getParameterTambahanInds().split("\n");
				for (String d : spl) {
					String[] value = d.split("<=>");
					if (value[0].trim().equalsIgnoreCase(jenis)) {
						val = value.length > 1 ? value[1].trim() : "";
					}
				}

				LampiranLain lampiranLain = LampiranLain.ambil(perbaikanAsset.getId(), jenis);

				String vall = val;map.put("param.id." + parameterTambahan.getId(), vall);map.put("param.nama." + parameterTambahan.getNama().toLowerCase(), vall);map.put("param.kode." + parameterTambahan.getKode(), vall);if (parameterTambahan.getTipeDataInputan().equals(ParameterTambahan.ANGKA)||parameterTambahan.getTipeDataInputan().equals(ParameterTambahan.TEXT_ANGKA)) {try {Double	nilai = val.trim().isEmpty() || val.trim().equalsIgnoreCase("null") ? null : Double.parseDouble(val);map.put("param.id." + parameterTambahan.getId(), nilai);map.put("param.nama." + parameterTambahan.getNama().toLowerCase(), nilai);map.put("param.kode." + parameterTambahan.getKode(), nilai);} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/helper/asset/LaporanPerbaikanAsset.java:236");}}

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
	public static Map generateParameter(Date tanggal, Date sampai, PerbaikanAsset perbaikanAsseta,
			JenisPerbaikanAsset j) throws Exception {

		Map parameters = ais.common.HashMapGenerator.getRandStringSerializable();

		parameters.put("tanggal", tanggal);
		parameters.put("sampai", sampai);

		parameters.put("jenisPerbaikanAsset", j.getNama());

		System.out.println("parameters => " + parameters);

		List<Map> maps = new ArrayList<Map>();

		if (perbaikanAsseta != null) {
			maps.add(initData(perbaikanAsseta));
		} else {

			List<PerbaikanAsset> perbaikanAssets = HibernateUtil.currentSession().createCriteria(PerbaikanAsset.class)
					.add(Restrictions
							.sqlRestriction("date(waktu) between date('" + Common.databaseDateFormat.get().format(tanggal)
									+ "') and date('" + Common.databaseDateFormat.get().format(sampai) + "')"))
					.add(Restrictions.eq("jenisPerbaikanAsset", j)).addOrder(Order.asc("waktu")).list();

			for (PerbaikanAsset perbaikanAsset : perbaikanAssets) {

				maps.add(initData(perbaikanAsset));
			}
		}

		parameters.put("maps", maps);

		return parameters;
	}

	@SuppressWarnings({ "unchecked" })
	public void onKHS(Event event) throws Exception {

		try {

			JenisPerbaikanAsset j = (JenisPerbaikanAsset) jenisPerbaikanAsset.getSelectedItem().getValue();
			if (j == null) {
				return;
			}

			LampiranLain lainMahaadministrasi = LampiranLain.ambil(j.getId(),
					LampiranLain.FILE_JRXML_LAYOUT_JENIS_PERBAIKAN);

			if (lainMahaadministrasi == null) {
				MyMessageboxConfig.show("File laporan perbaikan sarpras belum diupload", "Peringatan",
						MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
				return;
			}

			File file = Report.generateCompileFileReport(Report.PDF,
					generateParameter(tanggal.getValue(), sampai.getValue(), null, j),
					lainMahaadministrasi.ambilFile().getAbsolutePath(), ais.ui.util.WaktuUtil.getDate());
			CommonReport.tampilkanReportPDF(center, file);

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Perbaikan Asset", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini (mis. data akademik/keuangan/pegawai terkait) sudah lengkap dan benar, kemudian coba cetak ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void cetak(final PerbaikanAsset perbaikanAsset) throws Exception {

		try {

			JenisPerbaikanAsset j = perbaikanAsset.getJenisPerbaikanAsset();
			if (j == null) {
				return;
			}

			LampiranLain lainMahaadministrasi = LampiranLain.ambil(j.getId(),
					LampiranLain.FILE_JRXML_LAYOUT_JENIS_FORM_PERBAIKAN);

			if (lainMahaadministrasi == null) {
				MyMessageboxConfig.show("File template form perbaikan sarpras belum diupload", "Peringatan",
						MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
				return;
			}

			Map parameters = generateParameter(perbaikanAsset.getTanggal_dirubah(), perbaikanAsset.getTanggal_dirubah(),
					perbaikanAsset, perbaikanAsset.getJenisPerbaikanAsset());

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
						Map parameters = generateParameter(perbaikanAsset.getTanggal_dirubah(),
								perbaikanAsset.getTanggal_dirubah(), perbaikanAsset,
								perbaikanAsset.getJenisPerbaikanAsset());
						return parameters;
					}
				}, LampiranLain.FILE_JRXML_LAYOUT_JENIS_FORM_PERBAIKAN, null, null));
			}

			window.setVisible(true);
			window.onModal();

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Perbaikan Asset", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
					new String[] {
						"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
						"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}

	}
}
