package ais.action.master.payroll;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Window;

import ais.action.master.akunting.helper.AmbilDataPegawaiNotDefaultBanbox;
import ais.action.master.akunting.util.CommonAkunting;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.akunting.Akun;
import ais.database.model.akunting.GrupTransaksi;
import ais.database.model.akunting.PostingHistory;
import ais.database.model.payroll.JenisTransaksiPegawai;
import ais.database.model.payroll.TransaksiPegawai;
import ais.database.model.rab.SatuanKerja;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.WaktuUtil;

public class PostingTransaksiPegawaiAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyGrid grid;

	private MyTextbox searchnama;
	private AmbilDataPegawaiNotDefaultBanbox searchpegawai;
	private Combobox searchJenisTransaksi;
	private MyDatebox searchMulai;
	private MyDatebox searchSampai;

	private boolean approve = false;
	@SuppressWarnings("unused")
	private boolean delete = false;

	private Paging paging;

	private MyToolbarbuttonConfig sent;
	private AmbilDataSatuanKerjaBanbox satuanKerja;
	private Tbmuser tbmuser;
	private boolean adminLain;

	@Override
	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		Calendar calendar = WaktuUtil.getCalendar();
		calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) - 6);
		if (searchMulai != null) { searchMulai.setValue(calendar.getTime()); }
		calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) + 8);
		if (searchSampai != null) { searchSampai.setValue(calendar.getTime()); }

		tbmuser = Common.getCurrentUser();

		adminLain = Common.getApakahAdmin() || CommonPrivilages.checkPrevilages(CommonPrivilages.APPROVE);

		approve = CommonPrivilages.checkPrevilages(CommonPrivilages.APPROVE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		searchpegawai.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});

		Common.insertComboDanSemua(searchJenisTransaksi, "nama", "kode", JenisTransaksiPegawai.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		if (sent != null) { sent.setVisible(approve); }

		loadDataDenganProgressPosting(null);
		Common.initPaging(paging, new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				loadDataDenganProgressPosting(null);
			}
		});
	}

	class TransaksiPegawaiRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final TransaksiPegawai transaksiPegawai = (TransaksiPegawai) arg1;
			arg0.setAttribute("transaksiPegawai", transaksiPegawai);

			RevisiHelper
					.createNewRevisi(TransaksiPegawai.class, transaksiPegawai, transaksiPegawai.getPegawai().getNama())
					.setParent(arg0);
			new Label(transaksiPegawai.getJenisTransaksiPegawai().toString()).setParent(arg0);
			new Label(Common.numberFormat.get().format(transaksiPegawai.getNilai())).setParent(arg0);
			new Label(Common.dateFormat3.get().format(transaksiPegawai.getTanggal())).setParent(arg0);

			Akun akunDebet = transaksiPegawai.getJenisTransaksiPegawai().getAkunDebet();
			Akun akunKredit = transaksiPegawai.getJenisTransaksiPegawai().getAkun();

			if (akunDebet != null && akunKredit != null) {
				Double nilai = transaksiPegawai.getNilai();

				GrupTransaksi.tampilkanJurnal(akunDebet, nilai, akunKredit, nilai).setParent(arg0);
			} else {
				new Label("Transaksi tidak valid."
						+ (akunDebet != null ? " Debet: " + akunDebet.getKode() + "-" + akunDebet.getNama() + "."
								: " Akun debet tidak ada.")
						+ (akunKredit != null ? " Kredit: " + akunKredit.getKode() + "-" + akunKredit.getNama() + "."
								: " Akun kredit tidak ada."))
						.setParent(arg0);
			}

			new Label(transaksiPegawai.getKeterangan()).setParent(arg0);

			new Label(transaksiPegawai.getPostingHistory() == null ? Common.getBahasaConfig("Belum diposting")
					: transaksiPegawai.getPostingHistory().toString()).setParent(arg0);

			Hbox toolbar = new Hbox();
			toolbar.setParent(arg0);

			if (akunDebet != null && akunKredit != null) {
				MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/warning-outline.svg");
				button.setTooltiptext("Batalkan Posting Data");
				button.setVisible(adminLain && transaksiPegawai.getPostingHistory() != null);
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								transaksiPegawai.setPostingHistory(null);
								Common.refreshSaveOrUpdate(transaksiPegawai);
								HibernateUtil.currentSession()
										.createSQLQuery("delete from akunting.grup_transaksi where transaksi_pegawai="
												+ transaksiPegawai.getId() + " and closing is null")
										.executeUpdate();

								Common.createDefaultTimer(new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										loadDataDenganProgressPosting(null);
									}
								});
							}
						});

					}

				});
				button.setParent(toolbar);

				button = new MyToolbarbuttonConfig("", "/img/svg/check2-circle.svg");
				button.setTooltiptext("Posting Data");
				button.setVisible(transaksiPegawai.getPostingHistory() == null && tbmuser != null
						&& tbmuser.ambilSatuanKerja() != null);
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								Session session = HibernateUtil.currentSession();

								PostingHistory postingHistory = new PostingHistory(PostingHistory.JENIS_MAHASISWA);
								postingHistory.setTbmuser(Common.getCurrentUser());
								postingHistory.setTanggal(ais.ui.util.WaktuUtil.getDate());
								postingHistory.setKeterangan("Posting manual oleh " + tbmuser.getUserNama()
										+ " pada waktu " + Common.dateFormat5.get().format(ais.ui.util.WaktuUtil.getDate()));
								session.save(postingHistory);

								Akun akunDebet = transaksiPegawai.getJenisTransaksiPegawai().getAkunDebet();
								Akun akunKredit = transaksiPegawai.getJenisTransaksiPegawai().getAkun();

								if (akunDebet != null && akunKredit != null) {

									CommonAkunting.saveTransaksi(transaksiPegawai, akunDebet, postingHistory,
											transaksiPegawai.getNilai() < 0.0, tbmuser.ambilSatuanKerja(), session);

									transaksiPegawai.setPostingHistory(postingHistory);
									session.update(transaksiPegawai);
								}

								loadDataDenganProgressPosting(null);
							}
						});

					}

				});
				button.setParent(toolbar);
			}

		}

	}

	public void onBatalkanPostingSemua(Event event) throws Exception {

		MyMessageboxConfig.show(
				"Apakah Bapak/Ibu yakin ingin membatalkan posting transaksi pegawai ini? Perlu diketahui bahwa seluruh transaksi yang telah terposting akan dibatalkan.",
				"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
				new EventListener() {

					@SuppressWarnings("unchecked")
					@Override
					public void onEvent(Event event) throws Exception {
						int i = Integer.parseInt(event.getData().toString());
						if (i == MyMessageboxConfig.OK) {
							List<TransaksiPegawai> transaksiPegawais = initCriteria(true)
									.add(Restrictions.isNotNull("postingHistory")).list();

							for (TransaksiPegawai transaksiPegawai : transaksiPegawais) {
								transaksiPegawai.setPostingHistory(null);
								Common.refreshSaveOrUpdate(transaksiPegawai);
								HibernateUtil.currentSession()
										.createSQLQuery("delete from akunting.grup_transaksi where transaksi_pegawai="
												+ transaksiPegawai.getId() + " and closing is null")
										.executeUpdate();
							}
						}

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								loadDataDenganProgressPosting(null);
							}
						});
					}
				});

	}

	public void onPostingSemua(Event event) throws Exception {
		if (grid == null || grid.getRows() == null) {
			return;
		}

		final Window addWindow = new Window();
		page.getFirstRoot().appendChild(addWindow);
		addWindow.setTitle("Posting data akun");
		addWindow.setWidth("300px");
		addWindow.setHeight("300px");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setTitle("Posting semua transaksi pegawai");
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		MyGrid grid = new MyGrid();
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		Column column = new Column();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("100px");

		column = new Column();
		column.setParent(columns);
		column.setLabel("");

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new Label("Tanggal / Waktu"));
		final MyDatebox tanggal;
		row.appendChild(tanggal = new MyDatebox(ais.ui.util.WaktuUtil.getDate()));
		tanggal.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Diposting oleh")));
		row.appendChild(new Label(Common.getCurrentUser().ambilPegawai() == null ? Common.getCurrentUser().getUserId()
				: Common.getCurrentUser().ambilPegawai().getNama()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Satuan Kerja"));

		row.appendChild(satuanKerja = new AmbilDataSatuanKerjaBanbox(true));
		satuanKerja.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Keterangan")));
		final MyTextbox keterangan;
		row.appendChild(keterangan = new MyTextbox());
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("30px");
		toolbar.setParent(south);
		Toolbarbutton cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				addWindow.detach();
			}
		});
		cancel.setParent(toolbar);
		Toolbarbutton save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				final Date tgl = tanggal.getValue();
				if (tgl == null) {
					MyMessageboxConfig.show(
							"Mohon maaf, kolom Tanggal wajib diisi terlebih dahulu. Langkah yang dapat dilakukan: (1) tentukan Tanggal pada kolom yang tersedia; (2) pastikan Tanggal tidak dikosongkan; (3) ulangi kembali proses ini.",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return;
				}
				if (satuanKerja.getAttribute("satuanKerja") == null) {
					MyMessageboxConfig.show(
							"Mohon maaf, Satuan Kerja wajib dipilih terlebih dahulu. Langkah yang dapat dilakukan: (1) pilih Satuan Kerja yang sesuai pada pilihan yang tersedia; (2) pastikan pilihan tidak dikosongkan; (3) ulangi kembali proses ini.",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return;
				}
				if (keterangan.getValue().trim().equals("")) {
					MyMessageboxConfig.show(
							"Mohon maaf, kolom Keterangan wajib diisi terlebih dahulu. Langkah yang dapat dilakukan: (1) isikan Keterangan pada kolom yang tersedia; (2) pastikan Keterangan tidak dikosongkan; (3) ulangi kembali proses ini.",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return;
				}

				MyMessageboxConfig.show(
							"Apakah Bapak/Ibu yakin ingin mem-posting data ini? Perlu diketahui bahwa data yang telah diposting akan tercatat pada jurnal dan tidak dapat diubah secara langsung.",
							"Pertanyaan",
						MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

							@SuppressWarnings("unchecked")
							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {
									final Tbmuser tbmuser = Common.getCurrentUser();
									final Label label = Common.displayLoadBar(new EventListener() {

										@Override
										public void onEvent(Event arg0) throws Exception {
											MyMessageboxConfig.show("Alhamdulillah, posting transaksi pegawai telah berhasil dilakukan.",
													"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
													new EventListener() {

														@Override
														public void onEvent(Event arg0) throws Exception {
															onSearchDefault(arg0);
														}
													});

											addWindow.detach();
										}
									});

									new Thread(new Runnable() {

										@Override
										public void run() {
											try {

											SatuanKerja satuanKerja = (SatuanKerja) PostingTransaksiPegawaiAction.this.satuanKerja
													.getAttribute("satuanKerja");

											List<TransaksiPegawai> transaksiPegawais = initCriteria(false)
													.add(Restrictions.isNull("postingHistory")).list();

											Session session = HibernateUtil.currentNativeSession();
											PostingHistory postingHistory = new PostingHistory(
													PostingHistory.JENIS_TRANSAKSI_LAIN);
											postingHistory.setTbmuser(tbmuser);
											postingHistory.setTanggal(tgl);
											postingHistory.setKeterangan(keterangan.getValue().trim());
											session.getTransaction().begin();
											session.save(postingHistory);
											session.getTransaction().commit();
											int rowIndex = 1;
											for (TransaksiPegawai transaksiPegawai : transaksiPegawais) {
												Akun akunDebet = transaksiPegawai.getJenisTransaksiPegawai()
														.getAkunDebet();
												Akun akunKredit = transaksiPegawai.getJenisTransaksiPegawai().getAkun();
												if (akunDebet != null && akunKredit != null) {
													String ket = "Transaksi pegawai ("
															+ transaksiPegawai.getPegawai().getNama() + ") jenis ("
															+ transaksiPegawai.getJenisTransaksiPegawai().toString()
															+ "), Ket: " + transaksiPegawai.getKeterangan()
															+ ", Tanggal "
															+ Common.dateFormat3.get().format(transaksiPegawai.getTanggal())
															+ ", Nilai "
															+ Common.numberFormat.get().format(transaksiPegawai.getNilai());
													label.setValue(ket + " ("
															+ Common.numberFormat.get()
																	.format(rowIndex * 100.0 / transaksiPegawais.size())
															+ " %)");
													try {
														session.getTransaction().begin();
														CommonAkunting.saveTransaksi(transaksiPegawai, akunDebet,
																postingHistory, transaksiPegawai.getNilai() < 0.0,
																satuanKerja, session);
														session.getTransaction().commit();
													} catch (Exception e) {
														Common.tampilErrorJikaAdmin(e);
													}
													session.getTransaction().begin();
													transaksiPegawai.setPostingHistory(postingHistory);
													session.update(transaksiPegawai);
													session.getTransaction().commit();
												}
												rowIndex++;
											}
											label.setValue("");
											HibernateUtil.closeSession();
																					} finally {
												ais.database.hibernate.HibernateUtil.closeSession();
											}
										}
									}).start();

								}

							}
						});

			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);
		addWindow.onModal();
	}

	@SuppressWarnings("unchecked")
	private void onSearchDefaultTanpaProgress(Event event) {
		Common.initPaging(initCriteria(false), paging);
		List<TransaksiPegawai> transaksiPegawai = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(transaksiPegawai);
		grid.setRowRenderer(new TransaksiPegawaiRenderer());
		grid.setModelCheckMobile(strset);

		grid.renderAll();

	}

	private Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(TransaksiPegawai.class)
				.add((searchMulai == null || searchSampai == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.sqlRestriction(
						"date(tanggal) between date('" + Common.databaseDateFormat.get().format(searchMulai.getValue())
								+ "') and  date('" + Common.databaseDateFormat.get().format(searchSampai.getValue()) + "')")));
		if (order)
			criteria.addOrder(Order.desc("tanggal"));
		criteria.add((searchnama == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.ilike("keterangan", searchnama.getValue(), MatchMode.ANYWHERE)))
				.add((searchpegawai == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchpegawai.getAttribute("pegawai") == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("pegawai", searchpegawai.getAttribute("pegawai"))))
				.add(searchJenisTransaksi.getSelectedItem() == null
						|| searchJenisTransaksi.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("true")
								: Restrictions.eq("jenisTransaksiPegawai",
										searchJenisTransaksi.getSelectedItem().getValue()));
		return criteria;
	}



	public void onSearchDefault(Event event) {
		loadDataDenganProgressPosting(event);
	}

	private boolean postingJurnalLoadingAktif = false;
	private boolean postingJurnalReloadTertunda = false;

	private void loadDataDenganProgressPosting(final org.zkoss.zk.ui.event.Event event) {
		if (postingJurnalLoadingAktif) {
			postingJurnalReloadTertunda = true;
			ais.ui.util.PostingJurnalLoadingUtil.show("Memuat Ulang Data Posting Jurnal",
					"Permintaan reload baru diterima. Data akan dimuat ulang setelah proses yang berjalan selesai.", 12);
			return;
		}
		postingJurnalLoadingAktif = true;
		postingJurnalReloadTertunda = false;
		ais.ui.util.PostingJurnalLoadingUtil.show("Memuat Data Posting Jurnal",
				"Menyiapkan filter dan tabel data jurnal.", 7);
		Common.createDefaultTimer(new org.zkoss.zk.ui.event.EventListener() {
			@Override
			public void onEvent(org.zkoss.zk.ui.event.Event timerEvent) throws Exception {
				try {
					ais.ui.util.PostingJurnalLoadingUtil.update("Mengambil Data Posting Jurnal",
							"Mencari data sesuai tanggal, status posting, dan filter halaman.", 48);
					onSearchDefaultTanpaProgress(event);
					ais.ui.util.PostingJurnalLoadingUtil.update("Merapikan Tampilan",
							"Menyusun tabel, paging, status posting, dan preview jurnal.", 92);
				} finally {
					boolean reloadLagi = postingJurnalReloadTertunda;
					postingJurnalReloadTertunda = false;
					postingJurnalLoadingAktif = false;
					if (reloadLagi) {
						ais.ui.util.PostingJurnalLoadingUtil.update("Memuat Ulang Data Posting Jurnal",
								"Filter atau halaman berubah saat data sedang diproses. Data akan dimuat ulang sekarang.", 96);
						Common.createDefaultTimer(new org.zkoss.zk.ui.event.EventListener() {
							@Override
							public void onEvent(org.zkoss.zk.ui.event.Event ulangEvent) throws Exception {
								loadDataDenganProgressPosting(event);
							}
						});
					} else {
						ais.ui.util.PostingJurnalLoadingUtil.complete("Data Posting Jurnal Siap",
								"Tabel sudah selesai dimuat dan siap digunakan.", 100);
					}
				}
			}
		});
	}

}
