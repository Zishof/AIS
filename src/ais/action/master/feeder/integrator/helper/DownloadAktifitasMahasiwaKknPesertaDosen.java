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
import ais.database.model.Dosen;
import ais.database.model.Jurusan;
import ais.database.model.Mahasiswa;
import ais.database.model.MahasiswaDapatKelompokKkn;
import ais.database.model.Perkuliahan;
import ais.database.model.kkn.KelompokKkn;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Tipe khusus untuk download aktifitas mahasiwa kkn peserta dosen. Kelas ini memberi nama dan
 * batas tanggung jawab yang eksplisit pada perilaku yang diwarisi atau kontrak yang
 * diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Center center}, {@code Combobox
 * searchfakultas}, {@code Combobox searchjurusan}, {@code Combobox searchsemester}, {@code Combobox
 * searchtahunakademik}, {@code Textbox kelas}, {@code File file}; inisialisasi/lifecycle ({@code init()}, {@code
 * initSpreadsheet()}); konfigurasi constructor: {@code comboitem}. Bagian lain dari kontrak tetap mengikuti
 * kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class DownloadAktifitasMahasiwaKknPesertaDosen extends MyWindow {

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

	public DownloadAktifitasMahasiwaKknPesertaDosen() {
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

	public DownloadAktifitasMahasiwaKknPesertaDosen(String title, String border, boolean closable) {
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
		borderlayout.setWidth("100%");
		borderlayout.setHeight("100%");
		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, false);
		north.setHeight("260px");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(searchfakultas);
		searchfakultas.setWidth("90%");
		searchfakultas.setReadonly(true);

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(searchjurusan);
		searchjurusan.setWidth("90%");
		searchjurusan.setReadonly(true);

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Kelompok"));
		row.appendChild(kelas);
		kelas.setWidth("90%");

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
		row.appendChild(searchtahunakademik);
		searchtahunakademik.setWidth("90%");
		Common.generateTahunAjaran(searchtahunakademik);
		searchtahunakademik.setReadonly(true);
		
		row = new MyFormRow();
		row.setValign("top");
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
							"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "akm.xlsx");
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/integrator/helper/DownloadAktifitasMahasiwaKknPesertaDosen.java:189");

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
				rowhead.createCell(1).setCellValue("ID AKTIVITAS");
				rowhead.createCell(2).setCellValue("NIDN/ID Dosen NEO");
				rowhead.createCell(3).setCellValue("Nama Dosen");
				rowhead.createCell(4).setCellValue("Jenis Peran");
				rowhead.createCell(5).setCellValue("Urutan Bimbing/Uji");
				rowhead.createCell(6).setCellValue("Kategori Kegiatan");
				rowhead.createCell(7).setCellValue("Kode Prodi");

				Session session = HibernateUtil.currentNativeSession();

				List<KelompokKkn> kelompokKkns = ConstantValues.simpleList(session.createCriteria(KelompokKkn.class)

						.add(kel != null && !kel.trim().isEmpty()
								? Restrictions.ilike("nama_kelompok", kel.trim(), MatchMode.EXACT)
								: Restrictions.sqlRestriction("true"))

						.createAlias("kkn", "kkn")

						.add(semester == null || semester.trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("kkn.semester", semester))

						.add(tahunAkademik == null || tahunAkademik.trim().isEmpty()
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("kkn.tahunAkademik", tahunAkademik))

						.add(jurusan == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("kkn.jurusan", jurusan))

						.add(searchfakultas.getSelectedItem() == null
								|| searchfakultas.getSelectedItem().getValue() == null
								|| searchfakultas.getSelectedItem().getValue() == null
										? Restrictions.sqlRestriction("1=1")
										: CommonSearchFilterHelper.eqSelectedWithId("kkn.fakultas", searchfakultas, false))

						.addOrder(Order.asc("nama_kelompok")), KelompokKkn.class);

				int size = kelompokKkns.size();

				int rowIndex = 1;
				for (KelompokKkn kelompokKkn : kelompokKkns) {
					int pembimbing = 1;
					for (Dosen dosen : kelompokKkn.populateDosenBuNama()) {
						System.out.println("kelompokKkn.getNama_kelompok() -> " + kelompokKkn.getNama_kelompok());

						label.setValue("Sedang memproses data " + kelompokKkn.getNama() + " ("
								+ Common.numberFormat.get().format(rowIndex * 100.0 / size) + " %)");

						XSSFRow row = sheet.createRow(rowIndex);

						String id_smt = tahunAkademik.split("/")[0] + (semester.equals(Perkuliahan.SP) ? "3"
								: semester.equals(Perkuliahan.GENAP) ? "2" : "1");

						XSSFCell cell = row.createCell(0);
						cell.setCellValue(id_smt);

						cell = row.createCell(1);
						cell.setCellValue(kelompokKkn.getId().toString());

						cell = row.createCell(2);
						cell.setCellValue(dosen.getNidn());

						cell = row.createCell(3);
						cell.setCellValue(dosen.getNama());

						cell = row.createCell(4);
						cell.setCellValue(1);

						cell = row.createCell(5);
						cell.setCellValue(pembimbing);

						cell = row.createCell(6);
						cell.setCellValue("110300");

						cell = row.createCell(7);
						cell.setCellValue(kelompokKkn.getKkn().getJurusan() == null ? ""
								: kelompokKkn.getKkn().getJurusan().getKodeEpsbed());

						rowIndex++;
						pembimbing++;
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

				kelompokKkns.clear();
				label.setValue("");
							} catch (Exception e) {
					// FIX "gagal diam-diam" / hang selamanya: sebelumnya try di sini TIDAK punya catch,
					// sehingga exception (mis. gagal query/generate Excel) menembus run() tanpa
					// tertangani -> thread mati & label.setValue("") tak pernah tercapai, progress bar
					// tak pernah selesai (popup menggantung selamanya di sisi user).
					ais.common.Common.tampilErrorJikaAdmin(e);
					label.setValue("Error: " + ais.common.PesanFormalHelper.pesanGagalException(
							"pengambilan data Peserta Dosen KKN dari database untuk diekspor ke Neo Feeder",
							null, e,
							new String[] {
									"Periksa kembali data dan filter yang dipilih (Fakultas/Prodi/Nama Kelompok/TA/Semester), lalu coba ulangi.",
									"Pastikan data Dosen pembimbing/penguji KKN terkait sudah lengkap (NIDN, nama).",
									"Jika kendala berulang, hubungi Administrator Sistem atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini." })
							.replace("\n", " "));
				} finally {
					ais.database.hibernate.HibernateUtil.closeSession();
				}
			}
		}).start();

	}

}
