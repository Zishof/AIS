package ais.action.master.asset.helper;
/* ENHANCED_PENGGUNAAN_ANGGARAN_MEMORY_SAFE_FIX_HIBERNATE_SESSION_2026_06_03 - Java 1.6/1.7 compatible. */

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.FlushMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Foot;
import org.zkoss.zul.Footer;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.asset.MasterAsset;
import ais.database.model.asset.PermintaanPengadaanMasterAsset;
import ais.database.model.asset.PermintaanPengadaanMasterAssetDetail;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyGroupboxStyled;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;

public class PermintaanPengadaanMasterAssetHelper {

	private MyGrid gridMasterAsset;
	private boolean edit = false;
	private boolean delete = false;
	private double totalSemua = 0.0;
	private Footer footerTotalSemua;
	private boolean persetujuan = false;
	private PermintaanPengadaanMasterAsset currentPermintaanPengadaanMasterAsset;

	public PermintaanPengadaanMasterAssetHelper(MyGrid gridMasterAsset) {
		this.gridMasterAsset = gridMasterAsset;
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

	private static double hitungTotal(PermintaanPengadaanMasterAssetDetail detail) {
		if (detail == null) {
			return 0.0;
		}
		return doubleValue(detail.getJumlah()) * doubleValue(detail.getHargaBeli());
	}

	private static boolean isPermintaanDisetujui(PermintaanPengadaanMasterAsset data) {
		return data != null && data.getDisetujuiOleh() != null;
	}

	private static boolean isDetailReadonly(PermintaanPengadaanMasterAssetDetail detail, boolean edit) {
		return detail == null || isPermintaanDisetujui(detail.getPermintaanPengadaanMasterAsset()) || !edit;
	}

	private static boolean isHargaBolehDiubah(PermintaanPengadaanMasterAssetDetail detail) {
		if (detail == null || detail.getMasterAsset() == null) {
			return true;
		}
		return detail.getMasterAsset().getHargaBolehDiubah();
	}

	private static PermintaanPengadaanMasterAsset getManagedPermintaan(Session session,
			PermintaanPengadaanMasterAsset data) {
		if (session == null || data == null || data.getId() == null) {
			return data;
		}
		try {
			Object obj = session.get(PermintaanPengadaanMasterAsset.class, data.getId());
			return obj == null ? data : (PermintaanPengadaanMasterAsset) obj;
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			return data;
		}
	}

	private static MasterAsset getManagedMasterAsset(Session session, MasterAsset data) {
		if (session == null || data == null || data.getId() == null) {
			return data;
		}
		try {
			Object obj = session.get(MasterAsset.class, data.getId());
			return obj == null ? data : (MasterAsset) obj;
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			return data;
		}
	}

	private static PermintaanPengadaanMasterAssetDetail getManagedDetail(Session session,
			PermintaanPengadaanMasterAssetDetail data) {
		if (session == null || data == null || data.getId() == null) {
			return data;
		}
		try {
			Object obj = session.get(PermintaanPengadaanMasterAssetDetail.class, data.getId());
			return obj == null ? data : (PermintaanPengadaanMasterAssetDetail) obj;
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			return data;
		}
	}

	public MyGroupboxStyled initDetail(final PermintaanPengadaanMasterAsset permintaanPengadaanMasterAsset,
			boolean persetujuan) throws Exception {
		this.currentPermintaanPengadaanMasterAsset = permintaanPengadaanMasterAsset;
		this.persetujuan = persetujuan;
		MyGroupboxStyled myGroupboxStyled = new MyGroupboxStyled();
		myGroupboxStyled.appendChild(new MyCaptionStyled("Daftar Permintaan Barang/Jasa"));

		edit = !isPermintaanDisetujui(permintaanPengadaanMasterAsset);
		delete = !isPermintaanDisetujui(permintaanPengadaanMasterAsset);

		Toolbar toolbar = new Toolbar();
		toolbar.setVisible(!persetujuan);
		toolbar.setHeight("34px");
		toolbar.setStyle("border:0; background:#f8fafc; padding:4px 6px; margin-bottom:6px;");
		toolbar.setParent(myGroupboxStyled);

		MyToolbarbuttonConfig add = new MyToolbarbuttonConfig("Ambil Data Barang/Jasa", "/img/new.gif");
		add.setParent(toolbar);
		add.setVisible(!isPermintaanDisetujui(permintaanPengadaanMasterAsset));
		add.setTooltiptext("Tambah data barang/jasa ke dalam permintaan pengadaan");
		add.addEventListener("onClick", new EventListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {
				List<MasterAsset> masterAssets = new ArrayList<MasterAsset>();
				if (gridMasterAsset != null && gridMasterAsset.getRows() != null) {
					List<Row> myrows = gridMasterAsset.getRows().getChildren();
					for (Row row : myrows) {
						Object detailObject = row.getAttribute("permintaanPengadaanMasterAssetDetail");
						if (detailObject instanceof PermintaanPengadaanMasterAssetDetail) {
							MasterAsset masterAsset = ((PermintaanPengadaanMasterAssetDetail) detailObject).getMasterAsset();
							if (masterAsset != null) {
								masterAssets.add(masterAsset);
							}
						}
					}
				}

				AmbilDataMasterAssetBanyak ambilDataMasterAssetBanyak = new AmbilDataMasterAssetBanyak(masterAssets,
						null);
				ambilDataMasterAssetBanyak.setHeight("95%");
				ambilDataMasterAssetBanyak.setWidth("90%");
				ambilDataMasterAssetBanyak.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				ambilDataMasterAssetBanyak.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						List<MasterAsset> masterAssets = (List<MasterAsset>) arg0.getData();
						if (masterAssets == null || masterAssets.isEmpty()) {
							return;
						}

						Rows rows = gridMasterAsset.getRows() == null ? new Rows() : gridMasterAsset.getRows();
						rows.setParent(gridMasterAsset);

						for (MasterAsset masterAsset : masterAssets) {
							if (masterAsset == null) {
								continue;
							}

							PermintaanPengadaanMasterAssetDetail detail = new PermintaanPengadaanMasterAssetDetail();
							detail.setJumlah(1.0);
							detail.setKeterangan("");

							if (permintaanPengadaanMasterAsset != null && permintaanPengadaanMasterAsset.getId() != null) {
								Session session = HibernateUtil.currentSession();
								PermintaanPengadaanMasterAsset parentManaged = getManagedPermintaan(session,
										permintaanPengadaanMasterAsset);
								MasterAsset masterManaged = getManagedMasterAsset(session, masterAsset);
								detail.setPermintaanPengadaanMasterAsset(parentManaged);
								detail.setMasterAsset(masterManaged);
								session.save(detail);
							} else {
								detail.setPermintaanPengadaanMasterAsset(permintaanPengadaanMasterAsset);
								detail.setMasterAsset(masterAsset);
							}

							Row row = new Row();
							row.setValign("top");
							row.setParent(rows);
							initRow(row, detail);
						}
						eventListenerHitungUlang.onEvent(arg0);
					}
				});

				ambilDataMasterAssetBanyak.onModal();
			}
		});

		MyToolbarbuttonConfig cari = new MyToolbarbuttonConfig("Refresh", "/img/Button-Refresh-icon.png");
		cari.setParent(toolbar);
		cari.setVisible(permintaanPengadaanMasterAsset != null && permintaanPengadaanMasterAsset.getId() != null);
		cari.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				loadDataDetail(permintaanPengadaanMasterAsset);
			}
		});

		MyToolbarbuttonConfig ikutiHargaDpp = new MyToolbarbuttonConfig("Ikuti Harga DPP Terbaru", "/img/svg/refresh.svg");
		ikutiHargaDpp.setParent(toolbar);
		ikutiHargaDpp.setVisible(!persetujuan && !isPermintaanDisetujui(permintaanPengadaanMasterAsset));
		ikutiHargaDpp.setTooltiptext("Mengisi harga barang/jasa dari riwayat DPP terbaru Tagihan Vendor, PO, dan BAST.");
		ikutiHargaDpp.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				ikutiHargaDppTerbaruSemuaBaris();
			}
		});

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("History", "/img/jadwal.png");
		button.setVisible(permintaanPengadaanMasterAsset != null && permintaanPengadaanMasterAsset.getId() != null);
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				RevisiPermintaanPengadaanMasterAssetDetailHelper revisiHelper = new RevisiPermintaanPengadaanMasterAssetDetailHelper(
						permintaanPengadaanMasterAsset, new EventListener() {
							@Override
							public void onEvent(Event arg0) throws Exception {
								loadDataDetail(permintaanPengadaanMasterAsset);
							}
						});
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(revisiHelper);
				revisiHelper.setVisible(true);
				revisiHelper.onModal();
			}
		});
		button.setParent(toolbar);

		gridMasterAsset.setParent(myGroupboxStyled);
		gridMasterAsset.setWidth("100%");
		gridMasterAsset.setHeight("100%");
		gridMasterAsset.setStyle("min-height:360px; border:1px solid #e5e7eb; border-radius:8px; overflow:auto;");

		Columns columns = new Columns();
		columns.setParent(gridMasterAsset);
		columns.setSizable(true);
		columns.setStyle("background:#f8fafc; border-bottom:1px solid #e5e7eb; font-weight:bold;");

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama Barang/Jasa");
		column.setWidth("28%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Qty");
		column.setAlign("right");
		column.setWidth("12%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Harga");
		column.setAlign("right");
		column.setWidth("12%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("DPP Terbaru");
		column.setAlign("right");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Total");
		column.setAlign("right");
		column.setWidth("12%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Keterangan");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("6%");

		footerTotalSemua = new Footer(Common.numberFormat.get().format(totalSemua));

		loadDataDetail(permintaanPengadaanMasterAsset);

		Foot foot = new Foot();
		foot.setParent(gridMasterAsset);

		Footer footer = new Footer("Total");
		foot.appendChild(footer);
		footer = new Footer();
		foot.appendChild(footer);
		footer = new Footer();
		foot.appendChild(footer);
		footer = new Footer();
		foot.appendChild(footer);
		foot.appendChild(footerTotalSemua);
		footer = new Footer();
		foot.appendChild(footer);
		footer = new Footer();
		foot.appendChild(footer);

		return myGroupboxStyled;
	}

	public EventListener eventListenerHitungUlang = new EventListener() {
		@SuppressWarnings("unchecked")
		@Override
		public void onEvent(Event arg0) throws Exception {
			if (gridMasterAsset == null || gridMasterAsset.getRows() == null) {
				return;
			}
			List<Row> rows = gridMasterAsset.getRows().getChildren();
			totalSemua = 0.0;
			for (Row row : rows) {
				if (row.isVisible()) {
					Object detailObject = row.getAttribute("permintaanPengadaanMasterAssetDetail");
					if (detailObject instanceof PermintaanPengadaanMasterAssetDetail) {
						totalSemua += hitungTotal((PermintaanPengadaanMasterAssetDetail) detailObject);
					}
				}
			}
			if (footerTotalSemua != null) {
				footerTotalSemua.setLabel(Common.numberFormat.get().format(totalSemua));
			}
		}
	};

	@SuppressWarnings("unchecked")
	private void loadDataDetail(final PermintaanPengadaanMasterAsset permintaanPengadaanMasterAsset) throws Exception {
		List<PermintaanPengadaanMasterAssetDetail> details = new ArrayList<PermintaanPengadaanMasterAssetDetail>();

		if (permintaanPengadaanMasterAsset != null && permintaanPengadaanMasterAsset.getId() != null) {
			Session session = HibernateUtil.currentSession();
			Long idPermintaan = permintaanPengadaanMasterAsset.getId();

			/*
			 * Jangan memakai Restrictions.eq("permintaanPengadaanMasterAsset", object) di sini.
			 * Pada beberapa alur ZK/Hibernate object parent dapat berasal dari session lama, sehingga
			 * auto flush Hibernate dapat memicu: Illegal attempt to associate a collection with two open sessions.
			 *
			 * Selain filter berdasarkan ID, auto-flush dimatikan sementara hanya untuk query load detail.
			 * Ini penting karena error bisa tetap terjadi sebelum Criteria.list() dieksekusi, yaitu saat
			 * Hibernate mencoba flush entity lain yang kebetulan masih menempel pada current session.
			 * Setelah query selesai, flush mode dikembalikan seperti semula agar proses simpan/update lain
			 * tetap berjalan normal.
			 */
			FlushMode oldFlushMode = null;
			try {
				oldFlushMode = session.getFlushMode();
				session.setFlushMode(FlushMode.MANUAL);

				try {
					if (session.contains(permintaanPengadaanMasterAsset)) {
						session.evict(permintaanPengadaanMasterAsset);
					}
				} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/asset/helper/PermintaanPengadaanMasterAssetHelper.java:386");
				}

				details = session.createCriteria(PermintaanPengadaanMasterAssetDetail.class)
						.createAlias("permintaanPengadaanMasterAsset", "pr")
						.add(Restrictions.eq("pr.id", idPermintaan)).addOrder(Order.asc("id")).list();
			} finally {
				try {
					if (oldFlushMode != null) {
						session.setFlushMode(oldFlushMode);
					}
				} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/asset/helper/PermintaanPengadaanMasterAssetHelper.java:397");
				}
			}
		}

		Rows rows = gridMasterAsset.getRows() == null ? new Rows() : gridMasterAsset.getRows();
		rows.setParent(gridMasterAsset);
		Common.clear(rows);
		totalSemua = 0.0;
		for (PermintaanPengadaanMasterAssetDetail detail : details) {
			Row row = new Row();
			row.setValign("top");
			row.setStyle("border-bottom:1px solid #eef2f7;");
			row.setParent(rows);
			initRow(row, detail);
		}

		Common.createDefaultTimer(eventListenerHitungUlang);
	}

	public void initRow(final Row row, final PermintaanPengadaanMasterAssetDetail detail) throws Exception {
		if (detail == null) {
			return;
		}
		final MasterAsset masterAsset = detail.getMasterAsset();
		row.setValign("top");
		row.setAttribute("permintaanPengadaanMasterAssetDetail", detail);

		final MyDoublebox jumlah = new MyDoublebox(doubleValue(detail.getJumlah()));
		final MyDoublebox harga = new MyDoublebox(doubleValue(detail.getHargaBeli()));
		final Label total = new MyLabelKecil(Common.numberFormat.get().format(hitungTotal(detail)));

		Vbox infoBarang = RevisiHelper.createNewRevisi(PermintaanPengadaanMasterAssetDetail.class, detail,
				masterAsset == null ? "" : safeText(masterAsset.getNama()));
		infoBarang.setStyle("line-height:1.4;");
		infoBarang.setParent(row);

		if (masterAsset != null) {
			new Label(safeText(masterAsset.getMerk())).setParent(infoBarang);
			new Label(masterAsset.getJenisAsset() == null ? "" : safeText(masterAsset.getJenisAsset().getNama()))
					.setParent(infoBarang);
			new Label(masterAsset.getKelompokAsset() == null ? "" : safeText(masterAsset.getKelompokAsset().getNama()))
					.setParent(infoBarang);
		}

		final boolean readonly = isDetailReadonly(detail, edit);

		if (persetujuan) {
			new MyLabelKecil(formatNumber(detail.getJumlah())).setParent(row);
		} else {
			jumlah.setParent(row);
			jumlah.setDisabled(readonly);
			jumlah.setStyle("text-align:right");
			jumlah.setWidth("82%");
			jumlah.addEventListener(Events.ON_CHANGE, new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					Double nilaiBaru = Double.valueOf(Math.abs(jumlah.getValue() == null ? 0.0 : jumlah.getValue().doubleValue()));
					jumlah.setValue(nilaiBaru);
					detail.setJumlah(nilaiBaru);
					row.setAttribute("permintaanPengadaanMasterAssetDetail", detail);

					if (detail.getId() != null) {
						Session session = HibernateUtil.currentSession();
						PermintaanPengadaanMasterAssetDetail managed = getManagedDetail(session, detail);
						managed.setJumlah(nilaiBaru);
						Common.refreshUpdate(session, managed);
					}

					total.setValue(Common.numberFormat.get().format(hitungTotal(detail)));
					eventListenerHitungUlang.onEvent(arg0);
				}
			});
		}

		if (!isHargaBolehDiubah(detail) || persetujuan) {
			new MyLabelKecil(formatNumber(detail.getHargaBeli())).setParent(row);
		} else {
			harga.setParent(row);
			harga.setDisabled(readonly);
			harga.setStyle("text-align:right");
			harga.setWidth("82%");
			harga.addEventListener(Events.ON_CHANGE, new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					Double nilaiBaru = Double.valueOf(Math.abs(harga.getValue() == null ? 0.0 : harga.getValue().doubleValue()));
					harga.setValue(nilaiBaru);
					detail.setHargaBeli(nilaiBaru);
					row.setAttribute("permintaanPengadaanMasterAssetDetail", detail);

					if (detail.getId() != null) {
						Session session = HibernateUtil.currentSession();
						PermintaanPengadaanMasterAssetDetail managed = getManagedDetail(session, detail);
						managed.setHargaBeli(nilaiBaru);
						Common.refreshUpdate(session, managed);
					}

					total.setValue(Common.numberFormat.get().format(hitungTotal(detail)));
					eventListenerHitungUlang.onEvent(arg0);
				}
			});
		}

		appendHargaDppTerbaru(row, detail, harga, total, readonly || !isHargaBolehDiubah(detail) || persetujuan);

		total.setStyle("text-align:right; font-weight:bold;");
		total.setParent(row);

		final MyTextbox keterangan = new MyTextbox(safeText(detail.getKeterangan()));
		keterangan.setWidth("94%");
		keterangan.setHeight("32px");

		if (persetujuan) {
			new MyLabelKecil(safeText(detail.getKeterangan())).setParent(row);
		} else {
			keterangan.setParent(row);
			keterangan.setDisabled(readonly);
			keterangan.addEventListener(Events.ON_CHANGE, new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					String nilaiBaru = safeText(keterangan.getValue());
					detail.setKeterangan(nilaiBaru);
					row.setAttribute("permintaanPengadaanMasterAssetDetail", detail);

					if (detail.getId() != null) {
						Session session = HibernateUtil.currentSession();
						PermintaanPengadaanMasterAssetDetail managed = getManagedDetail(session, detail);
						managed.setKeterangan(nilaiBaru);
						Common.refreshUpdate(session, managed);
					}
				}
			});
		}

		Hbox hbox = new Hbox();
		hbox.setParent(row);

		if (!persetujuan) {
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
			button.setVisible(delete);
			button.setParent(hbox);
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
										if (detail.getId() != null) {
											Session session = HibernateUtil.currentSession();
											PermintaanPengadaanMasterAssetDetail managed = getManagedDetail(session, detail);
											session.delete(managed);
										}
										row.setVisible(false);
										row.detach();
										eventListenerHitungUlang.onEvent(event);
									}
								}
							});
				}
			});
		}
	}


	private void appendHargaDppTerbaru(final Row row, final PermintaanPengadaanMasterAssetDetail detail,
			final MyDoublebox harga, final Label total, boolean disabled) throws Exception {
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
				terapkanHargaDppTerbaru(detail, info, harga, total);
			}
		});
	}

	private void terapkanHargaDppTerbaru(final PermintaanPengadaanMasterAssetDetail detail,
			final AssetHargaDppHistoryUtil.HargaDppInfo info, final MyDoublebox harga, final Label total) throws Exception {
		if (detail == null || info == null || !info.hasHarga()) {
			MyMessageboxConfig.show("Mohon maaf, belum ada riwayat harga DPP yang dapat digunakan untuk barang ini. Langkah yang dapat dilakukan: (1) Pastikan barang/aset sudah pernah memiliki transaksi pengadaan sebelumnya; (2) Input harga DPP secara manual pada field Harga Beli; (3) Riwayat DPP akan tersedia setelah transaksi pengadaan pertama selesai. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Informasi", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return;
		}

		Double dppSatuan = info.getHargaDppSatuan();
		detail.setHargaBeli(dppSatuan);
		if (harga != null) {
			harga.setValue(dppSatuan);
		}
		if (detail.getId() != null) {
			Session session = HibernateUtil.currentSession();
			PermintaanPengadaanMasterAssetDetail managed = getManagedDetail(session, detail);
			managed.setHargaBeli(dppSatuan);
			Common.refreshUpdate(session, managed);
			updateHargaDefaultMasterAsset(session, managed.getMasterAsset(), dppSatuan);
		} else {
			Session session = HibernateUtil.currentSession();
			updateHargaDefaultMasterAsset(session, detail.getMasterAsset(), dppSatuan);
		}
		if (total != null) {
			total.setValue(Common.numberFormat.get().format(hitungTotal(detail)));
		}
		eventListenerHitungUlang.onEvent(null);
	}

	@SuppressWarnings("unchecked")
	private void ikutiHargaDppTerbaruSemuaBaris() throws Exception {
		if (gridMasterAsset == null || gridMasterAsset.getRows() == null) {
			return;
		}
		Session session = HibernateUtil.currentSession();
		List<Row> rows = gridMasterAsset.getRows().getChildren();
		int berhasil = 0;
		for (Row row : rows) {
			Object obj = row.getAttribute("permintaanPengadaanMasterAssetDetail");
			if (!(obj instanceof PermintaanPengadaanMasterAssetDetail)) {
				continue;
			}
			PermintaanPengadaanMasterAssetDetail detail = (PermintaanPengadaanMasterAssetDetail) obj;
			if (detail == null || detail.getMasterAsset() == null || isDetailReadonly(detail, edit)
					|| !isHargaBolehDiubah(detail)) {
				continue;
			}
			AssetHargaDppHistoryUtil.HargaDppInfo info = AssetHargaDppHistoryUtil.ambilHargaDppTerbaru(session,
					detail.getMasterAsset(), null);
			if (info != null && info.hasHarga()) {
				detail.setHargaBeli(info.getHargaDppSatuan());
				if (detail.getId() != null) {
					PermintaanPengadaanMasterAssetDetail managed = getManagedDetail(session, detail);
					managed.setHargaBeli(info.getHargaDppSatuan());
					Common.refreshUpdate(session, managed);
					updateHargaDefaultMasterAsset(session, managed.getMasterAsset(), info.getHargaDppSatuan());
				} else {
					updateHargaDefaultMasterAsset(session, detail.getMasterAsset(), info.getHargaDppSatuan());
				}
				berhasil++;
			}
		}
		loadDataDetail(currentPermintaanPengadaanMasterAsset);
		MyMessageboxConfig.show("Harga DPP terbaru diterapkan pada " + berhasil + " baris.", "Informasi",
				MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
	}

	private void updateHargaDefaultMasterAsset(Session session, MasterAsset masterAsset, Double hargaDppSatuan) {
		if (session == null || masterAsset == null || masterAsset.getId() == null || hargaDppSatuan == null) {
			return;
		}
		try {
			MasterAsset managed = getManagedMasterAsset(session, masterAsset);
			managed.setHargaBeliDefault(hargaDppSatuan);
			Common.refreshUpdate(session, managed);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

}
