package ais.action.master.sirs.detail;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Foot;
import org.zkoss.zul.Footer;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Messagebox;
import ais.ui.util.MyMessageboxConfig;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import org.zkoss.zul.RowRenderer;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.sirs.helper.AmbilDataDokterBanbox;
import ais.action.master.sirs.helper.AmbilDataItemMedisBanyakBerdasarkanStok;
import ais.action.master.sirs.helper.AmbilDataRacikanBanyak;
import ais.action.master.sirs.helper.BuatRacikanBaruHelper;
import ais.action.master.sirs.util.CommonTarifItem;
import ais.action.master.sirs.util.CommonTarifTindakan;
import ais.common.Common;
import ais.common.CommonSirs;
import ais.common.listener.GetTransaksi;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.sirs.BiayaTindakanPerKelas;
import ais.database.model.sirs.DiagnosaPenyakit;
import ais.database.model.sirs.Diskon;
import ais.database.model.sirs.Dokter;
import ais.database.model.sirs.HargaJualItem;
import ais.database.model.sirs.ItemMedis;
import ais.database.model.sirs.KelasPerawatan;
import ais.database.model.sirs.Racikan;
import ais.database.model.sirs.Tindakan;
import ais.database.model.sirs.TransaksiMedisDetail;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyTextbox;

public class TransaksiItemDetailHelper extends Borderlayout {

	/**
		 * 
		 */
	private static final long serialVersionUID = 5086031585928643232L;

	private Grid grid;
	private MyTextbox barcode;

	private Footer totalDiskon;
	private Footer totalPajak;
	private Footer total;
	private Footer totalHrg;

	private GetTransaksi dataParent;

	public TransaksiItemDetailHelper(GetTransaksi getTransaksi) throws Exception {
		super();
		this.dataParent = getTransaksi;
		display();
	}

	class TransaksiDetailRenderer extends ais.ui.util.MyRowRenderer {

		public TransaksiDetailRenderer() {

		}

		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");
			final TransaksiMedisDetail transaksiDetail = (TransaksiMedisDetail) data;

			final MyDoublebox jumlah;
			jumlah = new MyDoublebox(transaksiDetail.getQty() == null ? 0.0 : transaksiDetail.getQty());

			final Label total = new Label();
			final Label diskon;
			final Label pajak;

			if (transaksiDetail.getRacikan() == null) {

				final MyDoublebox harga;
				harga = new MyDoublebox(transaksiDetail.getMenggunakanAmountCustom() ? transaksiDetail.getAmountCustom()
						: transaksiDetail.getAmount());
				harga.setReadonly(!transaksiDetail.getMenggunakanAmountCustom());

				diskon = new Label(Common.numberFormat.get().format(transaksiDetail.getDiskonPersen()) + "% ("
						+ Common.numberFormat.get().format(transaksiDetail.getDiskon()) + ")");
				pajak = new Label(Common.numberFormat.get().format(transaksiDetail.getPajakPersen()) + "% ("
						+ Common.numberFormat.get().format(transaksiDetail.getPajak()) + ")");

				new Label("").setParent(row);

				final AmbilDataDokterBanbox dokter = new AmbilDataDokterBanbox();
				dokter.setAttribute("dokter", transaksiDetail.getDokter());
				dokter.setValue(transaksiDetail.getDokter() == null ? "" : transaksiDetail.getDokter().getNama());
				dokter.setReadonly(true);
				dokter.setParent(row);
				dokter.setWidth("95%");

				final EventListener eventListener = new EventListener() {

					@Override
					public void onEvent(Event event) throws Exception {

						final KelasPerawatan kelasPerawatan = dataParent.getKelasPerawatan();

						if (kelasPerawatan == null) {
							MyMessageboxConfig.show("Mohon Bapak/Ibu memilih salah satu kelas perawatan terlebih dahulu. Langkah yang dapat dilakukan: (1) buka daftar kelas perawatan; (2) pilih salah satu kelas perawatan yang sesuai; (3) ulangi kembali proses ini.", "Peringatan", MyMessageboxConfig.OK,
									MyMessageboxConfig.EXCLAMATION);
							return;
						}

						Double myJumlah = jumlah.getValue() == null ? 0.0 : jumlah.getValue();

						transaksiDetail.setDokter((Dokter) dokter.getAttribute("dokter"));

						HargaJualItem hargaJualItem = CommonTarifItem.getHargaJualItem(transaksiDetail.getItem(),
								kelasPerawatan, transaksiDetail.getDokter(),
								dataParent.getTransaksi().getPendaftaran().getAsuransi(),
								dataParent.getTransaksi().getPendaftaran().getKomunitass(),
								dataParent.getTransaksi().getPendaftaran().getPasien());

						transaksiDetail.setAmount(hargaJualItem.getHargaJual());

						if (!transaksiDetail.getMenggunakanAmountCustom()) {
							harga.setValue(transaksiDetail.getAmount());
						}

						transaksiDetail.setMenggunakanAmountCustom(hargaJualItem.getHargaBisaDirubahSaatTransaksi()
								&& hargaJualItem.getPembagianBiayaDalamPersen());

						harga.setReadonly(!transaksiDetail.getMenggunakanAmountCustom());

						transaksiDetail.setDiskonPersen(CommonSirs.getTotalDiskonDalamPersen(transaksiDetail.getItem(),
								null, null, myJumlah.intValue(), dataParent.getTransaksi().getTanggalTransaksi(),
								dataParent.getTransaksi().getPendaftaran() == null ? null
										: dataParent.getTransaksi().getPendaftaran().getAsuransi(),
								dataParent.getTransaksi().getPendaftaran() == null ? null
										: dataParent.getTransaksi().getPendaftaran().getKomunitass()));

						transaksiDetail.setDiskons(new HashSet<Diskon>());
						transaksiDetail.getDiskons()
								.addAll(CommonSirs.getDiskonSekarang(transaksiDetail.getItem(), null, null,
										myJumlah.intValue(), dataParent.getTransaksi().getTanggalTransaksi(),
										dataParent.getTransaksi().getPendaftaran() == null ? null
												: dataParent.getTransaksi().getPendaftaran().getAsuransi(),
										dataParent.getTransaksi().getPendaftaran() == null ? null
												: dataParent.getTransaksi().getPendaftaran().getKomunitass()));

						transaksiDetail.setQty(myJumlah);

						diskon.setValue(Common.numberFormat.get().format(transaksiDetail.getDiskonPersen()) + "% ("
								+ Common.numberFormat.get().format(transaksiDetail.getDiskon() * myJumlah) + ")");

						pajak.setValue(Common.numberFormat.get().format(transaksiDetail.getPajakPersen()) + "% ("
								+ Common.numberFormat.get().format(transaksiDetail.getPajak() * myJumlah) + ")");

						total.setValue(Common.numberFormat.get().format(transaksiDetail.getHasilPenghitunganTotal()));

						Session session = HibernateUtil.currentSession();
						session.update(transaksiDetail);
						dataParent.getSimpan().setDisabled(false);
						loadTotal();
					}
				};

				RevisiHelper
						.createNewRevisi(TransaksiMedisDetail.class, transaksiDetail,
								transaksiDetail.getItem() == null ? "" : transaksiDetail.getItem().getNama())
						.setParent(row);

				jumlah.setParent(row);
				new Label(transaksiDetail.getItem() == null || transaksiDetail.getItem().getSatuanItem() == null ? ""
						: transaksiDetail.getItem().getSatuanItem().getNama()).setParent(row);

				jumlah.setStyle("text-align:right");
				jumlah.setWidth("90%");
				jumlah.addEventListener(Events.ON_CHANGE, eventListener);

				harga.addEventListener("onChange", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						Session session = HibernateUtil.currentSession();
						session.refresh(transaksiDetail);
						transaksiDetail.setMenggunakanAmountCustom(true);
						transaksiDetail.setAmountCustom(harga.getValue() == null ? 0.0 : harga.getValue());

						session.update(transaksiDetail);

						eventListener.onEvent(arg0);
					}
				});

