package ais.action.master.asset.helper;

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
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Columns;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;

import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.asset.MasterAsset;
import ais.database.model.asset.PemakaianMasterAsset;
import ais.database.model.asset.PemakaianMasterAssetDetail;
import ais.ui.util.DataCriteria;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Controller/action ZK untuk pemakaian master asset detail. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyDetail}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code PemakaianMasterAsset
 * pemakaianMasterAsset}, {@code MyGrid grid}, {@code boolean edit}, {@code boolean delete}, {@code String
 * contents}, {@code Textbox nama}; inisialisasi/lifecycle ({@code initCriteria()}); pembacaan/pencarian ({@code
 * loadData()}, {@code uploadDataMasterAsset()}); operasi domain lain ({@code display()}); konfigurasi
 * constructor: {@code delete}, {@code edit}. Bagian lain dari kontrak tetap mengikuti kelas induk atau interface
 * yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 * <p><b>Lifecycle:</b> instance mengikuti lifecycle komponen ZK dan menyimpan state layar; jangan digunakan
 * sebagai singleton atau dibagikan antar desktop/session. Event handler harus tetap memakai konteks pengguna
 * serta session Hibernate milik request yang aktif.</p>
 *
 * @see MyDetail
 */
public class PemakaianMasterAssetDetailAction extends MyDetail implements DataCriteria {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5086031585928643232L;

	private PemakaianMasterAsset pemakaianMasterAsset;
	private MyGrid grid;
	private boolean edit = false;
	private boolean delete = false;

	private String[] contents = new String[] { "id", "masterAsset", "jumlah" };

	private Textbox nama;

