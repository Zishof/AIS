package ais.action.master.sirs.detail;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.lang.StringUtils;
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
import ais.action.master.sirs.helper.AmbilDataDokterBanbox;
import ais.action.master.sirs.helper.AmbilDataTindakanBanyak;
import ais.action.master.sirs.util.CommonTarifTindakan;
import ais.common.Common;
import ais.common.CommonSirs;
import ais.common.listener.GetTransaksi;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.sirs.BiayaTindakanPerKelas;
import ais.database.model.sirs.DiagnosaPenyakit;
import ais.database.model.sirs.Diskon;
import ais.database.model.sirs.Dokter;
import ais.database.model.sirs.KelasPerawatan;
import ais.database.model.sirs.Pendaftaran;
import ais.database.model.sirs.Tindakan;
import ais.database.model.sirs.TindakanDiagnosaPenyakit;
import ais.database.model.sirs.TransaksiMedis;
import ais.database.model.sirs.TransaksiMedisDetail;
import ais.ui.util.MyDoublebox;

/**
 * Helper terfokus untuk transaksi layanan detail. Tipe ini membungkus satu variasi kecil dari alur
 * yang lebih umum agar pemanggil memakai nama domain yang jelas dan tidak menggandakan
 * implementasi.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * Borderlayout}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan
 * yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Grid grid}, {@code Footer total},
 * {@code Footer totalHrg}, {@code Footer totalDiskon}, {@code Footer totalPajak}, {@code GetTransaksi
 * dataParent}; pembacaan/pencarian ({@code loadData()}, {@code loadTotal()}); operasi domain lain ({@code
 * display()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see Borderlayout
 */
public class TransaksiLayananDetailHelper extends Borderlayout {

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

	public TransaksiLayananDetailHelper(GetTransaksi getTransaksi) throws Exception {
		super();
		this.dataParent = getTransaksi;
		display();
	}

	/**
	 * Renderer lokal untuk layar/komponen {@link TransaksiLayananDetailHelper}. Kelas ini menerjemahkan satu item
	 * data menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link TransaksiLayananDetailHelper} dan dapat
	 * mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see TransaksiLayananDetailHelper
	 */
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

			final MyDoublebox jumlah;
			jumlah = new MyDoublebox(transaksiDetail.getQty() == null ? 0.0 : transaksiDetail.getQty());

			final MyDoublebox harga;
			harga = new MyDoublebox(transaksiDetail.getMenggunakanAmountCustom() ? transaksiDetail.getAmountCustom()
					: transaksiDetail.getAmount());

			final Label total = new Label();
			final Label diskon;
			final Label pajak;

			diskon = new Label(Common.numberFormat.get().format(transaksiDetail.getDiskonPersen()) + "% ("
					+ Common.numberFormat.get().format(transaksiDetail.getDiskon()) + ")");
			pajak = new Label(Common.numberFormat.get().format(transaksiDetail.getPajakPersen()) + "% ("
					+ Common.numberFormat.get().format(transaksiDetail.getPajak()) + ")");

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

					BiayaTindakanPerKelas biayaTindakanPerKelas = CommonTarifTindakan.getBiayaTindakanPerKelas(
							transaksiDetail.getTindakan(), kelasPerawatan, transaksiDetail.getDokter(),
							dataParent.getTransaksi().getPendaftaran().getAsuransi(),
							dataParent.getTransaksi().getPendaftaran().getKomunitass(),
							dataParent.getTransaksi().getPendaftaran().getPasien());

					transaksiDetail.setAmount(biayaTindakanPerKelas.getBiaya());

					if (!transaksiDetail.getMenggunakanAmountCustom()) {
						harga.setValue(transaksiDetail.getAmount());
					}

					transaksiDetail.setMenggunakanAmountCustom(biayaTindakanPerKelas.getHargaBisaDirubahSaatTransaksi()
							&& biayaTindakanPerKelas.getPembagianBiayaDalamPersen());

					harga.setReadonly(!transaksiDetail.getMenggunakanAmountCustom());

					transaksiDetail
							.setDiskonPersen(CommonSirs.getTotalDiskonDalamPersen(null, transaksiDetail.getTindakan(),
									null, myJumlah.intValue(), dataParent.getTransaksi().getTanggalTransaksi(),
									dataParent.getTransaksi().getPendaftaran() == null ? null
											: dataParent.getTransaksi().getPendaftaran().getAsuransi(),
									dataParent.getTransaksi().getPendaftaran() == null ? null
											: dataParent.getTransaksi().getPendaftaran().getKomunitass()));

