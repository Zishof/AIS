package ais.action.master.asset.helper;
/* ENHANCED_PENGGUNAAN_ANGGARAN_MEMORY_SAFE_2026_06_03 - Java 1.6/1.7 compatible. */

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
import org.zkoss.zul.Vbox;

import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.asset.Asset;
import ais.database.model.asset.MasterAsset;
import ais.database.model.asset.PenerimaanPengadaanMasterAsset;
import ais.database.model.asset.PenerimaanPengadaanMasterAssetDetail;
import ais.database.model.asset.PermintaanPengadaanMasterAsset;
import ais.database.model.asset.PermintaanPengadaanMasterAssetDetail;
import ais.database.model.file.LampiranLain;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Controller/action ZK untuk permintaan pengadaan master asset detail. Tipe ini merupakan titik
 * masuk UI yang menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi
 * khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyDetail}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code PermintaanPengadaanMasterAsset
 * permintaanPengadaanMasterAsset}, {@code MyGrid grid}, {@code boolean edit}, {@code boolean delete}, {@code
 * Textbox barcode}, {@code List permintaanPengadaanMasterAssetDetails}; pembacaan/pencarian ({@code loadData()},
 * {@code loadBarcode()}); operasi domain lain ({@code display()}, {@code appendHargaDppTerbaru()}, {@code
 * terapkanHargaDppTerbaru()}, {@code ikutiHargaDppTerbaruSemuaData()}); konfigurasi constructor: {@code delete},
 * {@code edit}. Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
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
public class PermintaanPengadaanMasterAssetDetailAction extends MyDetail {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5086031585928643232L;

	private PermintaanPengadaanMasterAsset permintaanPengadaanMasterAsset;
	private MyGrid grid;
	private boolean edit = false;
	private boolean delete = false;

	private Textbox barcode;
	private List<PermintaanPengadaanMasterAssetDetail> permintaanPengadaanMasterAssetDetails;

