package ais.action.master.epsbed;


import ais.common.CommonSearchFilterHelper;
import java.io.ByteArrayOutputStream;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zss.model.Worksheet;
import org.zkoss.zss.ui.Rect;
import org.zkoss.zss.ui.Spreadsheet;
import org.zkoss.zss.ui.impl.Utils;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Filedownload;
import ais.ui.util.MyGrid;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmuser;
import ais.database.model.epsbed.KapasitasMahasiswaBaru;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class TransaksiKapasitasMahasiswaBaru extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 790038368339375113L;

	private Spreadsheet spreadsheet = new ais.ui.util.MySpreadsheet();
	private Center center = new Center();
	private Combobox tahunakademik = new Combobox();
	private Combobox jenisSemester = new Combobox();
	private Combobox searchfakultas = new Combobox();
	private Combobox searchjurusan = new Combobox();

	public TransaksiKapasitasMahasiswaBaru() {
		super();
		try {

			Common.insertCombo(searchfakultas, new String[] { "nama", "kode" }, Fakultas.class, Restrictions.eq("aktif", true));
			searchfakultas.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					// TODO Auto-generated method stub

					Common.clear(searchjurusan);
					Common.selectComboItem(searchjurusan, null);
					Common.insertCombo(searchjurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class, Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
							CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false));
					initSpreadsheet();
				}
			});
			init();
			initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
		}
	}

	public TransaksiKapasitasMahasiswaBaru(String title, String border, boolean closable) {
		super(title, border, closable);
		try {
			init();
			initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
		}
	}

	private void init() throws Exception {

		Common.generateTahunAjaran(tahunakademik);
		tahunakademik.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				// TODO Auto-generated method stub
				initSpreadsheet();
			}
		});

		jenisSemester = new Combobox();
		org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
		comboitem.setValue(Perkuliahan.GANJIL);
		comboitem.setLabel(Perkuliahan.GANJIL);
		jenisSemester.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setValue(Perkuliahan.GENAP);
		comboitem.setLabel(Perkuliahan.GENAP);
		jenisSemester.appendChild(comboitem);
		Common.selectComboItem(jenisSemester, Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP);

		searchfakultas = new Combobox();
		searchjurusan = new Combobox();
		Common.insertCombo(searchfakultas, new String[] { "nama", "kode" }, Fakultas.class, Restrictions.eq("aktif", true));

		searchfakultas.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				// TODO Auto-generated method stub

				Common.clear(searchjurusan);
				Common.selectComboItem(searchjurusan, null);
				Common.insertCombo(searchjurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class, Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
						CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false));
				initSpreadsheet();
			}
		});
		searchjurusan.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				// TODO Auto-generated method stub
				initSpreadsheet();
			}
		});

		jenisSemester.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				// TODO Auto-generated method stub
				initSpreadsheet();
			}
		});

		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser.ambilFakultas() != null) {
			Common.selectComboItem(searchfakultas, tbmuser.ambilFakultas());
			if (tbmuser.ambilJurusan() != null) {
				Common.selectComboItem(searchjurusan, tbmuser.ambilJurusan());
			}
		}

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

		Row row = new Row();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
		row.appendChild(tahunakademik);
		tahunakademik.setWidth("90%");

		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(searchfakultas);
		searchfakultas.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Semester"));
		row.appendChild(jenisSemester);
		jenisSemester.setWidth("90%");
		row.appendChild(new ais.ui.util.MyLabelConfig("Program Studi"));
		row.appendChild(searchjurusan);
		searchjurusan.setWidth("90%");

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

		MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Export Epsbed (TRKAP.xls)", "/img/excel.png");
		print.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				try {
					ByteArrayOutputStream bout = new ByteArrayOutputStream();
					spreadsheet.getBook().write(bout);
					bout.close();
					Filedownload.save(bout.toByteArray(), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "TRKAP.xlsx");
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/epsbed/TransaksiKapasitasMahasiswaBaru.java:222");

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

		if (tahunakademik.getSelectedItem() == null) {
			MyMessageboxConfig.show("Tahun Akademik harus diisi");
			return;
		}

		if (jenisSemester.getSelectedItem() == null) {
			MyMessageboxConfig.show("Jenis Semester harus diisi");
			return;
		}

		Common.clear(center);
		Jurusan jurusan = null;
		if (searchjurusan.getSelectedItem() != null) {
			jurusan = (Jurusan) searchjurusan.getSelectedItem().getValue();
		}

		List<KapasitasMahasiswaBaru> kapasitasMahasiswabaru = HibernateUtil.currentSession()
				.createCriteria(KapasitasMahasiswaBaru.class)
				.add(Restrictions.eq("tahunAkademik", tahunakademik.getSelectedItem().getValue()))
				.add(Restrictions.eq("ganjilGenap", jenisSemester.getSelectedItem().getValue()))

		.createAlias("jurusan", "jurusan")
				.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null || searchjurusan.getSelectedItem().getValue()==null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.eq("jurusan.id", jurusan.getId()),
								CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false)))
				.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null || searchfakultas.getSelectedItem().getValue()==null ? Restrictions.sqlRestriction("1=1")
						: CommonSearchFilterHelper.eqSelectedWithId("jurusan.fakultas", searchfakultas, false))

		.list();
		System.out.println("epsbed transaksi kapasitas mahasiswa baru : " + kapasitasMahasiswabaru.size());
		PerguruanTinggi perguruanTinggi = (PerguruanTinggi) HibernateUtil.currentSession()
				.createCriteria(PerguruanTinggi.class).setMaxResults(1).uniqueResult();

		spreadsheet = new ais.ui.util.MySpreadsheet();
		spreadsheet.setParent(center);
		spreadsheet.setWidth("100%");
		spreadsheet.setHeight("100%");
		spreadsheet.setSrc("../../WEB-INF/rowcolumn.xlsx");
		spreadsheet.setMaxcolumns(50);
		spreadsheet.setMaxrows(kapasitasMahasiswabaru.size() + 1);

		Worksheet sheet = spreadsheet.getSelectedSheet();
		sheet.setDefaultColumnWidth(30);

		int rowIndex = 0;
		int colIndex = 0;

		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, "THSMSTRKA");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1, "KDPTITRKA");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 2, "KDJENTRKA");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 3, "KDPSTTRKA");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 4, "JMGETTRKA");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 5, "JMCALTRKA");

		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 6, "JMTERTRKA");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 7, "JMDAFTRKA");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 8, "JMMUNTRKA");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 9, "JMPINTRKA");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 10, "TGAW1TRKA");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 11, "TGAK1TRKA");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 12, "TMRE1TRKA");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 13, "TGAW2TRKA");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 14, "TGAK2TRKA");

		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 15, "TMRE2TRKA");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 16, "MTKLHTRKA");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 17, "KDEKSTRKA");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 18, "MTKLETRKA");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 19, "SMPDKTRKA");

		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 20, "JMPDKTRKA");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 21, "MTPDKTRKA");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 22, "JMHLKTRKA");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 23, "JMHPRTRKA");

		ais.ui.util.EcampusUtil.setBold(sheet, new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex), true);

		rowIndex++;

		for (KapasitasMahasiswaBaru kapasitasMhsBaru : kapasitasMahasiswabaru) {
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0,
					CommonEpsbed.getTahunSemesterPelaporan((String) tahunakademik.getSelectedItem().getValue(),
							(String) jenisSemester.getSelectedItem().getValue()));
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1,
					perguruanTinggi == null || perguruanTinggi.getId() == null ? "" : perguruanTinggi.getKodePerguruanTinggi());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 2,
					kapasitasMhsBaru.getJurusan().getJenjang().getJenjangEpsbed());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 3, kapasitasMhsBaru.getJurusan() == null
					? kapasitasMhsBaru.getJurusan().getKodeEpsbed() : kapasitasMhsBaru.getJurusan().getKodeEpsbed());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 4,
					kapasitasMhsBaru.getJumlahTargetMahasiswaBaru() == null ? ""
							: kapasitasMhsBaru.getJumlahTargetMahasiswaBaru().toString());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 5, kapasitasMhsBaru.getJumlahPendaftar() == null ? ""
					: kapasitasMhsBaru.getJumlahPendaftar().toString());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 6, kapasitasMhsBaru.getJumlahLulus().toString());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 7,
					kapasitasMhsBaru.getJumlahDaftarUlang().toString());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 8, kapasitasMhsBaru.getJumlahMundur().toString());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 9, kapasitasMhsBaru.getJumlahPindahan() == null ? ""
					: kapasitasMhsBaru.getJumlahPindahan().toString());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 10,
					kapasitasMhsBaru.getAwalPerkuliahanGanjil() == null ? ""
							: CommonEpsbed.dateFormatEpsbed.format(kapasitasMhsBaru.getAwalPerkuliahanGanjil()));
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 11,
					kapasitasMhsBaru.getAkhirPerkuliahanGanjil() == null ? ""
							: CommonEpsbed.dateFormatEpsbed.format(kapasitasMhsBaru.getAkhirPerkuliahanGanjil()));
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 12,
					kapasitasMhsBaru.getJumlahMingguKuliahGanjil() == null ? ""
							: kapasitasMhsBaru.getJumlahMingguKuliahGanjil().toString());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 13, kapasitasMhsBaru.getAwalPerkuliahanGenap() == null
					? "" : CommonEpsbed.dateFormatEpsbed.format(kapasitasMhsBaru.getAwalPerkuliahanGenap()));
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 14,
					kapasitasMhsBaru.getAkhirPerkuliahanGenap() == null ? ""
							: CommonEpsbed.dateFormatEpsbed.format(kapasitasMhsBaru.getAkhirPerkuliahanGenap()));
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 15,
					kapasitasMhsBaru.getJumlahMingguKuliahGenap() == null ? ""
							: kapasitasMhsBaru.getJumlahMingguKuliahGenap().toString());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 16, kapasitasMhsBaru.getMetodeHariPerkuliahan());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 17,
					kapasitasMhsBaru.getMetodeHariPerkuliahanEkstensi());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 18, "");
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 19, kapasitasMhsBaru.getAdaSP());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 20,
					kapasitasMhsBaru.getJumlahSP() == null ? "" : kapasitasMhsBaru.getJumlahSP().toString());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 21, kapasitasMhsBaru.getMetodePelaksanaanSP());

			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 22, "");
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 23, "");

			rowIndex++;
		}

		Common.setStyled(sheet);spreadsheet.setMaxrows(rowIndex + 1);

		// Tampilkan sebagai grid ringan; Excel tetap utuh saat tombol Download diklik.
		ais.ui.util.PratinjauXlsxHelper.gantiSpreadsheetDenganGrid(spreadsheet);
	}
}
