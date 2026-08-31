package ais.action.master;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;

import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.dao.BiodataCalonMahasiswaDao;
import ais.database.dao.DaoFactory;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.StatusLulusCalonMahasiswa;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyWindow;

/**
 * Controller/action ZK untuk update biodata calon mahasiswa baru. Tipe ini merupakan titik masuk
 * UI yang menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus
 * oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow window}, {@code String
 * namaFile}, {@code MyGrid grids}; inisialisasi/lifecycle ({@code doBeforeCompose()}, {@code doAfterCompose()},
 * {@code init()}); pembacaan/pencarian ({@code loadDataCalonMahasiswaLulus()}, {@code getNamaFile()}); mutasi
 * data ({@code setNamaFile()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut
 * di atas.</p>
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
public class UpdateBiodataCalonMahasiswaBaruAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4604126575556546716L;
	private MyWindow window;
	private String namaFile;
	private MyGrid grids;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		Common.initLaguage();
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		Common.initLaguage();
		if (session.getAttribute("usersTemp") == null) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		init();
		// initData(null);

	}

	private void init() throws InterruptedException, IOException {
		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(window);
		panel.setTitle("Cek Calon Mahasiswa yang lulus ujian");
		panel.setWidth("100%");
		panel.setHeight("100%");

		Panelchildren panelchildren = new Panelchildren();
		panelchildren.setParent(panel);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(panelchildren);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(center);

		North north = new North();
		north.setParent(borderlayout);

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(north);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);
		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("10%");
		column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("90%");

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Upload File"));

		/*
		 * Toolbar toolbar = new Toolbar();// toolbar.setHeight("25px");
		 * toolbar.setParent(south);
		 */
		MyButtonConfig button = new MyButtonConfig("Browse" + Common.ukuranLabelFileUpload(), "");
		button.setUpload(Common.ukuranFileUpload());
		button.addEventListener("onUpload", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {

				UploadEvent uploadEvent = (UploadEvent) event;
				Session session = Common.getManualSession();
				Media media = uploadEvent.getMedia();if(!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media))return;

				// Blob blob = new javax.sql.rowset.serial.SerialBlob(inputStream);
				/*
				 * LogCsvFileUpload logCsvFileUpload = new LogCsvFileUpload();
				 * logCsvFileUpload.setFileContent(blob); logCsvFileUpload.setMimeType(media
				 * .getContentType()); logCsvFileUpload.setFileName(media.getName());
				 * logCsvFileUpload.setUploadDate(ais.ui.util.WaktuUtil.getDate());
				 * session.save(logCsvFileUpload);
				 */

				try {

					// File blobFile = new File(Common.folderFoto +
					// fotobm.getNamaFile());
					// File blobFile = new File(strDirectoy + "/"
					// +media.getName());
					// FileOutputStream outStream = new
					// FileOutputStream(blobFile);
					// InputStream inStream = blob.// getBinaryStream();
					//

					/*
					 * CekCalonMahasiswaAction.this.setNamaFile(media.getName
					 * ());System.out.println("Nama File in = "+
					 * CekCalonMahasiswaAction.this.getNamaFile());
					 */

					ais.common.UploadReportHelper report = new ais.common.UploadReportHelper("Upload Biodata Calon Mahasiswa");
					XSSFWorkbook workbook = new XSSFWorkbook(media.getStreamData());
					XSSFSheet sheet = workbook.getSheetAt(0);

					try {
						int i = 4;
						while (true) {
							String a1 = Common.getCellContent(Common.getCell(sheet, 2, i));
							String b2 = Common.getCellContent(Common.getCell(sheet, 3, i));
							String c2 = Common.getCellContent(Common.getCell(sheet, 4, i));

							Integer integer = ((Number) session.createCriteria(StatusLulusCalonMahasiswa.class)
									.setProjection(Projections.rowCount()).add(Restrictions.eq("noUjian", b2))
									.uniqueResult()).intValue();

							// System.out.println("Integer ke- " + i);
							i++;
							if (!integer.equals(0))
								continue;

							StatusLulusCalonMahasiswa statusLulusCalonMahasiswa = new StatusLulusCalonMahasiswa();

							statusLulusCalonMahasiswa.setProgramStudi(a1);
							statusLulusCalonMahasiswa.setNoUjian(b2);
							statusLulusCalonMahasiswa.setNama(c2);

							session.save(statusLulusCalonMahasiswa);
							report.sukses(i, b2 + "/" + c2, "");

						}
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);
						if (!(e instanceof IndexOutOfBoundsException)) {
							report.gagal(-1, "upload loop", e, "Loop terminated prematurely");
						}
					}

					/*
					 * String stringa1 = a1.getStringCellValue(); String stringb2 =
					 * b2.getStringCellValue(); String stringc2 = c2.getStringCellValue();
					 * 
					 * System.out.println("String a1 : " + stringa1);
					 * System.out.println("String b2 : " + stringb2);
					 * System.out.println("String c2 : " + stringc2);
					 */

					try { org.zkoss.zul.Filedownload.save(report.simpanLaporan(), "text/plain"); }
					catch (Exception eDl) { ais.common.ErrorAuditUtil.record(eDl, "auto-audit(empty-catch) download laporan UpdateBiodataCalonMahasiswaBaru"); }

					MyMessageboxConfig.show("Upload File CSV berhasil, "
							+ "silahkan cek kelulusan calon mahasiswa dengan meng-klik button Check Kelulusan dibawah ini"
							+ report.getRingkasan(),
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);

				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
					System.out.println("ERROR(djv_exportBlob) Unable to export: " + media.getName());

					MyMessageboxConfig.show("Upload File CSV gagal", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
				}

			}

		});
		button.setParent(row);

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);
		button = new MyButtonConfig("Check Kelulusan", "");
		button.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {
				// TODO Auto-generated method stub
				Session session = HibernateUtil.currentSession();

				BiodataCalonMahasiswaDao biodataCalonMahasiswaDao = DaoFactory.getInstance()
						.getBiodataCalonMahasiswaDao();

				List<StatusLulusCalonMahasiswa> statusLulusCalonMahasiswas = session
						.createCriteria(StatusLulusCalonMahasiswa.class).addOrder(Order.desc("id")).list();

				try {
					List<BiodataCalonMahasiswa> calonMahasiswaLulus = new ArrayList<BiodataCalonMahasiswa>();
					for (StatusLulusCalonMahasiswa st : statusLulusCalonMahasiswas) {

						/*
						 * Integer integer = ((Number) session.createCriteria(
						 * BiodataCalonMahasiswa.class).setProjection( Projections.rowCount()).add(
						 * Restrictions.eq("noUjian", st.getNoUjian())) .uniqueResult()).intValue();
						 */

						BiodataCalonMahasiswa biodataCalonMahasiswa = (BiodataCalonMahasiswa) session
								.createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
								.add(Restrictions.eq("noUjian", st.getNoUjian().trim())).setMaxResults(1)
								.uniqueResult();

						// System.out.println("data ke-"+i+" nomor ujian: "+st.getNoUjian()+"
						// biodataCalonMahasiswa = "+biodataCalonMahasiswa);

						if (biodataCalonMahasiswa != null) {

							// System.out.println("==================== Biodata Calon Mahasiswa Tidak Sama
							// dengan Null ====================");

							biodataCalonMahasiswa.setStatusLulus(BiodataCalonMahasiswa.LULUS);

							Common.refreshUpdate(session, (biodataCalonMahasiswa));

							calonMahasiswaLulus.add(biodataCalonMahasiswa);

							loadDataCalonMahasiswaLulus(calonMahasiswaLulus);

						} else {
							BiodataCalonMahasiswa biCalonMahasiswa = new BiodataCalonMahasiswa();

							biCalonMahasiswa.setNama(st.getNama());
							biCalonMahasiswa.setNoUjian(st.getNoUjian());
							biCalonMahasiswa.setStatusLulus(BiodataCalonMahasiswa.LULUS);

							biodataCalonMahasiswaDao.save(biCalonMahasiswa);

							calonMahasiswaLulus.add(biCalonMahasiswa);

							loadDataCalonMahasiswaLulus(calonMahasiswaLulus);
						}
					}

				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}

				MyMessageboxConfig.show("Cek Status Lulus mahasiswa selesai", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.INFORMATION);
			}
		});
		button.setParent(south);

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		center.setTitle("Daftar Calon Mahasiswa yang sudah dinyatakan Lulus");

		grids = new MyGrid();
		grids.setParent(center);
		grids.setWidth("100%");
		grids.setHeight("100%");
		grids.setOddRowSclass("non-odd");

		columns = new Columns();
		columns.setParent(grids);
		column = new MyColumnConfig();
		column.setLabel("Nama");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setLabel("No.Registrasi");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setLabel("No.Ujian");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setLabel("Status Kelulusan");
		column.setParent(columns);

	}

	@SuppressWarnings({ "rawtypes" })
	public void loadDataCalonMahasiswaLulus(List calonMahasiswaLulus) {

		ListModel strset = new SimpleListModel(calonMahasiswaLulus);
		grids.setRowRenderer(new CalonMahasiswaLulusRenderer());
		grids.setModelCheckMobile(strset);

		grids.renderAll();
		grids.setOddRowSclass("non-odd");
		// grids.setPageSize(8);

	}

	/**
	 * Renderer lokal untuk layar/komponen {@link UpdateBiodataCalonMahasiswaBaruAction}. Kelas ini menerjemahkan
	 * satu item data menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link UpdateBiodataCalonMahasiswaBaruAction} dan
	 * dapat mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see UpdateBiodataCalonMahasiswaBaruAction
	 */
	class CalonMahasiswaLulusRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final BiodataCalonMahasiswa biodataCalonMahasiswa = (BiodataCalonMahasiswa) arg1;

			new Label(biodataCalonMahasiswa.getNama()).setParent(arg0);
			new Label(biodataCalonMahasiswa.getNoRegistrasi()).setParent(arg0);
			new Label(biodataCalonMahasiswa.getNoUjian()).setParent(arg0);
			new Label(biodataCalonMahasiswa.getStatusLulus().equals(1) ? "Lulus" : "").setParent(arg0);

		}

	}

	public String getNamaFile() {
		return namaFile;
	}

	public void setNamaFile(String namaFile) {
		this.namaFile = namaFile;
	}

}
