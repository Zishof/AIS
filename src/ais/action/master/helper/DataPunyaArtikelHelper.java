package ais.action.master.helper;
import ais.common.PesanFormalHelper;

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
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import ais.ui.util.MyCaptionStyled;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Space;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.action.master.helper.generic.AmbilDataArtikelBanyak;
import ais.action.master.library.helper.AmbilDataDariGoogleScholarBanyak;
import ais.action.master.penelitiandanpengabdian.ArtikelAction;
import ais.common.Common;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.DataPunyaArtikel;
import ais.database.model.JadwalUjianPMB;
import ais.database.model.KurikulumPunyaMatakuliah;
import ais.database.model.MahasiswaRequestTugasAkhir;
import ais.database.model.Perkuliahan;
import ais.database.model.ScholarArticle;
import ais.database.model.Skripsi;
import ais.database.model.Tbmuser;
import ais.database.model.kkn.KelompokKkn;
import ais.database.model.penelitiandanpengabdian.Artikel;
import ais.database.model.pkl.KelompokPkl;
import ais.database.model.sekolah.JadwalPelajaran;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;

public class DataPunyaArtikelHelper implements DataLoader {

	private Grid grid;
	private Skripsi skripsi;

	private Component component;

	private Paging paging;

	private Tbmuser tbmuser;
	private MahasiswaRequestTugasAkhir mahasiswaRequestTugasAkhir;
	private JadwalUjianPMB jadwalUjianPMB;
	private KelompokKkn kelompokKkn;
	private KelompokPkl kelompokPkl;
	private Perkuliahan perkuliahan;
	private String sqltambahan = "false";
	private KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah;
	private JadwalPelajaran jadwalPelajaran;
	private Textbox cari;

	public DataPunyaArtikelHelper() {

		tbmuser = Common.getCurrentUser();
	}

