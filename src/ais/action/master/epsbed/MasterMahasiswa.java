package ais.action.master.epsbed;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.Calendar;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
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
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;

import ais.action.ws.util.PembayaranUtil;
import ais.common.Common;
import ais.common.CommonSearchFilterHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.BiodataMahasiswa;
import ais.database.model.Jurusan;
import ais.database.model.Mahasiswa;
import ais.database.model.PerguruanTinggi;
import ais.database.model.StatusMahasiswa;
import ais.database.model.file.FotoMahasiswa;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Tipe khusus untuk master mahasiswa. Kelas ini memberi nama dan batas tanggung jawab yang
 * eksplisit pada perilaku yang diwarisi atau kontrak yang diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Center center}, {@code Combobox
 * searchfakultas}, {@code PembayaranUtil pembayaranUtil}, {@code Combobox searchjurusan}, {@code Intbox
 * searchangkatan}, {@code File file}; inisialisasi/lifecycle ({@code init()}, {@code initSpreadsheet()}). Bagian
 * lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class MasterMahasiswa extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 790038368339375113L;

	private Center center = new Center();
	private Combobox searchfakultas = new Combobox();

	public PembayaranUtil pembayaranUtil = PembayaranUtil.getInstance();

	private Combobox searchjurusan = new Combobox();
	private Intbox searchangkatan = new Intbox(ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR));

	private File file;

	public MasterMahasiswa() {
		super();
		try {

			Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);
			init();
			// initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
		}
	}

	public MasterMahasiswa(String title, String border, boolean closable) {
		super(title, border, closable);
		try {
			init();
			// initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
		}
	}

	private void init() throws Exception {

		setHeight("100%");
		setWidth("100%");
		setBorder("none");
		setClosable(false);
		setPosition("center");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);

		North north = new North();
		north.setParent(borderlayout);
		// FIX toolbar/tombol tidak tampil: pada ZK5 region North memakai tinggi bawaan
		// (+-100px); dengan flex=true isinya diregangkan ke tinggi tersebut sehingga
		// Toolbar yang diletakkan DI BAWAH grid filter ikut terpotong. Disamakan dengan
		// layar sejenis yang sudah benar (DownloadMahasiswa, DownloadKrs, DownloadNilai):
		// flex dimatikan + tinggi eksplisit. Autoscroll sebagai pengaman bila isi bertambah.
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

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(searchfakultas);
		searchfakultas.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(searchjurusan);
		searchjurusan.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Angkatan"));
		row.appendChild(searchangkatan);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		// toolbar.setHeight("25px");
		toolbar.setParent(div);

		MyToolbarbuttonConfig search = new MyToolbarbuttonConfig("Refresh", "/img/Button-Refresh-icon.png");
		search.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				// TODO Auto-generated method stub
				System.out.println("search");
				initSpreadsheet();
			}
		});
		search.setParent(toolbar);

		MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Export Epsbed (MSMHS.xls)", "/img/excel.png");
		print.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				try {
					Filedownload.save(new FileInputStream(file), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "MSMHS.xlsx");
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/epsbed/MasterMahasiswa.java:151");

				}
			}
		});
		print.setParent(toolbar);

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		initSpreadsheet();
	}

	@SuppressWarnings({ "unchecked" })
	private void initSpreadsheet() throws Exception {

		Common.clear(center);
		System.out.println("init spreadsheet running");
		final Jurusan jurusan = searchjurusan.getSelectedItem() == null
				|| searchjurusan.getSelectedItem().getValue() == null
				|| searchjurusan.getSelectedItem().getValue() == null ? null
						: (Jurusan) searchjurusan.getSelectedItem().getValue();

		final String filename = Sessions.getCurrent().getWebApp()
				.getRealPath("/tmp/data_" + URLEncoder.encode(Common.datetimeFormat2s.get().format(ais.ui.util.WaktuUtil.getDate()), "UTF-8") + ".xlsx");

		(file = new File(filename)).createNewFile();

		final Intbox sizedata = new Intbox(30);
		final Label label = Common.displayLoadBar(this, file, center, sizedata);

		final PerguruanTinggi perguruanTinggi = (PerguruanTinggi) HibernateUtil.currentSession()
				.createCriteria(PerguruanTinggi.class).setMaxResults(1).uniqueResult();

		new Thread(new Runnable() {

			@Override
			public void run() {
				try {

				XSSFWorkbook workbook = new XSSFWorkbook();
				XSSFSheet sheet = workbook.createSheet("DATA");
				sheet.setDefaultColumnWidth(20);
				int rowIndex = 0;

				XSSFRow rowhead = sheet.createRow((short) 0);

				rowhead.createCell(0).setCellValue("KDPTIMSMH");
				rowhead.createCell(1).setCellValue("KDJENMSMH");
				rowhead.createCell(2).setCellValue("KDPSTMSMH");
				rowhead.createCell(3).setCellValue("NIMHSMSMH");
				rowhead.createCell(4).setCellValue("NMMHSMSMH");
				rowhead.createCell(5).setCellValue("SHIFTMSMH");

				rowhead.createCell(6).setCellValue("TPLHRMSMH");
				rowhead.createCell(7).setCellValue("TGLHRMSMH");
				rowhead.createCell(8).setCellValue("USIAMMSMH");
				rowhead.createCell(9).setCellValue("KDJEKMSMH");
				rowhead.createCell(10).setCellValue("TAHUNMSMH");
				rowhead.createCell(11).setCellValue("SMAWLMSMH");
				rowhead.createCell(12).setCellValue("NIMANMSMH");
				rowhead.createCell(13).setCellValue("NILUNMSMH");
				rowhead.createCell(14).setCellValue("BTSTUMSMH");
				rowhead.createCell(15).setCellValue("ASSMAMSMH");
				rowhead.createCell(16).setCellValue("TGMSKMSMH");

				rowhead.createCell(17).setCellValue("TGLLSMSMH");
				rowhead.createCell(18).setCellValue("STMHSMSMH");
				rowhead.createCell(19).setCellValue("MLSEMMSMH");
				rowhead.createCell(20).setCellValue("STPIDMSMH");
				rowhead.createCell(21).setCellValue("SKSDIMSMH");
				rowhead.createCell(22).setCellValue("ASNIMMSMH");

				rowhead.createCell(23).setCellValue("ASPTIMSMH");
				rowhead.createCell(24).setCellValue("ASJENMSMH");
				rowhead.createCell(25).setCellValue("ASPSTMSMH");
				rowhead.createCell(26).setCellValue("BISTUMSMH");
				rowhead.createCell(27).setCellValue("PEKSBMSMH");

				rowhead.createCell(28).setCellValue("NMPEKMSMH");
				rowhead.createCell(29).setCellValue("PTPEKMSMH");
				rowhead.createCell(30).setCellValue("PSPEKMSMH");
				rowhead.createCell(31).setCellValue("NOPRMMSMH");
				rowhead.createCell(32).setCellValue("NOKP1MSMH");

				rowhead.createCell(33).setCellValue("NOKP2MSMH");
				rowhead.createCell(34).setCellValue("NOKP3MSMH");
				rowhead.createCell(35).setCellValue("NOKP4MSMH");
				rowhead.createCell(36).setCellValue("STKRSMSMH");
				rowhead.createCell(37).setCellValue("SMAW1MSMH");

				rowhead.createCell(38).setCellValue("TGMS1MSMH");
				rowhead.createCell(39).setCellValue("ALMHSMSMH");
				rowhead.createCell(40).setCellValue("TELRMMSMH");
				rowhead.createCell(41).setCellValue("NOHPPMSMH");
				rowhead.createCell(42).setCellValue("EMAILMSMH");
				rowhead.createCell(43).setCellValue("FOTOOMSMH");
				rowhead.createCell(44).setCellValue("NOIJSMSMH");
				rowhead.createCell(45).setCellValue("PDLLSMSMH");

				rowhead.createCell(46).setCellValue("NIK");

				Session session = HibernateUtil.currentNativeSession();

				List<Mahasiswa> mahasiswas = session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.createAlias("jurusan", "jurusan", Criteria.LEFT_JOIN)

				.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
						|| searchjurusan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(Restrictions.eq("jurusan", jurusan),
										Restrictions.eq("jurusan", jurusan)))

				.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
						|| searchfakultas.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("jurusan.fakultas", searchfakultas, false))

				.add(searchangkatan.getValue() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("tahunangkatan", searchangkatan.getValue()))

				.list();
				rowIndex = 1;
				for (Mahasiswa mahasiswa : mahasiswas) {
					if (mahasiswa == null || mahasiswa.getId() == null) {
						continue;
					}

					label.setValue("Sedang memproses data " + mahasiswa.toString() + " ("
							+ Common.numberFormat.get().format(rowIndex * 100.0 / mahasiswas.size()) + " %)");

					XSSFRow row = sheet.createRow(rowIndex);
					row.createCell(0)
							.setCellValue(perguruanTinggi == null || perguruanTinggi.getId() == null ? "" : perguruanTinggi.getKodePerguruanTinggi());
					row.createCell(1).setCellValue(mahasiswa.getJenjang().getJenjangEpsbed());
					row.createCell(2).setCellValue(mahasiswa.getJurusan() == null
							? mahasiswa.getJurusan().getKodeEpsbed() : mahasiswa.getJurusan().getKodeEpsbed());
					row.createCell(3).setCellValue(mahasiswa.getNim());
					row.createCell(4).setCellValue(mahasiswa.getNama());
					row.createCell(5).setCellValue(mahasiswa.getProgram().equalsIgnoreCase("Reguler") ? "R" : "N");
					row.createCell(6).setCellValue(mahasiswa.getTempatlahir());
					row.createCell(7).setCellValue(mahasiswa.getTanggallahir() == null ? ""
							: CommonEpsbed.dateFormatEpsbed.get().format(mahasiswa.getTanggallahir()));
					BiodataMahasiswa biodataMahasiswa = (BiodataMahasiswa) HibernateUtil.currentSession()
							.createCriteria(BiodataMahasiswa.class).add(Restrictions.eq("mahasiswa", mahasiswa))
							.setMaxResults(1).uniqueResult();
					// System.out.println(ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR)
					// + " year");
					Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
					calendar.setTime(mahasiswa.getTanggallahir() == null ? ais.ui.util.WaktuUtil.getDate() : mahasiswa.getTanggallahir());
					// System.out.println(mahasiswa.getTanggallahir());
					int age = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR) - calendar.get(Calendar.YEAR);
					// System.out.println("age : " + age);
					row.createCell(8).setCellValue(age);
					row.createCell(9).setCellValue(mahasiswa.getKelamin() == null ? ""
							: mahasiswa.getKelamin().equals("Laki-laki") ? "L" : "P");
					row.createCell(10).setCellValue(mahasiswa.getTahunangkatan());
					row.createCell(11).setCellValue(mahasiswa.getSemesterMulai());
					row.createCell(12).setCellValue("");
					row.createCell(13).setCellValue("");
					row.createCell(14).setCellValue(mahasiswa.getBatasStudi());

					BiodataCalonMahasiswa biodataCalonMahasiswa = (BiodataCalonMahasiswa) session
							.createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).add(Restrictions.eq("nim", mahasiswa.getNim()))
							.setMaxResults(1).uniqueResult();

					row.createCell(15)
							.setCellValue(biodataCalonMahasiswa == null ? ""
									: biodataCalonMahasiswa.getPropinsiSekolah() == null ? ""
											: biodataCalonMahasiswa.getPropinsiSekolah().getKodeEpsbed());
					row.createCell(16).setCellValue(mahasiswa.getTanggalMasuk() == null ? ""
							: CommonEpsbed.dateFormatEpsbed.get().format(mahasiswa.getTanggalMasuk()));
					row.createCell(17).setCellValue(mahasiswa.getTanggalLulus() == null ? ""
							: CommonEpsbed.dateFormatEpsbed.get().format(mahasiswa.getTanggalLulus()));

					StatusMahasiswa statusMahasiswa = ais.action.master.helper.HistoryStatusMahasiswaUtil.currentStatus(mahasiswa).getStatusMahasiswa();
					row.createCell(18).setCellValue(statusMahasiswa.getKodeEpsbed());
					row.createCell(19).setCellValue("");
					row.createCell(20).setCellValue(mahasiswa.getStatusAwalMahasiswa() == null ? ""
							: mahasiswa.getStatusAwalMahasiswa().getNama().equals("Baru") ? "B" : "P");
					row.createCell(21).setCellValue(mahasiswa.getJumlahSksPenyetaraan() == null ? "0"
							: mahasiswa.getJumlahSksPenyetaraan() + "");
					row.createCell(22)
							.setCellValue(mahasiswa.getNimPindahan() == null ? "" : mahasiswa.getNimPindahan());
					row.createCell(23).setCellValue(mahasiswa.getPindahanPerguruanTinggi());
					row.createCell(24).setCellValue(mahasiswa.getPindahJenjang() == null ? ""
							: mahasiswa.getPindahJenjang().getJenjangEpsbed());
					row.createCell(25).setCellValue(
							mahasiswa.getPindahJurusan() == null ? "" : mahasiswa.getPindahJurusan().getKodeEpsbed());
					row.createCell(26).setCellValue(biodataMahasiswa == null ? "" : biodataMahasiswa.getBiayaStudi());

					row.createCell(27).setCellValue(biodataMahasiswa == null ? "" : biodataMahasiswa.getTempatKerja());
					row.createCell(28)
							.setCellValue(biodataMahasiswa == null ? "" : biodataMahasiswa.getKodeTempatKerjaPt());
					row.createCell(29)
							.setCellValue(biodataMahasiswa == null ? "" : biodataMahasiswa.getKodeTempatKerjaPs());
					row.createCell(30).setCellValue(biodataMahasiswa == null ? "" : biodataMahasiswa.getNidnPromotor());
					row.createCell(31)
							.setCellValue(biodataMahasiswa == null ? "" : biodataMahasiswa.getNidnKoPromotor1());
					row.createCell(32)
							.setCellValue(biodataMahasiswa == null ? "" : biodataMahasiswa.getNidnKoPromotor2());
					row.createCell(33)
							.setCellValue(biodataMahasiswa == null ? "" : biodataMahasiswa.getNidnKoPromotor3());
					row.createCell(34)
							.setCellValue(biodataMahasiswa == null ? "" : biodataMahasiswa.getNidnKoPromotor4());
					row.createCell(35).setCellValue(mahasiswa.getStatusKrs());
					row.createCell(36).setCellValue("");
					row.createCell(37).setCellValue("");
					row.createCell(38).setCellValue("");
					row.createCell(39).setCellValue(mahasiswa.getAlamat());
					row.createCell(40).setCellValue(mahasiswa.getTelp());
					row.createCell(41).setCellValue(biodataMahasiswa == null ? "" : biodataMahasiswa.getHp());
					row.createCell(42).setCellValue(mahasiswa.getEmail());
					FotoMahasiswa fotoBiodataMahasiswa = null;
					row.createCell(43)
							.setCellValue(fotoBiodataMahasiswa == null ? "" : fotoBiodataMahasiswa.getId().toString());
					row.createCell(44).setCellValue("");
					row.createCell(45).setCellValue("");

					row.createCell(46).setCellValue(biodataMahasiswa == null ? "" : biodataMahasiswa.getNoIdentitas());

					rowIndex++;
					// spreadsheet.setRowfreeze(rowIndex);
				}

				Common.setStyled(sheet);sizedata.setValue(rowIndex + 1);

				try {
					FileOutputStream fileOut = new FileOutputStream(filename);
					workbook.write(fileOut);
					fileOut.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					Common.tampilErrorJikaAdmin(e); 
				}

				System.out.println("Your excel file has been generated! " );

				HibernateUtil.closeSession();

				mahasiswas.clear();
				label.setValue("");
							} finally {
					ais.database.hibernate.HibernateUtil.closeSession();
				}
			}
		}).start();

	}
}