					transaksiDetail.setDiskons(new HashSet<Diskon>());
					transaksiDetail.getDiskons()
							.addAll(CommonSirs.getDiskonSekarang(null, transaksiDetail.getTindakan(), null,
									myJumlah.intValue(), dataParent.getTransaksi().getTanggalTransaksi(),
									dataParent.getTransaksi().getPendaftaran() == null ? null
											: dataParent.getTransaksi().getPendaftaran().getAsuransi(),
									dataParent.getTransaksi().getPendaftaran() == null ? null
											: dataParent.getTransaksi().getPendaftaran().getKomunitass()));

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
							transaksiDetail.getTindakan() == null ? "" : transaksiDetail.getTindakan().getNama())
					.setParent(row);

			jumlah.setParent(row);

			jumlah.setStyle("text-align:right");
			jumlah.setWidth("90%");
			jumlah.addEventListener(Events.ON_CHANGE, eventListener);

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

											loadData(null);

										} catch (Exception e) {
											e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sirs/detail/TransaksiLayananDetailHelper.java:270");
											MyMessageboxConfig.show(Common.pesan(
																"Mohon maaf, data ini tidak dapat dihapus karena masih berelasi dengan data lainnya. Rincian teknis kesalahan: {V1}. Langkah yang dapat dilakukan: (1) pastikan tidak ada data lain yang masih terkait dengan data ini; (2) hapus terlebih dahulu seluruh data yang berelasi; (3) apabila kendala masih berlanjut, mohon hubungi administrator sistem.",
																	e.getMessage()));
										} finally {
											// FIX bocor: session dedikasi (openSession) dulu ditutup di jalur normal saja;
											// finally menjamin penutupan walau exception (idempoten via isOpen()).
											if (session != null && session.isOpen()) {
												try { session.disconnect(); } catch (Exception eSf) { ais.common.ErrorAuditUtil.record(eSf, "auto-audit(empty-catch) src/ais/action/master/sirs/detail/TransaksiLayananDetailHelper.java:278");}
												try { session.close(); } catch (Exception eSf) { ais.common.ErrorAuditUtil.record(eSf, "auto-audit(empty-catch) src/ais/action/master/sirs/detail/TransaksiLayananDetailHelper.java:279");}
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
								.add(Restrictions.isNotNull("tindakan"))
								.add(Restrictions.eq("transaksi", dataParent.getTransaksi())).list();

		if (transaksiDetails.isEmpty() && value != null && value instanceof Pendaftaran) {

			if (dataParent.onSave(null)) {

				Pendaftaran myPendaftaran = (Pendaftaran) value;

				Criterion cariTindakan = Restrictions.sqlRestriction("1=1");
				if (dataParent.getSumber().equals(TransaksiMedis.SUMBER_LAB)) {
					cariTindakan = Restrictions.eq("tindakan.tindakanLab", true);
				} else if (dataParent.getSumber().equals(TransaksiMedis.SUMBER_OPERASI)) {
					cariTindakan = Restrictions.eq("tindakan.tindakanOperasi", true);
				} else if (dataParent.getSumber().equals(TransaksiMedis.SUMBER_RADIOLOGI)) {
					cariTindakan = Restrictions.eq("tindakan.tindakanRadiologi", true);
				} else if (dataParent.getSumber().equals(TransaksiMedis.SUMBER_VK)) {
					cariTindakan = Restrictions.eq("tindakan.tindakanVk", true);
				} else if (dataParent.getSumber().equals(TransaksiMedis.SUMBER_RENAL_UNIT)) {
					cariTindakan = Restrictions.eq("tindakan.tindakanRenalUnit", true);
				} else if (dataParent.getSumber().equals(TransaksiMedis.SUMBER_GIZI)) {
					cariTindakan = Restrictions.eq("tindakan.tindakanGizi", true);
				}

				List<TindakanDiagnosaPenyakit> tindakanDiagnosaPenyakits = HibernateUtil.currentSession()
						.createCriteria(TindakanDiagnosaPenyakit.class)
						.createAlias("diagnosaPenyakit", "diagnosaPenyakit").createAlias("tindakan", "tindakan")
						.add(Restrictions.eq("diagnosaPenyakit.pendaftaran", myPendaftaran)).add(cariTindakan).list();
				final KelasPerawatan kelasPerawatan = dataParent.getKelasPerawatan();

				for (TindakanDiagnosaPenyakit tindakanDiagnosaPenyakit : tindakanDiagnosaPenyakits) {
					Tindakan tindakan = tindakanDiagnosaPenyakit.getTindakan();
					if (tindakan != null && tindakan.getId() != null) {

						TransaksiMedisDetail transaksiDetail = (TransaksiMedisDetail) session
								.createCriteria(TransaksiMedisDetail.class)
								.add(Restrictions.eq("tindakanDiagnosaPenyakit", tindakanDiagnosaPenyakit))
								.add(Restrictions.eq("transaksi", dataParent.getTransaksi())).setMaxResults(1)
								.uniqueResult();
						if (transaksiDetail == null) {
							transaksiDetail = new TransaksiMedisDetail();
						}

						transaksiDetail.setDokter(tindakanDiagnosaPenyakit.getDiagnosaPenyakit() == null ? null
								: tindakanDiagnosaPenyakit.getDiagnosaPenyakit().getDokter());
						transaksiDetail.setTindakanDiagnosaPenyakit(tindakanDiagnosaPenyakit);

						BiayaTindakanPerKelas biayaTindakanPerKelas = CommonTarifTindakan.getBiayaTindakanPerKelas(
								tindakan, kelasPerawatan, transaksiDetail.getDokter(),
								dataParent.getTransaksi().getPendaftaran().getAsuransi(),
								dataParent.getTransaksi().getPendaftaran().getKomunitass(),
								dataParent.getTransaksi().getPendaftaran().getPasien());

						transaksiDetail.setAmount(biayaTindakanPerKelas.getBiaya());
						transaksiDetail.setAmountCustom(biayaTindakanPerKelas.getBiaya());
						transaksiDetail
								.setMenggunakanAmountCustom(biayaTindakanPerKelas.getHargaBisaDirubahSaatTransaksi()
										&& biayaTindakanPerKelas.getPembagianBiayaDalamPersen());
						transaksiDetail.setTindakan(tindakan);
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
								.add(Restrictions.isNotNull("tindakan"))
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

		final Toolbarbutton ambilDataItem = new ais.ui.util.MyToolbarbuttonConfig(
				"Ambil Tindakan " + (dataParent.getSumber().equals(TransaksiMedis.SUMBER_LAIN) ? ""
						: StringUtils.capitalize(dataParent.getSumber().toLowerCase())),
				"/img/add_item.png");

		ambilDataItem.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {

				if (dataParent.getLokasi() == null) {
					MyMessageboxConfig.show("Mohon Bapak/Ibu memilih salah satu lokasi terlebih dahulu. Langkah yang dapat dilakukan: (1) buka daftar lokasi; (2) pilih salah satu lokasi yang sesuai; (3) ulangi kembali proses ini.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return;
				}
				Session session = HibernateUtil.currentSession();

				List<Tindakan> tindakans = dataParent.getTransaksi() == null
						|| dataParent.getTransaksi().getId() == null
								? new ArrayList<Tindakan>()
								: session.createCriteria(TransaksiMedisDetail.class)
										.setProjection(Projections.groupProperty("tindakan"))
										.add(Restrictions.isNotNull("tindakan"))
										.add(Restrictions.eq("transaksi", dataParent.getTransaksi())).list();

				AmbilDataTindakanBanyak ambilDataItemBanyak;

				if (dataParent.getSumber().equals(TransaksiMedis.SUMBER_LAB)) {
					ambilDataItemBanyak = new AmbilDataTindakanBanyak(tindakans, true, null, null, null, null, null);
				} else if (dataParent.getSumber().equals(TransaksiMedis.SUMBER_OPERASI)) {
					ambilDataItemBanyak = new AmbilDataTindakanBanyak(tindakans, null, true, null, null, null, null);
				} else if (dataParent.getSumber().equals(TransaksiMedis.SUMBER_RADIOLOGI)) {
					ambilDataItemBanyak = new AmbilDataTindakanBanyak(tindakans, null, null, true, null, null, null);
				} else if (dataParent.getSumber().equals(TransaksiMedis.SUMBER_VK)) {
					ambilDataItemBanyak = new AmbilDataTindakanBanyak(tindakans, null, null, null, true, null, null);
				} else if (dataParent.getSumber().equals(TransaksiMedis.SUMBER_RENAL_UNIT)) {
					ambilDataItemBanyak = new AmbilDataTindakanBanyak(tindakans, null, null, null, null, true, null);
				} else if (dataParent.getSumber().equals(TransaksiMedis.SUMBER_GIZI)) {
					ambilDataItemBanyak = new AmbilDataTindakanBanyak(tindakans, null, null, null, null, null, true);
				} else {
					ambilDataItemBanyak = new AmbilDataTindakanBanyak(tindakans);
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

						List<Tindakan> tindakans = (List<Tindakan>) arg0.getData();
						dataParent.getSimpan().setDisabled(tindakans.size() == 0);
						dataParent.getAdd().setDisabled(tindakans.size() != 0);
						Session session = HibernateUtil.currentSession();
						for (Tindakan tindakan : tindakans) {

							Double diskon = CommonSirs.getTotalDiskonDalamPersen(null, tindakan, null, 1,
									dataParent.getTransaksi().getTanggalTransaksi(),
									dataParent.getTransaksi().getPendaftaran() == null ? null
											: dataParent.getTransaksi().getPendaftaran().getAsuransi(),
									dataParent.getTransaksi().getPendaftaran() == null ? null
											: dataParent.getTransaksi().getPendaftaran().getKomunitass());
							Double pajak = CommonSirs.getTotalPajakDalamPersen(null, tindakan, null,
									dataParent.getTransaksi().getPendaftaran() == null ? null
											: dataParent.getTransaksi().getPendaftaran().getAsuransi(),
									dataParent.getTransaksi().getPendaftaran() == null ? null
											: dataParent.getTransaksi().getPendaftaran().getKomunitass());

							TransaksiMedisDetail transaksiDetail = new TransaksiMedisDetail();
							transaksiDetail.setDiskonPersen(diskon);
							transaksiDetail.setPajakPersen(pajak);
							transaksiDetail.getPajaks()
									.addAll(CommonSirs.getPajakSekarang(null, tindakan, null,
											dataParent.getTransaksi().getPendaftaran() == null ? null
													: dataParent.getTransaksi().getPendaftaran().getAsuransi(),
											dataParent.getTransaksi().getPendaftaran() == null ? null
													: dataParent.getTransaksi().getPendaftaran().getKomunitass()));
							transaksiDetail.getDiskons()
									.addAll(CommonSirs.getDiskonSekarang(null, tindakan, null, 1,
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

							BiayaTindakanPerKelas biayaTindakanPerKelas = CommonTarifTindakan.getBiayaTindakanPerKelas(
									tindakan, kelasPerawatan, transaksiDetail.getDokter(),
									dataParent.getTransaksi().getPendaftaran().getAsuransi(),
									dataParent.getTransaksi().getPendaftaran().getKomunitass(),
									dataParent.getTransaksi().getPendaftaran().getPasien());

							transaksiDetail.setAmount(biayaTindakanPerKelas.getBiaya());
							transaksiDetail.setAmountCustom(biayaTindakanPerKelas.getBiaya());
							transaksiDetail
									.setMenggunakanAmountCustom(biayaTindakanPerKelas.getHargaBisaDirubahSaatTransaksi()
											&& biayaTindakanPerKelas.getPembagianBiayaDalamPersen());

							transaksiDetail.setTindakan(tindakan);
							transaksiDetail.setQty(1.0);
							transaksiDetail
									.setKeterangan("Transaksi layanan di lokasi " + dataParent.getLokasi().getNama());
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
		column.setLabel("Perawatan");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Qty");
		column.setAlign("right");
		column.setWidth("8%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Hrg");
		column.setAlign("right");
		column.setWidth("10%");

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
		column.setWidth("15%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Hapus");
		column.setWidth("5%");

		Foot foot = new Foot();
		foot.setParent(grid);

		foot.appendChild(new Footer("Total"));

		foot.appendChild(new Footer());

		total = new Footer();
		total.setParent(foot);
		total.setStyle("font-weight:bold;font-size:15px;text-align:right;");

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