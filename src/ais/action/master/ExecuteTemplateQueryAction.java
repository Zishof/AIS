package ais.action.master;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;
import java.util.regex.Pattern;

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

	/** Boleh diawali tanda kurung/spasi/komentar, tapi statement wajib SELECT atau CTE (WITH ...). */
	private static final Pattern SELECT_ONLY_PATTERN = Pattern.compile("^[\\s(]*(select|with)\\b",
			Pattern.CASE_INSENSITIVE);
	private static final Pattern SQL_BLOCK_COMMENT = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);
	private static final Pattern SQL_LINE_COMMENT = Pattern.compile("--[^\\r\\n]*");

	/**
	 * Kata/fungsi yang dilarang meski statement lolos filter SELECT-only di atas:
	 * DML/DDL yang bisa disisipkan lewat data-modifying CTE ("with x as (delete ...)
	 * select * from x"), "into" untuk mencegah "select ... into tabel_baru" (yang
	 * pada PostgreSQL membuat tabel baru), serta fungsi bawaan PostgreSQL yang bisa
	 * dipanggil langsung di dalam SELECT untuk membaca/menulis berkas OS atau
	 * menjalankan koneksi/perintah lain (dblink, pg_read_file, lo_import/export, dst).
	 */
	private static final String[] FORBIDDEN_KEYWORDS = { "insert", "update", "delete", "truncate", "drop", "alter",
			"create", "grant", "revoke", "call", "merge", "into", "exec", "execute", "vacuum", "analyze", "reindex",
			"cluster", "lock", "listen", "notify", "discard", "deallocate", "prepare", "copy", "function",
			"procedure", "program", "dblink", "dblink_exec", "pg_read_file", "pg_read_binary_file", "pg_ls_dir",
			"pg_stat_file", "lo_import", "lo_export", "pg_terminate_backend", "pg_cancel_backend", "pg_reload_conf",
			"pg_rotate_logfile", "set_config" };

	private static String stripSqlComments(String sql) {
		String tanpaBlok = SQL_BLOCK_COMMENT.matcher(sql).replaceAll(" ");
		return SQL_LINE_COMMENT.matcher(tanpaBlok).replaceAll(" ");
	}

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
		/*
		 * Layar ini pada dasarnya adalah konsol SQL: hak READ menu saja tidak cukup
		 * karena Textbox query bisa diedit bebas oleh pengguna sebelum "Execute"
		 * ditekan. Wajib ADMINISTRATOR, fail-closed bila bukan.
		 */
		if (session.getAttribute("usersTemp") == null
				|| !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)
				|| !Common.getApakahAdminLain()) {
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

		String normalized = stripSqlComments(q).trim();
		while (normalized.endsWith(";")) {
			normalized = normalized.substring(0, normalized.length() - 1).trim();
		}

		if (normalized.isEmpty() || normalized.indexOf(';') >= 0) {
			MyMessageboxConfig.show(
					"Isi Template Query harus berupa satu pernyataan SELECT tunggal (tidak boleh ada titik koma di tengah)",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return;
		}

		if (!SELECT_ONLY_PATTERN.matcher(normalized).find()) {
			MyMessageboxConfig.show("Isi Template Query hanya boleh berupa pernyataan SELECT",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return;
		}

		String bertandaSpasi = " " + normalized.toLowerCase() + " ";
		for (String forbiddenKeyword : FORBIDDEN_KEYWORDS) {
			if (Pattern.compile("\\b" + forbiddenKeyword + "\\b").matcher(bertandaSpasi).find()) {
				MyMessageboxConfig.show(
						"Isi Template Query tidak boleh mengandung kata '" + forbiddenKeyword + "'",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
				return;
			}
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