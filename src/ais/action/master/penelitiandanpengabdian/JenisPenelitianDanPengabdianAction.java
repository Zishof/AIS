package ais.action.master.penelitiandanpengabdian;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.metadata.ClassMetadata;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;

import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.penelitiandanpengabdian.JenisPenelitianDanPengabdian;
import ais.ui.util.DataCriteria;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Controller/action ZK untuk jenis penelitian dan pengabdian. Tipe ini merupakan titik masuk UI
 * yang menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus
 * oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code Textbox searchkode}, {@code Textbox searchnama}, {@code Textbox kode},
 * {@code Textbox isi}, {@code Textbox keterangan}; inisialisasi/lifecycle ({@code doBeforeCompose()}, {@code
 * doAfterCompose()}, {@code init()}, {@code initCriteria()}); pembacaan/pencarian ({@code onSearchDefault()});
 * validasi/perhitungan ({@code checkNama()}); mutasi data ({@code onSave()}); operasi domain lain ({@code
 * onAdd()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
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
public class JenisPenelitianDanPengabdianAction extends GenericAutowireComposer implements DataCriteria {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchkode;
	private Textbox searchnama;
	private Textbox kode;
	private Textbox isi;
	private Textbox keterangan;
	private Combobox parent;

	private boolean edit = false;
	private boolean delete = false;

	private JenisPenelitianDanPengabdian jenisPenelitianDanPengabdian;
	private MyToolbarbuttonConfig add;

	public static String[] contents = new String[] { "id", "kode", "isi", "keterangan", "parent" };

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

		Session session = HibernateUtil.currentSession();
		int count = ((Number) session.createCriteria(JenisPenelitianDanPengabdian.class)
				.setProjection(Projections.rowCount()).uniqueResult()).intValue();
		if (count == 0) {
			JenisPenelitianDanPengabdian angket = new JenisPenelitianDanPengabdian();
			angket.setKode("001.000");
			angket.setIsi("Penelitian Ilmiah");
			Common.refreshSaveOrUpdate(session, angket);

			angket = new JenisPenelitianDanPengabdian();
			angket.setKode("002.000");
			angket.setIsi("Pengabdian Masyarakat");
			Common.refreshSaveOrUpdate(session, angket);

			angket = new JenisPenelitianDanPengabdian();
			angket.setKode("003.000");
			angket.setIsi("Kreatifitas Mahasiswa");
			Common.refreshSaveOrUpdate(session, angket);
		}

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = new MyToolbarbuttonConfig("Upload"+Common.ukuranLabelFileUpload(), "/img/excel.png");
		if (upload != null) { upload.setUpload(Common.ukuranFileUpload()); }
		upload.addEventListener("onUpload", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				UploadEvent uploadEvent = (UploadEvent) event;
				Media media = uploadEvent.getMedia();if(!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media))return;
				if (media.getName().toLowerCase().endsWith("xlsx")) {

					InputStream inputStream = media.getStreamData();
					// System.out.println("media = " + media);
					final File file = new File(
							Sessions.getCurrent().getWebApp().getRealPath("/temp/" + media.getName()));
					// System.out.println("file = " + file.getAbsolutePath());
					file.getParentFile().mkdirs();
					FileOutputStream fileOutputStream = new FileOutputStream(file);
					int c;
					while ((c = inputStream.read()) != -1) {
						fileOutputStream.write(c);
					}
					fileOutputStream.close();
					inputStream.close();

					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							final Label peringatan = new Label("");

							final Label label = new Label(ais.common.Common.getBahasaConfig("Proses upload data data .."));
							Clients.showBusy(label.getValue());
							final Timer timer = new Timer(200);
							timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
							timer.setRepeats(true);
							timer.addEventListener("onTimer", new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									Clients.showBusy(label.getValue());
									if (label.getValue().isEmpty()) {
										System.out.println("loading file " + file.getAbsolutePath());
										MyMessageboxConfig.show(
												"Upload data berhasil dilakukan." + (peringatan.getValue().isEmpty()
														? "" : "\n" + peringatan.getValue()),
												"Pemberitahuan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
												new EventListener() {

													@Override
													public void onEvent(Event arg0) throws Exception {
														onSearchDefault(arg0);
													}
												});
										Clients.clearBusy();
										timer.detach();
									}

								}
							});
							timer.start();

							new Thread(new Runnable() {

								@Override
								public void run() {
									try {

									try {

										XSSFWorkbook workbook = new XSSFWorkbook(file.getAbsolutePath());
										XSSFSheet sheet = workbook.getSheetAt(0);

										ClassMetadata classMetadata = HibernateUtil
												.getClassMetadata(JenisPenelitianDanPengabdian.class);
										Session session = HibernateUtil.currentNativeSession();

										int rowCount = (sheet.getLastRowNum() + 1);
										for (int i = 1; i < rowCount; i++) {
											try {

												Long id = Common.getSheetContentAsLong(sheet, 0, i);
												JenisPenelitianDanPengabdian jenisPenelitianDanPengabdian = id == null
														|| id.equals(-1L)
																? null
																: (JenisPenelitianDanPengabdian) session
																		.createCriteria(
																				JenisPenelitianDanPengabdian.class)
																		.add(Restrictions.idEq(id)).uniqueResult();

												if (jenisPenelitianDanPengabdian == null) {
													jenisPenelitianDanPengabdian = new JenisPenelitianDanPengabdian();
												}

												Common.setObjectValues(classMetadata, jenisPenelitianDanPengabdian,
														contents, 1, sheet, i);

												session.getTransaction().begin();
												session.saveOrUpdate(jenisPenelitianDanPengabdian);
												session.getTransaction().commit();

												label.setValue("Upload data \"" + jenisPenelitianDanPengabdian.getKode()
														+ " - " + jenisPenelitianDanPengabdian.getNama() + "\" ("
														+ Common.numberFormat.get().format(i * 100.0 / rowCount) + " %)");

											} catch (Exception e) {
												Common.tampilErrorJikaAdmin(e); 
											}

										}
									} catch (Exception e1) {
										// TODO Auto-generated catch block
										e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/penelitiandanpengabdian/JenisPenelitianDanPengabdianAction.java:250");
									}

									HibernateUtil.closeSession();

									label.setValue("");
																	} finally {
										ais.database.hibernate.HibernateUtil.closeSession();
									}
								}
							}).start();

						}
					}, "Harap tunggu.. sedang melakukan proses upload data..");

				} else {
					MyMessageboxConfig
							.show("File yang anda upload harus ber-format Excel Open XML Spreadsheet (xlsx). Jika masih menggunakan format lain, buka file excel tersebut, kemudian Save As Excel Open XML Spreadsheet (xlsx). "
									+ media, "Error", MyMessageboxConfig.OK, MyMessageboxConfig.ERROR);
				}
			}
		});
		Common.appendKeToolbar(upload, add, comp);
	}

	class JenisPenelitianDanPengabdianRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {arg0.setValign("top");
			// TODO Auto-generated method stub
			final JenisPenelitianDanPengabdian jenisPenelitianDanPengabdian = (JenisPenelitianDanPengabdian) arg1;

			new Label(jenisPenelitianDanPengabdian.getKode()).setParent(arg0);
			RevisiHelper.createNewRevisi(JenisPenelitianDanPengabdian.class, jenisPenelitianDanPengabdian,
					jenisPenelitianDanPengabdian.getIsi()).setParent(arg0);
			new Label(jenisPenelitianDanPengabdian.getKeterangan()).setParent(arg0);
			new Label(jenisPenelitianDanPengabdian.getParent() == null ? ""
					: jenisPenelitianDanPengabdian.getParent().getIsi()).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");checkbox.setDisabled(!edit);
			checkbox.setChecked(jenisPenelitianDanPengabdian.getAktif());
			checkbox.setParent(arg0);arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					jenisPenelitianDanPengabdian.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(jenisPenelitianDanPengabdian);
				}
			});

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(jenisPenelitianDanPengabdian);
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			button.setParent(toolbar);

			button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
			button.setVisible(delete);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {

											Common.refreshDelete(jenisPenelitianDanPengabdian);

											onSearchDefault(event);
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e); 
											MyMessageboxConfig
													.show("Data ini tidak dapat dihapus .., karena berelasi dengan data lainnya, error-nya adalah sbagai berikut:"
															+ e.getMessage());
										}

									}

								}
							});

				}
			});
			button.setParent(toolbar);
			ais.ui.util.MenuAksiBaris.pasang(toolbar);
			toolbar.setParent(arg0);
		}

	}

	public void onAdd(Event event) throws Exception {
		init(new JenisPenelitianDanPengabdian());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(JenisPenelitianDanPengabdian jenisPenelitianDanPengabdian) {
		this.jenisPenelitianDanPengabdian = jenisPenelitianDanPengabdian;
		addWindow.setTitle("kelompok Penelitian dan Pengabdian");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("35%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode "));
		row.appendChild(kode = new Textbox(jenisPenelitianDanPengabdian.getKode()));
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama "));
		row.appendChild(isi = new Textbox(
				jenisPenelitianDanPengabdian.getIsi() == null ? "" : jenisPenelitianDanPengabdian.getIsi()));
		isi.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Induk "));
		row.appendChild(parent = new Combobox());
		Common.insertComboDanSemua(parent, "isi", JenisPenelitianDanPengabdian.class);
		Common.selectComboItem(parent, jenisPenelitianDanPengabdian.getParent());
		parent.setWidth("90%");
		parent.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(jenisPenelitianDanPengabdian.getKeterangan() == null ? ""
				: jenisPenelitianDanPengabdian.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(south);
		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				addWindow.setVisible(false);
			}
		});
		cancel.setParent(toolbar);
		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (onSave(event)) {
					onSearchDefault(null);
					addWindow.setVisible(false);
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);

	}

	public boolean onSave(Event event) throws Exception {
		if (kode.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Kode  harus diisi", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (isi.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Nama  harus diisi", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}

		boolean i = checkNama();
		if (i) {
			MyMessageboxConfig.show("Kode  sudah ada di database", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (jenisPenelitianDanPengabdian.getId() != null) {
			jenisPenelitianDanPengabdian = (JenisPenelitianDanPengabdian) session
					.load(JenisPenelitianDanPengabdian.class, jenisPenelitianDanPengabdian.getId());

		}

		jenisPenelitianDanPengabdian.setKode(kode.getValue());
		jenisPenelitianDanPengabdian.setIsi(isi.getValue());
		jenisPenelitianDanPengabdian.setKeterangan(keterangan.getValue());
		jenisPenelitianDanPengabdian.setParent((JenisPenelitianDanPengabdian) (parent.getSelectedItem() == null ? null
				: parent.getSelectedItem().getValue()));

		Common.refreshSaveOrUpdate(session, jenisPenelitianDanPengabdian);

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(JenisPenelitianDanPengabdian.class);

		if (order)
			criteria.addOrder(Order.asc("kode"));
		criteria.add(Restrictions.ilike("kode", searchkode.getValue(), MatchMode.ANYWHERE))
				.add(Restrictions.ilike("isi", searchnama.getValue(), MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<JenisPenelitianDanPengabdian> jenisPenelitianDanPengabdian = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(jenisPenelitianDanPengabdian);
		grid.setRowRenderer(new JenisPenelitianDanPengabdianRenderer());
		grid.setModelCheckMobile(strset);

	}

	public Boolean checkNama() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(JenisPenelitianDanPengabdian.class)
				.setProjection(Projections.rowCount()).add(Restrictions.eq("kode", kode.getValue().trim()))
				.add(this.jenisPenelitianDanPengabdian.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.jenisPenelitianDanPengabdian.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

}
