package ais.action.master.pmb;

import java.util.List;
import java.util.TreeMap;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Box;
import org.zkoss.zul.Caption;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Div;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Groupbox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.maintenance.PMBAction;
import ais.action.master.TampilanELearningAction;
import ais.action.master.dashboard.admin.DashboardTimelinePertemuan;
import ais.action.master.helper.AktifitasJadwalUjianPMBHelper;
import ais.action.report.CommonReportHelper;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.HasilUjianMahasiswa;
import ais.database.model.JadwalUjianPMB;
import ais.database.model.Pertemuan;
import ais.database.model.PertemuanPunyaUjian;
import ais.database.model.RuangPaketPMB;
import ais.database.model.Tbmuser;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyLabelAgakKecilBoldHijau;
import ais.ui.util.MyLabelAgakKecilBoldMerah;
import ais.ui.util.MyLabelBolder;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;

public class TampilanUjianCalonMahasiswa extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4568230033051291139L;
	protected AktifitasJadwalUjianPMBHelper aktifitasJadwalUjianPMBHelper = new AktifitasJadwalUjianPMBHelper();
	private boolean tampilTutup = true;

	public TampilanUjianCalonMahasiswa(boolean tampilTutup) {
		super();
		this.tampilTutup = tampilTutup;
	}

	public TampilanUjianCalonMahasiswa(String title, String border, boolean closable) {
		super(title, border, closable);
		// TODO Auto-generated constructor stub
	}

	private boolean materiBol = true;
	private boolean ujianBol = true;
	private boolean tugasBol = true;

	@SuppressWarnings("unchecked")
	public void init(final BiodataCalonMahasiswa biodataCalonMahasiswa) throws Exception {
		final MyCheckboxConfig materiPil = new MyCheckboxConfig("Materi");
		final MyCheckboxConfig ujianPil = new MyCheckboxConfig("Ujian");
		final MyCheckboxConfig tugasPil = new MyCheckboxConfig("Tugas");
		final Textbox cari = new Textbox();
		EventListener eventListenerReload = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				materiBol = materiPil.isChecked();
				ujianBol = ujianPil.isChecked();
				tugasBol = tugasPil.isChecked();

				init(biodataCalonMahasiswa);
			}
		};

		try {
			Common.clear(this);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/pmb/TampilanUjianCalonMahasiswa.java:102");
			// TODO: handle exception
		}
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

		if (tampilTutup) {
			Borderlayout borderlayout1 = new ais.ui.util.MyBorderlayout();
			borderlayout1.setParent(this);

			North north = new North();
			north.setParent(borderlayout1);
			north.setSclass("headerHbox");
			ais.ui.util.ZkCompat.setFlex(north, true);

			Div kopBox = new Div();
			north.appendChild(kopBox);
			PMBAction.initHeader(kopBox, false);

			Center center = new Center();
			center.setParent(borderlayout1);
			ais.ui.util.ZkCompat.setFlex(center, true);
			center.appendChild(borderlayout);
		} else {
			borderlayout.setParent(this);
		}

		boolean mobile = Common.isMobile();

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, true);

		Box vbox = mobile ? new Vbox() : new Hbox();
		vbox.setParent(north);

		Hbox hbox = new Hbox();
		hbox.setParent(vbox);

		hbox.appendChild(new Label(ais.common.Common.getBahasaConfig("Cari :")));
		hbox.appendChild(cari);
		cari.setCols(15);

		final MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Refresh", "/img/Button-Refresh-icon.png");
		hbox.appendChild(button);

		hbox = new Hbox();
		hbox.setParent(vbox);
		hbox.appendChild(materiPil);
		hbox.appendChild(ujianPil);
		hbox.appendChild(tugasPil);

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		materiPil.addEventListener("onClick", eventListenerReload);
		ujianPil.addEventListener("onClick", eventListenerReload);
		tugasPil.addEventListener("onClick", eventListenerReload);

		materiPil.setChecked(materiBol);
		ujianPil.setChecked(ujianBol);
		tugasPil.setChecked(tugasBol);

		Session session = HibernateUtil.currentSession();

		Long idRuang = (Long) session.createCriteria(RuangPaketPMB.class)
				.add(Restrictions.eq("biodataCalonMahasiswa", biodataCalonMahasiswa))
				.setProjection(Projections.property("ruangPMB.id")).addOrder(Order.desc("id")).setMaxResults(1)
				.uniqueResult();
		if (idRuang == null) {
			idRuang = -1L;
		}

		System.out.println(biodataCalonMahasiswa + " idRuang->" + idRuang);

		List<JadwalUjianPMB> jadwal =

				ConstantValues
						.simpleList(
								session.createCriteria(JadwalUjianPMB.class)
										.add(Restrictions.or(Restrictions.eq("aktif", true),
												Restrictions.isNull("aktif")))

										.add(Restrictions.or(
												Restrictions.or(Restrictions.isNull("berlakuUntukSemuaRuangan"),
														Restrictions.eq("berlakuUntukSemuaRuangan", true)),
												Restrictions.ilike("ruanganYgIkut", "," + idRuang.toString() + ",",
														MatchMode.ANYWHERE)))

										.createAlias("ujianPMB", "ujianPMB")
										.add(Restrictions
												.eq("ujianPMB.gelombangPendaftaran",
														biodataCalonMahasiswa.getGelombangPendaftaran()))
										.add(Restrictions.or(
												Restrictions.or(Restrictions.isNull("paket"),
														Restrictions.eq("paket", biodataCalonMahasiswa.getPaket())),
												Restrictions.isNull("paket"))),
								JadwalUjianPMB.class);

		System.out.println(" jadwal size ->" + jadwal.size());

		final TreeMap<String, Long> pertemuansa = new TreeMap<String, Long>();
		for (JadwalUjianPMB jadwalUjianPMB : jadwal) {
			for (Pertemuan pertemuan : jadwalUjianPMB.ambilPertemuanList()) {
				String keyPert = Common.dateFormat8.get().format(pertemuan.getTanggal());

				keyPert += ("_" + (pertemuan.getWaktuMulai() == null && pertemuan.getWaktuSelesai() == null
						? "00.00-00.00"
						: (pertemuan.getWaktuMulai() == null ? "00.00" : pertemuan.getWaktuMulai()) + "-"
								+ (pertemuan.getWaktuSelesai() == null ? "00.00" : pertemuan.getWaktuSelesai())));

				pertemuansa.put(keyPert + "_" + pertemuan.getId(), pertemuan.getId());
			}
		}

		System.out.println(" pertemuans size ->" + pertemuansa.size());

		Tbmuser tbmuser = Common.getCurrentUser();
		hbox.appendChild(DashboardTimelinePertemuan.createScanFoto(tbmuser, pertemuansa));

		final Rows rows = new Rows();
		final Paging paging = new Paging();

		Borderlayout subBorderlayout = new Borderlayout();
		subBorderlayout.setParent(center);

		Center subcenter = new Center();
		subcenter.setParent(subBorderlayout);
		ais.ui.util.ZkCompat.setFlex(subcenter, true);
		subcenter.setBorder("none");

		Grid grid = new Grid();
		grid.setSclass("dgrid");
		grid.setParent(subcenter);
		grid.appendChild(rows);

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth(Common.isMobile() ? "40%" : "30%");

		EventListener eventListener = new EventListener() {

			@SuppressWarnings("deprecation")
			@Override
			public void onEvent(Event arg0) throws Exception {

				Common.clear(rows);
				Row row = new Row();
				row.setValign("top");
				row.setStyle("border:0px;background: transparent;font-size: x-small;");
				row.setParent(rows);
				ais.ui.util.ZkCompat.setSpans(row, "2");

				Vbox vbox = new Vbox();
				vbox.setParent(row);
				vbox.setWidth("100%");
				final Label label;
				final boolean refresh = arg0 != null && arg0.getTarget() == button;
				vbox.appendChild(label = new Label(ais.common.Common.getBahasaConfig("Ambil data ...")));
				Image img;
				vbox.appendChild(img = new Image("/loading_icon.gif"));
				img.setWidth("90%");

				Common.createDefaultTimerNoBusy(new EventListener() {

					@Override
					public void onEvent(Event a) throws Exception {

						Tbmuser tbmuser = new Tbmuser(biodataCalonMahasiswa);
						Common.clear(rows);

						if (biodataCalonMahasiswa.getGelombangPendaftaran() != null && !biodataCalonMahasiswa
								.getGelombangPendaftaran().getInfoSetelahUjianOnline().isEmpty()) {

							Row row = new Row();
							row.setValign("top");
							rows.appendChild(row);
							ais.ui.util.ZkCompat.setSpans(row, "2");

							Groupbox vbox = new ais.ui.util.MyGroupboxStyled();
							vbox.appendChild(new Caption("Informasi Ujian, Tugas dan Materi"));
							vbox.setWidth("95%");
							vbox.setParent(row);
							vbox.appendChild(new Html(
									biodataCalonMahasiswa.getGelombangPendaftaran().getInfoSetelahUjianOnline()));
						}

						boolean urutBerdasarkanNama = true;
						TampilanELearningAction.loadDataMateri(cari, rows, paging, refresh, materiPil, ujianPil,
								tugasPil, label, tbmuser, pertemuansa, true, urutBerdasarkanNama);

						if (biodataCalonMahasiswa.getGelombangPendaftaran() != null
								&& biodataCalonMahasiswa.getGelombangPendaftaran().getTerdapatUjianOnline()) {
							Session session = HibernateUtil.currentSession();
							try {
								List<PertemuanPunyaUjian> pertemuanPunyaUjians = ConstantValues
										.simpleList(session
										.createCriteria(PertemuanPunyaUjian.class).addOrder(Order.asc("nama"))

										.add(Restrictions.or(
												Restrictions.or(
														Restrictions.eq("tidakDitampilkanJikaWaktuSudahTerlewat",
																false),
														Restrictions.isNull("tidakDitampilkanJikaWaktuSudahTerlewat")),
												
												Restrictions.sqlRestriction("date('"
														+ Common.databaseDateFormat.get().format(WaktuUtil.getDate())
														+ "') between date(mulai_ujian) and date(sampai_ujian)")))

										.createAlias("pertemuan", "pertemuan")
										.createAlias("pertemuan.jadwalUjianPMB", "jadwalUjianPMB")
										.add(Restrictions.or(Restrictions.isNull("jadwalUjianPMB.paket"),
												Restrictions.eq("jadwalUjianPMB.paket",
														biodataCalonMahasiswa.getPaket())))
										.createAlias("jadwalUjianPMB.ujianPMB", "ujianPMB")
										.add(Restrictions.eq("ujianPMB.gelombangPendaftaran",
												biodataCalonMahasiswa.getGelombangPendaftaran()))
										,PertemuanPunyaUjian.class);

								if (!pertemuanPunyaUjians.isEmpty()) {

									boolean ada = false;
									System.out.println("pertemuanPunyaUjians -> " + pertemuanPunyaUjians.size());
									Double nilaiTotal = 0.0;
									Double jumlah = 0.0;
									Vbox vbox = new Vbox();

									Row row = new Row();
									row.setValign("top");
									rows.appendChild(row);
									ais.ui.util.ZkCompat.setSpans(row, "2");

									Groupbox groupbox = new ais.ui.util.MyGroupboxStyled();
									groupbox.appendChild(new Caption("Informasi Hasil Ujian"));
									groupbox.setWidth("95%");
									row.appendChild(groupbox);
									vbox.setParent(groupbox);
									vbox.setWidth("100%");

									for (PertemuanPunyaUjian pertemuanPunyaUjian : pertemuanPunyaUjians) {
										try {
											Number hasilUjianMahasiswaCount = ((Number) session
													.createCriteria(HasilUjianMahasiswa.class)
													.add(Restrictions.eq("biodataCalonMahasiswa",
															biodataCalonMahasiswa))
													.add(Restrictions.eq("pertemuanPunyaUjian", pertemuanPunyaUjian))
													.add(Restrictions.isNotNull("mulaiPada"))
													.setProjection(Projections.max("nilai")).uniqueResult());
											if (hasilUjianMahasiswaCount == null) {
												vbox.appendChild(
														new MyLabelAgakKecilBoldMerah("Anda belum mengikuti ujian \""
																+ pertemuanPunyaUjian.getNama() + "\""));
												ada = true;
											} else {
												nilaiTotal += hasilUjianMahasiswaCount.doubleValue();
												jumlah += 1.0;
											}
										} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/pmb/TampilanUjianCalonMahasiswa.java:365");
											// TODO: handle exception
										}
									}

									System.out.println("nilaiTotal -> " + nilaiTotal);

									if (!ada && nilaiTotal > 0.01) {

										vbox.appendChild(new MyLabelAgakKecilBoldHijau(Common.getKonfigurasi(
												"info_lulus_ujian_pmb",
												"Selamat saudara dinyatakan telah selesai mengikuti proses seleksi tahap ujian online. Ikuti tahap selanjutnya.")
												.getNilai()));

										String nilai = "CUKUP";
										Double rataRata = nilaiTotal / jumlah;
										if (rataRata >= 90.0) {
											nilai = "UNGGUL";
										} else if (rataRata >= 70.0) {
											nilai = "BAIK SEKALI";
										} else if (rataRata >= 50.0) {
											nilai = "BAIK";
										}

										if (biodataCalonMahasiswa.getGelombangPendaftaran()
												.getUjianOnlineOtomatisDiterima()) {

											if (biodataCalonMahasiswa.getGelombangPendaftaran()
													.getNilaiMinimalUjianOnlineOtomatisDiterima() <= rataRata
													&& biodataCalonMahasiswa.getProdiLulus() == null) {

												biodataCalonMahasiswa.setProdiLulus(biodataCalonMahasiswa.getProdi1());
												biodataCalonMahasiswa.setTanggalDiterima(WaktuUtil.getDate());
												biodataCalonMahasiswa.setStatusLulus(BiodataCalonMahasiswa.LULUS);
												biodataCalonMahasiswa.setKeterangan(
														"Dinyatakan lulus / diterima otomatis karena ikut ujian online");
												biodataCalonMahasiswa.setGelombangPendaftaranDiterima(
														biodataCalonMahasiswa.getGelombangPendaftaran());
												biodataCalonMahasiswa.setJenisSeleksiDipilih(
														biodataCalonMahasiswa.getJenisSeleksi());
												Common.refreshSaveOrUpdate(session, biodataCalonMahasiswa);

												if (biodataCalonMahasiswa.getProdiLulus() != null) {

													Common.createDefaultTimer(new EventListener() {

														@Override
														public void onEvent(Event arg0) throws Exception {
															CommonReportHelper.onCetakSuratKeteranganLulus(
																	biodataCalonMahasiswa, true);
														}
													});

												}
											}

										}

										vbox.appendChild(new MyLabelBolder(Common
												.getKonfigurasi("info_nilia_lulus_ujian_pmb",
														"Nilai yang Anda peroleh adalah : ")
												.getNilai() + " \"" + nilai + "\""));
									}
								}

							} catch (Exception e) {
								e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/pmb/TampilanUjianCalonMahasiswa.java:431");
							}
						}

					}
				});

			}
		};

		Common.createDefaultTimer(eventListener);
		button.addEventListener("onClick", eventListener);
		cari.addEventListener("onOK", eventListener);

		if (tampilTutup) {
			South south = new South();
			ais.ui.util.ZkCompat.setFlex(south, true);
			south.setParent(borderlayout);

			Toolbar toolbar = new Toolbar();
			toolbar.setHeight("35px");
			toolbar.setParent(south);
			MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
			cancel.setTooltiptext("Tutup");
			cancel.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					detach();
				}
			});
			cancel.setParent(toolbar);
			System.out.println(" pertemuans selesai");
		}
	}

}
