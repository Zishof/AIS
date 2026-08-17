package ais.action.master.sekolah.helper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.generic.AmbilDataSiswaBanyak;
import ais.action.report.CommonReportHelper;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.sekolah.KelasSiswa;
import ais.database.model.sekolah.KelasSiswaPunyaSiswa;
import ais.database.model.sekolah.KurikulumPunyaMatapelajaran;
import ais.database.model.sekolah.Matapelajaran;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Siswa;
import ais.ui.util.DataCriteria;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelStyled;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class DetailKelasSiswaHelper implements DataLoader, DataCriteria {

	private MyGrid grid;
	// private Siswa siswa;
	private KelasSiswa kelasSiswa;
	private boolean delete = false;

	private Textbox nama;
	private Intbox angkatan;
	private boolean create;

	private Paging paging;

	public DetailKelasSiswaHelper() {
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		create = CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE);

		paging = new Paging();
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(arg0);
			}
		});
	}

	class DetailPARenderer extends ais.ui.util.MyRowRenderer {

		public DetailPARenderer() {

		}

		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");
			final KelasSiswaPunyaSiswa kelasSiswaPunyaSiswa = (KelasSiswaPunyaSiswa) data;
			final Siswa siswa = kelasSiswaPunyaSiswa.getSiswa();

			if (kelasSiswaPunyaSiswa.getKelasSiswa().getTahunAjaran().equals(Common.getCurrentTahunAkademik())) {

				if (siswa.getKelas() == null) {
					siswa.setKelas(kelasSiswaPunyaSiswa.getKelasSiswa());
					Common.refreshUpdate(siswa);
				}
			}

			CommonMedia.tampilkanGambarKecil(siswa).setParent(row);
			RevisiHelper.createNewRevisi(Siswa.class, siswa, siswa.getNomorInduk()).setParent(row);

			new Label(siswa.getNama()).setParent(row);
			new Label(siswa.getTahunMasuk() + "").setParent(row);

			final Intbox nomorUrut = new Intbox(kelasSiswaPunyaSiswa.getNomorUrut());
			nomorUrut.setParent(row);
			nomorUrut.setWidth("90%");

			nomorUrut.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					kelasSiswaPunyaSiswa.setNomorUrut(nomorUrut.getValue());
					Common.refreshUpdate(kelasSiswaPunyaSiswa);
				}
			});

			List<Long> longs = kelasSiswaPunyaSiswa.ambilMk();
			String mp = "";
			for (Long key : longs) {
				Matapelajaran matapelajaran = (Matapelajaran) ConstantValues.ambil(Matapelajaran.class.getName(), key);
				if (matapelajaran != null) {
					mp += mp.isEmpty() ? matapelajaran.getNama() : ", " + matapelajaran.getNama();
				}
			}

			new Label(mp).setParent(row);

			Hbox toolbar = new Hbox();

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Edit", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Edit Data");
			button.setOrient("vertical");
			button.setVisible(delete);
			button.addEventListener("onClick", new EventListener() {
				@SuppressWarnings({ "deprecation", "unchecked" })
				@Override
				public void onEvent(Event event) throws Exception {

					final MyWindow addWindow = new MyWindow("Pilih Matapelajaran yang tidak diikuti", "none", true);
					addWindow.setWidth("450px");
					addWindow.setHeight("95%");
					addWindow.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());

					Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
					borderlayout.setParent(addWindow);
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
					column.setWidth("30%");

					column = new MyColumnConfig();
					column.setParent(columns);

					Rows rows = new Rows();
					rows.setParent(grid);

					MyFormRow row = new MyFormRow();
					row.setValign("top");
					row.setParent(rows);
					row.appendChild(new ais.ui.util.MyLabelConfig("Nama Kurikulum Sekolah"));
					row.appendChild(new Label(kelasSiswaPunyaSiswa.getKelasSiswa().getKurikulumSekolah().getNama()));

					row = new MyFormRow();
					row.setParent(rows);
					ais.ui.util.ZkCompat.setSpans(row, "2");
					row.appendChild(new MyLabelStyled("Matapelajaran"));

					row = new MyFormRow();
					row.setParent(rows);
					ais.ui.util.ZkCompat.setSpans(row, "2");

					Grid myGrid = new Grid();
					myGrid.setParent(row);
					Rows myrows = new Rows();
					myrows.setParent(myGrid);

					List<Long> longs = kelasSiswaPunyaSiswa.ambilMk();

					Session session = HibernateUtil.currentSession();
					List<Matapelajaran> matapelajarans = ConstantValues.simpleList(session
							.createCriteria(KurikulumPunyaMatapelajaran.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
							.setProjection(Projections.property("matapelajaran.id"))
							.add(Restrictions.eq("kurikulumSekolah",
									kelasSiswaPunyaSiswa.getKelasSiswa().getKurikulumSekolah()))
							.createAlias("matapelajaran", "matapelajaran")

							.add(longs == null || longs.isEmpty() ? Restrictions.sqlRestriction("true")
									: Restrictions.not(Restrictions.in("matapelajaran.id", longs)))

							.add(Restrictions.eq("matapelajaran.aktif", true))
							.addOrder(Order.asc("matapelajaran.urutan")), Matapelajaran.class, false);

					final List<Checkbox> checkboxs = new ArrayList<Checkbox>();
					for (Matapelajaran matapelajaran : matapelajarans) {

						MyFormRow rowData1 = new MyFormRow();
						rowData1.setParent(myrows);

						Checkbox checkbox = new Checkbox(matapelajaran.getNama());
						checkbox.setAttribute("matapelajaran", matapelajaran);
						checkbox.setChecked(longs.contains(matapelajaran.getId()));
						checkboxs.add(checkbox);
						rowData1.appendChild(checkbox);

					}

					South south = new South();
					ais.ui.util.ZkCompat.setFlex(south, true);
					south.setParent(borderlayout);

					Toolbar toolbar = new Toolbar();
					// toolbar.setHeight("25px");
					toolbar.setParent(south);
					MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
					cancel.setTooltiptext("Tutup");
					cancel.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {
							addWindow.detach();
						}
					});
					cancel.setParent(toolbar);
					MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
					save.setTooltiptext("Simpan");
					save.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {

							JSONArray array = new JSONArray();
							for (Checkbox checkbox : checkboxs) {
								if (checkbox.isChecked()) {
									Matapelajaran matapelajaran = (Matapelajaran) checkbox
											.getAttribute("matapelajaran");
									array.put(matapelajaran.getId());
								}
							}
							Session session = HibernateUtil.currentSession();
							session.refresh(kelasSiswaPunyaSiswa);
							kelasSiswaPunyaSiswa.setMpYgTidakDiambil(array.toString());

							Common.refreshUpdate(session, kelasSiswaPunyaSiswa);
							session.flush();

							addWindow.detach();

							Common.createDefaultTimer(new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									loadData(null);
								}
							});
						}
					});
					save.setParent(toolbar);
					borderlayout.setParent(addWindow);

					addWindow.onModal();
				}

			});
			button.setParent(toolbar);

			button = new MyToolbarbuttonConfig("Hapus", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
			button.setOrient("vertical");
			button.setVisible(delete);
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

											if (kelasSiswaPunyaSiswa.getKelasSiswa().getTahunAjaran()
													.equals(Common.getCurrentTahunAkademik())) {

												siswa.setKelas(null);

												Common.refreshUpdate(siswa);
											}
											Common.refreshDelete(kelasSiswaPunyaSiswa);

											Common.createDefaultTimer(new EventListener() {

												@Override
												public void onEvent(Event arg0) throws Exception {
													loadData(null);
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
			button.setParent(toolbar);

			toolbar.setParent(row);

		}

	}

	public Criteria initCriteria(boolean order) {

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(KelasSiswaPunyaSiswa.class)

				.add(Restrictions.eq("kelasSiswa", kelasSiswa))

				.createAlias("siswa", "siswa")

				.add(nama == null || nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(
								Restrictions.ilike("siswa.namaSiswa", nama.getValue().trim(), MatchMode.ANYWHERE),
								Restrictions.or(
										Restrictions.ilike("siswa.nomorInduk", nama.getValue().trim(),
												MatchMode.ANYWHERE),
										Restrictions.ilike("siswa.nomorIndukNasional", nama.getValue().trim(),
												MatchMode.ANYWHERE))))
				.add(angkatan == null || angkatan.getValue() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("siswa.tahunMasuk", angkatan.getValue()));

		if (order) {
			criteria.addOrder(Order.asc("nomorUrut")).addOrder(Order.asc("siswa.namaSiswa"))
					.addOrder(Order.desc("siswa.id"));
		}

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void loadData(Object value) {
		Common.initPaging(initCriteria(false), paging);
		List<KelasSiswaPunyaSiswa> siswa = ConstantValues.simpleList(
				initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
						.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())),
				KelasSiswaPunyaSiswa.class);

		ListModel strset = new SimpleListModel(siswa);
		grid.setRowRenderer(new DetailPARenderer());
		grid.setModelCheckMobile(strset);

	}

	public void displayDetailPA(final KelasSiswa kelasSiswa, final Component component, final MyWindow window) {

		this.kelasSiswa = kelasSiswa;
		Common.clear(component);

		ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
		groupbox.setStyle("min-height: 200px;");
		groupbox.setParent(component);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(groupbox);
		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Siswa : ")));
		toolbar.appendChild(nama = new Textbox());
		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Angkatan : ")));
		toolbar.appendChild(angkatan = new Intbox());

		nama.addEventListener("onOK", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				loadData(null);
			}

		});
		angkatan.addEventListener("onOK", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				loadData(null);
			}

		});

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Absensi", "/img/print.png");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				CommonReportHelper.onLaporanAbsensi(kelasSiswa, false);
			}

		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Ambil Siswa", "/img/new.gif");
		button.setVisible(create);
		button.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {

				Session session = HibernateUtil.currentSession();
				List<Siswa> siswas = session.createCriteria(KelasSiswaPunyaSiswa.class)
						.setProjection(Projections.groupProperty("siswa"))
						.add(Restrictions.eq("kelasSiswa", kelasSiswa)).list();

				AmbilDataSiswaBanyak ambilDataSiswaBanyak = new AmbilDataSiswaBanyak(siswas);
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(ambilDataSiswaBanyak);
				ambilDataSiswaBanyak.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						List<Siswa> siswas = (List<Siswa>) arg0.getData();

						Session session = HibernateUtil.currentSession();
						for (Siswa siswa : siswas) {

							KelasSiswaPunyaSiswa kelasSiswaPunyaSiswa = (KelasSiswaPunyaSiswa) session
									.createCriteria(KelasSiswaPunyaSiswa.class)
									.add(Restrictions.eq("kelasSiswa", kelasSiswa)).add(Restrictions.eq("siswa", siswa))
									.setMaxResults(1).uniqueResult();
							if (kelasSiswaPunyaSiswa == null) {
								kelasSiswaPunyaSiswa = new KelasSiswaPunyaSiswa();
							}

							kelasSiswaPunyaSiswa.setKelasSiswa(kelasSiswa);
							kelasSiswaPunyaSiswa.setSiswa(siswa);
							Common.refreshSaveOrUpdate(kelasSiswaPunyaSiswa);

							if (kelasSiswa.getTahunAjaran().equals(Common.getCurrentTahunAkademik())) {
								siswa.setKelas(kelasSiswa);
								Common.refreshSaveOrUpdate(siswa);
							}
						}

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								loadData(null);
							}
						});
					}
				});
				ambilDataSiswaBanyak.setWidth("850px");
				ambilDataSiswaBanyak.setHeight("97%");
				ambilDataSiswaBanyak.setVisible(true);
				ambilDataSiswaBanyak.onModal();

			}

		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Bersihkan", "/img/svg/trash.svg");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {

				MyMessageboxConfig.show("Apakah yakin ingin menghapus semua data ini ?", "Pertanyaan",
						MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {

							@SuppressWarnings("unchecked")
							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {
									try {

										Session session = HibernateUtil.currentSession();
										List<KelasSiswaPunyaSiswa> kelasSiswaPunyaSiswas = session
												.createCriteria(KelasSiswaPunyaSiswa.class)
												.add(Restrictions.eq("kelasSiswa", kelasSiswa)).list();
										for (KelasSiswaPunyaSiswa kelasSiswaPunyaSiswa : kelasSiswaPunyaSiswas) {
											session.delete(kelasSiswaPunyaSiswa);
											session.flush();
										}

										Common.createDefaultTimer(new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {
												loadData(null);
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
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Copy siswa dari kelas lain", "/img/svg/edit-copy.svg");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {

				final MyWindow window = new MyWindow("Copy siswa dari kelas lain", "none", true);
				window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				window.setHeight("200px");
				window.setWidth("300px");

				Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
				borderlayout.setParent(window);

				Center center = new Center();
				ais.ui.util.ZkCompat.setFlex(center, true);
				center.setParent(borderlayout);

				MyGrid grid = new MyGrid();
				grid.setWidth("100%");
				grid.setParent(center);
				grid.setWidth("100%");
				grid.setHeight("100%");

				Columns columns = new Columns();
				columns.setParent(grid);

				MyColumnConfig column = new MyColumnConfig();
				column.setParent(columns);
				column.setWidth("30%");

				column = new MyColumnConfig();
				column.setParent(columns);

				Rows rows = new Rows();
				rows.setParent(grid);

				MyFormRow row = new MyFormRow();
				row.setValign("top");
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Ajaran * "));
				final Combobox searchta;
				row.appendChild(searchta = Common.generateTahunAjaran(null));
				searchta.setWidth("90%");

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Kelas *"));
				final Combobox searchkelas;
				row.appendChild(searchkelas = new Combobox());
				searchkelas.setWidth("90%");

				EventListener kelasEvent = new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Tbmuser tbmuser = Common.getCurrentUser();
						String ta = (String) searchta.getSelectedItem().getValue();
						Sekolah s = tbmuser == null ? null : tbmuser.ambilSekolah();
						System.out.println("s => " + s);
						Common.insertComboDanSemua(searchkelas, new String[] { "nama", "tahunAjaran", "ruang" },
								"keterangan", KelasSiswa.class,
								Restrictions.and(Restrictions.eq("tahunAjaran", ta),
										Restrictions.and(
												Restrictions.or(Restrictions.isNull("sekolah"),
														s == null ? Restrictions.sqlRestriction("true")
																: Restrictions.eq("sekolah", s)),
												Restrictions.or(Restrictions.isNull("aktif"),
														Restrictions.eq("aktif", true)))));

						Common.selectComboItem(searchkelas, null);
					}
				};

				kelasEvent.onEvent(null);
				searchta.addEventListener("onChange", kelasEvent);

				South south = new South();
				ais.ui.util.ZkCompat.setFlex(south, true);
				south.setParent(borderlayout);

				Toolbar toolbar = new Toolbar();
				// toolbar.setHeight("25px");
				toolbar.setParent(south);
				MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
				cancel.setTooltiptext("Tutup");
				cancel.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						window.detach();
					}
				});
				cancel.setParent(toolbar);
				MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Copy data siswa", "/img/save.gif");
				save.setTooltiptext("Simpan");
				save.addEventListener("onClick", new EventListener() {
					@SuppressWarnings("unchecked")
					@Override
					public void onEvent(Event event) throws Exception {

						KelasSiswa kelasSiswaPilih = (KelasSiswa) (searchkelas.getSelectedItem() == null ? null
								: searchkelas.getSelectedItem().getValue());

						if (kelasSiswaPilih == null) {
							MyMessageboxConfig.show("Kelas harus diisi", "Peringatan", MyMessageboxConfig.OK,
									MyMessageboxConfig.INFORMATION);
							return;
						}

						Session session = HibernateUtil.currentSession();
						List<KelasSiswaPunyaSiswa> kelasSiswaPunyaSiswas = session
								.createCriteria(KelasSiswaPunyaSiswa.class)
								.add(Restrictions.eq("kelasSiswa", kelasSiswaPilih))
								.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
								.list();

						for (KelasSiswaPunyaSiswa kelasSiswaPunyaSiswa : kelasSiswaPunyaSiswas) {
							KelasSiswaPunyaSiswa kelasSiswaPunyaSiswaLama = (KelasSiswaPunyaSiswa) session
									.createCriteria(KelasSiswaPunyaSiswa.class)
									.add(Restrictions.eq("kelasSiswa", kelasSiswa))
									.add(Restrictions.eq("siswa", kelasSiswaPunyaSiswa.getSiswa())).setMaxResults(1)
									.uniqueResult();
							if (kelasSiswaPunyaSiswaLama == null) {
								kelasSiswaPunyaSiswaLama = new KelasSiswaPunyaSiswa();
								kelasSiswaPunyaSiswaLama.setSiswa(kelasSiswaPunyaSiswa.getSiswa());
								kelasSiswaPunyaSiswaLama.setKelasSiswa(kelasSiswa);
								session.save(kelasSiswaPunyaSiswaLama);
								session.flush();

							}
						}

						window.detach();

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								loadData(arg0);
							}
						});

					}
				});
				save.setParent(toolbar);

				window.setVisible(true);
				window.onModal();

			}

		});
		button.setParent(toolbar);

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, "siswa.nomorInduk",
				"siswa.nomorIndukNasional", "siswa.namaSiswa", "nomorUrut", "siswa.tahunMasuk", "siswa.sekolah.nama",
				"siswa.sekolah.yayasan", "siswa.statusSiswa");
		toolbar.appendChild(cetakToolbarbutton);

		MyToolbarbuttonConfig upload = new MyToolbarbuttonConfig("Upload " + Common.ukuranLabelFileUpload(),
				"/img/excel.png");
		upload.setUpload(Common.ukuranFileUpload());
		upload.addEventListener("onUpload", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				UploadEvent uploadEvent = (UploadEvent) event;
				Media media = uploadEvent.getMedia();if(!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media))return;
				if (media.getName().toLowerCase().endsWith("xlsx")) {

					InputStream inputStream = media.getStreamData();
					// System.out.println("media = " + media);
					final File file = new File(
							Sessions.getCurrent().getWebApp().getRealPath("/temp/" + media.getName()));
					// System.out.println("file = " + file.getAbsolutePath());
					file.getParentFile().mkdirs();
					FileOutputStream fileOutputStream = new FileOutputStream(file);
					int c;
					while ((c = inputStream.read()) != -1) {
						fileOutputStream.write(c);
					}
					fileOutputStream.close();
					inputStream.close();

					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							uploadDataSiswa(file, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									loadData(arg0);
									Clients.clearBusy();
								}
							});
						}
					}, "Harap tunggu.. sedang melakukan proses upload data..");

				} else {
					MyMessageboxConfig.show(
							"File yang anda upload harus ber-format Excel Open XML Spreadsheet (xlsx). Jika masih menggunakan format lain, buka file excel tersebut, kemudian Save As Excel Open XML Spreadsheet (xlsx). "
									+ media,
							"Error", MyMessageboxConfig.OK, MyMessageboxConfig.ERROR);
				}
			}
		});
		toolbar.appendChild(upload);

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(50);
		grid.getPagingChild().setMold("os");
		grid.setParent(groupbox);

		paging.setParent(groupbox);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Foto");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("NIS");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Angkatan");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("No.Urut");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Matapelajaran yg tidak diambil");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("12%");

		loadData(null);

	}

	public void uploadDataSiswa(final File file, final EventListener eventListener) throws Exception {

		final StringBuilder laporan = new StringBuilder();
		final int[] jumlah = {0, 0}; // [0]=berhasil, [1]=gagal/dilewati

		final Label label = new Label(ais.common.Common.getBahasaConfig("Proses upload data siswa.."));
		Clients.showBusy(label.getValue());
		final Timer timer = new Timer(200);
		timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		timer.setRepeats(true);
		timer.addEventListener("onTimer", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Clients.showBusy(label.getValue());
				if (label.getValue().isEmpty()) {
					timer.detach();
					Clients.clearBusy();
					String isi = "Laporan Upload Siswa ke Kelas\n"
							+ "==============================\n"
							+ "Berhasil : " + jumlah[0] + " siswa\n"
							+ "Gagal    : " + jumlah[1] + " baris\n\n"
							+ laporan.toString();
					try {
						org.zkoss.zul.Filedownload.save(isi.getBytes("UTF-8"), "text/plain", "laporan_upload_siswa_kelas.txt");
					} catch (Exception ex) {
						ex.printStackTrace();
					}
					MyMessageboxConfig.show(
							"Upload selesai.\nBerhasil: " + jumlah[0] + " siswa, Gagal/Dilewati: " + jumlah[1]
									+ " baris.\nLaporan rinci telah diunduh.",
							"Pemberitahuan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, eventListener);
				}
			}
		});
		timer.start();

		new Thread(new Runnable() {

			@Override
			public void run() {

				Session session = null;
				try {

					XSSFWorkbook workbook = new XSSFWorkbook(file.getAbsolutePath());
					XSSFSheet sheet = workbook.getSheetAt(0);

					// Gunakan openSession() bukan currentNativeSession(): Common.getSheetContentAsObject()
					// memanggil HibernateUtil.closeSession() yang menutup native session ThreadLocal.
					// openSession() tidak disimpan di ThreadLocal sehingga aman; ditutup di finally.
					session = HibernateUtil.openSession();

					int rowCount = (sheet.getLastRowNum() + 1);
					for (int i = 1; i < rowCount; i++) {
						try {

							Siswa siswa = (Siswa) Common.getSheetContentAsObject(sheet, 0, i, Siswa.class);
							Integer nomorUrut = Common.getSheetContentAsInteger(sheet, 3, i);
							if (siswa != null && siswa.getId() != null) {
								// Reload ke session khusus thread agar tidak ada cross-session cascade error.
								// KelasSiswaPunyaSiswa.kelasSiswa dan .siswa punya cascade=PERSIST,MERGE;
								// meng-set entitas dari session lain memicu TransientObjectException yang
								// ditelan diam-diam oleh CommonHibernateHelper.isStaleOrMissingRow().
								Siswa siswaSafe = (Siswa) session.get(Siswa.class, siswa.getId());
								KelasSiswa kelasSiswaSafe = (KelasSiswa) session.get(KelasSiswa.class, kelasSiswa.getId());
								if (siswaSafe == null || kelasSiswaSafe == null) {
									laporan.append("Baris ").append(i).append(": GAGAL - siswa/kelas tidak ditemukan di DB\n");
									jumlah[1]++;
									continue;
								}

								KelasSiswaPunyaSiswa ksps = (KelasSiswaPunyaSiswa) session
										.createCriteria(KelasSiswaPunyaSiswa.class)
										.createAlias("kelasSiswa", "kelasSiswa")
										.add(Restrictions.eq("kelasSiswa.tahunAjaran", kelasSiswaSafe.getTahunAjaran()))
										.add(Restrictions.eq("siswa", siswaSafe)).setMaxResults(1).uniqueResult();
								if (ksps == null) {
									ksps = new KelasSiswaPunyaSiswa();
								}
								ksps.setNomorUrut(nomorUrut);
								ksps.setKelasSiswa(kelasSiswaSafe);
								ksps.setSiswa(siswaSafe);
								session.getTransaction().begin();
								Common.refreshSaveOrUpdate(session, ksps);
								session.getTransaction().commit();

								if (kelasSiswaSafe.getTahunAjaran().equals(Common.getCurrentTahunAkademik())) {
									session.getTransaction().begin();
									session.createSQLQuery(
											"update sekolah.siswa set current_kelas_id=" + kelasSiswaSafe.getId()
													+ " where id=" + siswaSafe.getId())
											.executeUpdate();
									session.getTransaction().commit();
								}

								laporan.append("Baris ").append(i).append(": OK - ")
										.append(siswaSafe.getNim()).append(" ").append(siswaSafe.getNama()).append("\n");
								jumlah[0]++;
								label.setValue("Upload \"" + siswaSafe.getNim() + " - " + siswaSafe.getNama() + "\" ("
										+ Common.numberFormat.get().format(i * 100.0 / rowCount) + " %)");
							} else {
								String cellVal = Common.getCellContent(Common.getCell(sheet, 0, i));
								if (cellVal != null && !cellVal.trim().isEmpty()) {
									laporan.append("Baris ").append(i).append(": DILEWATI - NIS '")
											.append(cellVal).append("' tidak ditemukan\n");
									jumlah[1]++;
								}
							}

						} catch (Exception e) {
							laporan.append("Baris ").append(i).append(": ERROR - ").append(e.getMessage()).append("\n");
							jumlah[1]++;
							Common.tampilErrorJikaAdmin(e);
						}

					}
				} catch (Exception e1) {
					laporan.append("ERROR FATAL: ").append(e1.getMessage()).append("\n");
					e1.printStackTrace();
					ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/sekolah/helper/DetailKelasSiswaHelper.java:uploadDataSiswa");
				} finally {
					HibernateUtil.closeSessionQuietly(session);
					HibernateUtil.closeSession();
				}

				label.setValue("");
			}
		}).start();
	}

}
