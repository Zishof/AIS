package ais.action.master.asset.helper;
/* ENHANCED_PENGGUNAAN_ANGGARAN_MEMORY_SAFE_2026_06_03 - Java 1.6/1.7 compatible. */

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
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
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Box;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Foot;
import org.zkoss.zul.Footer;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Row;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.akunting.JenisUangMukaAction;
import ais.action.master.asset.util.AssetUtil;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.rab.helper.AmbilDataWorkspaceBanbox;
import ais.common.BarcodeCommon;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Konfigurasi;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.akunting.Pajak;
import ais.database.model.asset.Asset;
import ais.database.model.asset.AssetDetail;
import ais.database.model.asset.JenisPajakBarang;
import ais.database.model.asset.JenisPajakPpn;
import ais.database.model.asset.MasterAsset;
import ais.database.model.asset.PemesananPengadaanMasterAsset;
import ais.database.model.asset.PenerimaanPengadaanMasterAsset;
import ais.database.model.asset.PenerimaanPengadaanMasterAssetDetail;
import ais.database.model.asset.PermintaanPengadaanMasterAsset;
import ais.database.model.asset.PermintaanPengadaanMasterAssetDetail;
import ais.database.model.asset.SaldoAwalMasterAsset;
import ais.database.model.asset.SaldoAwalMasterAssetDetail;
import ais.database.model.file.LampiranLain;
import ais.database.model.rab.Workspace;
import ais.ui.util.DataCriteria;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;

public class SaldoAwalMasterAssetDetailAction extends MyDetail implements DataCriteria {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5086031585928643232L;

	private SaldoAwalMasterAsset saldoAwalMasterAsset;
	private MyGrid grid;
	private boolean edit = false;
	private boolean delete = false;

	private String[] contents = new String[] { "id", "masterAsset", "jumlah" };

	private Textbox nama;

	private boolean terimaTagihan = false;

	private Footer footerTotalSemua;

