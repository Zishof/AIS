package ais.action.master.feeder.integrator.helper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.poi.xssf.usermodel.XSSFCell;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.action.ws.util.PembayaranUtil;
import ais.common.Common;
import ais.common.CommonSearchFilterHelper;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.DetailBiaya;
import ais.database.model.DetailKegiatan;
import ais.database.model.HistoryStatusMahasiswa;
import ais.database.model.Jurusan;
import ais.database.model.Kegiatan;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.PengaturanPembayaranBulanan;
import ais.database.model.Perkuliahan;
import ais.database.model.StatusMahasiswa;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyIntbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Tipe khusus untuk download akm. Kelas ini memberi nama dan batas tanggung jawab yang eksplisit
 * pada perilaku yang diwarisi atau kontrak yang diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Center center}, {@code Combobox
 * searchfakultas}, {@code Combobox searchjurusan}, {@code Intbox searchangkatan}, {@code Combobox
 * searchprogram}, {@code Combobox searchsemester}, {@code Combobox searchtahunakademik}, {@code Textbox
 * nimMahasiswa}; inisialisasi/lifecycle ({@code init()}, {@code initSpreadsheet()}); validasi/perhitungan
 * ({@code hitungJmlData()}); konfigurasi constructor: {@code comboitem}. Bagian lain dari kontrak tetap
 * mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class DownloadAkm extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 790038368339375113L;

	private Center center = new Center();
	private Combobox searchfakultas = new Combobox();

	private Combobox searchjurusan = new Combobox();
	private Intbox searchangkatan = new Intbox(ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR));
	private Combobox searchprogram = new Combobox();

	private Combobox searchsemester = new Combobox();
	private Combobox searchtahunakademik = new Combobox();

	private Textbox nimMahasiswa = new Textbox();
	private Textbox namaMahasiswa = new Textbox();
	private Textbox kelas = new Textbox();
	private MyCheckboxConfig hitungUlang = new MyCheckboxConfig("Hitung Ulang IPK");

	private File file;

	private Combobox searchstatus;

	private MyIntbox mulai;

	private MyIntbox sampai;

	private Label jml;

	public DownloadAkm() {
		super();
		try {

			Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

			Common.initPrograms(searchprogram);

			org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
			comboitem.setLabel(Perkuliahan.GANJIL);
			comboitem.setValue(Perkuliahan.GANJIL);
			searchsemester.appendChild(comboitem);

			comboitem = new MyComboitemConfig();
			comboitem.setLabel(Perkuliahan.GENAP);
			comboitem.setValue(Perkuliahan.GENAP);
			searchsemester.appendChild(comboitem);
			Common.selectComboItem(searchsemester,
					Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP);

			comboitem = new MyComboitemConfig();
			comboitem.setLabel(Perkuliahan.SP);
			comboitem.setValue(Perkuliahan.SP);
			searchsemester.appendChild(comboitem);

			init();

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public DownloadAkm(String title, String border, boolean closable) {
		super(title, border, closable);
		try {
			init();
			// initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private void init() throws Exception {

		EventListener hitungEventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						hitungJmlData();
					}
				});

			}
		};

		setHeight("100%");
		setWidth("100%");
		setBorder("none");
		setClosable(false);
		setPosition("center");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);
		borderlayout.setHeight("2000px");
		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, false);
		north.setHeight("160px");
		north.setAutoscroll(true);

		Div div = new Div();
		div.setParent(north);

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(div);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Angkatan"));
		row.appendChild(searchangkatan);
		searchangkatan.setWidth("90%");
		searchangkatan.addEventListener("onOK", hitungEventListener);

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(searchfakultas);
		searchfakultas.setWidth("90%");
		searchfakultas.setReadonly(true);
		searchfakultas.addEventListener("onChange", hitungEventListener);

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(searchjurusan);
		searchjurusan.setWidth("90%");
		searchjurusan.setReadonly(true);
		searchjurusan.addEventListener("onChange", hitungEventListener);

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Program"));
		row.appendChild(searchprogram);
		searchprogram.setReadonly(true);
		searchprogram.setWidth("90%");
		searchprogram.addEventListener("onChange", hitungEventListener);

		row = new MyFormRow();

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Smt/Kelas"));

		Hbox hbox = new Hbox();
		hbox.appendChild(searchsemester);
		hbox.appendChild(kelas);
		row.appendChild(hbox);
		searchsemester.setReadonly(true);
		searchsemester.setCols(5);
		kelas.setCols(6);

		searchsemester.addEventListener("onChange", hitungEventListener);
		kelas.addEventListener("onOK", hitungEventListener);

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
		row.appendChild(searchtahunakademik);
		searchtahunakademik.setWidth("90%");
		Common.generateTahunAjaran(searchtahunakademik);
		searchtahunakademik.setReadonly(true);

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("NIM/Nama Mahasiswa"));
		Hbox hbox2 = new Hbox();
		row.appendChild(hbox2);
		hbox2.appendChild(nimMahasiswa);
		nimMahasiswa.setCols(8);
		hbox2.appendChild(namaMahasiswa);
		namaMahasiswa.setCols(8);

		nimMahasiswa.addEventListener("onOK", hitungEventListener);
		namaMahasiswa.addEventListener("onOK", hitungEventListener);

		row.appendChild(new ais.ui.util.MyLabelConfig("Status Mahasiswa"));
		row.appendChild(searchstatus = new Combobox());
		Common.insertComboDanSemua(searchstatus, "nama", "kodeEpsbed", StatusMahasiswa.class);
		searchstatus.setWidth("90%");

		row.appendChild(hitungUlang);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		// toolbar.setHeight("25px");
		toolbar.setParent(div);

		MyToolbarbuttonConfig search = new MyToolbarbuttonConfig("Tampilkan Data", "/img/svg/search.svg");
		search.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				initSpreadsheet();
			}
		});
		search.setParent(toolbar);

		MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Ambil Data", "/img/excel.png");
		print.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				try {
					Filedownload.save(new FileInputStream(file),
							"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "akm.xlsx");
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/integrator/helper/DownloadAkm.java:274");

				}
			}
		});
		print.setParent(toolbar);

		toolbar.appendChild(new ais.ui.util.MyLabelConfig("Mulai"));
		mulai = new MyIntbox(0);
		toolbar.appendChild(mulai);
		toolbar.appendChild(new ais.ui.util.MyLabelConfig("Banyak"));
		sampai = new MyIntbox(100);
		toolbar.appendChild(sampai);

		toolbar.appendChild(jml = new Label(ais.common.Common.getBahasaConfig(", jml data -> ")));

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		hitungEventListener.onEvent(null);
	}

	private void hitungJmlData() {
		String kel = kelas.getValue().trim();
		Jurusan jurusan = searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
				|| searchjurusan.getSelectedItem().getValue() == null ? null
						: (Jurusan) searchjurusan.getSelectedItem().getValue();
		Session session = HibernateUtil.currentNativeSession();
		int jmhData = ((Number) session.createCriteria(Mahasiswa.class).setProjection(Projections.rowCount())
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

				.add(kel != null && !kel.trim().isEmpty() ? Restrictions.ilike("kelas", kel.trim(), MatchMode.EXACT)
						: Restrictions.sqlRestriction("true"))

				.add(Restrictions.and(
						nimMahasiswa.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
								: Restrictions.ilike("nim", nimMahasiswa.getValue().trim(), MatchMode.ANYWHERE),

						namaMahasiswa.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
								: Restrictions.ilike("nama", namaMahasiswa.getValue().trim(), MatchMode.ANYWHERE)))

				.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
						|| searchjurusan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("jurusan", jurusan))

				.createAlias("jurusan", "jurusan", Criteria.LEFT_JOIN)

				.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
						|| searchfakultas.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("jurusan.fakultas", searchfakultas, false))

				.add(searchprogram.getSelectedItem() == null || searchprogram.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("program", searchprogram.getSelectedItem().getValue()))

				.add(searchangkatan.getValue() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("tahunangkatan", searchangkatan.getValue()))
				.uniqueResult()).intValue();
		jml.setValue(", jml data -> " + Common.numberFormat.get().format(jmhData));
	}

	@SuppressWarnings({ "unchecked" })
	private void initSpreadsheet() throws Exception {

		final String semester = (String) searchsemester.getSelectedItem().getValue();
		final String tahunAkademik = (String) searchtahunakademik.getSelectedItem().getValue();
		final String kel = kelas.getValue().trim();
		final boolean hitung = hitungUlang.isChecked();

		Common.clear(center);

		System.out.println("init spreadsheet running");
		final Jurusan jurusan = searchjurusan.getSelectedItem() == null
				|| searchjurusan.getSelectedItem().getValue() == null
				|| searchjurusan.getSelectedItem().getValue() == null ? null
						: (Jurusan) searchjurusan.getSelectedItem().getValue();

		final StatusMahasiswa selectedStatusMahasiswa = (StatusMahasiswa) (searchstatus.getSelectedItem() == null
				|| searchstatus.getSelectedItem().getValue() == null ? null
						: searchstatus.getSelectedItem().getValue());

		final String filename = Sessions.getCurrent().getWebApp()
				.getRealPath("/tmp/data_nilai_"
						+ URLEncoder.encode(Common.datetimeFormat2s.get().format(ais.ui.util.WaktuUtil.getDate()), "UTF-8")
						+ ".xlsx");

		(file = new File(filename)).createNewFile();

		final Intbox sizedata = new Intbox(30);
		final Label label = Common.displayLoadBar(this, file, center, sizedata);

		new Thread(new Runnable() {

			@Override
			public void run() {
				try {

				XSSFWorkbook workbook = new XSSFWorkbook();

				XSSFSheet sheet = workbook.createSheet("AKM");
				sheet.setDefaultColumnWidth(18);

				XSSFRow rowhead = sheet.createRow((short) 0);

				rowhead.createCell(0).setCellValue("NIM");
				rowhead.createCell(1).setCellValue("Nama Mahasiswa");
				rowhead.createCell(2).setCellValue("Semester");
				rowhead.createCell(3).setCellValue("SKS");
				rowhead.createCell(4).setCellValue("IP Semester");
				rowhead.createCell(5).setCellValue("SKS Kumulatif");
				rowhead.createCell(6).setCellValue("IP Kumulatif");
				rowhead.createCell(7).setCellValue("Status");
				rowhead.createCell(8).setCellValue("Kode Prodi");
				rowhead.createCell(9).setCellValue("Biaya Kuliah");

				Session session = HibernateUtil.currentNativeSession();

				List<Mahasiswa> mahasiswas = ConstantValues
						.simpleList(session.createCriteria(Mahasiswa.class)
								.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

								.setMaxResults(sampai.getValue() == null ? 100 : sampai.getValue())
								.setFirstResult(mulai.getValue() == null ? 0 : mulai.getValue())

								.add(kel != null && !kel.trim().isEmpty() ? Restrictions.ilike("kelas", kel.trim(),
										MatchMode.EXACT) : Restrictions.sqlRestriction("true"))

								.add(Restrictions.and(
										nimMahasiswa.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
												: Restrictions.ilike("nim", nimMahasiswa.getValue().trim(),
														MatchMode.ANYWHERE),

										namaMahasiswa.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
												: Restrictions.ilike("nama", namaMahasiswa.getValue().trim(),
														MatchMode.ANYWHERE)))

								.add(searchjurusan.getSelectedItem() == null
										|| searchjurusan.getSelectedItem().getValue() == null
										|| searchjurusan.getSelectedItem().getValue() == null
												? Restrictions.sqlRestriction("1=1")
												: Restrictions.eq("jurusan", jurusan))

								.createAlias("jurusan", "jurusan", Criteria.LEFT_JOIN)

								.add(searchfakultas.getSelectedItem() == null
										|| searchfakultas.getSelectedItem().getValue() == null
										|| searchfakultas.getSelectedItem().getValue() == null
												? Restrictions.sqlRestriction("1=1")
												: CommonSearchFilterHelper.eqSelectedWithId("jurusan.fakultas", searchfakultas, false))

								.add(searchprogram.getSelectedItem() == null
										|| searchprogram.getSelectedItem().getValue() == null
												? Restrictions.sqlRestriction("1=1")
												: Restrictions.eq("program",
														searchprogram.getSelectedItem().getValue()))

								.add(searchangkatan.getValue() == null ? Restrictions.sqlRestriction("1=1")
										: Restrictions.eq("tahunangkatan", searchangkatan.getValue()))

								.addOrder(Order.asc("nim")), Mahasiswa.class);

				int size = mahasiswas.size();

				int rowIndex = 1;
				for (Mahasiswa mahasiswa : mahasiswas) {

					try {
						Integer tahun = Integer.parseInt(StringUtils.split(tahunAkademik, "/")[0]);

						Integer currentSemester = Common.getSemester(mahasiswa.getTahunangkatan(),
								semester.equals(Perkuliahan.SP) ? Perkuliahan.GENAP : semester,
								mahasiswa.getPindahKeKampusIniMasukSemester(), tahun, mahasiswa.getSemesterMulai());
						HistoryStatusMahasiswa historyStatusMahasiswa = ais.action.master.helper.HistoryStatusMahasiswaUtil.currentStatus(mahasiswa, tahunAkademik,
								currentSemester);

						if (selectedStatusMahasiswa == null || (selectedStatusMahasiswa != null
								&& historyStatusMahasiswa != null && historyStatusMahasiswa.getStatusMahasiswa() != null
								&& selectedStatusMahasiswa.getId()
										.equals(historyStatusMahasiswa.getStatusMahasiswa().getId()))) {

							label.setValue("Sedang memproses data " + mahasiswa.toString() + " ("
									+ Common.numberFormat.get().format(rowIndex * 100.0 / size) + " %)");

							XSSFRow row = sheet.createRow(rowIndex);

							String id_smt = tahunAkademik.split("/")[0] + (semester.equals(Perkuliahan.SP) ? "3"
									: semester.equals(Perkuliahan.GENAP) ? "2" : "1");

							XSSFCell cell = row.createCell(0);
							cell.setCellValue(mahasiswa.getNim());

							cell = row.createCell(1);
							cell.setCellValue(mahasiswa.getNama());

							cell = row.createCell(2);
							cell.setCellValue(id_smt);

							KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa, currentSemester, null,
									null, hitung);

							cell = row.createCell(3);
							cell.setCellValue(krsMahasiswa.getSksYangDiambil());

							cell = row.createCell(4);
							cell.setCellValue(krsMahasiswa.getIps());

							cell = row.createCell(5);
							cell.setCellValue(krsMahasiswa.getSksk());

							cell = row.createCell(6);
							cell.setCellValue(krsMahasiswa.getIpk());

							cell = row.createCell(7);

							if (historyStatusMahasiswa != null
									&& historyStatusMahasiswa.getStatusMahasiswa().getKodeEpsbed() != null
									&& (historyStatusMahasiswa.getStatusMahasiswa().getKodeEpsbed().trim()
											.equalsIgnoreCase("A")
											|| historyStatusMahasiswa.getStatusMahasiswa().getKodeEpsbed().trim()
													.equalsIgnoreCase("C")
											|| historyStatusMahasiswa.getStatusMahasiswa().getKodeEpsbed().trim()
													.equalsIgnoreCase("D")
											|| historyStatusMahasiswa.getStatusMahasiswa().getKodeEpsbed().trim()
													.equalsIgnoreCase("L")
											|| historyStatusMahasiswa.getStatusMahasiswa().getKodeEpsbed().trim()
													.equalsIgnoreCase("P")
											|| historyStatusMahasiswa.getStatusMahasiswa().getKodeEpsbed().trim()
													.equalsIgnoreCase("N")
											|| historyStatusMahasiswa.getStatusMahasiswa().getKodeEpsbed().trim()
													.equalsIgnoreCase("G")
											|| historyStatusMahasiswa.getStatusMahasiswa().getKodeEpsbed().trim()
													.equalsIgnoreCase("X")
											|| historyStatusMahasiswa.getStatusMahasiswa().getKodeEpsbed().trim()
													.equalsIgnoreCase("K"))

							) {
								cell.setCellValue(historyStatusMahasiswa.getStatusMahasiswa().getKodeEpsbed());
							} else {
								cell.setCellValue("X");
							}

							row.createCell(8).setCellValue(mahasiswa.getJurusan().getKodeEpsbed());

							try {
								@SuppressWarnings("rawtypes")
								Collection detailBiayas = PembayaranUtil.getInstance().getDetailBiayaMahasiswa(
										mahasiswa, currentSemester, ConstantValues.PENDAFTARAN_MAHASISWA_LAMA, false);

								int countPengaturanBulanan = PembayaranUtil.getInstance().countBulanan(session,
										mahasiswa, ConstantValues.PENDAFTARAN_MAHASISWA_LAMA, currentSemester,
										detailBiayas, false, false);

								if (countPengaturanBulanan > 0) {

									detailBiayas = PembayaranUtil.getInstance().getDetailBiayaMahasiswa(mahasiswa,
											currentSemester, ConstantValues.PENDAFTARAN_MAHASISWA_LAMA,
											countPengaturanBulanan > 0 ? "-1" : null, true, false);

								}

								if (!detailBiayas.isEmpty()) {
									Kegiatan kegiatan = mahasiswa.ambilKegiatans(currentSemester,
											ConstantValues.PENDAFTARAN_MAHASISWA_LAMA);
									Collection<DetailKegiatan> detailKegiatans = kegiatan == null
											|| kegiatan.getId() == null ? null : kegiatan.ambilDetailKegiatan(false);
									Double biaya = 0.0;
									for (Object o : detailBiayas) {
										if (o instanceof PengaturanPembayaranBulanan) {
											PengaturanPembayaranBulanan pengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) o;
											Double jumlah = Kegiatan.ambilJumlahTagihan(kegiatan, detailKegiatans,
													mahasiswa, currentSemester, pengaturanPembayaranBulanan);
											biaya += jumlah;
										} else if (o instanceof DetailBiaya) {
											DetailBiaya detailBiaya = (DetailBiaya) o;

											Double jumlah = Kegiatan.ambilJumlahTagihan(kegiatan, detailBiaya, false);
											biaya += jumlah;
										}
									}
									row.createCell(9).setCellValue(biaya);
								} else {
									row.createCell(9).setCellValue(0.0);
								}
							} catch (Exception e) {
								e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/feeder/integrator/helper/DownloadAkm.java:559");
								row.createCell(9).setCellValue(0.0);
							}

							rowIndex++;
						}
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/feeder/integrator/helper/DownloadAkm.java:566");
					}

				}

				Common.setStyled(sheet);
				sizedata.setValue(rowIndex + 1);

				try {
					FileOutputStream fileOut = new FileOutputStream(filename);
					workbook.write(fileOut);
					fileOut.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					Common.tampilErrorJikaAdmin(e);
				}

				System.out.println("Your excel file has been generated! ");

				HibernateUtil.closeSession();

				mahasiswas.clear();
				label.setValue("");
							} catch (Exception e) {
					// FIX "gagal diam-diam" / hang selamanya: sebelumnya try di sini TIDAK punya catch,
					// sehingga exception (mis. gagal query/generate Excel) menembus run() tanpa
					// tertangani -> thread mati & label.setValue("") tak pernah tercapai, progress bar
					// tak pernah selesai (popup menggantung selamanya di sisi user).
					ais.common.Common.tampilErrorJikaAdmin(e);
					label.setValue("Error: " + ais.common.PesanFormalHelper.pesanGagalException(
							"pengambilan data AKM (Aktivitas Kuliah Mahasiswa) dari database untuk diekspor ke Neo Feeder",
							null, e,
							new String[] {
									"Periksa kembali data dan filter yang dipilih (Angkatan/Fakultas/Prodi/Semester/Kelas/NIM), lalu coba ulangi.",
									"Pastikan data KRS dan status Mahasiswa terkait sudah benar dan tersinkron.",
									"Jika kendala berulang, hubungi Administrator Sistem atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini." })
							.replace("\n", " "));
				} finally {
					ais.database.hibernate.HibernateUtil.closeSession();
				}
			}
		}).start();

	}

}
