package ais.action.master.helper;


import ais.common.CommonSearchFilterHelper;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import ais.ui.util.MyCaptionStyled;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.PesanFormalHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Perkuliahan;
import ais.database.model.TemplatePerkuliahan;
import ais.database.model.TemplatePerkuliahanDetail;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyIframe;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class TemplatePerkuliahanDetailHelper {

	private Boolean add = false;
	private Boolean delete = false;

	public TemplatePerkuliahanDetailHelper() {
		add = CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
	}

	public void display(final TemplatePerkuliahan templatePerkuliahan, Component component) {

		ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
		groupbox.setStyle("min-height: 200px;");
		groupbox.setParent(component);

		groupbox.appendChild(new MyCaptionStyled("Semester"));

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(groupbox);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("40px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Semester");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jumlah Jadwal");
		column.setWidth("30%");

		Rows rows = new Rows();
		rows.setParent(grid);

		for (int i = 1; i <= templatePerkuliahan.getJurusan().getJenjang().getJumlahSemester(); i++) {

			final int semester = i;
			MyFormRow row = new MyFormRow();row.setValign("top");
			row.setParent(rows);

			final Label jumlah = new Label();
			final Timer timer = new Timer(500);
			final EventListener jumlahEventListener = new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					timer.detach();
					Session session = HibernateUtil.currentSession();
					Integer count = ((Number) session.createCriteria(TemplatePerkuliahanDetail.class)
							.add(Restrictions.eq("templatePerkuliahan", templatePerkuliahan))
							.add(Restrictions.eq("semester", semester)).setProjection(Projections.rowCount())
							.uniqueResult()).intValue();
					jumlah.setValue(Common.numberFormat.get().format(count));
				}
			};

			final MyDetail detail = new MyDetail();
			detail.setParent(row);

			detail.addEventListener("onOpen", new EventListener() {

				private EventListener getThis() {
					return this;
				}

				@Override
				public void onEvent(Event arg0) throws Exception {
					Common.clear(detail);
					if (detail.isOpen()) {
						ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
						groupbox.setStyle("min-height: 200px;");
						groupbox.setParent(detail);
						groupbox.appendChild(new MyCaptionStyled("Daftar Template Jadwal Perkuliahan"));

						Toolbar toolbar = new Toolbar();
						toolbar.setParent(groupbox);
						// toolbar.setHeight("25px");
						toolbar.setParent(groupbox);
						MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Import Jadwal Perkuliahan",
								"/img/settings_16x16.png");
						button.setVisible(add);
						button.addEventListener("onClick", new EventListener() {

							@SuppressWarnings("deprecation")
							@Override
							public void onEvent(Event event) throws Exception {

								final Combobox fakultas;
								final Combobox jurusan;

								fakultas = new Combobox();
								jurusan = new Combobox();
								Common.initFakultasDanJurusan(fakultas, jurusan, null, null);

								final MyWindow window = new MyWindow("Import Jadwal Perkuliahan", "normal", true);
								ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);
								window.setWidth("850px");
								window.setHeight("90%");

								Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
								borderlayout.setParent(window);

								Center center = new Center();
								center.setParent(borderlayout);
								ais.ui.util.ZkCompat.setFlex(center, true);

								MyGrid grid = new MyGrid();
								grid.setWidth("100%");
								grid.setParent(center);

								Columns columns = new Columns();

								columns.setParent(grid);

								MyColumnConfig column = new MyColumnConfig();
								column.setParent(columns);
								column.setLabel("");
								column.setWidth("25%");

								column = new MyColumnConfig();
								column.setParent(columns);
								column.setLabel("");

								Rows rows = new Rows();
								rows.setParent(grid);

								final Combobox tahunAjaran = new Combobox();
								MyFormRow row = new MyFormRow();row.setValign("top");
								row.setParent(rows);
								row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
								Common.generateTahunAjaran(tahunAjaran);
								row.appendChild(tahunAjaran);
								tahunAjaran.setWidth("90%");

								row = new MyFormRow();
								row.setParent(rows);
								row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
								row.appendChild(fakultas);
								fakultas.setWidth("90%");

								row = new MyFormRow();
								row.setParent(rows);
								row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
								row.appendChild(jurusan);
								jurusan.setWidth("90%");

								final Html html = new ais.ui.util.MyHtml("<ol>");

								row = new MyFormRow();
								row.setParent(rows);
								ais.ui.util.ZkCompat.setSpans(row, "2");
								Hbox hbox = new Hbox();
								hbox.setParent(row);
								MyToolbarbuttonConfig button = new MyToolbarbuttonConfig(
										"Proses Import Jadwal Perkuliahan", "/img/settings_16x16.png");
								button.setParent(hbox);
								button.addEventListener("onClick", new EventListener() {
									@SuppressWarnings("unchecked")
									@Override
									public void onEvent(Event event) throws Exception {

										if (tahunAjaran.getSelectedItem() == null) {
											MyMessageboxConfig.show("Tahun Akademik harus diisi", "Peringatan", 1,
													MyMessageboxConfig.INFORMATION);
											return;
										}

										Session session = HibernateUtil.currentSession();

										List<Perkuliahan> perkuliahans = session.createCriteria(Perkuliahan.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
												.add(Restrictions.eq("tahunAjaran",
														tahunAjaran.getSelectedItem().getValue()))
												.add(Restrictions.eq("semester", semester))

												.add(CommonSearchFilterHelper.eqSelectedWithId("jurusan", jurusan, false))

												.createCriteria("jurusan")
												.add(CommonSearchFilterHelper.eqSelectedWithId("fakultas", fakultas, false))

												.list();

										html.setContent("<h1>Log Proses Import</h1><ol>");

										for (Perkuliahan perkuliahan : perkuliahans) {
											processImport(perkuliahan, templatePerkuliahan, html);

										}

										String content = html.getContent();
										content += "</ol>";
										html.setContent(content);

										jumlahEventListener.onEvent(event);
										getThis().onEvent(event);
									}
								});

								row = new MyFormRow();
								row.setParent(rows);
								ais.ui.util.ZkCompat.setSpans(row, "2");
								row.appendChild(html);

								South south = new South();
								ais.ui.util.ZkCompat.setFlex(south, true);
								south.setParent(borderlayout);

								Toolbar toolbar = new Toolbar();
								// toolbar.setHeight("25px");
								toolbar.setParent(south);
								MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
								cancel.setTooltiptext("Tutup");
								cancel.addEventListener("onClick", new EventListener() {
									@Override
									public void onEvent(Event event) throws Exception {
										window.detach();
									}
								});
								cancel.setParent(toolbar);

								window.onModal();

							}

						});
						button.setParent(toolbar);

						button = new MyToolbarbuttonConfig("Eksport Jadwal Perkuliahan", "/img/settings_16x16.png");
						button.setVisible(add);
						button.addEventListener("onClick", new EventListener() {

							@SuppressWarnings("deprecation")
							@Override
							public void onEvent(Event event) throws Exception {

								final Combobox fakultas;
								final Combobox jurusan;

								fakultas = new Combobox();
								jurusan = new Combobox();
								Common.initFakultasDanJurusan(fakultas, jurusan, null, null);

								final MyWindow window = new MyWindow("Eksport Jadwal Perkuliahan", "normal", true);
								ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);
								window.setWidth("850px");
								window.setHeight("90%");

								Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
								borderlayout.setParent(window);

								Center center = new Center();
								center.setParent(borderlayout);
								ais.ui.util.ZkCompat.setFlex(center, true);

								MyGrid grid = new MyGrid();
								grid.setWidth("100%");
								grid.setParent(center);

								Columns columns = new Columns();

								columns.setParent(grid);

								MyColumnConfig column = new MyColumnConfig();
								column.setParent(columns);
								column.setLabel("");
								column.setWidth("25%");

								column = new MyColumnConfig();
								column.setParent(columns);
								column.setLabel("");

								Rows rows = new Rows();
								rows.setParent(grid);

								final Combobox tahunAjaran = new Combobox();
								MyFormRow row = new MyFormRow();row.setValign("top");
								row.setParent(rows);
								row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
								Common.generateTahunAjaran(tahunAjaran);
								row.appendChild(tahunAjaran);
								tahunAjaran.setWidth("90%");

								row = new MyFormRow();
								row.setParent(rows);
								row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
								row.appendChild(fakultas);
								fakultas.setWidth("90%");

								row = new MyFormRow();
								row.setParent(rows);
								row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
								row.appendChild(jurusan);
								jurusan.setWidth("90%");

								final MyCheckboxConfig bersihkan;
								row = new MyFormRow();
								row.setParent(rows);
								row.appendChild(new ais.ui.util.MyLabelConfig(""));
								row.appendChild(bersihkan = new MyCheckboxConfig(
										"Bersihkan jadwal matakuliah tujuan sebelum di export"));

								final Html html = new ais.ui.util.MyHtml("<ol>");

								row = new MyFormRow();
								row.setParent(rows);
								ais.ui.util.ZkCompat.setSpans(row, "2");
								Hbox hbox = new Hbox();
								hbox.setParent(row);
								MyToolbarbuttonConfig button = new MyToolbarbuttonConfig(
										"Proses Eksport Jadwal Perkuliahan", "/img/settings_16x16.png");
								button.setParent(hbox);
								button.addEventListener("onClick", new EventListener() {
									@SuppressWarnings("unchecked")
									@Override
									public void onEvent(Event event) throws Exception {

										if (tahunAjaran.getSelectedItem() == null) {
											MyMessageboxConfig.show("Tahun Akademik harus diisi", "Peringatan", 1,
													MyMessageboxConfig.INFORMATION);
											return;
										}

										html.setContent("<h1>Log Proses Eksport</h1><ol>");

										if (bersihkan.isChecked()) {

											Session mySession = HibernateUtil.currentNativeSession();

											List<Perkuliahan> perkuliahans = mySession.createCriteria(Perkuliahan.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
													.add(Restrictions.eq("tahunAjaran",
															tahunAjaran.getSelectedItem().getValue()))
													.add(Restrictions.eq("semester", semester))

													.add(CommonSearchFilterHelper.eqSelectedWithId("jurusan", jurusan, false))

													.createCriteria("jurusan")
													.add(fakultas.getSelectedItem() == null
															? Restrictions.sqlRestriction("1=1")
															: CommonSearchFilterHelper.eqSelectedWithId("fakultas", fakultas, false))

													.list();

											for (Perkuliahan perkuliahan : perkuliahans) {
												Common.hapusPerkuliahan(perkuliahan, null, true, html);
											}

											HibernateUtil.closeSession();
										}

										Session session = HibernateUtil.currentSession();

										List<TemplatePerkuliahanDetail> templatePerkuliahanDetails = session
												.createCriteria(TemplatePerkuliahanDetail.class)
												.add(Restrictions.eq("templatePerkuliahan", templatePerkuliahan))
												.add(Restrictions.eq("semester", semester))

												.add(CommonSearchFilterHelper.eqSelectedWithId("jurusan", jurusan, false))

												.createCriteria("jurusan")
												.add(CommonSearchFilterHelper.eqSelectedWithId("fakultas", fakultas, false))

												.list();

										for (TemplatePerkuliahanDetail templatePerkuliahanDetail : templatePerkuliahanDetails) {
											processEksport(templatePerkuliahanDetail,
													tahunAjaran.getSelectedItem().getValue().toString(), html);

										}

										String content = html.getContent();
										content += "</ol>";
										html.setContent(content);

										jumlahEventListener.onEvent(event);
										getThis().onEvent(event);
									}
								});

								row = new MyFormRow();
								row.setParent(rows);
								ais.ui.util.ZkCompat.setSpans(row, "2");
								row.appendChild(html);

								South south = new South();
								ais.ui.util.ZkCompat.setFlex(south, true);
								south.setParent(borderlayout);

								Toolbar toolbar = new Toolbar();
								// toolbar.setHeight("25px");
								toolbar.setParent(south);
								MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
								cancel.setTooltiptext("Tutup");
								cancel.addEventListener("onClick", new EventListener() {
									@Override
									public void onEvent(Event event) throws Exception {
										window.detach();
									}
								});
								cancel.setParent(toolbar);

								window.onModal();

							}

						});
						button.setParent(toolbar);

						button = new MyToolbarbuttonConfig("Hapus Semua Jadwal", "/img/svg/trash.svg");
						button.setVisible(delete);
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
													try {

														String sql = "delete from template_perkuliahan_detail where template_perkuliahan = "
																+ templatePerkuliahan.getId() + " and semester = "
																+ semester;

														HibernateUtil.currentSession().createSQLQuery(sql)
																.executeUpdate();

														jumlahEventListener.onEvent(event);
														getThis().onEvent(event);
													} catch (Exception e) {
														Common.tampilErrorJikaAdmin(e);
														PesanFormalHelper.tampilkanGagalException(
																"menghapus data template perkuliahan ini",
																e,
																new String[] {
																		"Periksa apakah data ini masih berelasi dengan data lain (misalnya data perkuliahan atau jadwal yang sudah dibuat dari template) sehingga tidak dapat dihapus.",
																		"Hapus atau lepaskan terlebih dahulu data terkait yang masih berelasi, lalu ulangi proses penghapusan.",
																		"Jika data tetap tidak dapat dihapus, konfirmasikan kebutuhan penghapusan ini kepada Administrator." });
													}

												}

											}
										});

							}

						});
						button.setParent(toolbar);

						Sessions.getCurrent().setAttribute("templatePerkuliahan", templatePerkuliahan);
						Sessions.getCurrent().setAttribute("semester", semester);

						MyIframe include = new MyIframe(
								"/pages/master/template_perkuliahan/template_perkuliahan_detail.zul");
						include.setHeight("750px");
						include.setWidth("100%");
						include.setParent(groupbox);
					}
				}
			});

			new Label(i + "").setParent(row);

			row.appendChild(jumlah);
			ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(timer);
			timer.addEventListener("onTimer", jumlahEventListener);
			timer.start();
		}

	}

	private Perkuliahan processEksport(final TemplatePerkuliahanDetail templatePerkuliahanDetail, String tahunAkademik,
			final Html html) throws Exception {

		if (templatePerkuliahanDetail.getKelas() == null) {
			return null;
		}

		Perkuliahan perkuliahan_paralel = null;
		if (templatePerkuliahanDetail.getPerkuliahan_paralel() != null
				&& templatePerkuliahanDetail.getPerkuliahan_paralel().getId() != null) {
			perkuliahan_paralel = processEksport(templatePerkuliahanDetail.getPerkuliahan_paralel(), tahunAkademik,
					html);
		}

		Perkuliahan perkuliahan = new Perkuliahan();
		perkuliahan.setTahunAjaran(tahunAkademik);
		perkuliahan.setDosen1(templatePerkuliahanDetail.getDosen1());
		perkuliahan.setDosen2(templatePerkuliahanDetail.getDosen2());
		perkuliahan.setHari(templatePerkuliahanDetail.getHari());
		perkuliahan.setJurusan(templatePerkuliahanDetail.getJurusan());
		perkuliahan.setKelas(templatePerkuliahanDetail.getKelas());
		perkuliahan.setKurikulum(templatePerkuliahanDetail.getKurikulum());
		perkuliahan.setMatakuliah(templatePerkuliahanDetail.getMatakuliah());
		perkuliahan.setMerupakan_paralel(templatePerkuliahanDetail.getMerupakan_paralel());
		perkuliahan.setMerupakan_tanpa_dosen(templatePerkuliahanDetail.getMerupakan_tanpa_dosen());
		perkuliahan.setMerupakan_tanpa_jadwal_perkuliahan(
				templatePerkuliahanDetail.getMerupakan_tanpa_jadwal_perkuliahan());
		perkuliahan.setMerupakan_tanpa_ruangan(templatePerkuliahanDetail.getMerupakan_tanpa_ruangan());
		perkuliahan.setPerkuliahan_paralel(perkuliahan_paralel);
		perkuliahan.setProgram(templatePerkuliahanDetail.getProgram());
		perkuliahan.setRuang(templatePerkuliahanDetail.getRuang());
		perkuliahan.setSemester(templatePerkuliahanDetail.getSemester());
		perkuliahan.setTahunAjaran(tahunAkademik);
		perkuliahan.setWaktu(templatePerkuliahanDetail.getWaktu());
		perkuliahan.setWaktuMulai(templatePerkuliahanDetail.getWaktuMulai());
		perkuliahan.setWaktuMulaiD(templatePerkuliahanDetail.getWaktuMulaiD());
		perkuliahan.setWaktuSelesai(templatePerkuliahanDetail.getWaktuSelesai());
		perkuliahan.setWaktuSelesaiD(templatePerkuliahanDetail.getWaktuSelesaiD());
		// perkuliahan.setWarna(templatePerkuliahanDetail.getWarna());
		perkuliahan.setKapasitasKelas(templatePerkuliahanDetail.getKapasitasKelas());
		perkuliahan.setJamPerkuliahan(templatePerkuliahanDetail.getJamPerkuliahan());

		// Double waktuMulaiD = templatePerkuliahanDetail.getWaktuMulaiD();
		// Double waktuSelesaiD = templatePerkuliahanDetail.getWaktuSelesaiD();
		//
		// int s = (Integer) templatePerkuliahanDetail.getSemester();
		//
		// Perkuliahan myPerkuliahan = null;
		//
		// if ((myPerkuliahan = (Common.checkKelasJadwalPerkuliahan(perkuliahan
		// .getId(), templatePerkuliahanDetail.getJurusan(),
		// templatePerkuliahanDetail.getProgram(),
		// templatePerkuliahanDetail.getHari(), waktuMulaiD,
		// waktuSelesaiD, tahunAkademik, s % 2 == 0 ? Perkuliahan.GENAP
		// : Perkuliahan.GANJIL, templatePerkuliahanDetail
		// .getKelas(), (Integer) templatePerkuliahanDetail
		// .getSemester(), templatePerkuliahanDetail
		// .getMatakuliah(), html, null))) != null) {
		// return myPerkuliahan;
		// }
		//
		// if ((myPerkuliahan = (Common.checkJadwalRuangPerkuliahan(perkuliahan
		// .getId(), templatePerkuliahanDetail.getRuang(),
		// templatePerkuliahanDetail.getHari(), waktuMulaiD,
		// waktuSelesaiD, tahunAkademik, s % 2 == 0 ? Perkuliahan.GENAP
		// : Perkuliahan.GANJIL, templatePerkuliahanDetail
		// .getJurusan(), templatePerkuliahanDetail
		// .getMatakuliah(), templatePerkuliahanDetail.getKelas(),
		// html, null))) != null) {
		// return myPerkuliahan;
		// }
		// if ((myPerkuliahan = (Common.checkJadwalDosen(perkuliahan.getId(),
		// templatePerkuliahanDetail.getHari(), waktuMulaiD,
		// waktuSelesaiD, templatePerkuliahanDetail.getDosen1(),
		// tahunAkademik, s % 2 == 0 ? Perkuliahan.GENAP
		// : Perkuliahan.GANJIL, templatePerkuliahanDetail
		// .getJurusan(), templatePerkuliahanDetail
		// .getMatakuliah(), templatePerkuliahanDetail.getKelas(),
		// html, null))) != null) {
		// return myPerkuliahan;
		// }
		//
		// if (!templatePerkuliahanDetail.getMerupakan_paralel()) {
		// if (!templatePerkuliahanDetail.getMerupakan_tanpa_ruangan()
		// && (myPerkuliahan = (Common
		// .checkMatakuliahKesamaanBukanParalel(perkuliahan,
		// templatePerkuliahanDetail.getJurusan(),
		// templatePerkuliahanDetail.getKelas(),
		// templatePerkuliahanDetail.getMatakuliah(),
		// s, tahunAkademik,
		// templatePerkuliahanDetail.getProgram(),
		// html, null))) != null) {
		// return myPerkuliahan;
		// }
		// }

		Perkuliahan myPerkuliahan = checkKeberadaanPerkuliahan(perkuliahan, html);
		if (myPerkuliahan != null) {
			return myPerkuliahan;
		}

		Session session = HibernateUtil.currentNativeSession();
		perkuliahan.populateKurikulumPunyaMatakuliah();
		session.getTransaction().begin();
		Common.refreshUpdate(session, (perkuliahan));

		session.getTransaction().commit();

		HibernateUtil.closeSession();

		return perkuliahan;
	}

	public static Perkuliahan checkKeberadaanPerkuliahan(Perkuliahan perkuliahan, Html html) throws Exception {
		Perkuliahan myPerkuliahan = null;

		Double waktuMulaiD = perkuliahan.getWaktuMulaiD();
		Double waktuSelesaiD = perkuliahan.getWaktuSelesaiD();
		int s = (Integer) perkuliahan.getSemester();

		try {
			if ((myPerkuliahan = (Common.checkKelasJadwalPerkuliahan(perkuliahan.getId(), perkuliahan.getJurusan(),
					perkuliahan.getProgram(), perkuliahan.getHari(), waktuMulaiD, waktuSelesaiD,
					perkuliahan.getTahunAjaran(), s % 2 == 0 ? Perkuliahan.GENAP : Perkuliahan.GANJIL,
					perkuliahan.getKelas(), (Integer) perkuliahan.getSemester(), html, null, perkuliahan.getMinggu1(),
					perkuliahan.getMinggu2(), perkuliahan.getMinggu3(), perkuliahan.getMinggu4(),
					perkuliahan.getMinggu5(), perkuliahan.getPerkuliahanDimulai(), perkuliahan.getPerkuliahanSampai(),
					perkuliahan.getMatakuliah()))) != null) {
				return myPerkuliahan;
			}
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/helper/TemplatePerkuliahanDetailHelper.java:656");
		}

		try {
			if ((myPerkuliahan = (Common.checkJadwalRuangPerkuliahan(perkuliahan.getId(), perkuliahan.getRuang(),
					perkuliahan.getHari(), waktuMulaiD, waktuSelesaiD, perkuliahan.getTahunAjaran(),
					s % 2 == 0 ? Perkuliahan.GENAP : Perkuliahan.GANJIL, html, null, perkuliahan.getMinggu1(),
					perkuliahan.getMinggu2(), perkuliahan.getMinggu3(), perkuliahan.getMinggu4(),
					perkuliahan.getMinggu5(), perkuliahan.getPerkuliahanDimulai(),
					perkuliahan.getPerkuliahanSampai()))) != null) {
				return myPerkuliahan;
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			Common.tampilErrorJikaAdmin(e);
		}
		try {
			if ((myPerkuliahan = (Common.checkJadwalDosen(perkuliahan.getId(), perkuliahan.getHari(), waktuMulaiD,
					waktuSelesaiD, perkuliahan.getDosen1(), perkuliahan.getTahunAjaran(),
					s % 2 == 0 ? Perkuliahan.GENAP : Perkuliahan.GANJIL, perkuliahan.getJurusan(),
					perkuliahan.getMatakuliah(), perkuliahan.getKelas(), html, null, perkuliahan.getMinggu1(),
					perkuliahan.getMinggu2(), perkuliahan.getMinggu3(), perkuliahan.getMinggu4(),
					perkuliahan.getMinggu5(), perkuliahan.getPerkuliahanDimulai(),
					perkuliahan.getPerkuliahanSampai()))) != null) {
				return myPerkuliahan;
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			Common.tampilErrorJikaAdmin(e);
		}

		try {
			if (!perkuliahan.getMerupakan_paralel()) {
				if (!perkuliahan.getMerupakan_tanpa_ruangan()
						&& (myPerkuliahan = (Common.checkMatakuliahKesamaanBukanParalel(perkuliahan,
								perkuliahan.getJurusan(), perkuliahan.getKelas(), perkuliahan.getMatakuliah(), s,
								perkuliahan.getTahunAjaran(), perkuliahan.getProgram(), html, null,
								perkuliahan.getMinggu1(), perkuliahan.getMinggu2(), perkuliahan.getMinggu3(),
								perkuliahan.getMinggu4(), perkuliahan.getMinggu5(), perkuliahan.getPerkuliahanDimulai(),
								perkuliahan.getPerkuliahanSampai(), perkuliahan.getMerupakanRemedial()))) != null) {
					return myPerkuliahan;
				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			Common.tampilErrorJikaAdmin(e);
		}

		return null;
	}

	private TemplatePerkuliahanDetail processImport(final Perkuliahan perkuliahan,
			final TemplatePerkuliahan templatePerkuliahan, final Html html) throws Exception {

		if (perkuliahan.getKelas() == null || perkuliahan.getStatusSemesterPendek() != null) {
			return null;
		}

		TemplatePerkuliahanDetail templatePerkuliahanDetailParalel = null;

		if (perkuliahan != null && perkuliahan.getPerkuliahan_paralel() != null
				&& perkuliahan.getPerkuliahan_paralel().getId() != null) {
			templatePerkuliahanDetailParalel = processImport(perkuliahan.getPerkuliahan_paralel(), templatePerkuliahan,
					html);
		}

		TemplatePerkuliahanDetail templatePerkuliahanDetail = new TemplatePerkuliahanDetail();
		templatePerkuliahanDetail.setPerkuliahan_paralel(templatePerkuliahanDetailParalel);
		templatePerkuliahanDetail.setDosen1(perkuliahan.getDosen1());
		templatePerkuliahanDetail.setDosen2(perkuliahan.getDosen2());
		templatePerkuliahanDetail.setHari(perkuliahan.getHari());
		templatePerkuliahanDetail.setJurusan(perkuliahan.getJurusan());
		templatePerkuliahanDetail.setKelas(perkuliahan.getKelas());
		templatePerkuliahanDetail.setKurikulum(perkuliahan.getKurikulum());
		templatePerkuliahanDetail.setMatakuliah(perkuliahan.getMatakuliah());
		templatePerkuliahanDetail.setMerupakan_paralel(perkuliahan.getMerupakan_paralel());
		templatePerkuliahanDetail.setMerupakan_tanpa_dosen(perkuliahan.getMerupakan_tanpa_dosen());
		templatePerkuliahanDetail
				.setMerupakan_tanpa_jadwal_perkuliahan(perkuliahan.getMerupakan_tanpa_jadwal_perkuliahan());
		templatePerkuliahanDetail.setMerupakan_tanpa_ruangan(perkuliahan.getMerupakan_tanpa_ruangan());

		templatePerkuliahanDetail.setProgram(perkuliahan.getProgram());
		templatePerkuliahanDetail.setRuang(perkuliahan.getRuang());
		templatePerkuliahanDetail.setSemester(perkuliahan.getSemester());
		templatePerkuliahanDetail.setTemplatePerkuliahan(templatePerkuliahan);
		templatePerkuliahanDetail.setWaktu(perkuliahan.getWaktu());
		templatePerkuliahanDetail.setWaktuMulai(perkuliahan.getWaktuMulai());
		templatePerkuliahanDetail.setWaktuMulaiD(perkuliahan.getWaktuMulaiD());
		templatePerkuliahanDetail.setWaktuSelesai(perkuliahan.getWaktuSelesai());
		templatePerkuliahanDetail.setWaktuSelesaiD(perkuliahan.getWaktuSelesaiD());
		// templatePerkuliahanDetail.setWarna(perkuliahan.getWarna());
		templatePerkuliahanDetail.setKapasitasKelas(perkuliahan.getKapasitasKelas());
		templatePerkuliahanDetail.setJamPerkuliahan(perkuliahan.getJamPerkuliahan());

		Double waktuMulaiD = perkuliahan.getWaktuMulaiD();
		Double waktuSelesaiD = perkuliahan.getWaktuSelesaiD();

		int s = (Integer) perkuliahan.getSemester();

		TemplatePerkuliahanDetail temp = null;
		if ((temp = Common.checkKelasJadwalTemplatePerkuliahanDetail(templatePerkuliahan,
				templatePerkuliahanDetail.getId(), perkuliahan.getJurusan(), perkuliahan.getProgram(),
				perkuliahan.getHari(), waktuMulaiD, waktuSelesaiD, s % 2 == 0 ? Perkuliahan.GENAP : Perkuliahan.GANJIL,
				perkuliahan.getKelas(), (Integer) s, perkuliahan.getMatakuliah(), html)) != null) {
			return temp;
		}

		if ((temp = Common.checkJadwalTemplateRuangPerkuliahanDetail(templatePerkuliahan,
				templatePerkuliahanDetail.getId(), perkuliahan.getRuang(), perkuliahan.getHari(), waktuMulaiD,
				waktuSelesaiD, s % 2 == 0 ? Perkuliahan.GENAP : Perkuliahan.GANJIL,
				templatePerkuliahanDetail.getJurusan(), perkuliahan.getMatakuliah(), perkuliahan.getKelas(),
				html)) != null) {
			return temp;
		}
		if ((temp = Common.checkJadwalDosen(templatePerkuliahan, templatePerkuliahanDetail.getId(),
				perkuliahan.getHari(), waktuMulaiD, waktuSelesaiD, perkuliahan.getDosen1(),
				s % 2 == 0 ? Perkuliahan.GENAP : Perkuliahan.GANJIL, templatePerkuliahanDetail.getJurusan(),
				perkuliahan.getMatakuliah(), perkuliahan.getKelas(), html)) != null) {
			return temp;
		}

		if (perkuliahan.getMerupakan_paralel() == null || !perkuliahan.getMerupakan_paralel()) {
			if ((perkuliahan.getMerupakan_tanpa_ruangan() == null || !perkuliahan.getMerupakan_tanpa_ruangan())
					&& (temp = Common.checkMatakuliahKesamaanBukanParalel(templatePerkuliahanDetail,
							templatePerkuliahan, perkuliahan.getJurusan(), perkuliahan.getKelas(),
							perkuliahan.getMatakuliah(), s, perkuliahan.getProgram(), html)) != null) {
				return temp;
			}
		}

		Session session = HibernateUtil.currentNativeSession();

		session.getTransaction().begin();
		if (templatePerkuliahanDetail.getId() != null) {
			Common.refreshUpdate(session, (templatePerkuliahanDetail));
		} else {
			session.save(templatePerkuliahanDetail);
		}
		session.getTransaction().commit();

		HibernateUtil.closeSession();

		return templatePerkuliahanDetail;

	}

}