	public SaldoAwalMasterAssetDetailAction(SaldoAwalMasterAsset saldoAwalMasterAsset, boolean terimaTagihan) {
		super();
		this.saldoAwalMasterAsset = saldoAwalMasterAsset;
		this.terimaTagihan = terimaTagihan;
		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		this.addEventListener(Events.ON_OPEN, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(SaldoAwalMasterAssetDetailAction.this);
				if (isOpen()) {
					display();
				}
			}
		});
	}


	private static boolean isEmpty(String value) {
		return value == null || value.trim().length() == 0;
	}

	private static double doubleValue(Double value) {
		return value == null ? 0.0 : value.doubleValue();
	}

	private static String safeText(String value) {
		return value == null ? "" : value.trim();
	}

	private static String formatNumber(Double value) {
		return Common.numberFormat.get().format(doubleValue(value));
	}

	private static boolean isReadonly(SaldoAwalMasterAssetDetail detail, boolean edit) {
		return detail == null || detail.getSaldoAwal() == null || detail.getSaldoAwal().getDisetujuiOleh() != null || !edit;
	}

	/**
	 * Nilai PPH (nominal) yang aman dari NULL untuk ditampilkan di kolom "Nilai PPH".
	 * Memakai SaldoAwalMasterAssetDetail.hitungPph() (DPP - potongan, dikali %PPH) yang
	 * memang didesain sebagai nilai tampil; dibungkus agar baris baru yang qty/harga-nya
	 * belum terisi tidak memicu error.
	 */
	private static Double nilaiPphAman(SaldoAwalMasterAssetDetail detail) {
		if (detail == null) {
			return Double.valueOf(0.0);
		}
		try {
			Double v = detail.hitungPph();
			return v == null ? Double.valueOf(0.0) : v;
		} catch (Exception e) {
			return Double.valueOf(0.0);
		}
	}

	private void reloadNilaiTimer() {
		Common.createDefaultTimer(new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				reloadNilai();
			}
		});
	}

	class SaldoAwalMasterAssetDetailRenderer extends ais.ui.util.MyRowRenderer {

		public SaldoAwalMasterAssetDetailRenderer() {

		}

		@Override
		public void render(final Row rowD, Object data) throws Exception {
			final SaldoAwalMasterAssetDetail saldoAwalMasterAssetDetail = (SaldoAwalMasterAssetDetail) data;
			MasterAsset masterAsset = saldoAwalMasterAssetDetail.getMasterAsset();
			final MyDoublebox jumlah = new MyDoublebox(
					saldoAwalMasterAssetDetail.getJumlah() == null ? 0.0 : saldoAwalMasterAssetDetail.getJumlah());
			final MyDoublebox harga = new MyDoublebox(saldoAwalMasterAssetDetail.getHarga());

			boolean persetujuan = isReadonly(saldoAwalMasterAssetDetail, edit)
					|| (saldoAwalMasterAssetDetail.getDataPerMasterAsset() != null
							&& saldoAwalMasterAssetDetail.getDataPerMasterAsset());

			Vbox a;
			(a = RevisiHelper.createNewRevisi(SaldoAwalMasterAssetDetail.class, saldoAwalMasterAssetDetail,
					saldoAwalMasterAssetDetail.getMasterAsset() == null ? ""
							: saldoAwalMasterAssetDetail.getMasterAsset().getNama()))
					.setParent(rowD);

			PenerimaanPengadaanMasterAssetDetail penerimaanPengadaanMasterAssetDetail = saldoAwalMasterAssetDetail
					.getPenerimaanPengadaanMasterAssetDetail();


			if (penerimaanPengadaanMasterAssetDetail != null) {

				RevisiHelper
						.createNewRevisi(PenerimaanPengadaanMasterAsset.class,
								penerimaanPengadaanMasterAssetDetail.getPenerimaanPengadaanMasterAsset(),
								penerimaanPengadaanMasterAssetDetail.getPenerimaanPengadaanMasterAsset().getKode())
						.setParent(a);

				if (penerimaanPengadaanMasterAssetDetail.getPemesananPengadaanMasterAssetDetail() != null) {
					RevisiHelper.createNewRevisi(PemesananPengadaanMasterAsset.class,
							penerimaanPengadaanMasterAssetDetail.getPemesananPengadaanMasterAssetDetail()
									.getPemesananPengadaanMasterAsset(),
							penerimaanPengadaanMasterAssetDetail.getPemesananPengadaanMasterAssetDetail()
									.getPemesananPengadaanMasterAsset().getKode())
							.setParent(a);
				}

				if (penerimaanPengadaanMasterAssetDetail.getPemesananPengadaanMasterAssetDetail() != null
						&& penerimaanPengadaanMasterAssetDetail.getPemesananPengadaanMasterAssetDetail()
								.getPermintaanPengadaanMasterAssetDetail() != null) {
					RevisiHelper.createNewRevisi(PermintaanPengadaanMasterAsset.class,
							penerimaanPengadaanMasterAssetDetail.getPemesananPengadaanMasterAssetDetail()
									.getPermintaanPengadaanMasterAssetDetail().getPermintaanPengadaanMasterAsset(),
							penerimaanPengadaanMasterAssetDetail.getPemesananPengadaanMasterAssetDetail()
									.getPermintaanPengadaanMasterAssetDetail().getPermintaanPengadaanMasterAsset()
									.getKode())
							.setParent(a);
				}
			}

			final MyCheckboxConfig diskonDalamBentukPersen = new MyCheckboxConfig("Persen");
			diskonDalamBentukPersen.setChecked(saldoAwalMasterAssetDetail.getDiskonDalamBentukPersen());

			final MyDoublebox hargaPotongan = new MyDoublebox(saldoAwalMasterAssetDetail.getHargaPotongan());

			final Combobox persenPpn = new Combobox();
			Common.insertComboDanSemua(persenPpn, new String[] { "nama" }, "keterangan", JenisPajakPpn.class,
					"Tanpa PPN", Restrictions.eq("aktif", true));
			Common.selectComboItem(persenPpn, saldoAwalMasterAssetDetail.getJenisPajakPpn());
			Double dpp = Double.valueOf(doubleValue(saldoAwalMasterAssetDetail.getJumlah()) * doubleValue(saldoAwalMasterAssetDetail.getHarga()));
			Double ppn = Double.valueOf((doubleValue(saldoAwalMasterAssetDetail.getPersenPpn()) / 100.0) * doubleValue(dpp));
			final Label ppnNilai = new MyLabelKecil(Common.numberFormat.get().format(ppn));
			rowD.setAttribute("ppnNilai", ppnNilai);

			// Nilai PPH (nominal) — analog dengan "Nilai PPN"; nilai dari hitungPph() model.
			final Label pphNilai = new MyLabelKecil(formatNumber(nilaiPphAman(saldoAwalMasterAssetDetail)));
			pphNilai.setStyle("text-align:right");
			rowD.setAttribute("pphNilai", pphNilai);

			final Combobox persenPph = new Combobox();
			Common.insertComboDanSemua(persenPph, new String[] { "nama", "persen" }, "keterangan",
					JenisPajakBarang.class, "Tanpa Pajak", Restrictions.eq("aktif", true));
			Common.selectComboItem(persenPph, saldoAwalMasterAssetDetail.getJenisPajakBarang());

			if (saldoAwalMasterAssetDetail.getId() != null
					&& saldoAwalMasterAssetDetail.getJenisPajakBarang() != null) {
				Pajak.buat(null, null, null, saldoAwalMasterAssetDetail);
			}

			final Label total = new MyLabelKecil(
					formatNumber(saldoAwalMasterAssetDetail.getHargaTotal()));

			new Label(masterAsset == null ? "" : safeText(masterAsset.getMerk())).setParent(a);
			new Label(masterAsset == null || masterAsset.getJenisAsset() == null ? "" : safeText(masterAsset.getJenisAsset().getNama())).setParent(a);

			if (saldoAwalMasterAsset.getPenerimaanPengadaanMasterAsset() == null) {

				final AmbilDataWorkspaceBanbox workspace = new AmbilDataWorkspaceBanbox(false);
				workspace.setAttribute("workspace", saldoAwalMasterAssetDetail.getWorkspace());
				workspace.setValue(saldoAwalMasterAssetDetail.getWorkspace() == null ? ""
						: saldoAwalMasterAssetDetail.getWorkspace().getNama());
				workspace.setParent(rowD);
				workspace.setWidth("90%");

				workspace.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Workspace work = (Workspace) workspace.getAttribute("workspace");

						Double saldo = JenisUangMukaAction.hitungSaldo(null, null, null,
								saldoAwalMasterAssetDetail.getId(), work,
								(saldoAwalMasterAssetDetail.getSaldoAwal() == null ? ais.ui.util.WaktuUtil.getDate() : saldoAwalMasterAssetDetail.getSaldoAwal().getTanggalPembuatan()));

						if (Common.bolehKonfigurasi("saldo_harus_cukup_sebelum_mengajukan_realisasi_anggaran")) {

							if (saldoAwalMasterAssetDetail.getHargaTotal().doubleValue() > saldo.doubleValue()) {

								workspace.setAttribute("workspace", null);
								workspace.setValue("");

								MyMessageboxConfig.show("Saldo anggaran tidak mencukupi. Nilai tagihan melebihi sisa saldo", "Peringatan",
										MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
								return;
							}
						}

						saldoAwalMasterAssetDetail.setWorkspace(work);
						Session session = HibernateUtil.currentSession();
						Common.refreshUpdate(session, (saldoAwalMasterAssetDetail));

					}
				});
			}

			if ((saldoAwalMasterAsset != null && saldoAwalMasterAsset.getJsonTermin() != null)
					|| isReadonly(saldoAwalMasterAssetDetail, edit)) {
				new MyLabelKecil(Common.numberFormat.get().format(saldoAwalMasterAssetDetail.getJumlah()))
						.setParent(rowD);
				new MyLabelKecil(Common.numberFormat.get().format(saldoAwalMasterAssetDetail.getHarga()))
						.setParent(rowD);
			} else {

				(jumlah).setParent(rowD);
				jumlah.setDisabled(persetujuan);
				jumlah.setStyle("text-align:right");
				jumlah.setWidth("90%");
				jumlah.addEventListener(Events.ON_CHANGE, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						Double saldo = Math.abs(jumlah.getValue() == null ? 0.0 : jumlah.getValue());
						jumlah.setValue(saldo);
						Session session = HibernateUtil.currentSession();
						saldoAwalMasterAssetDetail.setJumlah(saldo);
						Common.refreshUpdate(session, (saldoAwalMasterAssetDetail));

						total.setValue(formatNumber(saldoAwalMasterAssetDetail.getHargaTotal()));

						Double dpp = Double.valueOf(doubleValue(saldoAwalMasterAssetDetail.getJumlah()) * doubleValue(saldoAwalMasterAssetDetail.getHarga()));
						Double ppn = Double.valueOf((doubleValue(saldoAwalMasterAssetDetail.getPersenPpn()) / 100.0) * doubleValue(dpp));
						ppnNilai.setValue(Common.numberFormat.get().format(ppn));
						pphNilai.setValue(formatNumber(nilaiPphAman(saldoAwalMasterAssetDetail)));

						reloadNilaiTimer();
					}
				});

				(harga).setParent(rowD);
				harga.setDisabled(persetujuan);
				harga.setStyle("text-align:right");
				harga.setWidth("90%");
				harga.addEventListener(Events.ON_CHANGE, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						Double saldo = Math.abs(harga.getValue() == null ? 0.0 : harga.getValue());
						harga.setValue(saldo);
						Session session = HibernateUtil.currentSession();
						saldoAwalMasterAssetDetail.setHarga(saldo);
						Common.refreshUpdate(session, (saldoAwalMasterAssetDetail));

						total.setValue(formatNumber(saldoAwalMasterAssetDetail.getHargaTotal()));

						Double dpp = Double.valueOf(doubleValue(saldoAwalMasterAssetDetail.getJumlah()) * doubleValue(saldoAwalMasterAssetDetail.getHarga()));
						Double ppn = Double.valueOf((doubleValue(saldoAwalMasterAssetDetail.getPersenPpn()) / 100.0) * doubleValue(dpp));
						ppnNilai.setValue(Common.numberFormat.get().format(ppn));
						pphNilai.setValue(formatNumber(nilaiPphAman(saldoAwalMasterAssetDetail)));

						reloadNilaiTimer();
					}
				});
			}

			if (persetujuan) {
				new Label(saldoAwalMasterAssetDetail.getDiskonDalamBentukPersen() ? "Ya" : "Tidak").setParent(rowD);
			} else {
				(diskonDalamBentukPersen).setParent(rowD);
			}

			diskonDalamBentukPersen.setDisabled(persetujuan);
			diskonDalamBentukPersen.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					saldoAwalMasterAssetDetail.setDiskonDalamBentukPersen(diskonDalamBentukPersen.isChecked());
					Common.refreshUpdate(session, (saldoAwalMasterAssetDetail));

					total.setValue(formatNumber(saldoAwalMasterAssetDetail.getHargaTotal()));
					pphNilai.setValue(formatNumber(nilaiPphAman(saldoAwalMasterAssetDetail)));

					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							reloadNilai();
						}
					});
				}
			});

			if (persetujuan) {
				new MyLabelKecil(Common.numberFormat.get().format(saldoAwalMasterAssetDetail.getHargaPotongan()))
						.setParent(rowD);
			} else {
				(hargaPotongan).setParent(rowD);
			}

			hargaPotongan.setDisabled(persetujuan);
			hargaPotongan.setStyle("text-align:right");
			hargaPotongan.setWidth("90%");
			hargaPotongan.addEventListener(Events.ON_CHANGE, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					saldoAwalMasterAssetDetail.setHargaPotongan(hargaPotongan.getValue());
					Common.refreshUpdate(session, (saldoAwalMasterAssetDetail));

					total.setValue(formatNumber(saldoAwalMasterAssetDetail.getHargaTotal()));
					pphNilai.setValue(formatNumber(nilaiPphAman(saldoAwalMasterAssetDetail)));

					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							reloadNilai();
						}
					});
				}
			});

			if (persetujuan) {
				new Label(saldoAwalMasterAssetDetail.getJenisPajakPpn() == null ? ""
						: saldoAwalMasterAssetDetail.getJenisPajakPpn().getNama()).setParent(rowD);
			} else {
				(persenPpn).setParent(rowD);
			}

			persenPpn.setDisabled(persetujuan);
			persenPpn.setStyle("text-align:right");
			persenPpn.setWidth("90%");
			persenPpn.addEventListener(Events.ON_CHANGE, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					saldoAwalMasterAssetDetail
							.setJenisPajakPpn((JenisPajakPpn) (persenPpn.getSelectedItem() == null ? null
									: persenPpn.getSelectedItem().getValue()));

					if (saldoAwalMasterAssetDetail.getId() != null) {
						Common.refreshUpdate(session, (saldoAwalMasterAssetDetail));
					}

					Double dpp = Double.valueOf(doubleValue(saldoAwalMasterAssetDetail.getJumlah()) * doubleValue(saldoAwalMasterAssetDetail.getHarga()));
					Double ppn = Double.valueOf((doubleValue(saldoAwalMasterAssetDetail.getPersenPpn()) / 100.0) * doubleValue(dpp));
					ppnNilai.setValue(Common.numberFormat.get().format(ppn));

					total.setValue(formatNumber(saldoAwalMasterAssetDetail.getHargaTotal()));

					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							reloadNilai();
						}
					});
				}
			});

			ppnNilai.setStyle("text-align:right");
			ppnNilai.setParent(rowD);

			if (persetujuan) {
				new Label(saldoAwalMasterAssetDetail.getJenisPajakBarang() == null ? ""
						: saldoAwalMasterAssetDetail.getJenisPajakBarang().getNama()).setParent(rowD);
			} else {
				(persenPph).setParent(rowD);
			}

			persenPph.setDisabled(persetujuan);
			persenPph.setStyle("text-align:right");
			persenPph.setWidth("90%");
			persenPph.addEventListener(Events.ON_CHANGE, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					saldoAwalMasterAssetDetail
							.setJenisPajakBarang((JenisPajakBarang) (persenPph.getSelectedItem() == null ? null
									: persenPph.getSelectedItem().getValue()));
					Common.refreshUpdate(session, (saldoAwalMasterAssetDetail));

					total.setValue(formatNumber(saldoAwalMasterAssetDetail.getHargaTotal()));
					pphNilai.setValue(formatNumber(nilaiPphAman(saldoAwalMasterAssetDetail)));

					if (saldoAwalMasterAssetDetail.getId() != null
							&& saldoAwalMasterAssetDetail.getJenisPajakBarang() != null) {
						Pajak.buat(null, null, null, saldoAwalMasterAssetDetail);
					}

					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							reloadNilai();
						}
					});
				}
			});

			pphNilai.setParent(rowD);

			total.setStyle("text-align:right");
			total.setParent(rowD);

			final MyTextbox keterangan = new MyTextbox(saldoAwalMasterAssetDetail.getKeterangan() == null ? ""
					: saldoAwalMasterAssetDetail.getKeterangan());
			keterangan.setWidth("90%");
			keterangan.setHeight("95%");
			if (persetujuan) {
				new MyLabelKecil(saldoAwalMasterAssetDetail.getKeterangan()).setParent(rowD);
			} else {
				keterangan.setParent(rowD);
			}
			keterangan.setDisabled(isReadonly(saldoAwalMasterAssetDetail, edit));
			keterangan.addEventListener(Events.ON_CHANGE, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					saldoAwalMasterAssetDetail.setKeterangan(keterangan.getValue());
					Common.refreshUpdate(session, (saldoAwalMasterAssetDetail));
				}
			});

			Hbox toolbar = new Hbox();

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Hapus", "/img/svg/trash.svg");
			button.setOrient("vertical");
			button.setDisabled(saldoAwalMasterAssetDetail.getSaldoAwal() == null || saldoAwalMasterAssetDetail.getSaldoAwal().getDisetujuiOleh() != null || !delete);
			button.setVisible(
					!terimaTagihan && saldoAwalMasterAssetDetail.getPenerimaanPengadaanMasterAssetDetail() == null);
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
													saldoAwalMasterAssetDetail);

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
			toolbar.setParent(rowD);

		}
	}

	@SuppressWarnings("unchecked")
	public void loadData(Object value) {

		List<SaldoAwalMasterAssetDetail> saldoAwalMasterAssetDetails = initCriteria(true).list();

		ListModel strset = new SimpleListModel(saldoAwalMasterAssetDetails);
		grid.setRowRenderer(new SaldoAwalMasterAssetDetailRenderer());
		grid.setModelCheckMobile(strset);

	}

	public void display() {

		ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
		groupbox.setStyle("min-height:260px; width:100%; overflow:auto; padding:6px;");
		groupbox.setParent(this);
		groupbox.appendChild(new MyCaptionStyled("Daftar Barang/Jasa"));

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("44px");
		toolbar.setStyle("border:0; background:#f8fafc; padding:6px; border-radius:8px;");
		toolbar.setParent(groupbox);

		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Cari : ")));
		toolbar.appendChild(nama = new Textbox());
		nama.setCols(24);
		nama.setWidth("220px");
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

		button = new MyToolbarbuttonConfig("Ambil Data Barang", "/img/add_item.png");
		button.setVisible(saldoAwalMasterAsset.getDisetujuiOleh() == null
				&& saldoAwalMasterAsset.getPenerimaanPengadaanMasterAsset() == null);
		button.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {
				Session session = HibernateUtil.currentSession();

				List<MasterAsset> masterAssets = session.createCriteria(SaldoAwalMasterAssetDetail.class)
						.setProjection(Projections.groupProperty("masterAsset"))
						.add(Restrictions.eq("saldoAwal", saldoAwalMasterAsset)).list();

				AmbilDataMasterAssetBanyak ambilDataMasterAssetBanyak = new AmbilDataMasterAssetBanyak(masterAssets,
						null);
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(ambilDataMasterAssetBanyak);
				ambilDataMasterAssetBanyak.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						List<MasterAsset> masterAssets = (List<MasterAsset>) arg0.getData();

						for (MasterAsset masterAsset : masterAssets) {
							SaldoAwalMasterAssetDetail saldoAwalMasterAssetDetail = new SaldoAwalMasterAssetDetail();
							saldoAwalMasterAssetDetail.setMasterAsset(masterAsset);
							saldoAwalMasterAssetDetail.setJumlah(0.0);
							saldoAwalMasterAssetDetail.setKeterangan("");
							saldoAwalMasterAssetDetail.setSaldoAwal(saldoAwalMasterAsset);
							Common.refreshSaveOrUpdate(saldoAwalMasterAssetDetail);
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
		upload.setDisabled(!merupakanAdmin || saldoAwalMasterAsset.getDisetujuiOleh() != null);
		upload.addEventListener("onUpload", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				UploadEvent uploadEvent = (UploadEvent) event;
				Media media = uploadEvent.getMedia();
				if (!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media))
					return;
				if (media.getName().toLowerCase().endsWith("xlsx")) {

					InputStream inputStream = media.getStreamData();
										final File file = new File(
							Sessions.getCurrent().getWebApp().getRealPath("/temp/" + media.getName()));
										file.getParentFile().mkdirs();
					FileOutputStream fileOutputStream = new FileOutputStream(file);
					byte[] buffer = new byte[8192];
					int read;
					while ((read = inputStream.read(buffer)) != -1) {
						fileOutputStream.write(buffer, 0, read);
					}
					fileOutputStream.close();
					inputStream.close();

					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							uploadDataMasterAsset(file, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									loadData(null);
									Clients.clearBusy();
								}
							}, contents);
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

		if (saldoAwalMasterAsset.getPenerimaanPengadaanMasterAsset() != null) {
			Integer cutomUkuranUpload = null;
			Boolean vertical = false;
			Boolean janganPreviewDiLayarUtama = true;
			Hbox hbox = new Hbox();

			EventListener eventListenerReload = new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Common.clear(SaldoAwalMasterAssetDetailAction.this);
					SaldoAwalMasterAssetDetailAction.this.display();
				}

			};

			LampiranLain.createDownloadUploadFileLain(hbox,
					saldoAwalMasterAsset.getPenerimaanPengadaanMasterAsset().getId(),
					PenerimaanPengadaanMasterAsset.class.getName(), "Tagihan", false, eventListenerReload, null, false,
					false, false, false, cutomUkuranUpload, vertical, janganPreviewDiLayarUtama);

			Box tombol = (Box) hbox.getAttribute("tombol");
			if (tombol != null) {
				List<Component> components = new ArrayList<Component>();
				for (Object o : tombol.getChildren()) {
					if (o instanceof Component) {
						Component s = (Component) o;
						components.add(s);
					}
				}

				for (Component s : components) {
					toolbar.appendChild(s);
				}
			}
		}

		button = new MyToolbarbuttonConfig("History", "/img/jadwal.png");
		button.setVisible(saldoAwalMasterAsset.getId() != null);
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				RevisiSaldoAwalMasterAssetDetailHelper revisiHelper = new RevisiSaldoAwalMasterAssetDetailHelper(
						saldoAwalMasterAsset, new EventListener() {

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

		grid = new MyGrid();
		grid.setWidth("100%");
		grid.setHeight("520px");
		grid.setStyle("border:0; background:#ffffff; overflow:auto;");
		grid.setMold("paging");
		grid.setPageSize(10);
		grid.setParent(groupbox);

		Columns columns = new Columns();

		columns.setParent(grid);
		columns.setSizable(true);
		columns.setStyle("background:#f8fafc; border-bottom:1px solid #e5e7eb; font-weight:bold;");

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama Barang / Jasa");
		column.setWidth("22%");

		if (saldoAwalMasterAsset.getPenerimaanPengadaanMasterAsset() == null) {
			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Anggaran");
			column.setWidth("10%");
		}

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Qty");
		column.setWidth("7%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Harga");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Persen");
		column.setWidth("7%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Diskon");
		column.setWidth("8%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("PPN");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nilai PPN");
		column.setAlign("right");
		column.setWidth("8%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("PPH");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nilai PPH");
		column.setAlign("right");
		column.setWidth("8%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Total");
		column.setAlign("right");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Keterangan");
		column.setWidth("14%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("7%");

		Foot foot = new Foot();
		foot.setParent(grid);

		Footer footer = new Footer("Total");
		foot.appendChild(footer);

		footer = new Footer();
		foot.appendChild(footer);

		footer = new Footer();
		foot.appendChild(footer);

		footer = new Footer();
		foot.appendChild(footer);

		footer = new Footer();
		foot.appendChild(footer);

		footer = new Footer();
		foot.appendChild(footer);

		footer = new Footer();
		foot.appendChild(footer);

		footer = new Footer();
		foot.appendChild(footer);

		footer = new Footer();
		foot.appendChild(footer);

		footer = new Footer();
		foot.appendChild(footer);

		footer = new Footer();
		foot.appendChild(footer);

		// sel footer kosong untuk kolom "Nilai PPH" (menjaga keselarasan kolom)
		footer = new Footer();
		foot.appendChild(footer);

		footerTotalSemua = new Footer(Common.numberFormat.get().format(0.0));
		foot.appendChild(footerTotalSemua);

		footer = new Footer();
		foot.appendChild(footer);

		loadData(null);

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				reloadNilai();
			}
		});

	}

	@SuppressWarnings("unchecked")
	public void reloadNilai() {
		Double nilai = 0.0;
		List<SaldoAwalMasterAssetDetail> saldoAwalMasterAssetDetails = initCriteria(true).list();

		for (SaldoAwalMasterAssetDetail saldoAwalMasterAssetDetail : saldoAwalMasterAssetDetails) {

			Double total = saldoAwalMasterAssetDetail.getHargaTotal();
			nilai += doubleValue(total);

		}

		try {

			Session session = HibernateUtil.currentSession();
			if (saldoAwalMasterAsset != null && saldoAwalMasterAsset.getId() != null) {
				// Perbarui HANYA kolom 'nilai' lewat HQL. Hindari flush PENUH entitas:
				// getKodeUnik() men-derive ulang kolom unik 'kodeunik' setiap dibaca,
				// dan saat flush penuh hasilnya bisa bentrok dgn baris lain ->
				// "duplicate key value violates ... saldo_awal_master_asset_kodeunik_key"
				// + transaksi abort. reloadNilai hanya perlu menyegarkan total, jadi
				// tidak boleh ikut menulis ulang kodeunik.
				final Double nilaiBaru = (nilai == null ? Double.valueOf(0.0) : nilai);
				org.hibernate.Query q = session.createQuery(
						"update SaldoAwalMasterAsset set nilai = :nilai where id = :id");
				// Jangan auto-flush entitas (yg akan menulis kodeunik) sebelum query ini.
				q.setFlushMode(org.hibernate.FlushMode.COMMIT);
				q.setParameter("nilai", nilaiBaru);
				q.setParameter("id", saldoAwalMasterAsset.getId());
				q.executeUpdate();
				// Selaraskan objek di memori dgn nilai DB (tetap bersih, tak menulis ulang).
				session.refresh(saldoAwalMasterAsset);
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		if (footerTotalSemua != null) {
			footerTotalSemua.setLabel(Common.numberFormat.get().format(nilai));
		}
	}

	public void uploadDataMasterAsset(final File file, final EventListener eventListener, final String[] contents)
			throws Exception {

		final ais.common.UploadReportHelper report = new ais.common.UploadReportHelper("Upload Saldo Awal Master Asset");
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
							SaldoAwalMasterAssetDetail saldoAwalMasterAssetDetail = id == null || id.equals(-1L) ? null
									: (SaldoAwalMasterAssetDetail) session
											.createCriteria(SaldoAwalMasterAssetDetail.class).add(Restrictions.idEq(id))
											.uniqueResult();
							MasterAsset masterAsset = (MasterAsset) Common.getSheetContentAsObject(sheet, 1, i,
									MasterAsset.class);
							Double jumlah = Common.getSheetContentAsDouble(sheet, 2, i);
							if (masterAsset == null) {
								String isbn = Common.getSheetContentAsString(sheet, 1, i);
								if (isbn != null && !isbn.trim().isEmpty()) {
									masterAsset = (MasterAsset) session.createCriteria(MasterAsset.class)
											.add(Restrictions.or(Restrictions.eq("kode", isbn.trim()),
													Restrictions.eq("nama", isbn.trim())))
											.addOrder(Order.desc("id")).setMaxResults(1).uniqueResult();
								}
							}
							if (masterAsset == null) {
								continue;
							}

							if (saldoAwalMasterAssetDetail == null) {
								saldoAwalMasterAssetDetail = (SaldoAwalMasterAssetDetail) session
										.createCriteria(SaldoAwalMasterAssetDetail.class)
										.add(Restrictions.eq("masterAsset", masterAsset))
										.add(Restrictions.eq("saldoAwal", saldoAwalMasterAsset))
										.addOrder(Order.desc("id")).setMaxResults(1).uniqueResult();
							}

							if (saldoAwalMasterAssetDetail == null) {
								saldoAwalMasterAssetDetail = new SaldoAwalMasterAssetDetail();
							}

							saldoAwalMasterAssetDetail.setJumlah(jumlah);
							saldoAwalMasterAssetDetail.setSaldoAwal(saldoAwalMasterAsset);
							saldoAwalMasterAssetDetail.setMasterAsset(masterAsset);

							session.getTransaction().begin();
							session.saveOrUpdate(saldoAwalMasterAssetDetail);
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
										e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/asset/helper/SaldoAwalMasterAssetDetailAction.java:1059");
				}

				HibernateUtil.closeSession();

				try {
					java.io.File rptFile = report.simpanLaporan();
					downloadPath.setValue(rptFile.getAbsolutePath());
				} catch (Exception eR) { ais.common.ErrorAuditUtil.record(eR, "auto-audit(empty-catch) SaldoAwalMasterAssetDetailAction laporan"); }
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
		return session.createCriteria(SaldoAwalMasterAssetDetail.class)

				.createAlias("masterAsset", "masterAsset")

				.add(nama == null || safeText(nama.getValue()).isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.or(
								Restrictions.ilike("masterAsset.kode", safeText(nama.getValue()), MatchMode.ANYWHERE),
								Restrictions.ilike("masterAsset.nama", safeText(nama.getValue()), MatchMode.ANYWHERE)))

				.addOrder(Order.asc("masterAsset.nama"))

				.add(Restrictions.eq("saldoAwal", saldoAwalMasterAsset)).setMaxResults(10000);
	}

	public static void pindahkanMenjadiBarangInventaris(SaldoAwalMasterAssetDetail saldoAwalMasterAssetDetail)
			throws Exception {
		Session session = HibernateUtil.currentNativeSession();
		session.refresh(saldoAwalMasterAssetDetail);
		Asset asset = (Asset) session.createCriteria(Asset.class)
				.add(Restrictions.eq("saldoAwalMasterAssetDetail", saldoAwalMasterAssetDetail)).setMaxResults(1)
				.uniqueResult();
		if (asset == null) {
			asset = new Asset();
		}
		asset.setSaldoAwalMasterAssetDetail(saldoAwalMasterAssetDetail);
		asset.setMasterAsset(saldoAwalMasterAssetDetail.getMasterAsset());
		asset.setDibuatOleh(Common.getCurrentUser());
		asset.setDisetujuiOleh(asset.getDibuatOleh());
		asset.setKeterangan(saldoAwalMasterAssetDetail.getKeterangan());

		asset.setNama(saldoAwalMasterAssetDetail.getMasterAsset().getNama());

		asset.setTanggalPersetujuan(saldoAwalMasterAssetDetail.getSaldoAwal().getTanggalPersetujuan());
		asset.setTbmuser(asset.getDibuatOleh());

		session.getTransaction().begin();
		if (asset.getId() == null) {
			session.save(asset);
		} else {
			Common.refreshSaveOrUpdate(session, asset);
		}
		session.getTransaction().commit();

		session.getTransaction().begin();
		saldoAwalMasterAssetDetail.setAsset(asset);
		Common.refreshSaveOrUpdate(session, saldoAwalMasterAssetDetail);
		session.getTransaction().commit();
		if (session.isOpen()) {
			session.disconnect();
			session.close();
		}
		HibernateUtil.closeSession();

		for (int i = 0; i < saldoAwalMasterAssetDetail.getJumlah(); i++) {
			try {
				AssetDetail assetDetail = new AssetDetail();
				assetDetail.setLokasi(saldoAwalMasterAssetDetail.getSaldoAwal().getLokasi());
				assetDetail.setRuang(saldoAwalMasterAssetDetail.getSaldoAwal().getRuang());
				assetDetail.setPemilikAsset(saldoAwalMasterAssetDetail.getSaldoAwal().getPemilikAsset());

				assetDetail.setNama(asset.getNama());
				assetDetail.setKeterangan(asset.getKeterangan());
				assetDetail.setStatusAsset(AssetUtil.AKTIF);
				assetDetail.setAsset(asset);
				assetDetail.setBarcode(BarcodeCommon.generateCode());
				assetDetail.setTanggalBeli(saldoAwalMasterAssetDetail.getSaldoAwal().getTanggalPersetujuan());

				Double dpp = (saldoAwalMasterAssetDetail.getHarga());
				Double hargaTotal = (dpp);
				assetDetail.setHargaBeli(hargaTotal);

				assetDetail.setBarcode(AssetDetail.generateBarcode(assetDetail, null, true));

				Session sessionData = HibernateUtil.currentNativeSession();
				sessionData.getTransaction().begin();
				sessionData.save(assetDetail);
				sessionData.getTransaction().commit();
				sessionData.disconnect();
				sessionData.close();
				HibernateUtil.closeSession();
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		}

		MyMessageboxConfig.show("Pemindahan menjadi barang inventaris sukses dilakukan", "Informasi",
				MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
	}

	public static void pindahkanMenjadiBarangInventaris(
			PermintaanPengadaanMasterAssetDetail permintaanPengadaanMasterAssetDetail) throws Exception {
		Session session = HibernateUtil.currentSession();
		session.refresh(permintaanPengadaanMasterAssetDetail);
		Asset asset = (Asset) session.createCriteria(Asset.class)
				.add(Restrictions.eq("permintaanPengadaanMasterAssetDetail", permintaanPengadaanMasterAssetDetail))
				.setMaxResults(1).uniqueResult();
		if (asset == null) {
			asset = new Asset();
		}
		asset.setPermintaanPengadaanMasterAssetDetail(permintaanPengadaanMasterAssetDetail);
		asset.setMasterAsset(permintaanPengadaanMasterAssetDetail.getMasterAsset());
		asset.setDibuatOleh(Common.getCurrentUser());
		asset.setDisetujuiOleh(asset.getDibuatOleh());
		asset.setKeterangan(permintaanPengadaanMasterAssetDetail.getKeterangan());

		asset.setNama(permintaanPengadaanMasterAssetDetail.getMasterAsset().getNama());

		asset.setTanggalPersetujuan(
				permintaanPengadaanMasterAssetDetail.getUangMuka().getPertangungjawaban().getTanggalPersetujuan());
		asset.setTbmuser(asset.getDibuatOleh());
		Common.refreshSaveOrUpdate(session, asset);
		permintaanPengadaanMasterAssetDetail.setAsset(asset);
		Common.refreshSaveOrUpdate(session, permintaanPengadaanMasterAssetDetail);

		for (int i = 0; i < permintaanPengadaanMasterAssetDetail.getJumlah(); i++) {
			try {
				AssetDetail assetDetail = new AssetDetail();
				assetDetail.setLokasi(
						permintaanPengadaanMasterAssetDetail.getPermintaanPengadaanMasterAsset().getLokasi());
				assetDetail
						.setRuang(permintaanPengadaanMasterAssetDetail.getPermintaanPengadaanMasterAsset().getRuang());
				assetDetail.setPemilikAsset(
						permintaanPengadaanMasterAssetDetail.getPermintaanPengadaanMasterAsset().getPemilikAsset());

				assetDetail.setNama(asset.getNama());
				assetDetail.setKeterangan(asset.getKeterangan());
				assetDetail.setStatusAsset(AssetUtil.AKTIF);
				assetDetail.setAsset(asset);
				assetDetail.setTanggalBeli(permintaanPengadaanMasterAssetDetail.getUangMuka().getPertangungjawaban()
						.getTanggalPersetujuan());

				Double dpp = (permintaanPengadaanMasterAssetDetail.getUangMuka().getNilai());
				Double ppn = 0.0;
				Double hargaTotal = (dpp + ppn);
				assetDetail.setHargaBeli(hargaTotal);

				assetDetail.setBarcode(AssetDetail.generateBarcode(assetDetail, null, true));

				Session sessionData = HibernateUtil.currentNativeSession();
				sessionData.getTransaction().begin();
				sessionData.save(assetDetail);
				sessionData.getTransaction().commit();
				sessionData.disconnect();
				sessionData.close();
				HibernateUtil.closeSession();
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		}

		MyMessageboxConfig.show("Pemindahan menjadi barang inventaris sukses dilakukan", "Informasi",
				MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
	}
}
