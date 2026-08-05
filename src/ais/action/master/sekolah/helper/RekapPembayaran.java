package ais.action.master.sekolah.helper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Box;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Foot;
import org.zkoss.zul.Footer;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Group;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.sekolah.JenisBiayaSekolah;
import ais.database.model.sekolah.NominalBiaya;
import ais.database.model.sekolah.PengaturanBiaya;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Tagihan;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyLabelBold;
import ais.ui.util.MyLabelBoldAja;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;

public class RekapPembayaran extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7381263733011550603L;

	private MyWindow window;
	private Combobox bulan;

	private Combobox tahun;

	private Center center;

	private Rows rowsDetailBiaya;

	private MyLabelBold totalTagihan;

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				init();
			}
		});
	}

	private void init() {

		Borderlayout borderlayout = new Borderlayout();
		borderlayout.setParent(window);

		center = new Center();
		ais.ui.util.ZkCompat.setFlex(center, true);
		center.setParent(borderlayout);

		North north = new North();
		north.setParent(borderlayout);

		Box hbox = new Hbox();

		if (Common.isMobile()) {
			hbox = new Vbox();
			hbox.setWidth("100%");
		} else {
			hbox.setHeight("70px");
			hbox.setPack("center");
			hbox.setAlign("center");
		}
		hbox.setParent(north);

		hbox.appendChild(new Label(ais.common.Common.getBahasaConfig("sd Bulan :")));
		hbox.appendChild(bulan = new Combobox());
		bulan.setReadonly(true);

		hbox.appendChild(new Label(ais.common.Common.getBahasaConfig("sd Tahun :")));
		hbox.appendChild(tahun = new Combobox());
		tahun.setReadonly(true);

		Comboitem comboitem;
		for (int i = 0; i < 12; i++) {
			comboitem = new Comboitem(Common.BULAN[i]);
			comboitem.setValue(i + 1);
			bulan.appendChild(comboitem);
		}

		Common.selectComboItem(bulan, ais.ui.util.WaktuUtil.getCalendar().get(Calendar.MONTH) + 1);

		Integer currTahun = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
		for (int i = currTahun - 10; i < currTahun + 10; i++) {
			comboitem = new Comboitem(i + "");
			comboitem.setValue(i);
			tahun.appendChild(comboitem);
		}

		Common.selectComboItem(tahun, currTahun);

		bulan.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				reloadTagihan();
			}
		});

		tahun.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				reloadTagihan();
			}
		});

		reloadTagihan();

	}

	private void reloadTagihan() {

		Common.clear(center);

		Row sub = Common.tampilanScroll1(center);

		Grid grid = new Grid();
		grid.setSclass("dgrid");
		grid.setWidth("100%");
		grid.setParent(sub);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig("Item Pembayaran");
		column.setParent(columns);

		column = new MyColumnConfig("Bulan/Tahun");
		column.setParent(columns);
		column.setWidth("17%");

		column = new MyColumnConfig("Tagihan");
		column.setParent(columns);
		column.setAlign("right");
		column.setWidth("35%");

		rowsDetailBiaya = new Rows();
		rowsDetailBiaya.setParent(grid);

		Foot foot = new Foot();
		foot.setParent(grid);

		Footer footer;
		foot.appendChild(footer = new Footer());
		footer.appendChild(new MyLabelBold("Total"));

		footer = new Footer();
		footer.setAlign("right");
		foot.appendChild(footer);

		footer = new Footer();
		footer.setAlign("right");
		foot.appendChild(footer);

		Vbox vboxData = new Vbox();
		vboxData.setAlign("end");
		vboxData.setPack("end");
		vboxData.setWidth("100%");
		footer.appendChild(vboxData);
		vboxData.appendChild(totalTagihan = new MyLabelBold("0"));
		vboxData.appendChild(new MyLabelBold(""));

		totalTagihan.setStyle("text-align:right;");

		final MyFormRow subBorderlayout = new MyFormRow();
		subBorderlayout.setParent(sub.getParent());

		Hbox spaceBayar = new Hbox();
		spaceBayar.setParent(subBorderlayout);
		spaceBayar.setAlign("center");
		spaceBayar.setPack("center");
		spaceBayar.setWidth("100%");

		spaceBayar.setHeight("60px");

		Common.clear(rowsDetailBiaya);

		Integer bulan = (Integer) this.bulan.getSelectedItem().getValue();
		Integer tahun = (Integer) this.tahun.getSelectedItem().getValue();

		System.out.println("bulan " + bulan + " tahun " + tahun);

	}

	@SuppressWarnings({ "unchecked", "unused" })
	private void tampilPembayaran(Sekolah sekolah, Integer bulan, Integer tahun) {
		if (sekolah != null) {

			Tbmuser tbmuser = Common.getCurrentUser();
			Session session = HibernateUtil.currentSession();
			List<PengaturanBiaya> pengaturanBiayas = ConstantValues.simpleList(session
					.createCriteria(PengaturanBiaya.class)

					.add(Restrictions.eq("sekolah", sekolah))

					.add(Restrictions.or(Restrictions.isNull("jenisBiayaSekolah.aktif"),
							Restrictions.eq("jenisBiayaSekolah.aktif", true)))
					.addOrder(Order.desc("id")).addOrder(Order.desc("jenisBiayaSekolah.periode"))
					.addOrder(Order.asc("jenisBiayaSekolah.nama")), PengaturanBiaya.class);

			System.out.println("pengaturanBiayas => " + pengaturanBiayas.size());

			for (PengaturanBiaya pengaturanBiaya : pengaturanBiayas) {
				JenisBiayaSekolah jenisBiaya = pengaturanBiaya.getJenisBiayaSekolah();
				if (pengaturanBiaya.getAktif() && sekolah != null) {

					List<Tagihan> tagihans = new ArrayList<Tagihan>();

					System.out.println("jenisBiaya => " + jenisBiaya + " " + tagihans.size());

					if (!tagihans.isEmpty()) {
						boolean ada = false;
						for (Tagihan tagihan : tagihans) {
							if (tagihan.getPembayaranSiswaDetail() == null) {
								if (tagihan.getNominalBiaya().getItemBiayaSekolah()
										.getNilaiBiayaBisaDiubahSaatPembayaran() || tagihan.getNominal() > 0.1) {
									ada = true;
									break;
								}
							}
						}

						System.out.println("ada => " + ada);

						if (ada) {

							Group group = new Group(pengaturanBiaya.toString());
							group.setParent(rowsDetailBiaya);

							for (final Tagihan tagihan : tagihans) {
								if (((tagihan.getAktif() &&  !tagihan.ambilBukanTagihanData()) && !tagihan.getNominalBiaya().getBukanTagihan()) && tagihan.getPembayaranSiswaDetail() == null) {
									if (jenisBiaya != null && jenisBiaya.getPeriode().equalsIgnoreCase("Bulanan")) {

										if (tagihan.getPengaturanBiaya().getBulanMulai() != null
												&& tagihan.getTahunbulan() < tagihan.getNominalBiaya()
														.getPengaturanBiaya().getBulanMulai()) {
											continue;
										}
										if (tagihan.getPengaturanBiaya().getBulanSampai() != null
												&& tagihan.getTahunbulan() > tagihan.getNominalBiaya()
														.getPengaturanBiaya().getBulanSampai()) {
											break;
										}
									}

									if (tagihan.getPembayaranSiswaDetail() == null) {

										MyFormRow row = new MyFormRow();
										row.setValign("top");
										row.setParent(rowsDetailBiaya);
										row.setValign("top");
										row.setAttribute("tagihan", tagihan);

										String ket = tagihan.getNominalBiaya().getItemBiayaSekolah().getNama()
												+ (tagihan.getNominalBiaya().getDibayarSebayak() > 1
														? " (ke " + tagihan.getBayarKe() + ")"
														: "");
										Double denda = tagihan.getDenda();
										if (denda > 0.01) {
											ket += ", Denda " + Common.numberFormat.get().format(tagihan.getDenda());
										}
										Date tglDeadline = tagihan.getTanggalDeadline();
										if (tglDeadline != null) {
											ket += ", Deadline " + Common.dateFormat4.get().format(tglDeadline);
										}

										ket += (tagihan.getDiskonSiswa() != null
												&& tagihan.getDiskonSiswa().getMemotongTagihan()
														? " - " + tagihan.getDiskonSiswa().getNama()
														: "");

										if (!tagihan.getLink().isEmpty() && (tagihan.getExpired() == null
												|| tagihan.getExpired().after(WaktuUtil.getDate()))) {
											Vbox vbox = new Vbox();
											vbox.setWidth("95%");
											vbox.setParent(row);
											vbox.appendChild(new ais.ui.util.MyHtml(
													"<div style='font-size:9px;color:red;font-weight: bolder;'>bisa dibayar di link <a style='font-size:7px;color:blue;font-weight: normal;' onclick=\"popupCenter({url: '"
															+ tagihan.getLink()
															+ "', title: 'Pembayaran', w: 600, h: 600});\" href=\"#\">"
															+ tagihan.getLink() + "</a> "
															+ (tagihan.getExpired() == null ? ""
																	: " sampai dengan " + Common.dateFormat.get()
																			.format(tagihan.getExpired()))
															+ "</div>"));
										}

										else if ((tbmuser == null || tbmuser.getSiswa() != null
												|| tbmuser.getCalonSiswa() != null) && tagihan.getVa() != null
												&& (tagihan.getExpired() == null
														|| tagihan.getExpired().after(WaktuUtil.getDate()))) {
											row.appendChild(new ais.ui.util.MyHtml("<div style='font-size:10px;'>"
													+ tagihan.getNominalBiaya().getItemBiayaSekolah().getNama()
													+ (tagihan.getNominalBiaya().getDibayarSebayak() > 1
															? " (ke " + tagihan.getBayarKe() + ")"
															: "")
													+ "</div><div style='font-size:9px;color:red;font-weight: bolder;'>bisa dibayar di nomor VA : "
													+ tagihan.getVa()
													+ (tagihan.getExpired() == null ? ""
															: " sampai dengan "
																	+ Common.dateFormat.get().format(tagihan.getExpired()))
													+ "</div>"));
										} else {

											if (tagihan.getVa() != null && (tagihan.getExpired() == null
													|| tagihan.getExpired().after(WaktuUtil.getDate()))) {
												Vbox vbox = new Vbox();
												vbox.setWidth("95%");
												vbox.setParent(row);
												vbox.appendChild(new ais.ui.util.MyHtml(
														"<div style='font-size:9px;color:red;font-weight: bolder;'>bisa dibayar di nomor VA : "
																+ tagihan.getVa()
																+ (tagihan.getExpired() == null ? ""
																		: " sampai dengan " + Common.dateFormat.get()
																				.format(tagihan.getExpired()))
																+ "</div>"));
											} else {
												row.appendChild(new MyLabelAgakKecil());
											}

										}

										try {

											RevisiHelper.createNewRevisi(Tagihan.class, tagihan,
													(tagihan.getBulan() == null ? ""
															: Common.BULAN[tagihan.getBulan() - 1] + " ")
															+ (tagihan.getTahun() == null ? "-" : tagihan.getTahun()))
													.setParent(row);
										} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/RekapPembayaran.java:381");
										}

										row.appendChild(new Label(Common.numberFormat.get()
												.format(((tagihan.getNominal() - tagihan.getDiskon()) + denda))));

										if (tagihan.getNominalBiaya().getItemBiayaSekolah().getBolehDiangsur()
												&& tagihan.getPengaturanBiaya().getJenisBiayaSekolah()
														.getBolehAngsurBerapapun()
												&& !tagihan.getNominalBiaya().getItemBiayaSekolah().getWajibPilih()) {

											if (tagihan.getNominalBiaya().getDibayarSebayak().intValue() == tagihan
													.getBayarKe().intValue()) {

												if (!tagihan.getNominalBiaya().getItemBiayaSekolah()
														.getAngsuranSeragam() && tagihan.getKunci() == null
														&& tagihan.getPengaturanBiaya().getKunci() == null) {

													Toolbarbutton toolbarbutton = new MyToolbarbuttonConfig("",
															"/img/svg/addthis.svg");
													toolbarbutton.setParent(row);

													toolbarbutton.addEventListener("onClick", new EventListener() {

														@Override
														public void onEvent(Event arg0) throws Exception {

															final MyWindow addWindow = new MyWindow("Tambah Angsuran",
																	"none", false);
															page.getFirstRoot().appendChild(addWindow);
															addWindow.setHeight("300px");
															addWindow.setWidth("400px");

															Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
															Center center = new Center();
															center.setParent(borderlayout);
															ais.ui.util.ZkCompat.setFlex(center, true);
															MyGrid grid = new MyGrid();
															grid.setWidth("100%");
															grid.setParent(center);
															grid.setWidth("100%");
															grid.setHeight("100%");

															Columns columns = new Columns();
															columns.setParent(grid);

															MyColumnConfig column = new MyColumnConfig();
															column.setParent(columns);
															column.setWidth("40%");

															column = new MyColumnConfig();
															column.setParent(columns);

															Rows rows = new Rows();
															rows.setParent(grid);

															MyFormRow row = new MyFormRow();
															row.setValign("top");
															row.setParent(rows);
															row.appendChild(new ais.ui.util.MyLabelConfig("Biaya"));

															row.appendChild(new MyLabelBoldAja(
																	tagihan.getItemBiayaSekolah().getNama()));

															Session session = HibernateUtil.currentSession();
															Tagihan maksTagihan = (Tagihan) session
																	.createCriteria(Tagihan.class)
																	.add(Restrictions.eq("nominalBiaya",
																			tagihan.getNominalBiaya()))
																	.addOrder(Order.desc("bayarKe")).setMaxResults(1)
																	.uniqueResult();

															row = new MyFormRow();
															row.setParent(rows);
															row.appendChild(new ais.ui.util.MyLabelConfig("Tagihan"));

															final Double tag = maksTagihan == null
																	? tagihan.getNominal()
																	: maksTagihan.getNominal();

															row.appendChild(new MyLabelBoldAja(
																	Common.numberFormat.get().format(tag)));

															row = new MyFormRow();
															row.setParent(rows);
															row.appendChild(new ais.ui.util.MyLabelConfig(
																	"Nominal yang akan dibayar *"));
															final MyDoublebox dibayar;
															row.appendChild(dibayar = new MyDoublebox(0.0));
															dibayar.setWidth("90%");

															row = new MyFormRow();
															row.setParent(rows);
															row.appendChild(new ais.ui.util.MyLabelConfig(
																	"Catatan / Informasi"));
															final MyTextbox informasi;
															row.appendChild(informasi = new MyTextbox());
															informasi.setWidth("90%");
															informasi.setRows(5);

															South south = new South();
															ais.ui.util.ZkCompat.setFlex(south, true);
															south.setParent(borderlayout);

															Toolbar toolbar = new Toolbar();
															// toolbar.setHeight("25px");
															toolbar.setParent(south);
															MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig(
																	"Batal", "/img/cancel.gif");
															cancel.setTooltiptext("Tutup");
															cancel.addEventListener("onClick", new EventListener() {
																@Override
																public void onEvent(Event event) throws Exception {
																	addWindow.detach();
																}
															});
															cancel.setParent(toolbar);
															MyToolbarbuttonConfig save = new MyToolbarbuttonConfig(
																	"Simpan", "/img/save.gif");
															save.setTooltiptext("Simpan");
															save.addEventListener("onClick", new EventListener() {
																@Override
																public void onEvent(Event event) throws Exception {

																	if (dibayar.getValue() == null
																			|| dibayar.getValue() < 0.01
																			|| dibayar.getValue() >= tag) {
																		MyMessageboxConfig.show(
																				"Nominal Biaya harus diisi dengan benar",
																				"Peringatan", MyMessageboxConfig.OK,
																				MyMessageboxConfig.INFORMATION);
																		return;
																	}

																	addWindow.detach();

																	Session session = HibernateUtil.currentSession();

																	NominalBiaya nominalBiaya = tagihan
																			.getNominalBiaya();
																	session.refresh(nominalBiaya);

																	Number maks = (Number) session
																			.createCriteria(Tagihan.class)
																			.add(Restrictions.eq("nominalBiaya",
																					nominalBiaya))
																			.setProjection(Projections.rowCount())
																			.add(Restrictions.gt("nominal", 0.1))
																			.uniqueResult();
																	int jml = (maks == null ? 1 : maks.intValue());
																	nominalBiaya.setDibayarSebayak(jml + 1);
																	Common.refreshUpdate(session, nominalBiaya);
																	session.flush();

																	tagihan.setNominalBiaya(nominalBiaya);
																	tagihan.setNominal(dibayar.getValue());
																	Common.refreshUpdate(session, tagihan);

																	session.flush();

																	Double sisaYgBelum = tag - dibayar.getValue();
																	if (sisaYgBelum > 0.1) {

																		Tagihan tagihan1 = ((Tagihan) session
																				.createCriteria(Tagihan.class)
																				.add(Restrictions.eq("nominalBiaya",
																						nominalBiaya))
																				.add(Restrictions.eq("bayarKe",
																						jml + 1))
																				.uniqueResult());

																		if (tagihan1 == null) {
																			try {
																				Tagihan tagihanBaru = new Tagihan();
																				tagihanBaru
																						.setNominalBiaya(nominalBiaya);
																				tagihanBaru.setBulan(nominalBiaya
																						.getPengaturanBiaya()
																						.getJenisBiayaSekolah()
																						.getUntukBulan());
																				tagihanBaru.setTahun(nominalBiaya
																						.getPengaturanBiaya()
																						.getJenisBiayaSekolah()
																						.getUntukTahun());
																				tagihanBaru.setSiswa(
																						nominalBiaya.getSiswa());
																				tagihanBaru.setCalonSiswa(
																						nominalBiaya.getCalonSiswa());
																				tagihanBaru.setItemBiayaSekolah(
																						tagihan.getItemBiayaSekolah());
																				tagihanBaru.setBayarKe(jml + 1);
																				tagihanBaru.setNominal(sisaYgBelum);

																				tagihanBaru.setInformasi(
																						informasi.getValue());
																				session.save(tagihanBaru);
																				session.flush();
																			} catch (Exception e) {
																				Common.tampilErrorJikaAdmin(e);
																			}
																		} else {
																			tagihan1.setInformasi(informasi.getValue());
																			tagihan1.setAktif(true);
																			tagihan1.setNominal(sisaYgBelum);
																			Common.refreshUpdate(session, tagihan1);
																			session.flush();
																		}
																	}

																	Common.createDefaultTimer(new EventListener() {

																		@Override
																		public void onEvent(Event arg0)
																				throws Exception {
																			reloadTagihan();
																		}
																	});
																}
															});
															save.setParent(toolbar);
															borderlayout.setParent(addWindow);

															addWindow.onModal();

														}
													});
												}
											} else if (tagihan.getNominalBiaya().getDibayarSebayak().intValue() > 1
													&& !tagihan.getNominalBiaya().getItemBiayaSekolah()
															.getWajibPilih()) {

												if (!tagihan.getNominalBiaya().getItemBiayaSekolah()
														.getAngsuranSeragam() && tagihan.getKunci() == null
														&& tagihan.getPengaturanBiaya().getKunci() == null) {

													MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("",
															"/img/svg/trash.svg");
													button.setTooltiptext("Hapus Data");
													button.addEventListener("onClick", new EventListener() {
														@Override
														public void onEvent(Event event) throws Exception {
															MyMessageboxConfig.show(
																	"Apakah yakin ingin membatalkan angsuran ini ?",
																	"Pertanyaan",
																	MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
																	MyMessageboxConfig.QUESTION, new EventListener() {

																		@Override
																		public void onEvent(Event event)
																				throws Exception {
																			int i = Integer.parseInt(
																					event.getData().toString());
																			if (i == MyMessageboxConfig.OK) {
																				try {

																					Double tag = tagihan.getNominal();
																					NominalBiaya nominalBiaya = tagihan
																							.getNominalBiaya();
																					Session session = HibernateUtil
																							.currentSession();
																					Tagihan tagihanTerakhir = ((Tagihan) session
																							.createCriteria(
																									Tagihan.class)
																							.add(Restrictions.eq(
																									"nominalBiaya",
																									tagihan.getNominalBiaya()))
																							.add(Restrictions
																									.gt("bayarKe", 1))
																							.addOrder(Order
																									.desc("bayarKe"))
																							.addOrder(Order.desc("id"))
																							.setMaxResults(1)
																							.uniqueResult());
																					if (tagihanTerakhir != null) {
																						tagihanTerakhir.setNominal(
																								tagihanTerakhir
																										.getNominal()
																										+ tag);
																						Common.refreshUpdate(session,
																								tagihanTerakhir);
																						session.flush();

																					}

																					Common.refreshDelete(session,
																							tagihan);

																					List<Tagihan> tagihans = session
																							.createCriteria(
																									Tagihan.class)
																							.add(Restrictions
																									.gt("nominal", 0.1))
																							.add(Restrictions.eq(
																									"nominalBiaya",
																									nominalBiaya))
																							.addOrder(Order.asc("id"))
																							.list();
																					int index = 1;
																					for (Tagihan t : tagihans) {
																						t.setBayarKe(index);
																						Common.refreshUpdate(session,
																								t);
																						session.flush();
																						index++;
																					}

																					System.out.println("index -> "
																							+ index + " tagihans "
																							+ tagihans);

																					if (nominalBiaya != null) {

																						Session mySession = HibernateUtil
																								.currentNativeSession();
																						mySession.refresh(nominalBiaya);

																						nominalBiaya.setDibayarSebayak(
																								index - 1);
																						mySession.getTransaction()
																								.begin();
																						Common.refreshUpdate(mySession,
																								nominalBiaya);
																						mySession.getTransaction()
																								.commit();

																						// mySession.disconnect();
																						if (mySession.isOpen()) {mySession.disconnect();mySession.close();}
																						HibernateUtil.closeSession();
																					}

																					Common.createDefaultTimer(
																							new EventListener() {

																								@Override
																								public void onEvent(
																										Event arg0)
																										throws Exception {

																									reloadTagihan();
																								}
																							});

																				} catch (Exception e) {
																					Common.tampilErrorJikaAdmin(e);
																					MyMessageboxConfig.show(
																							"Data ini tidak dapat dihapus .., karena berelasi dengan data lainnya, error-nya adalah sbagai berikut:"
																									+ e.getMessage());
																				}

																			}

																		}
																	});

														}
													});
													button.setParent(row);
												}
											}
										}

									}
								}
							}
						}
					}
				}
			}

		}
	}
}
