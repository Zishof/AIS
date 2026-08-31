package ais.action.master.dashboard.keuangan;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.poi.xssf.usermodel.XSSFCell;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.zk.ui.Desktop;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;

import ais.action.ws.util.PembayaranUtil;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.CicilanPembayaran;
import ais.database.model.DetailBiaya;
import ais.database.model.DetailKegiatan;
import ais.database.model.Fakultas;
import ais.database.model.ItemBiaya;
import ais.database.model.JenisKegiatan;
import ais.database.model.Jurusan;
import ais.database.model.Kegiatan;
import ais.database.model.Mahasiswa;
import ais.database.model.PengaturanPembayaranBulanan;
import ais.database.model.Perkuliahan;
import ais.database.model.StatusAwalMahasiswa;
import ais.database.model.StatusMahasiswa;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Komponen dashboard khusus untuk dashboard tunggakan mahasiswa per bulan. Kelas ini memilih
 * variasi data atau tampilan dashboard sambil memakai lifecycle dan mekanisme pemuatan dari kelas
 * induknya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Combobox searchfakultas}, {@code
 * Combobox searchjurusan}, {@code Combobox searchprogram}, {@code Combobox angkatanMhsMulai}, {@code Combobox
 * angkatanMhs}, {@code Combobox searchStatusAwalMahasiswa}, {@code Combobox searchstatus}, {@code Combobox
 * semester}; inisialisasi/lifecycle ({@code initFakultas()}, {@code init()}, {@code initSpreadsheet()}). Bagian
 * lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class DashboardTunggakanMahasiswaPerBulan extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 790038368339375113L;

	private Combobox searchfakultas = new Combobox();
	private Combobox searchjurusan = new Combobox();
	private Combobox searchprogram = new Combobox();
	private Combobox angkatanMhsMulai = new Combobox();private Combobox angkatanMhs = new Combobox();
	private Combobox searchStatusAwalMahasiswa;
	private Combobox searchstatus;
	private Combobox semester = new Combobox();
	private Center center = new Center();

	private void initFakultas() {

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

	}

	private Combobox jenisPembayaran;

	private File file;

	private Combobox tahunAkademik = new Combobox();

	private MyCheckboxConfig BelumBayar;

	private MyCheckboxConfig TelahBayar;

	public DashboardTunggakanMahasiswaPerBulan() {
		super();
		try {
			initFakultas();
			init();

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public DashboardTunggakanMahasiswaPerBulan(String title, String border, boolean closable) {
		super(title, border, closable);
		try {
			initFakultas();
			init();
			// initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	@SuppressWarnings("deprecation")
	private void init() throws Exception {

		jenisPembayaran = Common.initJenisPembayaranMahasiswa(jenisPembayaran);
		jenisPembayaran.setReadonly(true);

		for (int i = 1; i < 32; i++) {
			org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
			comboitem.setLabel(i + "");
			comboitem.setValue(i);
			semester.appendChild(comboitem);
		}
		semester.setReadonly(true);
		Common.selectComboItem(semester, 1);

		setHeight("100%");
		setWidth("100%");
		setBorder("none");
		setClosable(false);
		setPosition("center");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, false);
		/* FIX 21-08-2026: tinggi panel filter kurang 52px sehingga baris toolbar
		 * (Proses/Download) terpotong di bagian bawah. Ditambah satu tinggi baris
		 * toolbar ZK. Autoscroll tetap aktif sebagai pengaman bila isi filter
		 * bertambah di kemudian hari. */
		north.setHeight("252px");
		north.setAutoscroll(true);

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
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(searchfakultas);
		searchfakultas.setWidth("90%");
		searchfakultas.setWidth("90%");
		// searchfakultas.addEventListener("onChange", new EventListener() {
		//
		// @Override
		// public void onEvent(Event arg0) throws Exception {
		// initSpreadsheet();
		// }
		// });

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(searchjurusan);
		searchjurusan.setWidth("90%");
		searchjurusan.setWidth("90%");
		// searchjurusan.addEventListener("onChange", new EventListener() {
		//
		// @Override
		// public void onEvent(Event arg0) throws Exception {
		// initSpreadsheet();
		// }
		// });

		Common.initPrograms(searchprogram);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Program"));
		row.appendChild(searchprogram);
		searchprogram.setWidth("90%");

		row.appendChild(new ais.ui.util.MyLabelConfig("Status Awal"));
		row.appendChild(searchStatusAwalMahasiswa = new Combobox());
		Common.insertComboDanSemua(searchStatusAwalMahasiswa, "nama", StatusAwalMahasiswa.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.selectComboItem(searchStatusAwalMahasiswa, null);
		searchStatusAwalMahasiswa.setCols(4);
		searchStatusAwalMahasiswa.setReadonly(true);

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Angkatan"));
		row.appendChild(angkatanMhs);
		for (int i = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR) - 10; i <= ais.ui.util.WaktuUtil
				.getCalendar().get(Calendar.YEAR) + 10; i++) {
			MyComboitemConfig comboitem = new MyComboitemConfig();
			comboitem.setLabel(i + "");
			comboitem.setValue(i);
			angkatanMhs.appendChild(comboitem);
		}
		Common.selectComboItem(angkatanMhs, ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR));

		row = new MyFormRow();
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester"));
		row.appendChild(semester);
		row.setParent(rows);
		semester.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
		tahunAkademik = Common.generateTahunAjaran(tahunAkademik);
		row.appendChild(tahunAkademik);
		tahunAkademik.setWidth("90%");
		tahunAkademik.setReadonly(true);

		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Pembayaran"));
		row.appendChild(jenisPembayaran);
		row.setParent(rows);
		jenisPembayaran.setWidth("90%");

		row.appendChild(new ais.ui.util.MyLabelConfig("Status"));
		row.appendChild(searchstatus = new Combobox());
		searchstatus.setCols(4);
		searchstatus.setReadonly(true);
		Common.insertComboDanSemua(searchstatus, new String[] { "nama", "kodeEpsbed" }, StatusMahasiswa.class);
		Common.selectComboItem(searchstatus, null);

		BelumBayar = new MyCheckboxConfig("Belum bayar");
		TelahBayar = new MyCheckboxConfig("Telah bayar");
		BelumBayar.setParent(row);
		TelahBayar.setParent(row);

		BelumBayar.setChecked(true);
		TelahBayar.setChecked(true);

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		row = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(row, "8");
		row.setParent(rows);
		Toolbar toolbar = new Toolbar();
		toolbar.setParent(row);
		MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Proses", "/img/print.png");
		print.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				initSpreadsheet();
			}
		});
		print.setParent(toolbar);

		print = new MyToolbarbuttonConfig("Download", "/img/print.png");
		print.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				try {
					Filedownload.save(new FileInputStream(file), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "Data Mahasiswa.xlsx");
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/keuangan/DashboardTunggakanMahasiswaPerBulan.java:262");

				}
			}
		});
		print.setParent(toolbar);

	}

	@SuppressWarnings({ "unchecked" })
	private void initSpreadsheet() throws Exception {

		Common.clear(center);

		final String tahunAkademik = (String) (this.tahunAkademik.getSelectedItem() == null
				|| this.tahunAkademik.getSelectedItem().getValue() == null ? null
						: this.tahunAkademik.getSelectedItem().getValue());

		final Fakultas fakultas = (Fakultas) (searchfakultas.getSelectedItem() == null
				|| searchfakultas.getSelectedItem().getValue() == null
				|| searchfakultas.getSelectedItem().getValue() == null ? null
						: searchfakultas.getSelectedItem().getValue());
		final Jurusan jurusan = (Jurusan) (searchjurusan.getSelectedItem() == null
				|| searchjurusan.getSelectedItem().getValue() == null
				|| searchjurusan.getSelectedItem().getValue() == null ? null
						: searchjurusan.getSelectedItem().getValue());

		final Integer smt = (Integer) (semester.getSelectedItem() == null ? null
				: semester.getSelectedItem().getValue());
		final String program = (String) (searchprogram.getSelectedItem() == null
				|| searchprogram.getSelectedItem().getValue() == null ? null
						: searchprogram.getSelectedItem().getValue());

		final Integer angkatan = (Integer) (angkatanMhs.getSelectedItem() == null ? null
				: angkatanMhs.getSelectedItem().getValue());

		if (tahunAkademik == null) {
			return;
		}

		final StatusMahasiswa statusMahasiswa = (StatusMahasiswa) (searchstatus.getSelectedItem() == null
				|| searchstatus.getSelectedItem().getValue() == null ? null
						: searchstatus.getSelectedItem().getValue());
		final StatusAwalMahasiswa statusAwalMahasiswa = (StatusAwalMahasiswa) (searchStatusAwalMahasiswa
				.getSelectedItem() == null ? null : searchStatusAwalMahasiswa.getSelectedItem().getValue());

		final String filename = Sessions.getCurrent().getWebApp().getRealPath("/tmp/data_"
				+ URLEncoder.encode(Common.datetimeFormat2s.get().format(ais.ui.util.WaktuUtil.getDate()), "UTF-8") + ".xlsx");

		(file = new File(filename)).createNewFile();

		final Intbox sizedata = new Intbox(30);
		// center (Center borderlayout) hanya boleh 1 anak: bungkus dengan satu Div.
		final org.zkoss.zul.Div pembungkusVisual = new org.zkoss.zul.Div();
		pembungkusVisual.setWidth("100%");
		pembungkusVisual.setStyle("height:100%;overflow:auto;box-sizing:border-box;");
		pembungkusVisual.setParent(center);
		final org.zkoss.zul.Div chartHost = new org.zkoss.zul.Div();
		chartHost.setWidth("100%");
		chartHost.setParent(pembungkusVisual);
		final org.zkoss.zul.Div tableHost = new org.zkoss.zul.Div();
		tableHost.setWidth("100%");
		tableHost.setStyle("height:100%;");
		tableHost.setParent(pembungkusVisual);
		final Label label = Common.displayLoadBar(this, file, tableHost, sizedata);
		// final boolean telahDinilai = TelahDinilai.isChecked();
		// final boolean belumDinilai = BelumDinilai.isChecked();

		// KE-21: chartHost.appendChild() dipanggil di dalam Thread mentah (bukan event
		// listener) -> "Components can be accessed only in event listeners". Tangkap
		// desktop di SINI (thread ZK yang sah) agar bisa diaktifkan sesaat via
		// Executions.activate/deactivate tepat sebelum sentuhan komponen di background thread.
		final Desktop desktop = chartHost.getDesktop();

		new Thread(new Runnable() {

			@SuppressWarnings("rawtypes")
			@Override
			public void run() {

				java.util.LinkedHashMap<String, Double> tagihanPerBulan = new java.util.LinkedHashMap<String, Double>();
				java.util.LinkedHashMap<String, Double> dibayarPerBulan = new java.util.LinkedHashMap<String, Double>();

				XSSFWorkbook workbook = new XSSFWorkbook();

				XSSFSheet sheet = workbook.createSheet("BAYAR");
				sheet.setDefaultColumnWidth(10);

				XSSFRow rowhead = sheet.createRow((short) 0);

				rowhead.createCell(0).setCellValue("NIM");
				rowhead.createCell(1).setCellValue("NAMA");
				rowhead.createCell(2).setCellValue("BULAN");
				rowhead.createCell(3).setCellValue("SEMESTER");
				rowhead.createCell(4).setCellValue("ITEM BIAYA");
				rowhead.createCell(5).setCellValue("NOMINAL");
				rowhead.createCell(6).setCellValue("TELAH DIBAYAR");
				rowhead.createCell(7).setCellValue("NILAI DIBAYAR");
				rowhead.createCell(8).setCellValue("TAGIHAN");
				rowhead.createCell(9).setCellValue("TOTAL BELUM TERBAYAR");
				rowhead.createCell(10).setCellValue("TOTAL TELAH TERBAYAR");

				Session session = null;
				List<Mahasiswa> mahasiswas = null;
				try {
					session = HibernateUtil.currentNativeSession();

					mahasiswas = session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

						.add(statusAwalMahasiswa == null ? Restrictions.sqlRestriction("true")
								: Restrictions.eq("statusAwalMahasiswa", statusAwalMahasiswa))

						.add(statusMahasiswa != null ? Restrictions.sqlRestriction(
								" this_.id in (select mahasiswa from history_status_mahasiswa where status_mahasiswa="
										+ statusMahasiswa.getId() + " and tahunakademik = '" + tahunAkademik
										+ "' and semester%2=" + (semester.equals(Perkuliahan.GANJIL) ? 1 : 0) + ") ")
								: Restrictions.sqlRestriction("true"))

						.createAlias("jurusan", "jurusan").addOrder(Order.asc("jurusan"))
						.add(Restrictions.eq("tahunangkatan", angkatan)).addOrder(Order.asc("nim"))

						.add(fakultas == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("jurusan.fakultas", fakultas))

						.add(jurusan == null ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("jurusan", jurusan))

						.add(program == null ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("program", program))

						.add(angkatan == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("tahunangkatan", angkatan))

						.list();

				int size = mahasiswas.size();

				int rowIndex = 1;
				int rowIndexMhs = 1;

				Double totalTagihanBelumTerbayarSemua = 0.0;
				Double totalTagihanTelahTerbayarSemua = 0.0;

				for (Mahasiswa mahasiswa : mahasiswas) {

					label.setValue("Sedang memproses data " + mahasiswa.toString() + " ("
							+ Common.numberFormat.get().format(rowIndexMhs * 100.0 / size) + " %)");
					rowIndexMhs++;

					JenisKegiatan jenisKegiatan = (JenisKegiatan) jenisPembayaran.getSelectedItem().getValue();
					List<CicilanPembayaran> cicilanPembayarans = mahasiswa.ambilCicilan();

					Kegiatan kegiatan = mahasiswa.ambilKegiatans(smt, jenisKegiatan);
					Collection<DetailKegiatan> detailKegiatans = kegiatan == null || kegiatan.getId() == null ? null
							: kegiatan.ambilDetailKegiatan(false);
					Collection detailBiayas = PembayaranUtil.getInstance().getDetailBiayaMahasiswa(mahasiswa, smt,
							jenisKegiatan, false);

					int countPengaturanBulanan = PembayaranUtil.getInstance().countBulanan(session, mahasiswa,
							jenisKegiatan, smt, detailBiayas, false, false);

					detailBiayas = PembayaranUtil.getInstance().getDetailBiayaMahasiswa(mahasiswa, smt, jenisKegiatan,
							countPengaturanBulanan > 0 ? "-1" : null, true, false);

					if (detailBiayas.isEmpty()) {
						continue;
					}

					Double totalTagihanBelumTerbayar = 0.0;
					Double totalTagihanTelahTerbayar = 0.0;

					XSSFRow row = null;

					Double totalTagihan = 0.0;
					for (Object o : detailBiayas) {
						try {
							if (o != null) {
								if (o instanceof PengaturanPembayaranBulanan) {
									PengaturanPembayaranBulanan pengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) o;
									Double tag = Kegiatan.ambilJumlahTagihan(kegiatan, detailKegiatans, mahasiswa, smt,
											pengaturanPembayaranBulanan);
									if (tag < 0.01) {
										continue;
									}

									row = sheet.createRow(rowIndex);
									XSSFCell cell = row.createCell(0);
									cell.setCellValue(mahasiswa.getNim());

									cell = row.createCell(1);
									cell.setCellValue(mahasiswa.getNama());

									cell = row.createCell(2);
									cell.setCellValue(pengaturanPembayaranBulanan.getNamaBulan());

									cell = row.createCell(3);
									cell.setCellValue(pengaturanPembayaranBulanan.getDetailBiaya().getSemester());

									cell = row.createCell(4);
									cell.setCellValue(
											pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama());

									cell = row.createCell(5);
									cell.setCellValue(Common.numberFormat.get().format(tag));

									Double telahDibayar = mahasiswa.hitungTotalCicilan(kegiatan,
											pengaturanPembayaranBulanan, cicilanPembayarans);

									if (telahDibayar != null && telahDibayar > 0.1) {
										totalTagihanTelahTerbayar += telahDibayar;
									} else {
										totalTagihanBelumTerbayar += tag;
									}

									{
										String bln = pengaturanPembayaranBulanan.getNamaBulan();
										if (bln == null || bln.trim().length() == 0) {
											bln = "Tanpa Bulan";
										}
										double by = (telahDibayar == null || telahDibayar < 0.1) ? 0
												: telahDibayar.doubleValue();
										Double t0 = tagihanPerBulan.get(bln);
										tagihanPerBulan.put(bln, Double.valueOf((t0 == null ? 0 : t0.doubleValue())
												+ (tag == null ? 0 : tag.doubleValue())));
										Double b0 = dibayarPerBulan.get(bln);
										dibayarPerBulan.put(bln, Double.valueOf((b0 == null ? 0 : b0.doubleValue()) + by));
									}

									cell = row.createCell(6);
									cell.setCellValue(telahDibayar == null || telahDibayar < 0.01 ? "Belum" : "Sudah");

									cell = row.createCell(7);
									cell.setCellValue(telahDibayar == null || telahDibayar < 0.01 ? ""
											: Common.numberFormat.get().format(telahDibayar));

									if (telahDibayar == null || telahDibayar < 0.01) {
										totalTagihan += tag;
									}

									cell = row.createCell(8);
									cell.setCellValue(Common.numberFormat.get().format(totalTagihan));
								} else if (o instanceof DetailBiaya) {
									DetailBiaya detailBiaya = (DetailBiaya) o;
									ItemBiaya itemBiaya = detailBiaya.getItemBiaya();

									Double tag = Kegiatan.ambilJumlahTagihan(kegiatan, detailBiaya, false);
									if (tag < 0.01) {
										continue;
									}

									Double telahDibayar = mahasiswa.hitungTotalCicilan(kegiatan, detailBiaya,
											cicilanPembayarans);

									{
										double by = (telahDibayar == null || telahDibayar < 0.1) ? 0
												: telahDibayar.doubleValue();
										Double t0 = tagihanPerBulan.get("Tanpa Bulan");
										tagihanPerBulan.put("Tanpa Bulan", Double.valueOf((t0 == null ? 0 : t0.doubleValue())
												+ (tag == null ? 0 : tag.doubleValue())));
										Double b0 = dibayarPerBulan.get("Tanpa Bulan");
										dibayarPerBulan.put("Tanpa Bulan",
												Double.valueOf((b0 == null ? 0 : b0.doubleValue()) + by));
									}

									row = sheet.createRow(rowIndex);
									XSSFCell cell = row.createCell(0);
									cell.setCellValue(mahasiswa.getNim());

									cell = row.createCell(1);
									cell.setCellValue(mahasiswa.getNama());

									cell = row.createCell(2);
									cell.setCellValue("-");

									cell = row.createCell(3);
									cell.setCellValue(detailBiaya.getSemester());

									cell = row.createCell(4);
									cell.setCellValue(itemBiaya.getNama());

									cell = row.createCell(5);
									cell.setCellValue(Common.numberFormat.get().format(tag));

									if (telahDibayar != null && telahDibayar > 0.1) {
										totalTagihanTelahTerbayar += telahDibayar;
									} else {
										totalTagihanBelumTerbayar += tag;
									}

									cell = row.createCell(6);
									cell.setCellValue(telahDibayar == null || telahDibayar < 0.01 ? "Belum" : "Sudah");

									cell = row.createCell(7);
									cell.setCellValue(telahDibayar == null || telahDibayar < 0.01 ? ""
											: Common.numberFormat.get().format(telahDibayar));

									if (telahDibayar == null || telahDibayar < 0.01) {
										totalTagihan += tag;
									}

									cell = row.createCell(8);
									cell.setCellValue(Common.numberFormat.get().format(totalTagihan));
								}

								rowIndex++;
							}
						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/dashboard/keuangan/DashboardTunggakanMahasiswaPerBulan.java:567");
						}
					}

					if (row != null) {
						XSSFCell cell = row.createCell(9);
						cell.setCellValue(Common.numberFormat.get().format(totalTagihanBelumTerbayar));

						cell = row.createCell(10);
						cell.setCellValue(Common.numberFormat.get().format(totalTagihanTelahTerbayar));
					}

				}

				XSSFRow row = sheet.createRow(rowIndex);

				XSSFCell cell = row.createCell(9);
				cell.setCellValue("Total Semua");

				cell = row.createCell(10);
				cell.setCellValue(Common.numberFormat.get().format(totalTagihanBelumTerbayarSemua));

				cell = row.createCell(11);
				cell.setCellValue(Common.numberFormat.get().format(totalTagihanTelahTerbayarSemua));

				Common.setStyled(sheet);sizedata.setValue(rowIndex + 1);

				try {
					FileOutputStream fileOut = new FileOutputStream(filename);
					workbook.write(fileOut);
					fileOut.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					Common.tampilErrorJikaAdmin(e);
				}

				// Ringkasan visual (HTML/CSS): tren Tagihan vs Dibayar per bulan + komposisi.
				try {
					java.util.List<String> labelBulan = new java.util.ArrayList<String>(tagihanPerBulan.keySet());
					java.util.List<Double> seriTagihan = new java.util.ArrayList<Double>();
					java.util.List<Double> seriDibayar = new java.util.ArrayList<Double>();
					for (int li = 0; li < labelBulan.size(); li++) {
						seriTagihan.add(tagihanPerBulan.get(labelBulan.get(li)));
						seriDibayar.add(dibayarPerBulan.get(labelBulan.get(li)));
					}
					String html = ais.action.master.dashboard.helper.DashboardVisualHelper.trenDuaSeri(
							"Tren Tagihan vs Pembayaran per Bulan",
							"Garis tagihan dan pembayaran tiap bulan; selisihnya adalah tunggakan.", labelBulan,
							seriTagihan, "Tagihan", seriDibayar, "Dibayar", "Tagihan vs Dibayar");
					if (desktop != null && desktop.isAlive()) {
						try {
							Executions.activate(desktop);
							try {
								chartHost.appendChild(new org.zkoss.zul.Html(html));
							} finally {
								Executions.deactivate(desktop);
							}
						} catch (org.zkoss.zk.ui.DesktopUnavailableException due) { ais.common.ErrorAuditUtil.record(due, "auto-audit(empty-catch) src/ais/action/master/dashboard/keuangan/DashboardTunggakanMahasiswaPerBulan.java:624");
							// Tab/halaman keburu ditutup user tepat saat aktivasi -- lewati saja.
						}
					}
				} catch (Exception eChart) {
					Common.tampilErrorJikaAdmin(eChart);
				}

				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				} finally {
					if (session != null) {
						try {
							HibernateUtil.closeSession();
						} catch (Exception e) {
							Common.tampilErrorJikaAdmin(e);
						}
					}
					if (mahasiswas != null) {
						try {
							mahasiswas.clear();
						} catch (Exception e) {
							Common.tampilErrorJikaAdmin(e);
						}
					}
					try {
						label.setValue("");
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);
					}
				}
			}
		}).start();

	}
}
