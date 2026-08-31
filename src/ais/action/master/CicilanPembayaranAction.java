package ais.action.master;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zss.ui.Spreadsheet;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Decimalbox;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Html;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.PembayaranUtilHelper;
import ais.action.master.helper.RevisiCicilanPembayaranHelper;
import ais.action.master.helper.RevisiHelper;
import ais.action.report.CommonReportHelper;
import ais.action.ws.util.PembayaranUtil;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.CicilanPembayaran;
import ais.database.model.DetailBiaya;
import ais.database.model.DetailKegiatan;
import ais.database.model.DetailSettingBiaya;
import ais.database.model.Fakultas;
import ais.database.model.GeneralValueObject;
import ais.database.model.ItemBiaya;
import ais.database.model.Jenjang;
import ais.database.model.Jurusan;
import ais.database.model.Kegiatan;
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;
import ais.database.model.PengaturanPembayaranBulanan;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.VOMahasiswa;
import ais.ui.util.DataCriteria;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;

/**
 * Controller/action ZK untuk cicilan pembayaran. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Html dashboardHtml}, {@code Html
 * progressHtml}, {@code MyGrid grid}, {@code Paging paging}, {@code Textbox searchnama}, {@code Combobox
 * searchfakultas}, {@code Combobox jenissemester}, {@code Combobox searchjurusan}; inisialisasi/lifecycle
 * ({@code doBeforeCompose()}, {@code doAfterCompose()}, {@code initCriteria()}); pembacaan/pencarian ({@code
 * onSearchDefault()}, {@code refreshDashboardAman()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau
 * interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 * <p><b>Lifecycle:</b> instance mengikuti lifecycle komponen ZK dan menyimpan state layar; jangan digunakan
 * sebagai singleton atau dibagikan antar desktop/session. Event handler harus tetap memakai konteks pengguna
 * serta session Hibernate milik request yang aktif.</p>
 *
 * @see GenericAutowireComposer
 */
public class CicilanPembayaranAction extends GenericAutowireComposer implements DataCriteria {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730217402400328L;

	private Html dashboardHtml;
	private Html progressHtml;

	private MyGrid grid;

	private Paging paging;

	private Textbox searchnama;
	private Combobox searchfakultas;
	private Combobox jenissemester;
	private Combobox searchjurusan;
	private Decimalbox searchtahun;
	private Combobox ta;
	private Combobox searchjenjang;
	private MyDatebox start;
	private MyDatebox end;
	private List<Checkbox> mapItemBiaya = new ArrayList<Checkbox>();
	private MyToolbarbuttonConfig find;
	private boolean tampilkanTanggalKwitansi = false;

	// private Row filterPendukung;
	private North mynorth;

	private MyColumnConfig idTanggal;
	private boolean deposit = false;
	private Tbmuser tbmuser;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	@SuppressWarnings("unchecked")
	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		Common.initLaguage();
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		tbmuser = Common.getCurrentUser();
		if (tbmuser != null && tbmuser.getMahasiswa() != null) {
			searchnama.setValue(tbmuser.getMahasiswa().getNim());
			searchnama.setDisabled(true);

			if (mynorth != null) {
				mynorth.setVisible(false);
			}
		} else if (tbmuser != null && tbmuser.getBiodataCalonMahasiswa() != null) {
			searchnama.setValue(tbmuser.getBiodataCalonMahasiswa().getNoRegistrasi());
			searchnama.setDisabled(true);

			if (mynorth != null) {
				mynorth.setVisible(false);
			}
		}

		Common.generateTahunAjaranDanSemua(ta);
		Common.selectComboItem(ta, Common.getCurrentTahunAkademik());

		tampilkanTanggalKwitansi = Common.bolehKonfigurasi("tampilkan_tanggal_kwitansi_di_pembayaran", Konfigurasi.TIDAK_AKTIF);

		if (idTanggal != null && tampilkanTanggalKwitansi) {
			idTanggal.setLabel("Tgl.Bayar/Tgl.Kwitansi");
		}

		deposit = execution.getParameter("deposit") == null ? false : true;

