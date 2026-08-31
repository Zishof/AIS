package ais.action.master;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;

import org.hibernate.Session;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Center;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Textbox;

import ais.action.master.helper.AmbilDataTemplateQueryBanbox;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.TemplateQuery;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyWindow;

/**
 * Controller/action ZK untuk execute template query. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code AmbilDataTemplateQueryBanbox
 * templateQuery}, {@code Textbox query}, {@code File file}, {@code Center center}, {@code MyWindow window};
 * inisialisasi/lifecycle ({@code doBeforeCompose()}, {@code doAfterCompose()}); pembacaan/pencarian ({@code
 * onSearchDefault()}, {@code onDownloadData()}); operasi domain lain ({@code onExecute()}). Bagian lain dari
 * kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
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
public class ExecuteTemplateQueryAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;

	private AmbilDataTemplateQueryBanbox templateQuery;

	private Textbox query;

	private File file;
	private Center center;
	private MyWindow window;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(
			org.zkoss.zk.ui.Page page, org.zkoss.zk.ui.Component parent,
			org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		Common.initLaguage();
		if (session.getAttribute("usersTemp") == null
				|| !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		templateQuery.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault();
			}
		});
	}

	public void onSearchDefault() {
		TemplateQuery templateQuery = (TemplateQuery) this.templateQuery
				.getAttribute("templateQuery");
		if (templateQuery == null) {
			return;
		}

		query.setText(templateQuery.getQuery());

	}

	public void onDownloadData(Event event) throws Exception {
		if (file == null) {
			MyMessageboxConfig.show("Click \"Execute\" terlebih dahulu", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return;
		}

		TemplateQuery templateQuery = (TemplateQuery) ExecuteTemplateQueryAction.this.templateQuery
				.getAttribute("templateQuery");
		String fileName = "template_query_" + templateQuery.getNama() + ".xlsx";
		fileName = fileName.replaceAll(" ", "_");
		try {
			Filedownload.save(new FileInputStream(file),
					"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", fileName);
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

	}

	@SuppressWarnings("unchecked")
	public void onExecute(Event event) throws Exception {
		final String q = query.getValue();
		if (q.trim().equals("")) {
			MyMessageboxConfig.show("Pilih salah satu template query", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return;
		}

		if (query.getValue().trim().toLowerCase().contains("update")) {
			MyMessageboxConfig.show("Isi Template Query tidak boleh ada kata update",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return;
		}

		if (query.getValue().trim().toLowerCase().contains("delete")) {
			MyMessageboxConfig.show("Isi Template Query tidak boleh ada kata delete",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return;
		}

		if (query.getValue().trim().toLowerCase().contains("truncate")) {
			MyMessageboxConfig.show("Isi Template Query tidak boleh ada kata truncate",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return;
		}

		if (query.getValue().trim().toLowerCase().contains("drop")) {
			MyMessageboxConfig.show("Isi Template Query tidak boleh ada kata drop",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return;
		}

		if (query.getValue().trim().toLowerCase().contains("alter")) {
			MyMessageboxConfig.show("Isi Template Query tidak boleh ada kata alter",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return;
		}

		final String filename = Sessions
				.getCurrent()
				.getWebApp()
				.getRealPath(
						"/tmp/data_"
								+ URLEncoder.encode(
										Common.dateFormat7.get().format(ais.ui.util.WaktuUtil.getDate()),
										"UTF-8") + ".xlsx");

		(file = new File(filename)).createNewFile();
		final Intbox sizedata = new Intbox(30);
		final Label label = Common.displayLoadBar(window, file, center,
				sizedata);
		new Thread(new Runnable() {

			@Override
			public void run() {
				try {

				XSSFWorkbook workbook = new XSSFWorkbook();
				XSSFSheet sheet = workbook.createSheet("DATA");
				sheet.setDefaultColumnWidth(20);

				Session session = HibernateUtil.currentNativeSession();
				List<Object[]> objects = session.createSQLQuery(q).list();

				int i = 0;
				try {
					for (Object[] myObjects : objects) {
						label.setValue("Sedang memproses data ("
								+ Common.numberFormat.get().format(i * 100.0
										/ objects.size()) + " %)");
						XSSFRow row = sheet.createRow(i);
						int j = 0;
						for (Object object : myObjects) {
							row.createCell(j).setCellValue(
									object == null ? "" : object.toString());
							j++;
						}
						i++;
					}
				} catch (Exception e) {
					for (Object object : objects) {
						label.setValue("Sedang memproses data ("
								+ Common.numberFormat.get().format(i * 100.0
										/ objects.size()) + " %)");
						XSSFRow row = sheet.createRow(i);
						int j = 0;
						row.createCell(j).setCellValue(
								object == null ? "" : object.toString());
						j++;
						i++;
					}
				}

				sizedata.setValue(i + 1);

				try {
					FileOutputStream fileOut = new FileOutputStream(filename);
					workbook.write(fileOut);
					fileOut.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					Common.tampilErrorJikaAdmin(e); 
				}

				System.out.println("Your excel file has been generated! "
						);
				objects = null;

				HibernateUtil.closeSession();
				label.setValue("");
							} finally {
					ais.database.hibernate.HibernateUtil.closeSession();
				}
			}
		}).start();

	}
}