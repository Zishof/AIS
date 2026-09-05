package ais.action.master.library;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
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
import org.zkoss.zul.Doublebox;
import org.zkoss.zul.Hbox;
import ais.ui.util.MyInclude;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.library.helper.AmbilDataPerpustakaanBanbox;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.dao.DaoFactory;
import ais.database.dao.library.DendaKeterlambatanItemDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.Tbmuser;
import ais.database.model.library.DendaKeterlambatanItem;
import ais.database.model.library.JenisAnggota;
import ais.database.model.library.Perpustakaan;
import ais.database.model.library.TipeAnggota;
import ais.ui.util.DataCriteria;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Controller/action ZK untuk denda keterlambatan item. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code AmbilDataPerpustakaanBanbox searchnama}, {@code Combobox searchfakultas},
 * {@code Combobox searchjurusan}, {@code AmbilDataPerpustakaanBanbox perpustakaan}, {@code Combobox
 * jenisAnggota}; inisialisasi/lifecycle ({@code doBeforeCompose()}, {@code doAfterCompose()}, {@code init()},
 * {@code initCriteria()}); pembacaan/pencarian ({@code uploadDataDenda()}, {@code onSearchDefault()}); mutasi
 * data ({@code onSave()}); operasi domain lain ({@code onHariLibur()}, {@code onKonfigurasi()}, {@code
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
public class DendaKeterlambatanItemAction extends GenericAutowireComposer implements DataCriteria {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private AmbilDataPerpustakaanBanbox searchnama;
	private Combobox searchfakultas;
	private Combobox searchjurusan;

	private AmbilDataPerpustakaanBanbox perpustakaan;
	private Combobox jenisAnggota;
	private Combobox tipeAnggota;
	private Combobox fakultas;
	private Combobox jurusan;
	private MyDatebox mulaiBerlaku;
	private Intbox jumlahHari;
	private Doublebox denda;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private DendaKeterlambatanItem dendaKeterlambatanItem;
	private MyToolbarbuttonConfig add;
	// private MyCheckboxConfig berulang;
	// dendaPerItem tidak lagi disunting dari form: lihat catatan pada onSave() dan
	// DendaKeterlambatanItem#getDendaPerItem().

	private String[] contents = new String[] { "id", "perpustakaan", "mulaiBerlaku", "jumlahHari", "denda",
			"dendaPerItem", "jenisAnggota", "tipeAnggota", "fakultas", "jurusan", "keterangan" };

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

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

		searchnama.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});

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

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakDataCustomButton(DendaKeterlambatanItem.class, this,
				"Download Data Denda", "/img/print.png", contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = new MyToolbarbuttonConfig("Upload Data Denda" + Common.ukuranLabelFileUpload(),
				"/img/excel.png");
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
							uploadDataDenda(file, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									onSearchDefault(arg0);
									Clients.clearBusy();
								}
							}, contents);
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

	private Tabpanel hariLibur;

	public void onHariLibur(Event event) {
		if (hariLibur.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(hariLibur);
			MyInclude iframe = new MyInclude("/pages/master/library/hari_libur_perpustakaan.zul");
			iframe.setParent(window);
		}
	}

	private Tabpanel konfigurasi;

	public void onKonfigurasi(Event event) {
		if (konfigurasi.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(konfigurasi);
			MyInclude iframe = new MyInclude("/pages/master/library/konfigurasi_perpustakaan.zul");
			iframe.setParent(window);
		}
	}

	public static void uploadDataDenda(final File file, final EventListener eventListener, final String[] contents)
			throws Exception {

		final Label peringatan = new Label("");
		final ais.common.UploadReportHelper report = new ais.common.UploadReportHelper("Upload Denda Keterlambatan");
		final Label downloadPath = new Label("");

		final Tbmuser tbmuser = Common.getCurrentUser();

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
					if (!downloadPath.getValue().isEmpty()) {
						try { org.zkoss.zul.Filedownload.save(new java.io.File(downloadPath.getValue()), "text/plain"); } catch (Exception eD) { ais.common.ErrorAuditUtil.record(eD, "auto-audit(empty-catch) DendaKeterlambatanItemAction laporan-download"); }
					}
					MyMessageboxConfig.show(
							report.getRingkasan(),
							"Pemberitahuan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, eventListener);
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

					ClassMetadata classMetadata = HibernateUtil.getClassMetadata(DendaKeterlambatanItem.class);
					Session session = HibernateUtil.currentNativeSession();

					int rowCount = (sheet.getLastRowNum() + 1);
					for (int i = 1; i < rowCount; i++) {
						try {

							Long id = Common.getSheetContentAsLong(sheet, 0, i);
							DendaKeterlambatanItem dendaKeterlambatanItem = id == null || id.equals(-1L) ? null
									: (DendaKeterlambatanItem) session.createCriteria(DendaKeterlambatanItem.class)
											.add(Restrictions.idEq(id)).uniqueResult();

							if (dendaKeterlambatanItem == null) {
								Integer jumlahHari = Common.getSheetContentAsInteger(sheet, 3, i);
								Perpustakaan perpustakaan = (Perpustakaan) Common.getSheetContentAsObject(sheet, 1, i,
										Perpustakaan.class, null);
								JenisAnggota jenisAnggota = (JenisAnggota) Common.getSheetContentAsObject(sheet, 6, i,
										JenisAnggota.class, null);
								TipeAnggota tipeAnggota = (TipeAnggota) Common.getSheetContentAsObject(sheet, 7, i,
										TipeAnggota.class, null);
								Fakultas fakultas = (Fakultas) Common.getSheetContentAsObject(sheet, 8, i,
										Fakultas.class, null);
								if (fakultas == null) {
									fakultas = tbmuser.ambilFakultas();
								}
								Jurusan jurusan = (Jurusan) Common.getSheetContentAsObject(sheet, 9, i, Jurusan.class,
										Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
								if (jurusan == null) {
									jurusan = tbmuser.ambilJurusan();
								}
								dendaKeterlambatanItem = jumlahHari == null || perpustakaan == null ? null
										: (DendaKeterlambatanItem) session.createCriteria(DendaKeterlambatanItem.class)
												.add(Restrictions.eq("jumlahHari", jumlahHari))
												.add(Restrictions.eq("perpustakaan", perpustakaan))
												.add(jenisAnggota == null ? Restrictions.isNull("jenisAnggota")
														: Restrictions.eq("jenisAnggota", jenisAnggota))
												.add(tipeAnggota == null ? Restrictions.isNull("tipeAnggota")
														: Restrictions.eq("tipeAnggota", tipeAnggota))
												.add(fakultas == null ? Restrictions.isNull("fakultas")
														: Restrictions.eq("fakultas", fakultas))
												.add(jurusan == null ? Restrictions.isNull("jurusan")
														: Restrictions.eq("jurusan", jurusan))
												.addOrder(Order.desc("id")).setMaxResults(1).uniqueResult();
							}

							if (dendaKeterlambatanItem == null) {
								dendaKeterlambatanItem = new DendaKeterlambatanItem();
							}

							Common.setObjectValues(classMetadata, dendaKeterlambatanItem, contents, 1, sheet, i);
							// Dipaksa true seperti onSave(): kolom dendaPerItem pada berkas upload
							// diabaikan agar tarif hasil upload tidak jatuh ke cabang mati.
							dendaKeterlambatanItem.setDendaPerItem(true);

							session.getTransaction().begin();
							session.saveOrUpdate(dendaKeterlambatanItem);
							session.getTransaction().commit();

							label.setValue("Upload data \""
									+ Common.numberFormat.get().format(dendaKeterlambatanItem.getJumlahHari()) + " - Rp."
									+ Common.numberFormat.get().format(dendaKeterlambatanItem.getDenda()) + "\" ("
									+ Common.numberFormat.get().format(i * 100.0 / rowCount) + " %)");
							report.sukses(i, dendaKeterlambatanItem.getJumlahHari() + " hari - Rp." + dendaKeterlambatanItem.getDenda(), "");

						} catch (Exception e) {
							Common.tampilErrorJikaAdmin(e);
							report.gagal(i, "Baris " + i, e, "Periksa format tanggal dan data peminjam.");
						}

					}
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/library/DendaKeterlambatanItemAction.java:337");
				}

				HibernateUtil.closeSession();

				try {
					java.io.File rptFile = report.simpanLaporan();
					downloadPath.setValue(rptFile.getAbsolutePath());
				} catch (Exception eR) { ais.common.ErrorAuditUtil.record(eR, "auto-audit(empty-catch) DendaKeterlambatanItemAction laporan"); }
				label.setValue("");
							} finally {
					ais.database.hibernate.HibernateUtil.closeSession();
				}
			}
		}).start();
	}

	/**
	 * Renderer lokal untuk layar/komponen {@link DendaKeterlambatanItemAction}. Kelas ini menerjemahkan satu item
	 * data menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link DendaKeterlambatanItemAction} dan dapat
	 * mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see DendaKeterlambatanItemAction
	 */
	class DendaKeterlambatanItemRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final DendaKeterlambatanItem dendaKeterlambatanItem = (DendaKeterlambatanItem) arg1;

			RevisiHelper.createNewRevisi(DendaKeterlambatanItem.class, dendaKeterlambatanItem,
					dendaKeterlambatanItem.getPerpustakaan().getNama()).setParent(arg0);
			new Label(dendaKeterlambatanItem.getMulaiBerlaku() == null ? ""
					: Common.dateFormat2.get().format(dendaKeterlambatanItem.getMulaiBerlaku())).setParent(arg0);
			new Label(dendaKeterlambatanItem.getDenda() == null ? ""
					: Common.numberFormat.get().format(dendaKeterlambatanItem.getDenda())).setParent(arg0);

			// new Label(dendaKeterlambatanItem.getBerulang() ? "Ya" : "Tidak")
			// .setParent(arg0);
			new Label(dendaKeterlambatanItem.getJumlahHari() + " hari").setParent(arg0);
			new Label(dendaKeterlambatanItem.getDendaPerItem() ? "Ya" : "Tidak").setParent(arg0);

			new Label(dendaKeterlambatanItem.getTipeAnggota() == null ? "Semua"
					: dendaKeterlambatanItem.getTipeAnggota().getNama()).setParent(arg0);
			new Label(dendaKeterlambatanItem.getJenisAnggota() == null ? "Semua"
					: dendaKeterlambatanItem.getJenisAnggota().getNama()).setParent(arg0);

			new Label(dendaKeterlambatanItem.getFakultas() == null ? "Semua"
					: dendaKeterlambatanItem.getFakultas().getNama()).setParent(arg0);
			new Label(dendaKeterlambatanItem.getJurusan() == null ? "Semua"
					: dendaKeterlambatanItem.getJurusan().getNama()).setParent(arg0);

			new Label(dendaKeterlambatanItem.getKeterangan()).setParent(arg0);

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(dendaKeterlambatanItem);
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

											Common.refreshDelete(dendaKeterlambatanItem);

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
		init(new DendaKeterlambatanItem());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(DendaKeterlambatanItem dendaKeterlambatanItem) throws Exception {
		this.dendaKeterlambatanItem = dendaKeterlambatanItem;
		addWindow.setTitle(dendaKeterlambatanItem.getId() == null ? "Tambah Denda Pengembalian" : "Ubah Denda Pengembalian");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Perpustakaan"));
		row.appendChild(perpustakaan = new AmbilDataPerpustakaanBanbox());
		perpustakaan.setAttribute("perpustakaan", dendaKeterlambatanItem.getPerpustakaan());
		perpustakaan.setValue(dendaKeterlambatanItem.getPerpustakaan() == null ? ""
				: dendaKeterlambatanItem.getPerpustakaan().getNama());
		perpustakaan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Anggota"));
		row.appendChild(jenisAnggota = new Combobox());
		jenisAnggota.setWidth("90%");
		jenisAnggota.setReadonly(true);
		Common.insertComboDanSemua(jenisAnggota, "nama", JenisAnggota.class);
		Common.selectComboItem(jenisAnggota, dendaKeterlambatanItem.getJenisAnggota());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tipe Anggota"));
		row.appendChild(tipeAnggota = new Combobox());
		tipeAnggota.setWidth("90%");
		tipeAnggota.setReadonly(true);
		Common.insertComboDanSemua(tipeAnggota, "nama", TipeAnggota.class);
		Common.selectComboItem(tipeAnggota, dendaKeterlambatanItem.getTipeAnggota());

		Tbmuser tbmuser = Common.getCurrentUser();
		Common.initFakultasDanJurusanDanSemua(fakultas = new Combobox(), jurusan = new Combobox(), null, null);
		if (dendaKeterlambatanItem.getFakultas() == null && tbmuser.ambilFakultas() != null) {
			dendaKeterlambatanItem.setFakultas(tbmuser.ambilFakultas());
		}
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(fakultas);
		Common.selectComboItem(fakultas, dendaKeterlambatanItem.getFakultas());
		fakultas.setWidth("90%");

		Common.insertComboDanSemua(jurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
				Restrictions.eq("fakultas", dendaKeterlambatanItem.getFakultas() == null ? tbmuser.ambilFakultas()
						: dendaKeterlambatanItem.getFakultas()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(jurusan);
		jurusan.setWidth("90%");
		Common.pilihJurusan(jurusan, dendaKeterlambatanItem.getJurusan());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Mulai Berlaku"));
		row.appendChild(mulaiBerlaku = new MyDatebox(dendaKeterlambatanItem.getMulaiBerlaku()));
		mulaiBerlaku.setFormat(Common.dateFormat1.get().toPattern());
		mulaiBerlaku.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Denda"));
		row.appendChild(denda = new MyDoublebox(dendaKeterlambatanItem.getDenda()));
		denda.setWidth("90%");
		//
		// row = new MyFormRow();
		//		// row.setParent(rows);
		// row.appendChild(new ais.ui.util.MyLabelConfig("Berulang"));
		// row.appendChild(berulang = new MyCheckboxConfig());
		// berulang.setChecked(dendaKeterlambatanItem.getBerulang());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Denda ini berlaku setelah"));
		row.appendChild(jumlahHari = new Intbox(dendaKeterlambatanItem.getJumlahHari()));
		jumlahHari.setWidth("90%");

		// Baris "Denda Per Item" sengaja dihapus dari form: satu-satunya cabang perhitungan
		// denda yang masih hidup (LibraryUtil.hitungDendaItem) mensyaratkan dendaPerItem=true,
		// sehingga tarif yang dibuat dengan kotak itu tidak dicentang tidak pernah dikenakan
		// kepada siapa pun. onSave() sekarang memaksa nilainya true untuk semua tarif baru/
		// yang disunting lewat layar ini. Lihat DendaKeterlambatanItem#getDendaPerItem().

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(
				dendaKeterlambatanItem.getKeterangan() == null ? "" : dendaKeterlambatanItem.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);
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
		if (perpustakaan.getAttribute("perpustakaan") == null) {
			MyMessageboxConfig.show("Perpustakaan harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (mulaiBerlaku.getValue() == null) {
			MyMessageboxConfig.show("Mulai Berlaku harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (jumlahHari.getValue() == null) {
			MyMessageboxConfig.show("Jumlah Hari harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (denda.getValue() == null) {
			MyMessageboxConfig.show("Denda harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		DendaKeterlambatanItemDao dendaKeterlambatanItemDao = DaoFactory.getInstance().getDendaKeterlambatanItemDao();
		if (dendaKeterlambatanItem.getId() != null) {
			dendaKeterlambatanItem = dendaKeterlambatanItemDao.load(dendaKeterlambatanItem.getId());
		}

		// Dipaksa true: lihat catatan penghapusan baris "Denda Per Item" di init() di atas.
		dendaKeterlambatanItem.setDendaPerItem(true);
		// dendaKeterlambatanItem.setBerulang(berulang.isChecked());
		dendaKeterlambatanItem.setDenda(denda.getValue());
		dendaKeterlambatanItem.setJumlahHari(jumlahHari.getValue());
		dendaKeterlambatanItem.setMulaiBerlaku(mulaiBerlaku.getValue());
		dendaKeterlambatanItem.setPerpustakaan((Perpustakaan) perpustakaan.getAttribute("perpustakaan"));
		dendaKeterlambatanItem.setKeterangan(keterangan.getValue());

		dendaKeterlambatanItem.setJenisAnggota((JenisAnggota) (jenisAnggota.getSelectedItem() == null ? null
				: jenisAnggota.getSelectedItem().getValue()));
		dendaKeterlambatanItem.setTipeAnggota((TipeAnggota) (tipeAnggota.getSelectedItem() == null ? null
				: tipeAnggota.getSelectedItem().getValue()));
		dendaKeterlambatanItem.setFakultas(
				(Fakultas) (fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null ? null
						: fakultas.getSelectedItem().getValue()));
		dendaKeterlambatanItem
				.setJurusan((Jurusan) (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null
						? null : jurusan.getSelectedItem().getValue()));

		if (dendaKeterlambatanItem.getId() != null) {
			dendaKeterlambatanItemDao.update(dendaKeterlambatanItem);
		} else {
			dendaKeterlambatanItemDao.save(dendaKeterlambatanItem);
		}

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(DendaKeterlambatanItem.class);
		if (order)
			criteria.addOrder(Order.desc("id"));
		criteria.add((searchnama == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchnama.getAttribute("perpustakaan") == null ? Restrictions.sqlRestriction("1=1")
				: Restrictions.eq("perpustakaan", searchnama.getAttribute("perpustakaan"))));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<DendaKeterlambatanItem> dendaKeterlambatanItem = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(dendaKeterlambatanItem);
		grid.setRowRenderer(new DendaKeterlambatanItemRenderer());
		grid.setModelCheckMobile(strset);

	}

}