	class DetailSkripsiRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object data) throws Exception {
			final DataPunyaArtikel dataPunyaArtikel = (DataPunyaArtikel) data;
			final Artikel artikel = dataPunyaArtikel.getArtikel();

			DetailArtikelHelper.displayRow(arg0, artikel, null, false);

			Hbox toolbar = new Hbox();

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Kutipan", "/img/eye-icon.png");
			button.setOrient("vertical");
			button.setTooltiptext("Kutipan Data");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					ArtikelAction.tampilkanKutipan(artikel);
				}

			});
			button.setParent(toolbar);

			button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
			button.setVisible(tbmuser != null);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {
											Common.refreshDelete(dataPunyaArtikel);
											loadData(null);

										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											PesanFormalHelper.tampilkanGagalException("Menghapus data", "Data yang Bapak/Ibu coba hapus kemungkinan besar masih memiliki keterkaitan/relasi dengan data lain pada tabel terkait (misalnya digunakan sebagai referensi oleh transaksi, detail, atau riwayat lain), sehingga sistem basis data menolak proses penghapusan ini demi menjaga integritas data secara keseluruhan.", e, new String[]{"Periksa kembali apakah data ini masih digunakan atau direferensikan oleh data lain yang berelasi.", "Hapus atau lepaskan terlebih dahulu keterkaitan/relasi data tersebut sebelum mencoba menghapus data ini kembali.", "Jika Bapak/Ibu yakin data ini seharusnya sudah tidak digunakan lagi, hubungi Administrator untuk pengecekan lebih lanjut."});
										}

									}

								}
							});

				}

			});
			button.setParent(toolbar);
			ais.ui.util.MenuAksiBaris.pasang(toolbar);
			toolbar.setParent(arg0);

		}

	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria crit = session.createCriteria(DataPunyaArtikel.class).createAlias("artikel", "artikel")
				.add(cari.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.or(
								Restrictions.ilike("artikel.judul", cari.getValue().trim(), MatchMode.ANYWHERE),
								Restrictions.or(
										Restrictions.ilike("artikel.keyword", cari.getValue().trim(),
												MatchMode.ANYWHERE),
										Restrictions.ilike("artikel.abstrak", cari.getValue().trim(),
												MatchMode.ANYWHERE))))

				.add(Restrictions.or(Restrictions.eq("jadwalPelajaran", jadwalPelajaran), Restrictions.or(
						Restrictions.eq("perkuliahan", perkuliahan),
						Restrictions.or(Restrictions.sqlRestriction(sqltambahan), Restrictions.or(
								Restrictions.eq("kurikulumPunyaMatakuliah", kurikulumPunyaMatakuliah), Restrictions.or(
										Restrictions.eq("kelompokPkl", kelompokPkl),
										Restrictions.or(Restrictions.eq("kelompokKkn", kelompokKkn),
												Restrictions.or(
														Restrictions.or(
																Restrictions.eq("mahasiswaRequestTugasAkhir",
																		mahasiswaRequestTugasAkhir),
																Restrictions.eq("skripsi", skripsi)),
														Restrictions.eq("jadwalUjianPMB", jadwalUjianPMB)))))))));

		if (order) {
			crit.addOrder(Order.asc("id"));
		}
		return crit;
	}

	@SuppressWarnings("unchecked")
	public void loadData(Object value) {

		Common.initPaging(initCriteria(false), paging);

		List<DataPunyaArtikel> dataPunyaArtikel = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();

		if (component instanceof Tabpanel) {
			((Tabpanel) component).getLinkedTab()
					.setLabel("Artikel " + (dataPunyaArtikel.size() == 0 ? "" : "(" + dataPunyaArtikel.size() + ")"));
		}

		ListModel strset = new SimpleListModel(dataPunyaArtikel);
		grid.setRowRenderer(new DetailSkripsiRenderer());
		grid.setModel(strset);

	}

	public void display(final String sqltambahan, final Component component) {
		this.sqltambahan = sqltambahan;
		display(skripsi, mahasiswaRequestTugasAkhir, jadwalUjianPMB, kelompokKkn, kelompokPkl, perkuliahan,
				kurikulumPunyaMatakuliah, component);
	}

	public void display(final Skripsi skripsi, final MahasiswaRequestTugasAkhir mahasiswaRequestTugasAkhir,
			final JadwalUjianPMB jadwalUjianPMB, final KelompokKkn kelompokKkn, final KelompokPkl kelompokPkl,
			final Perkuliahan perkuliahan, final KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah,
			final Component component) {
		display(skripsi, mahasiswaRequestTugasAkhir, jadwalUjianPMB, kelompokKkn, kelompokPkl, perkuliahan,
				kurikulumPunyaMatakuliah, null, component);
	}

	public void display(final Skripsi skripsi, final MahasiswaRequestTugasAkhir mahasiswaRequestTugasAkhir,
			final JadwalUjianPMB jadwalUjianPMB, final KelompokKkn kelompokKkn, final KelompokPkl kelompokPkl,
			final Perkuliahan perkuliahan, final KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah,
			final JadwalPelajaran jadwalPelajaran, final Component component) {
		this.skripsi = skripsi;
		this.kelompokKkn = kelompokKkn;
		this.kelompokPkl = kelompokPkl;
		this.mahasiswaRequestTugasAkhir = mahasiswaRequestTugasAkhir;
		this.jadwalUjianPMB = jadwalUjianPMB;
		this.perkuliahan = perkuliahan;
		this.kurikulumPunyaMatakuliah = kurikulumPunyaMatakuliah;
		this.jadwalPelajaran = jadwalPelajaran;
		if (component != null) {
			Common.clear(component);
		}
		this.component = component;

		paging = new Paging();
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});
		cari = new Textbox();
		if (component instanceof Tabpanel) {
			ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
			groupbox.setStyle("min-height: 200px;");
			groupbox.setParent(component);
			groupbox.appendChild(new MyCaptionStyled("Daftar Buku Referensi"));

			Toolbar toolbar = new Toolbar();

			// toolbar.setHeight("25px");
			toolbar.setParent(groupbox);
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Ambil Artikel", "/img/new.gif");
			button.setVisible(tbmuser != null && Common.getCurrentUser().getMahasiswa() == null);
			button.addEventListener("onClick", new EventListener() {

				@SuppressWarnings("unchecked")
				@Override
				public void onEvent(Event event) throws Exception {

					List<Artikel> artikels = HibernateUtil.currentSession().createCriteria(DataPunyaArtikel.class)
							.add(Restrictions.or(Restrictions.eq("perkuliahan", perkuliahan),
									Restrictions.or(Restrictions.eq("kelompokPkl", kelompokPkl),
											Restrictions.or(Restrictions.eq("kelompokKkn", kelompokKkn),
													Restrictions.or(
															Restrictions.or(
																	Restrictions.eq("mahasiswaRequestTugasAkhir",
																			mahasiswaRequestTugasAkhir),
																	Restrictions.eq("skripsi", skripsi)),
															Restrictions.eq("jadwalUjianPMB", jadwalUjianPMB))))))
							.setProjection(Projections.property("artikel")).list();
					AmbilDataArtikelBanyak ambilDataArtikelBanyak = new AmbilDataArtikelBanyak(artikels);
					ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(ambilDataArtikelBanyak);
					ambilDataArtikelBanyak.setEventListener(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							List<Artikel> artikels = (List<Artikel>) arg0.getData();
							for (Artikel artikel : artikels) {
								DataPunyaArtikel dataPunyaArtikel = new DataPunyaArtikel();
								dataPunyaArtikel.setArtikel(artikel);
								dataPunyaArtikel.setKeterangan("");
								dataPunyaArtikel.setJadwalUjianPMB(jadwalUjianPMB);
								dataPunyaArtikel.setSkripsi(skripsi);
								dataPunyaArtikel.setKelompokKkn(kelompokKkn);
								dataPunyaArtikel.setPerkuliahan(perkuliahan);
								dataPunyaArtikel.setKelompokPkl(kelompokPkl);
								dataPunyaArtikel.setMahasiswaRequestTugasAkhir(mahasiswaRequestTugasAkhir);
								dataPunyaArtikel.setKurikulumPunyaMatakuliah(kurikulumPunyaMatakuliah);
								dataPunyaArtikel.setJadwalPelajaran(jadwalPelajaran);
								Common.refreshSaveOrUpdate(dataPunyaArtikel);
							}

							loadData(null);
						}
					});
					ambilDataArtikelBanyak.setWidth("97%");
					ambilDataArtikelBanyak.setHeight("97%");
					ambilDataArtikelBanyak.setVisible(true);
					ambilDataArtikelBanyak.onModal();

				}

			});
			button.setParent(toolbar);

			button = new MyToolbarbuttonConfig("Ambil Artikel dari Google Scholar",
					"/img/education-university-icon.png");

			button.setVisible(false);

			button.addEventListener("onClick", new EventListener() {

				@SuppressWarnings("unchecked")
				@Override
				public void onEvent(Event event) throws Exception {

					AmbilDataDariGoogleScholarBanyak ambilDataDariGoogleScholarBanyak = new AmbilDataDariGoogleScholarBanyak(
							perkuliahan != null && perkuliahan.getMatakuliah() != null
									? perkuliahan.getMatakuliah().getNama() : "");
					ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot()
							.appendChild(ambilDataDariGoogleScholarBanyak);
					ambilDataDariGoogleScholarBanyak.setHeight("95%");
					ambilDataDariGoogleScholarBanyak.setWidth("90%");


					ambilDataDariGoogleScholarBanyak.setEventListener(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							List<ScholarArticle> objects = (List<ScholarArticle>) arg0.getData();
							Session session = HibernateUtil.currentSession();
							for (ScholarArticle scholarArticle : objects) {

								try {

									Artikel artikel = (Artikel) session.createCriteria(Artikel.class)
											.add(Restrictions.eq("scholarArticle", scholarArticle)).uniqueResult();
									if (artikel == null) {
										artikel = new Artikel();
										artikel.setScholarArticle(scholarArticle);
										session.save(artikel);
									}

									DataPunyaArtikel dataPunyaArtikel = new DataPunyaArtikel();
									dataPunyaArtikel.setArtikel(artikel);
									dataPunyaArtikel.setKeterangan("");
									dataPunyaArtikel.setJadwalUjianPMB(jadwalUjianPMB);
									dataPunyaArtikel.setSkripsi(skripsi);
									dataPunyaArtikel.setKelompokKkn(kelompokKkn);
									dataPunyaArtikel.setPerkuliahan(perkuliahan);
									dataPunyaArtikel.setKelompokPkl(kelompokPkl);
									dataPunyaArtikel.setMahasiswaRequestTugasAkhir(mahasiswaRequestTugasAkhir);
									dataPunyaArtikel.setKurikulumPunyaMatakuliah(kurikulumPunyaMatakuliah);
									dataPunyaArtikel.setJadwalPelajaran(jadwalPelajaran);
									Common.refreshSaveOrUpdate(dataPunyaArtikel);
								} catch (Exception e) {
									Common.tampilErrorJikaAdmin(e);

								}

							}

							loadData(null);
						}
					});

					ambilDataDariGoogleScholarBanyak.onModal();

				}

			});
			button.setParent(toolbar);

			toolbar.appendChild(new Space());
			toolbar.appendChild(new MyLabelConfig("Cari : "));
			toolbar.appendChild(cari);
			cari.setCols(15);
			cari.addEventListener("onOK", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					loadData(null);
				}
			});
			button = new MyToolbarbuttonConfig("", "/img/svg/search.svg");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					loadData(null);
				}
			});
			button.setParent(toolbar);

			grid = new Grid();// grid.setOddRowSclass("non-odd");
			grid.setWidth("100%");
			grid.setParent(groupbox);

			paging.setParent(groupbox);
		} else {
			grid = new Grid();// grid.setOddRowSclass("non-odd");
			grid.setWidth("100%");
			grid.setParent(component);

			if (component instanceof Center) {
				South south = new South();
				south.setParent(component.getParent());
				paging.setParent(south);

				North north = new North();
				north.setParent(component.getParent());
				ais.ui.util.ZkCompat.setFlex(north, true);
				north.setHeight("25px");

				Toolbar toolbar = new Toolbar();
				// toolbar.setHeight("25px");
				toolbar.setParent(north);

				MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Ambil Artikel", "/img/new.gif");
				button.setVisible(tbmuser != null && Common.getCurrentUser().getMahasiswa() == null);
				button.addEventListener("onClick", new EventListener() {

					@SuppressWarnings("unchecked")
					@Override
					public void onEvent(Event event) throws Exception {

						List<Artikel> artikels = HibernateUtil.currentSession().createCriteria(DataPunyaArtikel.class)
								.add(Restrictions.or(Restrictions.eq("perkuliahan", perkuliahan), Restrictions.or(
										Restrictions.eq("kelompokPkl", kelompokPkl), Restrictions
												.or(Restrictions.eq("kelompokKkn", kelompokKkn), Restrictions.or(
														Restrictions.or(
																Restrictions.eq("mahasiswaRequestTugasAkhir",
																		mahasiswaRequestTugasAkhir),
																Restrictions.eq("skripsi", skripsi)),
														Restrictions.eq("jadwalUjianPMB", jadwalUjianPMB))))))
								.setProjection(Projections.property("artikel")).list();
						AmbilDataArtikelBanyak ambilDataArtikelBanyak = new AmbilDataArtikelBanyak(artikels);
						ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot()
								.appendChild(ambilDataArtikelBanyak);
						ambilDataArtikelBanyak.setEventListener(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								List<Artikel> artikels = (List<Artikel>) arg0.getData();
								for (Artikel artikel : artikels) {
									DataPunyaArtikel dataPunyaArtikel = new DataPunyaArtikel();
									dataPunyaArtikel.setArtikel(artikel);
									dataPunyaArtikel.setKeterangan("");
									dataPunyaArtikel.setJadwalUjianPMB(jadwalUjianPMB);
									dataPunyaArtikel.setSkripsi(skripsi);
									dataPunyaArtikel.setKelompokKkn(kelompokKkn);
									dataPunyaArtikel.setPerkuliahan(perkuliahan);
									dataPunyaArtikel.setKelompokPkl(kelompokPkl);
									dataPunyaArtikel.setMahasiswaRequestTugasAkhir(mahasiswaRequestTugasAkhir);
									dataPunyaArtikel.setKurikulumPunyaMatakuliah(kurikulumPunyaMatakuliah);
									dataPunyaArtikel.setJadwalPelajaran(jadwalPelajaran);
									Common.refreshSaveOrUpdate(dataPunyaArtikel);
								}

								loadData(null);
							}
						});
						ambilDataArtikelBanyak.setWidth("97%");
						ambilDataArtikelBanyak.setHeight("97%");
						ambilDataArtikelBanyak.setVisible(true);
						ambilDataArtikelBanyak.onModal();

					}

				});
				button.setParent(toolbar);

				button = new MyToolbarbuttonConfig("Ambil Artikel dari Google Scholar",
						"/img/education-university-icon.png");
				button.addEventListener("onClick", new EventListener() {

					@SuppressWarnings("unchecked")
					@Override
					public void onEvent(Event event) throws Exception {

						AmbilDataDariGoogleScholarBanyak ambilDataDariGoogleScholarBanyak = new AmbilDataDariGoogleScholarBanyak(
								perkuliahan != null && perkuliahan.getMatakuliah() != null
										? perkuliahan.getMatakuliah().getNama() : "");
						ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot()
								.appendChild(ambilDataDariGoogleScholarBanyak);
						ambilDataDariGoogleScholarBanyak.setHeight("95%");
						ambilDataDariGoogleScholarBanyak.setWidth("90%");

						ambilDataDariGoogleScholarBanyak.setEventListener(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {

								List<ScholarArticle> objects = (List<ScholarArticle>) arg0.getData();
								Session session = HibernateUtil.currentSession();
								for (ScholarArticle scholarArticle : objects) {

									try {

										Artikel artikel = (Artikel) session.createCriteria(Artikel.class)
												.add(Restrictions.eq("scholarArticle", scholarArticle)).uniqueResult();
										if (artikel == null) {
											artikel = new Artikel();
											artikel.setScholarArticle(scholarArticle);
											session.save(artikel);
										}

										DataPunyaArtikel dataPunyaArtikel = new DataPunyaArtikel();
										dataPunyaArtikel.setArtikel(artikel);
										dataPunyaArtikel.setKeterangan("");
										dataPunyaArtikel.setJadwalUjianPMB(jadwalUjianPMB);
										dataPunyaArtikel.setSkripsi(skripsi);
										dataPunyaArtikel.setKelompokKkn(kelompokKkn);
										dataPunyaArtikel.setPerkuliahan(perkuliahan);
										dataPunyaArtikel.setKelompokPkl(kelompokPkl);
										dataPunyaArtikel.setMahasiswaRequestTugasAkhir(mahasiswaRequestTugasAkhir);
										dataPunyaArtikel.setKurikulumPunyaMatakuliah(kurikulumPunyaMatakuliah);
										dataPunyaArtikel.setJadwalPelajaran(jadwalPelajaran);
										Common.refreshSaveOrUpdate(dataPunyaArtikel);
									} catch (Exception e) {
										Common.tampilErrorJikaAdmin(e);

									}

								}

								loadData(null);
							}
						});

						ambilDataDariGoogleScholarBanyak.onModal();

					}

				});
				button.setParent(toolbar);

				toolbar.appendChild(new Space());
				toolbar.appendChild(new MyLabelConfig("Cari : "));
				toolbar.appendChild(cari);
				cari.setCols(15);
				cari.addEventListener("onOK", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						loadData(null);
					}
				});
				button = new MyToolbarbuttonConfig("", "/img/svg/search.svg");
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						loadData(null);
					}
				});
				button.setParent(toolbar);
			}
		}

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("0px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Rincian");
		column.setWidth("35%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Abstrak");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Informasi");
		column.setWidth("0%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("10%");

		loadData(null);

	}

}