	public PemakaianMasterAssetDetailAction(PemakaianMasterAsset pemakaianMasterAsset) {
		super();
		this.pemakaianMasterAsset = pemakaianMasterAsset;
		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		this.addEventListener(Events.ON_OPEN, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(PemakaianMasterAssetDetailAction.this);
				if (isOpen()) {
					display();
				}
			}
		});
	}

	/**
	 * Renderer lokal untuk layar/komponen {@link PemakaianMasterAssetDetailAction}. Kelas ini menerjemahkan satu
	 * item data menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link PemakaianMasterAssetDetailAction} dan dapat
	 * mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see PemakaianMasterAssetDetailAction
	 */
	class PemakaianMasterAssetDetailRenderer extends ais.ui.util.MyRowRenderer {

		public PemakaianMasterAssetDetailRenderer() {

		}

		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");
			final PemakaianMasterAssetDetail pemakaianMasterAssetDetail = (PemakaianMasterAssetDetail) data;
			MasterAsset masterAsset = pemakaianMasterAssetDetail.getMasterAsset();
			final MyDoublebox jumlah = new MyDoublebox(
					pemakaianMasterAssetDetail.getJumlah() == null ? 0.0 : pemakaianMasterAssetDetail.getJumlah());

			RevisiHelper.createNewRevisi(PemakaianMasterAssetDetail.class, pemakaianMasterAssetDetail,
					pemakaianMasterAssetDetail.getMasterAsset() == null ? ""
							: pemakaianMasterAssetDetail.getMasterAsset().getNama())
					.setParent(row);

			new Label(masterAsset.getMerk()).setParent(row);
			new Label(masterAsset.getJenisAsset() == null ? "" : masterAsset.getJenisAsset().getNama()).setParent(row);
			new Label(masterAsset.getKelompokAsset() == null ? "" : masterAsset.getKelompokAsset().getNama())
					.setParent(row);

			(jumlah).setParent(row);
			jumlah.setDisabled(pemakaianMasterAssetDetail.getPemakaianMasterAsset().getDisetujuiOleh() != null
					|| (pemakaianMasterAssetDetail.getDataPerMasterAsset() != null
							&& pemakaianMasterAssetDetail.getDataPerMasterAsset())
					|| !edit);
			jumlah.setStyle("text-align:right");
			jumlah.setWidth("90%");
			jumlah.addEventListener(Events.ON_CHANGE, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					Double saldo = Math.abs(jumlah.getValue() == null ? 0.0 : jumlah.getValue());
					jumlah.setValue(saldo);
					Session session = HibernateUtil.currentSession();
					pemakaianMasterAssetDetail.setJumlah(saldo);
					Common.refreshUpdate(session, (pemakaianMasterAssetDetail));
				}
			});

			final MyTextbox keterangan = new MyTextbox(pemakaianMasterAssetDetail.getKeterangan() == null ? ""
					: pemakaianMasterAssetDetail.getKeterangan());
			keterangan.setWidth("90%");
			keterangan.setHeight("95%");
			keterangan.setParent(row);
			keterangan.setDisabled(
					pemakaianMasterAssetDetail.getPemakaianMasterAsset().getDisetujuiOleh() != null || !edit);
			keterangan.addEventListener(Events.ON_CHANGE, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					pemakaianMasterAssetDetail.setKeterangan(keterangan.getValue());
					Common.refreshUpdate(session, (pemakaianMasterAssetDetail));
				}
			});

			Hbox toolbar = new Hbox();

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setDisabled(
					pemakaianMasterAssetDetail.getPemakaianMasterAsset().getDisetujuiOleh() != null || !delete);
			button.setTooltiptext("Hapus Data");
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
											Common.refreshDelete(HibernateUtil.currentSession(),
													pemakaianMasterAssetDetail);

											loadData(null);

										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
										PesanFormalHelper.tampilkanGagalException(
												"penghapusan data ini",
												"Data yang Bapak/Ibu coba hapus kemungkinan besar masih digunakan/direferensikan oleh data transaksi Asset lain di sistem (mis. dokumen pengadaan, penerimaan, pembayaran, peminjaman, atau riwayat terkait), sehingga database menolak penghapusan demi menjaga integritas data.",
												e,
												new String[] {
														"Periksa apakah data ini masih digunakan/dirujuk oleh transaksi atau data lain yang berelasi.",
														"Hapus atau ubah terlebih dahulu data yang masih berelasi tersebut, baru ulangi penghapusan data ini.",
														"Nonaktifkan saja data ini (bukan menghapus) apabila data ini memang masih perlu dirujuk oleh data lain." });

										}

									}

								}
							});

				}

			});
			button.setParent(toolbar);
			toolbar.setParent(row);

		}
	}

	@SuppressWarnings("unchecked")
	public void loadData(Object value) {

		List<PemakaianMasterAssetDetail> pemakaianMasterAssetDetails = initCriteria(true).list();

		ListModel strset = new SimpleListModel(pemakaianMasterAssetDetails);
		grid.setRowRenderer(new PemakaianMasterAssetDetailRenderer());
		grid.setModelCheckMobile(strset);

	}

	public void display() {
		ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
		groupbox.setStyle("min-height: 200px;");
		groupbox.setParent(this);
		groupbox.appendChild(new MyCaptionStyled("Daftar  Asset"));

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("40px");
		toolbar.setParent(groupbox);

		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Cari : ")));
		toolbar.appendChild(nama = new Textbox());
		nama.setCols(8);
		nama.addEventListener("onOK", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}

		});
		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}

		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Ambil Data Asset", "/img/add_item.png");
		button.setDisabled(pemakaianMasterAsset.getDisetujuiOleh() != null);
		button.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {
				Session session = HibernateUtil.currentSession();

				List<MasterAsset> masterAssets = session.createCriteria(PemakaianMasterAssetDetail.class)
						.setProjection(Projections.groupProperty("masterAsset"))
						.add(Restrictions.eq("pemakaianMasterAsset", pemakaianMasterAsset)).list();

				AmbilDataMasterAssetBanyak ambilDataMasterAssetBanyak = new AmbilDataMasterAssetBanyak(masterAssets,
						MasterAsset.TIPE_HABIS_PAKAI);
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(ambilDataMasterAssetBanyak);
				ambilDataMasterAssetBanyak.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						List<MasterAsset> masterAssets = (List<MasterAsset>) arg0.getData();

						for (MasterAsset masterAsset : masterAssets) {
							PemakaianMasterAssetDetail pemakaianMasterAssetDetail = new PemakaianMasterAssetDetail();
							pemakaianMasterAssetDetail.setMasterAsset(masterAsset);
							pemakaianMasterAssetDetail.setJumlah(0.0);
							pemakaianMasterAssetDetail.setKeterangan("");
							pemakaianMasterAssetDetail.setPemakaianMasterAsset(pemakaianMasterAsset);
							Common.refreshSaveOrUpdate(pemakaianMasterAssetDetail);
						}

						loadData(null);
					}
				});
				ambilDataMasterAssetBanyak.setWidth("850px");
				ambilDataMasterAssetBanyak.setHeight("97%");
				ambilDataMasterAssetBanyak.setVisible(true);
				ambilDataMasterAssetBanyak.onModal();
			}

		});
		button.setParent(toolbar);

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		toolbar.appendChild(cetakToolbarbutton);

		MyToolbarbuttonConfig upload = new MyToolbarbuttonConfig("Upload Data" + Common.ukuranLabelFileUpload(),
				"/img/excel.png");
		upload.setUpload(Common.ukuranFileUpload());
		Tbmuser tbmuser = Common.getCurrentUser();
		boolean merupakanAdmin = (tbmuser != null && tbmuser != null && tbmuser.hakAkses() != null
				&& tbmuser.hakAkses() != null && tbmuser != null && tbmuser.hakAkses() != null
				&& tbmuser.hakAkses().getRoleId() != null && (tbmuser != null && tbmuser.hakAkses() != null
						&& tbmuser.hakAkses().getRoleId().equalsIgnoreCase(Tbmrole.ADMINISTRATOR)));
		upload.setDisabled(!merupakanAdmin || pemakaianMasterAsset.getDisetujuiOleh() != null);
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
							PemakaianMasterAssetDetailAction.uploadDataMasterAsset(file, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									loadData(null);
									Clients.clearBusy();
								}
							}, contents, pemakaianMasterAsset);
						}
					}, "Harap tunggu.. sedang melakukan proses upload data..");

				} else {
					MyMessageboxConfig.show(
							"File yang anda upload harus ber-format Excel Open XML Spreadsheet (xlsx). Jika masih menggunakan format lain, buka file excel tersebut, kemudian Save As Excel Open XML Spreadsheet (xlsx). "
									+ media,
							"Error", MyMessageboxConfig.OK, MyMessageboxConfig.ERROR);
				}
			}
		});
		toolbar.appendChild(upload);

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(25);
		grid.setParent(groupbox);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Merk");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jenis");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Kelompok");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jumlah");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Keterangan");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("5%");

		loadData(null);
	}

	public static void uploadDataMasterAsset(final File file, final EventListener eventListener,
			final String[] contents, final PemakaianMasterAsset pemakaianMasterAsset) throws Exception {

		final ais.common.UploadReportHelper report = new ais.common.UploadReportHelper("Upload Pemakaian Master Asset");
		final Label downloadPath = new Label("");
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
					if (!downloadPath.getValue().isEmpty()) { try { org.zkoss.zul.Filedownload.save(new java.io.File(downloadPath.getValue()), "text/plain"); } catch (Exception eD) {} }
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

					Session session = HibernateUtil.currentNativeSession();

					int rowCount = (sheet.getLastRowNum() + 1);
					for (int i = 1; i < rowCount; i++) {
						try {

							Long id = Common.getSheetContentAsLong(sheet, 0, i);
							PemakaianMasterAssetDetail pemakaianMasterAssetDetail = id == null || id.equals(-1L) ? null
									: (PemakaianMasterAssetDetail) session
											.createCriteria(PemakaianMasterAssetDetail.class).add(Restrictions.idEq(id))
											.uniqueResult();
							MasterAsset masterAsset = (MasterAsset) Common.getSheetContentAsObject(sheet, 1, i,
									MasterAsset.class);
							Double jumlah = Common.getSheetContentAsDouble(sheet, 2, i);
							if (masterAsset == null) {
								String isbn = Common.getSheetContentAsString(sheet, 1, i);
								if (isbn != null && !isbn.trim().isEmpty()) {
									masterAsset = (MasterAsset) session.createCriteria(MasterAsset.class)
											.add(Restrictions.or(Restrictions.eq("isbn", isbn.trim()),
													Restrictions.eq("isbn10", isbn.trim())))
											.addOrder(Order.desc("id")).setMaxResults(1).uniqueResult();
								}
							}
							if (masterAsset == null) {
								continue;
							}

							if (pemakaianMasterAssetDetail == null) {
								pemakaianMasterAssetDetail = (PemakaianMasterAssetDetail) session
										.createCriteria(PemakaianMasterAssetDetail.class)
										.add(Restrictions.eq("masterAsset", masterAsset))
										.add(Restrictions.eq("pemakaianMasterAsset", pemakaianMasterAsset))
										.addOrder(Order.desc("id")).setMaxResults(1).uniqueResult();
							}

							if (pemakaianMasterAssetDetail == null) {
								pemakaianMasterAssetDetail = new PemakaianMasterAssetDetail();
							}

							pemakaianMasterAssetDetail.setJumlah(jumlah);
							pemakaianMasterAssetDetail.setPemakaianMasterAsset(pemakaianMasterAsset);
							pemakaianMasterAssetDetail.setMasterAsset(masterAsset);

							session.getTransaction().begin();
							session.saveOrUpdate(pemakaianMasterAssetDetail);
							session.getTransaction().commit();

							label.setValue("Upload data \"" + masterAsset.toString() + "\" ("
									+ Common.numberFormat.get().format(i * 100.0 / rowCount) + " %)");
							report.sukses(i, masterAsset.toString(), "");

						} catch (Exception e) {
							report.gagal(i, "baris-" + i, e, "Pastikan kode asset, tanggal, dan nilai perolehan sudah benar.");
							Common.tampilErrorJikaAdmin(e);
						}

					}
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/asset/helper/PemakaianMasterAssetDetailAction.java:475");
				}

				HibernateUtil.closeSession();

				try {
					java.io.File rptFile = report.simpanLaporan();
					downloadPath.setValue(rptFile.getAbsolutePath());
				} catch (Exception eR) { ais.common.ErrorAuditUtil.record(eR, "auto-audit(empty-catch) PemakaianMasterAssetDetailAction laporan"); }
				label.setValue("");
							} finally {
					ais.database.hibernate.HibernateUtil.closeSession();
				}
			}
		}).start();
	}

	@Override
	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		return session.createCriteria(PemakaianMasterAssetDetail.class)

				.createAlias("masterAsset", "masterAsset")

				.add(nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.or(
								Restrictions.ilike("masterAsset.kode", nama.getValue().trim(), MatchMode.ANYWHERE),
								Restrictions.ilike("masterAsset.nama", nama.getValue().trim(), MatchMode.ANYWHERE)))

				.addOrder(Order.asc("masterAsset.nama"))
				.add(Restrictions.eq("pemakaianMasterAsset", pemakaianMasterAsset)).setMaxResults(10000);
	}

}