	public PermintaanPengadaanMasterAssetDetailAction(PermintaanPengadaanMasterAsset permintaanPengadaanMasterAsset) {
		super();
		this.permintaanPengadaanMasterAsset = permintaanPengadaanMasterAsset;
		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		this.addEventListener(Events.ON_OPEN, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(PermintaanPengadaanMasterAssetDetailAction.this);
				if (isOpen()) {
					display();
				}
			}
		});
	}

	/**
	 * Renderer lokal untuk layar/komponen {@link PermintaanPengadaanMasterAssetDetailAction}. Kelas ini
	 * menerjemahkan satu item data menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik
	 * kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link PermintaanPengadaanMasterAssetDetailAction}
	 * dan dapat mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see PermintaanPengadaanMasterAssetDetailAction
	 */
	class PermintaanPengadaanMasterAssetDetailRenderer extends ais.ui.util.MyRowRenderer {

		public PermintaanPengadaanMasterAssetDetailRenderer() {

		}

		@Override
		public void render(final Row row, Object data) throws Exception {
			row.setValign("top");
			final PermintaanPengadaanMasterAssetDetail permintaanPengadaanMasterAssetDetail = (PermintaanPengadaanMasterAssetDetail) data;
			MasterAsset masterAsset = permintaanPengadaanMasterAssetDetail.getMasterAsset();
			Hbox hbox = new Hbox();
			hbox.setParent(row);

			LampiranLain.createDownloadUploadFileLain(hbox,
					permintaanPengadaanMasterAssetDetail.getMasterAsset().getId(), LampiranLain.GAMBAR_MASTER_ASSET,
					"Gambar", false, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

						}
					}, null, false, false, false, true, null, false, true);

			final MyDoublebox jumlah = new MyDoublebox(permintaanPengadaanMasterAssetDetail.getJumlah());

			final Label total = new MyLabelKecil(
					Common.numberFormat.get().format(permintaanPengadaanMasterAssetDetail.getJumlah()
							* permintaanPengadaanMasterAssetDetail.getHargaBeli()));
			hbox = new Hbox();
			hbox.setParent(row);
			new Label(permintaanPengadaanMasterAssetDetail.getMasterAsset() == null ? ""
					: permintaanPengadaanMasterAssetDetail.getMasterAsset().getKode()).setParent(hbox);

			PenerimaanPengadaanMasterAsset penerimaanPengadaanMasterAsset = (PenerimaanPengadaanMasterAsset) HibernateUtil
					.currentSession().createCriteria(PenerimaanPengadaanMasterAssetDetail.class)
					.setProjection(Projections.property("penerimaanPengadaanMasterAsset"))
					.add(Restrictions.eq("permintaanPengadaanMasterAsset", permintaanPengadaanMasterAssetDetail))
					.setMaxResults(1).uniqueResult();
			if (penerimaanPengadaanMasterAsset != null) {
				// Beri pemisah " - " antara kode barang (MasterAsset) dan kode BAST (Penerimaan) agar
				// tidak menyatu terbaca, mis. "ATK0216 - 0082/BAST/YTB/FAS/VI/2026" (bukan "ATK02160082/BAST/...").
				String kodeBarang = permintaanPengadaanMasterAssetDetail.getMasterAsset() == null ? ""
						: permintaanPengadaanMasterAssetDetail.getMasterAsset().getKode();
				String pemisah = (kodeBarang != null && !kodeBarang.trim().isEmpty()) ? " - " : "";
				new Label(pemisah + penerimaanPengadaanMasterAsset.getKode()).setParent(hbox);

			}

			Vbox a;
			(a = RevisiHelper.createNewRevisi(PermintaanPengadaanMasterAssetDetail.class,
					permintaanPengadaanMasterAssetDetail,
					permintaanPengadaanMasterAssetDetail.getMasterAsset() == null ? ""
							: permintaanPengadaanMasterAssetDetail.getMasterAsset().getNama()))
					.setParent(row);

			new Label(masterAsset.getMerk()).setParent(a);
			new Label(masterAsset.getJenisAsset() == null ? "" : masterAsset.getJenisAsset().getNama()).setParent(a);
			new Label(masterAsset.getKelompokAsset() == null ? "" : masterAsset.getKelompokAsset().getNama())
					.setParent(a);

			final MyDoublebox hargaBeli = new MyDoublebox(
					permintaanPengadaanMasterAssetDetail.getHargaBeli() == null ? 0.0
							: permintaanPengadaanMasterAssetDetail.getHargaBeli());
			boolean persetujuan = permintaanPengadaanMasterAsset.getDisetujuiOleh() != null;

			if (permintaanPengadaanMasterAssetDetail.getUangMuka() != null
					&& permintaanPengadaanMasterAssetDetail.getUangMuka().getPertangungjawaban() != null
					&& permintaanPengadaanMasterAssetDetail.getUangMuka().getPertangungjawaban()
							.getDisetujuiOleh() != null) {
				persetujuan = true;
			}

			if (persetujuan) {
				new MyLabelKecil(Common.numberFormat.get().format(permintaanPengadaanMasterAssetDetail.getJumlah()))
						.setParent(row);
			} else {
				(jumlah).setParent(row);
			}
			jumlah.setDisabled(
					permintaanPengadaanMasterAssetDetail.getPermintaanPengadaanMasterAsset().getDisetujuiOleh() != null
							|| !edit);
			jumlah.setStyle("text-align:right");
			jumlah.setWidth("90%");
			jumlah.addEventListener(Events.ON_CHANGE, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					permintaanPengadaanMasterAssetDetail.setJumlah(jumlah.getValue());
					Common.refreshUpdate(session, (permintaanPengadaanMasterAssetDetail));

					total.setValue(Common.numberFormat.get().format(permintaanPengadaanMasterAssetDetail.getJumlah()
							* permintaanPengadaanMasterAssetDetail.getHargaBeli()));
				}
			});
			if (permintaanPengadaanMasterAssetDetail != null
					&& !permintaanPengadaanMasterAssetDetail.getMasterAsset().getHargaBolehDiubah()) {
				new MyLabelKecil(Common.numberFormat.get().format(permintaanPengadaanMasterAssetDetail.getHargaBeli()))
						.setParent(row);
			} else if (persetujuan) {
				new MyLabelKecil(Common.numberFormat.get().format(permintaanPengadaanMasterAssetDetail.getHargaBeli()))
						.setParent(row);
			} else {
				(hargaBeli).setParent(row);
			}
			hargaBeli.setDisabled(
					permintaanPengadaanMasterAssetDetail.getPermintaanPengadaanMasterAsset().getDisetujuiOleh() != null
							|| !edit);
			hargaBeli.setStyle("text-align:right");
			hargaBeli.setWidth("90%");
			hargaBeli.addEventListener(Events.ON_CHANGE, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					permintaanPengadaanMasterAssetDetail.setHargaBeli(hargaBeli.getValue());
					Common.refreshUpdate(session, (permintaanPengadaanMasterAssetDetail));

					MasterAsset masterAsset = permintaanPengadaanMasterAssetDetail.getMasterAsset();
					session.refresh(masterAsset);
					masterAsset.setHargaBeliDefault(hargaBeli.getValue());
					Common.refreshUpdate(session, masterAsset);

					total.setValue(Common.numberFormat.get().format(permintaanPengadaanMasterAssetDetail.getJumlah()
							* permintaanPengadaanMasterAssetDetail.getHargaBeli()));
				}
			});

			appendHargaDppTerbaru(row, permintaanPengadaanMasterAssetDetail, hargaBeli, total,
					permintaanPengadaanMasterAssetDetail.getPermintaanPengadaanMasterAsset().getDisetujuiOleh() != null
							|| !edit || !permintaanPengadaanMasterAssetDetail.getMasterAsset().getHargaBolehDiubah());

			total.setStyle("text-align:right");
			total.setParent(row);

			final MyTextbox keterangan = new MyTextbox(permintaanPengadaanMasterAssetDetail.getKeterangan() == null ? ""
					: permintaanPengadaanMasterAssetDetail.getKeterangan());
			keterangan.setWidth("90%");
			keterangan.setHeight("95%");
			if (persetujuan) {
				new MyLabelKecil(permintaanPengadaanMasterAssetDetail.getKeterangan()).setParent(row);
			} else {
				keterangan.setParent(row);
			}
			keterangan.setDisabled(
					permintaanPengadaanMasterAssetDetail.getPermintaanPengadaanMasterAsset().getDisetujuiOleh() != null
							|| !edit);
			keterangan.addEventListener(Events.ON_CHANGE, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					permintaanPengadaanMasterAssetDetail.setKeterangan(keterangan.getValue());
					Common.refreshUpdate(session, (permintaanPengadaanMasterAssetDetail));
				}
			});

			// Kolom aksi rapi (pola MahasiswaAction): semua tombol dibungkus kebab popup (⋯)
			// via UIHelper.buatBarisAksi — kolom aksi jadi kecil dan konsisten antar layar.
			final java.util.List<org.zkoss.zk.ui.Component> aksiButtons =
					new java.util.ArrayList<org.zkoss.zk.ui.Component>();

			if (permintaanPengadaanMasterAssetDetail.getUangMuka() != null
					&& permintaanPengadaanMasterAssetDetail.getUangMuka().getPertangungjawaban() != null
					&& permintaanPengadaanMasterAssetDetail.getUangMuka().getPertangungjawaban()
							.getDisetujuiOleh() != null) {

				final MyToolbarbuttonConfig asset = new MyToolbarbuttonConfig("Jadikan Inventaris",
						"/img/svg/edit-box-line.svg");
				final MyToolbarbuttonConfig hapusAsset = new MyToolbarbuttonConfig("Hapus Inventaris",
						"/img/svg/edit-box-line.svg");

				asset.setOrient("vertical");
				asset.setDisabled(!edit);
				asset.setVisible(
						permintaanPengadaanMasterAssetDetail.getAsset() == null && permintaanPengadaanMasterAssetDetail
								.getMasterAsset().getTipe().equalsIgnoreCase(MasterAsset.TIPE_TIDAK_HABIS_PAKAI));

				asset.setTooltiptext("Jadikan Inventaris/Sarpras");
				asset.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						MyMessageboxConfig.show(
								"Apakah yakin ingin menjadikan pengadaan ini menjadi barang inventaris/Sarpras ?",
								"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
								MyMessageboxConfig.QUESTION, new EventListener() {

									@Override
									public void onEvent(Event event) throws Exception {
										int i = Integer.parseInt(event.getData().toString());
										if (i == MyMessageboxConfig.OK) {
											SaldoAwalMasterAssetDetailAction.pindahkanMenjadiBarangInventaris(
													permintaanPengadaanMasterAssetDetail);
											asset.setVisible(false);
											hapusAsset.setVisible(true);
										}

									}
								});

					}

				});
				aksiButtons.add(asset);

				hapusAsset.setOrient("vertical");
				hapusAsset.setDisabled(permintaanPengadaanMasterAssetDetail.getPermintaanPengadaanMasterAsset()
						.getDisetujuiOleh() == null || !edit);
				hapusAsset.setVisible(permintaanPengadaanMasterAssetDetail.getAsset() != null);
				hapusAsset.setTooltiptext("Hapus Inventaris/Sarpras");
				hapusAsset.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						MyMessageboxConfig.show(
								"Apakah yakin ingin menhapus barang inventaris/Sarpras dari pengadaan ini ?",
								"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
								MyMessageboxConfig.QUESTION, new EventListener() {

									@Override
									public void onEvent(Event event) throws Exception {
										int i = Integer.parseInt(event.getData().toString());
										if (i == MyMessageboxConfig.OK) {
											Asset myAsset = permintaanPengadaanMasterAssetDetail.getAsset();
											Session session = HibernateUtil.currentSession();
											session.createSQLQuery(
													"delete from asset.asset where id = " + myAsset.getId())
													.executeUpdate();
											hapusAsset.setVisible(false);
											asset.setVisible(true);
										}

									}
								});

					}

				});
				aksiButtons.add(hapusAsset);

			}

			if (!persetujuan) {
				MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
				button.setDisabled(permintaanPengadaanMasterAssetDetail.getPermintaanPengadaanMasterAsset()
						.getDisetujuiOleh() != null || !delete);
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

												Common.refreshDelete(permintaanPengadaanMasterAssetDetail);

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
				aksiButtons.add(button);
			}

			ais.ui.util.UIHelper.buatBarisAksi(row, 3, aksiButtons);

		}
	}

	@SuppressWarnings("unchecked")
	public void loadData(Object value) {
		Session session = HibernateUtil.currentSession();
		permintaanPengadaanMasterAssetDetails = session
				.createCriteria(PermintaanPengadaanMasterAssetDetail.class).addOrder(Order.desc("id"))
				.add(Restrictions.eq("permintaanPengadaanMasterAsset", permintaanPengadaanMasterAsset)).list();

		ListModel strset = new SimpleListModel(permintaanPengadaanMasterAssetDetails);
		grid.setRowRenderer(new PermintaanPengadaanMasterAssetDetailRenderer());
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
		button.setDisabled(permintaanPengadaanMasterAsset.getDisetujuiOleh() != null);
		button.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {
				Session session = HibernateUtil.currentSession();

				List<MasterAsset> masterAssets = session.createCriteria(PermintaanPengadaanMasterAssetDetail.class)
						.setProjection(Projections.groupProperty("masterAsset"))
						.add(Restrictions.eq("permintaanPengadaanMasterAsset", permintaanPengadaanMasterAsset)).list();

				AmbilDataMasterAssetBanyak ambilDataMasterAssetBanyak = new AmbilDataMasterAssetBanyak(masterAssets,
						null);
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(ambilDataMasterAssetBanyak);
				ambilDataMasterAssetBanyak.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						List<MasterAsset> masterAssets = (List<MasterAsset>) arg0.getData();
						Session session = HibernateUtil.currentSession();
						for (MasterAsset masterAsset : masterAssets) {
							PermintaanPengadaanMasterAssetDetail permintaanPengadaanMasterAssetDetail = new PermintaanPengadaanMasterAssetDetail();
							permintaanPengadaanMasterAssetDetail.setMasterAsset(masterAsset);
							permintaanPengadaanMasterAssetDetail.setJumlah(0.0);
							permintaanPengadaanMasterAssetDetail.setKeterangan("");
							permintaanPengadaanMasterAssetDetail
									.setPermintaanPengadaanMasterAsset(permintaanPengadaanMasterAsset);
							session.save(permintaanPengadaanMasterAssetDetail);
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

		new Label("Tambah Kode/Nama").setParent(toolbar);
		new Space().setParent(toolbar);
		barcode = new Textbox();
		barcode.setDisabled(permintaanPengadaanMasterAsset.getDisetujuiOleh() != null);
		barcode.setParent(toolbar);
		barcode.addEventListener("onOK", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadBarcode();
			}
		});

		MyToolbarbuttonConfig cari = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		cari.setParent(toolbar);
		cari.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});

		button = new MyToolbarbuttonConfig("History", "/img/jadwal.png");
		button.setVisible(permintaanPengadaanMasterAsset.getId() != null);
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				RevisiPermintaanPengadaanMasterAssetDetailHelper revisiHelper = new RevisiPermintaanPengadaanMasterAssetDetailHelper(
						permintaanPengadaanMasterAsset, new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								loadData(null);
							}
						});
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(revisiHelper);
				revisiHelper.setVisible(true);
				revisiHelper.onModal();

			}

		});
		button.setParent(toolbar);

		MyToolbarbuttonConfig ikutiHargaDpp = new MyToolbarbuttonConfig("Ikuti Harga DPP Terbaru", "/img/svg/refresh.svg");
		ikutiHargaDpp.setDisabled(permintaanPengadaanMasterAsset.getDisetujuiOleh() != null);
		ikutiHargaDpp.setTooltiptext("Mengisi harga barang/jasa dari riwayat DPP terbaru Tagihan Vendor, PO, dan BAST.");
		ikutiHargaDpp.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				ikutiHargaDppTerbaruSemuaData();
			}
		});
		ikutiHargaDpp.setParent(toolbar);

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(50);
		grid.setParent(groupbox);

		Columns columns = new Columns();

		columns.setParent(grid);
		columns.setSizable(true);
		columns.setStyle("background:#f8fafc; border-bottom:1px solid #e5e7eb; font-weight:bold;");

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("70px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Kode");
		column.setWidth("8%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Qty");
		// Diperlebar (5% -> 40%) agar kotak input jumlah/QTY jelas terlihat saat pengajuan SOP.
		column.setWidth("40%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Harga");
		column.setWidth("8%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("DPP Terbaru");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Total");
		column.setAlign("right");
		column.setWidth("6%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Keterangan");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("6%");

		loadData(null);
	}

	public void loadBarcode() throws Exception {
		String barcode = this.barcode.getText().trim();
		if (barcode.trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, field Kode/Nama belum diisi. Langkah yang dapat dilakukan: (1) Scan barcode barang atau ketik kode/nama aset yang dicari; (2) Pastikan kode atau nama sesuai dengan data di master aset; (3) ulangi proses pencarian. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
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

		PermintaanPengadaanMasterAssetDetail permintaanPengadaanMasterAssetDetail = new PermintaanPengadaanMasterAssetDetail();
		permintaanPengadaanMasterAssetDetail.setMasterAsset(masterAsset);
		permintaanPengadaanMasterAssetDetail.setJumlah(1.0);
		permintaanPengadaanMasterAssetDetail.setKeterangan("");
		permintaanPengadaanMasterAssetDetail.setPermintaanPengadaanMasterAsset(permintaanPengadaanMasterAsset);
		session.save(permintaanPengadaanMasterAssetDetail);
		loadData(null);
		this.barcode.focus();
		this.barcode.select();
	}



	private void appendHargaDppTerbaru(final Row row, final PermintaanPengadaanMasterAssetDetail detail,
			final MyDoublebox hargaBeli, final Label total, boolean disabled) throws Exception {
		Vbox box = new Vbox();
		box.setWidth("100%");
		box.setParent(row);

		final AssetHargaDppHistoryUtil.HargaDppInfo info = AssetHargaDppHistoryUtil.ambilHargaDppTerbaru(
				HibernateUtil.currentSession(), detail == null ? null : detail.getMasterAsset(), null);

		if (info == null || !info.hasHarga()) {
			Label kosong = new MyLabelKecil("Belum ada riwayat DPP");
			kosong.setStyle("color:#94a3b8;font-size:10px;white-space:normal;");
			kosong.setParent(box);
			return;
		}

		Label utama = new MyLabelKecil("Rp " + AssetHargaDppHistoryUtil.formatMoney(info.getHargaDppSatuan()));
		utama.setStyle("font-weight:bold;color:#0f766e;font-size:11px;white-space:normal;");
		utama.setTooltiptext("Riwayat harga DPP global barang/jasa ini. Informasi ini bukan status PO/BAST untuk PR yang sedang dibuka.");
		utama.setParent(box);

		Label kecil = new MyLabelKecil("Riwayat harga global - "
				+ AssetHargaDppHistoryUtil.formatDate(info.getTanggal()));
		kecil.setStyle("color:#64748b;font-size:9px;white-space:normal;");
		kecil.setParent(box);

		MyToolbarbuttonConfig ikuti = new MyToolbarbuttonConfig("Ikuti", "/img/svg/refresh.svg");
		ikuti.setTooltiptext("Mengisi kolom Harga memakai DPP/satuan terbaru.");
		ikuti.setDisabled(disabled);
		ikuti.setParent(box);
		ikuti.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				terapkanHargaDppTerbaru(detail, info, hargaBeli, total);
			}
		});
	}

	private void terapkanHargaDppTerbaru(final PermintaanPengadaanMasterAssetDetail detail,
			final AssetHargaDppHistoryUtil.HargaDppInfo info, final MyDoublebox hargaBeli, final Label total)
			throws Exception {
		if (detail == null || info == null || !info.hasHarga()) {
			MyMessageboxConfig.show("Mohon maaf, belum ada riwayat harga DPP yang dapat digunakan untuk barang ini. Langkah yang dapat dilakukan: (1) Pastikan barang/aset sudah pernah memiliki transaksi pengadaan sebelumnya; (2) Input harga DPP secara manual pada field Harga Beli; (3) Riwayat DPP akan tersedia setelah transaksi pengadaan pertama selesai. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Informasi", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return;
		}
		Session session = HibernateUtil.currentSession();
		detail.setHargaBeli(info.getHargaDppSatuan());
		if (detail.getId() != null) {
			Common.refreshUpdate(session, detail);
		}
		try {
			MasterAsset masterAsset = detail.getMasterAsset();
			if (masterAsset != null && masterAsset.getId() != null) {
				session.refresh(masterAsset);
				masterAsset.setHargaBeliDefault(info.getHargaDppSatuan());
				Common.refreshUpdate(session, masterAsset);
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		if (hargaBeli != null) {
			hargaBeli.setValue(info.getHargaDppSatuan());
		}
		if (total != null) {
			total.setValue(Common.numberFormat.get().format(detail.getJumlah() * detail.getHargaBeli()));
		}
	}

	private void ikutiHargaDppTerbaruSemuaData() throws Exception {
		if (permintaanPengadaanMasterAssetDetails == null || permintaanPengadaanMasterAssetDetails.isEmpty()) {
			return;
		}
		Session session = HibernateUtil.currentSession();
		int berhasil = 0;
		for (PermintaanPengadaanMasterAssetDetail detail : permintaanPengadaanMasterAssetDetails) {
			if (detail == null || detail.getMasterAsset() == null || !detail.getMasterAsset().getHargaBolehDiubah()) {
				continue;
			}
			AssetHargaDppHistoryUtil.HargaDppInfo info = AssetHargaDppHistoryUtil.ambilHargaDppTerbaru(session,
					detail.getMasterAsset(), null);
			if (info != null && info.hasHarga()) {
				detail.setHargaBeli(info.getHargaDppSatuan());
				if (detail.getId() != null) {
					Common.refreshUpdate(session, detail);
				}
				try {
					MasterAsset masterAsset = detail.getMasterAsset();
					if (masterAsset != null && masterAsset.getId() != null) {
						session.refresh(masterAsset);
						masterAsset.setHargaBeliDefault(info.getHargaDppSatuan());
						Common.refreshUpdate(session, masterAsset);
					}
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
				berhasil++;
			}
		}
		loadData(null);
		MyMessageboxConfig.show("Harga DPP terbaru diterapkan pada " + berhasil + " baris.", "Informasi",
				MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
	}

}
