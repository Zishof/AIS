package ais.action.master.sosial;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.generic.AmbilDataDonaturBanyak;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.action.master.sop.TampilanAlurSopAction;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.common.UIClassHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.file.FileFotoLain;
import ais.database.model.file.LampiranLain;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.sop.DataSop;
import ais.database.model.sop.DisposisiSop;
import ais.database.model.sosial.Donatur;
import ais.database.model.sosial.KategoriProgramDonatur;
import ais.database.model.sosial.ProgramDonatur;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.FormSop;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;

public class ProgramDonaturAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault, FormSop {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private AmbilDataSatuanKerjaBanbox searchparent;

	private boolean edit = false;
	private boolean delete = false;

	private ProgramDonatur programDonatur;
	private MyToolbarbuttonConfig add;
	private AmbilDataSatuanKerjaBanbox satuanKerja;
	private Combobox kategoriProgramDonatur;

	private MyTextbox nama;
	private Textbox keterangan;
	private DisposisiSop disposisiSop;
	private MyTextbox kode;

	private String dn = null;
	private MyDatebox mulai;
	private MyDatebox sampai;
	private MyTextbox linkPeta;
	protected Rows myGridGaleri;
	protected HashMap<Long, LampiranLain> maps;

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

		searchparent.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});

		satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		String[] contents = new String[] { "id", "kode", "nama", "tanggalMulai", "tanggalSampai", "satuanKerja",
				"kategoriProgramDonatur", "donaturs", "linkPeta", "gambars", "videos", "linkUrl", "tanggalPembuatan",
				"tanggalPersetujuan", "dibuatOleh", "disetujuiOleh" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(ProgramDonatur.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

//		MyToolbarbuttonConfig upload = Common.uploadData(this, ProgramDonatur.class, contents);
//		upload.setVisible(add.isVisible() && edit && delete);
//		add.getParent().appendChild(upload);

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});
	}

	class ProgramDonaturRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final ProgramDonatur programDonatur = (ProgramDonatur) arg1;

			Vbox a;
			(a = RevisiHelper.createNewRevisi(ProgramDonatur.class, programDonatur, (programDonatur.getNama())))
					.setParent(arg0);
			a.appendChild(new Label(programDonatur.getKode()));

			Vbox vbox = new Vbox();
			vbox.setParent(arg0);
			new Label(programDonatur.getKategoriProgramDonatur() == null ? ""
					: programDonatur.getKategoriProgramDonatur().getNama()).setParent(vbox);

			A lk = new A("Lokasi");
			lk.setHref(programDonatur.getLinkPeta());
			lk.setTarget("_blank");
			lk.setParent(arg0);

			List<Long> ids = new ArrayList<Long>();
			for (String id : programDonatur.getDonaturs().split(",")) {
				try {
					ids.add(Long.parseLong(id));
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sosial/ProgramDonaturAction.java:192");
					// TODO: handle exception
				}
			}

			new Label(Common.numberFormat.get().format(ids.size())).setParent(arg0);

			Vbox vbox2 = new Vbox();
			vbox2.setParent(arg0);
			vbox2.appendChild(new Label(programDonatur.getKeterangan()));
			if (programDonatur.getDisposisiSop() != null) {
				A aa;
				(aa = new A()).setParent(vbox2);
				aa.setStyle("font-size:9px;");
				UIClassHelper.applyReadMore(aa, "SOP " + programDonatur.getDisposisiSop().getKeterangan() + " ("
						+ programDonatur.getDisposisiSop().getSop().getNama() + ")");
				aa.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						TampilanAlurSopAction.prosess(programDonatur.getDisposisiSop().getId(), null, null, true,
								arg0.getTarget());
					}
				});
			}

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(programDonatur.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					programDonatur.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(programDonatur);
				}
			});

			Common.copyEditDeleteButtons(edit, delete, programDonatur, ProgramDonaturAction.this).setParent(arg0);

		}

	}

	public void onAdd(Event event) throws Exception {
		init(new ProgramDonatur());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		programDonatur = (ProgramDonatur) obj;
		init(programDonatur);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@SuppressWarnings("deprecation")
	@Override
	public MyGrid form(GeneralValueObject generalValueObject, DisposisiSop disposisiSop,
			final MyToolbarbuttonConfig save, EventListener setujui) throws Exception {
		this.programDonatur = (ProgramDonatur) generalValueObject;
		this.disposisiSop = (this.disposisiSop != null && (disposisiSop == null || disposisiSop.getId() == null)) ? this.disposisiSop : disposisiSop;

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Program"));
		row.appendChild(kode = new MyTextbox(programDonatur.getKode()));
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Judul Program *"));
		row.appendChild(nama = new MyTextbox(programDonatur.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Kegiatan *"));
		mulai = new MyDatebox(programDonatur.getTanggalMulai());
		sampai = new MyDatebox(programDonatur.getTanggalSampai());

		Hbox hbox = new Hbox();
		row.appendChild(hbox);

		hbox.appendChild(mulai);
		hbox.appendChild(new Label(ais.common.Common.getBahasaConfig("s.d")));
		hbox.appendChild(sampai);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Link/Url Map Lokasi"));
		row.appendChild(linkPeta = new MyTextbox(programDonatur.getLinkPeta()));
		linkPeta.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Satuan Kerja"));
		satuanKerja = new AmbilDataSatuanKerjaBanbox(true);
		satuanKerja.setValue(programDonatur.getSatuanKerja() == null ? "" : programDonatur.getSatuanKerja().getNama());
		satuanKerja.setAttribute("satuanKerja", programDonatur.getSatuanKerja());
		row.appendChild(satuanKerja);
		satuanKerja.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kategori Program *"));
		row.appendChild(kategoriProgramDonatur = new Combobox());
		Common.insertCombo(kategoriProgramDonatur, new String[] { "nama", "kode" }, "keterangan",
				KategoriProgramDonatur.class, Restrictions.eq("aktif", true));
		Common.selectComboItem(kategoriProgramDonatur, programDonatur.getKategoriProgramDonatur());
		kategoriProgramDonatur.setWidth("90%");
		kategoriProgramDonatur.setReadonly(true);

		final MyFormRow rowUsernameDisposisi = new MyFormRow();
		rowUsernameDisposisi.setParent(rows);
		rowUsernameDisposisi.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		rowUsernameDisposisi.appendChild(keterangan = new Textbox(programDonatur.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(2);

		final Rows rowsLampiran = new Rows();
		final EventListener eventListener = new EventListener() {

			public EventListener getThis() {
				return this;
			}

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event arg0) throws Exception {
				List<Long> ids = new ArrayList<Long>();
				for (String id : programDonatur.getDonaturs().split(",")) {
					try {
						ids.add(Long.parseLong(id));
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sosial/ProgramDonaturAction.java:349");
						// TODO: handle exception
					}
				}

				Session session = HibernateUtil.currentSession();
				List<Donatur> donaturs = ConstantValues.simpleList(session.createCriteria(Donatur.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(ids.size() == 0 ? Restrictions.sqlRestriction("1!=1") : Restrictions.in("id", ids)),
						Donatur.class);

				Common.clear(rowsLampiran);
				for (final Donatur donatur : donaturs) {

					MyFormRow row = new MyFormRow();
					row.setValign("top");
					row.setParent(rowsLampiran);

					row.appendChild(new Label(donatur.getNama()));
					row.appendChild(new Label(donatur.getKeterangan()));
					row.appendChild(new Label(
							donatur.getGelombangDonatur() == null ? "" : donatur.getGelombangDonatur().getNama()));
					if (programDonatur.getId() != null) {
						MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
						button.setTooltiptext("Hapus Data");
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
														Session session = HibernateUtil.currentSession();
														session.refresh(programDonatur);
														String dn = StringUtils.replace(programDonatur.getDonaturs(),
																"," + donatur.getId() + ",", "");

														programDonatur.setDonaturs(dn);

														Common.refreshUpdate(session, programDonatur);

														Common.createDefaultTimer(getThis());
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
					} else {
						row.appendChild(new Label());
					}
				}
			}

		};

		MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Ambil Donatur", "/img/user_male_add.png");

		dn = null;
		final MyFormRow rowAmbilPengguna = new MyFormRow();
		rowAmbilPengguna.setParent(rows);
		rowAmbilPengguna.appendChild(new ais.ui.util.MyLabelConfig(""));
		rowAmbilPengguna.appendChild(toolbarbutton);
		toolbarbutton.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event arg0) throws Exception {

				List<Long> ids = new ArrayList<Long>();
				for (String id : programDonatur.getDonaturs().split(",")) {
					try {
						ids.add(Long.parseLong(id));
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sosial/ProgramDonaturAction.java:437");
						// TODO: handle exception
					}
				}

				Session session = HibernateUtil.currentSession();
				List<Donatur> donatur = ConstantValues.simpleList(session.createCriteria(Donatur.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(ids.size() == 0 ? Restrictions.sqlRestriction("1!=1") : Restrictions.in("id", ids)),
						Donatur.class);

				AmbilDataDonaturBanyak ambil = new AmbilDataDonaturBanyak(donatur);
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(ambil);
				ambil.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Session session = HibernateUtil.currentSession();
						if (programDonatur.getId() != null) {
							session.refresh(programDonatur);
						}
						dn = programDonatur.getDonaturs();
						List<Donatur> tbmusers = (List<Donatur>) arg0.getData();
						if (tbmusers != null && tbmusers.size() != 0) {
							for (Donatur tbmuser : tbmusers) {
								dn += dn.isEmpty() ? tbmuser.getId() + "" : "," + tbmuser.getId();
							}
						}

						programDonatur.setDonaturs(dn);

						if (programDonatur.getId() != null) {
							Common.refreshUpdate(session, programDonatur);
						}
						Common.createDefaultTimer(eventListener);
					}
				});
				ambil.setWidth("850px");
				ambil.setHeight("97%");
				ambil.setVisible(true);
				ambil.onModal();
			}
		});

		MyFormRow rowLampiran = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(rowLampiran, "2");
		rowLampiran.setParent(rows);

		final Grid gridLampiran = new Grid();
		gridLampiran.setSclass("fgrid");
		gridLampiran.setParent(rowLampiran);

		columns = new Columns();
		columns.setParent(gridLampiran);

		column = new MyColumnConfig("Nama");
		column.setParent(columns);
		column.setWidth("30%");

		column = new MyColumnConfig("Alamat");
		column.setParent(columns);

		column = new MyColumnConfig("Masa");
		column.setParent(columns);
		column.setWidth("30%");

		column = new MyColumnConfig("Hps");
		column.setParent(columns);
		column.setWidth("8%");

		rowsLampiran.setParent(gridLampiran);

		Common.createDefaultTimer(eventListener);

		final MyFormRow rowLampiranGaleri = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(rowLampiranGaleri, "2");
		rowLampiranGaleri.setParent(rows);

		EventListener galeryEvent = new EventListener() {

			@SuppressWarnings({ "unchecked" })
			@Override
			public void onEvent(Event arg0) throws Exception {
				if (rowLampiranGaleri.getChildren().isEmpty()) {

					maps = new HashMap<Long, LampiranLain>();

					Grid grid = new Grid();
					grid.setSclass("dgrid");
					grid.setWidth("100%");
					grid.setParent(rowLampiranGaleri);
					grid.setWidth("100%");
					grid.setHeight("100%");

					Columns columns = new Columns();
					MyColumnConfig column = new MyColumnConfig();
					column.setWidth("15%");
					columns.appendChild(column);
					column = new MyColumnConfig();
					columns.appendChild(column);
					grid.appendChild(columns);

					Rows rows = new Rows();
					rows.setParent(grid);

					MyFormRow row = new MyFormRow();
					row.setValign("top");
					row.setParent(rows);
					row.appendChild(new ais.ui.util.MyLabelConfig("Galeri"));

					Hbox myHbox = new Hbox();
					myHbox.setParent(row);
					myHbox.setHeight("30px");

					Hbox hboxGambar = new Hbox();
					hboxGambar.setParent(myHbox);
					tampilkanButton(hboxGambar);

					row = new MyFormRow();
					row.setParent(rows);
					ais.ui.util.ZkCompat.setSpans(row, "2");

					myGridGaleri = (Rows) Common.tampilanScroll1(row).getParent();

					columns = new Columns();
					columns.setParent(myGridGaleri.getGrid());

					column = new MyColumnConfig("Foto / Video");
					column.setWidth("60%");
					column.setParent(columns);

					column = new MyColumnConfig("Keterangan");
					column.setWidth("30%");
					column.setParent(columns);

					column = new MyColumnConfig("Hapus");
					column.setWidth("10%");
					column.setParent(columns);

					if (programDonatur.getId() != null) {
						try {
							Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();
							List<LampiranLain> lampiranLains = streamingSession.createCriteria(LampiranLain.class)
									.addOrder(Order.asc("id")).add(Restrictions.eq("ref", programDonatur.getId()))
									.add(Restrictions.ilike("jenis", "Galery_ProgramDonatur_", MatchMode.START)).list();
							for (LampiranLain lampiran : lampiranLains) {
								maps.put(lampiran.getId(), lampiran);
							}

							StreamingHibernateUtil.getInstance().closeSession();

						} catch (Exception e1) {
							StreamingHibernateUtil.getInstance().rollbackTransaction();
							e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/sosial/ProgramDonaturAction.java:590");
						}
					}

					reloadDataGambar(programDonatur);
				}

			}
		};

		galeryEvent.onEvent(null);

		return grid;
	}

	private void tampilkanButton(final Hbox hboxGambar) {
		Common.clear(hboxGambar);
		LampiranLain.createDownloadUploadFileLain(hboxGambar, programDonatur.getId(),
				"Galery_ProgramDonatur_" + Common.getGeneratedBarCode(), "Galeri", false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						LampiranLain lainMahasiswaCover = (LampiranLain) arg0.getData();
						maps.put(lainMahasiswaCover.getId(), lainMahasiswaCover);
						reloadDataGambar(programDonatur);

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								tampilkanButton(hboxGambar);
							}
						});
					}
				});
	}

	private void reloadDataGambar(final ProgramDonatur programDonatur) throws Exception {
		Common.clear(myGridGaleri);

		for (final LampiranLain lampiranLain : maps.values()) {
			MyFormRow row = new MyFormRow();
			row.setValign("top");
			row.setParent(myGridGaleri);

			String link = FileFotoLain.ambilLinkLampiranLain(lampiranLain, false, false, LampiranLain.class);

			Common.displayUrlContent(link, row);

			final Textbox textbox = new Textbox(lampiranLain.getDeskripsi());
			textbox.setWidth("90%");
			textbox.setRows(7);
			textbox.setParent(row);

			textbox.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					try {
						Session session = StreamingHibernateUtil.getInstance().currentSession();

						session.refresh(lampiranLain);
						lampiranLain.setDeskripsi(textbox.getValue());

						session.getTransaction().begin();
						session.update(lampiranLain);
						session.getTransaction().commit();

						StreamingHibernateUtil.getInstance().closeSession();
					} catch (Exception e) {
						StreamingHibernateUtil.getInstance().rollbackTransaction();
						Common.tampilErrorJikaAdmin(e);
					}

				}
			});

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
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

											LampiranLain d = maps.remove(lampiranLain.getId());
											System.out.println("d = > " + d);

											try {
												Session session = StreamingHibernateUtil.getInstance().currentSession();

												session.getTransaction().begin();
												session.delete(lampiranLain);
												session.getTransaction().commit();

												StreamingHibernateUtil.getInstance().closeSession();
											} catch (Exception e) {
												StreamingHibernateUtil.getInstance().rollbackTransaction();
												Common.tampilErrorJikaAdmin(e);
											}

											reloadDataGambar(programDonatur);
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

	private void init(final ProgramDonatur programDonatur) throws Exception {
		this.programDonatur = programDonatur;
		addWindow.setTitle(programDonatur.getId() == null ? "Tambah Program Donasi" : "Ubah Program Donasi");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		disposisiSop=null;center.appendChild(form(programDonatur, disposisiSop, save, null));

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
				addWindow.setVisible(false);
			}
		});
		cancel.setParent(toolbar);

		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (onSave(event)) {
					onSearchDefault(null);
					addWindow.setVisible(false);
				}
			}
		});
		save.setParent(toolbar);

		borderlayout.setParent(addWindow);

	}

	public boolean onSave(Event event) throws Exception {

		if (kategoriProgramDonatur.getSelectedItem() == null) {
			MyMessageboxConfig.show("Kategori harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (programDonatur.getId() != null) {
			programDonatur = (ProgramDonatur) session.load(ProgramDonatur.class, programDonatur.getId());

		}
		programDonatur.setKode(kode.getValue().trim());
		programDonatur.setTanggalMulai(mulai.getValue());
		programDonatur.setTanggalSampai(sampai.getValue());
		programDonatur.setSatuanKerja((SatuanKerja) satuanKerja.getAttribute("satuanKerja"));
		programDonatur.setKategoriProgramDonatur(
				(KategoriProgramDonatur) (kategoriProgramDonatur.getSelectedItem() == null ? null
						: kategoriProgramDonatur.getSelectedItem().getValue()));

		programDonatur.setNama(nama.getValue());
		programDonatur.setLinkPeta(linkPeta.getValue().trim());

		if (dn != null) {
			programDonatur.setDonaturs(dn);
		}

		if (disposisiSop != null && disposisiSop.getId() != null) {
			programDonatur.setDisposisiSop(disposisiSop);
		}

		if (programDonatur.getId() != null) {

			Common.refreshUpdate(session, programDonatur);
		} else {

			programDonatur.setTanggalPembuatan(WaktuUtil.getDate());
			programDonatur.setDibuatOleh(Common.getCurrentUser());
			session.save(programDonatur);
		}

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				try {
					Session session = StreamingHibernateUtil.getInstance().currentSession();

					for (LampiranLain lampiranLain : maps.values()) {

						if (lampiranLain.getId() != null) {
							session.refresh(lampiranLain);
							lampiranLain.setRef(programDonatur.getId());

							session.getTransaction().begin();
							session.update(lampiranLain);
							session.getTransaction().commit();
						}
					}

					StreamingHibernateUtil.getInstance().closeSession();
				} catch (Exception e) {
					StreamingHibernateUtil.getInstance().rollbackTransaction();
					Common.tampilErrorJikaAdmin(e);
				}
			}
		});

		return true;
	}

	private SatuanKerjaTreeModel satuanKerjaTreeModel;
	private boolean persetujuan = false;

	public Criteria initCriteria(boolean order) {

		SatuanKerja parent = (SatuanKerja) searchparent.getAttribute("satuanKerja");
		Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
		if (parent != null) {
			satuanKerjas.clear();
			satuanKerjas.add(parent);
			satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
		}

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(ProgramDonatur.class);

		if (order)
			criteria.addOrder(Order.desc("id"));
		criteria

				.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.or(Restrictions.ilike("kode", searchnama.getValue().trim(), MatchMode.ANYWHERE),
								Restrictions.or(
										Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE),
										Restrictions.ilike("keterangan", searchnama.getValue().trim(),
												MatchMode.ANYWHERE))))

				.add(satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.isNull("satuanKerja"),
								Restrictions.or(
										parent == null ? Restrictions.isNull("satuanKerja")
												: Restrictions.sqlRestriction("false"),
										Restrictions.in("satuanKerja", satuanKerjas))))

		;
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<ProgramDonatur> programDonatur = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(programDonatur);
		grid.setRowRenderer(new ProgramDonaturRenderer());
		grid.setModelCheckMobile(strset);

	}

	@Override
	public String istilah() throws Exception {
		// TODO Auto-generated method stub
		return "Program Donatur";
	}

	@Override
	public DataSop ambil() throws Exception {
		// TODO Auto-generated method stub
		return programDonatur;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Class ambilClass() throws Exception {
		// TODO Auto-generated method stub
		return ProgramDonatur.class;
	}

	@Override
	public void setPersetujuan(boolean persetujuan) {
		this.persetujuan = persetujuan;
	}

	@Override
	public File cetakData(GeneralValueObject generalValueObject) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}
}
