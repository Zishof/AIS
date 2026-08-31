package ais.action.master.payroll.detail;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Div;
import org.zkoss.zul.Groupbox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Space;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.generic.AmbilDataPegawaiBanyak;
import ais.action.master.payroll.util.RencanaItemGajiPegawaiTreeModel;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Pegawai;
import ais.database.model.Tbmuser;
import ais.database.model.payroll.FormatItemGaji;
import ais.database.model.payroll.ItemGaji;
import ais.database.model.payroll.RencanaGaji;
import ais.database.model.payroll.RencanaGajiPunyaPegawai;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyGrid;
import ais.ui.util.MyJSONObject;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyLabelAgakKecilBold;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.WaktuUtil;

/**
 * Controller/action ZK untuk rencana gaji punya pegawai. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyDetail}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code RencanaGaji rencanaGaji}, {@code Map
 * grids}, {@code Map pagings}, {@code MyTextbox kode}, {@code MyTextbox nama}, {@code ItemGaji gridSelected};
 * inisialisasi/lifecycle ({@code initCriteria()}); pembacaan/pencarian ({@code loadData()}); operasi domain lain
 * ({@code display()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di
 * atas.</p>
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
public class RencanaGajiPunyaPegawaiAction extends MyDetail {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2586031585928643232L;

	private RencanaGaji rencanaGaji;
	private Map<Long, MyGrid> grids = new HashMap<Long, MyGrid>();
	private Map<Long, Paging> pagings = new HashMap<Long, Paging>();

	public RencanaGajiPunyaPegawaiAction(RencanaGaji rencanaGaji) {
		super();
		this.rencanaGaji = rencanaGaji;
		this.addEventListener(Events.ON_OPEN, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(RencanaGajiPunyaPegawaiAction.this);
				if (isOpen()) {
					display();
				}
			}
		});

	}

	/**
	 * Renderer lokal untuk layar/komponen {@link RencanaGajiPunyaPegawaiAction}. Kelas ini menerjemahkan satu item
	 * data menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link RencanaGajiPunyaPegawaiAction} dan dapat
	 * mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code Date sekarang}, {@code ItemGaji
	 * gridSelected}; operasi lokal: {@code render}(). Aturan bisnis bersama tetap berada pada kelas induk atau
	 * service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see RencanaGajiPunyaPegawaiAction
	 */
	class RencanaGajiPunyaPegawaiRenderer extends ais.ui.util.MyRowRenderer {
		Date sekarang = WaktuUtil.getDate();
		private ItemGaji gridSelected;

		public RencanaGajiPunyaPegawaiRenderer(ItemGaji gridSelected) {
			this.gridSelected = gridSelected;
		}

		@Override
		public void render(final Row arg0, Object data) throws Exception {
			final RencanaGajiPunyaPegawai rencanaGajiPunyaPegawai = (RencanaGajiPunyaPegawai) data;
			final Pegawai pegawai = rencanaGajiPunyaPegawai.getPegawai();

			MyJSONObject jsonObject = new MyJSONObject(rencanaGajiPunyaPegawai.getKomponenGaji());

			CommonMedia.tampilkanGambarKecil(pegawai).setParent(arg0);
			Vbox a;
			(a = RevisiHelper.createNewRevisi(RencanaGajiPunyaPegawai.class, rencanaGajiPunyaPegawai,
					pegawai.getNama())).setParent(arg0);

			new Label(pegawai.getMycode() == null ? "" : pegawai.getMycode()).setParent(a);

			Double nilai1 = 0.0;

			if (gridSelected == null) {
				nilai1 = rencanaGajiPunyaPegawai.getNilai1();
			} else {
				try {
					nilai1 = Double.parseDouble(jsonObject.get(gridSelected.getKode() + "_1").toString());
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/payroll/detail/RencanaGajiPunyaPegawaiAction.java:124");
					// TODO: handle exception
				}
			}

			Double nilai2 = 0.0;

			if (gridSelected == null) {
				nilai2 = rencanaGajiPunyaPegawai.getNilai2();
			} else {
				try {
					nilai2 = Double.parseDouble(jsonObject.get(gridSelected.getKode() + "_2").toString());
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/payroll/detail/RencanaGajiPunyaPegawaiAction.java:136");
					// TODO: handle exception
				}
			}

			Double nilai3 = 0.0;

			if (gridSelected == null) {
				nilai3 = rencanaGajiPunyaPegawai.getNilai3();
			} else {
				try {
					nilai3 = Double.parseDouble(jsonObject.get(gridSelected.getKode() + "_3").toString());
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/payroll/detail/RencanaGajiPunyaPegawaiAction.java:148");
					// TODO: handle exception
				}
			}

			Double nilai4 = 0.0;

			if (gridSelected == null) {
				nilai4 = rencanaGajiPunyaPegawai.getNilai4();
			} else {
				try {
					nilai4 = Double.parseDouble(jsonObject.get(gridSelected.getKode() + "_4").toString());
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/payroll/detail/RencanaGajiPunyaPegawaiAction.java:160");
					// TODO: handle exception
				}
			}

			Double nilai5 = 0.0;

			if (gridSelected == null) {
				nilai5 = rencanaGajiPunyaPegawai.getNilai5();
			} else {
				try {
					nilai5 = Double.parseDouble(jsonObject.get(gridSelected.getKode() + "_5").toString());
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/payroll/detail/RencanaGajiPunyaPegawaiAction.java:172");
					// TODO: handle exception
				}
			}

			Double nilai6 = 0.0;

			if (gridSelected == null) {
				nilai6 = rencanaGajiPunyaPegawai.getNilai6();
			} else {
				try {
					nilai6 = Double.parseDouble(jsonObject.get(gridSelected.getKode() + "_6").toString());
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/payroll/detail/RencanaGajiPunyaPegawaiAction.java:184");
					// TODO: handle exception
				}
			}

			Double nilai7 = 0.0;

			if (gridSelected == null) {
				nilai7 = rencanaGajiPunyaPegawai.getNilai7();
			} else {
				try {
					nilai7 = Double.parseDouble(jsonObject.get(gridSelected.getKode() + "_7").toString());
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/payroll/detail/RencanaGajiPunyaPegawaiAction.java:196");
					// TODO: handle exception
				}
			}

			Double nilai8 = 0.0;

			if (gridSelected == null) {
				nilai8 = rencanaGajiPunyaPegawai.getNilai8();
			} else {
				try {
					nilai8 = Double.parseDouble(jsonObject.get(gridSelected.getKode() + "_8").toString());
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/payroll/detail/RencanaGajiPunyaPegawaiAction.java:208");
					// TODO: handle exception
				}
			}

			Double nilai9 = 0.0;

			if (gridSelected == null) {
				nilai9 = rencanaGajiPunyaPegawai.getNilai9();
			} else {
				try {
					nilai9 = Double.parseDouble(jsonObject.get(gridSelected.getKode() + "_9").toString());
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/payroll/detail/RencanaGajiPunyaPegawaiAction.java:220");
					// TODO: handle exception
				}
			}

			Double nilai10 = 0.0;

			if (gridSelected == null) {
				nilai10 = rencanaGajiPunyaPegawai.getNilai10();
			} else {
				try {
					nilai10 = Double.parseDouble(jsonObject.get(gridSelected.getKode() + "_10").toString());
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/payroll/detail/RencanaGajiPunyaPegawaiAction.java:232");
					// TODO: handle exception
				}
			}

			Double nilai11 = 0.0;

			if (gridSelected == null) {
				nilai11 = rencanaGajiPunyaPegawai.getNilai11();
			} else {
				try {
					nilai11 = Double.parseDouble(jsonObject.get(gridSelected.getKode() + "_11").toString());
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/payroll/detail/RencanaGajiPunyaPegawaiAction.java:244");
					// TODO: handle exception
				}
			}

			Double nilai12 = 0.0;

			if (gridSelected == null) {
				nilai12 = rencanaGajiPunyaPegawai.getNilai12();
			} else {
				try {
					nilai12 = Double.parseDouble(jsonObject.get(gridSelected.getKode() + "_12").toString());
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/payroll/detail/RencanaGajiPunyaPegawaiAction.java:256");
					// TODO: handle exception
				}
			}

			new MyLabelAgakKecil(Common.numberFormat.get().format(nilai1)).setParent(arg0);
			new MyLabelAgakKecil(Common.numberFormat.get().format(nilai2)).setParent(arg0);
			new MyLabelAgakKecil(Common.numberFormat.get().format(nilai3)).setParent(arg0);
			new MyLabelAgakKecil(Common.numberFormat.get().format(nilai4)).setParent(arg0);
			new MyLabelAgakKecil(Common.numberFormat.get().format(nilai5)).setParent(arg0);
			new MyLabelAgakKecil(Common.numberFormat.get().format(nilai6)).setParent(arg0);
			new MyLabelAgakKecil(Common.numberFormat.get().format(nilai7)).setParent(arg0);
			new MyLabelAgakKecil(Common.numberFormat.get().format(nilai8)).setParent(arg0);
			new MyLabelAgakKecil(Common.numberFormat.get().format(nilai9)).setParent(arg0);
			new MyLabelAgakKecil(Common.numberFormat.get().format(nilai10)).setParent(arg0);
			new MyLabelAgakKecil(Common.numberFormat.get().format(nilai11)).setParent(arg0);
			new MyLabelAgakKecil(Common.numberFormat.get().format(nilai12)).setParent(arg0);

			new MyLabelAgakKecilBold(Common.numberFormat.get().format(nilai1 + nilai2 + nilai3 + nilai4 + nilai5 + nilai6
					+ nilai7 + nilai8 + nilai9 + nilai10 + nilai11 + nilai12)).setParent(arg0);

			Vbox vbox2 = new Vbox();
			vbox2.setParent(arg0);

			Hbox toolbar = new Hbox();
			vbox2.appendChild(toolbar);

			Toolbarbutton

			button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show(
							"Apakah Bapak/Ibu yakin ingin menghapus data ini? Data yang telah dihapus tidak dapat dikembalikan. Tekan OK untuk melanjutkan penghapusan, atau Batal untuk membatalkan.",
							"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {
											Common.refreshDelete(rencanaGajiPunyaPegawai);

											loadData(null, gridSelected);

										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											MyMessageboxConfig.show(MyMessageboxConfig.format(
													"Mohon maaf, data ini tidak dapat dihapus karena masih berelasi dengan data lainnya. Rincian teknis: {V1}. Langkah yang dapat dilakukan: (1) hapus atau lepaskan terlebih dahulu data lain yang berelasi; (2) periksa kembali keterkaitan data; (3) hubungi administrator apabila kendala masih berlanjut.",
													e.getMessage()));
										}

									}

								}
							});

				}

			});
			button.setParent(toolbar);

		}
	}

	private MyTextbox kode;
	private MyTextbox nama;

	private Criteria initCriteria(boolean order) {

		Criterion critKode = Restrictions.sqlRestriction("false");
		if (!kode.getValue().trim().equals("")) {
			critKode = Restrictions.or(critKode,
					Restrictions.or(Restrictions.ilike("pegawai.code", kode.getValue().trim(), MatchMode.ANYWHERE),
							Restrictions.ilike("pegawai.mycode", kode.getValue().trim(), MatchMode.ANYWHERE)));
		} else {
			critKode = Restrictions.sqlRestriction("true");
		}

		Criterion critNama = Restrictions.sqlRestriction("false");
		if (!nama.getValue().trim().equals("")) {
			critNama = Restrictions.or(critNama,
					Restrictions.ilike("pegawai.nama", nama.getValue().trim(), MatchMode.ANYWHERE));
		} else {
			critNama = Restrictions.sqlRestriction("true");
		}

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(RencanaGajiPunyaPegawai.class)

				.createAlias("pegawai", "pegawai")

				.add(Restrictions.isNotNull("pegawai.formatItemGaji")).add(Restrictions.eq("pegawai.aktif", true))

				.add(critKode).add(critNama).add(Restrictions.eq("rencanaGaji", rencanaGaji));
		if (order)
			criteria.addOrder(Order.asc("pegawai.nama"));

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void loadData(Event event, ItemGaji itemGaji) {

		MyGrid grid = grids.get(itemGaji == null || itemGaji.getId() == null ? -1L : itemGaji.getId());
		Paging paging = pagings.get(itemGaji == null || itemGaji.getId() == null ? -1L : itemGaji.getId());

		if (grid != null) {

			Common.initPaging25(initCriteria(false), paging);

			List<RencanaGajiPunyaPegawai> rencanaGajiPunyaPegawais = rencanaGaji == null || rencanaGaji.getId() == null
					? new ArrayList<RencanaGajiPunyaPegawai>()
					: initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE_25)
							.setFirstResult(
									Common.ROWS_COUNT_ON_PAGE_25 * (paging == null ? 0 : paging.getActivePage()))
							.list();

			ListModel strset = new SimpleListModel(rencanaGajiPunyaPegawais);
			grid.setRowRenderer(new RencanaGajiPunyaPegawaiRenderer(itemGaji));
			grid.setModelCheckMobile(strset);
			grid.renderAll();
		}
	}

	private ItemGaji gridSelected = null;

	@SuppressWarnings("unchecked")
	private void display() throws Exception {

		Groupbox groupbox = new ais.ui.util.MyGroupboxStyled();
		groupbox.setParent(this);
		groupbox.appendChild(new MyCaptionStyled("Daftar Pegawai"));

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(groupbox);
		Toolbarbutton button = new MyToolbarbuttonConfig("Ambil Data Pegawai", "/img/add_item.png");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				Session session = HibernateUtil.currentSession();

				Tbmuser tbmuser = Common.getCurrentUser();

				List<Pegawai> pegawais = session.createCriteria(RencanaGajiPunyaPegawai.class)
						.setProjection(Projections.groupProperty("pegawai")).createAlias("rencanaGaji", "rencanaGaji")
						.createAlias("pegawai", "pegawai").add(Restrictions.isNotNull("pegawai.formatItemGaji"))
						.add(Restrictions.eq("pegawai.aktif", true))
						.add(Restrictions.eq("rencanaGaji.tahun", rencanaGaji.getTahun())).list();

				AmbilDataPegawaiBanyak ambilDataPegawaiBanyak = new AmbilDataPegawaiBanyak(pegawais,
						tbmuser == null ? null : tbmuser.ambilSatuanKerja());
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(ambilDataPegawaiBanyak);
				ambilDataPegawaiBanyak.setEventListener(new EventListener() {

					@Override
					public void onEvent(final Event arg0) throws Exception {

						final Label label = Common.displayLoadBar(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								loadData(null, gridSelected);
							}
						});

						new Thread(new Runnable() {

							@Override
							public void run() {
								try {

								List<Pegawai> pegawais = (List<Pegawai>) arg0.getData();

								int size = pegawais.size();
								int index = 0;

								for (Pegawai pegawai : pegawais) {

									index++;
									label.setValue("Memproses data rencana gaji " + pegawai.getNama() + " ("
											+ Common.numberFormat.get().format((index * 100.0) / size) + "%)");

									Session session1 = HibernateUtil.currentNativeSession();
									RencanaGajiPunyaPegawai rencanaGajiPunyaPegawai = (RencanaGajiPunyaPegawai) session1
											.createCriteria(RencanaGajiPunyaPegawai.class)
											.add(Restrictions.eq("rencanaGaji", rencanaGaji))
											.add(Restrictions.eq("pegawai", pegawai)).setMaxResults(1).uniqueResult();
									session1.disconnect();
									session1.close();
									HibernateUtil.closeSession();

									if (rencanaGajiPunyaPegawai == null) {
										rencanaGajiPunyaPegawai = new RencanaGajiPunyaPegawai();
										rencanaGajiPunyaPegawai.setPegawai(pegawai);
										rencanaGajiPunyaPegawai.setKeterangan("");
										rencanaGajiPunyaPegawai.setRencanaGaji(rencanaGaji);

										Session session = HibernateUtil.currentNativeSession();
										session.getTransaction().begin();
										session.save(rencanaGajiPunyaPegawai);
										session.getTransaction().commit();
										// session.disconnect();
										if (session.isOpen()) {session.disconnect();session.close();}
										HibernateUtil.closeSession();
									}

									if (rencanaGajiPunyaPegawai.getPegawai() != null
											&& rencanaGajiPunyaPegawai.getPegawai().getFormatItemGaji() != null) {
										RencanaItemGajiPegawaiTreeModel rencanaItemGajiPegawaiTreeModel = new RencanaItemGajiPegawaiTreeModel(
												false, rencanaGajiPunyaPegawai);
										try {
											rencanaItemGajiPegawaiTreeModel.reset(WaktuUtil.getDate(), null,
													rencanaGaji.getTahun());
										} catch (Exception e) {
											// TODO Auto-generated catch block
											e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/payroll/detail/RencanaGajiPunyaPegawaiAction.java:477");
										}

									}
								}

								label.setValue("");
															} finally {
									ais.database.hibernate.HibernateUtil.closeSession();
								}
							}
						}).start();

					}
				});
				ambilDataPegawaiBanyak.setWidth("90%");
				ambilDataPegawaiBanyak.setHeight("97%");
				ambilDataPegawaiBanyak.setVisible(true);
				ambilDataPegawaiBanyak.onModal();
			}

		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Hitung Ulang", "/img/add_item.png");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {

				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event a) throws Exception {

						final Label label = Common.displayLoadBar(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								loadData(null, gridSelected);
							}
						});

						new Thread(new Runnable() {

							@Override
							public void run() {

								List<RencanaGajiPunyaPegawai> rencanaGajiPunyaPegawais = initCriteria(true).list();
								int size = rencanaGajiPunyaPegawais.size();
								int index = 0;
								for (RencanaGajiPunyaPegawai rencanaGajiPunyaPegawai : rencanaGajiPunyaPegawais) {
									try {
										index++;
										label.setValue("Memproses data rencana gaji "
												+ rencanaGajiPunyaPegawai.getPegawai().getNama() + " ("
												+ Common.numberFormat.get().format((index * 100.0) / size) + "%)");
										if (rencanaGajiPunyaPegawai.getPegawai() != null
												&& rencanaGajiPunyaPegawai.getPegawai().getFormatItemGaji() != null) {
											RencanaItemGajiPegawaiTreeModel rencanaItemGajiPegawaiTreeModel = new RencanaItemGajiPegawaiTreeModel(
													false, rencanaGajiPunyaPegawai);

											rencanaItemGajiPegawaiTreeModel.reset(WaktuUtil.getDate(), null,
													rencanaGaji.getTahun());

										}

									} catch (Exception e) {
										// TODO Auto-generated catch block
										e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/payroll/detail/RencanaGajiPunyaPegawaiAction.java:546");
									}
								}

								label.setValue("");
							}
						}).start();

					}
				});

			}

		});
		button.setParent(toolbar);

		toolbar.appendChild(new Space());
		toolbar.appendChild(new Space());
		toolbar.appendChild(new Space());
		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Kode:")));
		toolbar.appendChild(kode = new MyTextbox());
		kode.setWidth("80px");
		kode.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(arg0, gridSelected);
			}
		});

		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Nama:")));
		toolbar.appendChild(nama = new MyTextbox());
		nama.setWidth("80px");
		nama.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(arg0, gridSelected);
			}
		});

		Toolbarbutton search;
		toolbar.appendChild(search = new MyToolbarbuttonConfig("", "/img/svg/search.svg"));
		search.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(arg0, gridSelected);
			}
		});

		Toolbarbutton delete;
		toolbar.appendChild(delete = new MyToolbarbuttonConfig("", "/img/svg/trash.svg"));
		delete.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				MyMessageboxConfig.show(
						"Apakah Bapak/Ibu yakin ingin menghapus seluruh data ini? Data yang telah dihapus tidak dapat dikembalikan. Tekan OK untuk melanjutkan penghapusan, atau Batal untuk membatalkan.",
						"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {

							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {
									try {

										Session session = HibernateUtil.currentSession();
										session.createSQLQuery(
												"delete from payroll.rencana_gaji_punya_pegawai where rencana_gaji = "
														+ rencanaGaji.getId())
												.executeUpdate();

										loadData(event, gridSelected);
									} catch (Exception e) {
										Common.tampilErrorJikaAdmin(e);
										MyMessageboxConfig.show(MyMessageboxConfig.format(
												"Mohon maaf, data ini tidak dapat dihapus karena masih berelasi dengan data lainnya. Rincian teknis: {V1}. Langkah yang dapat dilakukan: (1) hapus atau lepaskan terlebih dahulu data lain yang berelasi; (2) periksa kembali keterkaitan data; (3) hubungi administrator apabila kendala masih berlanjut.",
												e.getMessage()));
									}

								}

							}
						});

			}
		});

		Session session = HibernateUtil.currentSession();
		List<FormatItemGaji> formatItemGajis = session.createCriteria(RencanaGajiPunyaPegawai.class)
				.add(Restrictions.eq("rencanaGaji", rencanaGaji)).createAlias("pegawai", "pegawai")
				.add(Restrictions.isNotNull("pegawai.formatItemGaji")).add(Restrictions.eq("pegawai.aktif", true))
				.setProjection(Projections.groupProperty("pegawai.formatItemGaji")).list();

		List<ItemGaji> itemGajis = session.createCriteria(ItemGaji.class)
				.add(formatItemGajis.isEmpty() ? Restrictions.sqlRestriction("false")
						: Restrictions.in("formatItemGaji", formatItemGajis))
				.list();

		TreeMap<String, ItemGaji> myItems = new TreeMap<String, ItemGaji>();
		NumberFormat nf = new DecimalFormat("000");
		for (ItemGaji itemGaji : itemGajis) {
			String kode = nf.format(itemGaji.getNomorUrut()) + "-" + itemGaji.getKode();
			myItems.put(kode, itemGaji);
		}

		// GANTI TAB -> BUTTON GROUP (kelas reusable ais.ui.util.MyButtonTabbox): tab per item
		// gaji ini data-driven, sama seperti pola "Ke-1".."Ke-N" di SetingBiayaAction yang
		// sebelumnya bermasalah blank/scroll pakai Tab/Tabpanel bawaan ZK. gridSelected harus
		// ikut tab yang lagi tampil SETIAP KALI pindah (dipakai tombol lain di luar tabbox),
		// bukan cuma sekali muat -- makanya dipisah dari pemuat lazy lewat onSetiapPilih.
		final ais.ui.util.MyButtonTabbox tabboxGaji = ais.ui.util.MyButtonTabbox.buat(groupbox, "3000px", null);

		int indexGaji = 0;
		int indexTerpilihGaji = 1;
		for (final ItemGaji itemGaji : myItems.values()) {
			indexGaji++;
			final int index = indexGaji;

			tabboxGaji.onSetiapPilih(index, new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					gridSelected = itemGaji;
				}
			});

			tabboxGaji.tambahTabLazy(index, itemGaji.getKode(), new ais.ui.util.MyButtonTabbox.PemuatTab() {
				@Override
				public void muat(Div panelUtamaData) throws Exception {

					Div div = new Div();
					div.setParent(panelUtamaData);

					MyGrid grid = new MyGrid();

					grids.put(itemGaji == null || itemGaji.getId() == null ? -1L : itemGaji.getId(), grid);

					grid.setMold("paging");
					grid.setPageSize(100);
					grid.setParent(div);

					Columns columns = new Columns();

					columns.setParent(grid);

					Column column = new Column();
					column.setParent(columns);
					column.setLabel("Foto");
					column.setWidth("70px");

					column = new Column();
					column.setParent(columns);
					column.setLabel("Nama");
					column.setWidth("10%");

					for (int i = 0; i < 12; i++) {
						column = new Column();
						column.setParent(columns);
						column.setLabel(Common.BULAN[i]);
						column.setAlign("right");
						column.setWidth("8%");
					}

					column = new Column();
					column.setParent(columns);
					column.setAlign("right");
					column.setLabel("RENC_TOT_" + (itemGaji == null ? "" : itemGaji.getKode()));
					column.setWidth("10%");

					column = new Column();
					column.setParent(columns);
					column.setLabel("");
					column.setWidth("5%");

					Paging paging = new Paging();

					pagings.put(itemGaji == null || itemGaji.getId() == null ? -1L : itemGaji.getId(), paging);

					Common.initPaging25(paging, new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							loadData(arg0, itemGaji);
						}
					});
					paging.setParent(div);

					loadData(null, itemGaji);
				}
			});
			tabboxGaji.setTooltipTombol(index, itemGaji.getNama());

			// Pola lama: tab TERAKHIR yang dibuat selalu jadi default terpilih (bukan yang
			// pertama) -- dipertahankan persis di sini.
			indexTerpilihGaji = index;
		}
		tabboxGaji.pilih(indexTerpilihGaji);
	}

}
