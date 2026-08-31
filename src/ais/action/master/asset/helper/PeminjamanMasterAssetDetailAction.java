package ais.action.master.asset.helper;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
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

import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.asset.MasterAsset;
import ais.database.model.asset.PeminjamanMasterAsset;
import ais.database.model.asset.PeminjamanMasterAssetDetail;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.WaktuUtil;

/**
 * Controller/action ZK untuk peminjaman master asset detail. Tipe ini merupakan titik masuk UI
 * yang menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus
 * oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyDetail}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code PeminjamanMasterAsset
 * peminjamanMasterAsset}, {@code MyGrid grid}, {@code boolean edit}, {@code boolean delete}, {@code Textbox
 * barcode}, {@code Boolean pengembalian}; pembacaan/pencarian ({@code loadData()}, {@code loadBarcode()});
 * operasi domain lain ({@code display()}); konfigurasi constructor: {@code delete}, {@code edit}. Bagian lain
 * dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
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
public class PeminjamanMasterAssetDetailAction extends MyDetail {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5086031585928643232L;

	private PeminjamanMasterAsset peminjamanMasterAsset;
	private MyGrid grid;
	private boolean edit = false;
	private boolean delete = false;

	private Textbox barcode;

	private Boolean pengembalian = false;

	public PeminjamanMasterAssetDetailAction(PeminjamanMasterAsset peminjamanMasterAsset, Boolean pengembalian) {
		super();
		this.pengembalian = pengembalian;
		this.peminjamanMasterAsset = peminjamanMasterAsset;
		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		this.addEventListener(Events.ON_OPEN, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(PeminjamanMasterAssetDetailAction.this);
				if (isOpen()) {
					display();
				}
			}
		});
	}

	class PeminjamanMasterAssetDetailRenderer extends ais.ui.util.MyRowRenderer {

		public PeminjamanMasterAssetDetailRenderer() {

		}

		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");
			final PeminjamanMasterAssetDetail peminjamanMasterAssetDetail = (PeminjamanMasterAssetDetail) data;

			new Label(peminjamanMasterAssetDetail.getAssetDetail() == null ? ""
					: peminjamanMasterAssetDetail.getAssetDetail().getBarcode()).setParent(row);

			RevisiHelper.createNewRevisi(PeminjamanMasterAssetDetail.class, peminjamanMasterAssetDetail,
					peminjamanMasterAssetDetail.getMasterAsset() == null ? ""
							: peminjamanMasterAssetDetail.getMasterAsset().getNama())
					.setParent(row);
			boolean persetujuan;
			if (pengembalian) {
				persetujuan = peminjamanMasterAssetDetail.getPeminjamanMasterAsset()
						.getPengembalianMasterAsset() != null
						&& peminjamanMasterAssetDetail.getPeminjamanMasterAsset().getPengembalianMasterAsset()
								.getDisetujuiOleh() != null;
				final MyDatebox waktuPengembalian = new MyDatebox(peminjamanMasterAssetDetail.getWaktuPengembalian());

				final MyCheckboxConfig dikembalikan = new MyCheckboxConfig("Dikembalikan");
				dikembalikan.setChecked(peminjamanMasterAssetDetail.getDikembalikan());

				if (persetujuan) {
					new Label("Dikembalikan : " + (peminjamanMasterAssetDetail.getDikembalikan() ? "Ya" : "Tidak"))
							.setParent(row);
				} else {
					dikembalikan.setParent(row);
				}
				dikembalikan.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Session session = HibernateUtil.currentSession();
						peminjamanMasterAssetDetail.setDikembalikan(dikembalikan.isChecked());
						waktuPengembalian.setDisabled(dikembalikan.isChecked());
						if (dikembalikan.isChecked() && waktuPengembalian.getValue() == null) {
							waktuPengembalian.setValue(WaktuUtil.getDate());
							peminjamanMasterAssetDetail.setWaktuPengembalian(waktuPengembalian.getValue());
						}

						Common.refreshUpdate(session, (peminjamanMasterAssetDetail));

					}
				});
				waktuPengembalian.setDisabled(dikembalikan.isChecked());
				waktuPengembalian.setFormat(Common.dateFormat.get().toPattern());

				if (persetujuan) {
					new Label(peminjamanMasterAssetDetail.getWaktuPengembalian() == null ? ""
							: Common.dateFormat.get().format(peminjamanMasterAssetDetail.getWaktuPengembalian()))
							.setParent(row);
				} else {
					(waktuPengembalian).setParent(row);
				}
				waktuPengembalian.setWidth("90%");
				waktuPengembalian.addEventListener(Events.ON_CHANGE, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Session session = HibernateUtil.currentSession();
						peminjamanMasterAssetDetail.setWaktuPengembalian(waktuPengembalian.getValue());
						Common.refreshUpdate(session, (peminjamanMasterAssetDetail));

					}
				});

			} else {
				persetujuan = peminjamanMasterAssetDetail.getPeminjamanMasterAsset().getDisetujuiOleh() != null;
			}

			final MyTextbox keterangan = new MyTextbox(peminjamanMasterAssetDetail.getKeterangan() == null ? ""
					: peminjamanMasterAssetDetail.getKeterangan());
			keterangan.setWidth("90%");
			keterangan.setHeight("95%");

			if (persetujuan) {
				new Label(peminjamanMasterAssetDetail.getKeterangan()).setParent(row);
			} else {

				keterangan.setParent(row);
			}
			keterangan.setDisabled(
					peminjamanMasterAssetDetail.getPeminjamanMasterAsset().getDisetujuiOleh() != null || !edit);
			keterangan.addEventListener(Events.ON_CHANGE, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					peminjamanMasterAssetDetail.setKeterangan(keterangan.getValue());
					Common.refreshUpdate(session, (peminjamanMasterAssetDetail));
				}
			});

			Hbox toolbar = new Hbox();
			if (persetujuan) {
				MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
				button.setDisabled(
						peminjamanMasterAssetDetail.getPeminjamanMasterAsset().getDisetujuiOleh() != null || !delete);
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

												Common.refreshDelete(peminjamanMasterAssetDetail);

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
			}
			toolbar.setParent(row);

		}
	}

	@SuppressWarnings("unchecked")
	public void loadData(Object value) {
		Session session = HibernateUtil.currentSession();
		List<PeminjamanMasterAssetDetail> peminjamanMasterAssetDetails = session
				.createCriteria(PeminjamanMasterAssetDetail.class).addOrder(Order.desc("id"))
				.add(Restrictions.eq("peminjamanMasterAsset", peminjamanMasterAsset)).list();

		ListModel strset = new SimpleListModel(peminjamanMasterAssetDetails);
		grid.setRowRenderer(new PeminjamanMasterAssetDetailRenderer());
		grid.setModelCheckMobile(strset);

	}

	public void display() {

		ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
		groupbox.setStyle("min-height: 200px;");
		groupbox.setParent(this);
		groupbox.appendChild(new MyCaptionStyled("Permintaan Pengadaan"));

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("40px");
		toolbar.setParent(groupbox);
		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Ambil Data Aset", "/img/add_item.png");
		button.setDisabled(peminjamanMasterAsset.getDisetujuiOleh() != null);
		button.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {
				Session session = HibernateUtil.currentSession();

				List<MasterAsset> masterAssets = session.createCriteria(PeminjamanMasterAssetDetail.class)
						.setProjection(Projections.groupProperty("masterAsset"))
						.add(Restrictions.eq("peminjamanMasterAsset", peminjamanMasterAsset)).list();

				AmbilDataMasterAssetBanyak ambilDataMasterAssetBanyak = new AmbilDataMasterAssetBanyak(masterAssets,
						null);
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(ambilDataMasterAssetBanyak);
				ambilDataMasterAssetBanyak.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						List<MasterAsset> masterAssets = (List<MasterAsset>) arg0.getData();
						Session session = HibernateUtil.currentSession();
						for (MasterAsset masterAsset : masterAssets) {
							PeminjamanMasterAssetDetail peminjamanMasterAssetDetail = new PeminjamanMasterAssetDetail();
							peminjamanMasterAssetDetail.setMasterAsset(masterAsset);
							// peminjamanMasterAssetDetail.setJumlah(0.0);
							peminjamanMasterAssetDetail.setKeterangan("");
							peminjamanMasterAssetDetail.setPeminjamanMasterAsset(peminjamanMasterAsset);
							session.save(peminjamanMasterAssetDetail);
						}

						loadData(null);
					}
				});
				ambilDataMasterAssetBanyak.setWidth("97%");
				ambilDataMasterAssetBanyak.setHeight("97%");
				ambilDataMasterAssetBanyak.setVisible(true);
				ambilDataMasterAssetBanyak.onModal();
			}

		});
		button.setParent(toolbar);

		new Space().setParent(toolbar);
		new Space().setParent(toolbar);
		new Space().setParent(toolbar);

		new Label("Kode/Nama").setParent(toolbar);
		new Space().setParent(toolbar);
		barcode = new Textbox();
		barcode.setDisabled(peminjamanMasterAsset.getDisetujuiOleh() != null);
		barcode.setParent(toolbar);
		barcode.addEventListener("onOK", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadBarcode();
			}
		});

		MyToolbarbuttonConfig cari = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		cari.setDisabled(peminjamanMasterAsset.getDisetujuiOleh() != null);
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
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");
		column.setWidth("20%");

		if (pengembalian) {
			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Dikembalikan");
			column.setWidth("12%");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Tgl. Pengembalian");
			column.setWidth("12%");
		}

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
			MyMessageboxConfig.show("Mohon maaf, field Kode/Nama belum diisi. Langkah yang dapat dilakukan: (1) Scan barcode barang atau ketik kode/nama aset yang akan dipinjam; (2) Pastikan kode atau nama sesuai dengan data di master aset; (3) ulangi proses pencarian. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
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

		PeminjamanMasterAssetDetail peminjamanMasterAssetDetail = new PeminjamanMasterAssetDetail();
		peminjamanMasterAssetDetail.setMasterAsset(masterAsset);
//		peminjamanMasterAssetDetail.setJumlah(1.0);
		peminjamanMasterAssetDetail.setKeterangan("");
		peminjamanMasterAssetDetail.setPeminjamanMasterAsset(peminjamanMasterAsset);
		session.save(peminjamanMasterAssetDetail);
		loadData(null);
		this.barcode.focus();
		this.barcode.select();
	}

}
