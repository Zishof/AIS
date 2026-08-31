package ais.action.master.library.helper;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Image;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.library.util.LibraryUtil;
import ais.action.report.Report;
import ais.common.BarcodeCommon;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.library.SaldoAwalDetail;
import ais.database.model.library.SaldoAwalDetailDetail;
import ais.database.model.library.StatusItem;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import net.sourceforge.barbecue.Barcode;
import net.sourceforge.barbecue.BarcodeFactory;
import net.sourceforge.barbecue.BarcodeImageHandler;

/**
 * Controller/action ZK untuk saldo awal detail detail. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyDetail}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code SaldoAwalDetail saldoAwalDetail},
 * {@code MyGrid grid}, {@code boolean add}, {@code boolean delete}, {@code boolean edit}, {@code List
 * saldoAwalDetailDetails}, {@code EventListener eventListener}; pembacaan/pencarian ({@code loadData()});
 * operasi domain lain ({@code display()}); konfigurasi constructor: {@code add}, {@code delete}, {@code edit}.
 * Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
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
public class SaldoAwalDetailDetailAction extends MyDetail {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5086031585928643232L;

	private SaldoAwalDetail saldoAwalDetail;
	private MyGrid grid;

	private boolean add = false;
	private boolean delete = false;
	private boolean edit = false;

	private List<SaldoAwalDetailDetail> saldoAwalDetailDetails;
	private EventListener eventListener;

	public SaldoAwalDetailDetailAction(SaldoAwalDetail saldoAwalDetail, EventListener eventListener) {
		super();
		add = CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		this.saldoAwalDetail = saldoAwalDetail;
		this.eventListener = eventListener;
		this.addEventListener(Events.ON_OPEN, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(SaldoAwalDetailDetailAction.this);
				if (isOpen()) {
					display();
				}
			}
		});
	}

	class SaldoAwalDetailDetailRenderer extends ais.ui.util.MyRowRenderer {

		public SaldoAwalDetailDetailRenderer() {

		}

		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");
			final SaldoAwalDetailDetail saldoAwalDetailDetail = (SaldoAwalDetailDetail) data;

			final Vbox barcodeLabel = RevisiHelper.createNewRevisi(SaldoAwalDetailDetail.class, saldoAwalDetailDetail,
					saldoAwalDetailDetail.getBarcode() == null ? "" : saldoAwalDetailDetail.getBarcode());

			barcodeLabel.setParent(row);

			final Image image = BarcodeCommon.generateBarcodeImage(saldoAwalDetailDetail.getBarcode());

			(image).setParent(row);

			final Textbox barcode = new Textbox(saldoAwalDetailDetail.getBarcode());
			barcode.setDisabled(
					saldoAwalDetailDetail.getSaldoAwalDetail().getSaldoAwal().getDisetujuiOleh() != null || !edit);
			barcode.setWidth("90%");
			barcode.setParent(row);
			barcode.addEventListener(Events.ON_CHANGE, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					saldoAwalDetailDetail.setBarcode(barcode.getValue().trim());
					Common.refreshUpdate(session, (saldoAwalDetailDetail));

					image.setContent(BarcodeCommon.generateBarcodeAImage(saldoAwalDetailDetail.getBarcode()));
				}
			});

			final Combobox status = new Combobox();
			Common.insertCombo(status, "nama", StatusItem.class);
			Common.selectComboItem(status, saldoAwalDetailDetail.getStatusItem());
			status.setWidth("90%");
			status.setHeight("95%");
			status.setParent(row);
			status.setDisabled(
					saldoAwalDetailDetail.getSaldoAwalDetail().getSaldoAwal().getDisetujuiOleh() != null || !edit);
			status.addEventListener(Events.ON_CHANGE, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					saldoAwalDetailDetail.setStatusItem((StatusItem) (status.getSelectedItem() == null || status.getSelectedItem().getValue() == null ? null
							: status.getSelectedItem().getValue()));
					Common.refreshUpdate(session, (saldoAwalDetailDetail));
				}
			});

			final MyTextbox keterangan = new MyTextbox(
					saldoAwalDetailDetail.getKeterangan() == null ? "" : saldoAwalDetailDetail.getKeterangan());
			keterangan.setWidth("90%");
			keterangan.setHeight("95%");
			keterangan.setParent(row);
			keterangan.setDisabled(
					saldoAwalDetailDetail.getSaldoAwalDetail().getSaldoAwal().getDisetujuiOleh() != null || !edit);
			keterangan.addEventListener(Events.ON_CHANGE, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					saldoAwalDetailDetail.setKeterangan(keterangan.getValue());
					Common.refreshUpdate(session, (saldoAwalDetailDetail));
				}
			});

			Hbox toolbar = new Hbox();

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setDisabled(
					saldoAwalDetailDetail.getSaldoAwalDetail().getSaldoAwal().getDisetujuiOleh() != null && delete);
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

									Common.refreshDelete(saldoAwalDetailDetail);

									loadData(null);

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
			toolbar.setParent(row);

		}
	}

	@SuppressWarnings("unchecked")
	public void loadData(Object value) throws Exception {
		Session session = HibernateUtil.currentSession();
		saldoAwalDetailDetails = saldoAwalDetail == null ? new ArrayList<SaldoAwalDetailDetail>()
				: session.createCriteria(SaldoAwalDetailDetail.class).addOrder(Order.desc("id"))
						.add(Restrictions.eq("saldoAwalDetail", saldoAwalDetail)).list();

		ListModel strset = new SimpleListModel(saldoAwalDetailDetails);
		grid.setRowRenderer(new SaldoAwalDetailDetailRenderer());
		grid.setModelCheckMobile(strset);

		eventListener.onEvent(new Event("", grid, saldoAwalDetailDetails));
	}

	public void display() throws Exception {
		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(this);
		panel.setWidth("100%");
		panel.setHeight("430px");
		panel.setTitle("Daftar Item Saldo Awal Detail");
		panel.setBorder("none");
		panel.setStyle("border:0px;");

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(panel);
		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Tambah Item", "/img/add_item.png");
		button.setDisabled(saldoAwalDetail.getSaldoAwal().getDisetujuiOleh() != null && add);
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {

				final MyWindow window = new MyWindow("Item Batch", "none", true);
				window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				window.setHeight("140px");
				window.setWidth("550px");

				final Intbox jumlahItem = new Intbox(0);

				Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
				borderlayout.setParent(window);
				Center center = new Center();
				center.setParent(borderlayout);
				ais.ui.util.ZkCompat.setFlex(center, true);

				MyGrid grid = new MyGrid();
				grid.setWidth("100%");
				grid.setParent(center);

				Columns columns = new Columns();
				columns.setParent(grid);

				MyColumnConfig column = new MyColumnConfig();
				column.setParent(columns);
				column.setWidth("20%");

				column = new MyColumnConfig();
				column.setParent(columns);
				column.setWidth("90%");

				Rows rows = new Rows();
				rows.setParent(grid);

				MyFormRow row = new MyFormRow();row.setValign("top");
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Jumlah Item"));
				row.appendChild(jumlahItem);
				jumlahItem.setWidth("90%");

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
						window.detach();
					}
				});
				cancel.setParent(toolbar);
				MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
				save.setTooltiptext("Simpan");
				save.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						Session session = HibernateUtil.currentSession();
						for (int i = 0; i < jumlahItem.getValue(); i++) {
							SaldoAwalDetailDetail saldoAwalDetailDetail = new SaldoAwalDetailDetail();
							saldoAwalDetailDetail.setStatusItem(LibraryUtil.AKTIF);
							saldoAwalDetailDetail.setSaldoAwalDetail(saldoAwalDetail);
							saldoAwalDetailDetail.setBarcode(BarcodeCommon.generateCode());
							session.save(saldoAwalDetailDetail);
						}

						loadData(null);
						window.detach();
					}
				});
				save.setParent(toolbar);

				window.onModal();
			}

		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Hapus Semua Item", "/img/svg/trash.svg");
		button.setParent(toolbar);
		button.setDisabled(saldoAwalDetail.getSaldoAwal().getDisetujuiOleh() != null && delete);
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
						MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {

					@Override
					public void onEvent(Event event) throws Exception {
						int i = Integer.parseInt(event.getData().toString());
						if (i == MyMessageboxConfig.OK) {
							String sql = "delete from library.saldo_awal_detail_detail where saldo_awal_detail = "
									+ saldoAwalDetail.getId();

							HibernateUtil.currentSession().createSQLQuery(sql).executeUpdate();
							loadData(null);

						}

					}
				});
			}
		});

		button = new MyToolbarbuttonConfig("Barcode Semua Item", "/img/print.png");
		button.setParent(toolbar);
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				@SuppressWarnings("rawtypes")
				final Map parameters = ais.common.HashMapGenerator.getRand();
				List<Map<String, Serializable>> maps = new ArrayList<Map<String, Serializable>>();
				for (SaldoAwalDetailDetail saldoAwalDetailDetail : saldoAwalDetailDetails) {
					Map<String, Serializable> map = new java.util.HashMap<String, Serializable>();
					map.put("kode", saldoAwalDetailDetail.getSaldoAwalDetail().getItem().getIsbn());
					map.put("judul", saldoAwalDetailDetail.getSaldoAwalDetail().getItem().getNama());

					final File myfilebarcode = new File(Common.ambilREAL_PATH_REPORT()
							+ "/barcode_" + saldoAwalDetailDetail.getBarcode() + ".png");

					Barcode mybarcode = BarcodeFactory.createCode128B(saldoAwalDetailDetail.getBarcode());
					BarcodeImageHandler.savePNG(mybarcode, myfilebarcode);
					map.put("barcode", myfilebarcode.getAbsolutePath());
					maps.add(map);
				}
				Report.generatePDFReport(Report.PDF, parameters, "library/barcode_saldo_awal", ais.ui.util.WaktuUtil.getDate(), maps
						);
			}
		});

		button = new MyToolbarbuttonConfig("QRcode Semua Item", "/img/print.png");
		button.setParent(toolbar);
		button.addEventListener("onClick", new EventListener() {

			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			public void onEvent(Event arg0) throws Exception {
				Session session = HibernateUtil.currentSession();
				saldoAwalDetailDetails = saldoAwalDetail == null ? new ArrayList<SaldoAwalDetailDetail>()
						: session.createCriteria(SaldoAwalDetailDetail.class).addOrder(Order.desc("id"))
								.add(Restrictions.eq("saldoAwalDetail", saldoAwalDetail)).list();

				final Map parameters = ais.common.HashMapGenerator.getRand();
				List<Map<String, Serializable>> maps = new ArrayList<Map<String, Serializable>>();
				for (SaldoAwalDetailDetail saldoAwalDetailDetail : saldoAwalDetailDetails) {
					Map<String, Serializable> map = new java.util.HashMap<String, Serializable>();
					map.put("kode", saldoAwalDetailDetail.getSaldoAwalDetail().getItem().getIsbn());
					map.put("judul", saldoAwalDetailDetail.getSaldoAwalDetail().getItem().getNama());

					File myfilebarcode = new File(Common.ambilREAL_PATH_REPORT() + "/barcode_"
							+ saldoAwalDetailDetail.getBarcode() + ".png");

					Barcode mybarcode = BarcodeFactory.createCode128B(saldoAwalDetailDetail.getBarcode());
					BarcodeImageHandler.savePNG(mybarcode, myfilebarcode);
					map.put("barcode", myfilebarcode.getAbsolutePath());

					String code = saldoAwalDetailDetail.getBarcode() + ";"
							+ saldoAwalDetailDetail.getSaldoAwalDetail().getItem().getIsbn() + ";"
							+ saldoAwalDetailDetail.getSaldoAwalDetail().getItem().getNama() + ";"
							+ (saldoAwalDetailDetail.getSaldoAwalDetail().getItem().getPenerbit() == null ? ""
									: saldoAwalDetailDetail.getSaldoAwalDetail().getItem().getPenerbit().getNama())
							+ ";" + (saldoAwalDetailDetail.getSaldoAwalDetail().getItem().getJenisItem() == null ? ""
									: saldoAwalDetailDetail.getSaldoAwalDetail().getItem().getJenisItem().getNama());

					myfilebarcode = new File(Common.ambilREAL_PATH_REPORT() + "/crcode_"
							+ saldoAwalDetailDetail.getBarcode() + ".png");

					BarcodeCommon.generateCRCode(code, myfilebarcode);
					map.put("cr_code", myfilebarcode.getAbsolutePath());

					maps.add(map);
				}
				Report.generatePDFReport(Report.PDF, parameters, "library/crcode_saldo_awal", ais.ui.util.WaktuUtil.getDate(), maps
						);
			}
		});

		Panelchildren panelchildren = new Panelchildren();
		panelchildren.setParent(panel);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(panelchildren);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(10);grid.getPagingChild().setMold("os");
		grid.setParent(center);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Barcode");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Image");
		column.setWidth("25%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Ubah");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Status");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Keterangan");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("5%");

		loadData(null);
	}

}
