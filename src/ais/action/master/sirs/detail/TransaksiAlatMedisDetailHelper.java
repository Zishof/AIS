package ais.action.master.sirs.detail;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
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
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.sirs.helper.AmbilDataAlatMedisBanyak;
import ais.action.master.sirs.helper.AmbilDataDokterBanbox;
import ais.action.master.sirs.util.CommonTarifAlatMedis;
import ais.common.Common;
import ais.common.CommonSirs;
import ais.common.listener.GetTransaksi;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.sirs.AlatMedis;
import ais.database.model.sirs.AlatMedisDiagnosaPenyakit;
import ais.database.model.sirs.BiayaAlatMedisPerKelas;
import ais.database.model.sirs.DiagnosaPenyakit;
import ais.database.model.sirs.Diskon;
import ais.database.model.sirs.Dokter;
import ais.database.model.sirs.KelasPerawatan;
import ais.database.model.sirs.Pendaftaran;
import ais.database.model.sirs.TempatTidur;
import ais.database.model.sirs.TransaksiMedis;
import ais.database.model.sirs.TransaksiMedisDetail;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDoublebox;

public class TransaksiAlatMedisDetailHelper extends Borderlayout {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5086031585928643232L;

	private Grid grid;

	private Footer total;
	private Footer totalHrg;

	private Footer totalDiskon;

	private Footer totalPajak;

	private GetTransaksi dataParent;

