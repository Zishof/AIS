package ais.action.master;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Group;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.West;
import org.zkoss.zul.Window;

import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.Detailperkuliahan;
import ais.database.model.DiskusiPengumumanPerkuliahan;
import ais.database.model.Dosen;
import ais.database.model.GeneralValueObject;
import ais.database.model.KategoriPengumuman;
import ais.database.model.Kurikulum;
import ais.database.model.Mahasiswa;
import ais.database.model.PengumumanPerkuliahan;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranLain;
import ais.database.model.file.LampiranPengumumanPerkuliahan;
import ais.delivery.email.sender.MailSender;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class TampilanPengumumanPerkuliahanAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2301873239699174688L;
	private Mahasiswa mahasiswa;
	private PengumumanPerkuliahan pengumumanPerkuliahan;

	private Textbox catatan;

	private MyGrid gridKomentar;
	private MyGrid grids;

	private Boolean readonly = false;
	private Borderlayout utama;
	private DiskusiPengumumanPerkuliahan diskusiPengumumanPerkuliahan;

	// private Paging paging;

	// private Tabpanels tabpanels;
	private Component menu;
	private Tabs tabspeng;
	private Tabpanels tabpanelspeng;
	private Textbox cari = new Textbox();
	private Rows rows;
	private Tbmuser tbmuser;

	protected LampiranLain lampiranLain;

	private Perkuliahan perkuliahan = null;
	private boolean sederhana = false;

	private String menuBgColor;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);

		if (execution.getParameter("perkuliahan") != null) {
			perkuliahan = (Perkuliahan) HibernateUtil.currentSession().createCriteria(Perkuliahan.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.idEq(Long.parseLong(execution.getParameter("perkuliahan").trim())))
					.uniqueResult();
		}

		tbmuser = Common.getCurrentFromSpringUser();
		if (tbmuser == null || tbmuser.getUserId() == null) {
			onSearchDefault(null);
			return;
		}

		menuBgColor = Common.getKonfigurasi("menu_bg_color", "#F5F5F5").getNilai();

		sederhana = execution.getParameter("sederhana") != null
				&& execution.getParameter("sederhana").equalsIgnoreCase("true");

		mahasiswa = tbmuser.getMahasiswa();
		if (session != null) { session.setAttribute("usersTemp", tbmuser); }
		loadMenu();

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				onSearchDefault(null);

			}
		});

	}

	private void loadMenu() {

		String desktopWidth = execution.getParameter("desktopWidth");

		if (sederhana || Common.isMobile() || (desktopWidth != null
				&& Integer.parseInt(desktopWidth.replaceAll("px", "")) < ConstantValues.UKURAN_BATAS_MOBILE)) {
			menu = new North();
			((North) menu).setFlex(true);
			((North) menu).setHeight("100%");
		} else {
			menu = new West();
			((West) menu).setStyle("background:" + menuBgColor + " repeat-x 0 0;");
			((West) menu).setWidth("250px");

		}

		if (utama != null) {
			utama.appendChild(menu);
		}

		Row rowPencarian = Common.tampilanScroll1(menu);
		rowPencarian.setStyle("border:0px;background: " + menuBgColor + ";");

		Hbox hbox = new Hbox();
		hbox.setParent(rowPencarian);

		hbox.setWidth("100%");
		hbox.setPack("center");
		hbox.setAlign("center");

		MyLabelConfig c;
		hbox.appendChild(c = new MyLabelConfig("Cari:"));
		c.setStyle("font-size:11px;");

		cari = new Textbox();
		cari.setCols(10);
		hbox.appendChild(cari);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/search.svg");
		hbox.appendChild(button);

		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				loadData(cari.getValue().trim());
			}
		});

		cari.addEventListener("onOK", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(cari.getValue().trim());
			}
		});

		MyFormRow rowDicari = new MyFormRow();
		rowDicari.setStyle("border:0px;background: " + menuBgColor + ";");
		rowDicari.setParent(rowPencarian.getParent());

		Grid grid = new Grid();
		grid.setSclass("dgrid");
		grid.setSclass("fgrid");
		grid.setWidth("100%");
		grid.setParent(rowDicari);
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("100%");

		rows = new Rows();
		rows.setParent(grid);
		loadData(cari.getValue());

		MyToolbarbuttonConfig buttonTambah = new MyToolbarbuttonConfig("Tambah", "/img/add_item.png");
		buttonTambah.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				PengumumanPerkuliahan pengumumanPerkuliahan = new PengumumanPerkuliahan();
				pengumumanPerkuliahan.setPerkuliahan(perkuliahan);
				PengumumanPerkuliahanAction.onAddExternal(event, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						loadData(cari.getValue().trim());
						onSearchDefault(null);
					}
				}, pengumumanPerkuliahan);
			}
		});
		buttonTambah.setParent(hbox);

	}

	boolean sudahTampil = false;

	@SuppressWarnings("unchecked")
	public void loadData(String keyword) {

		Common.clear(rows);

		List<PengumumanPerkuliahan> pengumumanPerkuliahanes = ConstantValues.simpleList(
				initCriteria(true).add(keyword == null || keyword.trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("judul", keyword, MatchMode.ANYWHERE)).setMaxResults(500),
				PengumumanPerkuliahan.class);

		KategoriPengumuman kategoriPengumuman = new KategoriPengumuman();
		kategoriPengumuman.setId(-1L);

		if (tbmuser != null
				&& (tbmuser.ambilDosen() != null || tbmuser.getMahasiswa() != null || tbmuser.getSiswa() != null
						|| tbmuser.getCalonSiswa() != null || tbmuser.getBiodataCalonMahasiswa() != null)) {
			for (final PengumumanPerkuliahan pengumumanPerkuliahan : pengumumanPerkuliahanes) {
				if (KategoriPengumuman.PENGUMUMAN_UTAMA != null && pengumumanPerkuliahan.getKategoriPengumuman() != null
						&& KategoriPengumuman.PENGUMUMAN_UTAMA.getId()
								.equals(pengumumanPerkuliahan.getKategoriPengumuman().getId())) {
					continue;
				}
				try {
					JSONArray isiPollings = new JSONArray(pengumumanPerkuliahan.getIsiPolling());
					if (isiPollings.length() > 0 && tbmuser != null && tbmuser.getUserId() != null) {
						JSONObject jawabanPolling = new JSONObject(pengumumanPerkuliahan.getJawabanPolling());
						boolean terjawab = !jawabanPolling.isNull(tbmuser.getUserId());
						if (!terjawab && !sudahTampil) {
							sudahTampil = true;

							Common.createDefaultTimer(new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									prosess(pengumumanPerkuliahan.getId(), sederhana);

									MyMessageboxConfig.showFormat(
											"Kepada Bapak/Ibu, saat ini terdapat Polling / Jejak Pendapat dengan judul \"{V1}\" yang masih perlu Anda isi. Mohon berkenan melengkapi Polling / Jejak Pendapat berikut agar partisipasi dan masukan Anda dapat kami catat.",
											"Polling / Jejak Pendapat", MyMessageboxConfig.OK,
											MyMessageboxConfig.INFORMATION, pengumumanPerkuliahan.getJudul());
								}
							});
							break;
						}
					}
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}
		}

		for (final PengumumanPerkuliahan pengumumanPerkuliahan : pengumumanPerkuliahanes) {

			if (KategoriPengumuman.PENGUMUMAN_UTAMA != null && pengumumanPerkuliahan.getKategoriPengumuman() != null
					&& KategoriPengumuman.PENGUMUMAN_UTAMA.getId()
							.equals(pengumumanPerkuliahan.getKategoriPengumuman().getId())) {
				continue;
			}

			KategoriPengumuman kategoriPengumumanTemporari = pengumumanPerkuliahan.getKategoriPengumuman();
			if (kategoriPengumumanTemporari != null && (kategoriPengumuman == null
					|| !kategoriPengumuman.getId().equals(kategoriPengumumanTemporari.getId()))) {
				kategoriPengumuman = kategoriPengumumanTemporari;
				Group group = new ais.ui.util.MyGroupConfig(kategoriPengumuman.getNama());
				group.setParent(rows);

			} else if (kategoriPengumumanTemporari == null && kategoriPengumuman != null) {
				kategoriPengumuman = null;
				Group group = new ais.ui.util.MyGroupConfig("Pengumuman dan Informasi");
				group.setParent(rows);

			}

			final MyFormRow row = new MyFormRow();row.setValign("top");
			row.setStyle("border:0px;background: " + menuBgColor + ";font-size: x-small;");

			row.setParent(rows);

			String text = pengumumanPerkuliahan.getJudul();

			text = text.length() > 255 ? text.substring(0, 254) + ".." : text;

			Toolbarbutton toolbarbutton = new ais.ui.util.MyToolbarbuttonConfig(text);
			toolbarbutton.setStyle("font-size: x-small;");

			row.appendChild(toolbarbutton);
			toolbarbutton.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Clients.scrollIntoView(row);
					prosess(pengumumanPerkuliahan.getId(), sederhana);
				}

			});

		}

		pengumumanPerkuliahanes = null;
	}

	@SuppressWarnings("unchecked")
	private void prosess(Long pengumunan, final boolean sederhana) throws Exception {
		final PengumumanPerkuliahan pengumumanPerkuliahan = (PengumumanPerkuliahan) ConstantValues
				.simpleObject(HibernateUtil.currentSession().createCriteria(PengumumanPerkuliahan.class)
						.add(Restrictions.idEq(pengumunan)), PengumumanPerkuliahan.class);
		if (pengumumanPerkuliahan != null) {
			List<Tabpanel> tabpanels = this.tabpanelspeng.getChildren();
			synchronized (tabpanels) {

				final Window window;

				String desktopWidth = execution.getParameter("desktopWidth");
				if (sederhana || Common.isMobile() || (desktopWidth != null
						&& Integer.parseInt(desktopWidth.replaceAll("px", "")) < ConstantValues.UKURAN_BATAS_MOBILE)) {
					window = new Window(pengumumanPerkuliahan.getJudul(), "none", false);
					window.setParent(page.getFirstRoot());
					window.setHeight("95%");
					window.setWidth("90%");
				} else {
					window = null;
				}

				if (window == null) {
					for (final Tabpanel myTabpanel : tabpanels) {

						try {
							if (myTabpanel.getAttribute("pengumumanPerkuliahan") == null) {
								continue;
							}

							PengumumanPerkuliahan myPengumumanPerkuliahan = (PengumumanPerkuliahan) myTabpanel
									.getAttribute("pengumumanPerkuliahan");

							if (myTabpanel != null && myPengumumanPerkuliahan != null
									&& myPengumumanPerkuliahan.getId() != null && myPengumumanPerkuliahan.getId()
											.toString().equals(pengumumanPerkuliahan.getId().toString())) {
								myTabpanel.getLinkedTab().setSelected(true);
								return;
							}
						} catch (Exception e) {
							ais.common.Common.tampilErrorJikaAdmin(e);
						}

					}
				}

				final MyTabConfig tab = new MyTabConfig(pengumumanPerkuliahan.getJudul());
				final Tabpanel tabpanel = new ais.ui.util.MyTabpanel();
				tab.setClosable(false);

				if (window == null) {
					tab.setParent(tabspeng);
					tabpanel.setParent(this.tabpanelspeng);
				}

				tabpanel.setAttribute("pengumumanPerkuliahan", pengumumanPerkuliahan);

				final Borderlayout subSubBorderlayout = new ais.ui.util.MyBorderlayout();
				subSubBorderlayout.setParent(window == null ? tabpanel : window);

				Center subcenter = new Center();
				subcenter.setParent(subSubBorderlayout);
				ais.ui.util.ZkCompat.setFlex(subcenter, true);
				subcenter.setBorder("none");

				MyGrid grids = new MyGrid();
				grids.setMold("paging");
				grids.setParent(subcenter);
				grids.setSclass("fgrid");

				Columns columns = new Columns();

				columns.setParent(grids);

				MyColumnConfig column = new MyColumnConfig();
				column.setParent(columns);
				column.setWidth("0px");

				column = new MyColumnConfig();
				column.setParent(columns);

				Rows rows = new Rows();
				rows.setParent(grids);

				MyFormRow row = new MyFormRow();row.setValign("top");
				row.setParent(rows);

				final MyDetail detail = new MyDetail();
				detail.setParent(row);
				detail.setOpen(true);
				displayDetailPertemuanFileContent(pengumumanPerkuliahan, detail);

				final Vbox vbox = new Vbox();
				vbox.setHeight("100%");
				vbox.setWidth("100%");
				vbox.setParent(row);

				new ais.ui.util.MyHtml(PengumumanPerkuliahanAction.tampilPengumuman(pengumumanPerkuliahan))
						.setParent(vbox);
				Vbox vbox2 = new Vbox();
				vbox2.setParent(vbox);
				PengumumanPerkuliahanAction.tampilkanPolling(pengumumanPerkuliahan, vbox2);

				if (pengumumanPerkuliahan != null && pengumumanPerkuliahan.getPerkuliahan() != null) {
					vbox.appendChild(new ais.ui.util.MyHtml("<hr>"));
					Perkuliahan perkuliahan = pengumumanPerkuliahan.getPerkuliahan();
					ais.action.master.helper.PerkuliahanUIHelper.displayDosenPerkuliahan(vbox, perkuliahan, true);
					Kurikulum kurikulum = perkuliahan.getKurikulum();
					Vbox a = RevisiHelper.createNewRevisi(Perkuliahan.class, perkuliahan,
							perkuliahan.getMatakuliah().getKode() + "-" + perkuliahan.getMatakuliah().getNama() + " "
									+ perkuliahan.getMatakuliah().getSks() + " sks "
									+ (kurikulum == null ? "" : " (Kurikulum:" + kurikulum.getTahun() + ")"));
					a.setParent(vbox);

					MatakuliahPrasyaratAction.tampilPrasyarat(a, perkuliahan.getMatakuliah());
					ais.action.master.helper.PerkuliahanUIHelper.displayHariJamRuanganPerkuliahanUmum(vbox, perkuliahan);
				}

				tab.setSelected(true);

				Hbox toolbar = new Hbox();
				toolbar.setParent(vbox);
				MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Komentar", "/img/live-on.gif");
				toolbarbutton.setOrient("vertical");
				toolbarbutton.setParent(toolbar);
				toolbarbutton.setVisible(pengumumanPerkuliahan.getBolehDiberiKomentar());
				toolbarbutton.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						init(new DiskusiPengumumanPerkuliahan(), pengumumanPerkuliahan, detail);

					}
				});

				if (pengumumanPerkuliahan.getAdaVideoConference()) {
					try {
						Common.createVideoConrefrence(pengumumanPerkuliahan, toolbar, true, false, new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								Common.createDefaultTimer(new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										prosess(pengumumanPerkuliahan.getId(), sederhana);
									}
								});
							}
						});
					} catch (Exception e) {
						ais.common.Common.tampilErrorJikaAdmin(e);
					}
					Common.tampilOnline(pengumumanPerkuliahan, vbox);
				}

				MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Ubah", "/img/svg/edit-box-line.svg");
				button.setOrient("vertical");
				button.setTooltiptext("Ubah Data");
				button.setVisible(tbmuser != null && tbmuser.getMahasiswa() == null &&  tbmuser.getSiswa() == null);
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						PengumumanPerkuliahanAction.onAddExternal(event, new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {

								if (window == null) {
									List<Tabpanel> tabpanels = tabpanelspeng.getChildren();
									synchronized (tabpanels) {
										for (final Tabpanel myTabpanel : tabpanels) {

											try {
												if (myTabpanel.getAttribute("pengumumanPerkuliahan") == null) {
													continue;
												}

												PengumumanPerkuliahan myPengumumanPerkuliahan = (PengumumanPerkuliahan) myTabpanel
														.getAttribute("pengumumanPerkuliahan");

												if (myTabpanel != null && myPengumumanPerkuliahan != null
														&& myPengumumanPerkuliahan.getId() != null
														&& myPengumumanPerkuliahan.getId().toString()
																.equals(pengumumanPerkuliahan.getId().toString())) {
													myTabpanel.setVisible(false);
													myTabpanel.getLinkedTab().setVisible(false);
													myTabpanel.setAttribute("pengumumanPerkuliahan", null);
													break;
												}
											} catch (Exception e) {
												ais.common.Common.tampilErrorJikaAdmin(e);
											}
										}
									}
								}

								Common.createDefaultTimer(new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										prosess(pengumumanPerkuliahan.getId(), sederhana);
									}
								});
							}
						}, pengumumanPerkuliahan);
					}
				});
				button.setParent(toolbar);

				button = new MyToolbarbuttonConfig("Hapus", "/img/svg/trash.svg");
				button.setTooltiptext("Hapus Data");
				button.setOrient("vertical");
				button.setVisible(tbmuser != null && tbmuser.getMahasiswa() == null &&  tbmuser.getSiswa() == null);
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						MyMessageboxConfig.show(
								"Apakah Bapak/Ibu yakin ingin menghapus data ini? Data yang telah dihapus tidak dapat dikembalikan. Silakan tekan OK untuk melanjutkan penghapusan, atau Batal untuk membatalkannya.",
								"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
								new EventListener() {

									@Override
									public void onEvent(Event event) throws Exception {
										int i = Integer.parseInt(event.getData().toString());
										if (i == MyMessageboxConfig.OK) {
											try {

												Common.refreshDelete(pengumumanPerkuliahan);

												if (window == null) {
													List<Tabpanel> tabpanels = tabpanelspeng.getChildren();
													synchronized (tabpanels) {
														for (final Tabpanel myTabpanel : tabpanels) {

															try {
																if (myTabpanel.getAttribute(
																		"pengumumanPerkuliahan") == null) {
																	continue;
																}

																PengumumanPerkuliahan myPengumumanPerkuliahan = (PengumumanPerkuliahan) myTabpanel
																		.getAttribute("pengumumanPerkuliahan");

																if (myTabpanel != null
																		&& myPengumumanPerkuliahan != null
																		&& myPengumumanPerkuliahan.getId() != null
																		&& myPengumumanPerkuliahan.getId().toString()
																				.equals(pengumumanPerkuliahan.getId()
																						.toString())) {
																	myTabpanel.setVisible(false);
																	myTabpanel.getLinkedTab().setVisible(false);
																	myTabpanel.setAttribute("pengumumanPerkuliahan",
																			null);
																	break;
																}
															} catch (Exception e) {
																ais.common.Common.tampilErrorJikaAdmin(e);
															}
														}
													}
												} else {
													window.detach();
												}

												onSearchDefault(event);
											} catch (Exception e) {
												Common.tampilErrorJikaAdmin(e);
												MyMessageboxConfig.show(
														"Mohon maaf, data ini tidak dapat dihapus untuk saat ini. Langkah yang dapat dilakukan: (1) pastikan data tidak sedang digunakan atau berelasi dengan data lain; (2) muat ulang halaman lalu coba kembali; (3) apabila masih gagal, mohon hubungi administrator sistem.");
											}
										}

									}
								});

					}
				});
				button.setParent(toolbar);

				if (window != null) {
					try {

						South south = new South();
						ais.ui.util.ZkCompat.setFlex(south, true);
						south.setParent(subSubBorderlayout);

						final Window myWindow = window;
						Toolbar toolbar1 = new Toolbar();
						toolbar1.setParent(south);
						MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Selesai", "/img/cancel.gif");
						cancel.setTooltiptext("Tutup");
						cancel.addEventListener("onClick", new EventListener() {
							@Override
							public void onEvent(Event event) throws Exception {
								myWindow.detach();
							}
						});
						cancel.setParent(toolbar1);

						window.onModal();
					} catch (Exception e) {
						ais.common.Common.tampilErrorJikaAdmin(e);
					}
				}

			}
		}
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		List<Long> perkuliahans = new ArrayList<Long>();
		if (tbmuser != null && tbmuser.getMahasiswa() != null) {
			perkuliahans = tbmuser.getMahasiswa().ambilPerkuliahanDanParalel();
		} else if (tbmuser != null && tbmuser.ambilDosen() != null
				&& tbmuser.hakAkses().getRoleId().equalsIgnoreCase("dosen")) {
			perkuliahans = tbmuser.ambilDosen().ambilPerkuliahan(session);
		} else if (perkuliahan != null) {
			perkuliahans.add(perkuliahan.getId());
		}

		Criteria criteria = session.createCriteria(PengumumanPerkuliahan.class)

				.add(!perkuliahans.isEmpty() ? Restrictions.in("perkuliahan.id", perkuliahans)
						: Restrictions.sqlRestriction("true"))

				.add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")));

		if (order)
			criteria.createAlias("kategoriPengumuman", "kategoriPengumuman", Criteria.LEFT_JOIN)
					.addOrder(Order.asc("kategoriPengumuman.nomorUrut")).addOrder(Order.desc("tanggal"));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		loadData(cari.getValue());

		if (!sederhana && !Common.isMobile()) {
			Common.createDefaultTimer(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					List<PengumumanPerkuliahan> listPengumumanPerkuliahan = initCriteria(true)
							.setMaxResults(Common.ROWS_COUNT_ON_PAGE_1).list();

					if (!listPengumumanPerkuliahan.isEmpty()) {
						prosess(listPengumumanPerkuliahan.get(0).getId(), false);
					}
				}
			});
		}
	}

	public void displayDetailPertemuanFileContent(final PengumumanPerkuliahan pengumumanPerkuliahan,
			final MyDetail detail) {

		this.pengumumanPerkuliahan = pengumumanPerkuliahan;

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(detail);

				Session session = HibernateUtil.currentSession();
				int jumlahLampiranPengumumanPerkuliahan = ((Number) session
						.createCriteria(LampiranPengumumanPerkuliahan.class).setProjection(Projections.rowCount())
						.add(Restrictions.or(Restrictions.isNull("ditampilkan"), Restrictions.eq("ditampilkan", true)))
						.add(Restrictions.eq("pengumumanPerkuliahan", pengumumanPerkuliahan)).uniqueResult())
						.intValue();
				int jumlahPengumumanPengumumanPerkuliahan = pengumumanPerkuliahan.getBolehDiberiKomentar()
						? ((Number) session.createCriteria(DiskusiPengumumanPerkuliahan.class)
								.add(Restrictions.ne("catatan", "")).add(Restrictions.isNotNull("catatan"))
								.setProjection(Projections.rowCount())
								.add(Restrictions.eq("pengumumanPerkuliahan", pengumumanPerkuliahan)).uniqueResult())
								.intValue()
						: 0;

				if (jumlahPengumumanPengumumanPerkuliahan == 0 && jumlahLampiranPengumumanPerkuliahan == 0) {
					return;
				}

				ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
				groupbox.setStyle("min-height: 200px;");
				groupbox.setParent(detail);
				if (jumlahPengumumanPengumumanPerkuliahan != 0 && jumlahLampiranPengumumanPerkuliahan != 0) {
					groupbox.appendChild(
							new MyCaptionStyled("Daftar lampiran atau komentar terkait dengan pengumuman ini"));
				} else if (pengumumanPerkuliahan.getBolehDiberiKomentar()
						&& jumlahPengumumanPengumumanPerkuliahan != 0) {
					groupbox.appendChild(new MyCaptionStyled("Daftar komentar terkait dengan pengumuman ini"));
				} else if (jumlahLampiranPengumumanPerkuliahan != 0) {
					groupbox.appendChild(new MyCaptionStyled("Daftar lampiran terkait dengan pengumuman ini"));
				}

				if (jumlahLampiranPengumumanPerkuliahan > 0) {
					grids = new MyGrid();
					grids.setMold("paging");
					grids.setPageSize(10);
					grids.setParent(groupbox);

					loadDataAttachment();
				}

				if (pengumumanPerkuliahan.getBolehDiberiKomentar() && jumlahPengumumanPengumumanPerkuliahan > 0) {
					gridKomentar = new MyGrid();
					gridKomentar.setMold("paging");
					gridKomentar.setPageSize(1000);
					gridKomentar.setParent(groupbox);

					Columns columns = new Columns();

					columns.setParent(gridKomentar);

					MyColumnConfig column = new MyColumnConfig();
					column.setParent(columns);
					column.setLabel("Komentar");

					column = new MyColumnConfig();
					column.setParent(columns);
					column.setLabel("Tanggal");
					column.setWidth("20%");

					column = new MyColumnConfig();
					column.setParent(columns);
					column.setLabel("Oleh");
					column.setWidth("20%");

					column = new MyColumnConfig();
					column.setParent(columns);
					column.setLabel("Hapus");
					column.setWidth("10%");

					loadData(pengumumanPerkuliahan, detail);
				}
			}
		});

	}

	@SuppressWarnings("unchecked")
	public void loadDataAttachment() {
		Session session = HibernateUtil.currentSession();
		List<LampiranPengumumanPerkuliahan> lampiranPengumumanPerkuliahan = session
				.createCriteria(LampiranPengumumanPerkuliahan.class).addOrder(Order.desc("id"))
				.add(Restrictions.or(Restrictions.isNull("ditampilkan"), Restrictions.eq("ditampilkan", true)))
				.add(Restrictions.eq("pengumumanPerkuliahan", pengumumanPerkuliahan)).list();

		ListModel strset = new SimpleListModel(lampiranPengumumanPerkuliahan);

		grids.setRowRenderer(new DetailLampiranPengumumanPerkuliahanRenderer());
		grids.setModelCheckMobile(strset);

		grids.renderAll();
		grids.setOddRowSclass("non-odd");

	}

	class DetailLampiranPengumumanPerkuliahanRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final LampiranPengumumanPerkuliahan lampiranPengumumanPerkuliahan = (LampiranPengumumanPerkuliahan) arg1;
			Vbox vbox = new Vbox();
			vbox.setParent(arg0);

			vbox.setWidth("100%");
			CommonMedia.preview(lampiranPengumumanPerkuliahan, vbox);

			new Label(Common.dateFormat.get().format(lampiranPengumumanPerkuliahan.getUploadDate())).setParent(vbox);

			Hbox hbox = new Hbox();
			hbox.setParent(vbox);
			MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig(lampiranPengumumanPerkuliahan.getNama(),
					lampiranPengumumanPerkuliahan.iconDonwload());
			toolbarbutton.setParent(hbox);
			toolbarbutton.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					LampiranPengumumanPerkuliahan content = (LampiranPengumumanPerkuliahan) HibernateUtil
							.currentSession().createCriteria(LampiranPengumumanPerkuliahan.class)
							.add(Restrictions.idEq(lampiranPengumumanPerkuliahan.getId())).setMaxResults(1)
							.uniqueResult();

					Filedownload.save(content.ambilFile(), lampiranPengumumanPerkuliahan.getMimeType());
				}

			});

		}

	}

	private void init(DiskusiPengumumanPerkuliahan diskusiPengumumanPerkuliahan,
			final PengumumanPerkuliahan pengumumanPerkuliahan, final MyDetail detail) throws Exception {
		this.diskusiPengumumanPerkuliahan = diskusiPengumumanPerkuliahan;
		this.pengumumanPerkuliahan = pengumumanPerkuliahan;
		final MyWindow addWindow = new MyWindow();
		addWindow.setParent(page.getFirstRoot());
		addWindow.setTitle("Komentar Pengumuman Perkuliahan");
		addWindow.setHeight("300px");
		addWindow.setWidth(sederhana || Common.isMobile() ? "100%" : "300px");

		Common.clear(addWindow);
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
		MyColumnConfig column = new MyColumnConfig();
		column.setWidth("25%");
		columns.appendChild(column);
		column = new MyColumnConfig();
		column.setWidth("85%");
		columns.appendChild(column);
		grid.appendChild(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Komentar"));
		catatan = new Textbox();
		catatan.setValue(
				diskusiPengumumanPerkuliahan.getCatatan() == null ? "" : diskusiPengumumanPerkuliahan.getCatatan());
		catatan.setWidth("90%");
		catatan.setRows(3);
		row.appendChild(catatan);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label());
		Hbox hbox = new Hbox();
		hbox.setParent(row);
		LampiranLain.createDownloadUploadFileLain(hbox, diskusiPengumumanPerkuliahan.getId(),
				"Lampiran Komentar Pengumuman Perkuliahan", "Lampiran Komentar", false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						lampiranLain = (LampiranLain) arg0.getData();

					}
				}, null, false, false, false, true);

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
				displayDetailPertemuanFileContent(pengumumanPerkuliahan, detail);
				addWindow.detach();
			}
		});
		cancel.setParent(toolbar);

		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (onSave(event)) {
					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							displayDetailPertemuanFileContent(pengumumanPerkuliahan, detail);
						}
					});
					addWindow.detach();
				}
			}
		});
		save.setParent(toolbar);

		borderlayout.setParent(addWindow);

		addWindow.onModal();

	}

	public boolean onSave(Event event) throws Exception {

		String myoleh = "";

		if (tbmuser != null) {
			if (tbmuser.getMahasiswa() != null) {
				myoleh = tbmuser.getMahasiswa().getNim() + " - " + tbmuser.getMahasiswa().getNama() + " (Mahasiswa)";
			} else if (tbmuser.getMahasiswa() != null) {
				myoleh = tbmuser.ambilDosen().getNama() + " (Dosen)";
			} else {
				myoleh = tbmuser.getUserId() + " (" + tbmuser.hakAkses().getRoleName() + ")";
			}
		}

		Mahasiswa mahasiswa = tbmuser == null ? null : tbmuser.getMahasiswa();
		if (mahasiswa != null) {
			tbmuser = null;
		}

		Session session = HibernateUtil.currentSession();
		if (diskusiPengumumanPerkuliahan.getId() != null) {
			diskusiPengumumanPerkuliahan = (DiskusiPengumumanPerkuliahan) session
					.load(DiskusiPengumumanPerkuliahan.class, diskusiPengumumanPerkuliahan.getId());
		}
		diskusiPengumumanPerkuliahan.setTanggal(ais.ui.util.WaktuUtil.getDate());
		diskusiPengumumanPerkuliahan.setOleh(myoleh);
		diskusiPengumumanPerkuliahan.setCatatan(catatan.getValue());
		diskusiPengumumanPerkuliahan.setPengumumanPerkuliahan(pengumumanPerkuliahan);
		diskusiPengumumanPerkuliahan.setTbmuser(tbmuser);
		diskusiPengumumanPerkuliahan.setMahasiswa(mahasiswa);

		Common.refreshSaveOrUpdate(session, diskusiPengumumanPerkuliahan);

		try {
			Session sessionStream = StreamingHibernateUtil.getInstance().currentSession();
			System.out.println("lampiranLain => " + lampiranLain);
			if (lampiranLain != null && lampiranLain.getId() != null) {
				sessionStream.refresh(lampiranLain);
				lampiranLain.setRef(diskusiPengumumanPerkuliahan.getId());

				sessionStream.getTransaction().begin();
				sessionStream.update(lampiranLain);
				sessionStream.getTransaction().commit();
			}

			StreamingHibernateUtil.getInstance().closeSession();
		} catch (Exception e) {
			StreamingHibernateUtil.getInstance().rollbackTransaction();
			Common.tampilErrorJikaAdmin(e);
		}

		lampiranLain = null;

		TampilanPengumumanPerkuliahanAction.kirimEmail(diskusiPengumumanPerkuliahan);

		return true;
	}

	public static void kirimEmail(final DiskusiPengumumanPerkuliahan diskusiPengumumanPerkuliahan) {
		if (!diskusiPengumumanPerkuliahan.getCatatan().trim().isEmpty()) {
			Common.createDefaultTimer(new EventListener() {

				@SuppressWarnings("unchecked")
				@Override
				public void onEvent(Event arg0) throws Exception {
					Tbmuser tbmuser = Common.getCurrentUser();
					String emailUser = "";

					if (tbmuser != null && tbmuser.getEmail() != null
							&& Common.isValidEmailAddress(tbmuser.getEmail())) {
						emailUser += emailUser.trim().isEmpty() ? tbmuser.getEmail().trim()
								: "," + tbmuser.getEmail().trim();
					}

					List<String> emails = diskusiPengumumanPerkuliahan.getPengumumanPerkuliahan().getKorespondensi()
							.trim().isEmpty()
									? new ArrayList<String>()
									: HibernateUtil.currentSession().createCriteria(Tbmuser.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
											.add(Restrictions.in("userId",
													diskusiPengumumanPerkuliahan.getPengumumanPerkuliahan()
															.getKorespondensi().trim().split(",")))
											.setProjection(Projections.groupProperty("email")).list();
					for (String email : emails) {
						if (email != null && !email.trim().isEmpty() && Common.isValidEmailAddress(email)) {
							emailUser += emailUser.trim().isEmpty() ? email.trim() : "," + email.trim();
						}
					}

					JSONArray userIds = new JSONArray();
					for (String email : diskusiPengumumanPerkuliahan.getPengumumanPerkuliahan().getKorespondensi()
							.trim().split(",")) {
						if (!email.trim().isEmpty()) {
							userIds.put(email);
						}
					}

					emails = HibernateUtil.currentSession().createCriteria(DiskusiPengumumanPerkuliahan.class)
							.add(Restrictions.eq("pengumumanPerkuliahan",
									diskusiPengumumanPerkuliahan.getPengumumanPerkuliahan()))
							.createAlias("mahasiswa", "mahasiswa")
							.setProjection(Projections.groupProperty("mahasiswa.email")).list();
					for (String email : emails) {
						if (email != null && !email.trim().isEmpty() && Common.isValidEmailAddress(email)) {
							emailUser += emailUser.trim().isEmpty() ? email.trim() : "," + email.trim();
						}
					}

					emails = HibernateUtil.currentSession().createCriteria(DiskusiPengumumanPerkuliahan.class)
							.add(Restrictions.eq("pengumumanPerkuliahan",
									diskusiPengumumanPerkuliahan.getPengumumanPerkuliahan()))
							.createAlias("mahasiswa", "mahasiswa")
							.setProjection(Projections.groupProperty("mahasiswa.nim")).list();
					for (String email : emails) {
						userIds.put(email);
					}

					emails = HibernateUtil.currentSession().createCriteria(DiskusiPengumumanPerkuliahan.class)
							.add(Restrictions.eq("pengumumanPerkuliahan",
									diskusiPengumumanPerkuliahan.getPengumumanPerkuliahan()))
							.createAlias("tbmuser", "tbmuser").setProjection(Projections.groupProperty("tbmuser.email"))

							.list();
					for (String email : emails) {
						if (email != null && !email.trim().isEmpty() && Common.isValidEmailAddress(email)) {
							emailUser += emailUser.trim().isEmpty() ? email.trim() : "," + email.trim();
						}
					}

					emails = HibernateUtil.currentSession().createCriteria(DiskusiPengumumanPerkuliahan.class)
							.add(Restrictions.eq("pengumumanPerkuliahan",
									diskusiPengumumanPerkuliahan.getPengumumanPerkuliahan()))
							.createAlias("tbmuser", "tbmuser")
							.setProjection(Projections.groupProperty("tbmuser.userId"))

							.list();
					for (String email : emails) {
						userIds.put(email);
					}

					tbmuser = diskusiPengumumanPerkuliahan.getTbmuser();
					Mahasiswa mahasiswa = diskusiPengumumanPerkuliahan.getMahasiswa();

					// System.out.println("emailUser = " + emailUser);

					if (!emailUser.trim().isEmpty() || userIds.length() > 0) {
						String subject = "Komentar pengumuman akademik => "
								+ diskusiPengumumanPerkuliahan.getPengumumanPerkuliahan().getJudul();

						String body = "Komentar dari "
								+ (tbmuser != null ? tbmuser.getUserNama() + " (" + tbmuser.getUserId() + ")"
										: (mahasiswa == null ? "" : mahasiswa.getNim() + " " + mahasiswa.getNama()));

						body += diskusiPengumumanPerkuliahan.getCatatan() + "<br><br>Isi pengumuman<hr>"
								+ diskusiPengumumanPerkuliahan.getPengumumanPerkuliahan().getCatatan();

						body += "<br><br>Komentar Lainnya<hr>";

						List<DiskusiPengumumanPerkuliahan> komentars = HibernateUtil.currentSession()
								.createCriteria(DiskusiPengumumanPerkuliahan.class)
								.add(Restrictions.eq("pengumumanPerkuliahan",
										diskusiPengumumanPerkuliahan.getPengumumanPerkuliahan()))
								.list();
						body += "<ul>";
						for (DiskusiPengumumanPerkuliahan komentar : komentars) {
							tbmuser = komentar.getTbmuser();
							mahasiswa = komentar.getMahasiswa();
							body += "<li>" + (tbmuser != null ? tbmuser.getUserNama() + " (" + tbmuser.getUserId() + ")"
									: (mahasiswa == null ? "" : mahasiswa.getNim() + " " + mahasiswa.getNama()));
							body += " : " + komentar.getCatatan() + " "
									+ Common.dateFormat.get().format(komentar.getTanggal());
							body += "</li>";
						}
						body += "</ul>";

						body += "<br><br><hr>Untuk informasi lebih lanjut bisa dilihat di "
								+ Common.getRequestHostWithProtocol()
								+ ", kemudian click pengumuman.<br><br>Terima Kasih";

						String sender = Common.getKonfigurasi("default_email", "info@zishof.com").getNilai();
						MailSender.sendMail(userIds, subject, body, sender, emailUser, diskusiPengumumanPerkuliahan);
					}
				}
			});
		}
	}

	public static void broadcastEmail(final PengumumanPerkuliahan pengumumanPerkuliahan) {
		if (!pengumumanPerkuliahan.getCatatan().trim().isEmpty()) {
			Common.createDefaultTimer(new EventListener() {

				@SuppressWarnings("unchecked")
				@Override
				public void onEvent(Event arg0) throws Exception {
					String emailUser = "";

					JSONArray userIds = new JSONArray();

					if (pengumumanPerkuliahan.getBroadcastKeMahasiswaAktif()) {

						Collection<Long> emails = pengumumanPerkuliahan.getPerkuliahan().ambilDetailperkuliahan();

						for (Long detailperkuliahanid : emails) {
							Detailperkuliahan a = (Detailperkuliahan) GeneralValueObject
									.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());

							if (a != null && a.getMahasiswa() != null) {

								userIds.put(a.getMahasiswa().getNim());

								String email = a.getMahasiswa().getEmail();
								if (email != null && !email.trim().isEmpty() && Common.isValidEmailAddress(email)) {
									emailUser += emailUser.trim().isEmpty() ? email.trim() : "," + email.trim();
								}
							}

						}
					}

					if (pengumumanPerkuliahan.getBroadcastKeDosen()) {

						List<Dosen> dosens = pengumumanPerkuliahan.getPerkuliahan().populateDosenBuNama();
						for (Dosen d : dosens) {
							String email = d.getEmail();
							if (email != null && !email.trim().isEmpty() && Common.isValidEmailAddress(email)) {
								emailUser += emailUser.trim().isEmpty() ? email.trim() : "," + email.trim();
							}
						}

						if (!dosens.isEmpty()) {
							List<String> emails = HibernateUtil.currentSession().createCriteria(Tbmuser.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
									.add(Restrictions.in("dosen", dosens))
									.setProjection(Projections.groupProperty("userId")).list();
							for (String email : emails) {
								userIds.put(email);
							}
						}
					}

					System.out.println("broadcastEmail emailUser = " + emailUser);

					if (!emailUser.trim().isEmpty() || userIds.length() > 0) {
						String subject = "Pengumuman akademik => " + pengumumanPerkuliahan.getJudul();
						Tbmuser tbmuser = Common.getCurrentUser();
						String body = "Anda mendapatkan informasi pengumuman akademik \""
								+ pengumumanPerkuliahan.getJudul() + "\" oleh "
								+ (tbmuser != null ? tbmuser.getUserNama() + " (" + tbmuser.getUserId() + ")" : "");

						body += "<br>Isi pengumuman<hr>" + pengumumanPerkuliahan.getCatatan();

						body += "<br><br>Komentar<hr>";

						List<DiskusiPengumumanPerkuliahan> komentars = HibernateUtil.currentSession()
								.createCriteria(DiskusiPengumumanPerkuliahan.class)
								.add(Restrictions.eq("pengumumanPerkuliahan", pengumumanPerkuliahan)).list();
						body += "<ul>";
						for (DiskusiPengumumanPerkuliahan komentar : komentars) {
							tbmuser = komentar.getTbmuser();
							Mahasiswa mahasiswa = komentar.getMahasiswa();
							body += "<li>" + (tbmuser != null ? tbmuser.getUserNama() + " (" + tbmuser.getUserId() + ")"
									: (mahasiswa == null ? "" : mahasiswa.getNim() + " " + mahasiswa.getNama()));
							body += " : " + komentar.getCatatan() + " "
									+ Common.dateFormat.get().format(komentar.getTanggal());
							body += "</li>";
						}
						body += "</ul>";

						body += "<br><br><hr>Untuk informasi lebih lanjut bisa dilihat di "
								+ Common.getRequestHostWithProtocol()
								+ ", kemudian click pengumuman.<br><br>Terima Kasih";

						String sender = Common.getKonfigurasi("default_email", "info@zishof.com").getNilai();
						MailSender.sendMail(userIds, subject, body, sender, emailUser, pengumumanPerkuliahan);
					}
				}

			});
		}

	}

	public static void kirimEmailKeKorespondensi(final PengumumanPerkuliahan pengumumanPerkuliahan) {
		if (!pengumumanPerkuliahan.getCatatan().trim().isEmpty()
				&& !pengumumanPerkuliahan.getKorespondensi().isEmpty()) {
			Common.createDefaultTimer(new EventListener() {

				@SuppressWarnings("unchecked")
				@Override
				public void onEvent(Event arg0) throws Exception {
					String emailUser = "";

					JSONArray userIds = new JSONArray();
					for (String email : pengumumanPerkuliahan.getKorespondensi().trim().split(",")) {
						if (!email.trim().isEmpty()) {
							userIds.put(email);
						}
					}

					List<String> emails = HibernateUtil.currentSession().createCriteria(Tbmuser.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
							.add(Restrictions.in("userId", pengumumanPerkuliahan.getKorespondensi().trim().split(",")))
							.setProjection(Projections.groupProperty("email")).list();
					for (String email : emails) {
						if (email != null && !email.trim().isEmpty() && Common.isValidEmailAddress(email)) {
							emailUser += emailUser.trim().isEmpty() ? email.trim() : "," + email.trim();
						}
					}

					// System.out.println("emailUser = " + emailUser);

					if (!emailUser.trim().isEmpty() || userIds.length() > 0) {
						String subject = "Korespondensi pengumuman akademik => " + pengumumanPerkuliahan.getJudul();
						Tbmuser tbmuser = Common.getCurrentUser();
						String body = "Anda ditugaskan sebagai koresponsi pada pengumuman akademik \""
								+ pengumumanPerkuliahan.getJudul() + "\" oleh "
								+ (tbmuser != null ? tbmuser.getUserNama() + " (" + tbmuser.getUserId() + ")" : "");

						body += "<br>Isi pengumuman<hr>" + pengumumanPerkuliahan.getCatatan();

						body += "<br><br>Komentar<hr>";

						List<DiskusiPengumumanPerkuliahan> komentars = HibernateUtil.currentSession()
								.createCriteria(DiskusiPengumumanPerkuliahan.class)
								.add(Restrictions.eq("pengumumanPerkuliahan", pengumumanPerkuliahan)).list();
						body += "<ul>";
						for (DiskusiPengumumanPerkuliahan komentar : komentars) {
							tbmuser = komentar.getTbmuser();
							Mahasiswa mahasiswa = komentar.getMahasiswa();
							body += "<li>" + (tbmuser != null ? tbmuser.getUserNama() + " (" + tbmuser.getUserId() + ")"
									: (mahasiswa == null ? "" : mahasiswa.getNim() + " " + mahasiswa.getNama()));
							body += " : " + komentar.getCatatan() + " "
									+ Common.dateFormat.get().format(komentar.getTanggal());
							body += "</li>";
						}
						body += "</ul>";

						body += "<br><br><hr>Untuk informasi lebih lanjut bisa dilihat di "
								+ Common.getRequestHostWithProtocol()
								+ ", kemudian click pengumuman.<br><br>Terima Kasih";

						String sender = Common.getKonfigurasi("default_email", "info@zishof.com").getNilai();
						MailSender.sendMail(userIds, subject, body, sender, emailUser, pengumumanPerkuliahan);
					}
				}
			});
		}
	}

	@SuppressWarnings("unchecked")
	public void loadData(PengumumanPerkuliahan pengumumanPerkuliahan, MyDetail detail) {
		Session session = HibernateUtil.currentSession();
		List<DiskusiPengumumanPerkuliahan> diskusiPengumumanPerkuliahan = session
				.createCriteria(DiskusiPengumumanPerkuliahan.class).addOrder(Order.desc("id"))
				.add(Restrictions.eq("pengumumanPerkuliahan", pengumumanPerkuliahan))
				.add(Restrictions.ne("catatan", "")).add(Restrictions.isNotNull("catatan")).list();

		ListModel strset = new SimpleListModel(diskusiPengumumanPerkuliahan);
		gridKomentar.setRowRenderer(new DetailPengumumanRenderer(detail));
		gridKomentar.setModelCheckMobile(strset);
		gridKomentar.renderAll();

	}

	class DetailPengumumanRenderer extends ais.ui.util.MyRowRenderer {

		private MyDetail detail;

		public DetailPengumumanRenderer(MyDetail detail) {
			this.detail = detail;
			tbmuser = Common.getCurrentUser();
			mahasiswa = tbmuser == null ? null : tbmuser.getMahasiswa();
		}

		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");
			final DiskusiPengumumanPerkuliahan diskusiPengumumanPerkuliahan = (DiskusiPengumumanPerkuliahan) data;

			Vbox vbox = new Vbox();
			vbox.setParent(row);
			new Label(diskusiPengumumanPerkuliahan.getCatatan()).setParent(vbox);

			Hbox hboxA = new Hbox();
			hboxA.setParent(vbox);

			Hbox hbox = new Hbox();
			hbox.setParent(hboxA);
			LampiranLain.createDownloadUploadFileLain(hbox, diskusiPengumumanPerkuliahan.getId(),
					"Lampiran Komentar Pengumuman Perkuliahan", "Lampiran Komentar", false, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							lampiranLain = (LampiranLain) arg0.getData();

						}
					}, null, false, false, false, false);

			new Label(diskusiPengumumanPerkuliahan.getTanggal() == null ? ""
					: Common.dateFormat3.get().format(diskusiPengumumanPerkuliahan.getTanggal())).setParent(row);
			new Label(diskusiPengumumanPerkuliahan.getOleh()).setParent(row);

			// Kolom aksi rapi (pola MahasiswaAction): semua tombol dikumpulkan lalu dibungkus
			// kebab popup (⋯) via UIHelper.buatBarisAksi — kolom aksi jadi kecil dan konsisten.
			// Syarat tampil grup aksi dipindah ke pembungkus kebab agar perilakunya sama persis.
			final java.util.List<org.zkoss.zk.ui.Component> aksiButtons =
					new java.util.ArrayList<org.zkoss.zk.ui.Component>();
			final boolean bolehUbahKomentar =
					(diskusiPengumumanPerkuliahan.getTbmuser() != null && tbmuser != null && tbmuser.getUserId() != null
							&& diskusiPengumumanPerkuliahan.getTbmuser().getUserId().equals(tbmuser.getUserId()))
							|| (diskusiPengumumanPerkuliahan.getMahasiswa() != null && mahasiswa != null
									&& diskusiPengumumanPerkuliahan.getMahasiswa().getId().equals(mahasiswa.getId()));
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(diskusiPengumumanPerkuliahan, diskusiPengumumanPerkuliahan.getPengumumanPerkuliahan(), detail);
				}
			});
			aksiButtons.add(button);

			button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show(
							"Apakah Bapak/Ibu yakin ingin menghapus data ini? Data yang telah dihapus tidak dapat dikembalikan. Silakan tekan OK untuk melanjutkan penghapusan, atau Batal untuk membatalkannya.",
							"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {
											final PengumumanPerkuliahan pengumumanPerkuliahan = diskusiPengumumanPerkuliahan
													.getPengumumanPerkuliahan();
											Common.refreshDelete(diskusiPengumumanPerkuliahan);
											Common.createDefaultTimer(new EventListener() {

												@Override
												public void onEvent(Event arg0) throws Exception {
													displayDetailPertemuanFileContent(pengumumanPerkuliahan, detail);
												}
											});
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											MyMessageboxConfig.show(MyMessageboxConfig.format(
													"Mohon maaf, data ini tidak dapat dihapus karena masih berelasi dengan data lainnya. Rincian teknis kesalahan: {V1}. Langkah yang dapat dilakukan: (1) hapus terlebih dahulu data lain yang berelasi dengan data ini; (2) muat ulang halaman lalu coba kembali; (3) apabila masih gagal, mohon hubungi administrator sistem dengan menyertakan rincian kesalahan di atas.",
													e.getMessage()));
										}

									}

								}
							});

				}

			});
			aksiButtons.add(button);
			ais.ui.util.UIHelper.buatBarisAksi(row, 3, aksiButtons).setVisible(bolehUbahKomentar);

		}

	}

	public void setReadonly(Boolean readonly) {
		this.readonly = readonly;
	}

	public Boolean getReadonly() {
		return readonly;
	}

}
