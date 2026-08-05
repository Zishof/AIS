package ais.action.master.sekolah.helper;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.poi.ss.usermodel.Hyperlink;
import org.zkoss.poi.xssf.usermodel.XSSFCell;
import org.zkoss.poi.xssf.usermodel.XSSFCellStyle;
import org.zkoss.poi.xssf.usermodel.XSSFColor;
import org.zkoss.poi.xssf.usermodel.XSSFFont;
import org.zkoss.poi.xssf.usermodel.XSSFHyperlink;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.CommonSearchFilterHelper;
import ais.common.CommonDashboardHtmlHelper;
import ais.common.CommonPrivilages;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.akunting.DaftarPengajuanTransfer;
import ais.database.model.employ.TipeMasaKerja;
import ais.database.model.file.LampiranLain;
import ais.database.model.sekolah.DiskonSiswa;
import ais.database.model.sekolah.DiskonSiswaItemBiaya;
import ais.database.model.sekolah.DiskonSiswaPunyaSiswa;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Tagihan;
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class DiskonSiswaPunyaSiswaHelper implements DataLoader, DataCriteria, DataSearchDefault {

	private MyGrid grid;
	private Html dashboardHtml;
	private Html progressHtml;
	private DiskonSiswa diskonSiswa;
	private Textbox nama;
	private Intbox angkatan;

	private Combobox searchyayasan = new Combobox();
	private Combobox searchsekolah = new Combobox();

	private Paging paging;
	private Tbmuser tbmuser;

	public DiskonSiswaPunyaSiswaHelper() {

		Common.initYayasanDanSekolahDanSemua(null, null, searchyayasan, searchsekolah);

		tbmuser = Common.getCurrentUser();

		paging = new Paging();
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});

	}

	class DetailDiskonSiswaRenderer extends ais.ui.util.MyRowRenderer {

		private boolean delete = false;
		private boolean edit = false;

		public DetailDiskonSiswaRenderer() {
			delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
			edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		}

		@SuppressWarnings("unchecked")
		@Override
		public void render(final Row row, Object data) throws Exception {
			row.setValign("top");

			final DiskonSiswaPunyaSiswa diskonSiswaPunyaSiswa = (DiskonSiswaPunyaSiswa) data;

			if (diskonSiswaPunyaSiswa.getSiswa() != null) {

				Vbox a;
				(a = RevisiHelper.createNewRevisi(DiskonSiswaPunyaSiswa.class, diskonSiswaPunyaSiswa,
						diskonSiswaPunyaSiswa.getSiswa().getNim())).setParent(row);

				new Label(diskonSiswaPunyaSiswa.getSiswa().getNama()).setParent(row);

				Vbox vbox = new Vbox();
				vbox.setParent(a);
				Hbox hbox = new Hbox();

				LampiranLain.createDownloadUploadFileLain(hbox, diskonSiswaPunyaSiswa.getId(),
						DiskonSiswaPunyaSiswa.class.getName(), "Surat Dapat Diskon", false, new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {

							}
						}, null, false, false, false, true);

				hbox.setParent(vbox);

				new Label(diskonSiswaPunyaSiswa.getSiswa().getTahunMasuk() + "").setParent(row);

				new Label(diskonSiswaPunyaSiswa.getSiswa().getSekolah() == null ? ""
						: diskonSiswaPunyaSiswa.getSiswa().getSekolah().getNama() + "").setParent(row);
			} else if (diskonSiswaPunyaSiswa.getCalonSiswa() != null) {

				Vbox a;
				(a = RevisiHelper.createNewRevisi(DiskonSiswaPunyaSiswa.class, diskonSiswaPunyaSiswa,
						diskonSiswaPunyaSiswa.getCalonSiswa().getNim())).setParent(row);

				new Label(diskonSiswaPunyaSiswa.getCalonSiswa().getNama()).setParent(row);

				Vbox vbox = new Vbox();
				vbox.setParent(a);
				Hbox hbox = new Hbox();

				LampiranLain.createDownloadUploadFileLain(hbox, diskonSiswaPunyaSiswa.getId(),
						DiskonSiswaPunyaSiswa.class.getName(), "Surat Dapat Diskon", false, new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {

							}
						}, null, false, false, false, true);

				hbox.setParent(vbox);

				new Label(diskonSiswaPunyaSiswa.getCalonSiswa().getTahunMasuk() + "").setParent(row);

				new Label(diskonSiswaPunyaSiswa.getCalonSiswa().getSekolah() == null ? ""
						: diskonSiswaPunyaSiswa.getCalonSiswa().getSekolah().getNama() + "").setParent(row);

			}

			final MyTextbox keterangan = new MyTextbox(diskonSiswaPunyaSiswa.getKeterangan());
			keterangan.setWidth("90%");
			keterangan.setRows(2);

			EventListener eventListener = new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					diskonSiswaPunyaSiswa.setKeterangan(keterangan.getValue());

					Common.refreshUpdate(diskonSiswaPunyaSiswa);

				}
			};

			keterangan.addEventListener("onChange", eventListener);

			keterangan.setParent(row);

			Session session = HibernateUtil.currentSession();

			List<Tagihan> tagihans = session.createCriteria(Tagihan.class).add(Restrictions.eq("aktif", true))
					.add(diskonSiswaPunyaSiswa.getSiswa() != null
							? Restrictions.eq("siswa", diskonSiswaPunyaSiswa.getSiswa())
							: Restrictions.eq("calonSiswa", diskonSiswaPunyaSiswa.getCalonSiswa()))
					.add(Restrictions.eq("diskonSiswa", diskonSiswa)).addOrder(Order.asc("id")).list();

			Vbox vbox = new Vbox();
			vbox.setParent(row);

			for (Tagihan tagihan : tagihans) {
				if (((tagihan.getAktif() &&  !tagihan.ambilBukanTagihanData()) && !tagihan.getNominalBiaya().getBukanTagihan())) {
					RevisiHelper.createNewRevisi(Tagihan.class, tagihan,
							tagihan.getItemBiayaSekolah().getNama()
									+ (tagihan.getBulan() != null ? ", bulan " + tagihan.getBulan() : "") + " "
									+ Common.numberFormat.get().format(tagihan.getNominal()) + ", diskon "
									+ Common.numberFormat.get().format(tagihan.getDiskonTidakLangsung()),
							"font-size:7px;").setParent(vbox);
				}

			}

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Setujui");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(diskonSiswaPunyaSiswa.getSetujui());
			checkbox.setParent(row);

			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					diskonSiswaPunyaSiswa.setSetujui(checkbox.isChecked());
					Common.refreshSaveOrUpdate(diskonSiswaPunyaSiswa);
				}
			});

			final MyToolbarbuttonConfig deleteButton = new MyToolbarbuttonConfig("Hapus", "/img/svg/trash.svg");
			deleteButton.setOrient("vertical");

			Hbox toolbar = new Hbox();

			deleteButton.setOrient("vertical");
			deleteButton.setVisible(delete);
			deleteButton.setTooltiptext("Hapus Data");
			deleteButton.addEventListener("onClick", new EventListener() {
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

											Common.refreshDelete(diskonSiswaPunyaSiswa);
											loadData(null);

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
			deleteButton.setParent(toolbar);
			toolbar.setParent(row);

		}

	}

	public Criteria initCriteria(boolean order) {

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(DiskonSiswaPunyaSiswa.class);

		criteria.createAlias("diskonSiswa", "diskonSiswa")

				.createAlias("diskonSiswa.sekolah", "sekolah")

				.add(searchsekolah.getSelectedItem() == null || searchsekolah.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: CommonSearchFilterHelper.eqSelectedWithId("diskonSiswa.sekolah", searchsekolah, false))

				.add(searchyayasan.getSelectedItem() == null || searchyayasan.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: CommonSearchFilterHelper.eqSelectedWithId("sekolah.yayasan", searchyayasan, false))

				.add(diskonSiswa == null || diskonSiswa.getId() == null
						? Restrictions.sqlRestriction("1=0")
						: Restrictions.eq("diskonSiswa", diskonSiswa));

		criteria.createAlias("siswa", "siswa", Criteria.LEFT_JOIN)
				.createAlias("calonSiswa", "calonSiswa", Criteria.LEFT_JOIN)

				.add(angkatan.getValue() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.eq("siswa.tahunMasuk", angkatan.getValue()),
								Restrictions.eq("calonSiswa.tahunMasuk", angkatan.getValue())))

				.add(nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true") :

						Restrictions.or(
								Restrictions.ilike("siswa.nomorIndukNasional", nama.getValue().trim(),
										MatchMode.ANYWHERE),
								Restrictions.or(
										Restrictions.ilike("calonSiswa.nomorIndukNasional", nama.getValue().trim(),
												MatchMode.ANYWHERE),
										Restrictions.or(
												Restrictions.ilike("calonSiswa.namaSiswa", nama.getValue().trim(),
														MatchMode.ANYWHERE),
												Restrictions.or(
														Restrictions.ilike("siswa.namaSiswa", nama.getValue().trim(),
																MatchMode.ANYWHERE),
														Restrictions.or(
																Restrictions.ilike("siswa.nomorInduk",
																		nama.getValue().trim(), MatchMode.ANYWHERE),
																Restrictions.ilike("calonSiswa.nomorInduk",
																		nama.getValue().trim(),
																		MatchMode.ANYWHERE)))))));

		if (order)
			criteria.addOrder(Order.asc("siswa.nim")).addOrder(Order.asc("calonSiswa.nomorInduk"));
		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser != null && tbmuser.getOrangTua() != null && !tbmuser.getOrangTua().ambilAnakSiswa().isEmpty()) {
			criteria.add(Restrictions.in("siswa.id", tbmuser.getOrangTua().ambilAnakSiswa()));
		}
		return criteria;
	}


	@SuppressWarnings("unchecked")
	public void loadData(Object value) {

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				try {
					showProgress(10, "Membaca penerima diskon", "Data siswa dan calon siswa sedang disiapkan.");
					refreshDashboard();

					showProgress(45, "Menghitung halaman", "Jumlah penerima diskon sedang dihitung.");
					Common.initPaging(initCriteria(false), paging);

					showProgress(75, "Mengambil daftar", "Data penerima diskon halaman aktif sedang dimuat.");
					List<DiskonSiswaPunyaSiswa> myDiskonSiswaPunyaSiswas = initCriteria(true)
							.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
							.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage()))
							.list();

					showProgress(95, "Menampilkan data", "Tabel penerima diskon sedang disusun.");
					ListModel strset = new SimpleListModel(myDiskonSiswaPunyaSiswas);
					grid.setRowRenderer(new DetailDiskonSiswaRenderer());
					grid.setModelCheckMobile(strset);
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				} finally {
					hideProgress();
				}
			}
		});

	}

	private DataLoader getDataloader() {
		return this;
	}

	public void display(final DiskonSiswa diskonSiswa, final Component component, final MyWindow window) {
		this.diskonSiswa = diskonSiswa;
		Common.clear(component);

		ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
		groupbox.setStyle("min-height: 200px; width:100%;");
		groupbox.setParent(component);

		progressHtml = new Html();
		progressHtml.setVisible(false);
		progressHtml.setParent(groupbox);

		dashboardHtml = new Html();
		dashboardHtml.setParent(groupbox);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(groupbox);

		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Siswa : ")));
		toolbar.appendChild(nama = new Textbox());
		nama.setCols(10);
		nama.addEventListener(Events.ON_OK, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});

		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Angkatan : ")));
		toolbar.appendChild(angkatan = new Intbox());
		angkatan.setCols(4);
		angkatan.addEventListener(Events.ON_OK, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});

		toolbar.appendChild(new Label(Common.getBahasaConfig("Yayasan") + " : "));
		toolbar.appendChild(searchyayasan);
		searchyayasan.setCols(10);
		searchyayasan.addEventListener(Events.ON_CHANGE, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});

		Common.selectComboItem(searchyayasan, diskonSiswa.getYayasan());
		if (diskonSiswa.getYayasan() != null) {
			searchyayasan.setDisabled(true);
		}

		toolbar.appendChild(new Label(Common.getBahasaConfig("Sekolah") + " : "));
		toolbar.appendChild(searchsekolah);
		searchsekolah.setCols(10);
		searchsekolah.addEventListener(Events.ON_CHANGE, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});

		if (diskonSiswa.getSekolah() != null) {
			Yayasan selectedYayasan = (Yayasan) (searchyayasan.getSelectedItem() == null
					|| searchyayasan.getSelectedItem().getValue() == null
					|| searchyayasan.getSelectedItem().getValue() == null ? null
							: searchyayasan.getSelectedItem().getValue());
			if (selectedYayasan != null) {
				Common.insertComboDanSemua(searchsekolah, new String[] { "nama", }, "jenisSekolah", Sekolah.class,
						Restrictions.eq("yayasan", selectedYayasan));
				Common.selectComboItem(searchsekolah, diskonSiswa.getSekolah());
				searchsekolah.setDisabled(true);
			}
		}

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}

		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Ambil Siswa", "/img/new.gif");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				AmbilDataSiswaForDiskonSiswaHelper dataSiswaHelper = new AmbilDataSiswaForDiskonSiswaHelper(
						diskonSiswa);
				dataSiswaHelper.display(getDataloader(), window);
			}

		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Ambil Calon Siswa", "/img/new.gif");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				AmbilDataCalonCalonSiswaForDiskonSiswaHelper dataSiswaHelper = new AmbilDataCalonCalonSiswaForDiskonSiswaHelper(
						diskonSiswa);
				dataSiswaHelper.display(getDataloader(), window);
			}

		});
		button.setParent(toolbar);

		if (diskonSiswa.getJenis() != null) {
			button = new MyToolbarbuttonConfig("Singkronkan Data " + diskonSiswa.getJenis(), "/img/new.gif");
			button.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event event) throws Exception {

					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							if (diskonSiswa.getJenis().equals(DiskonSiswa.DISKON_ANAK_ALUMNI)) {
								DiskonSiswa.prosesAnakAlumni(diskonSiswa);
							} else if (diskonSiswa.getJenis().equals(DiskonSiswa.DISKON_ANAK_PEGAWAI_TETAP)) {
								DiskonSiswa.prosesAnakPegawai(diskonSiswa, TipeMasaKerja.Tetap);
							} else if (diskonSiswa.getJenis().equals(DiskonSiswa.DISKON_ANAK_PEGAWAI_HONORER)) {
								DiskonSiswa.prosesAnakPegawai(diskonSiswa, TipeMasaKerja.Honorer);
							} else if (diskonSiswa.getJenis().equals(DiskonSiswa.DISKON_SAUDARA)) {
								DiskonSiswa.prosesSaudara(diskonSiswa);
							} else if (diskonSiswa.getJenis().equals(DiskonSiswa.DISKON_SAUDARA_ALUMNI)) {
								DiskonSiswa.prosesSaudaraAlumni(diskonSiswa);
							} else if (diskonSiswa.getJenis().equals(DiskonSiswa.DISKON_ALUMNI)) {
								DiskonSiswa.prosesAlumni(diskonSiswa);
							} else if (diskonSiswa.getJenis().equals(DiskonSiswa.DISKON_SEMUA)) {
								DiskonSiswa.prosesSemua(diskonSiswa);
							}

							onSearchDefault(null);

						}
					});

				}

			});
			button.setParent(toolbar);
		}

		button = new MyToolbarbuttonConfig("Singkronkan Tagihan", "/img/new.gif");
		button.setTooltiptext("Sinkronkan ulang nilai diskon ke tagihan untuk diskon ini.");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {

				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						try {
							showProgress(10, "Sinkronisasi tagihan", "Menghitung ulang tagihan penerima diskon.");
							int jumlah = DiskonSiswaSyncHelper.sinkronkan(diskonSiswa, false);
							showProgress(100, "Sinkronisasi selesai", "Data tagihan yang diperbarui: " + jumlah + ".");
							try {
								MyMessageboxConfig.show("Sinkronisasi tagihan selesai. Data tagihan yang diperbarui: " + jumlah + ".");
							} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/DiskonSiswaPunyaSiswaHelper.java:545");
							}
							onSearchDefault(null);
						} catch (Exception e) {
							Common.tampilErrorJikaAdmin(e);
						} finally {
							hideProgress();
						}
					}
				});

			}

		});
		button.setParent(toolbar);


		if (!diskonSiswa.getMemotongTagihan()) {
			button = new MyToolbarbuttonConfig("Kirimkan Diskon Ke Pembayaran", "/img/new.gif");
			button.setTooltiptext("Sinkronkan diskon lalu kirimkan nilai diskon ke proses pembayaran/transfer.");
			button.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event event) throws Exception {

					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							try {
								showProgress(10, "Mengirim diskon", "Data diskon sedang disiapkan untuk pembayaran.");
								int jumlah = DiskonSiswaSyncHelper.sinkronkan(diskonSiswa, true);
								showProgress(100, "Pengiriman selesai", "Data tagihan yang diproses: " + jumlah + ".");
								try {
									MyMessageboxConfig.show("Diskon berhasil diproses. Data tagihan yang diperbarui: " + jumlah + ".");
								} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/DiskonSiswaPunyaSiswaHelper.java:580");
								}
								onSearchDefault(null);
							} catch (Exception e) {
								Common.tampilErrorJikaAdmin(e);
							} finally {
								hideProgress();
							}
						}
					});

				}

			});
			button.setParent(toolbar);
		}

		button = new MyToolbarbuttonConfig("Bersihkan", "/img/svg/trash.svg");
		button.setVisible(tbmuser.getSiswa() == null && tbmuser.getSiswa() == null);
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {

				MyMessageboxConfig.show("Apakah yakin ingin menghapus semua data ini ?", "Pertanyaan",
						MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {

							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {
									try {

										Session session = HibernateUtil.currentSession();

										session.createSQLQuery(
												"delete from sekolah.diskon_siswa_punya_siswa_baru where diskon_siswa = "
														+ diskonSiswa.getId())
												.executeUpdate();

										loadData(null);

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

		List<String> columnHeadersAdding = new ArrayList<String>();
		columnHeadersAdding.add("Bukti");

		EventListener dataAdding = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Object[] objects = (Object[]) arg0.getData();
				DiskonSiswaPunyaSiswa diskonSiswaPunyaSiswa = (DiskonSiswaPunyaSiswa) objects[0];

				XSSFRow row = (XSSFRow) objects[2];
				XSSFWorkbook workbook = (XSSFWorkbook) objects[3];
				XSSFFont hlink_font = workbook.createFont();
				hlink_font.setUnderline(XSSFFont.U_SINGLE);
				hlink_font.setColor(new XSSFColor(Color.BLUE));

				final XSSFCellStyle hlink_style = workbook.createCellStyle();
				hlink_style.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
				hlink_style.setFillForegroundColor(new XSSFColor(Color.LIGHT_GRAY));
				hlink_style.setFont(hlink_font);

				class DataAddingHelper {
					public void process(XSSFRow row, int index, DiskonSiswaPunyaSiswa diskonSiswaPunyaSiswa,
							String jenis) throws Exception {
						LampiranLain lam = LampiranLain.ambil(diskonSiswaPunyaSiswa.getId(), jenis);

						XSSFCell cell = row.createCell(index);

						if (lam != null) {

							String nama = lam.getNama();

							cell.setCellStyle(hlink_style);
							cell.setCellValue(nama);
							String url = lam.createLinkUri();
							XSSFHyperlink link = row.getSheet().getWorkbook().getCreationHelper()
									.createHyperlink(Hyperlink.LINK_URL);
							link.setAddress(url);
							cell.setHyperlink(link);
						}
					}
				}

				DataAddingHelper dataAddingHelper = new DataAddingHelper();

				dataAddingHelper.process(row, 9, diskonSiswaPunyaSiswa, DiskonSiswaPunyaSiswa.class.getName());

			}
		};

		String[] contents = new String[] { "id", "diskonSiswa", "siswa", "calonSiswa", "keterangan", "setujui" };

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakDataCustomButton(DiskonSiswaPunyaSiswa.class, this,
				"Download", "/img/print.png", columnHeadersAdding, dataAdding, contents);

		toolbar.appendChild(cetakToolbarbutton);

		MyToolbarbuttonConfig upload = Common.uploadData(this, DiskonSiswaPunyaSiswa.class, contents);
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
		column.setLabel("NIM/No.Reg");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Angkatan");
		column.setWidth("5%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Sekolah");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Keterangan");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Tagihan dan Diskon");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Setujui");
		column.setWidth("7%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("7%");

		loadData(null);

	}

	private void refreshDashboard() {
		if (dashboardHtml == null) {
			return;
		}
		try {
			long total = countRows(false);
			long disetujui = countRows(true);
			long belum = total - disetujui;
			String[] cards = new String[] {
					CommonDashboardHtmlHelper.metricCard("Total Penerima",
							Common.numberFormat1.get().format(Long.valueOf(total)), "Jumlah data sesuai filter."),
					CommonDashboardHtmlHelper.metricCard("Disetujui",
							Common.numberFormat1.get().format(Long.valueOf(disetujui)),
							"Diskon sudah dapat diterapkan."),
					CommonDashboardHtmlHelper.metricCard("Belum Disetujui",
							Common.numberFormat1.get().format(Long.valueOf(belum)), "Masih perlu dicek.") };
			dashboardHtml.setContent(CommonDashboardHtmlHelper.panel("Penerima Diskon",
					"Daftar siswa dan calon siswa yang mendapatkan aturan diskon ini.",
					CommonDashboardHtmlHelper.cards(cards)));
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private long countRows(boolean hanyaDisetujui) {
		try {
			Criteria criteria = initCriteria(false);
			if (hanyaDisetujui) {
				criteria.add(Restrictions.eq("setujui", Boolean.TRUE));
			}
			criteria.setProjection(org.hibernate.criterion.Projections.rowCount());
			Object value = criteria.uniqueResult();
			return value instanceof Number ? ((Number) value).longValue() : 0L;
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			return 0L;
		}
	}

	private void showProgress(int percent, String title, String detail) {
		if (progressHtml == null) {
			return;
		}
		progressHtml.setVisible(true);
		progressHtml.setContent(CommonDashboardHtmlHelper.progressBar(percent, title, detail));
	}

	private void hideProgress() {
		try {
			if (progressHtml != null) {
				progressHtml.setContent("");
				progressHtml.setVisible(false);
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/DiskonSiswaPunyaSiswaHelper.java:809");
		}
	}

	@Override
	public void onSearchDefault(Event event) {
		loadData(null);
	}

}