	public TransaksiAlatMedisDetailHelper(GetTransaksi getTransaksi) throws Exception {
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

			final AmbilDataDokterBanbox dokter = new AmbilDataDokterBanbox();
			dokter.setAttribute("dokter", transaksiDetail.getDokter());
			dokter.setValue(transaksiDetail.getDokter() == null ? "" : transaksiDetail.getDokter().getNama());
			dokter.setReadonly(true);
			dokter.setParent(row);
			dokter.setWidth("95%");

			final Label diskon;
			final Label pajak;

			diskon = new Label(Common.numberFormat.get().format(transaksiDetail.getDiskonPersen()) + "% ("
					+ Common.numberFormat.get().format(transaksiDetail.getDiskon()) + ")");
			pajak = new Label(Common.numberFormat.get().format(transaksiDetail.getPajakPersen()) + "% ("
					+ Common.numberFormat.get().format(transaksiDetail.getPajak()) + ")");

			final MyDatebox mulai = new MyDatebox(transaksiDetail.getMulai());
			final MyDatebox sampai = new MyDatebox(transaksiDetail.getSampai());

			final Label jumlah;
			jumlah = new Label(
					Common.numberFormat.get().format(transaksiDetail.getQty() == null ? 0.0 : transaksiDetail.getQty()));

			final MyDoublebox harga;
			harga = new MyDoublebox(transaksiDetail.getMenggunakanAmountCustom() ? transaksiDetail.getAmountCustom()
					: transaksiDetail.getAmount());

			final Label total = new Label();

			final EventListener eventListener = new EventListener() {

				@Override
				public void onEvent(Event event) throws Exception {

					final KelasPerawatan kelasPerawatan = dataParent.getKelasPerawatan();

					if (kelasPerawatan == null) {
						MyMessageboxConfig.show("Mohon Bapak/Ibu memilih salah satu kelas perawatan terlebih dahulu. Langkah yang dapat dilakukan: (1) buka daftar kelas perawatan; (2) pilih salah satu kelas perawatan yang sesuai; (3) ulangi kembali proses ini.", "Peringatan", MyMessageboxConfig.OK,
								MyMessageboxConfig.EXCLAMATION);
						return;
					}

					transaksiDetail.setMulai(mulai.getValue());
					transaksiDetail.setSampai(sampai.getValue());

					Double myJumlah = transaksiDetail.getQty();

					transaksiDetail.setDokter((Dokter) dokter.getAttribute("dokter"));

					BiayaAlatMedisPerKelas biayaAlatMedisPerKelas = CommonTarifAlatMedis.getBiayaAlatMedisPerKelas(
							transaksiDetail.getAlatMedis(), kelasPerawatan, transaksiDetail.getDokter(),
							dataParent.getTransaksi().getPendaftaran().getAsuransi(),
							dataParent.getTransaksi().getPendaftaran().getKomunitass(),
							dataParent.getTransaksi().getPendaftaran().getPasien());

					transaksiDetail.setAmount(biayaAlatMedisPerKelas.getBiaya());

					if (!transaksiDetail.getMenggunakanAmountCustom()) {
						harga.setValue(transaksiDetail.getAmount());
					}

					transaksiDetail.setMenggunakanAmountCustom(biayaAlatMedisPerKelas.getHargaBisaDirubahSaatTransaksi()
							&& biayaAlatMedisPerKelas.getPembagianBiayaDalamPersen());

					harga.setReadonly(!transaksiDetail.getMenggunakanAmountCustom());

					transaksiDetail.setDiskonPersen(
							CommonSirs.getTotalDiskonDalamPersen(null, null, transaksiDetail.getAlatMedis(),
									myJumlah.intValue(), dataParent.getTransaksi().getTanggalTransaksi(),
									dataParent.getTransaksi().getPendaftaran() == null ? null
											: dataParent.getTransaksi().getPendaftaran().getAsuransi(),
									dataParent.getTransaksi().getPendaftaran() == null ? null
											: dataParent.getTransaksi().getPendaftaran().getKomunitass()));

					transaksiDetail.setDiskons(new HashSet<Diskon>());
					transaksiDetail.getDiskons()
							.addAll(CommonSirs.getDiskonSekarang(null, null, transaksiDetail.getAlatMedis(),
									myJumlah.intValue(), dataParent.getTransaksi().getTanggalTransaksi(),
									dataParent.getTransaksi().getPendaftaran() == null ? null
											: dataParent.getTransaksi().getPendaftaran().getAsuransi(),
									dataParent.getTransaksi().getPendaftaran() == null ? null
											: dataParent.getTransaksi().getPendaftaran().getKomunitass()));

					jumlah.setValue(Common.numberFormat.get()
							.format(transaksiDetail.getQty() == null ? 0.0 : transaksiDetail.getQty()));

					Session session = HibernateUtil.currentSession();
					transaksiDetail.setQty(myJumlah);
					session.update(transaksiDetail);
					dataParent.getSimpan().setDisabled(false);
					dataParent.getAdd().setDisabled(true);
					loadTotal();

					total.setValue(Common.numberFormat.get().format(transaksiDetail.getHasilPenghitunganTotal()));

					diskon.setValue(Common.numberFormat.get().format(transaksiDetail.getDiskonPersen()) + "% ("
							+ Common.numberFormat.get().format(transaksiDetail.getDiskon() * myJumlah) + ")");

					pajak.setValue(Common.numberFormat.get().format(transaksiDetail.getPajakPersen()) + "% ("
							+ Common.numberFormat.get().format(transaksiDetail.getPajak() * myJumlah) + ")");
				}
			};

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

			RevisiHelper
					.createNewRevisi(TransaksiMedisDetail.class, transaksiDetail,
							transaksiDetail.getAlatMedis() == null ? "" : transaksiDetail.getAlatMedis().getNama())
					.setParent(row);

			mulai.setParent(row);
			sampai.setParent(row);

			mulai.setWidth("90%");
			sampai.setWidth("90%");

			jumlah.setParent(row);
			new Label(transaksiDetail.getAlatMedis() == null || transaksiDetail.getAlatMedis().getPer() == null ? ""
					: transaksiDetail.getAlatMedis().getPer()).setParent(row);

			mulai.addEventListener(Events.ON_CHANGE, eventListener);
			sampai.addEventListener(Events.ON_CHANGE, eventListener);

			harga.setReadonly(!transaksiDetail.getMenggunakanAmountCustom());
			harga.setStyle("text-align:right");
			harga.setWidth("90%");
			harga.setParent(row);

			diskon.setParent(row);
			pajak.setParent(row);

			total.setValue(Common.numberFormat.get().format(transaksiDetail.getHasilPenghitunganTotal()));
			total.setParent(row);

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
													"delete from sirs.racikan_detail where racikan in (select id from sirs.racikan where transaksi_detail = "
															+ transaksiDetail.getId() + ");")
													.executeUpdate();

											session.createSQLQuery("delete from sirs.racikan where transaksi_detail = "
													+ transaksiDetail.getId() + ";").executeUpdate();

											session.createSQLQuery("delete from sirs.transaksi_medis_detail where id = "
													+ transaksiDetail.getId() + ";").executeUpdate();

											session.getTransaction().commit();
											// session.disconnect();
											if (session.isOpen()) {session.disconnect();session.close();}

