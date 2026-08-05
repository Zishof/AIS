package ais.action.master;

import java.util.Date;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Decimalbox;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;

import ais.action.master.helper.RevisiHelper;
import ais.action.report.CommonReportHelper;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.Fakultas;
import ais.database.model.JenisKegiatan;
import ais.database.model.Jurusan;
import ais.database.model.Kegiatan;
import ais.database.model.LogPembayaran;
import ais.database.model.Mahasiswa;
import ais.database.model.Perkuliahan;
import ais.database.model.bni.BniRequest;
import ais.database.model.faspay.FaspayRequest;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyToolbarbuttonConfig;

public class LogPembayaranAction extends GenericAutowireComposer implements DataCriteria, DataSearchDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;

	private org.zkoss.zul.Html dashboardHtml;
	private org.zkoss.zul.Html progressHtml;

	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Combobox searchfakultas;
	private Combobox jenissemester;
	private Combobox searchjurusan;
	private Decimalbox searchtahun;
	private Combobox searchJenisPembayaran;
	private Textbox searchnamamhs;
	private MyDatebox start;
	private MyDatebox end;

	private MyToolbarbuttonConfig find;
	private Mahasiswa mahasiswa;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		Common.initLaguage();

		if (Common.isNumber(execution.getParameter("mahasiswa"))) {
			mahasiswa = (Mahasiswa) HibernateUtil.currentSession().createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.idEq(Long.parseLong(execution.getParameter("mahasiswa").trim()))).uniqueResult();
		}

		Common.createDefaultTimer(new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event arg0) throws Exception {

				new Thread(new Runnable() {

					@Override
					public void run() {
						try {
						Session sessionUtama = HibernateUtil.currentNativeSession();
						int count = ((Number) sessionUtama.createCriteria(LogPembayaran.class)
								.setProjection(Projections.rowCount()).uniqueResult()).intValue();
						HibernateUtil.closeSession();
						if (count == 0) {

							sessionUtama = HibernateUtil.currentNativeSession();
							List<FaspayRequest> faspayRequests = sessionUtama.createCriteria(FaspayRequest.class)
									.add(Restrictions.eq("kodeStatus", "2")).list();
							HibernateUtil.closeSession();
							for (FaspayRequest faspayRequest : faspayRequests) {
								try {
									Kegiatan kegiatan = null;
									Mahasiswa mhs = faspayRequest.getMahasiswa();
									BiodataCalonMahasiswa bio = faspayRequest.getBiodataCalonMahasiswa();
									if (mhs != null) {
										kegiatan = mhs.ambilKegiatans(faspayRequest.getSemester(),
												faspayRequest.getJenisKegiatan());
									} else if (bio != null) {
										kegiatan = bio.ambilKegiatans(faspayRequest.getSemester(),
												faspayRequest.getJenisKegiatan());
									}
									System.out.println(
											"input data faspayRequest " + faspayRequest + " kegiatan " + kegiatan);
									if (kegiatan != null) {
										try {
											Session session = HibernateUtil.currentNativeSession();
											LogPembayaran logPembayaran = new LogPembayaran();
											logPembayaran.setFaspayRequest(faspayRequest);
											logPembayaran.setKegiatan(kegiatan);
											logPembayaran.setNominal(faspayRequest.getAmount());
											logPembayaran.setTanggal(faspayRequest.getTanggal_dirubah());
											session.getTransaction().begin();
											session.save(logPembayaran);
											session.getTransaction().commit();

										} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
										HibernateUtil.closeSession();
									}
								} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
							}

							faspayRequests = null;

							sessionUtama = HibernateUtil.currentNativeSession();
							List<BniRequest> bniRequests = sessionUtama.createCriteria(BniRequest.class)
									.add(Restrictions.eq("response_code", "000")).list();
							HibernateUtil.closeSession();
							for (BniRequest bniRequest : bniRequests) {
								Kegiatan kegiatan = null;
								Mahasiswa mhs = bniRequest.getMahasiswa();
								BiodataCalonMahasiswa bio = bniRequest.getBiodataCalonMahasiswa();

								if (mhs != null) {
									kegiatan = mhs.ambilKegiatans(bniRequest.getSemester(),
											bniRequest.getJenisKegiatan());
								} else if (bio != null) {
									kegiatan = bio.ambilKegiatans(bniRequest.getSemester(),
											bniRequest.getJenisKegiatan());
								}

								System.out.println("input data bniRequest " + bniRequest + " kegiatan " + kegiatan);
								if (kegiatan != null) {

									try {
										Session session = HibernateUtil.currentNativeSession();
										LogPembayaran logPembayaran = new LogPembayaran();
										logPembayaran.setKegiatan(kegiatan);
										logPembayaran.setBniRequest(bniRequest);
										logPembayaran.setNominal(bniRequest.getAmount());
										logPembayaran.setTanggal(bniRequest.getTanggal_dirubah());
										session.getTransaction().begin();
										session.save(logPembayaran);
										session.getTransaction().commit();
									} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
									HibernateUtil.closeSession();
								}
							}

							bniRequests = null;

							sessionUtama = HibernateUtil.currentNativeSession();
							List<Object[]> kelases = sessionUtama.createCriteria(Kegiatan.class).add(Restrictions.eq("aktif", true)).add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")))
									.add(Restrictions
											.sqlRestriction("this_.id not in (select kegiatan from log_pembayaran)"))
									.setProjection(Projections.projectionList().add(Projections.property("id"))
											.add(Projections.property("amount"))
											.add(Projections.property("tanggal_dirubah"))
											.add(Projections.property("amountTerhutang")))

									.list();
							HibernateUtil.closeSession();

							for (Object[] k : kelases) {
								if (k != null) {
									try {
										Session session = HibernateUtil.currentNativeSession();
										System.out.println("input data kegiatan " + k[0]);
										LogPembayaran logPembayaran = new LogPembayaran();
										logPembayaran.setKegiatan(new Kegiatan(Long.parseLong(k[0].toString())));
										try {
											logPembayaran.setNominal(Double.parseDouble(k[1].toString()));
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
										}
										try {
											logPembayaran.setTanggal((Date) k[2]);
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
										}
										session.getTransaction().begin();
										session.save(logPembayaran);
										session.getTransaction().commit();
									} catch (Exception e) {
										Common.tampilErrorJikaAdmin(e);
									}
									HibernateUtil.closeSession();
								}
							}

						}
											} finally {
							ais.database.hibernate.HibernateUtil.closeSession();
						}
					}
				}).start();

			}
		});

		MyComboitemConfig comboitem = new MyComboitemConfig(Perkuliahan.GANJIL);
		if (comboitem != null) { comboitem.setValue(Perkuliahan.GANJIL); }
		jenissemester.appendChild(comboitem);
		comboitem = new MyComboitemConfig(Perkuliahan.GENAP);
		if (comboitem != null) { comboitem.setValue(Perkuliahan.GENAP); }
		jenissemester.appendChild(comboitem);

		comboitem = new MyComboitemConfig("Semua");
		if (comboitem != null) { comboitem.setValue(null); }
		jenissemester.appendChild(comboitem);
		if (jenissemester != null) { jenissemester.setSelectedItem(comboitem); }
		if (jenissemester != null) { jenissemester.setReadonly(true); }

		Common.insertCombo(searchJenisPembayaran, "namaKegiatan", JenisKegiatan.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

		onSearchDefault(null);
		refreshDashboardAman();
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
		refreshDashboardAman();
		}
		});

		String[] contents = new String[] { "id", "kegiatan", "nominal", "tanggal", "validator", "biayaAdministrasi",
				"keterangan", "faspayRequest", "bniRequest", "jatelindoRequest", "postingHistory",
				"postingHistoryPaymentGateway" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, find, comp);

	}

	class LogPembayaranRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {arg0.setValign("top");
			// TODO Auto-generated method stub
			final LogPembayaran logPembayaran = (LogPembayaran) arg1;

			final Kegiatan kegiatan = logPembayaran.getKegiatan();

			if (kegiatan.getMahasiswa() != null) {

				new Label(kegiatan.getRefNumber() == null ? "" : kegiatan.getRefNumber()).setParent(arg0);

				new Label(kegiatan.getMahasiswa() == null ? "" : kegiatan.getMahasiswa().getNim()).setParent(arg0);

				RevisiHelper
						.createNewRevisi(LogPembayaran.class, logPembayaran,
								kegiatan.getMahasiswa() == null ? "" : kegiatan.getMahasiswa().getNama())
						.setParent(arg0);

				new Label(logPembayaran.getNominal() instanceof Number ? Common.numberFormat.get().format(logPembayaran.getNominal()) : "0").setParent(arg0);
				new Label(logPembayaran.getBiayaAdministrasi() instanceof Number ? Common.numberFormat.get().format(logPembayaran.getBiayaAdministrasi()) : "0").setParent(arg0);
				new Label(kegiatan.getMahasiswa() == null || kegiatan.getMahasiswa().getJurusan() == null ? ""
						: kegiatan.getMahasiswa().getJurusan().getNama()).setParent(arg0);
				new Label(kegiatan.getMahasiswa() == null || kegiatan.getMahasiswa().getJurusan() == null
						|| kegiatan.getMahasiswa().getJurusan().getFakultas() == null ? ""
								: kegiatan.getMahasiswa().getJurusan().getFakultas().getNama()).setParent(arg0);
			} else if (kegiatan.getCalonMahasiswa() != null) {
				new Label(kegiatan.getRefNumber() == null ? "" : kegiatan.getRefNumber()).setParent(arg0);

				if (kegiatan.getJenisKegiatan() != null && kegiatan.getJenisKegiatan().getId()
						.equals(ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU.getId())) {
					new Label(kegiatan.getCalonMahasiswa() == null ? ""
							: (kegiatan.getCalonMahasiswa().getNoUjian() == null
									? kegiatan.getCalonMahasiswa().getNoRegistrasi()
									: kegiatan.getCalonMahasiswa().getNoUjian())).setParent(arg0);
				} else {
					new Label(
							kegiatan.getCalonMahasiswa() == null ? "" : kegiatan.getCalonMahasiswa().getNoRegistrasi())
									.setParent(arg0);
				}

				RevisiHelper
						.createNewRevisi(LogPembayaran.class, logPembayaran,
								kegiatan.getCalonMahasiswa() == null ? "" : kegiatan.getCalonMahasiswa().getNama())
						.setParent(arg0);

				new Label(logPembayaran.getNominal() instanceof Number ? Common.numberFormat.get().format(logPembayaran.getNominal()) : "0").setParent(arg0);
				new Label(logPembayaran.getBiayaAdministrasi() instanceof Number ? Common.numberFormat.get().format(logPembayaran.getBiayaAdministrasi()) : "0").setParent(arg0);
				new Label(kegiatan.getCalonMahasiswa() == null || kegiatan.getCalonMahasiswa().getProdiLulus() == null
						? (kegiatan.getCalonMahasiswa().getProdi1() == null ? ""
								: kegiatan.getCalonMahasiswa().getProdi1().getNama())
						: kegiatan.getCalonMahasiswa().getProdiLulus().getNama()).setParent(arg0);
				new Label(kegiatan.getCalonMahasiswa() == null || kegiatan.getCalonMahasiswa().getProdiLulus() == null
						|| kegiatan.getCalonMahasiswa().getProdiLulus().getFakultas() == null
								? (kegiatan.getCalonMahasiswa().getProdi1() == null ? ""
										: kegiatan.getCalonMahasiswa().getProdi1().getFakultas().getNama())
								: kegiatan.getCalonMahasiswa().getProdiLulus().getFakultas().getNama()).setParent(arg0);
			}
			new Label(kegiatan.getSemster() + "").setParent(arg0);

			new Label(Common.dateFormat3.get().format(logPembayaran.getTanggal())).setParent(arg0);
			new Label(kegiatan.getJenisKegiatan() == null ? "" : kegiatan.getJenisKegiatan().getNamaKegiatan())
					.setParent(arg0);
			new Label(logPembayaran.getValidator()).setParent(arg0);

			Hbox toolbar = new Hbox();

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Cetak", "/img/print.png");
			button.setTooltiptext("Cetak");
			button.setOrient("vertical");
			button.addEventListener("onClick", new EventListener() {
				@SuppressWarnings({})
				@Override
				public void onEvent(Event event) throws Exception {
					if (kegiatan.getMahasiswa() != null) {
						CommonReportHelper.cetakBuktipembayaranMahasiswa(kegiatan, false);
					} else {
						CommonReportHelper.cetakBuktipembayaranCalonMahasiswa(kegiatan, false);

					}
				}
			});
			button.setParent(toolbar);
			toolbar.setParent(arg0);

		}

	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Fakultas fakultas = (Fakultas) (searchfakultas.getSelectedItem() == null
				|| searchfakultas.getSelectedItem().getValue() == null
				|| searchfakultas.getSelectedItem().getValue() == null ? null
						: searchfakultas.getSelectedItem().getValue());
		Jurusan jurusan = (Jurusan) (searchjurusan.getSelectedItem() == null
				|| searchjurusan.getSelectedItem().getValue() == null
				|| searchjurusan.getSelectedItem().getValue() == null ? null
						: searchjurusan.getSelectedItem().getValue());

		Criteria criteria = session.createCriteria(LogPembayaran.class).add((start == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (start.getValue() == null
				? Restrictions.sqlRestriction("1=1")
				: Restrictions.sqlRestriction(
						"(this_.tanggal) >= ('" + Common.databaseDateFormat.get().format(start.getValue()) + " 00:00:00')")))

				.add((end == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (end.getValue() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.sqlRestriction("(this_.tanggal) <= ('"
								+ Common.databaseDateFormat.get().format(end.getValue()) + " 23:59:59')")));
		if (order)
			criteria.addOrder(Order.desc("id"));

		criteria.createCriteria("kegiatan")

				.add(mahasiswa == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("mahasiswa", mahasiswa))

				.add(searchJenisPembayaran.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("jenisKegiatan", searchJenisPembayaran.getSelectedItem().getValue()))

				.add(jenissemester.getSelectedItem() == null || jenissemester.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: jenissemester.getSelectedItem().getValue().toString().equalsIgnoreCase(Perkuliahan.GENAP)
								? Restrictions.in("semster", Common.genap) : Restrictions.in("semster", Common.ganjil))

				.createAlias("mahasiswa", "mahasiswa", Criteria.LEFT_JOIN)
				.createAlias("calonMahasiswa", "calonMahasiswa", Criteria.LEFT_JOIN)

				.createAlias("mahasiswa.jurusan", "jurusan", Criteria.LEFT_JOIN)
				.createAlias("calonMahasiswa.prodiLulus", "prodiLulus", Criteria.LEFT_JOIN)

				.add(searchtahun.getValue() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.eq("mahasiswa.tahunangkatan", searchtahun.getValue().intValue()),
								Restrictions.eq("calonMahasiswa.tahun", searchtahun.getValue().intValue())))

				.add(jurusan == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.eq("mahasiswa.jurusan", jurusan),
								Restrictions.eq("calonMahasiswa.prodiLulus", jurusan)))

				.add(searchnamamhs.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(
								Restrictions.ilike("mahasiswa.nama", searchnamamhs.getValue().trim(),
										MatchMode.ANYWHERE),
								Restrictions.ilike("calonMahasiswa.nama", searchnamamhs.getValue().trim(),
										MatchMode.ANYWHERE)))

				.add(fakultas == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.eq("jurusan.fakultas", fakultas),
								Restrictions.eq("prodiLulus.fakultas", fakultas)))

				.add(Restrictions.or(
						Restrictions.or(Restrictions.or(
								Restrictions.ilike("mahasiswa.nim", searchnama.getValue().trim(), MatchMode.ANYWHERE),
								Restrictions.ilike("calonMahasiswa.nim", searchnama.getValue().trim(),
										MatchMode.ANYWHERE)),
								Restrictions.ilike("calonMahasiswa.noRegistrasi", searchnama.getValue().trim(),
										MatchMode.ANYWHERE)),
						Restrictions.ilike("calonMahasiswa.noUjian", searchnama.getValue().trim())));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<Kegiatan> kegiatan = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(kegiatan);
		grid.setRowRenderer(new LogPembayaranRenderer());
		grid.setModelCheckMobile(strset);

	}


	private void refreshDashboardAman() {
		try {
			// paging.getTotalSize() sudah dihitung onSearchDefault (selalu dipanggil tepat sebelum
			// method ini) -> hindari SELECT count(*) yang sama dijalankan dua kali.
			ais.action.master.helper.GenericActionDashboardHelper.refreshFromCriteria(dashboardHtml, progressHtml, this,
					"Dasbor Log Pembayaran", "Pantau histori pembayaran, nilai transaksi, dan pola pembayaran berdasarkan filter yang sedang dipakai.",
					paging == null ? -1L : paging.getTotalSize());
		} catch (Exception e) {
			try {
				ais.common.Common.tampilErrorJikaAdmin(e);
			} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/LogPembayaranAction.java:456");
			}
		}
	}

}