		if (start != null) start.setReadonly(true);
		if (end != null) end.setReadonly(true);

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.WEEK_OF_MONTH, calendar.get(Calendar.WEEK_OF_MONTH) - 3);
		if (start != null) start.setValue(calendar.getTime());
		calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1);
		if (end != null) end.setValue(calendar.getTime());

		Common.insertComboDanSemua(searchjenjang, "nama", Jenjang.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		MyComboitemConfig comboitem = new MyComboitemConfig(Perkuliahan.GANJIL);
		if (comboitem != null) { comboitem.setValue(Perkuliahan.GANJIL); }
		jenissemester.appendChild(comboitem);
		comboitem = new MyComboitemConfig(Perkuliahan.GENAP);
		if (comboitem != null) { comboitem.setValue(Perkuliahan.GENAP); }
		jenissemester.appendChild(comboitem);

		comboitem = new MyComboitemConfig("Semua");
		if (comboitem != null) { comboitem.setValue(null); }
		jenissemester.appendChild(comboitem);
		if (jenissemester != null) { jenissemester.setSelectedItem(comboitem); }
		if (jenissemester != null) { jenissemester.setReadonly(true); }

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

		onSearchDefault(null);

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(CicilanPembayaran.class, this,
				"kegiatan.mahasiswa.nim||kegiatan.calonMahasiswa.noRegistrasi",
				"kegiatan.mahasiswa.nama||kegiatan.calonMahasiswa.nama",
				"kegiatan.mahasiswa.jurusan.nama||kegiatan.calonMahasiswa.prodiLulus.nama||kegiatan.calonMahasiswa.prodi1.nama",
				"jenisPembayaran.nama", "nilai", "denda", "deposit", "tanggal", "tanggalKwitansi", "itemBiaya",
				"detailBiaya", "keterangan");
		Common.appendKeToolbar(cetakToolbarbutton, find, comp);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("History", "/img/jadwal.png");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				RevisiCicilanPembayaranHelper revisiHelper = new RevisiCicilanPembayaranHelper(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								onSearchDefault(arg0);
							}
						});
					}
				});
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(revisiHelper);
				revisiHelper.setVisible(true);
				revisiHelper.onModal();

			}

		});
		if (button != null) { button.setParent(find.getParent()); }

		MyToolbarbuttonConfig generatePasswordMahasiswa = new MyToolbarbuttonConfig("Download Rekap Pembayaran",
				"/img/excel.png");
		generatePasswordMahasiswa.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				final String filename = Sessions.getCurrent().getWebApp().getRealPath("/tmp/pembayaran_mahasiswa_"
						+ URLEncoder.encode(Common.datetimeFormat2s.get().format(ais.ui.util.WaktuUtil.getDate()), "UTF-8")
						+ ".xlsx");
				final File file = new File(filename);
				final Label label = new Label(ais.common.Common.getBahasaConfig("Proses load data .."));
				final Intbox intbox = new Intbox(10);
				final Intbox colS = new Intbox(10);
				Clients.showBusy(label.getValue());

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

								spreadsheet.setMaxrows(intbox.getValue() + 3);
								spreadsheet.setMaxcolumns(colS.getValue());
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
										} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
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

				Clients.showBusy(label.getValue());

				new Thread(new Runnable() {

					@SuppressWarnings({})
					@Override
					public void run() {

						List<CicilanPembayaran> cicilanPembayarans = initCriteria(true).setMaxResults(1048576).list();

						XSSFWorkbook workbook = new XSSFWorkbook();
						XSSFSheet sheet = workbook.createSheet("Rekap Pembayaran");
						sheet.setDefaultColumnWidth(20);
						int rowIndex = 0;

						XSSFRow rowhead = sheet.createRow((short) 0);
						rowhead.createCell(0).setCellValue("NIM/No.Reg");
						rowhead.createCell(1).setCellValue("Nama");
						rowhead.createCell(2).setCellValue("Fakultas");
						rowhead.createCell(3).setCellValue("Prodi");
						rowhead.createCell(4).setCellValue("Semester");
						rowhead.createCell(5).setCellValue("Jenis Pembayaran");
						rowhead.createCell(6).setCellValue("Item Biaya");
						rowhead.createCell(7).setCellValue("Bulan");
						rowhead.createCell(8).setCellValue("Nominal");
						rowhead.createCell(9).setCellValue("Hari/Tanggal/Waktu");
						rowhead.createCell(10).setCellValue("Keterangan");

						colS.setValue(11);

						// Long id = null;
						for (CicilanPembayaran cicilanPembayaran : cicilanPembayarans) {

							try {
								BiodataCalonMahasiswa biodataCalonMahasiswa = cicilanPembayaran.getKegiatan()
										.getCalonMahasiswa();
								Mahasiswa mahasiswa = cicilanPembayaran.getKegiatan().getMahasiswa();

								rowIndex++;

								XSSFRow row = sheet.createRow(rowIndex);

								if (mahasiswa != null) {
									row.createCell(0).setCellValue(mahasiswa.getNim());
									row.createCell(1).setCellValue(mahasiswa.getNama());

									row.createCell(2)
											.setCellValue(mahasiswa.getJurusan() == null
													|| mahasiswa.getJurusan().getFakultas() == null ? ""
															: mahasiswa.getJurusan().getFakultas().getNama());
									row.createCell(3).setCellValue(
											mahasiswa.getJurusan() == null ? "" : mahasiswa.getJurusan().getNama());
									row.createCell(4).setCellValue(cicilanPembayaran.getKegiatan().getSemster());
									row.createCell(5).setCellValue(
											cicilanPembayaran.getKegiatan().getJenisKegiatan().getNamaKegiatan());

								} else if (biodataCalonMahasiswa != null) {
									row.createCell(0).setCellValue(biodataCalonMahasiswa.getNoRegistrasi());
									row.createCell(1).setCellValue(biodataCalonMahasiswa.getNama());

									Jurusan jurusan = biodataCalonMahasiswa.getProdiLulus();
									if (jurusan == null) {
										jurusan = biodataCalonMahasiswa.getProdi1();
									}

									row.createCell(2).setCellValue(jurusan == null || jurusan.getFakultas() == null ? ""
											: jurusan.getFakultas().getNama());
									row.createCell(3).setCellValue(jurusan == null ? "" : jurusan.getNama());
									row.createCell(4).setCellValue(cicilanPembayaran.getKegiatan().getSemster());
									row.createCell(5).setCellValue(
											cicilanPembayaran.getKegiatan().getJenisKegiatan().getNamaKegiatan());
								}

								row.createCell(6).setCellValue(cicilanPembayaran.getItemBiaya().getNama());
								row.createCell(7)
										.setCellValue(cicilanPembayaran.getPengaturanPembayaranBulanan() == null ? 0
												: cicilanPembayaran.getPengaturanPembayaranBulanan().getRealBulan());
								row.createCell(8)
										.setCellValue(Common.numberFormat.get().format(cicilanPembayaran.getNilai()));
								row.createCell(9)
										.setCellValue(Common.dateFormat5.get().format(cicilanPembayaran.getTanggal()));
								row.createCell(10).setCellValue(cicilanPembayaran.getKeterangan());
							} catch (Exception e) {
								ais.common.Common.tampilErrorJikaAdmin(e);
							}
						}

						intbox.setValue(cicilanPembayarans.size());

						try {
							FileOutputStream fileOut = new FileOutputStream(filename);
							workbook.write(fileOut);
							fileOut.close();
						} catch (IOException e) {
							// TODO Auto-generated
							// catch
							// block
							Common.tampilErrorJikaAdmin(e);
						}

						label.setValue("");

						HibernateUtil.closeSession();
					}
				}).start();

			}
		});
		Common.appendKeToolbar(generatePasswordMahasiswa, find, comp);

		generatePasswordMahasiswa = new MyToolbarbuttonConfig("Download Rekap Tagihan dan Pembayaran",
				"/img/excel.png");
		generatePasswordMahasiswa.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				final List<Long> ids = new ArrayList<Long>();
				for (Checkbox checkbox : mapItemBiaya) {
					if (checkbox.isChecked()) {
						ItemBiaya itemBiaya = (ItemBiaya) checkbox.getAttribute("itemBiaya");
						ids.add(itemBiaya.getId());
					}
				}

				final String filename = Sessions.getCurrent().getWebApp()
						.getRealPath("/tmp/tagihan_dan_pembayaran_mahasiswa_" + URLEncoder.encode(
								Common.datetimeFormat2s.get().format(ais.ui.util.WaktuUtil.getDate()), "UTF-8") + ".xlsx");
				final File file = new File(filename);
				final Label label = new Label(ais.common.Common.getBahasaConfig("Proses load data .."));
				final Intbox intbox = new Intbox(10);
				final Intbox colS = new Intbox(10);
				Clients.showBusy(label.getValue());

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

								spreadsheet.setMaxrows(intbox.getValue() + 3);
								spreadsheet.setMaxcolumns(colS.getValue());
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
										} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
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

				Clients.showBusy(label.getValue());

				new Thread(new Runnable() {

					private Long proses(int rowIndex, XSSFSheet sheet, Long id, Kegiatan kegiatan,
							DetailBiaya detailBiaya, PengaturanPembayaranBulanan pengaturanPembayaranBulanan,
							List<CicilanPembayaran> cicilanPembayarans2, Collection<DetailKegiatan> detailKegiatans) {

						if (detailBiaya != null) {
							id = detailBiaya.getItemBiaya().getId();
							if (!ids.contains(id)) {
								return -1L;
							}

						} else if (pengaturanPembayaranBulanan != null) {
							id = pengaturanPembayaranBulanan.getId();
							if (!ids.contains(pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getId())) {
								return -1L;
							}

						}

						if (detailBiaya == null && pengaturanPembayaranBulanan == null) {
							return -1L;
						}

						DetailBiaya tempdetailBiaya = null;
						PengaturanPembayaranBulanan temppengaturanPembayaranBulanan = null;

						if (pengaturanPembayaranBulanan != null) {
							temppengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) pengaturanPembayaranBulanan;
							if (temppengaturanPembayaranBulanan != null) {
								tempdetailBiaya = temppengaturanPembayaranBulanan.getDetailBiaya();
							}

						}

						else if (detailBiaya != null) {
							tempdetailBiaya = (DetailBiaya) detailBiaya;
						}

						DetailKegiatan tempdata =kegiatan==null?null: temppengaturanPembayaranBulanan != null
								? kegiatan.ambilSatuDetailKegiatan(temppengaturanPembayaranBulanan, detailKegiatans)
								: kegiatan.ambilSatuDetailKegiatan(tempdetailBiaya);

						DetailKegiatan detailKegiatan = tempdata;
						if (detailKegiatan != null && detailKegiatan.getBukanTagihan()) {
							return 0L;
						}

						BiodataCalonMahasiswa biodataCalonMahasiswa = kegiatan.getCalonMahasiswa();
						Mahasiswa mahasiswa = kegiatan.getMahasiswa();
						XSSFRow row = sheet.createRow(rowIndex);

						if (mahasiswa != null) {
							row.createCell(0).setCellValue(mahasiswa.getNim());
							row.createCell(1).setCellValue(mahasiswa.getNama());

							row.createCell(2).setCellValue(
									mahasiswa.getJurusan() == null || mahasiswa.getJurusan().getFakultas() == null ? ""
											: mahasiswa.getJurusan().getFakultas().getNama());
							row.createCell(3).setCellValue(
									mahasiswa.getJurusan() == null ? "" : mahasiswa.getJurusan().getNama());
							row.createCell(5).setCellValue(kegiatan.getJenisKegiatan() == null ? ""
									: kegiatan.getJenisKegiatan().getNamaKegiatan());
						} else if (biodataCalonMahasiswa != null) {
							row.createCell(0).setCellValue(biodataCalonMahasiswa.getNoRegistrasi());
							row.createCell(1).setCellValue(biodataCalonMahasiswa.getNama());

							Jurusan jurusan = biodataCalonMahasiswa.getProdiLulus();
							if (jurusan == null) {
								jurusan = biodataCalonMahasiswa.getProdi1();
							}

							row.createCell(2).setCellValue(jurusan == null || jurusan.getFakultas() == null ? ""
									: jurusan.getFakultas().getNama());
							row.createCell(3).setCellValue(jurusan == null ? "" : jurusan.getNama());
							row.createCell(4).setCellValue(kegiatan.getSemster());
							row.createCell(5).setCellValue(kegiatan.getJenisKegiatan() == null ? ""
									: kegiatan.getJenisKegiatan().getNamaKegiatan());
						}

						Double dibayar = 0.0;
						String tgl = "";
						for (CicilanPembayaran cicilanPembayaran : cicilanPembayarans2) {
							dibayar += cicilanPembayaran.getNilai();
							tgl += Common.dateFormat5.get().format(cicilanPembayaran.getTanggal()) + ", ";
						}

						row.createCell(6).setCellValue(pengaturanPembayaranBulanan != null
								? pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama() + " "
										+ pengaturanPembayaranBulanan.getRealBulan()
								: (detailBiaya.getItemBiaya() == null ? "" : detailBiaya.getItemBiaya().getNama()));

						Double nilaiBiayaHarusDiBayars = 0.0;
						if (pengaturanPembayaranBulanan != null) {
							nilaiBiayaHarusDiBayars = mahasiswa != null
									? Kegiatan.ambilJumlahTagihan(kegiatan, detailKegiatans, mahasiswa,
											kegiatan.getSemster(), pengaturanPembayaranBulanan)
									: Kegiatan.ambilJumlahTagihan(kegiatan, detailKegiatans, kegiatan.getSemster(),
											pengaturanPembayaranBulanan);
							if (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getPenghitungan()
									.equals(ItemBiaya.DIKALI_NILAI_MINUS)) {
								nilaiBiayaHarusDiBayars = -Math.abs(nilaiBiayaHarusDiBayars);
							}
						} else {
							nilaiBiayaHarusDiBayars = Kegiatan.ambilJumlahTagihan(kegiatan, detailBiaya, false);
							if (detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_NILAI_MINUS)) {
								nilaiBiayaHarusDiBayars = -Math.abs(nilaiBiayaHarusDiBayars);
							}
						}

						Date tanggalBayar = WaktuUtil.getDate();
						for (CicilanPembayaran cicilanPembayaran : cicilanPembayarans2) {
							try {
								if (cicilanPembayaran.getItemBiaya() != null
										&& cicilanPembayaran.getItemBiaya().getId().equals(
												pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getId())
										&& pengaturanPembayaranBulanan.getDetailBiaya().getBayarKe()
												.equals(cicilanPembayaran.getBayarKe())
										&& cicilanPembayaran.getKegiatan().getId().equals(kegiatan.getId())) {

									if (pengaturanPembayaranBulanan != null
											&& cicilanPembayaran.getPengaturanPembayaranBulanan() != null) {
										PengaturanPembayaranBulanan p = cicilanPembayaran
												.getPengaturanPembayaranBulanan();
										if (p.getDetailBiaya().getItemBiaya().getId().equals(
												pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getId())
												&& p.getRealBulan()
														.equals(pengaturanPembayaranBulanan.getRealBulan())) {
											tanggalBayar = cicilanPembayaran.getTanggal();
										}
									}
								}
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/CicilanPembayaranAction.java:710");
								// TODO: handle exception
							}
						}

						row.createCell(7).setCellValue(Common.numberFormat.get().format(nilaiBiayaHarusDiBayars));
						row.createCell(8).setCellValue(Common.numberFormat.get().format(dibayar));
						row.createCell(9).setCellValue(Common.numberFormat.get().format(nilaiBiayaHarusDiBayars - dibayar));
						row.createCell(10).setCellValue(tgl);

						Double hasilDenda = detailKegiatan != null
								&& (detailKegiatan.getBatalkanDenda() || nilaiBiayaHarusDiBayars.intValue() == 0)
										? nilaiBiayaHarusDiBayars
										: detailKegiatan != null && detailKegiatan.getMenggunakanDendaCustom()
												? nilaiBiayaHarusDiBayars
												: pengaturanPembayaranBulanan.checkDenda(nilaiBiayaHarusDiBayars,
														tanggalBayar, null, kegiatan.getJenisKegiatan());

						if (detailKegiatan != null && detailKegiatan.getMenggunakanDendaCustom()) {
							pengaturanPembayaranBulanan.setInfoDenda(" Penambahan denda senilai "
									+ Common.numberFormat.get().format(detailKegiatan.getDendaCustom()) + ".");
						}

						Double nilaiDenda = hasilDenda - nilaiBiayaHarusDiBayars;

						Double telahDibayar = VOMahasiswa.hitungTotalCicilan(kegiatan, pengaturanPembayaranBulanan,
								cicilanPembayarans2);

						nilaiBiayaHarusDiBayars = hasilDenda.intValue() > nilaiBiayaHarusDiBayars.intValue()
								? hasilDenda
								: nilaiBiayaHarusDiBayars;

						if ((detailBiaya != null
								&& detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_NILAI_MINUS))
								|| (pengaturanPembayaranBulanan != null && pengaturanPembayaranBulanan.getDetailBiaya()
										.getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_NILAI_MINUS))) {
							nilaiBiayaHarusDiBayars = -Math.abs(nilaiBiayaHarusDiBayars);
							telahDibayar = (telahDibayar == null ? 0.0 : -Math.abs(telahDibayar.doubleValue()));
						}

						row.createCell(11).setCellValue(nilaiDenda);

						Double tot = nilaiBiayaHarusDiBayars + dibayar;
						if (tot.intValue() == 0) {
							return -1L;
						}

						return id;
					}

					@SuppressWarnings({ "rawtypes" })
					@Override
					public void run() {
						try {

						List<Long> kegiatansid = initCriteria(false)
								.setProjection(Projections.groupProperty("kegiatan.id"))
								.addOrder(Order.asc("kegiatan.id")).setMaxResults(1048576).list();

						int index = 1;
						int size = kegiatansid.size();

						Map<Long, Collection> mapsTagihan = new HashMap<Long, Collection>();
						index = 1;
						size = kegiatansid.size();
						for (Long kegiatanid : kegiatansid) {

							try {
								Kegiatan kegiatan = (Kegiatan) GeneralValueObject.ambilData(Kegiatan.class,
										kegiatanid.toString(), true);
								if (kegiatan != null) {
									label.setValue("Memproses data tagihan " + kegiatan + " ("
											+ Common.numberFormat.get().format((index * 100.0) / size) + "%)");
									if (kegiatan.getMahasiswa() != null) {
										PembayaranUtil.getInstance();
										Collection detailBiayas = PembayaranUtilHelper.getDetailBiayaMahasiswa(
												kegiatan.getMahasiswa(), kegiatan.getSemster(),
												kegiatan.getJenisKegiatan(), false);
										Session session = HibernateUtil.currentNativeSession();
										int countPengaturanBulanan = PembayaranUtilHelper.countBulanan(session,
												kegiatan.getMahasiswa(), kegiatan.getJenisKegiatan(),
												kegiatan.getSemster(), detailBiayas, false, true);

										if (countPengaturanBulanan > 0) {
											detailBiayas = PembayaranUtilHelper.getDetailBiayaMahasiswa(
													kegiatan.getMahasiswa(), kegiatan.getSemster(),
													kegiatan.getJenisKegiatan(), "-1", false);
										}

										mapsTagihan.put(kegiatan.getId(), detailBiayas);
										HibernateUtil.closeSession();
									} else if (kegiatan.getCalonMahasiswa() != null) {
										BiodataCalonMahasiswa calonMahasiswa = kegiatan.getCalonMahasiswa();
										Jurusan prodiLulus = calonMahasiswa.getProdiLulus();
										Collection detailBiayas = new ArrayList();
										if (prodiLulus == null || prodiLulus.getId() == null) {
											Jurusan myjurusan1 = calonMahasiswa.getProdi1() == null
													? calonMahasiswa.getProdi2()
													: calonMahasiswa.getProdi1();
											java.util.Collection<DetailBiaya> detailBiayas1 = PembayaranUtilHelper.getDetailBiayaCalonMahasiswa(calonMahasiswa,
															kegiatan.getJenisKegiatan(), myjurusan1,
															kegiatan.getSemster(), false);

											detailBiayas.addAll(detailBiayas1);
										} else {
											java.util.Collection<DetailBiaya> detailBiayas1 = PembayaranUtilHelper.getDetailBiayaCalonMahasiswa(calonMahasiswa,
															kegiatan.getJenisKegiatan(), prodiLulus,
															kegiatan.getSemster(), false);
											detailBiayas.addAll(detailBiayas1);
										}

										Session session = HibernateUtil.currentNativeSession();
										int countPengaturanBulanan = PembayaranUtilHelper.countBulanan(session,
												kegiatan.getCalonMahasiswa(), kegiatan.getJenisKegiatan(),
												kegiatan.getSemster(), detailBiayas, false, true);

										if (countPengaturanBulanan > 0) {
											detailBiayas = PembayaranUtil.getInstance().getPengaturanPembayaranSemua(
													kegiatan.getCalonMahasiswa(), session, kegiatan.getSemster(),
													kegiatan.getJenisKegiatan(), detailBiayas, false, false);
										}

										mapsTagihan.put(kegiatan.getId(), detailBiayas);

										HibernateUtil.closeSession();
									}
								}
							} catch (Exception e) {
								ais.common.Common.tampilErrorJikaAdmin(e);
							}

						}

						XSSFWorkbook workbook = new XSSFWorkbook();
						XSSFSheet sheet = workbook.createSheet("Rekap Tagihan dan Pembayaran");
						sheet.setDefaultColumnWidth(20);

						XSSFRow rowhead = sheet.createRow((short) 0);
						rowhead.createCell(0).setCellValue("NIM/No.Reg");
						rowhead.createCell(1).setCellValue("Nama");
						rowhead.createCell(2).setCellValue("Fakultas");
						rowhead.createCell(3).setCellValue("Prodi");
						rowhead.createCell(4).setCellValue("Semester");
						rowhead.createCell(5).setCellValue("Jenis Pembayaran");
						rowhead.createCell(6).setCellValue("Item Biaya");
						rowhead.createCell(7).setCellValue("Tagihan");
						rowhead.createCell(8).setCellValue("Dibayar");
						rowhead.createCell(9).setCellValue("Sisa");
						rowhead.createCell(10).setCellValue("Terakhir bayar Hari/Tanggal/Waktu");
						rowhead.createCell(11).setCellValue("Denda");
						colS.setValue(12);

						index = 1;
						size = kegiatansid.size();
						Long id = null;
						int rowIndex = 1;
						for (Long kegiatanid : kegiatansid) {
							try {

								Kegiatan kegiatan = (Kegiatan) GeneralValueObject.ambilData(Kegiatan.class,
										kegiatanid.toString(), true);
								if (kegiatan != null) {

									List<CicilanPembayaran> mycicilanPembayarans = kegiatan.ambilCicilan();
									label.setValue("Memproses data tagihan dan pembayaran " + kegiatan + " ("
											+ Common.numberFormat.get().format((index * 100.0) / size) + "%)");

									Collection<DetailKegiatan> detailKegiatans = kegiatan == null
											|| kegiatan.getId() == null ? null : kegiatan.ambilDetailKegiatan(false);

									index++;
									for (Object o : mapsTagihan.get(kegiatanid)) {

										try {
											DetailBiaya detailBiaya = null;
											PengaturanPembayaranBulanan pengaturanPembayaranBulanan = null;
											if (o instanceof DetailBiaya) {
												detailBiaya = (DetailBiaya) o;
											} else if (o instanceof PengaturanPembayaranBulanan) {
												pengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) o;
											}

											List<CicilanPembayaran> cicilanPembayarans2 = new ArrayList<CicilanPembayaran>();
											for (CicilanPembayaran cicilanPembayaran : mycicilanPembayarans) {
												try {
													if (cicilanPembayaran != null) {
														if (pengaturanPembayaranBulanan != null
																&& cicilanPembayaran
																		.getPengaturanPembayaranBulanan() != null
																&& pengaturanPembayaranBulanan.getId()
																		.equals(cicilanPembayaran
																				.getPengaturanPembayaranBulanan()
																				.getId())) {
															cicilanPembayarans2.add(cicilanPembayaran);
														}
														if (detailBiaya != null && detailBiaya.getItemBiaya() != null
																&& detailBiaya.getItemBiaya().getId().equals(
																		cicilanPembayaran.getItemBiaya().getId())) {
															cicilanPembayarans2.add(cicilanPembayaran);
														}
													}
												} catch (Exception e) {
													ais.common.Common.tampilErrorJikaAdmin(e);
												}
											}

											Long kembali = proses(rowIndex, sheet, id, kegiatan, detailBiaya,
													pengaturanPembayaranBulanan, cicilanPembayarans2, detailKegiatans);
											if (kembali != null && !kembali.equals(-1L)) {
												id = kembali;
												rowIndex++;
											}
											cicilanPembayarans2 = null;
										} catch (Exception e) {
											ais.common.Common.tampilErrorJikaAdmin(e);
										}
									}
									mycicilanPembayarans = null;
								}
							} catch (Exception e) {
								ais.common.Common.tampilErrorJikaAdmin(e);
							}
						}

						intbox.setValue(rowIndex + 1);

						mapsTagihan = null;

						try {
							FileOutputStream fileOut = new FileOutputStream(filename);
							workbook.write(fileOut);
							fileOut.close();
						} catch (IOException e) {
							Common.tampilErrorJikaAdmin(e);
						}

						label.setValue("");

						HibernateUtil.closeSession();
											} finally {
							ais.database.hibernate.HibernateUtil.closeSession();
						}
					}
				}).start();

			}
		});
		Common.appendKeToolbar(generatePasswordMahasiswa, find, comp);

		final Vbox vbox = new Vbox();
		if (vbox != null) { vbox.setParent(find.getParent()); }

		// Query read-only ini memicu auto-flush; bila persistence context thread-local mengandung
		// entity transient (mis. Mahasiswa yang belum tersimpan dari komponen lain di halaman),
		// auto-flush gagal dengan TransientObjectException. Pakai FlushMode.MANUAL sementara agar
		// query tidak memaksa flush; mode dikembalikan di finally (currentSession, jangan ditutup).
		Session sessKhsItem = HibernateUtil.currentSession();
		org.hibernate.FlushMode flushSebelumnya = sessKhsItem.getFlushMode();
		List<ItemBiaya> itemBiayas;
		try {
			sessKhsItem.setFlushMode(org.hibernate.FlushMode.MANUAL);
			itemBiayas = sessKhsItem.createCriteria(DetailSettingBiaya.class)
					.createAlias("itemBiaya", "itemBiaya")
					.add(Restrictions.or(Restrictions.eq("itemBiaya.aktif", true), Restrictions.isNull("itemBiaya.aktif")))
					.setProjection(Projections.groupProperty("itemBiaya")).addOrder(Order.asc("itemBiaya")).list();
		} finally {
			sessKhsItem.setFlushMode(flushSebelumnya);
		}
		Hbox hbox1 = new Hbox();
		vbox.appendChild(hbox1);
		int index = 0;
		for (ItemBiaya itemBiaya : itemBiayas) {
			if (index % 15 == 0) {
				hbox1 = new Hbox();
				vbox.appendChild(hbox1);
			}
			index++;
			Checkbox checkbox = new Checkbox(itemBiaya.getNama());
			checkbox.setAttribute("itemBiaya", itemBiaya);
			mapItemBiaya.add(checkbox);
			checkbox.setChecked(true);
			checkbox.setStyle("font-size:8px");
			checkbox.setParent(hbox1);
		}
		itemBiayas = null;

	}

	/**
	 * Renderer lokal untuk layar/komponen {@link CicilanPembayaranAction}. Kelas ini menerjemahkan satu item data
	 * menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link CicilanPembayaranAction} dan dapat mengakses
	 * state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see CicilanPembayaranAction
	 */
	class CicilanPembayaranRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final CicilanPembayaran cicilanPembayaran = (CicilanPembayaran) arg1;
			final Kegiatan kegiatan = cicilanPembayaran.getKegiatan();
			if (kegiatan.getMahasiswa() != null) {

				new Label(kegiatan.toString()).setParent(arg0);

				new Label(kegiatan.getMahasiswa() == null ? "" : kegiatan.getMahasiswa().getNim()).setParent(arg0);

				RevisiHelper
						.createNewRevisi(CicilanPembayaran.class, cicilanPembayaran,
								kegiatan.getMahasiswa() == null ? "" : kegiatan.getMahasiswa().getNama())
						.setParent(arg0);

			} else if (kegiatan.getCalonMahasiswa() != null) {
				new Label(kegiatan.toString()).setParent(arg0);

				if (kegiatan.getJenisKegiatan() != null && kegiatan.getJenisKegiatan().getId()
						.equals(ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU.getId())) {
					new Label(kegiatan.getCalonMahasiswa() == null ? ""
							: (kegiatan.getCalonMahasiswa().getNoUjian() == null
									? kegiatan.getCalonMahasiswa().getNoRegistrasi()
									: kegiatan.getCalonMahasiswa().getNoUjian()))
							.setParent(arg0);
				} else {
					new Label(
							kegiatan.getCalonMahasiswa() == null ? "" : kegiatan.getCalonMahasiswa().getNoRegistrasi())
							.setParent(arg0);
				}

				RevisiHelper
						.createNewRevisi(CicilanPembayaran.class, cicilanPembayaran,
								kegiatan.getCalonMahasiswa() == null ? "" : kegiatan.getCalonMahasiswa().getNama())
						.setParent(arg0);

			}
			Integer tahap = cicilanPembayaran.getTahap();
			new Label(kegiatan.getSemster() + ""
					+ ((ConstantValues.aktifkanTahapanTerhubungKeKeuangan && tahap != null && tahap > 0)
							? " / thp: " + cicilanPembayaran.getTahap()
							: ""))
					.setParent(arg0);

			new Label(cicilanPembayaran.getJenisPembayaran() == null ? ""
					: cicilanPembayaran.getJenisPembayaran().getNama()).setParent(arg0);

			if (tampilkanTanggalKwitansi) {
				Vbox vbox = new Vbox();
				vbox.setParent(arg0);
				new Label(Common.dateFormat3.get().format(cicilanPembayaran.getTanggal())).setParent(vbox);
				new Label(Common.dateFormat3.get().format(cicilanPembayaran.getTanggalKwitansi())).setParent(vbox);
			} else {
				new Label(Common.dateFormat3.get().format(cicilanPembayaran.getTanggal())).setParent(arg0);
			}
			new Label(Common.numberFormat.get().format(cicilanPembayaran.getNilai())).setParent(arg0);
			new Label(Common.numberFormat.get().format(cicilanPembayaran.getDeposit())).setParent(arg0);

			new Label(cicilanPembayaran.getKeterangan()).setParent(arg0);

			// Kolom aksi rapi (pola MahasiswaAction): semua tombol dibungkus kebab popup (⋯)
			// via UIHelper.buatBarisAksi — kolom aksi jadi kecil dan konsisten antar layar.
			final java.util.List<org.zkoss.zk.ui.Component> aksiButtons =
					new java.util.ArrayList<org.zkoss.zk.ui.Component>();

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Cetak", "/img/print.png");
			button.setTooltiptext("Cetak");
			button.setOrient("vertical");
			button.addEventListener("onClick", new EventListener() {
				@SuppressWarnings({})
				@Override
				public void onEvent(Event event) throws Exception {
					if (kegiatan.getMahasiswa() != null) {
						CommonReportHelper.cetakBuktipembayaranMahasiswa(kegiatan, false);
					} else {
						CommonReportHelper.cetakBuktipembayaranCalonMahasiswa(kegiatan, false);
					}
				}
			});
			aksiButtons.add(button);

			button = new MyToolbarbuttonConfig("Reversal", "/img/svg/warning-outline.svg");
			button.setTooltiptext("Reversal");
			button.setOrient("vertical");

			Tbmuser tbmuser = Common.getCurrentUser();
			boolean bolehMerubahCicilan = false;
			String admLain = Common.getKonfigurasi("admin_yang_bisa_menghapus_data_pembayaran_mahasiswa", "am")
					.getNilai();
			String[] aa = admLain.split(";");
			for (String a : aa) {
				try {
					bolehMerubahCicilan = a.trim().equalsIgnoreCase(tbmuser.hakAkses().getRoleId());
					if (bolehMerubahCicilan) {
						break;
					}
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);

				}
			}

			if (!bolehMerubahCicilan) {
				admLain = Common.getKonfigurasi("admin_lain_bisa_menghapus_pembayaran_mahasiswa", "").getNilai();
				aa = admLain.split(";");
				for (String a : aa) {
					try {
						bolehMerubahCicilan = a.trim().equalsIgnoreCase(tbmuser.getUserId());
						if (bolehMerubahCicilan) {
							break;
						}
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);

					}
				}
			}

			button.setVisible(bolehMerubahCicilan || (tbmuser != null 
					&& tbmuser.hakAkses() != null && tbmuser.hakAkses().getRoleId() != null
					&& tbmuser.hakAkses().getRoleId().trim().equalsIgnoreCase(Tbmrole.ADMINISTRATOR)));
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					MyMessageboxConfig.show("Apakah yakin ingin melakukan reversal pada pembayaran ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {

											HibernateUtil.currentSession().delete(cicilanPembayaran);

											MyMessageboxConfig.show("Reversal cicilah berhasil dilakukan",
													"Pemberitahuan", MyMessageboxConfig.OK,
													MyMessageboxConfig.EXCLAMATION);

											onSearchDefault(null);

										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											MyMessageboxConfig.show(
													"Data ini tidak dapat Reversal .., error-nya adalah sbagai berikut:"
															+ e.getMessage());
										}

									}

								}
							});

				}

			});
			aksiButtons.add(button);

			ais.ui.util.UIHelper.buatBarisAksi(arg0, 3, aksiButtons);
		}

	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Fakultas fakultas = (Fakultas) (searchfakultas.getSelectedItem() == null
				|| searchfakultas.getSelectedItem().getValue() == null
				|| searchfakultas.getSelectedItem().getValue() == null ? null
						: searchfakultas.getSelectedItem().getValue());
		Jurusan jurusan = (Jurusan) (searchjurusan.getSelectedItem() == null
				|| searchjurusan.getSelectedItem().getValue() == null
				|| searchjurusan.getSelectedItem().getValue() == null ? null
						: searchjurusan.getSelectedItem().getValue());

		List<Long> ids = new ArrayList<Long>();
		for (Checkbox checkbox : mapItemBiaya) {
			if (checkbox.isChecked()) {
				ItemBiaya itemBiaya = (ItemBiaya) checkbox.getAttribute("itemBiaya");
				ids.add(itemBiaya.getId());
			}
		}

		String sqlItemBiaya = "true";
		if (!mapItemBiaya.isEmpty()) {
			sqlItemBiaya = "";
			for (Long itemBiayaId : ids) {
				sqlItemBiaya += sqlItemBiaya.isEmpty() ? itemBiayaId : "," + itemBiayaId;
			}
			sqlItemBiaya = "this_.item_biaya in (" + sqlItemBiaya + ")";
		}

		Criteria criteria = session.createCriteria(CicilanPembayaran.class)
				.add(deposit ? Restrictions.ge("deposit", 0.1) : Restrictions.sqlRestriction("true"))
				.add(Restrictions.sqlRestriction(sqlItemBiaya)).createAlias("kegiatan", "kegiatan")

				.add(ta.getSelectedItem() == null || ta.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("kegiatan.tahunAkademik", ta.getSelectedItem().getValue()))

				.add(Restrictions.or(Restrictions.gt("nilai", 0.01), Restrictions.lt("nilai", -0.01)))

				.add((jenissemester.getSelectedItem() == null || jenissemester.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: (jenissemester.getSelectedItem().getValue().toString().equalsIgnoreCase(Perkuliahan.GENAP)
								? Restrictions.in("kegiatan.semster", Common.genap)
								: Restrictions.in("kegiatan.semster", Common.ganjil))));

		if (order)
			criteria.addOrder(Order.desc("id"));
		criteria.createAlias("kegiatan.mahasiswa", "mahasiswa", Criteria.LEFT_JOIN)
				.createAlias("kegiatan.calonMahasiswa", "calonMahasiswa", Criteria.LEFT_JOIN)

				.createAlias("mahasiswa.jurusan", "jurusan", Criteria.LEFT_JOIN)
				.createAlias("calonMahasiswa.prodiLulus", "prodiLulus", Criteria.LEFT_JOIN)

				.add(searchtahun.getValue() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.eq("mahasiswa.tahunangkatan", searchtahun.getValue().intValue()),
								Restrictions.eq("calonMahasiswa.tahun", searchtahun.getValue().intValue())))

				.add(jurusan == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.eq("mahasiswa.jurusan", jurusan),
								Restrictions.eq("calonMahasiswa.prodiLulus", jurusan)))

				.add((start == null || end == null || start.getValue() == null || end.getValue() == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.sqlRestriction(
						"date(this_.tanggal) between date('" + Common.databaseDateFormat.get().format(start.getValue())
								+ "') and date('" + Common.databaseDateFormat.get().format(end.getValue()) + "')")))

				.add(fakultas == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.eq("jurusan.fakultas", fakultas),
								Restrictions.eq("prodiLulus.fakultas", fakultas)))

				.add(Restrictions.or(
						Restrictions.or(Restrictions.or(
								Restrictions.ilike("mahasiswa.nim", searchnama.getValue().trim(), MatchMode.ANYWHERE),
								Restrictions.ilike("calonMahasiswa.nim", searchnama.getValue().trim(),
										MatchMode.ANYWHERE)),
								Restrictions.ilike("calonMahasiswa.noRegistrasi", searchnama.getValue().trim(),
										MatchMode.ANYWHERE)),
						Restrictions.ilike("calonMahasiswa.noUjian", searchnama.getValue().trim())));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		// -1 = belum ada hitungan siap pakai (dihitung ulang via refleksi di refreshDashboardAman,
		// perilaku lama) -- HANYA diisi dari paging.getTotalSize() pada cabang "else" di bawah, karena
		// pada cabang mahasiswa (tampilan sendiri) paging tidak pernah di-initPaging ulang di sini
		// (malah disembunyikan), sehingga nilainya basi/tidak mewakili filter yang sedang aktif.
		long totalSudahDihitung = -1L;

		if (tbmuser != null && tbmuser.getMahasiswa() != null) {
			try {
				paging.getParent().setVisible(false);
				grid.setMold("paging");
				grid.getPagingChild().setMold("os");
				grid.setPageSize(10);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/CicilanPembayaranAction.java:1251");
				// TODO: handle exception
			}

			List<CicilanPembayaran> cicilanPembayaran = tbmuser.getMahasiswa().ambilCicilan(null, deposit);
			ListModel strset = new SimpleListModel(cicilanPembayaran);
			grid.setRowRenderer(new CicilanPembayaranRenderer());
			grid.setModelCheckMobile(strset);
		} else {
			Common.initPaging(initCriteria(false), paging);
			List<CicilanPembayaran> cicilanPembayaran = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
					.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
			ListModel strset = new SimpleListModel(cicilanPembayaran);
			grid.setRowRenderer(new CicilanPembayaranRenderer());
			grid.setModelCheckMobile(strset);
			// paging.getTotalSize() sudah dihitung Common.initPaging(...) barusan -> teruskan agar
			// refreshDashboardAman TIDAK menjalankan ulang SELECT count(*) yang sama (join berat).
			totalSudahDihitung = paging == null ? -1L : paging.getTotalSize();
		}

		refreshDashboardAman(totalSudahDihitung);
	}

	private void refreshDashboardAman(long totalSudahDihitung) {
		try {
			ais.action.master.helper.GenericActionDashboardHelper.refreshFromCriteria(dashboardHtml, progressHtml, this,
					"Dasbor Pembayaran Angsuran",
					"Pantau data cicilan pembayaran mahasiswa, tren angsuran, dan komposisi pembayaran berdasarkan filter yang sedang dipakai.",
					totalSudahDihitung);
		} catch (Exception e) {
			try {
				Common.tampilErrorJikaAdmin(e);
			} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/CicilanPembayaranAction.java:1279");
			}
		}
	}
}