				dokter.setEventListener(eventListener);

				harga.setParent(row);

				diskon.setParent(row);
				pajak.setParent(row);

				total.setValue(Common.numberFormat.get().format(transaksiDetail.getHasilPenghitunganTotal()));
				total.setParent(row);

			} else {

				final Label harga;
				harga = new Label();

				diskon = new Label(Common.numberFormat.get().format(transaksiDetail.getDiskon()));
				pajak = new Label(Common.numberFormat.get().format(transaksiDetail.getPajak()));

				final Combobox jasaRacik = new Combobox();
				Common.insertComboItems(jasaRacik, "nama", CommonSirs.populateJasaRacik());
				Common.selectComboItem(jasaRacik, transaksiDetail.getTindakan());
				if (jasaRacik.getSelectedItem() == null) {
					try {
						jasaRacik.setSelectedIndex(0);
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sirs/detail/TransaksiItemDetailHelper.java:242");
					}
				}
				jasaRacik.setWidth("90%");

				final EventListener eventListener = new EventListener() {

					@Override
					public void onEvent(Event event) throws Exception {

						Tindakan myJasaRacik = (Tindakan) (jasaRacik.getSelectedItem() == null ? null
								: jasaRacik.getSelectedItem().getValue());

						if (myJasaRacik == null) {
							return;
						}

						Double myJumlah = jumlah.getValue() == null ? 0.0 : jumlah.getValue();
						KelasPerawatan mykelasPerawatan = dataParent.getKelasPerawatan();

						Session session = HibernateUtil.currentSession();
						BiayaTindakanPerKelas biayaTindakanPerKelas = CommonTarifTindakan
								.getBiayaTindakanPerKelas(myJasaRacik, mykelasPerawatan);

						Double myhargaRacik = biayaTindakanPerKelas.getBiaya();

						Double myHarga = transaksiDetail.getAmount() == null ? 0.0 : transaksiDetail.getAmount();
						harga.setValue(Common.numberFormat.get().format(myJumlah * myhargaRacik) + " + "
								+ Common.numberFormat.get().format(myJumlah * myHarga));
						Double myTotal = (myJumlah * myhargaRacik) + (transaksiDetail.getHasilPenghitunganTotal());
						total.setValue(Common.numberFormat.get().format(myTotal));

						transaksiDetail.setTindakan(myJasaRacik);
						transaksiDetail.setAmountJasa(myhargaRacik);
						transaksiDetail.setQty(myJumlah);
						session.update(transaksiDetail);
						dataParent.getSimpan().setDisabled(false);
						dataParent.getAdd().setDisabled(true);
						loadTotal();

						diskon.setValue(Common.numberFormat.get().format(transaksiDetail.getDiskon() * myJumlah));

						pajak.setValue(Common.numberFormat.get().format(transaksiDetail.getPajak() * myJumlah));
					}
				};

				jasaRacik.addEventListener("onChange", eventListener);

				final KelasPerawatan kelasPerawatan = dataParent.getKelasPerawatan();

				final EventListener racikanEventListener = new EventListener() {

					@Override
					public void onEvent(Event event) throws Exception {
						TransaksiMedisDetail myTransaksiDetail = (TransaksiMedisDetail) event.getData();
						transaksiDetail.setAmount(myTransaksiDetail.getAmount());

						eventListener.onEvent(event);
					}
				};

				final TransaksiRacikanDetailAction transaksiRacikanDetailAction;
				(transaksiRacikanDetailAction = new TransaksiRacikanDetailAction(transaksiDetail.getRacikan(),
						transaksiDetail, kelasPerawatan, racikanEventListener)).setParent(row);

				final AmbilDataDokterBanbox dokter = new AmbilDataDokterBanbox();
				dokter.setAttribute("dokter", transaksiDetail.getDokter());
				dokter.setValue(transaksiDetail.getDokter() == null ? "" : transaksiDetail.getDokter().getNama());
				dokter.setReadonly(true);
				dokter.setParent(row);
				dokter.setWidth("95%");

				dokter.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						transaksiRacikanDetailAction.reloadDokter((Dokter) dokter.getAttribute("dokter"));
					}
				});

				RevisiHelper
						.createNewRevisi(TransaksiMedisDetail.class, transaksiDetail,
								transaksiDetail.getRacikan() == null ? "" : transaksiDetail.getRacikan().getNama())
						.setParent(row);

				jumlah.setParent(row);

				jasaRacik.setParent(row);

				jumlah.setStyle("text-align:right");
				jumlah.setWidth("90%");
				jumlah.addEventListener(Events.ON_CHANGE, eventListener);

				harga.setParent(row);

				diskon.setParent(row);
				pajak.setParent(row);

				total.setParent(row);
				eventListener.onEvent(null);

			}

			Hbox toolbar = new Hbox();

			Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig("", "/img/delete.gif");

			button.setTooltiptext("Hapus Data");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah Bapak/Ibu yakin ingin menghapus data ini? Perlu diketahui, data yang telah dihapus tidak dapat dikembalikan.", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = new Integer(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										Session session = null;
										try {

											session = HibernateUtil.getSessionFactory().openSession();
											session.getTransaction().begin();

											session.createSQLQuery(
													"update sirs.transaksi_medis_detail set racikan = null where id = "
															+ transaksiDetail.getId() + ";")
													.executeUpdate();

											session.createSQLQuery(
													"delete from sirs.detail_transaksi_pasien where racikan_detail in (select id from sirs.racikan_detail where racikan in (select id from sirs.racikan where transaksi_detail = "
															+ transaksiDetail.getId() + "));")
													.executeUpdate();

											session.createSQLQuery(
													"delete from racikan_detail where racikan in (select id from sirs.racikan where transaksi_detail = "
															+ transaksiDetail.getId() + ");")
													.executeUpdate();

											session.createSQLQuery("delete from sirs.racikan where transaksi_detail = "
													+ transaksiDetail.getId() + ";").executeUpdate();

											session.createSQLQuery("delete from sirs.transaksi_medis_detail where id = "
													+ transaksiDetail.getId() + ";").executeUpdate();

											session.getTransaction().commit();
											// session.disconnect();
											if (session.isOpen()) {session.disconnect();session.close();}

											loadData(null);

										} catch (Exception e) {
											e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sirs/detail/TransaksiItemDetailHelper.java:394");
											MyMessageboxConfig.show(Common.pesan(
																"Mohon maaf, data ini tidak dapat dihapus karena masih berelasi dengan data lainnya. Rincian teknis kesalahan: {V1}. Langkah yang dapat dilakukan: (1) pastikan tidak ada data lain yang masih terkait dengan data ini; (2) hapus terlebih dahulu seluruh data yang berelasi; (3) apabila kendala masih berlanjut, mohon hubungi administrator sistem.",
																	e.getMessage()));
										} finally {
											// FIX bocor: session dedikasi (openSession) dulu ditutup di jalur normal saja;
											// finally menjamin penutupan walau exception (idempoten via isOpen()).
											if (session != null && session.isOpen()) {
												try { session.disconnect(); } catch (Exception eSf) { ais.common.ErrorAuditUtil.record(eSf, "auto-audit(empty-catch) src/ais/action/master/sirs/detail/TransaksiItemDetailHelper.java:402");}
												try { session.close(); } catch (Exception eSf) { ais.common.ErrorAuditUtil.record(eSf, "auto-audit(empty-catch) src/ais/action/master/sirs/detail/TransaksiItemDetailHelper.java:403");}
											}
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
		List<TransaksiMedisDetail> transaksiDetails = dataParent.getTransaksi() == null
				|| dataParent.getTransaksi().getId() == null
						? new ArrayList<TransaksiMedisDetail>()
						: session.createCriteria(TransaksiMedisDetail.class).addOrder(Order.desc("id"))
								.add(Restrictions.or(Restrictions.isNotNull("racikan"), Restrictions.isNotNull("item")))
								.add(Restrictions.eq("transaksi", dataParent.getTransaksi())).list();

		ListModel strset = new SimpleListModel(transaksiDetails);
		grid.setRowRenderer(new TransaksiDetailRenderer());
		grid.setModel(strset);
		grid.renderAll();
		if (dataParent.getResep() != null) {
			dataParent.getResep().setDisabled(dataParent.getTransaksi().getId() != null);
		}
		loadTotal();
	}

	@SuppressWarnings("unchecked")
	public void loadTotal() {
		Session session = HibernateUtil.currentSession();
		Double mytotal = 0.0;
		Double myhrg = 0.0;
		Double mydiskon = 0.0;
		Double mypajak = 0.0;

		List<TransaksiMedisDetail> transaksiDetails = dataParent.getTransaksi() == null
				|| dataParent.getTransaksi().getId() == null
						? new ArrayList<TransaksiMedisDetail>()
						: session.createCriteria(TransaksiMedisDetail.class).addOrder(Order.desc("id"))
								.add(Restrictions.or(Restrictions.isNotNull("racikan"), Restrictions.isNotNull("item")))
								.add(Restrictions.eq("transaksi", dataParent.getTransaksi())).list();
		for (TransaksiMedisDetail transaksiDetail : transaksiDetails) {
			mytotal += transaksiDetail.getQty() == null ? 0.0 : transaksiDetail.getQty();

			mydiskon += transaksiDetail.getDiskon() * transaksiDetail.getQty();
			mypajak += transaksiDetail.getPajak() * transaksiDetail.getQty();

			if (transaksiDetail.getRacikan() == null) {
				myhrg += transaksiDetail.getHasilPenghitunganTotal();

			} else {
				myhrg += ((transaksiDetail.getQty() == null ? 0.0 : transaksiDetail.getQty())
						* (transaksiDetail.getAmountJasa() == null ? 0.0 : transaksiDetail.getAmountJasa()))
						+ transaksiDetail.getHasilPenghitunganTotal();
			}
		}

		totalDiskon.setLabel(mydiskon == null ? "0.0" : Common.numberFormat.get().format(mydiskon));
		totalPajak.setLabel(mypajak == null ? "0.0" : Common.numberFormat.get().format(mypajak));

		total.setLabel(mytotal == null ? "0.0" : Common.numberFormat.get().format(mytotal));
		totalHrg.setLabel(myhrg == null ? "0.0" : Common.numberFormat.get().format(myhrg));
	}

	private void display() throws Exception {

		setHeight("100%");
		setWidth("100%");
		setStyle("border:0px;background: transparent;");

		Center center = new Center();
		center.setParent(this);
		ais.ui.util.ZkCompat.setFlex(center, true);

		North north = new North();
		north.setParent(this);
		ais.ui.util.ZkCompat.setFlex(north, true);

		Div div = new Div();
		div.setParent(north);

		Grid searchgrid = new Grid();
		searchgrid.setParent(div);

		Columns columns = new Columns();
		columns.setParent(searchgrid);

		Column column = new Column();
		column.setParent(columns);
		column.setWidth("25%");

		column = new Column();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(searchgrid);

		Row

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Barcode Item")));
		row.appendChild(barcode = new MyTextbox());
		barcode.setWidth("90%");
		barcode.setStyle("font-weight:bold;font-size:18px;");
		barcode.addEventListener("onOK", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				if (dataParent.getLokasi() == null) {
					MyMessageboxConfig.show("Mohon Bapak/Ibu memilih salah satu lokasi terlebih dahulu. Langkah yang dapat dilakukan: (1) buka daftar lokasi; (2) pilih salah satu lokasi yang sesuai; (3) ulangi kembali proses ini.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					barcode.select();
					barcode.focus();
					return;
				}

				final KelasPerawatan kelasPerawatan = dataParent.getKelasPerawatan();

				if (kelasPerawatan == null) {
					MyMessageboxConfig.show("Mohon Bapak/Ibu memilih salah satu kelas perawatan terlebih dahulu. Langkah yang dapat dilakukan: (1) buka daftar kelas perawatan; (2) pilih salah satu kelas perawatan yang sesuai; (3) ulangi kembali proses ini.", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.EXCLAMATION);
					barcode.select();
					barcode.focus();
					return;
				}

				if (dataParent.getTransaksi().getId() == null) {
					if (!dataParent.onSave(arg0)) {
						return;
					}
				}

				if (barcode.getValue().trim().equals("")) {
					MyMessageboxConfig.show("Mohon Bapak/Ibu memasukkan barcode dengan benar. Langkah yang dapat dilakukan: (1) periksa kembali barcode yang dimasukkan; (2) pastikan tidak ada karakter atau spasi yang keliru; (3) ulangi kembali proses pemindaian barcode.", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.EXCLAMATION);
					barcode.select();
					barcode.focus();
					return;
				}
				Session session = HibernateUtil.currentSession();
				ItemMedis item = (ItemMedis) session.createCriteria(ItemMedis.class)
						.add(Restrictions.eq("barcode", barcode.getValue().trim())).setMaxResults(1).uniqueResult();
				if (item == null) {
					MyMessageboxConfig.showFormat("Mohon maaf, item dengan barcode \"{V1}\" tidak ditemukan. Langkah yang dapat dilakukan: (1) periksa kembali barcode yang dimasukkan; (2) pastikan item telah terdaftar di dalam sistem; (3) ulangi kembali proses pemindaian barcode.",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION, barcode.getValue().trim());
					barcode.select();
					barcode.focus();
					return;
				}

				Double diskon = CommonSirs.getTotalDiskonDalamPersen(item, null, null, 1,
						dataParent.getTransaksi().getTanggalTransaksi(),
						dataParent.getTransaksi().getPendaftaran() == null ? null
								: dataParent.getTransaksi().getPendaftaran().getAsuransi(),
						dataParent.getTransaksi().getPendaftaran() == null ? null
								: dataParent.getTransaksi().getPendaftaran().getKomunitass());
				Double pajak = CommonSirs.getTotalPajakDalamPersen(item, null, null,
						dataParent.getTransaksi().getPendaftaran() == null ? null
								: dataParent.getTransaksi().getPendaftaran().getAsuransi(),
						dataParent.getTransaksi().getPendaftaran() == null ? null
								: dataParent.getTransaksi().getPendaftaran().getKomunitass());

				TransaksiMedisDetail transaksiDetail = new TransaksiMedisDetail();
				transaksiDetail.setDiskonPersen(diskon);
				transaksiDetail.setPajakPersen(pajak);
				transaksiDetail.getPajaks()
						.addAll(CommonSirs.getPajakSekarang(item, null, null,
								dataParent.getTransaksi().getPendaftaran() == null ? null
										: dataParent.getTransaksi().getPendaftaran().getAsuransi(),
								dataParent.getTransaksi().getPendaftaran() == null ? null
										: dataParent.getTransaksi().getPendaftaran().getKomunitass()));
				transaksiDetail.getDiskons()
						.addAll(CommonSirs.getDiskonSekarang(item, null, null, 1,
								dataParent.getTransaksi().getTanggalTransaksi(),
								dataParent.getTransaksi().getPendaftaran() == null ? null
										: dataParent.getTransaksi().getPendaftaran().getAsuransi(),
								dataParent.getTransaksi().getPendaftaran() == null ? null
										: dataParent.getTransaksi().getPendaftaran().getKomunitass()));

				if (dataParent.getTransaksi() != null && dataParent.getTransaksi().getPendaftaran() != null) {
					DiagnosaPenyakit diagnosaPenyakit = (DiagnosaPenyakit) session
							.createCriteria(DiagnosaPenyakit.class)
							.add(Restrictions.eq("pendaftaran", dataParent.getTransaksi().getPendaftaran()))
							.addOrder(Order.desc("id")).setMaxResults(1).uniqueResult();
					if (diagnosaPenyakit != null && diagnosaPenyakit.getDokter() != null) {
						transaksiDetail.setDokter(diagnosaPenyakit.getDokter());
					} else {
						transaksiDetail.setDokter(dataParent.getTransaksi().getPendaftaran().getDokter());
					}
				}

				HargaJualItem hargaJualItem = CommonTarifItem.getHargaJualItem(item, kelasPerawatan,
						transaksiDetail.getDokter(), dataParent.getTransaksi().getPendaftaran().getAsuransi(),
						dataParent.getTransaksi().getPendaftaran().getKomunitass(),
						dataParent.getTransaksi().getPendaftaran().getPasien());

				transaksiDetail.setAmount(hargaJualItem.getHargaJual());
				transaksiDetail.setMenggunakanAmountCustom(hargaJualItem.getHargaBisaDirubahSaatTransaksi()
						&& hargaJualItem.getPembagianBiayaDalamPersen());

				transaksiDetail.setAmount(hargaJualItem.getHargaJual());
				transaksiDetail.setAmountCustom(hargaJualItem.getHargaJual());
				transaksiDetail.setItem(item);
				transaksiDetail.setQty(1.0);
				transaksiDetail.setKeterangan("Transaksi penjualan di lokasi " + dataParent.getLokasi().getNama());
				transaksiDetail.setTransaksi(dataParent.getTransaksi());
				session.save(transaksiDetail);

				loadData(null);
				barcode.select();
				barcode.focus();
				dataParent.getSimpan().setDisabled(false);
				dataParent.getAdd().setDisabled(true);
			}
		});

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("25px");
		toolbar.setParent(div);

		final Toolbarbutton ambilDataItem = new ais.ui.util.MyToolbarbuttonConfig("Ambil Obat", "/img/add_item.png");

		ambilDataItem.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {

				if (dataParent.getLokasi() == null) {
					MyMessageboxConfig.show("Mohon Bapak/Ibu memilih salah satu lokasi terlebih dahulu. Langkah yang dapat dilakukan: (1) buka daftar lokasi; (2) pilih salah satu lokasi yang sesuai; (3) ulangi kembali proses ini.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return;
				}
				Session session = HibernateUtil.currentSession();

				List<ItemMedis> items = dataParent.getTransaksi() == null || dataParent.getTransaksi().getId() == null
						? new ArrayList<ItemMedis>()
						: session.createCriteria(TransaksiMedisDetail.class).setProjection(Projections.groupProperty("item"))
								.add(Restrictions.isNotNull("item"))
								.add(Restrictions.eq("transaksi", dataParent.getTransaksi())).list();

				AmbilDataItemMedisBanyakBerdasarkanStok ambilDataItemBanyak = new AmbilDataItemMedisBanyakBerdasarkanStok(
						items, dataParent.getLokasi(), "nama_item", true);
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(ambilDataItemBanyak);
				ambilDataItemBanyak.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						final KelasPerawatan kelasPerawatan = dataParent.getKelasPerawatan();

						if (kelasPerawatan == null) {
							MyMessageboxConfig.show("Mohon Bapak/Ibu memilih salah satu kelas perawatan terlebih dahulu. Langkah yang dapat dilakukan: (1) buka daftar kelas perawatan; (2) pilih salah satu kelas perawatan yang sesuai; (3) ulangi kembali proses ini.", "Peringatan", MyMessageboxConfig.OK,
									MyMessageboxConfig.EXCLAMATION);
							return;
						}

						if (dataParent.getTransaksi().getId() == null) {
							if (!dataParent.onSave(arg0)) {
								return;
							}
						}

						List<ItemMedis> items = (List<ItemMedis>) arg0.getData();
						dataParent.getSimpan().setDisabled(items.size() == 0);
						dataParent.getAdd().setDisabled(items.size() != 0);
						Session session = HibernateUtil.currentSession();
						for (ItemMedis item : items) {
							Double diskon = CommonSirs.getTotalDiskonDalamPersen(item, null, null, 1,
									dataParent.getTransaksi().getTanggalTransaksi(),
									dataParent.getTransaksi().getPendaftaran() == null ? null
											: dataParent.getTransaksi().getPendaftaran().getAsuransi(),
									dataParent.getTransaksi().getPendaftaran() == null ? null
											: dataParent.getTransaksi().getPendaftaran().getKomunitass());
							Double pajak = CommonSirs.getTotalPajakDalamPersen(item, null, null,
									dataParent.getTransaksi().getPendaftaran() == null ? null
											: dataParent.getTransaksi().getPendaftaran().getAsuransi(),
									dataParent.getTransaksi().getPendaftaran() == null ? null
											: dataParent.getTransaksi().getPendaftaran().getKomunitass());

							TransaksiMedisDetail transaksiDetail = new TransaksiMedisDetail();
							transaksiDetail.setDiskonPersen(diskon);
							transaksiDetail.setPajakPersen(pajak);
							transaksiDetail.getPajaks()
									.addAll(CommonSirs.getPajakSekarang(item, null, null,
											dataParent.getTransaksi().getPendaftaran() == null ? null
													: dataParent.getTransaksi().getPendaftaran().getAsuransi(),
											dataParent.getTransaksi().getPendaftaran() == null ? null
													: dataParent.getTransaksi().getPendaftaran().getKomunitass()));
							transaksiDetail.getDiskons()
									.addAll(CommonSirs.getDiskonSekarang(item, null, null, 1,
											dataParent.getTransaksi().getTanggalTransaksi(),
											dataParent.getTransaksi().getPendaftaran() == null ? null
													: dataParent.getTransaksi().getPendaftaran().getAsuransi(),
											dataParent.getTransaksi().getPendaftaran() == null ? null
													: dataParent.getTransaksi().getPendaftaran().getKomunitass()));

							if (dataParent.getTransaksi() != null
									&& dataParent.getTransaksi().getPendaftaran() != null) {

								DiagnosaPenyakit diagnosaPenyakit = (DiagnosaPenyakit) session
										.createCriteria(DiagnosaPenyakit.class)
										.add(Restrictions.eq("pendaftaran", dataParent.getTransaksi().getPendaftaran()))
										.addOrder(Order.desc("id")).setMaxResults(1).uniqueResult();
								if (diagnosaPenyakit != null && diagnosaPenyakit.getDokter() != null) {
									transaksiDetail.setDokter(diagnosaPenyakit.getDokter());
								} else {
									transaksiDetail.setDokter(dataParent.getTransaksi().getPendaftaran().getDokter());
								}
							}

							HargaJualItem hargaJualItem = CommonTarifItem.getHargaJualItem(item, kelasPerawatan,
									transaksiDetail.getDokter(),
									dataParent.getTransaksi().getPendaftaran().getAsuransi(),
									dataParent.getTransaksi().getPendaftaran().getKomunitass(),
									dataParent.getTransaksi().getPendaftaran().getPasien());

							transaksiDetail.setMenggunakanAmountCustom(hargaJualItem.getHargaBisaDirubahSaatTransaksi()
									&& hargaJualItem.getPembagianBiayaDalamPersen());

							transaksiDetail.setAmount(hargaJualItem.getHargaJual());
							transaksiDetail.setAmountCustom(hargaJualItem.getHargaJual());
							transaksiDetail.setItem(item);

							transaksiDetail.setQty(1.0);
							transaksiDetail
									.setKeterangan("Transaksi penjualan di lokasi " + dataParent.getLokasi().getNama());
							transaksiDetail.setTransaksi(dataParent.getTransaksi());
							session.save(transaksiDetail);
						}

						loadData(null);
					}
				});
				ambilDataItemBanyak.setWidth("95%");
				ambilDataItemBanyak.setHeight("97%");
				ambilDataItemBanyak.setVisible(true);
				ambilDataItemBanyak.onModal();
			}

		});
		ambilDataItem.setParent(toolbar);

		final Toolbarbutton buatRacikan = new ais.ui.util.MyToolbarbuttonConfig("Ambil Racikan", "/img/Drug-basket-icon.png");

		buatRacikan.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event arg0) throws Exception {

				if (dataParent.getLokasi() == null) {
					MyMessageboxConfig.show("Mohon Bapak/Ibu memilih salah satu lokasi terlebih dahulu. Langkah yang dapat dilakukan: (1) buka daftar lokasi; (2) pilih salah satu lokasi yang sesuai; (3) ulangi kembali proses ini.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return;
				}
				Session session = HibernateUtil.currentSession();

				List<Racikan> racikans = dataParent.getTransaksi() == null || dataParent.getTransaksi().getId() == null
						? new ArrayList<Racikan>()
						: session.createCriteria(TransaksiMedisDetail.class)
								.setProjection(Projections.groupProperty("racikan"))
								.add(Restrictions.isNotNull("racikan"))
								.add(Restrictions.eq("transaksi", dataParent.getTransaksi())).list();
				AmbilDataRacikanBanyak ambilDataRacikanBanyak = new AmbilDataRacikanBanyak(racikans);
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(ambilDataRacikanBanyak);
				ambilDataRacikanBanyak.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						final KelasPerawatan kelasPerawatan = dataParent.getKelasPerawatan();

						if (kelasPerawatan == null) {
							MyMessageboxConfig.show("Mohon Bapak/Ibu memilih salah satu kelas perawatan terlebih dahulu. Langkah yang dapat dilakukan: (1) buka daftar kelas perawatan; (2) pilih salah satu kelas perawatan yang sesuai; (3) ulangi kembali proses ini.", "Peringatan", MyMessageboxConfig.OK,
									MyMessageboxConfig.EXCLAMATION);
							return;
						}

						if (dataParent.getTransaksi().getId() == null) {
							if (!dataParent.onSave(arg0)) {
								return;
							}
						}

						List<Racikan> racikans = (List<Racikan>) arg0.getData();
						dataParent.getSimpan().setDisabled(racikans.size() == 0);
						dataParent.getAdd().setDisabled(racikans.size() != 0);
						Session session = HibernateUtil.currentSession();
						for (Racikan racikan : racikans) {

							TransaksiMedisDetail transaksiDetail = new TransaksiMedisDetail();

							if (dataParent.getTransaksi() != null
									&& dataParent.getTransaksi().getPendaftaran() != null) {

								DiagnosaPenyakit diagnosaPenyakit = (DiagnosaPenyakit) session
										.createCriteria(DiagnosaPenyakit.class)
										.add(Restrictions.eq("pendaftaran", dataParent.getTransaksi().getPendaftaran()))
										.addOrder(Order.desc("id")).setMaxResults(1).uniqueResult();
								if (diagnosaPenyakit != null && diagnosaPenyakit.getDokter() != null) {
									transaksiDetail.setDokter(diagnosaPenyakit.getDokter());
								} else {
									transaksiDetail.setDokter(dataParent.getTransaksi().getPendaftaran().getDokter());
								}
							}

							transaksiDetail.setAmount(CommonSirs.hitungHargaJualRacikan(racikan, kelasPerawatan,
									transaksiDetail.getDokter(),
									dataParent.getTransaksi().getPendaftaran().getAsuransi(),
									dataParent.getTransaksi().getPendaftaran().getKomunitass(),
									dataParent.getTransaksi().getPendaftaran().getPasien()));
							transaksiDetail.setAmountCustom(transaksiDetail.getAmount());
							transaksiDetail.setDiskon(CommonSirs.hitungDiskonRacikan(racikan, kelasPerawatan,
									dataParent.getTransaksi().getTanggalTransaksi(), transaksiDetail.getDokter(),
									dataParent.getTransaksi().getPendaftaran() == null ? null
											: dataParent.getTransaksi().getPendaftaran().getAsuransi(),
									dataParent.getTransaksi().getPendaftaran() == null ? null
											: dataParent.getTransaksi().getPendaftaran().getKomunitass(),
									dataParent.getTransaksi().getPendaftaran().getPasien()));
							transaksiDetail.setPajak(
									CommonSirs.hitungPajakRacikan(racikan, kelasPerawatan, transaksiDetail.getDokter(),
											dataParent.getTransaksi().getPendaftaran() == null ? null
													: dataParent.getTransaksi().getPendaftaran().getAsuransi(),
											dataParent.getTransaksi().getPendaftaran() == null ? null
													: dataParent.getTransaksi().getPendaftaran().getKomunitass(),
											dataParent.getTransaksi().getPendaftaran().getPasien()));

							transaksiDetail.setRacikan(racikan);
							transaksiDetail.setQty(1.0);
							transaksiDetail.setKeterangan(
									"Transaksi penjualan racikan di lokasi " + dataParent.getLokasi().getNama());
							transaksiDetail.setTransaksi(dataParent.getTransaksi());
							session.save(transaksiDetail);
						}

						loadData(null);
					}
				});
				ambilDataRacikanBanyak.setWidth("95%");
				ambilDataRacikanBanyak.setHeight("97%");
				ambilDataRacikanBanyak.setVisible(true);
				ambilDataRacikanBanyak.onModal();
			}
		});
		buatRacikan.setParent(toolbar);

		Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig("Racikan Baru", "/img/add_item.png");
		button.setParent(toolbar);
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {

				final KelasPerawatan kelasPerawatan = dataParent.getKelasPerawatan();

				if (kelasPerawatan == null) {
					MyMessageboxConfig.show("Mohon Bapak/Ibu memilih salah satu kelas perawatan terlebih dahulu. Langkah yang dapat dilakukan: (1) buka daftar kelas perawatan; (2) pilih salah satu kelas perawatan yang sesuai; (3) ulangi kembali proses ini.", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.EXCLAMATION);
					return;
				}

				BuatRacikanBaruHelper buatRacikanBaruHelper = new BuatRacikanBaruHelper(new EventListener() {

					@Override
					public void onEvent(Event event) throws Exception {
						Racikan racikan = (Racikan) event.getData();
						if (racikan != null) {

							if (dataParent.getTransaksi().getId() == null) {
								if (!dataParent.onSave(event)) {
									return;
								}
							}

							Session session = HibernateUtil.currentSession();

							TransaksiMedisDetail transaksiDetail = new TransaksiMedisDetail();

							if (dataParent.getTransaksi() != null
									&& dataParent.getTransaksi().getPendaftaran() != null) {
								DiagnosaPenyakit diagnosaPenyakit = (DiagnosaPenyakit) session
										.createCriteria(DiagnosaPenyakit.class)
										.add(Restrictions.eq("pendaftaran", dataParent.getTransaksi().getPendaftaran()))
										.addOrder(Order.desc("id")).setMaxResults(1).uniqueResult();
								if (diagnosaPenyakit != null && diagnosaPenyakit.getDokter() != null) {
									transaksiDetail.setDokter(diagnosaPenyakit.getDokter());
								} else {
									transaksiDetail.setDokter(dataParent.getTransaksi().getPendaftaran().getDokter());
								}
							}

							transaksiDetail.setAmount(CommonSirs.hitungHargaJualRacikan(racikan, kelasPerawatan,
									transaksiDetail.getDokter(),
									dataParent.getTransaksi().getPendaftaran().getAsuransi(),
									dataParent.getTransaksi().getPendaftaran().getKomunitass(),
									dataParent.getTransaksi().getPendaftaran().getPasien()));
							transaksiDetail.setAmountCustom(transaksiDetail.getAmount());
							transaksiDetail.setDiskon(CommonSirs.hitungDiskonRacikan(racikan, kelasPerawatan,
									dataParent.getTransaksi().getTanggalTransaksi(), transaksiDetail.getDokter(),
									dataParent.getTransaksi().getPendaftaran() == null ? null
											: dataParent.getTransaksi().getPendaftaran().getAsuransi(),
									dataParent.getTransaksi().getPendaftaran() == null ? null
											: dataParent.getTransaksi().getPendaftaran().getKomunitass(),
									dataParent.getTransaksi().getPendaftaran().getPasien()));
							transaksiDetail.setPajak(
									CommonSirs.hitungPajakRacikan(racikan, kelasPerawatan, transaksiDetail.getDokter(),
											dataParent.getTransaksi().getPendaftaran() == null ? null
													: dataParent.getTransaksi().getPendaftaran().getAsuransi(),
											dataParent.getTransaksi().getPendaftaran() == null ? null
													: dataParent.getTransaksi().getPendaftaran().getKomunitass(),
											dataParent.getTransaksi().getPendaftaran().getPasien()));

							transaksiDetail.setRacikan(racikan);
							transaksiDetail.setQty(1.0);
							transaksiDetail.setKeterangan(
									"Transaksi penjualan racikan di lokasi " + dataParent.getLokasi().getNama());
							transaksiDetail.setTransaksi(dataParent.getTransaksi());
							session.save(transaksiDetail);

							loadData(null);
						}
					}
				});

				buatRacikanBaruHelper.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				buatRacikanBaruHelper.setHeight("95%");
				buatRacikanBaruHelper.setWidth("700px");
				buatRacikanBaruHelper.onModal();
			}
		});

		grid = new Grid();
		grid.setMold("paging");
		grid.setPageSize(25);
		grid.setParent(center);

		columns = new Columns();

		columns.setParent(grid);

		column = new Column();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("40px");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Dokter/Bidan");
		column.setWidth("15%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Item");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Qty");
		column.setAlign("right");
		column.setWidth("8%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Satuan");
		column.setWidth("10%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Hrg");
		column.setAlign("right");
		column.setWidth("10%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Diskon");
		column.setAlign("right");
		column.setWidth("10%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Pajak");
		column.setAlign("right");
		column.setWidth("10%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Ttl");
		column.setAlign("right");
		column.setWidth("10%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Hapus");
		column.setWidth("4%");

		Foot foot = new Foot();
		foot.setParent(grid);
		foot.appendChild(new Footer());
		foot.appendChild(new Footer());

		foot.appendChild(new Footer("Total"));

		total = new Footer();
		total.setParent(foot);
		total.setStyle("font-weight:bold;font-size:15px;text-align:right;");

		foot.appendChild(new Footer());
		foot.appendChild(new Footer());

		totalDiskon = new Footer();
		totalDiskon.setParent(foot);
		totalDiskon.setStyle("font-weight:bold;font-size:15px;text-align:right;");

		totalPajak = new Footer();
		totalPajak.setParent(foot);
		totalPajak.setStyle("font-weight:bold;font-size:15px;text-align:right;");

		totalHrg = new Footer();
		totalHrg.setParent(foot);
		totalHrg.setStyle("font-weight:bold;font-size:15px;text-align:right;");

		foot.appendChild(new Footer());

		loadData(null);
		// resepEventListener.onEvent(null);
	}
}