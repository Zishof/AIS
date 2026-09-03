package ais.action.report.format1.sekolah;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.pdfbox.util.PDFMergerUtility;
import org.hibernate.Criteria;
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
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.West;

import ais.action.master.sekolah.helper.AmbilDataKelasSiswaBanbox;
import ais.action.master.sekolah.helper.AmbilDataSiswaBanbox;
import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.Konfigurasi;
import ais.database.model.ParameterTambahan;
import ais.database.model.Perkuliahan;
import ais.database.model.Statusabsensi;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranLain;
import ais.database.model.sekolah.AbsenPiket;
import ais.database.model.sekolah.AbsenPiketDetail;
import ais.database.model.sekolah.CatatanSiswa;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.JenisCatatanKelasSiswa;
import ais.database.model.sekolah.JenisCatatanSiswa;
import ais.database.model.sekolah.KelasSiswa;
import ais.database.model.sekolah.KelasSiswaPunyaSiswa;
import ais.database.model.sekolah.KelompokParameterTambahanCatatanSiswa;
import ais.database.model.sekolah.ParameterTambahanCatatanSiswa;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Siswa;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Penyusun/penyaji laporan untuk laporan catatan siswa. Kelas ini mengubah data domain menjadi
 * bentuk laporan yang dipakai UI, ekspor, atau proses cetak tanpa memindahkan aturan transaksi ke
 * lapisan report.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code AmbilDataSiswaBanbox bandboxSiswa},
 * {@code Center center}, {@code MyDatebox tanggal}, {@code MyDatebox sampai}, {@code Combobox
 * jenisCatatanSiswa}, {@code Combobox tahunAkademik}, {@code Combobox searchsmt}, {@code
 * AmbilDataKelasSiswaBanbox bandboxKelas}; inisialisasi/lifecycle ({@code init()}, {@code initData()});
 * pelaporan/ekspor ({@code cetak()}); operasi domain lain ({@code generateParameter()}, {@code onKHS()}). Bagian
 * lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class LaporanCatatanSiswa extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = -397946194166101691L;

	private AmbilDataSiswaBanbox bandboxSiswa;
	private Center center;

	public LaporanCatatanSiswa() {
		super();
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Catatan Siswa", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanCatatanSiswa(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);
		init();
	}

	private MyDatebox tanggal;

	private MyDatebox sampai;

	private Combobox jenisCatatanSiswa;

	private Combobox tahunAkademik;

	private Combobox searchsmt;

	private AmbilDataKelasSiswaBanbox bandboxKelas;

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
		row.appendChild(new ais.ui.util.MyLabelConfig(Common.getBahasa("label_siswa")));
		row.appendChild(bandboxSiswa = new AmbilDataSiswaBanbox());
		bandboxSiswa.setWidth("90%");

		if (Common.getCurrentUser() != null && Common.getCurrentUser().getSiswa() != null) {
			Siswa siswa = Common.getCurrentUser().getSiswa();
			bandboxSiswa.setAttribute("siswa", siswa);
			bandboxSiswa.setAttribute("myValue", siswa);
			bandboxSiswa.setValue(siswa.getNim() + " - " + siswa.getNama());
			bandboxSiswa.setId("mhs_" + siswa.getId());
			bandboxSiswa.setDisabled(true);
		}

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kelas"));
		row.appendChild(bandboxKelas = new AmbilDataKelasSiswaBanbox());
		bandboxKelas.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Catatan *"));
		row.appendChild(jenisCatatanSiswa = new Combobox());
		jenisCatatanSiswa.setWidth("90%");
		jenisCatatanSiswa.setReadonly(true);

		EventListener eventListenerJenis = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				bandboxKelas.getParent().setVisible(true);
				bandboxSiswa.getParent().setVisible(true);

				Siswa siswa = (Siswa) bandboxSiswa.getAttribute("siswa");
				KelasSiswa kelasSiswa = (KelasSiswa) bandboxKelas.getAttribute("kelasSiswa");

				if (siswa != null) {
					bandboxKelas.getParent().setVisible(false);
					Common.insertCombo(jenisCatatanSiswa, new String[] { "nama", "kode" }, "keterangan",
							JenisCatatanSiswa.class, Restrictions.and(Restrictions.eq("sekolah", siswa.getSekolah()),
									Restrictions.eq("aktif", true)));
				} else if (kelasSiswa != null) {
					bandboxSiswa.getParent().setVisible(false);
					Common.insertCombo(jenisCatatanSiswa, new String[] { "nama", "kode" }, "keterangan",
							JenisCatatanSiswa.class,
							Restrictions.and(Restrictions.eq("sekolah", kelasSiswa.getSekolah()),
									Restrictions.eq("aktif", true)));
				}

			}

		};
		jenisCatatanSiswa.addEventListener("onChange", eventListener);
		bandboxSiswa.setEventListener(eventListenerJenis);
		bandboxKelas.setEventListener(eventListenerJenis);

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

				if (bandboxSiswa.getAttribute("siswa") == null) {
					MyMessageboxConfig.show("Pilih Siswa", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return null;
				}

				if (jenisCatatanSiswa.getSelectedItem() == null
						|| jenisCatatanSiswa.getSelectedItem().getValue() == null) {
					MyMessageboxConfig.show("Jenis Catatan Siswa", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return null;
				}

				if (jenisCatatanSiswa.getSelectedItem() == null
						|| jenisCatatanSiswa.getSelectedItem().getValue() == null) {
					return null;
				}

				JenisCatatanSiswa j = (JenisCatatanSiswa) jenisCatatanSiswa.getSelectedItem().getValue();

				Map parameters = generateParameter((Siswa) bandboxSiswa.getAttribute("siswa"), tanggal.getValue(),
						sampai.getValue(),
						tahunAkademik.getSelectedItem() == null ? null
								: tahunAkademik.getSelectedItem().getValue().toString(),
						(Integer) (searchsmt.getSelectedItem() == null ? null : searchsmt.getSelectedItem().getValue()),
						null, j);

				LampiranLain lainMahasiswa = LampiranLain.ambil(j.getId(),
						LampiranLain.FILE_JRXML_LAYOUT_JENIS_FORM_CATATAN_SISWA);

				if (lainMahasiswa != null) {
					parameters.put("nama_laporan", lainMahasiswa.ambilFile().getAbsolutePath());
				}

				return parameters;
			}
		}, "Catatan_Siswa", null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onKHS(arg0);

			}
		}, false));

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static Map initData(CatatanSiswa catatanSiswa) {
		Map map = new HashMap();
		JenisCatatanSiswa ja = catatanSiswa.getJenisCatatanSiswa();
		Session session = HibernateUtil.currentSession();
		session.refresh(ja);

		if (catatanSiswa.getKelasSiswa() != null) {
			Common.insertProperty(KelasSiswa.class, catatanSiswa.getKelasSiswa(), map, "kelas", 2, "sekolah",
					"yayasan");
		}

		map.put("id", catatanSiswa.getId());
		map.put("nama", catatanSiswa.getNama());
		map.put("waktu", catatanSiswa.getWaktu());
		map.put("tahunAjaran", catatanSiswa.getTahunAjaran());
		map.put("semester", catatanSiswa.getSemester());

		if (catatanSiswa.getWaktu() != null) {
			map.put("waktu.formated1", Common.dateFormat6.get().format(catatanSiswa.getWaktu()));
			map.put("waktu.formated2", Common.dateFormat2.get().format(catatanSiswa.getWaktu()));
			map.put("waktu.formated3", Common.dateFormat51.get().format(catatanSiswa.getWaktu()));
			map.put("waktu.formated4", Common.timeFormat.get().format(catatanSiswa.getWaktu()));
			map.put("waktu.formated5", Common.dateFormat1.get().format(catatanSiswa.getWaktu()));
		}

		map.put("keterangan", catatanSiswa.getKeterangan());

		Guru guru = catatanSiswa.getGuru();
		if (guru != null) {
			Common.insertProperty(Guru.class, guru, map, "guru", 2, "sekolah", "yayasan");
		}

		for (KelompokParameterTambahanCatatanSiswa kelompokParameterTambahanCatatanSiswa : ja
				.getKelompokParameterTambahanCatatanSiswas()) {
			map.put("kelompok_id", kelompokParameterTambahanCatatanSiswa.getId());
			map.put("kelompok", kelompokParameterTambahanCatatanSiswa.getNama());

			List<ParameterTambahan> parameterTambahans = ConstantValues
					.simpleList(
							session.createCriteria(ParameterTambahanCatatanSiswa.class)
									.add(Restrictions.eq("kelompokParameterTambahanCatatanSiswa",
											kelompokParameterTambahanCatatanSiswa))
									.createAlias("parameterTambahan", "parameterTambahan")
									.createAlias("kelompokParameterTambahanCatatanSiswa",
											"kelompokParameterTambahanCatatanSiswa")
									.add(Restrictions.eq("parameterTambahan.aktif", true))
									.add(Restrictions.eq("kelompokParameterTambahanCatatanSiswa.aktif", true))
									.setProjection(Projections.groupProperty("parameterTambahan.id")),
							ParameterTambahan.class, false);
			Collections.sort(parameterTambahans);

			for (ParameterTambahan parameterTambahan : parameterTambahans) {
				String jenis = LampiranLain.resolveJenisParameterTambahan(CatatanSiswa.class, catatanSiswa.getId(),
						kelompokParameterTambahanCatatanSiswa.getId() + "->" + parameterTambahan.getId());
				String jenis_id = kelompokParameterTambahanCatatanSiswa.getId() + "_" + parameterTambahan.getId();

				String val = "";
				String ket = "";
				String[] spl = catatanSiswa.getParameterTambahanInds().split("\n");
				for (String d : spl) {
					String[] value = d.split("<=>");
					if (value[0].trim().equalsIgnoreCase(jenis)) {
						val = value.length > 1 ? value[1].trim() : "";

						try {
							ket = value.length > 0 ? value[value.length - 1] : "";
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/sekolah/LaporanCatatanSiswa.java:405");
							PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Catatan Siswa", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
								new String[] {
									"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
									"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
									"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
								});

						}
					}
				}

				LampiranLain lampiranLain = LampiranLain.ambil(catatanSiswa.getId(), jenis);

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
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/sekolah/LaporanCatatanSiswa.java:426");
						PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Catatan Siswa", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
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

					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/sekolah/LaporanCatatanSiswa.java:444");
						PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Catatan Siswa", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
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
										map.put(dd.replaceAll("[^\\sa-zA-Z0-9]", "").replaceAll(" ", ""),
												jsonObject.get(key));

									} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/sekolah/LaporanCatatanSiswa.java:474");
										PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Catatan Siswa", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
											new String[] {
												"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
												"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
												"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
											});

									}
								}

							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/sekolah/LaporanCatatanSiswa.java:479");
								PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Catatan Siswa", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
									new String[] {
										"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
										"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
										"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
									});

							}
						}

					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/sekolah/LaporanCatatanSiswa.java:484");
						PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Catatan Siswa", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
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

					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/sekolah/LaporanCatatanSiswa.java:506");
						PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Catatan Siswa", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
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

					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/sekolah/LaporanCatatanSiswa.java:527");
						PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Catatan Siswa", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
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

		if (catatanSiswa.getSiswa() != null && catatanSiswa.getKelasSiswa() != null) {
			JenisCatatanKelasSiswa jck = (JenisCatatanKelasSiswa) ConstantValues
					.simpleObject(session.createCriteria(JenisCatatanKelasSiswa.class)
							.add(Restrictions.ilike("nama", ja.getNama(), MatchMode.EXACT))
							.add(Restrictions.eq("sekolah", ja.getSekolah()))
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
							.setMaxResults(1), JenisCatatanKelasSiswa.class);
			if (jck != null) {
				try {
					Map m = LaporanCatatanKelasSiswa.generateParameter(catatanSiswa.getKelasSiswa(), null, null, null,
							jck, catatanSiswa.getTahunAjaran(), catatanSiswa.getSemester());
					if (m != null) {
						List<Map> maps = (List<Map>) m.get("maps");

						System.out.println("maps cataan siswa dari catatan kelas -> " + maps);

						if (!maps.isEmpty()) {
							map.putAll(maps.get(0));
						}
						m.clear();
						m = null;
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/sekolah/LaporanCatatanSiswa.java:565");
					PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Catatan Siswa", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
						new String[] {
							"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
							"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
							"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
						});
				}
			}
		}

		if (catatanSiswa.getGuru() != null) {
			Common.insertProperty(Guru.class, catatanSiswa.getGuru(), map, "guru", 2, "sekolah", "yayasan");
		}

		return map;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static Map generateParameter(Siswa siswa, Date tanggal, Date sampai, String ta, Integer smt,
			CatatanSiswa catatanSiswaa, JenisCatatanSiswa j) throws Exception {

		if (siswa == null) {
			return null;
		}

		Map parameters = ais.common.HashMapGenerator.getRandStringSerializable();

		if (j.getId() != null) {
			try {
				Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();
				List<LampiranLain> lampiranLains = streamingSession.createCriteria(LampiranLain.class)
						.addOrder(Order.asc("id")).add(Restrictions.eq("ref", j.getId()))
						.add(Restrictions.ilike("jenis", "Catatan_Siswa_", MatchMode.START)).list();
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
				e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/report/format1/sekolah/LaporanCatatanSiswa.java:605");
			}
		}

		siswa.putPhoto(parameters);

		parameters.put("sekolah", (siswa.getSekolah() == null ? "" : siswa.getSekolah().getNama()));
		parameters.put("nama", (siswa.getNama()));
		parameters.put("siswa_id", siswa.getId());
		parameters.put("kelas", siswa.getKelas() == null ? "" : siswa.getKelas().getNama());
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

		Common.insertProperty(Siswa.class, siswa, parameters, "siswa");

		if (siswa.getKelas() != null && siswa.getKelas().getGuruPembina() != null) {
			Common.insertProperty(Guru.class, siswa.getKelas().getGuruPembina(), parameters, "guru_wali");
			parameters.put("url_foto_guru_wali",
					CommonMedia.getUrlFotoPengguna(new Tbmuser(siswa.getKelas().getGuruPembina())));
		}

		if (siswa.getGuruBk() != null) {
			Common.insertProperty(Guru.class, siswa.getGuruBk(), parameters, "guru_bk");
			parameters.put("url_foto_guru_bk", CommonMedia.getUrlFotoPengguna(new Tbmuser(siswa.getGuruBk())));
		}

		if (siswa.getSekolah() != null) {
			Common.insertProperty(Sekolah.class, siswa.getSekolah(), parameters, "sekolah");
		}

		if (siswa.getGuruPembina() != null) {
			Common.insertProperty(Guru.class, siswa.getGuruPembina(), parameters, "guru_pembina");
			parameters.put("url_foto_guru_pembina",
					CommonMedia.getUrlFotoPengguna(new Tbmuser(siswa.getGuruPembina())));
		}

		parameters.put("jenisCatatanSiswa", j.getNama());

		List<Map> maps = new ArrayList<Map>();

		if (catatanSiswaa != null) {
			maps.add(initData(catatanSiswaa));
		} else {
			List<CatatanSiswa> catatanSiswas = HibernateUtil.currentSession().createCriteria(CatatanSiswa.class)

					.add(ta == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("tahunAjaran", ta))
					.add(smt == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("semester", smt))

					.add(Restrictions
							.sqlRestriction("date(waktu) between date('" + Common.databaseDateFormat.get().format(tanggal)
									+ "') and date('" + Common.databaseDateFormat.get().format(sampai) + "')"))
					.add(Restrictions.eq("siswa", siswa)).add(Restrictions.eq("jenisCatatanSiswa", j))
					.addOrder(Order.asc("waktu")).list();

			for (CatatanSiswa catatanSiswa : catatanSiswas) {
				maps.add(initData(catatanSiswa));
			}
		}

		parameters.put("maps", maps);

		if (siswa != null) {
			Session session = HibernateUtil.currentSession();
			List<Object[]> kelasSiswas = session.createCriteria(KelasSiswaPunyaSiswa.class)
					.add(Restrictions.eq("siswa", siswa))
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.createAlias("kelasSiswa", "kelasSiswa", Criteria.LEFT_JOIN)
					.createAlias("kelasSiswa.guruPembina", "guruPembina", Criteria.LEFT_JOIN)
					.createAlias("kelasSiswa.guruBk", "guruBk", Criteria.LEFT_JOIN)
					.add(Restrictions.eq("kelasSiswa.tahunAjaran", ta))

					.setProjection(Projections.projectionList().add(Projections.property("kelasSiswa.nama"))

							.add(Projections.property("guruPembina.id")).add(Projections.property("guruBk.id"))
							.add(Projections.property("kelasSiswa.tahunAjaran"))
							.add(Projections.property("kelasSiswa.id"))

					)

					.add(Restrictions.eq("kelasSiswa.aktif", true)).list();
			List<Long> kelasid = new ArrayList<Long>();
			for (Object[] kelasSiswa : kelasSiswas) {

				Long guruPembinaId = kelasSiswa[1] == null ? null : ((Number) kelasSiswa[1]).longValue();
				Long guruBkId = kelasSiswa[2] == null ? null : ((Number) kelasSiswa[2]).longValue();

				Guru guruPembina = guruPembinaId == null ? null
						: (Guru) ConstantValues.ambil(Guru.class.getName(), guruPembinaId);
				Guru guruBk = guruBkId == null ? null : (Guru) ConstantValues.ambil(Guru.class.getName(), guruBkId);

				parameters.put("kelassiswa.nama", kelasSiswa[0] + "");
				parameters.put("kelassiswa.ta", kelasSiswa[3] + "");
				parameters.put("kelassiswa.guruBk", guruBk == null ? "" : guruBk.getNama());
				parameters.put("kelassiswa.guruPembina", guruPembina == null ? "" : guruPembina.getNama());
				kelasid.add(((Number) kelasSiswa[4]).longValue());

				if (guruPembina != null) {
					Common.insertProperty(Guru.class, guruPembina, parameters, "guruPembina");
				}
				if (guruBk != null) {
					Common.insertProperty(Guru.class, guruBk, parameters, "guruBk");
				}
			}

			List<AbsenPiket> absenPikets = kelasid.isEmpty() ? null
					: session.createCriteria(AbsenPiket.class)

							.add(Restrictions.in("kelas.id", kelasid))

							.add(siswa.getSekolah() == null ? Restrictions.sqlRestriction("1=1")
									: Restrictions.eq("sekolah", siswa.getSekolah()))

							.add(Restrictions.eq("tahunAjaran", ta)).add(Restrictions.eq("semester", smt))
							.addOrder(Order.asc("tanggal")).addOrder(Order.asc("id")).list();

			int hadir = 0;
			int sakit = 0;
			int izin = 0;
			int alpa = 0;
			int belum = 0;
			if (absenPikets != null) {

				Integer jamke = 0;
				for (AbsenPiket absenPiket : absenPikets) {

					AbsenPiketDetail absenPiketDetail = AbsenPiketDetail.ambil(null, siswa, absenPiket,
							absenPiket.getKelas().getAbsensi(), jamke);

					Statusabsensi statusabsensi = (Statusabsensi) ConstantValues.ambil(Statusabsensi.class.getName(),
							absenPiketDetail.retreiveAbsensiId(siswa.getId() + "_" + absenPiket.getId()));
					if (statusabsensi == null) {
						statusabsensi = ConstantValues.BELUM_ABSEN;
					}

					Integer h = ConstantValues.MASUK != null
							&& statusabsensi.getId().equals(ConstantValues.MASUK.getId()) ? 1 : 0;
					Integer ss = ConstantValues.SAKIT != null
							&& statusabsensi.getId().equals(ConstantValues.SAKIT.getId()) ? 1 : 0;
					Integer ii = ConstantValues.IZIN != null
							&& statusabsensi.getId().equals(ConstantValues.IZIN.getId()) ? 1 : 0;
					Integer a = ConstantValues.TIDAK_ADA_ALASAN != null
							&& statusabsensi.getId().equals(ConstantValues.TIDAK_ADA_ALASAN.getId()) ? 1 : 0;
					Integer b = ConstantValues.BELUM_ABSEN != null
							&& statusabsensi.getId().equals(ConstantValues.BELUM_ABSEN.getId()) ? 1 : 0;

					belum += b;
					hadir += h;
					sakit += ss;
					izin += ii;
					alpa += a;

				}
			}
			parameters.put("hadir", hadir);
			parameters.put("sakit", sakit);
			parameters.put("izin", izin);
			parameters.put("alpa", alpa);
			parameters.put("belum", belum);
		}

		return parameters;
	}

	@SuppressWarnings({ "unchecked" })
	public void onKHS(Event event) throws Exception {

		try {

			final JenisCatatanSiswa j = (JenisCatatanSiswa) (jenisCatatanSiswa.getSelectedItem() == null ? null
					: jenisCatatanSiswa.getSelectedItem().getValue());
			if (j == null) {
				return;
			}

			final LampiranLain lainMahasiswa = LampiranLain.ambil(j.getId(),
					LampiranLain.FILE_JRXML_LAYOUT_JENIS_CATATAN_SISWA);

			if (lainMahasiswa == null) {
				MyMessageboxConfig.show("File laporan catatan siswa belum diupload", "Peringatan",
						MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
				return;
			}

			Siswa siswa = (Siswa) bandboxSiswa.getAttribute("siswa");
			KelasSiswa kelasSiswa = (KelasSiswa) bandboxKelas.getAttribute("kelasSiswa");

			if (siswa != null) {
				File file = Report.generateCompileFileReport(Report.PDF, generateParameter(siswa, tanggal.getValue(),
						sampai.getValue(),
						tahunAkademik.getSelectedItem() == null ? null
								: tahunAkademik.getSelectedItem().getValue().toString(),
						(Integer) (searchsmt.getSelectedItem() == null ? null : searchsmt.getSelectedItem().getValue()),
						null, j), lainMahasiswa.ambilFile().getAbsolutePath(), ais.ui.util.WaktuUtil.getDate());
				CommonReport.tampilkanReportPDF(center, file);
			} else if (kelasSiswa != null) {

				final List<KelasSiswaPunyaSiswa> kelasSiswaPunyaSiswas = ConstantValues.simpleList(HibernateUtil
						.currentSession().createCriteria(KelasSiswaPunyaSiswa.class)
						.add(Restrictions.eq("kelasSiswa", kelasSiswa)).createAlias("siswa", "siswa")
						.addOrder(Common.bolehKonfigurasi("absensi_urut_berdasarkan_nim") ? Order.asc("siswa.nim") : Order.asc("siswa.nama")),
						KelasSiswaPunyaSiswa.class);

				final PDFMergerUtility ut = new PDFMergerUtility();
				final File filePdfBaru = new File(
						Common.ambilREAL_PATH_REPORT() + "/" + Common.getGeneratedBarCode() + ".pdf");
				final Label label = Common.displayLoadBar(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						ut.setDestinationStream(new FileOutputStream(filePdfBaru));
						ut.mergeDocuments();
						CommonReport.tampilkanReportPDF(center, filePdfBaru);
					}
				});

				new Thread(new Runnable() {

					@Override
					public void run() {
						int index = 0;
						int size = kelasSiswaPunyaSiswas.size();
						for (KelasSiswaPunyaSiswa kelasSiswaPunyaSiswa : kelasSiswaPunyaSiswas) {
							index++;
							label.setValue("Memperoses data " + kelasSiswaPunyaSiswa.getSiswa().getNama() + " ("
									+ Common.numberFormat.get().format(((index * 1.0) / (size * 1.0)) * 100.0) + "%)");
							try {
								File file = Report.generateCompileFileReport(Report.PDF,
										generateParameter(kelasSiswaPunyaSiswa.getSiswa(), tanggal.getValue(),
												sampai.getValue(),
												tahunAkademik.getSelectedItem() == null ? null
														: tahunAkademik.getSelectedItem().getValue().toString(),
												(Integer) (searchsmt.getSelectedItem() == null ? null
														: searchsmt.getSelectedItem().getValue()),
												null, j),
										lainMahasiswa.ambilFile().getAbsolutePath(), ais.ui.util.WaktuUtil.getDate());
								ut.addSource(file);
							} catch (Exception e) {
								e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/sekolah/LaporanCatatanSiswa.java:856");
								PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Catatan Siswa", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
									new String[] {
										"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
										"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
										"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
									});
							}

						}
						kelasSiswaPunyaSiswas.clear();
						ais.action.report.helper.LoadingReportUtil.selesai(label);
					}
				}).start();

			}

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Catatan Siswa", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
					new String[] {
						"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
						"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void cetak(final CatatanSiswa catatanSiswa) throws Exception {

		try {

			JenisCatatanSiswa j = catatanSiswa.getJenisCatatanSiswa();
			if (j == null) {

				return;
			}

			LampiranLain lainMahasiswa = LampiranLain.ambil(j.getId(),
					LampiranLain.FILE_JRXML_LAYOUT_JENIS_FORM_CATATAN_SISWA);

			if (lainMahasiswa == null) {
//				MyMessageboxConfig.show("File template form catatan siswa belum diupload", "Peringatan",
//						MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
				return;
			}

			Map parameters = generateParameter(catatanSiswa.getSiswa(), catatanSiswa.getTanggal_dirubah(),
					catatanSiswa.getTanggal_dirubah(), catatanSiswa.getTahunAjaran(), catatanSiswa.getSemester(),
					catatanSiswa, catatanSiswa.getJenisCatatanSiswa());

			File file = Report.generateCompileFileReport(Report.PDF, parameters,
					lainMahasiswa.ambilFile().getAbsolutePath(), ais.ui.util.WaktuUtil.getDate());

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
						Map parameters = generateParameter(catatanSiswa.getSiswa(), catatanSiswa.getTanggal_dirubah(),
								catatanSiswa.getTanggal_dirubah(), catatanSiswa.getTahunAjaran(),
								catatanSiswa.getSemester(), catatanSiswa, catatanSiswa.getJenisCatatanSiswa());

						JenisCatatanSiswa j = catatanSiswa.getJenisCatatanSiswa();
						LampiranLain lainMahasiswa = LampiranLain.ambil(j.getId(),
								LampiranLain.FILE_JRXML_LAYOUT_JENIS_FORM_CATATAN_SISWA);

						if (lainMahasiswa != null) {
							parameters.put("nama_laporan", lainMahasiswa.ambilFile().getAbsolutePath());
						}

						return parameters;
					}
				}, LampiranLain.FILE_JRXML_LAYOUT_JENIS_FORM_CATATAN_SISWA, null, null));
			}

			window.setVisible(true);
			window.onModal();

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Catatan Siswa", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
					new String[] {
						"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
						"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}

	}
}