											// TransaksiDetailDao
											// transaksiDetailDao =
											// DaoFactory
											// .getInstance()
											// .getTransaksiDetailDao();
											//
											// transaksiDetailDao
											// .delete(transaksiDetailDao
											// .merge(transaksiDetail));

											loadData(null);

										} catch (Exception e) {
											e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sirs/detail/TransaksiAlatMedisDetailHelper.java:299");
											MyMessageboxConfig.show(Common.pesan(
																"Mohon maaf, data ini tidak dapat dihapus karena masih berelasi dengan data lainnya. Rincian teknis kesalahan: {V1}. Langkah yang dapat dilakukan: (1) pastikan tidak ada data lain yang masih terkait dengan data ini; (2) hapus terlebih dahulu seluruh data yang berelasi; (3) apabila kendala masih berlanjut, mohon hubungi administrator sistem.",
																	e.getMessage()));
										} finally {
											// FIX bocor: session dedikasi (openSession) dulu ditutup di jalur normal saja;
											// finally menjamin penutupan walau exception (idempoten via isOpen()).
											if (session != null && session.isOpen()) {
												try { session.disconnect(); } catch (Exception eSf) { ais.common.ErrorAuditUtil.record(eSf, "auto-audit(empty-catch) src/ais/action/master/sirs/detail/TransaksiAlatMedisDetailHelper.java:307");}
												try { session.close(); } catch (Exception eSf) { ais.common.ErrorAuditUtil.record(eSf, "auto-audit(empty-catch) src/ais/action/master/sirs/detail/TransaksiAlatMedisDetailHelper.java:308");}
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
	public void loadData(Object value) throws Exception {
		Session session = HibernateUtil.currentSession();
		List<TransaksiMedisDetail> transaksiDetails = dataParent.getTransaksi() == null
				|| dataParent.getTransaksi().getId() == null
						? new ArrayList<TransaksiMedisDetail>()
						: session.createCriteria(TransaksiMedisDetail.class).addOrder(Order.desc("id"))
								.add(Restrictions.isNotNull("alatMedis"))
								.add(Restrictions.eq("transaksi", dataParent.getTransaksi())).list();

		System.out.println("transaksiDetails = " + transaksiDetails.size());

		if (transaksiDetails.isEmpty() && value != null && value instanceof Pendaftaran) {

			if (dataParent.onSave(null)) {

				Pendaftaran myPendaftaran = (Pendaftaran) value;

				Criterion cariAlatMedis = Restrictions.sqlRestriction("1=1");
				if (dataParent.getSumber().equals(TransaksiMedis.SUMBER_LAB)) {
					cariAlatMedis = Restrictions.eq("alatMedis.alatMedisLab", true);
				} else if (dataParent.getSumber().equals(TransaksiMedis.SUMBER_OPERASI)) {
					cariAlatMedis = Restrictions.eq("alatMedis.alatMedisOperasi", true);
				} else if (dataParent.getSumber().equals(TransaksiMedis.SUMBER_RADIOLOGI)) {
					cariAlatMedis = Restrictions.eq("alatMedis.alatMedisRadiologi", true);
				} else if (dataParent.getSumber().equals(TransaksiMedis.SUMBER_VK)) {
					cariAlatMedis = Restrictions.eq("alatMedis.alatMedisVk", true);
				} else if (dataParent.getSumber().equals(TransaksiMedis.SUMBER_RENAL_UNIT)) {
					cariAlatMedis = Restrictions.eq("alatMedis.alatMedisRenalUnit", true);
				} else if (dataParent.getSumber().equals(TransaksiMedis.SUMBER_GIZI)) {
					cariAlatMedis = Restrictions.eq("alatMedis.alatMedisGizi", true);
				}

				List<AlatMedisDiagnosaPenyakit> alatMedisDiagnosaPenyakits = HibernateUtil.currentSession()
						.createCriteria(AlatMedisDiagnosaPenyakit.class)
						.createAlias("diagnosaPenyakit", "diagnosaPenyakit").createAlias("alatMedis", "alatMedis")
						.add(Restrictions.eq("diagnosaPenyakit.pendaftaran", myPendaftaran)).add(cariAlatMedis).list();
				final KelasPerawatan kelasPerawatan = dataParent.getKelasPerawatan();

				for (AlatMedisDiagnosaPenyakit alatMedisDiagnosaPenyakit : alatMedisDiagnosaPenyakits) {
					AlatMedis alatMedis = alatMedisDiagnosaPenyakit.getAlatMedis();
					if (alatMedis != null && alatMedis.getId() != null) {

						BiayaAlatMedisPerKelas hargaJualItem = (BiayaAlatMedisPerKelas) session
								.createCriteria(BiayaAlatMedisPerKelas.class)
								.add(Restrictions.eq("alatMedis", alatMedis))
								.add(Restrictions.eq("kelasPerawatan", kelasPerawatan)).setMaxResults(1).uniqueResult();

						if (hargaJualItem == null) {
							hargaJualItem = new BiayaAlatMedisPerKelas();
							hargaJualItem.setBiaya(0.0);
							hargaJualItem.setAlatMedis(alatMedis);
							hargaJualItem.setKelasPerawatan(kelasPerawatan);
							session.save(hargaJualItem);
						}

						TransaksiMedisDetail transaksiDetail = (TransaksiMedisDetail) session
								.createCriteria(TransaksiMedisDetail.class)
								.add(Restrictions.eq("alatMedisDiagnosaPenyakit", alatMedisDiagnosaPenyakit))
								.add(Restrictions.eq("transaksi", dataParent.getTransaksi())).setMaxResults(1)
								.uniqueResult();
						if (transaksiDetail == null) {
							transaksiDetail = new TransaksiMedisDetail();
						}
						transaksiDetail.setAlatMedisDiagnosaPenyakit(alatMedisDiagnosaPenyakit);
						transaksiDetail.setAmount(hargaJualItem.getBiaya() == null ? 0.0 : hargaJualItem.getBiaya());
						transaksiDetail.setAlatMedis(alatMedis);
						transaksiDetail.setQty(1.0);
						transaksiDetail.setKeterangan("Transaksi layanan di lokasi "
								+ (dataParent.getLokasi() == null ? "" : dataParent.getLokasi().getNama()));
						transaksiDetail.setTransaksi(dataParent.getTransaksi());

						session.saveOrUpdate(transaksiDetail);
						transaksiDetails.add(transaksiDetail);
					}
				}

				dataParent.getSimpan().setDisabled(transaksiDetails.size() == 0);
				dataParent.getAdd().setDisabled(transaksiDetails.size() != 0);
			}
		}

		ListModel strset = new SimpleListModel(transaksiDetails);
		grid.setRowRenderer(new TransaksiDetailRenderer());
		grid.setModel(strset);
		grid.renderAll();

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
								.add(Restrictions.isNotNull("alatMedis"))
								.add(Restrictions.eq("transaksi", dataParent.getTransaksi())).list();
		for (TransaksiMedisDetail transaksiDetail : transaksiDetails) {
			mytotal += transaksiDetail.getQty() == null ? 0.0 : transaksiDetail.getQty();

			mydiskon += transaksiDetail.getDiskon() * transaksiDetail.getQty();
			mypajak += transaksiDetail.getPajak() * transaksiDetail.getQty();

			myhrg += transaksiDetail.getHasilPenghitunganTotal();
		}

		totalDiskon.setLabel(mydiskon == null ? "0.0" : Common.numberFormat.get().format(mydiskon));
		totalPajak.setLabel(mypajak == null ? "0.0" : Common.numberFormat.get().format(mypajak));

		total.setLabel(mytotal == null ? "0.0" : Common.numberFormat.get().format(mytotal));
		totalHrg.setLabel(myhrg == null ? "0.0" : Common.numberFormat.get().format(myhrg));
	}

	@SuppressWarnings("unchecked")
	private void ambilData(String jenis) throws Exception {

		if (dataParent.getLokasi() == null) {
			MyMessageboxConfig.show("Mohon Bapak/Ibu memilih salah satu lokasi terlebih dahulu. Langkah yang dapat dilakukan: (1) buka daftar lokasi; (2) pilih salah satu lokasi yang sesuai; (3) ulangi kembali proses ini.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);

			return;
		}
		Session session = HibernateUtil.currentSession();

		List<AlatMedis> alatMediss = dataParent.getTransaksi() == null || dataParent.getTransaksi().getId() == null
				? new ArrayList<AlatMedis>()
				: session.createCriteria(TransaksiMedisDetail.class).setProjection(Projections.groupProperty("alatMedis"))
						.add(Restrictions.isNotNull("alatMedis"))
						.add(Restrictions.eq("transaksi", dataParent.getTransaksi())).list();

		AmbilDataAlatMedisBanyak ambilDataItemBanyak;

		if (dataParent.getSumber().equals(TransaksiMedis.SUMBER_LAB)) {
			ambilDataItemBanyak = new AmbilDataAlatMedisBanyak(alatMediss, jenis, true, null, null, null, null, null);
		} else if (dataParent.getSumber().equals(TransaksiMedis.SUMBER_OPERASI)) {
			ambilDataItemBanyak = new AmbilDataAlatMedisBanyak(alatMediss, jenis, null, true, null, null, null, null);
		} else if (dataParent.getSumber().equals(TransaksiMedis.SUMBER_RADIOLOGI)) {
			ambilDataItemBanyak = new AmbilDataAlatMedisBanyak(alatMediss, jenis, null, null, true, null, null, null);
		} else if (dataParent.getSumber().equals(TransaksiMedis.SUMBER_VK)) {
			ambilDataItemBanyak = new AmbilDataAlatMedisBanyak(alatMediss, jenis, null, null, null, true, null, null);
		} else if (dataParent.getSumber().equals(TransaksiMedis.SUMBER_RENAL_UNIT)) {
			ambilDataItemBanyak = new AmbilDataAlatMedisBanyak(alatMediss, jenis, null, null, null, null, true, null);
		} else if (dataParent.getSumber().equals(TransaksiMedis.SUMBER_GIZI)) {
			ambilDataItemBanyak = new AmbilDataAlatMedisBanyak(alatMediss, jenis, null, null, null, null, null, true);
		} else {
			ambilDataItemBanyak = new AmbilDataAlatMedisBanyak(alatMediss, jenis);
		}

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

				List<AlatMedis> alatMediss = (List<AlatMedis>) arg0.getData();
				dataParent.getSimpan().setDisabled(alatMediss.size() == 0);
				dataParent.getAdd().setDisabled(alatMediss.size() != 0);
				Session session = HibernateUtil.currentSession();

				for (AlatMedis alatMedis : alatMediss) {

					Double diskon = CommonSirs.getTotalDiskonDalamPersen(null, null, alatMedis, 1,
							dataParent.getTransaksi().getTanggalTransaksi(),
							dataParent.getTransaksi().getPendaftaran() == null ? null
									: dataParent.getTransaksi().getPendaftaran().getAsuransi(),
							dataParent.getTransaksi().getPendaftaran() == null ? null
									: dataParent.getTransaksi().getPendaftaran().getKomunitass());
					Double pajak = CommonSirs.getTotalPajakDalamPersen(null, null, alatMedis,
							dataParent.getTransaksi().getPendaftaran() == null ? null
									: dataParent.getTransaksi().getPendaftaran().getAsuransi(),
							dataParent.getTransaksi().getPendaftaran() == null ? null
									: dataParent.getTransaksi().getPendaftaran().getKomunitass());

					TransaksiMedisDetail transaksiDetail = new TransaksiMedisDetail();
					transaksiDetail.setDiskonPersen(diskon);
					transaksiDetail.setPajakPersen(pajak);
					transaksiDetail.getPajaks()
							.addAll(CommonSirs.getPajakSekarang(null, null, alatMedis,
									dataParent.getTransaksi().getPendaftaran() == null ? null
											: dataParent.getTransaksi().getPendaftaran().getAsuransi(),
									dataParent.getTransaksi().getPendaftaran() == null ? null
											: dataParent.getTransaksi().getPendaftaran().getKomunitass()));
					transaksiDetail.getDiskons()
							.addAll(CommonSirs.getDiskonSekarang(null, null, alatMedis, 1,
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

					BiayaAlatMedisPerKelas biayaAlatMedisPerKelas = CommonTarifAlatMedis.getBiayaAlatMedisPerKelas(
							transaksiDetail.getAlatMedis(), kelasPerawatan, transaksiDetail.getDokter(),
							dataParent.getTransaksi().getPendaftaran().getAsuransi(),
							dataParent.getTransaksi().getPendaftaran().getKomunitass(),
							dataParent.getTransaksi().getPendaftaran().getPasien());

					transaksiDetail.setAmount(biayaAlatMedisPerKelas.getBiaya());
					transaksiDetail.setAmountCustom(biayaAlatMedisPerKelas.getBiaya());
					transaksiDetail.setMenggunakanAmountCustom(biayaAlatMedisPerKelas.getHargaBisaDirubahSaatTransaksi()
							&& biayaAlatMedisPerKelas.getPembagianBiayaDalamPersen());

					transaksiDetail.setAlatMedis(alatMedis);
					transaksiDetail.setQty(1.0);
					transaksiDetail.setKeterangan("Transaksi alat medis di lokasi " + dataParent.getLokasi().getNama());
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

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("25px");
		toolbar.setParent(north);

		Toolbarbutton ambilDataItem = new ais.ui.util.MyToolbarbuttonConfig("Ambil Alat Medis", "/img/add_item.png");

		ambilDataItem.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				ambilData(AlatMedis.JENIS_UMUM);
			}

		});
		ambilDataItem.setParent(toolbar);

		ambilDataItem = new ais.ui.util.MyToolbarbuttonConfig("Ambil Bed", "/img/add_item.png");

		ambilDataItem.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				ambilData(AlatMedis.JENIS_TEMPAT_TIDUR);
			}

		});
		ambilDataItem.setParent(toolbar);

		final Toolbarbutton generate = new ais.ui.util.MyToolbarbuttonConfig("Generate Penggunaan Ruangan, Kamar atau Bed",
				"/img/add_item.png");

		generate.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {

				if (dataParent.getLokasi() == null) {
					MyMessageboxConfig.show("Mohon Bapak/Ibu memilih salah satu lokasi terlebih dahulu. Langkah yang dapat dilakukan: (1) buka daftar lokasi; (2) pilih salah satu lokasi yang sesuai; (3) ulangi kembali proses ini.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);

					return;
				}

				final KelasPerawatan kelasPerawatan = dataParent.getKelasPerawatan();

				if (kelasPerawatan == null) {
					MyMessageboxConfig.show("Mohon Bapak/Ibu memilih salah satu kelas perawatan terlebih dahulu. Langkah yang dapat dilakukan: (1) buka daftar kelas perawatan; (2) pilih salah satu kelas perawatan yang sesuai; (3) ulangi kembali proses ini.", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.EXCLAMATION);
					return;
				}

				if (dataParent.getTransaksi().getId() == null) {
					if (!dataParent.onSave(event)) {
						return;
					}
				}

				Session session = HibernateUtil.currentSession();
				List<Pendaftaran> pendaftarans = session.createCriteria(Pendaftaran.class)
						.add(Restrictions.isNotNull("tempatTidur"))
						.add(Restrictions.eq("pasien", dataParent.getTransaksi().getPendaftaran().getPasien()))
						.add(Restrictions.isNull("pembayaran")).list();

				System.out.println("pendaftarans = " + pendaftarans);
				System.out.println("pasien = " + dataParent.getTransaksi().getPendaftaran().getPasien());

				if (pendaftarans.isEmpty()) {
					MyMessageboxConfig.show("Mohon maaf, data penggunaan Ruangan, Kamar, atau Bed tidak ditemukan. Langkah yang dapat dilakukan: (1) pastikan pasien telah memiliki penempatan Ruangan, Kamar, atau Bed; (2) periksa kembali data penggunaan ruangan pasien; (3) apabila kendala masih berlanjut, mohon hubungi administrator sistem.", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.EXCLAMATION);
					return;
				}

				for (Pendaftaran pendaftaran : pendaftarans) {
					TempatTidur tempatTidur = pendaftaran.getTempatTidur();
					AlatMedis alatMedis = (AlatMedis) session.createCriteria(AlatMedis.class)
							.add(Restrictions.eq("tempatTidur", tempatTidur)).setMaxResults(1).uniqueResult();
					if (alatMedis == null) {
						alatMedis = (AlatMedis) session.createCriteria(AlatMedis.class)
								.add(Restrictions.eq("kamar", tempatTidur.getKamar())).setMaxResults(1).uniqueResult();
					}
					if (alatMedis == null) {
						alatMedis = (AlatMedis) session.createCriteria(AlatMedis.class)
								.add(Restrictions.eq("ruang", tempatTidur.getRuang())).setMaxResults(1).uniqueResult();
					}

					System.out.println("tempatTidur = " + tempatTidur + " alatMedis = " + alatMedis);

					if (alatMedis == null) {
						continue;
					}

					TransaksiMedisDetail transaksiDetail = (TransaksiMedisDetail) session.createCriteria(TransaksiMedisDetail.class)
							.add(Restrictions.eq("alatMedis", alatMedis)).createAlias("transaksi", "transaksi")
							.add(Restrictions.eq("transaksi.pendaftaran", pendaftaran)).setMaxResults(1).uniqueResult();

					if (transaksiDetail == null) {
						transaksiDetail = new TransaksiMedisDetail();

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

						BiayaAlatMedisPerKelas biayaAlatMedisPerKelas = CommonTarifAlatMedis.getBiayaAlatMedisPerKelas(
								transaksiDetail.getAlatMedis(), kelasPerawatan, transaksiDetail.getDokter(),
								dataParent.getTransaksi().getPendaftaran().getAsuransi(),
								dataParent.getTransaksi().getPendaftaran().getKomunitass(),
								dataParent.getTransaksi().getPendaftaran().getPasien());

						transaksiDetail.setAmount(biayaAlatMedisPerKelas.getBiaya());
						transaksiDetail.setAmountCustom(biayaAlatMedisPerKelas.getBiaya());
						transaksiDetail
								.setMenggunakanAmountCustom(biayaAlatMedisPerKelas.getHargaBisaDirubahSaatTransaksi()
										&& biayaAlatMedisPerKelas.getPembagianBiayaDalamPersen());
					}

					Double diskon = CommonSirs.getTotalDiskonDalamPersen(null, null, alatMedis, 1,
							dataParent.getTransaksi().getTanggalTransaksi(),
							dataParent.getTransaksi().getPendaftaran() == null ? null
									: dataParent.getTransaksi().getPendaftaran().getAsuransi(),
							dataParent.getTransaksi().getPendaftaran() == null ? null
									: dataParent.getTransaksi().getPendaftaran().getKomunitass());
					Double pajak = CommonSirs.getTotalPajakDalamPersen(null, null, alatMedis,
							dataParent.getTransaksi().getPendaftaran() == null ? null
									: dataParent.getTransaksi().getPendaftaran().getAsuransi(),
							dataParent.getTransaksi().getPendaftaran() == null ? null
									: dataParent.getTransaksi().getPendaftaran().getKomunitass());

					transaksiDetail.setDiskonPersen(diskon);
					transaksiDetail.setPajakPersen(pajak);
					transaksiDetail.getPajaks().addAll(CommonSirs.getPajakSekarang(null, null, alatMedis,
							pendaftaran.getAsuransi(), pendaftaran.getKomunitass()));
					transaksiDetail.getDiskons()
							.addAll(CommonSirs.getDiskonSekarang(null, null, alatMedis, 1,
									dataParent.getTransaksi().getTanggalTransaksi(), pendaftaran.getAsuransi(),
									pendaftaran.getKomunitass()));

					transaksiDetail.setMulai(pendaftaran.getTanggalPendaftaran());
					transaksiDetail.setSampai(new Date());

					transaksiDetail.setAlatMedis(alatMedis);
					transaksiDetail.setKeterangan("Transaksi alat medis di lokasi " + dataParent.getLokasi().getNama());
					transaksiDetail.setTransaksi(dataParent.getTransaksi());
					session.save(transaksiDetail);
				}

				loadData(null);
			}

		});
		generate.setParent(toolbar);

		grid = new Grid();
		grid.setMold("paging");
		grid.setPageSize(25);
		grid.setParent(center);

		Columns columns = new Columns();

		columns.setParent(grid);

		Column column = new Column();
		column.setParent(columns);
		column.setLabel("Dokter/Bidan");
		column.setWidth("15%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Alat Medis");
		column.setWidth("15%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Mulai");
		column.setWidth("18%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Sampai");
		column.setWidth("18%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Qty");
		column.setAlign("right");
		column.setWidth("8%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Satuan");
		column.setWidth("8%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Hrg");
		column.setAlign("right");
		column.setWidth("14%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Diskon");
		column.setAlign("right");
		column.setWidth("12%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Pajak");
		column.setAlign("right");
		column.setWidth("12%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Ttl");
		column.setAlign("right");
		column.setWidth("14%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Hapus");
		column.setWidth("6%");

		Foot foot = new Foot();
		foot.setParent(grid);

		foot.appendChild(new Footer("Total"));

		foot.appendChild(new Footer());
		foot.appendChild(new Footer());
		foot.appendChild(new Footer());

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
	}
}
