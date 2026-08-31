package ais.action.master;
import org.hibernate.criterion.MatchMode;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
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
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.database.dao.DaoFactory;
import ais.database.dao.PendapatanOrangTuaDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.PendapatanOrangTua;
import ais.ui.util.DataCriteria;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Controller/action ZK untuk pendapatan orang tua. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code Textbox searchkode}, {@code MyDoublebox mulaiDari}, {@code MyDoublebox
 * sampai}, {@code Textbox keterangan}, {@code boolean edit}; inisialisasi/lifecycle ({@code doBeforeCompose()},
 * {@code doAfterCompose()}, {@code init()}, {@code initCriteria()}); pembacaan/pencarian ({@code
 * onSearchDefault()}); validasi/perhitungan ({@code checkPendapatan()}); mutasi data ({@code onSave()}); operasi
 * domain lain ({@code onAdd()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang
 * disebut di atas.</p>
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
public class PendapatanOrangTuaAction extends GenericAutowireComposer implements DataCriteria {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5697272511682717282L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	// private Textbox searchmulaiDari;
 private Textbox searchkode;
	// private Textbox searchSampai;
	private MyDoublebox mulaiDari;
	private MyDoublebox sampai;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;
	private PendapatanOrangTua pendapatanOrangTua;
	private MyToolbarbuttonConfig add;
	private Textbox kode;

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

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, "id", "nama", "kode", "mulaiDari", "sampai");
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = new MyToolbarbuttonConfig("Upload" + Common.ukuranLabelFileUpload(),
				"/img/excel.png");
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		if (upload != null) { upload.setUpload(Common.ukuranFileUpload()); }
		upload.addEventListener("onUpload", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				UploadEvent uploadEvent = (UploadEvent) event;
				Media media = uploadEvent.getMedia();if(!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media))return;
				if (media.getName().toLowerCase().endsWith("xlsx")) {

					InputStream inputStream = media.getStreamData();
					// System.out.println("media = " + media);
					File file = new File(Sessions.getCurrent().getWebApp().getRealPath("/temp/" + media.getName()));
					// System.out.println("file = " + file.getAbsolutePath());
					file.getParentFile().mkdirs();
					FileOutputStream fileOutputStream = new FileOutputStream(file);
					int c;
					while ((c = inputStream.read()) != -1) {
						fileOutputStream.write(c);
					}
					fileOutputStream.close();
					inputStream.close();

					XSSFWorkbook workbook = new XSSFWorkbook(file.getAbsolutePath());
					XSSFSheet sheet = workbook.getSheetAt(0);

					String peringatan = "";
					for (int i = 1; i < (sheet.getLastRowNum() + 1); i++) {
						try {

							Session session = HibernateUtil.currentNativeSession();

							if (Common.getSheetContentAsString(sheet, 1, i) == null) {
								break;
							}

							String nama = Common.getSheetContentAsString(sheet, 1, i);
							String kode = Common.getSheetContentAsString(sheet, 2, i);
							Double mulaiDari = Common.getSheetContentAsDouble(sheet, 3, i);
							Double sampai = Common.getSheetContentAsDouble(sheet, 4, i);

							if (nama != null && !nama.trim().isEmpty()) {
								Long id = Common.getSheetContentAsLong(sheet, 0, i);
								PendapatanOrangTua pendapatanOrangTua = id == null || id.equals(-1L) ? null
										: (PendapatanOrangTua) session.createCriteria(PendapatanOrangTua.class)
												.add(Restrictions.idEq(id)).uniqueResult();

								if (pendapatanOrangTua == null) {
									pendapatanOrangTua = new PendapatanOrangTua();
								}

								pendapatanOrangTua.setNama(nama);
								pendapatanOrangTua.setKode(kode);
								pendapatanOrangTua.setMulaiDari(mulaiDari);
								pendapatanOrangTua.setSampai(sampai);

								session.getTransaction().begin();
								session.saveOrUpdate(pendapatanOrangTua);
								session.getTransaction().commit();
							}

							HibernateUtil.closeSession();

						} catch (Exception e) {
							Common.tampilErrorJikaAdmin(e); 
						}

					}

					MyMessageboxConfig.show(
							"Upload data berhasil dilakukan." + (peringatan.isEmpty() ? "" : "\n" + peringatan),
							"Pemberitahuan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
							new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									onSearchDefault(null);
								}
							});

				} else {
					MyMessageboxConfig.show(
							"File yang anda upload harus ber-format Excel Open XML Spreadsheet (xlsx). Jika masih menggunakan format lain, buka file excel tersebut, kemudian Save As Excel Open XML Spreadsheet (xlsx). "
									+ media,
							"Error", MyMessageboxConfig.OK, MyMessageboxConfig.ERROR);
				}
			}
		});
		Common.appendKeToolbar(upload, add, comp);
	}

	class PendapatanOrangTuaRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {arg0.setValign("top");
			// TODO Auto-generated method stub
			final PendapatanOrangTua pendapatanOrangTua = (PendapatanOrangTua) arg1;
			new Label(pendapatanOrangTua.getKode()).setParent(arg0);
			new Label(Common.numberFormat.get().format(pendapatanOrangTua.getMulaiDari())).setParent(arg0);
			new Label(Common.numberFormat.get().format(pendapatanOrangTua.getSampai())).setParent(arg0);
			new Label(pendapatanOrangTua.getKeterangan()).setParent(arg0);

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(pendapatanOrangTua);
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
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {

											Common.refreshDelete(pendapatanOrangTua);

											onSearchDefault(event);
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e); 
											MyMessageboxConfig.show(
													"Data ini tidak dapat dihapus .., karena berelasi dengan data lainnya, error-nya adalah sbagai berikut:"
															+ e.getMessage());
										}

									}

								}
							});

				}
			});
			button.setParent(toolbar);
			toolbar.setParent(arg0);
		}

	}

	public void onAdd(Event event) throws Exception {
		init(new PendapatanOrangTua());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(PendapatanOrangTua pendapatanOrangTua) {
		this.pendapatanOrangTua = pendapatanOrangTua;
		addWindow.setTitle(pendapatanOrangTua.getId() == null ? "Tambah Pendapatan Orang Tua" : "Ubah Pendapatan Orang Tua");
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

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode"));
		row.appendChild(kode = new Textbox(pendapatanOrangTua.getKode()));
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Mulai Dari"));
		row.appendChild(mulaiDari = new MyDoublebox(pendapatanOrangTua.getMulaiDari()));
		mulaiDari.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sampai"));
		row.appendChild(sampai = new MyDoublebox(pendapatanOrangTua.getSampai()));
		sampai.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(
				pendapatanOrangTua.getKeterangan() == null ? "" : pendapatanOrangTua.getKeterangan()));
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
		if (mulaiDari.getValue() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Pendapatan Orang Tua Mulai Dari",
					"Kolom Pendapatan Orang Tua Mulai Dari belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Pendapatan Orang Tua Mulai Dari.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		if (sampai.getValue() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Pendapatan Orang Tua Sampai",
					"Kolom Pendapatan Orang Tua Sampai belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Pendapatan Orang Tua Sampai.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		/*
		 * if (keterangan.getValue().trim().equals("")) {
		 * MyMessageboxConfig.show("Keterangan harus diisi", "Peringatan",
		 * MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION); return false; }
		 */

		/*
		 * boolean i = checkPendapatan(); if (i) {
		 * MyMessageboxConfig.show("Pendapatan Orang Tua sudah ada di database",
		 * "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION); return
		 * false; }
		 */

		PendapatanOrangTuaDao pendapatanOrangTuaDao = DaoFactory.getInstance().getPendapatanOrangTuaDao();
		if (pendapatanOrangTua.getId() != null) {
			pendapatanOrangTua = pendapatanOrangTuaDao.load(pendapatanOrangTua.getId());

		}

		pendapatanOrangTua.setKode(kode.getValue());

		pendapatanOrangTua.setMulaiDari(mulaiDari.getValue());
		pendapatanOrangTua.setSampai(sampai.getValue());
		pendapatanOrangTua.setKeterangan(keterangan.getValue());

		// pendapatanOrangTuaDao.beginTransaction();
		if (pendapatanOrangTua.getId() != null) {
			pendapatanOrangTuaDao.update(pendapatanOrangTua);
		} else {
			pendapatanOrangTuaDao.save(pendapatanOrangTua);
		}
		// pendapatanOrangTuaDao.commitTransaction();
		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(PendapatanOrangTua.class);
		if (order)
			criteria.addOrder(Order.asc("mulaiDari"));
		return criteria;

	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<PendapatanOrangTua> pendapatanOrangTua = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(pendapatanOrangTua);
		grid.setRowRenderer(new PendapatanOrangTuaRenderer());
		grid.setModelCheckMobile(strset);

	}

	public Boolean checkPendapatan() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(PendapatanOrangTua.class).setProjection(Projections.rowCount())
				.add(Restrictions.eq("mulaiDari", (mulaiDari.getValue())))
				.add(Restrictions.eq("sampai", sampai.getValue()))
				.add(this.pendapatanOrangTua.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.pendapatanOrangTua.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

}
