package ais.action.master.feeder.integrator.helper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.Calendar;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
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
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.common.CommonSearchFilterHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Jurusan;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.Perkuliahan;
import ais.database.model.StatusMahasiswa;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class DownloadHistory extends MyWindow {

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

	private File file;

	private Combobox searchstatus;

	public DownloadHistory() {
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

			init();

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public DownloadHistory(String title, String border, boolean closable) {
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

		MyFormRow row = new MyFormRow();row.setValign("top");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Angkatan"));
		row.appendChild(searchangkatan);
		searchangkatan.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(searchfakultas);
		searchfakultas.setWidth("90%");
		searchfakultas.setReadonly(true);

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(searchjurusan);
		searchjurusan.setWidth("90%");
		searchjurusan.setReadonly(true);

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Program"));
		row.appendChild(searchprogram);
		searchprogram.setReadonly(true);
		searchprogram.setWidth("90%");

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

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
		row.appendChild(searchtahunakademik);
		searchtahunakademik.setWidth("90%");
		Common.generateTahunAjaranDanSemua(searchtahunakademik);
		searchtahunakademik.setReadonly(true);

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("NIM/Nama Mahasiswa"));
		Hbox hbox2 = new Hbox();
		row.appendChild(hbox2);
		hbox2.appendChild(nimMahasiswa);
		nimMahasiswa.setCols(8);
		hbox2.appendChild(namaMahasiswa);
		namaMahasiswa.setCols(8);

		row.appendChild(new ais.ui.util.MyLabelConfig("Status Mahasiswa"));
		row.appendChild(searchstatus = new Combobox());
		Common.insertComboDanSemua(searchstatus, "nama", "kodeEpsbed", StatusMahasiswa.class);
		searchstatus.setWidth("90%");

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
					Filedownload.save(new FileInputStream(file), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "mahasiswa.xlsx");
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/integrator/helper/DownloadHistory.java:222");

				}
			}
		});
		print.setParent(toolbar);

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
	}

	@SuppressWarnings({ "unchecked" })
	private void initSpreadsheet() throws Exception {

		final String semester = (String) searchsemester.getSelectedItem().getValue();
		final String tahunAkademik = (String) searchtahunakademik.getSelectedItem().getValue();
		final String kel = kelas.getValue().trim();

		Common.clear(center);

		System.out.println("init spreadsheet running");
		final Jurusan jurusan = searchjurusan.getSelectedItem() == null
				|| searchjurusan.getSelectedItem().getValue() == null
				|| searchjurusan.getSelectedItem().getValue() == null ? null
						: (Jurusan) searchjurusan.getSelectedItem().getValue();

		final StatusMahasiswa selectedStatusMahasiswa = (StatusMahasiswa) (searchstatus.getSelectedItem() == null
				|| searchstatus.getSelectedItem().getValue() == null ? null
						: searchstatus.getSelectedItem().getValue());

		final String filename = Sessions.getCurrent().getWebApp().getRealPath("/tmp/data_nilai_"
				+ URLEncoder.encode(Common.datetimeFormat2s.get().format(ais.ui.util.WaktuUtil.getDate()), "UTF-8") + ".xlsx");

		(file = new File(filename)).createNewFile();

		final Intbox sizedata = new Intbox(30);
		final Label label = Common.displayLoadBar(this, file, center, sizedata);

		new Thread(new Runnable() {

			@Override
			public void run() {
				try {

				XSSFWorkbook workbook = new XSSFWorkbook();

				XSSFSheet sheet = workbook.createSheet("Mahasiswa");
				sheet.setDefaultColumnWidth(18);

				XSSFRow rowhead = sheet.createRow((short) 0);

				rowhead.createCell(0).setCellValue("NIM");
				rowhead.createCell(1).setCellValue("Mulai Semester");
				rowhead.createCell(2).setCellValue("Jenis Pendaftaran");
				rowhead.createCell(3).setCellValue("Jalur Pendaftaran");
				rowhead.createCell(4).setCellValue("Tanggal Pendaftaran");
				rowhead.createCell(5).setCellValue("SKS Diakui");
				rowhead.createCell(6).setCellValue("Asal Perguruan Tinggi");
				rowhead.createCell(7).setCellValue("Asal Program Studi");
				rowhead.createCell(8).setCellValue("Kode Prodi");

				Session session = HibernateUtil.currentNativeSession();

				Criterion criteriaStatus = Restrictions.sqlRestriction("true");
				if (selectedStatusMahasiswa != null) {
					String sql = "this_.id in (select mahasiswa from history_status_mahasiswa where status_mahasiswa="
							+ selectedStatusMahasiswa.getId() + " and tahunakademik = '"
							+ Common.getCurrentTahunAkademik() + "' and semester%2="
							+ (Common.isNowSemensterGanjil() ? 1 : 0) + ")";
					System.out.println("sql=>" + sql);
					criteriaStatus = Restrictions.sqlRestriction(sql);
				}

				List<Mahasiswa> mahasiswas = session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

						.add(criteriaStatus)

						.add(kel != null && !kel.trim().isEmpty()
								? Restrictions.ilike("kelas", kel.trim(), MatchMode.EXACT)
								: Restrictions.sqlRestriction("true"))

						.add(Restrictions.and(
								nimMahasiswa.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
										: Restrictions.ilike("nim", nimMahasiswa.getValue().trim(), MatchMode.ANYWHERE),

								namaMahasiswa.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
										: Restrictions.ilike("nama", namaMahasiswa.getValue().trim(),
												MatchMode.ANYWHERE)))

						.add(searchjurusan.getSelectedItem() == null
								|| searchjurusan.getSelectedItem().getValue() == null
										? Restrictions.sqlRestriction("1=1")
										: Restrictions.eq("jurusan", jurusan))

						.createAlias("jurusan", "jurusan", Criteria.LEFT_JOIN)

						.add(searchfakultas.getSelectedItem() == null
								|| searchfakultas.getSelectedItem().getValue() == null
										? Restrictions.sqlRestriction("1=1")
										: CommonSearchFilterHelper.eqSelectedWithId("jurusan.fakultas", searchfakultas, false))

						.add(searchprogram.getSelectedItem() == null
								|| searchprogram.getSelectedItem().getValue() == null
										? Restrictions.sqlRestriction("1=1")
										: Restrictions.eq("program", searchprogram.getSelectedItem().getValue()))

						.add(searchangkatan.getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("tahunangkatan", searchangkatan.getValue()))

						.addOrder(Order.asc("nim")).list();

				int size = mahasiswas.size();

				int rowIndex = 1;
				for (Mahasiswa mahasiswa : mahasiswas) {

					label.setValue("Sedang memproses data " + mahasiswa.toString() + " ("
							+ Common.numberFormat.get().format(rowIndex * 100.0 / size) + " %)");

					Integer tahun = Integer.parseInt(StringUtils.split(tahunAkademik, "/")[0]);

					Integer currentSemester = Common.getSemester(mahasiswa.getTahunangkatan(), semester,
							mahasiswa.getPindahKeKampusIniMasukSemester(), tahun, mahasiswa.getSemesterMulai());

					KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa, currentSemester, null, null,
							false);

					XSSFRow row = sheet.createRow(rowIndex);

					row.createCell(0).setCellValue(mahasiswa.getNim());
					row.createCell(1).setCellValue(mahasiswa.getTahunangkatan()
							+ (mahasiswa.getSemesterMulai().equals(Perkuliahan.GANJIL) ? "1" : "2"));
					row.createCell(2)
							.setCellValue(mahasiswa.getStatusAwalMahasiswa() == null
									|| mahasiswa.getStatusAwalMahasiswa().getFeeder() == null ? ""
											: mahasiswa.getStatusAwalMahasiswa().getFeeder().toString());
					row.createCell(3).setCellValue(
							mahasiswa.getJenisSeleksi() == null || mahasiswa.getJenisSeleksi().getKode() == null ? ""
									: mahasiswa.getJenisSeleksi().getKode().trim());
					row.createCell(4).setCellValue(mahasiswa.getTanggalMasuk() == null ? ""
							: Common.databaseDateFormat.get().format(mahasiswa.getTanggalMasuk()));
					row.createCell(5).setCellValue(krsMahasiswa.getSksk());
					row.createCell(6).setCellValue(mahasiswa.getPindahanDariKampus());
					row.createCell(7).setCellValue(mahasiswa.getNamaProdiPindah());
					row.createCell(8).setCellValue(mahasiswa.getJurusan().getKodeEpsbed());

					rowIndex++;

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
						} catch (Exception e) {
					// FIX "hang selamanya": try tanpa catch sebelumnya membiarkan exception (mis. gagal
					// query/generate Excel) menembus run() tanpa tertangkap, sehingga label progres
					// tidak pernah diset dan popup progres macet selamanya bagi pengguna.
					Common.tampilErrorJikaAdmin(e);
					label.setValue("Error: " + ais.common.PesanFormalHelper.pesanGagalException(
							"pengambilan data Riwayat Pendaftaran Mahasiswa (History) dari database untuk dikirim ke Neo Feeder",
							null, e,
							new String[] {
									"Periksa kembali data Mahasiswa serta KRS terkait dan coba ulangi.",
									"Pastikan data Mahasiswa dan KRS terkait sudah lengkap dan tersinkron.",
									"Jika kendala berulang, hubungi Administrator Sistem atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini." })
							.replace("\n", " "));
				} finally {
					ais.database.hibernate.HibernateUtil.closeSession();
				}
			}
		}).start();

	}

}
