package ais.action.master.asset.helper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Columns;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Space;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.asset.AssetDetail;
import ais.database.model.asset.MasterAsset;
import ais.database.model.asset.PenghapusanMasterAsset;
import ais.database.model.asset.PenghapusanMasterAssetDetail;
import ais.database.model.file.LampiranLain;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Controller/action ZK untuk penghapusan master asset detail. Tipe ini merupakan titik masuk UI
 * yang menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus
 * oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyDetail}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code PenghapusanMasterAsset
 * penghapusanMasterAsset}, {@code MyGrid grid}, {@code boolean edit}, {@code boolean delete}, {@code Textbox
 * barcode}; pembacaan/pencarian ({@code loadData()}, {@code loadBarcode()}); operasi domain lain ({@code
 * display()}); konfigurasi constructor: {@code delete}, {@code edit}. Bagian lain dari kontrak tetap mengikuti
 * kelas induk atau interface yang disebut di atas.</p>
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
public class PenghapusanMasterAssetDetailAction extends MyDetail {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5086031585928643232L;

	private PenghapusanMasterAsset penghapusanMasterAsset;
	private MyGrid grid;
	private boolean edit = false;
	private boolean delete = false;

	private Textbox barcode;

	public PenghapusanMasterAssetDetailAction(PenghapusanMasterAsset penghapusanMasterAsset, Boolean disetujui) {
		super();
		this.penghapusanMasterAsset = penghapusanMasterAsset;
		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		this.addEventListener(Events.ON_OPEN, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(PenghapusanMasterAssetDetailAction.this);
				if (isOpen()) {
					display();
				}
			}
		});
	}

	class PenghapusanMasterAssetDetailRenderer extends ais.ui.util.MyRowRenderer {

		public PenghapusanMasterAssetDetailRenderer() {

		}

		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");
			final PenghapusanMasterAssetDetail penghapusanMasterAssetDetail = (PenghapusanMasterAssetDetail) data;

			new Label(penghapusanMasterAssetDetail.getAssetDetail() == null ? ""
					: penghapusanMasterAssetDetail.getAssetDetail().getBarcode()).setParent(row);

			Vbox a;
			(a = RevisiHelper.createNewRevisi(PenghapusanMasterAssetDetail.class, penghapusanMasterAssetDetail,
					penghapusanMasterAssetDetail.getAssetDetail() == null ? ""
							: penghapusanMasterAssetDetail.getAssetDetail().getNama()))
					.setParent(row);

			Hbox hbox = new Hbox();
			hbox.setParent(a);

			LampiranLain.createDownloadUploadFileLain(hbox, penghapusanMasterAssetDetail.getId(),
					PenghapusanMasterAssetDetail.class.getName(), "Gambar", false, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

						}
					}, null, false, false, false, true, null, false, true);

			new Label(penghapusanMasterAssetDetail.getPenyusutanAsset() == null ? ""
					: Common.numberFormat.get().format(penghapusanMasterAssetDetail.getPenyusutanAsset().getNilaiBuku()))
					.setParent(row);

			final MyDoublebox hargaBeli = new MyDoublebox(penghapusanMasterAssetDetail.getHargaBeli() == null ? 0.0
					: penghapusanMasterAssetDetail.getHargaBeli());

			(hargaBeli).setParent(row);
			hargaBeli.setDisabled(
					penghapusanMasterAssetDetail.getPenghapusanMasterAsset().getDisetujuiOleh() != null || !edit);
			hargaBeli.setStyle("text-align:right");
			hargaBeli.setWidth("90%");
			hargaBeli.addEventListener(Events.ON_CHANGE, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					penghapusanMasterAssetDetail.setHargaBeli(hargaBeli.getValue());
					Common.refreshUpdate(session, (penghapusanMasterAssetDetail));

					MasterAsset masterAsset = penghapusanMasterAssetDetail.getMasterAsset();
					session.refresh(masterAsset);
					masterAsset.setHargaBeliDefault(hargaBeli.getValue());
					Common.refreshUpdate(session, masterAsset);

				}
			});

			final MyTextbox keterangan = new MyTextbox(penghapusanMasterAssetDetail.getKeterangan() == null ? ""
					: penghapusanMasterAssetDetail.getKeterangan());
			keterangan.setWidth("90%");
			keterangan.setHeight("95%");
			keterangan.setParent(row);
			keterangan.setDisabled(
					penghapusanMasterAssetDetail.getPenghapusanMasterAsset().getDisetujuiOleh() != null || !edit);
			keterangan.addEventListener(Events.ON_CHANGE, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					penghapusanMasterAssetDetail.setKeterangan(keterangan.getValue());
					Common.refreshUpdate(session, (penghapusanMasterAssetDetail));
				}
			});

			Hbox toolbar = new Hbox();

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setDisabled(
					penghapusanMasterAssetDetail.getPenghapusanMasterAsset().getDisetujuiOleh() != null || !delete);
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

											Common.refreshDelete(penghapusanMasterAssetDetail);

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
		Session session = HibernateUtil.currentSession();
		List<PenghapusanMasterAssetDetail> penghapusanMasterAssetDetails = session
				.createCriteria(PenghapusanMasterAssetDetail.class).addOrder(Order.desc("id"))
				.add(Restrictions.eq("penghapusanMasterAsset", penghapusanMasterAsset)).list();

		ListModel strset = new SimpleListModel(penghapusanMasterAssetDetails);
		grid.setRowRenderer(new PenghapusanMasterAssetDetailRenderer());
		grid.setModelCheckMobile(strset);

	}

	public void display() {

		ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
		groupbox.setStyle("min-height: 200px;");
		groupbox.setParent(this);

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("40px");
		toolbar.setParent(groupbox);
		if (penghapusanMasterAsset.getDisetujuiOleh() == null) {
			MyToolbarbuttonConfig add = new MyToolbarbuttonConfig("Ambil Data Alat/Fasilitas", "/img/new.gif");
			add.setParent(toolbar);
			add.setVisible(penghapusanMasterAsset.getDisetujuiOleh() == null);
			add.setTooltiptext("Tambah");
			add.addEventListener("onClick", new EventListener() {
				@SuppressWarnings("unchecked")
				@Override
				public void onEvent(Event event) throws Exception {

					List<AssetDetail> masterAssets = new ArrayList<AssetDetail>();

					Session session = HibernateUtil.currentSession();
					List<PenghapusanMasterAssetDetail> penghapusanMasterAssetDetails = session
							.createCriteria(PenghapusanMasterAssetDetail.class).addOrder(Order.desc("id"))
							.add(Restrictions.eq("penghapusanMasterAsset", penghapusanMasterAsset)).list();
					for (PenghapusanMasterAssetDetail penghapusanMasterAssetDetail : penghapusanMasterAssetDetails) {
						AssetDetail assetDetail = penghapusanMasterAssetDetail.getAssetDetail();
						if (assetDetail != null) {
							masterAssets.add(assetDetail);
						}
					}

					AmbilDataAssetDetailBanyak ambilDataAssetDetailBanyak = new AmbilDataAssetDetailBanyak(masterAssets,
							null);
					ambilDataAssetDetailBanyak.setHeight("95%");
					ambilDataAssetDetailBanyak.setWidth("90%");
					ambilDataAssetDetailBanyak
							.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
					ambilDataAssetDetailBanyak.setEventListener(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							Calendar calendar1 = ais.ui.util.WaktuUtil.getCalendar();
							calendar1.setTime(penghapusanMasterAsset.getTanggalPembuatan());

							Session session = HibernateUtil.currentSession();
							List<AssetDetail> assetDetails = (List<AssetDetail>) arg0.getData();
							for (AssetDetail assetDetail : assetDetails) {
								PenghapusanMasterAssetDetail penghapusanAssetDetailDetail = new PenghapusanMasterAssetDetail();
								penghapusanAssetDetailDetail.setAssetDetail(assetDetail);
								penghapusanAssetDetailDetail.setKeterangan("");
								penghapusanAssetDetailDetail.setPenghapusanMasterAsset(penghapusanMasterAsset);
								penghapusanAssetDetailDetail.setMasterAsset(assetDetail.getAsset().getMasterAsset());

								PenghapusanMasterAssetHelper.masukkanPenyusutan(calendar1, penghapusanMasterAsset,
										penghapusanAssetDetailDetail, assetDetail, session);

								if (penghapusanMasterAsset.getId() != null) {
									session.save(penghapusanAssetDetailDetail);
									session.flush();
								}

							}

							loadData(null);
						}
					});

					ambilDataAssetDetailBanyak.onModal();

				}
			});
		}

		new Space().setParent(toolbar);
		new Space().setParent(toolbar);
		new Space().setParent(toolbar);

		new Label("Kode/Nama").setParent(toolbar);
		new Space().setParent(toolbar);
		barcode = new Textbox();
		barcode.setDisabled(penghapusanMasterAsset.getDisetujuiOleh() != null);
		barcode.setParent(toolbar);
		barcode.addEventListener("onOK", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadBarcode();
			}
		});

		MyToolbarbuttonConfig cari = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		cari.setDisabled(penghapusanMasterAsset.getDisetujuiOleh() != null);
		cari.setParent(toolbar);
		cari.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadBarcode();
			}
		});

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(50);
		grid.setParent(groupbox);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Kode");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nilai Buku");
		column.setWidth("10%");
		column.setAlign("right");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Harga Jual");
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

	public void loadBarcode() throws Exception {
		String barcode = this.barcode.getText().trim();
		if (barcode.trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, field Kode/Nama belum diisi. Langkah yang dapat dilakukan: (1) Scan barcode barang atau ketik kode/nama aset yang akan dihapus; (2) Pastikan kode atau nama sesuai dengan data di master aset; (3) ulangi proses pencarian. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return;
		}

		Session session = HibernateUtil.currentSession();

		MasterAsset masterAsset = null;

		masterAsset = (MasterAsset) session.createCriteria(MasterAsset.class)
				.add(Restrictions.or(Restrictions.ilike("kode", barcode, MatchMode.EXACT),
						Restrictions.ilike("nama", barcode, MatchMode.EXACT)))
				.setMaxResults(1).uniqueResult();

		if (masterAsset == null) {
			MyMessageboxConfig.show("Kode " + barcode + " tidak ditemukan", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return;
		}

		PenghapusanMasterAssetDetail penghapusanMasterAssetDetail = new PenghapusanMasterAssetDetail();
		penghapusanMasterAssetDetail.setMasterAsset(masterAsset);
//		penghapusanMasterAssetDetail.setJumlah(1.0);
		penghapusanMasterAssetDetail.setKeterangan("");
		penghapusanMasterAssetDetail.setPenghapusanMasterAsset(penghapusanMasterAsset);
		session.save(penghapusanMasterAssetDetail);
		loadData(null);
		this.barcode.focus();
		this.barcode.select();
	}

}
