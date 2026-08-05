package ais.action.master.feeder.integrator.helper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
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
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Jurusan;
import ais.database.model.Perkuliahan;
import ais.database.model.pkl.KelompokPkl;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class DownloadAktifitasMahasiwaPkl extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 790038368339375113L;

	private Center center = new Center();
	private Combobox searchfakultas = new Combobox();

	private Combobox searchjurusan = new Combobox();

	private Combobox searchsemester = new Combobox();
	private Combobox searchtahunakademik = new Combobox();

	private Textbox kelas = new Textbox();

	private File file;

	public DownloadAktifitasMahasiwaPkl() {
		super();
		try {

			Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

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

	public DownloadAktifitasMahasiwaPkl(String title, String border, boolean closable) {
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
		ais.ui.util.ZkCompat.setFlex(north, true);

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
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Kelompok"));
		row.appendChild(kelas);
		kelas.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
		row.appendChild(searchtahunakademik);
		searchtahunakademik.setWidth("90%");
		Common.generateTahunAjaran(searchtahunakademik);
		searchtahunakademik.setReadonly(true);
		
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester"));
		row.appendChild(searchsemester);
		searchsemester.setWidth("90%");
		
		searchsemester.setReadonly(true);

		Toolbar toolbar = new Toolbar();
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
							"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "pkl.xlsx");
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/integrator/helper/DownloadAktifitasMahasiwaPkl.java:186");

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

				XSSFSheet sheet = workbook.createSheet("Template Aktivitas");
				sheet.setDefaultColumnWidth(18);

				XSSFRow rowhead = sheet.createRow((short) 0);

				rowhead.createCell(0).setCellValue("Semester");
				rowhead.createCell(1).setCellValue("Jenis Aktivitas");
				rowhead.createCell(2).setCellValue("Judul");
				rowhead.createCell(3).setCellValue("Lokasi");
				rowhead.createCell(4).setCellValue("Nomor SK Tugas");
				rowhead.createCell(5).setCellValue("Tanggal SK Tugas");
				rowhead.createCell(6).setCellValue("Jenis Anggota");
				rowhead.createCell(7).setCellValue("ID AKTIVITAS");
				rowhead.createCell(8).setCellValue("Kode Prodi");
				rowhead.createCell(9).setCellValue("Program MBKM");
				rowhead.createCell(10).setCellValue("Tanggal Mulai");
				rowhead.createCell(11).setCellValue("Tanggal Selesai");

				Session session = HibernateUtil.currentNativeSession();

				List<KelompokPkl> kelompokPkls = ConstantValues.simpleList(session.createCriteria(KelompokPkl.class)

						.add(kel != null && !kel.trim().isEmpty()
								? Restrictions.ilike("nama_kelompok", kel.trim(), MatchMode.EXACT)
								: Restrictions.sqlRestriction("true"))

						.createAlias("pkl", "pkl")

						.add(semester == null || semester.trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("pkl.semester", semester))

						.add(tahunAkademik == null || tahunAkademik.trim().isEmpty()
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("pkl.tahunAkademik", tahunAkademik))

						.add(jurusan == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("pkl.jurusan", jurusan))

						.add(searchfakultas.getSelectedItem() == null
								|| searchfakultas.getSelectedItem().getValue() == null
								|| searchfakultas.getSelectedItem().getValue() == null
										? Restrictions.sqlRestriction("1=1")
										: CommonSearchFilterHelper.eqSelectedWithId("pkl.fakultas", searchfakultas, false))

						.addOrder(Order.asc("nama_kelompok")), KelompokPkl.class);

				int size = kelompokPkls.size();

				int rowIndex = 1;
				for (KelompokPkl kelompokPkl : kelompokPkls) {

					System.out.println("kelompokPkl.getNama_kelompok() -> " + kelompokPkl.getNama_kelompok());

					String idjenis = kelompokPkl.getPkl().getJenisAktfitasMahasiswa() == null ? "6"
							: kelompokPkl.getPkl().getJenisAktfitasMahasiswa().getFeeder().toString();

					label.setValue("Sedang memproses data " + kelompokPkl.getNama() + " ("
							+ Common.numberFormat.get().format(rowIndex * 100.0 / size) + " %)");

					XSSFRow row = sheet.createRow(rowIndex);

					String id_smt = tahunAkademik.split("/")[0]
							+ (semester.equals(Perkuliahan.SP) ? "3" : semester.equals(Perkuliahan.GENAP) ? "2" : "1");

					XSSFCell cell = row.createCell(0);
					cell.setCellValue(id_smt);

					cell = row.createCell(1);
					cell.setCellValue(idjenis);

					cell = row.createCell(2);
					cell.setCellValue(kelompokPkl.getNama_kelompok());

					cell = row.createCell(3);
					cell.setCellValue(kelompokPkl.getAlamat());

					cell = row.createCell(4);
					cell.setCellValue(kelompokPkl.getNoSk());

					cell = row.createCell(5);
					cell.setCellValue(kelompokPkl.getTglSk() == null ? ""
							: Common.databaseDateFormat.get().format(kelompokPkl.getTglSk()));

					cell = row.createCell(6);
					cell.setCellValue("1");

					cell = row.createCell(7);
					cell.setCellValue(kelompokPkl.getId().toString());

					cell = row.createCell(8);
					cell.setCellValue(kelompokPkl.getPkl() == null || kelompokPkl.getPkl().getJurusan() == null ? ""
							: kelompokPkl.getPkl().getJurusan().getKodeEpsbed());

					cell = row.createCell(9);
					cell.setCellValue("1");

					cell = row.createCell(10);
					cell.setCellValue(kelompokPkl.getTanggal_mulai() == null ? ""
							: Common.databaseDateFormat.get().format(kelompokPkl.getTanggal_mulai()));

					cell = row.createCell(11);
					cell.setCellValue(kelompokPkl.getTanggal_selesai() == null ? ""
							: Common.databaseDateFormat.get().format(kelompokPkl.getTanggal_selesai()));

					rowIndex++;
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

				kelompokPkls.clear();
				label.setValue("");
				} catch (Exception e) {
					// FIX "gagal diam-diam"/"hang selamanya": sebelumnya blok try ini tidak punya catch,
					// sehingga exception (mis. gagal query/parse) menembus keluar run() tanpa pernah
					// men-set label progres, membuat popup progres menggantung selamanya.
					Common.tampilErrorJikaAdmin(e);
					label.setValue("Error: " + ais.common.PesanFormalHelper.pesanGagalException(
							"pengambilan data Aktivitas Mahasiswa PKL untuk Neo Feeder",
							null, e,
							new String[] {
									"Periksa kembali koneksi ke server Neo Feeder (Pengaturan Koneksi) dan coba ulangi.",
									"Pastikan data Kelompok PKL terkait sudah lengkap (jenis aktivitas, SK Tugas, tanggal mulai/selesai).",
									"Jika kendala berulang, hubungi Administrator Sistem atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini." })
							.replace("\n", " "));
							} finally {
					ais.database.hibernate.HibernateUtil.closeSession();
				}
			}
		}).start();

	}

}
