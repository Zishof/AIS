package ais.action.master.epsbed;


import ais.common.CommonSearchFilterHelper;
import java.io.ByteArrayOutputStream;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.criterion.Order;
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
import ais.ui.util.MyComboitemConfig;
import org.zkoss.zul.Div;
import org.zkoss.zul.Filedownload;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Label;
import ais.ui.util.MyMessageboxConfig;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmuser;
import ais.database.model.epsbed.EpsbedPublikasiDosen;

/**
 * Tipe khusus untuk transaksi publikasi dosen. Kelas ini memberi nama dan batas tanggung jawab
 * yang eksplisit pada perilaku yang diwarisi atau kontrak yang diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Spreadsheet spreadsheet}, {@code Center
 * center}, {@code Combobox tahunakademik}, {@code Combobox jenisSemester}, {@code Combobox searchfakultas},
 * {@code Combobox searchjurusan}; inisialisasi/lifecycle ({@code init()}, {@code initSpreadsheet()}). Bagian
 * lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class TransaksiPublikasiDosen extends MyWindow {

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

	public TransaksiPublikasiDosen() {
		super();
		try {
			init();
			// initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
		}
	}

	public TransaksiPublikasiDosen(String title, String border, boolean closable) {
		super(title, border, closable);
		try {
			init();
			// initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
		}
	}

	private void init() throws Exception {

		Common.generateTahunAjaran(tahunakademik);

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
				// initSpreadsheet();
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
				initSpreadsheet();
			}
		});
		search.setParent(toolbar);

		MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Export Epsbed (TRPUD.xls)", "/img/excel.png");
		print.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				try {
					ByteArrayOutputStream bout = new ByteArrayOutputStream();
					spreadsheet.getBook().write(bout);
					bout.close();
					Filedownload.save(bout.toByteArray(), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "TRPUD.xlsx");
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/epsbed/TransaksiPublikasiDosen.java:183");

				}
			}
		});
		print.setParent(toolbar);

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		initSpreadsheet();
	}

	@SuppressWarnings("unchecked")
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
		final Label label = Common.displayLoadBar(this);
		final List<EpsbedPublikasiDosen> epsbedPublikasiDosens = HibernateUtil.currentSession()
				.createCriteria(EpsbedPublikasiDosen.class).addOrder(Order.asc("dosen")).createCriteria("dosen")
				.createAlias("jurusan", "jurusan")
				
				.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null || searchjurusan.getSelectedItem().getValue()==null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.eq("jurusan.id", jurusan.getId()),
								CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false)))
				.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null || searchfakultas.getSelectedItem().getValue()==null ? Restrictions.sqlRestriction("1=1")
						: CommonSearchFilterHelper.eqSelectedWithId("jurusan.fakultas", searchfakultas, false))

		.list();

		System.out.println("epsbed Transaksi publikasi dosen  :" + epsbedPublikasiDosens.size());

		final PerguruanTinggi perguruanTinggi = (PerguruanTinggi) HibernateUtil.currentSession()
				.createCriteria(PerguruanTinggi.class).setMaxResults(1).uniqueResult();

		spreadsheet = new ais.ui.util.MySpreadsheet();
		spreadsheet.setParent(center);
		spreadsheet.setWidth("100%");
		spreadsheet.setHeight("100%");
		spreadsheet.setSrc("../../WEB-INF/rowcolumn.xlsx");
		spreadsheet.setMaxcolumns(30);
		spreadsheet.setMaxrows(epsbedPublikasiDosens.size() + 1);

		final Worksheet sheet = spreadsheet.getSelectedSheet();
		sheet.setDefaultColumnWidth(30);

		int rowIndex = 0;
		int colIndex = 0;

		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, "THSMSTRPU");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1, "KDPTITRPU");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 2, "KDJENTRPU");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 3, "KDPSTTRPU");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 4, "NODOSTRPU");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 5, "NORUTTRPU");

		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 6, "KDLITTRPU");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 7, "KDPUBTRPU");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 8, "KDATHTRPU");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 9, "KDKELTRPU");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 10, "THNBLTRPU");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 11, "KDBIYTRPU");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 12, "JMBIYTRPU");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 13, "JUDU1TRPU");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 14, "JUDU2TRPU");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 15, "JUDU3TRPU");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 16, "JUDU4TRPU");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 17, "JUDU5TRPU");

		ais.ui.util.EcampusUtil.setBold(sheet, new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex), true);

		// rowIndex++;
		//
		//
		// new Thread(new Runnable() {
		//
		// @Override
		// public void run() {
		// try {
		// Thread.sleep(500);
		// } catch (Exception e1) { ais.common.ErrorAuditUtil.record(e1, "auto-audit(empty-catch) src/ais/action/master/epsbed/TransaksiPublikasiDosen.java:278");
		// // TODO Auto-generated catch block
		// e1.printStackTrace();
		// }
		rowIndex = 1;
		for (EpsbedPublikasiDosen epsbedPublikasiDosen : epsbedPublikasiDosens) {

			label.setValue("Sedang memproses data " + epsbedPublikasiDosen.toString() + " ("
					+ Common.numberFormat.get().format(rowIndex * 100.0 / epsbedPublikasiDosens.size()) + " %)");

			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0,
					CommonEpsbed.getTahunSemesterPelaporan((String) tahunakademik.getSelectedItem().getValue(),
							(String) jenisSemester.getSelectedItem().getValue()));
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1,
					perguruanTinggi == null || perguruanTinggi.getId() == null ? "" : perguruanTinggi.getKodePerguruanTinggi());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 2,
					epsbedPublikasiDosen.getDosen().getJurusan() == null ? ""
							: epsbedPublikasiDosen.getDosen().getJurusan() == null
									? epsbedPublikasiDosen.getDosen().getJurusan().getJenjang().getJenjangEpsbed()
									: epsbedPublikasiDosen.getDosen().getJurusan().getJenjang().getJenjangEpsbed());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 3,
					epsbedPublikasiDosen.getDosen().getJurusan() == null ? ""
							: epsbedPublikasiDosen.getDosen().getJurusan() == null
									? epsbedPublikasiDosen.getDosen().getJurusan().getKodeEpsbed()
									: epsbedPublikasiDosen.getDosen().getJurusan().getKodeEpsbed());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 4,
					epsbedPublikasiDosen.getDosen().getNidn() == null ? "" : epsbedPublikasiDosen.getDosen().getNidn());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 5, epsbedPublikasiDosen.getUrut() + "");
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 6,
					epsbedPublikasiDosen.getKodeJenisPenelitian().getKode());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 7,
					epsbedPublikasiDosen.getKodeMediaPublikasi().getKode());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 8, epsbedPublikasiDosen.getKodeAuthor().getKode());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 9,
					epsbedPublikasiDosen.getKodeKegiatanMandiriKelompok());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 10,
					epsbedPublikasiDosen.getTahunPublikasi() + "" + epsbedPublikasiDosen.getBulanPublikasi());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 11,
					epsbedPublikasiDosen.getKodePembiayaan().getKode());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 12, epsbedPublikasiDosen.getJumlahBiaya());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 13, epsbedPublikasiDosen.getJudul1());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 14, epsbedPublikasiDosen.getJudul2());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 15, epsbedPublikasiDosen.getJudul3());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 16, epsbedPublikasiDosen.getJudul4());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 17, epsbedPublikasiDosen.getJudul5());

			rowIndex++;
		}

		epsbedPublikasiDosens.clear();
		label.setValue("");
		// }
		// }).start();

		// Tampilkan sebagai grid ringan; Excel tetap utuh saat tombol Download diklik.
		ais.ui.util.PratinjauXlsxHelper.gantiSpreadsheetDenganGrid(spreadsheet);
	}
}
