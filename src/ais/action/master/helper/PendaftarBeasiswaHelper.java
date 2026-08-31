package ais.action.master.helper;


import ais.common.CommonSearchFilterHelper;
import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.poi.ss.usermodel.Hyperlink;
import org.zkoss.poi.xssf.usermodel.XSSFCell;
import org.zkoss.poi.xssf.usermodel.XSSFCellStyle;
import org.zkoss.poi.xssf.usermodel.XSSFColor;
import org.zkoss.poi.xssf.usermodel.XSSFFont;
import org.zkoss.poi.xssf.usermodel.XSSFHyperlink;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zss.ui.Spreadsheet;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.beasiswa.BeasiswaUntukMahasiswaAction;
import ais.action.report.Report;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.PesanFormalHelper;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.Beasiswa;
import ais.database.model.Jenjang;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.beasiswa.BeasiswaPunyaPersyaratan;
import ais.database.model.beasiswa.MahasiswaBeasiswaPersyaratan;
import ais.database.model.beasiswa.MahasiswaDaftarBeasiswa;
import ais.database.model.beasiswa.PersyaratanBeasiswa;
import ais.database.model.file.LampiranBeasiswaMahasiswa;
import ais.ui.util.DataCriteria;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Helper ZK untuk fitur "Pendaftar Beasiswa": menampilkan, memfilter, menilai (terima/tolak),
 * dan mengelola daftar mahasiswa yang mendaftar pada satu {@link Beasiswa} tertentu. Tampilan
 * utama berupa grid berpaging yang menunjukkan data akademik ringkas tiap pendaftar (SKS/SKSK,
 * IP/IPK hasil sinkronisasi {@link Common#singkronkanKrsMahasiswa}), skor total, serta checkbox
 * "Terima"/"Ditolak" yang langsung menyimpan perubahan status ke {@link MahasiswaDaftarBeasiswa}
 * saat diklik (hanya aktif bila mode {@code approve} diaktifkan lewat
 * {@link #displayPrasyaratBeasiswa}).
 *
 * <p>
 * Selain grid, kelas ini menyediakan sejumlah aksi laporan/berkas: cetak PDF daftar pendaftar,
 * daftar penerima, dan rekap penerima ({@link ais.action.report.Report}); ekspor seluruh data
 * pendaftar (termasuk jawaban tiap {@link PersyaratanBeasiswa} beasiswa tersebut, dengan
 * hyperlink ke lampiran bila persyaratan mewajibkan unggahan) ke berkas Excel lewat
 * {@link #cetakDataCustomButton}; serta impor massal dari Excel (menerima/menolak berdasarkan
 * kolom "Diterima" dan menghitung ulang {@code memenuhiSyarat}) via tombol Upload — proses
 * ekspor/impor keduanya berjalan asinkron di thread terpisah dengan indikator progres memakai
 * {@link org.zkoss.zul.Timer} dan {@link org.zkoss.zul.Label} yang di-poll.
 * </p>
 *
 * <p>
 * Tombol "Hitung Skor" menjumlahkan nilai numerik yang tersimpan pada jawaban bertipe
 * {@link PersyaratanBeasiswa#PILIHAN_CUSTOM} (format {@code "label:angka"}) untuk seluruh
 * pendaftar yang cocok filter aktif, lalu menuliskannya ke {@code totalSkor}. Tombol "Baru"
 * membuka dialog {@link AmbilDataMahasiswaSeleksiBeasiswaHelper} untuk menambah pendaftar baru.
 * </p>
 */
public class PendaftarBeasiswaHelper implements DataLoader, DataCriteria {

	private MyGrid grid;
	private Beasiswa beasiswa;
	private Textbox nim;
	private Combobox fakultas;
	private Combobox jenjang;
	private Combobox jurusan;

	private Paging paging;
	private Intbox angkatan;
	private MyCheckboxConfig hanyaYgBelumDiterima;
	private boolean approve;

	/**
	 * Renderer baris grid: menampilkan NIM, nama, jurusan, SKS/SKSK dan IP/IPK terkini (via
	 * {@link Common#singkronkanKrsMahasiswa}), skor total, serta checkbox terima/tolak dan
	 * tombol edit/hapus. Checkbox "Ditolak" dan "Terima" saling eksklusif (memilih salah satu
	 * menonaktifkan yang lain) dan langsung menulis perubahan lewat {@link Common#refreshUpdate}.
	 */
	class PendaftarBeasiswaRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row row, Object data) throws Exception {
			row.setValign("top");
			final MahasiswaDaftarBeasiswa mahasiswaDaftarBeasiswa = (MahasiswaDaftarBeasiswa) data;

			final Mahasiswa mahasiswa = mahasiswaDaftarBeasiswa.getMahasiswa();

			new Label(mahasiswa.getNim()).setParent(row);
			new Label(mahasiswa.getNama()).setParent(row);
			new Label(mahasiswa.getJurusan() == null ? "" : mahasiswa.getJurusan().getNama()).setParent(row);

			Integer tahunAngkatanMhs = mahasiswa.getTahunangkatan();
			String semesterMulai = mahasiswaDaftarBeasiswa.getBeasiswa().getSemester();
			String ta = mahasiswaDaftarBeasiswa.getBeasiswa().getTahunAkademik();
			Integer tahun = Integer.parseInt(StringUtils.split(ta, "/")[0]);
			Integer semester = Common.getSemester(tahunAngkatanMhs, semesterMulai,
					mahasiswa.getPindahKeKampusIniMasukSemester(), tahun, mahasiswa.getSemesterMulai());

			KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa, semester, null, null);
			new Label(Common.numberFormat.get().format(krsMahasiswa.getSksYangDiambil()) + " / "
					+ Common.numberFormat.get().format(krsMahasiswa.getSksk())).setParent(row);
			new Label(Common.numberFormat.get().format(krsMahasiswa.getIps()) + " / "
					+ Common.numberFormat.get().format(krsMahasiswa.getIpk())).setParent(row);

			new Label(Common.numberFormat.get().format(mahasiswaDaftarBeasiswa.getTotalSkor())).setParent(row);

			final Label labelmemenuhiSyarat = new Label();
			labelmemenuhiSyarat.setParent(row);

			Vbox vbox = new Vbox();
			vbox.setParent(row);

			final MyCheckboxConfig labelTelahTerpenuhi = new MyCheckboxConfig("Terima");
			labelTelahTerpenuhi.setDisabled(!approve);
			labelTelahTerpenuhi.setParent(vbox);
			labelTelahTerpenuhi
					.setChecked(mahasiswaDaftarBeasiswa.getTerima().equals(MahasiswaDaftarBeasiswa.DITERIMA));

			labelTelahTerpenuhi.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					mahasiswaDaftarBeasiswa.setTerima(labelTelahTerpenuhi.isChecked() ? MahasiswaDaftarBeasiswa.DITERIMA
							: MahasiswaDaftarBeasiswa.BELUM_DIPROSES);
					Common.refreshUpdate(mahasiswaDaftarBeasiswa);
				}
			});

			final MyCheckboxConfig labelTelahDitolak = new MyCheckboxConfig("Ditolak");
			labelTelahDitolak.setDisabled(!approve);
			labelTelahDitolak.setParent(vbox);
			labelTelahDitolak.setChecked(mahasiswaDaftarBeasiswa.getTerima().equals(MahasiswaDaftarBeasiswa.DITOLAK));
			labelTelahTerpenuhi.setDisabled(labelTelahDitolak.isChecked());
			labelTelahDitolak.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					labelTelahTerpenuhi.setDisabled(labelTelahDitolak.isChecked());

					mahasiswaDaftarBeasiswa.setTerima(labelTelahDitolak.isChecked() ? MahasiswaDaftarBeasiswa.DITOLAK
							: MahasiswaDaftarBeasiswa.BELUM_DIPROSES);
					Common.refreshUpdate(mahasiswaDaftarBeasiswa);
				}
			});

			Hbox toolbar = new Hbox();
			toolbar.setParent(row);

			final MyToolbarbuttonConfig buttonEdit = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			buttonEdit.setTooltiptext("Ubah Data");
			buttonEdit.setParent(toolbar);
			buttonEdit.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					BeasiswaUntukMahasiswaAction.onAddExternal(event, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							loadData(null);
						}
					}, mahasiswaDaftarBeasiswa.getBeasiswa(), mahasiswa);
				}

			});

			final MyToolbarbuttonConfig buttonDelete = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			buttonDelete.setTooltiptext("Hapus Data");
			buttonDelete.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {

											Common.refreshDelete(HibernateUtil.currentSession(),
													mahasiswaDaftarBeasiswa);

											loadData(null);
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											MyMessageboxConfig.show(
													"Data ini tidak dapat dihapus .., karena berelasi dengan data lainnya, error-nya adalah sbagai berikut:"
															+ e.getMessage());
										}

									}

								}
							});

				}
			});
			buttonDelete.setParent(toolbar);

		}

	}

	/**
	 * Membangun {@link Criteria} pendaftar {@link #beasiswa} yang aktif, difilter berdasarkan
	 * status "belum diterima" (checkbox {@code hanyaYgBelumDiterima}), angkatan, kata kunci
	 * NIM/nama, jurusan, jenjang, dan fakultas — implementasi kontrak {@link DataCriteria}.
	 *
	 * @param order bila {@code true}, tambahkan pengurutan (angkatan desc, NIM asc)
	 * @return criteria siap dieksekusi/dihitung jumlah barisnya
	 */
	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(MahasiswaDaftarBeasiswa.class)
				.add(Restrictions.eq("beasiswa", beasiswa))

				.add(hanyaYgBelumDiterima.isChecked() ? Restrictions.eq("terima", 0)
						: Restrictions.sqlRestriction("true"))

				.createCriteria("mahasiswa")

				.add(angkatan.getValue() == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("tahunangkatan", angkatan.getValue()))

				.add(nim.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.or(Restrictions.ilike("nim", nim.getValue().trim(), MatchMode.ANYWHERE),
								Restrictions.ilike("nama", nim.getValue().trim(), MatchMode.ANYWHERE)))

				.add(jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null
						? Restrictions.isNotNull("jurusan")
						: CommonSearchFilterHelper.eqSelectedWithId("jurusan", jurusan, false));

		if (order)
			criteria.addOrder(Order.desc("tahunangkatan")).addOrder(Order.asc("nim"));

		criteria.createCriteria("jurusan", Criteria.LEFT_JOIN)

				.add(jenjang.getSelectedItem() == null || jenjang.getSelectedItem().getValue() == null
						? Restrictions.isNotNull("jenjang")
						: Restrictions.eq("jenjang", jenjang.getSelectedItem().getValue()))

				.add(fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null
						? Restrictions.isNotNull("fakultas")
						: CommonSearchFilterHelper.eqSelectedWithId("fakultas", fakultas, false));

		return criteria;
	}

	/** Implementasi {@link DataLoader#loadData}: memuat ulang paging dan satu halaman data pendaftar (50 baris) lalu merender ulang grid dengan {@link PendaftarBeasiswaRenderer}. */
	@SuppressWarnings("unchecked")
	public void loadData(Object value) {

		Common.initPaging50(initCriteria(false), paging);

		List<MahasiswaDaftarBeasiswa> mahasiswaDaftarBeasiswa = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE_50)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE_50 * (paging == null ? 0 : paging.getActivePage())).list();

		ListModel strset = new SimpleListModel(mahasiswaDaftarBeasiswa);
		grid.setRowRenderer(new PendaftarBeasiswaRenderer());
		grid.setModelCheckMobile(strset);

	}

	private DataLoader getDataloader() {
		return this;
	}

	/**
	 * Membuat tombol toolbar yang, saat diklik, mengekspor seluruh data pendaftar (mengikuti
	 * filter aktif dari {@code dataCriteria}, tanpa batas paging) ke berkas Excel (.xlsx) di
	 * direktori sementara aplikasi. Kolom baku (ID, NIM, Nama, Jurusan, Fakultas, Diterima, Skor,
	 * IPK) diikuti satu kolom per {@link PersyaratanBeasiswa} beasiswa ini; sel persyaratan yang
	 * mewajibkan lampiran diberi hyperlink ke berkas lampiran (bila ada). Selama proses berjalan
	 * di thread terpisah, sebuah {@link org.zkoss.zul.Timer} mem-poll label progres dan pada
	 * akhirnya membuka jendela pratinjau {@link org.zkoss.zss.ui.Spreadsheet} dengan tombol
	 * unduh. Baris yang gagal diproses dilewati (dicatat lewat {@link Common#tampilErrorJikaAdmin})
	 * tanpa menghentikan keseluruhan ekspor.
	 *
	 * @param dataCriteria penyedia criteria sumber data (biasanya {@code this}); boleh berbeda dari filter grid utama
	 * @param buttonLabel  label tombol
	 * @param buttonImage  path ikon tombol
	 * @return tombol toolbar siap ditempel ke toolbar pemanggil
	 */
	@SuppressWarnings("unchecked")
	public MyToolbarbuttonConfig cetakDataCustomButton(final DataCriteria dataCriteria, String buttonLabel,
			String buttonImage) {

		Session session = HibernateUtil.currentSession();
		final List<PersyaratanBeasiswa> persyaratanBeasiswas = session.createCriteria(BeasiswaPunyaPersyaratan.class)
				.createAlias("persyaratanBeasiswa", "persyaratanBeasiswa").add(Restrictions.eq("beasiswa", beasiswa))
				.setProjection(Projections.property("persyaratanBeasiswa"))
				.addOrder(Order.asc("persyaratanBeasiswa.nama")).addOrder(Order.asc("persyaratanBeasiswa.labelInputan"))
				.list();

		MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig(buttonLabel, buttonImage);

		toolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				final Label label = new Label(ais.common.Common.getBahasaConfig("Proses load data .."));
				final Intbox intbox = new Intbox(10);
				Clients.showBusy(label.getValue());

				final String filename = Sessions.getCurrent().getWebApp()
						.getRealPath("/tmp/cetak_data_"
								+ URLEncoder.encode(
										Common.datetimeFormat2s.get().format(ais.ui.util.WaktuUtil.getDate()), "UTF-8")
								+ ".xlsx");
				final File file;
				(file = new File(filename)).createNewFile();

				final Timer timer = new Timer(200);
				timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				timer.setRepeats(true);
				timer.addEventListener("onTimer", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						try {

							Clients.showBusy(label.getValue());
							System.out.println("label " + label.getValue());

							if (label.getValue().trim().equalsIgnoreCase("-")) {
								Clients.clearBusy();
								timer.detach();
							} else if (label.getValue().isEmpty()) {

								Center center = new Center();
								final MyWindow window = new MyWindow("Cetak Data", "none", true);
								window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
								window.setHeight("97%");
								window.setWidth("90%");

								Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
								borderlayout.setParent(window);

								ais.ui.util.ZkCompat.setFlex(center, true);
								center.setParent(borderlayout);

								System.out.println("loading file " + file.getAbsolutePath());
								Common.clear(center);
								Spreadsheet spreadsheet = new ais.ui.util.MySpreadsheet();
								Common.clear(center);
								spreadsheet.setParent(center);
								spreadsheet.setWidth("100%");
								spreadsheet.setHeight("100%");
								spreadsheet.setSrc("../../tmp/" + file.getName());
								spreadsheet.setMaxrows(intbox.getValue() + 1);
								spreadsheet.setMaxcolumns(persyaratanBeasiswas.size() + 8);
								ais.ui.util.PratinjauXlsxHelper.gantiSpreadsheetDenganGrid(spreadsheet);

								South south = new South();
								south.setParent(borderlayout);

								Toolbar toolbar = new Toolbar();
								// toolbar.setHeight("25px");
								toolbar.setParent(south);
								MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
								cancel.setTooltiptext("Tutup");
								cancel.addEventListener("onClick", new EventListener() {
									@Override
									public void onEvent(Event event) throws Exception {
										window.detach();
									}
								});
								cancel.setParent(toolbar);

								MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Download Data",
										"/img/excel.png");
								print.addEventListener("onClick", new EventListener() {
									@Override
									public void onEvent(Event event) throws Exception {

										try {
											Filedownload.save(new FileInputStream(file),
													"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
													file.getName());
										} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PendaftarBeasiswaHelper.java:379");

										}
									}
								});
								print.setParent(toolbar);

								window.setVisible(true);
								window.onModal();

								Clients.clearBusy();
								timer.detach();
							}

						} catch (Exception e) {
							Clients.clearBusy();
						}

					}
				});
				timer.start();

				try {

					Clients.showBusy(label.getValue());

					new Thread(new Runnable() {

						@Override
						public void run() {

							try {
								Object d = dataCriteria == null ? null : dataCriteria.initCriteria(true);
								@SuppressWarnings("rawtypes")
								List<MahasiswaDaftarBeasiswa> data = (d != null && d instanceof Criteria)
										? ((Criteria) d).setMaxResults(1048576).list()
										: (List) d;
								intbox.setValue(data.size());
								System.out.println("data = " + data.size());

								XSSFWorkbook workbook = new XSSFWorkbook();

								XSSFCellStyle lockedNumericStyle = workbook.createCellStyle();
								lockedNumericStyle.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
								lockedNumericStyle.setFillForegroundColor(new XSSFColor(Color.LIGHT_GRAY));
								// lockedNumericStyle.setLocked(true);

								XSSFFont hlink_font = workbook.createFont();
								hlink_font.setUnderline(XSSFFont.U_SINGLE);
								hlink_font.setColor(new XSSFColor(Color.BLUE));

								XSSFCellStyle hlink_style = workbook.createCellStyle();
								hlink_style.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
								hlink_style.setFillForegroundColor(new XSSFColor(Color.LIGHT_GRAY));
								hlink_style.setFont(hlink_font);

								XSSFCellStyle notLocked = workbook.createCellStyle();
								notLocked.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
								notLocked.setFillForegroundColor(new XSSFColor(Color.LIGHT_GRAY));
								// notLocked.setLocked(false);

								XSSFSheet sheet = workbook.createSheet("CETAK DATA");
								// sheet.protectSheet("passwordrahasia");
								sheet.setDefaultColumnWidth(20);
								int rowIndex = 0;

								XSSFRow rowhead = sheet.createRow((short) 0);

								rowhead.createCell(0).setCellValue("ID");

								rowhead.createCell(1).setCellValue("NIM");
								rowhead.createCell(2).setCellValue("Nama");
								rowhead.createCell(3).setCellValue("Jurusan");
								rowhead.createCell(4).setCellValue("Fakultas");
								rowhead.createCell(5).setCellValue("Diterima");
								rowhead.createCell(6).setCellValue("Skor");
								rowhead.createCell(7).setCellValue("IPK");

								for (int i = 8; i < persyaratanBeasiswas.size() + 8; i++) {
									PersyaratanBeasiswa persyaratanBeasiswa = persyaratanBeasiswas.get(i - 8);
									if (persyaratanBeasiswa.getLabelInputan() == null
											|| persyaratanBeasiswa.getLabelInputan().trim().isEmpty()) {
										rowhead.createCell(i).setCellValue(persyaratanBeasiswa.getNama());
									} else {
										rowhead.createCell(i).setCellValue(persyaratanBeasiswa.getLabelInputan());

									}
								}

								for (MahasiswaDaftarBeasiswa o : data) {

									try {
										rowIndex++;
										if (o == null) {
											continue;
										}
										Mahasiswa mahasiswa = o.getMahasiswa();
										label.setValue("Sedang memproses data " + o.toString() + " ("
												+ Common.numberFormat.get().format(rowIndex * 100.0 / data.size())
												+ " %)");

										XSSFRow row = sheet.createRow(rowIndex);
										XSSFCell cell = row.createCell(0);
										cell.setCellStyle(lockedNumericStyle);
										cell.setCellValue(o.getId());

										cell = row.createCell(1);
										cell.setCellStyle(lockedNumericStyle);
										cell.setCellValue(mahasiswa.getNim());

										cell = row.createCell(2);
										cell.setCellStyle(lockedNumericStyle);
										cell.setCellValue(mahasiswa.getNama());

										cell = row.createCell(3);
										cell.setCellStyle(lockedNumericStyle);
										cell.setCellValue(mahasiswa.getJurusan().getNama());

										cell = row.createCell(4);
										cell.setCellStyle(lockedNumericStyle);
										cell.setCellValue(mahasiswa.getJurusan().getFakultas().getNama());

										cell = row.createCell(5);
										cell.setCellStyle(notLocked);
										cell.setCellValue(o.getTerima().equals(1));

										cell = row.createCell(6);
										cell.setCellStyle(lockedNumericStyle);
										cell.setCellValue(o.getTotalSkor());

										KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa);

										cell = row.createCell(7);
										cell.setCellStyle(lockedNumericStyle);
										cell.setCellValue(krsMahasiswa.getIpk());

										Session session = HibernateUtil.currentSession();
										for (int i = 8; i < persyaratanBeasiswas.size() + 8; i++) {

											try {
												PersyaratanBeasiswa persyaratanBeasiswa = persyaratanBeasiswas
														.get(i - 8);
												MahasiswaBeasiswaPersyaratan mahasiswaBeasiswaPersyaratan = (MahasiswaBeasiswaPersyaratan) session
														.createCriteria(MahasiswaBeasiswaPersyaratan.class)
														.add(Restrictions.eq("mahasiswa", mahasiswa))
														.add(Restrictions.eq("beasiswa", beasiswa)).add(Restrictions
																.eq("persyaratanBeasiswa", persyaratanBeasiswa))
														.uniqueResult();
												if (mahasiswaBeasiswaPersyaratan == null) {
													mahasiswaBeasiswaPersyaratan = new MahasiswaBeasiswaPersyaratan();
													mahasiswaBeasiswaPersyaratan.setMahasiswa(mahasiswa);
													mahasiswaBeasiswaPersyaratan.setBeasiswa(beasiswa);
													mahasiswaBeasiswaPersyaratan
															.setPersyaratanBeasiswa(persyaratanBeasiswa);
													session.save(mahasiswaBeasiswaPersyaratan);
												}

												if (persyaratanBeasiswa.getTipeDataInputan()
														.equals(PersyaratanBeasiswa.TEXT)
														|| persyaratanBeasiswa.getTipeDataInputan()
																.equals(PersyaratanBeasiswa.TEXT_ANGKA)) {
													cell = row.createCell(i);
													cell.setCellStyle(lockedNumericStyle);
													if (mahasiswaBeasiswaPersyaratan.getNilaiString() != null) {
														cell.setCellValue(
																mahasiswaBeasiswaPersyaratan.getNilaiString());
													}
												} else if (persyaratanBeasiswa.getTipeDataInputan()
														.equals(PersyaratanBeasiswa.TANGGAL)) {
													cell = row.createCell(i);
													cell.setCellStyle(lockedNumericStyle);
													cell.setCellValue(
															mahasiswaBeasiswaPersyaratan.getNilaiTanggal() == null ? ""
																	: Common.dateFormat1.get()
																			.format(mahasiswaBeasiswaPersyaratan
																					.getNilaiTanggal()));

												} else if (persyaratanBeasiswa.getTipeDataInputan()
														.equals(PersyaratanBeasiswa.ANGKA)) {
													cell = row.createCell(i);
													cell.setCellStyle(lockedNumericStyle);
													if (mahasiswaBeasiswaPersyaratan.getNilaiNumber() != null) {
														cell.setCellValue(
																mahasiswaBeasiswaPersyaratan.getNilaiNumber());
													}

												} else if (persyaratanBeasiswa.getTipeDataInputan()
														.equals(PersyaratanBeasiswa.PILIHAN_YA_TIDAK)) {
													cell = row.createCell(i);
													cell.setCellStyle(lockedNumericStyle);
													if (mahasiswaBeasiswaPersyaratan.getNilaiBoolean() != null) {
														cell.setCellValue(
																mahasiswaBeasiswaPersyaratan.getNilaiBoolean());
													}

												} else if (persyaratanBeasiswa.getTipeDataInputan()
														.equals(PersyaratanBeasiswa.PILIHAN_CUSTOM)) {
													cell = row.createCell(i);
													cell.setCellStyle(lockedNumericStyle);
													if (mahasiswaBeasiswaPersyaratan.getNilaiString() != null) {
														cell.setCellValue(
																mahasiswaBeasiswaPersyaratan.getNilaiString());
													}

												} else {
													cell = row.createCell(i);
													if (persyaratanBeasiswa.getLabelInputan() == null
															|| persyaratanBeasiswa.getLabelInputan().trim().isEmpty()) {
														cell.setCellValue(persyaratanBeasiswa.getNama());
													} else {
														cell.setCellValue(persyaratanBeasiswa.getLabelInputan());

													}
												}

												if (persyaratanBeasiswa.getHarusMenyertakanLampiran()) {
													cell.setCellStyle(hlink_style);
													try {
														Session streamingSession = StreamingHibernateUtil.getInstance()
																.currentSession();

														int jumlah = ((Number) streamingSession
																.createCriteria(LampiranBeasiswaMahasiswa.class)
																.setProjection(Projections.rowCount())
																.add(Restrictions.eq("persyaratanBeasiswa",
																		mahasiswaBeasiswaPersyaratan.getId()))
																.setMaxResults(1).uniqueResult()).intValue();

														Long ids = (Long) (streamingSession
																.createCriteria(LampiranBeasiswaMahasiswa.class)
																.setProjection(Projections.property("id"))
																.add(Restrictions.eq("persyaratanBeasiswa",
																		mahasiswaBeasiswaPersyaratan.getId()))
																.setMaxResults(1).uniqueResult());

														String url = CommonMedia.getFile(ids,
																LampiranBeasiswaMahasiswa.class.getName());

														if (jumlah > 0) {
															XSSFHyperlink link = row.getSheet().getWorkbook()
																	.getCreationHelper()
																	.createHyperlink(Hyperlink.LINK_URL);
															link.setAddress(url);
															cell.setHyperlink(link);
														}

													} catch (Exception e) {
														StreamingHibernateUtil.getInstance().rollbackTransaction();
													}
													StreamingHibernateUtil.getInstance().closeSession();
												}
											} catch (Exception e) {
												Common.tampilErrorJikaAdmin(e);
											}
										}

									} catch (Exception e) {
										Common.tampilErrorJikaAdmin(e);
									}
								}

								try {
									FileOutputStream fileOut = new FileOutputStream(filename);
									workbook.write(fileOut);
									fileOut.close();
								} catch (IOException e) {
									// TODO Auto-generated catch block
									Common.tampilErrorJikaAdmin(e);
								}
								System.out.println("Your excel file has been generated! ");
								data.clear();
								data = null;
								label.setValue("");
							} catch (Exception e) {
								label.setValue("-");
							}

						}
					}).start();

				} catch (Exception e) {
					// TODO Auto-generated catch block
					Common.tampilErrorJikaAdmin(e);
					PesanFormalHelper.tampilkanGagalException(
							"mencetak/mengekspor data pendaftar beasiswa ke Excel",
							e, new String[] {
									"Muat ulang (refresh) halaman ini lalu coba cetak data kembali.",
									"Periksa apakah jumlah data yang akan diekspor tidak terlalu besar.",
									"Apabila kendala masih berlanjut, hubungi Admin dengan menyertakan tangkapan layar (screenshot) pesan ini."
							});
				}
			}
		});

		return toolbarbutton;
	}

	/**
	 * Titik masuk utama: membangun seluruh UI daftar pendaftar untuk {@code beasiswa} di dalam
	 * {@code component} — toolbar filter (NIM/nama, jenjang, fakultas, jurusan, angkatan, status
	 * belum diterima), tombol aksi (cari, cetak Pendaftar/Penerima/Rekap, Hitung Skor, Baru,
	 * Download/Upload Excel), dan grid berpaging hasil {@link #loadData(Object)}.
	 *
	 * @param beasiswa  beasiswa yang daftar pendaftarnya ditampilkan
	 * @param component komponen induk tempat UI ditempel (dibersihkan lebih dulu)
	 * @param window    window pembungkus (diteruskan ke dialog "Baru" agar dapat menutup diri sendiri)
	 * @param approve   {@code true} untuk mengaktifkan checkbox terima/tolak pada tiap baris (mode approval)
	 */
	public void displayPrasyaratBeasiswa(final Beasiswa beasiswa, final Component component, final MyWindow window,
			final boolean approve) {
		this.beasiswa = beasiswa;
		this.approve = approve;
		Common.clear(component);

		paging = new Paging();
		Common.initPaging50(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});

		Common.initFakultasDanJurusanDanSemua(fakultas = new Combobox(), jurusan = new Combobox(), null, null);

		ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
		groupbox.setStyle("min-height: 200px;");
		groupbox.setParent(component);
		groupbox.appendChild(new MyCaptionStyled("Daftar mahasiswa yang mendaftar beasiswa"));

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(groupbox);
		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Mhs : ")));
		toolbar.appendChild(nim = new Textbox());
		nim.setWidth("");
		nim.setWidth("70px");

		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Jenjang")));
		jenjang = new Combobox();
		Common.insertComboDanSemua(jenjang, "nama", "keterangan", Jenjang.class, Restrictions.eq("aktif", true));
		toolbar.appendChild(jenjang);
		jenjang.setWidth("70px");

		jenjang.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});

		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Fakultas")));
		toolbar.appendChild(fakultas);
		fakultas.setWidth("70px");

		fakultas.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});

		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Jurusan")));
		toolbar.appendChild(jurusan);
		jurusan.setWidth("70px");

		jurusan.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});

		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Angkatan")));
		toolbar.appendChild(angkatan = new Intbox());
		angkatan.setWidth("50px");

		toolbar.appendChild(hanyaYgBelumDiterima = new MyCheckboxConfig("Belum diterima"));
		hanyaYgBelumDiterima.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}

		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Pendaftar", "/img/print.png");
		button.addEventListener("onClick", new EventListener() {
			@SuppressWarnings("rawtypes")
			final Map parameters = ais.common.HashMapGenerator.getRand();

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event arg0) throws Exception {

				Integer countPendaftar = ((Number) HibernateUtil.currentSession()
						.createCriteria(MahasiswaDaftarBeasiswa.class).add(Restrictions.eq("beasiswa", beasiswa))
						.setProjection(Projections.rowCount()).uniqueResult()).intValue();

				if (countPendaftar == 0) {
					MyMessageboxConfig.show("Tidak Ada Pendaftar", "Informasi", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return;
				}

				parameters.put("id_beasiswa", beasiswa.getId());
				// parameters
				// .put("jurusan", fakultas.getSelectedItem().getValue());
				// parameters.put("fakultas", fakultas.getSelectedItem()
				// .getValue());
				Report.generatePDFReport(Report.PDF, parameters, "pendaftar_beasiswa", ais.ui.util.WaktuUtil.getDate());
			}

		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Penerima", "/img/print.png");
		button.addEventListener("onClick", new EventListener() {
			@SuppressWarnings("rawtypes")
			final Map parameters = ais.common.HashMapGenerator.getRand();

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event arg0) throws Exception {

				Integer countPenerima = ((Number) HibernateUtil.currentSession()
						.createCriteria(MahasiswaDaftarBeasiswa.class).add(Restrictions.eq("beasiswa", beasiswa))
						.add(Restrictions.eq("terima", 1)).setProjection(Projections.rowCount()).uniqueResult())
						.intValue();

				if (countPenerima == 0) {
					MyMessageboxConfig.show("Tidak Ada Mahasiswa yang Diterima di Beasiswa Ini", "Informasi",
							MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
					return;
				}

				parameters.put("id_beasiswa", beasiswa.getId());
				Report.generatePDFReport(Report.PDF, parameters, "pendaftar_beasiswa_diterima",
						ais.ui.util.WaktuUtil.getDate());
			}

		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Rekap", "/img/print.png");
		button.addEventListener("onClick", new EventListener() {
			final HashMap<String, Long> parameters = new HashMap<String, Long>();

			@Override
			public void onEvent(Event arg0) throws Exception {

				Integer countPenerima = ((Number) HibernateUtil.currentSession()
						.createCriteria(MahasiswaDaftarBeasiswa.class).add(Restrictions.eq("beasiswa", beasiswa))
						.add(Restrictions.eq("terima", 1)).setProjection(Projections.rowCount()).uniqueResult())
						.intValue();

				if (countPenerima == 0) {
					MyMessageboxConfig.show("Tidak Ada Mahasiswa yang Diterima di Beasiswa Ini", "Informasi",
							MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
					return;
				}

				parameters.put("id_beasiswa", beasiswa.getId());
				Report.generatePDFReport(Report.PDF, parameters, "penerima-beasiswa", ais.ui.util.WaktuUtil.getDate());
			}

		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Hitung Skor", "/img/excel.png");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Common.createDefaultTimer(new EventListener() {

					@SuppressWarnings("unchecked")
					@Override
					public void onEvent(Event arg0) throws Exception {
						List<MahasiswaDaftarBeasiswa> mahasiswaDaftarBeasiswas = initCriteria(true).list();
						Session session = HibernateUtil.currentSession();
						for (MahasiswaDaftarBeasiswa mahasiswaDaftarBeasiswa : mahasiswaDaftarBeasiswas) {

							List<MahasiswaBeasiswaPersyaratan> mahasiswaBeasiswaPersyaratans = session
									.createCriteria(MahasiswaBeasiswaPersyaratan.class)
									.add(Restrictions.eq("mahasiswa", mahasiswaDaftarBeasiswa.getMahasiswa()))
									.add(Restrictions.eq("beasiswa", mahasiswaDaftarBeasiswa.getBeasiswa()))
									.createAlias("persyaratanBeasiswa", "persyaratanBeasiswa")
									.add(Restrictions.eq("persyaratanBeasiswa.tipeDataInputan",
											PersyaratanBeasiswa.PILIHAN_CUSTOM))
									.list();
							Integer totalSkor = 0;
							for (MahasiswaBeasiswaPersyaratan mahasiswaBeasiswaPersyaratan : mahasiswaBeasiswaPersyaratans) {
								String val = mahasiswaBeasiswaPersyaratan.getNilaiString() == null ? ""
										: mahasiswaBeasiswaPersyaratan.getNilaiString().trim();
								String[] kol = StringUtils.split(val, ":");
								Integer skor = 0;
								try {
									skor = Integer.parseInt(kol[1].trim());
								} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PendaftarBeasiswaHelper.java:875");

								}
								totalSkor += skor;
							}
							mahasiswaDaftarBeasiswa.setTotalSkor(totalSkor);

							Common.refreshSaveOrUpdate(session, mahasiswaDaftarBeasiswa);
						}
						loadData(null);
					}
				});
			}

		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Baru", "/img/new.gif");
		final AmbilDataMahasiswaSeleksiBeasiswaHelper ambilDataMahasiswaSeleksiBeasiswaHelper = new AmbilDataMahasiswaSeleksiBeasiswaHelper();
		button.addEventListener(Events.ON_CLICK, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				// TODO Auto-generated method stub
				ambilDataMahasiswaSeleksiBeasiswaHelper.display(beasiswa, getDataloader(), window);
			}
		});
		button.setParent(toolbar);

		MyToolbarbuttonConfig cetakToolbarbutton = cetakDataCustomButton(this, "Download", "/img/excel.png");
		toolbar.appendChild(cetakToolbarbutton);

		MyToolbarbuttonConfig upload = new MyToolbarbuttonConfig("Upload" + Common.ukuranLabelFileUpload(),
				"/img/excel.png");
		upload.setUpload(Common.ukuranFileUpload());
		upload.addEventListener("onUpload", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				UploadEvent uploadEvent = (UploadEvent) event;
				Media media = uploadEvent.getMedia();
				if (!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media))
					return;
				if (media.getName().toLowerCase().endsWith("xlsx")) {

					InputStream inputStream = media.getStreamData();
					// System.out.println("media = " + media);
					final File file = new File(
							Sessions.getCurrent().getWebApp().getRealPath("/temp/" + media.getName()));
					// System.out.println("file = " + file.getAbsolutePath());
					file.getParentFile().mkdirs();
					FileOutputStream fileOutputStream = new FileOutputStream(file);
					int c;
					while ((c = inputStream.read()) != -1) {
						fileOutputStream.write(c);
					}
					fileOutputStream.close();
					inputStream.close();

					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							final Label peringatan = new Label("");

							final Label label = new Label(ais.common.Common.getBahasaConfig("Proses upload data data .."));
							Clients.showBusy(label.getValue());
							final Timer timer = new Timer(200);
							timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
							timer.setRepeats(true);
							timer.addEventListener("onTimer", new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									Clients.showBusy(label.getValue());
									if (label.getValue().isEmpty()) {
										System.out.println("loading file " + file.getAbsolutePath());
										MyMessageboxConfig.show("Upload data berhasil dilakukan."
												+ (peringatan.getValue().isEmpty() ? "" : "\n" + peringatan.getValue()),
												"Pemberitahuan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
												new EventListener() {

													@Override
													public void onEvent(Event arg0) throws Exception {
														loadData(null);
													}
												});
										Clients.clearBusy();
										timer.detach();
									}

								}
							});
							timer.start();

							new Thread(new Runnable() {

								@Override
								public void run() {
									try {

									try {

										XSSFWorkbook workbook = new XSSFWorkbook(file.getAbsolutePath());
										XSSFSheet sheet = workbook.getSheetAt(0);

										Session session = HibernateUtil.currentNativeSession();
										int rowCount = (sheet.getLastRowNum() + 1);
										int jumlahDiproses = 0;
										int jumlahDiterima = 0;
										int jumlahTidakDitemukan = 0;
										int jumlahGagal = 0;
										for (int i = 1; i < rowCount; i++) {
											try {

												// FIX "Session is closed!" pada baris ke-N: bila baris sebelumnya gagal
												// commit (mis. constraint/lock), Hibernate dapat menutup Session ini
												// sendiri (fatal JDBCException) padahal loop masih memakai referensi
												// `session` yang sama utk semua baris. Ambil ulang referensi tiap
												// iterasi -- currentNativeSession() sudah self-heal (baca dok di
												// MenuHelper.ensureKantinMenus) sehingga baris berikutnya tetap dapat
												// diproses alih-alih ikut gagal karena Session basi.
												if (!session.isOpen()) {
													session = HibernateUtil.currentNativeSession();
												}

												Long id = Common.getSheetContentAsLong(sheet, 0, i);
												String nimExcel = Common.getSheetContentAsString(sheet, 1, i);

												Mahasiswa mahasiswa = (Mahasiswa) Common.getSheetContentAsObject(sheet,
														1, i, Mahasiswa.class);
												if (mahasiswa == null) {
													mahasiswa = cariMahasiswaDariNim(session, nimExcel);
												}
												System.out.println("mahasiswa -> " + mahasiswa + ", id -> " + id
														+ ", nim -> " + nimExcel);
												if (id == null && mahasiswa == null) {
													jumlahTidakDitemukan++;
													continue;
												}

												boolean diterima = Boolean.TRUE.equals(Common.getSheetContentAsBoolean(sheet, 5, i));

												MahasiswaDaftarBeasiswa mahasiswaDaftarBeasiswa = cariPendaftarBeasiswa(
														session, id, mahasiswa, beasiswa);

												if (mahasiswaDaftarBeasiswa == null && mahasiswa != null) {
													mahasiswaDaftarBeasiswa = new MahasiswaDaftarBeasiswa();
													mahasiswaDaftarBeasiswa.setBeasiswa(beasiswa);
													mahasiswaDaftarBeasiswa.setMahasiswa(mahasiswa);
												}

												if (mahasiswaDaftarBeasiswa != null) {
													mahasiswaDaftarBeasiswa.setTerima(diterima ? 1 : 0);

													boolean memenuhiSyarat = Common
															.checkApakahMemenuhiSyaratBeasiswa(mahasiswaDaftarBeasiswa);
													mahasiswaDaftarBeasiswa.setMemenuhiSyarat(memenuhiSyarat);

													Transaction tx = null;
													try {
														tx = session.beginTransaction();
														session.saveOrUpdate(mahasiswaDaftarBeasiswa);
														tx.commit();
													} catch (Exception simpan) {
														if (tx != null && tx.isActive()) {
															tx.rollback();
														}
														throw simpan;
													}
													jumlahDiproses++;
													if (diterima) {
														jumlahDiterima++;
													}

													label.setValue("Upload data \"" + mahasiswaDaftarBeasiswa.getNama()
															+ "\" ("
															+ Common.numberFormat.get().format(i * 100.0 / rowCount)
															+ " %)");
												} else {
													jumlahTidakDitemukan++;
												}

											} catch (Exception e) {
												jumlahGagal++;
												Common.tampilErrorJikaAdmin(e);
											}

										}
										peringatan.setValue("Ringkasan: diproses " + jumlahDiproses + " baris, diterima "
												+ jumlahDiterima + " baris, tidak ditemukan " + jumlahTidakDitemukan
												+ " baris, gagal " + jumlahGagal + " baris.");
									} catch (Exception e1) {
										// TODO Auto-generated catch block
										e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/helper/PendaftarBeasiswaHelper.java:1031");
									}

									HibernateUtil.closeSession();

									label.setValue("");
																	} finally {
										ais.database.hibernate.HibernateUtil.closeSession();
									}
								}
							}).start();

						}
					}, "Harap tunggu.. sedang melakukan proses upload data..");

				} else {
					MyMessageboxConfig.show(
							"File yang anda upload harus ber-format Excel Open XML Spreadsheet (xlsx). Jika masih menggunakan format lain, buka file excel tersebut, kemudian Save As Excel Open XML Spreadsheet (xlsx). "
									+ media,
							"Error", MyMessageboxConfig.OK, MyMessageboxConfig.ERROR);
				}
			}
		});
		toolbar.appendChild(upload);

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(50);
		grid.getPagingChild().setMold("os");
		grid.setParent(groupbox);

		paging.setParent(groupbox);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("NIM");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jurusan");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("SKS/SKSK");
		column.setWidth("7%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("IP/IPk");
		column.setWidth("7%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Skor");
		column.setWidth("5%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Memenuhi Syarat");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Terima/Tidak");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Ubah/Hapus");
		column.setWidth("10%");

		loadData(null);
		// borderlayout.setParent(component);

	}

	/**
	 * Mencari record {@link MahasiswaDaftarBeasiswa} yang sudah ada untuk upload Excel: dicoba
	 * lebih dulu berdasarkan {@code id} baris (bila kolom ID di Excel terisi dan cocok dengan
	 * {@code beasiswa}), lalu jatuh ke pencarian berdasarkan pasangan (mahasiswa, beasiswa) bila
	 * id tidak ditemukan/tidak diisi.
	 *
	 * @return record yang ditemukan, atau {@code null} bila belum ada pendaftaran sebelumnya
	 */
	private MahasiswaDaftarBeasiswa cariPendaftarBeasiswa(Session session, Long id, Mahasiswa mahasiswa,
			Beasiswa beasiswa) {
		MahasiswaDaftarBeasiswa hasil = null;
		if (session == null || beasiswa == null) {
			return null;
		}
		if (id != null) {
			hasil = (MahasiswaDaftarBeasiswa) session.createCriteria(MahasiswaDaftarBeasiswa.class)
					.add(Restrictions.idEq(id)).add(Restrictions.eq("beasiswa", beasiswa)).uniqueResult();
			if (hasil != null) {
				return hasil;
			}
		}
		if (mahasiswa != null) {
			hasil = (MahasiswaDaftarBeasiswa) session.createCriteria(MahasiswaDaftarBeasiswa.class)
					.add(Restrictions.eq("beasiswa", beasiswa)).add(Restrictions.eq("mahasiswa", mahasiswa))
					.uniqueResult();
		}
		return hasil;
	}

	/** Mencari {@link Mahasiswa} berdasarkan NIM persis (trim), dipakai sebagai fallback upload Excel saat kolom mahasiswa tidak dapat diresolusi langsung dari sel. */
	private Mahasiswa cariMahasiswaDariNim(Session session, String nimExcel) {
		if (session == null || nimExcel == null || nimExcel.trim().isEmpty()) {
			return null;
		}
		return (Mahasiswa) session.createCriteria(Mahasiswa.class)
				.add(Restrictions.eq("nim", nimExcel.trim())).uniqueResult();
	}

	/**
	 * Menetapkan status terima/tolak pendaftaran mahasiswa terbaru pada {@code beasiswa} tertentu
	 * (baris terbaru dipilih via {@code Order.desc("id")}), lalu menampilkan pesan konfirmasi ke
	 * pengguna. Dipakai sebagai aksi cepat di luar grid utama (mis. dari layar lain yang hanya
	 * perlu mengubah satu status tanpa membuka daftar penuh).
	 *
	 * @param mahasiswa mahasiswa yang statusnya diubah
	 * @param beasiswa  beasiswa terkait
	 * @param checked   {@code true} untuk menerima, {@code false} untuk menolak
	 */
	public void terimaBeasiswa(Mahasiswa mahasiswa, Beasiswa beasiswa, boolean checked) throws Exception {
		Session session = HibernateUtil.currentSession();
		MahasiswaDaftarBeasiswa mahasiswaDiterimaBeasiswaIni = (MahasiswaDaftarBeasiswa) session
				.createCriteria(MahasiswaDaftarBeasiswa.class).add(Restrictions.eq("mahasiswa", mahasiswa))
				.add(Restrictions.eq("beasiswa", beasiswa)).setMaxResults(1).addOrder(Order.desc("id")).uniqueResult();

		if (checked) {

			mahasiswaDiterimaBeasiswaIni.setTerima(1);
			Common.refreshUpdate(session, mahasiswaDiterimaBeasiswaIni);
			MyMessageboxConfig.show(
					"Mahasiswa " + mahasiswa.getNama() + " / " + mahasiswa.getNim() + " diterima untuk beasiswa "
							+ beasiswa.getNama() + " / " + beasiswa.getTahun(),
					"INFORMASI", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return;

		}

		if (!checked) {
			mahasiswaDiterimaBeasiswaIni.setTerima(0);
			Common.refreshUpdate(session, mahasiswaDiterimaBeasiswaIni);
			MyMessageboxConfig.show(
					"Mahasiswa " + mahasiswa.getNama() + " / " + mahasiswa.getNim() + " ditolak untuk beasiswa "
							+ beasiswa.getNama() + " / " + beasiswa.getTahun(),
					"INFORMASI", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return;
		}

	}

}
