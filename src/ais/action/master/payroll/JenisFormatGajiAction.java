package ais.action.master.payroll;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Image;
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
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.file.FileFotoLain;
import ais.database.model.file.LampiranLain;
import ais.database.model.payroll.JenisFormatGaji;
import ais.database.model.rab.SatuanKerja;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class JenisFormatGajiAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private AmbilDataSatuanKerjaBanbox searchparent;

	private Textbox nama;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private JenisFormatGaji jenisFormatGaji;
	private MyToolbarbuttonConfig add;
	protected LampiranLain lampiran;
	private HashMap<Long, LampiranLain> maps;
	private AmbilDataSatuanKerjaBanbox satuanKerja;
	private Rows rowsGridGaleri;
	private SatuanKerjaTreeModel satuanKerjaTreeModel;

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

		String[] contents = new String[] { "id", "nama", "satuanKerja", "keterangan", "aktif", "perKeluarga",
				"perSatker", "perBank", "perSatkerFakultas", "perSatkerJurusan", "perSatkerSekolah", "queryManual" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, JenisFormatGaji.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});
	}

	class JenisFormatGajiRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final JenisFormatGaji jenisFormatGaji = (JenisFormatGaji) arg1;

			RevisiHelper.createNewRevisi(JenisFormatGaji.class, jenisFormatGaji, jenisFormatGaji.getNama())
					.setParent(arg0);
			new Label(jenisFormatGaji.getSatuanKerja() == null ? "" : jenisFormatGaji.getSatuanKerja().getNama())
					.setParent(arg0);

			new Label(jenisFormatGaji.getKeterangan()).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(jenisFormatGaji.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					jenisFormatGaji.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(jenisFormatGaji);
				}
			});

			final MyCheckboxConfig perKeluarga = new MyCheckboxConfig("Per-Keluarga");
			perKeluarga.setDisabled(!edit);
			perKeluarga.setChecked(jenisFormatGaji.getPerKeluarga());
			perKeluarga.setParent(arg0);
			perKeluarga.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					jenisFormatGaji.setPerKeluarga(perKeluarga.isChecked());
					Common.refreshSaveOrUpdate(jenisFormatGaji);
				}
			});

			final MyCheckboxConfig perSatker = new MyCheckboxConfig("Per-Satker");
			perSatker.setDisabled(!edit);
			perSatker.setChecked(jenisFormatGaji.getPerSatker());
			perSatker.setParent(arg0);
			perSatker.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					jenisFormatGaji.setPerSatker(perSatker.isChecked());
					Common.refreshSaveOrUpdate(jenisFormatGaji);
				}
			});

			final MyCheckboxConfig perBank = new MyCheckboxConfig("Per-Bank");
			perBank.setDisabled(!edit);
			perBank.setChecked(jenisFormatGaji.getPerBank());
			perBank.setParent(arg0);
			perBank.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					jenisFormatGaji.setPerBank(perBank.isChecked());
					Common.refreshSaveOrUpdate(jenisFormatGaji);
				}
			});

			final MyCheckboxConfig perFakultas = new MyCheckboxConfig("Per-Fakultas");
			perFakultas.setDisabled(!edit);
			perFakultas.setChecked(jenisFormatGaji.getPerSatkerFakultas());
			perFakultas.setParent(arg0);
			perFakultas.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					jenisFormatGaji.setPerSatkerFakultas(perFakultas.isChecked());
					Common.refreshSaveOrUpdate(jenisFormatGaji);
				}
			});

			final MyCheckboxConfig perJurusan = new MyCheckboxConfig("Per-Prodi");
			perJurusan.setDisabled(!edit);
			perJurusan.setChecked(jenisFormatGaji.getPerSatkerJurusan());
			perJurusan.setParent(arg0);
			perJurusan.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					jenisFormatGaji.setPerSatkerJurusan(perJurusan.isChecked());
					Common.refreshSaveOrUpdate(jenisFormatGaji);
				}
			});

			final MyCheckboxConfig perSekolah = new MyCheckboxConfig("Per-Sekolah");
			perSekolah.setDisabled(!edit);
			perSekolah.setChecked(jenisFormatGaji.getPerSatkerSekolah());
			perSekolah.setParent(arg0);
			perSekolah.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					jenisFormatGaji.setPerSatkerSekolah(perSekolah.isChecked());
					Common.refreshSaveOrUpdate(jenisFormatGaji);
				}
			});

			final MyCheckboxConfig queryManual = new MyCheckboxConfig("Query Manual");
			queryManual.setDisabled(!edit);
			queryManual.setChecked(jenisFormatGaji.getQueryManual());
			queryManual.setParent(arg0);
			queryManual.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					jenisFormatGaji.setPerSatkerSekolah(queryManual.isChecked());
					Common.refreshSaveOrUpdate(jenisFormatGaji);
				}
			});

			Common.copyEditDeleteButtons(edit, delete, jenisFormatGaji, JenisFormatGajiAction.this).setParent(arg0);

		}

	}

	public void onAdd(Event event) throws Exception {
		init(new JenisFormatGaji());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		jenisFormatGaji = (JenisFormatGaji) obj;
		init(jenisFormatGaji);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@SuppressWarnings({ "deprecation", "unchecked" })
	private void init(final JenisFormatGaji jenisFormatGaji) throws Exception {
		this.jenisFormatGaji = jenisFormatGaji;
		addWindow.setTitle(jenisFormatGaji.getId() == null ? "Tambah Jenis Format Gaji" : "Ubah Jenis Format Gaji");
		Common.clear(addWindow);
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Jenis Format Gaji *"));
		row.appendChild(nama = new Textbox(jenisFormatGaji.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Satuan Kerja"));
		satuanKerja = new AmbilDataSatuanKerjaBanbox(true, false);
		satuanKerja
				.setValue(jenisFormatGaji.getSatuanKerja() == null ? "" : jenisFormatGaji.getSatuanKerja().getNama());
		satuanKerja.setAttribute("satuanKerja", jenisFormatGaji.getSatuanKerja());
		row.appendChild(satuanKerja);
		satuanKerja.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(jenisFormatGaji.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		lampiran = null;
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("File Laporan (jrxml atau jasper)"));
		Hbox hbox = new Hbox();
		hbox.setParent(row);
		LampiranLain.createDownloadUploadFileLain(hbox, jenisFormatGaji.getId(),
				LampiranLain.FILE_JRXML_LAYOUT_JENIS_FORMAT_GAJI, "File Laporan jrxml", false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						lampiran = (LampiranLain) arg0.getData();
					}
				}, null, false, false, false, true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Gambar / Background Pendukung"));

		Hbox myHbox = new Hbox();
		myHbox.setParent(row);
		myHbox.setHeight("30px");

		Hbox hboxGambar = new Hbox();
		hboxGambar.setParent(myHbox);
		tampilkanButton(hboxGambar);

		row = new MyFormRow();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		MyGrid myGridGaleri = new MyGrid();
		myGridGaleri.setParent(row);

		rowsGridGaleri = new Rows();
		myGridGaleri.appendChild(rowsGridGaleri);

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
		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
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

		try {

			if (jenisFormatGaji.getId() != null) {
				try {
					Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();
					List<LampiranLain> lampiranLains = streamingSession.createCriteria(LampiranLain.class)
							.addOrder(Order.asc("id")).add(Restrictions.eq("ref", jenisFormatGaji.getId()))
							.add(Restrictions.ilike("jenis", "Jenis_Format_Gaji_", MatchMode.START)).list();
					for (LampiranLain lampiran : lampiranLains) {
						maps.put(lampiran.getId(), lampiran);
					}

					StreamingHibernateUtil.getInstance().closeSession();

				} catch (Exception e1) {
					StreamingHibernateUtil.getInstance().rollbackTransaction();
					e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/payroll/JenisFormatGajiAction.java:414");
				}
			}

			reloadDataGambar();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

	}

	private void tampilkanButton(final Hbox hboxGambar) {
		maps = new HashMap<Long, LampiranLain>();
		Common.clear(hboxGambar);
		LampiranLain.createDownloadUploadFileLain(hboxGambar, jenisFormatGaji.getId(),
				"Jenis_Format_Gaji_" + Common.getGeneratedBarCode(), "Gambar Pendukung", false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						LampiranLain lainMahasiswaCover = (LampiranLain) arg0.getData();
						maps.put(lainMahasiswaCover.getId(), lainMahasiswaCover);
						reloadDataGambar();
						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								tampilkanButton(hboxGambar);

							}
						});
					}
				});
	}

	private void reloadDataGambar() throws Exception {
		Common.clear(rowsGridGaleri);

		for (final LampiranLain lampiranLain : maps.values()) {

			String link = FileFotoLain.ambilLinkLampiranLain(lampiranLain, false, false, LampiranLain.class);
			MyFormRow roww = new MyFormRow();
			roww.setParent(rowsGridGaleri);

			Vbox vbox = new Vbox();
			vbox.setParent(roww);

			Image image = new Image(link);
			image.setStyle("max-width: 256px !important;min-width: 60px !important;min-height: 300px !important;");
			image.setSclass("gambar_profile");
			image.setWidth("90%");
			image.setParent(vbox);

			A a = new A(link);
			a.setParent(vbox);
			a.setTarget("_blank");
			a.setHref(link);

			final Textbox textbox = new Textbox(lampiranLain.getDeskripsi());
			textbox.setWidth("90%");
			textbox.setRows(3);
			textbox.setParent(vbox);

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

											reloadDataGambar();
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
			button.setParent(vbox);
		}
	}

	public boolean onSave(Event event) throws Exception {
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, kolom Nama Jenis Format Gaji belum diisi. Langkah yang dapat dilakukan: (1) isikan Nama Jenis Format Gaji pada kolom yang tersedia; (2) pastikan kolom tidak dikosongkan; (3) simpan kembali data ini. Jika masih mengalami kendala, hubungi Administrator.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (jenisFormatGaji.getId() != null) {
			jenisFormatGaji = (JenisFormatGaji) session.load(JenisFormatGaji.class, jenisFormatGaji.getId());

		}

		jenisFormatGaji.setNama(nama.getValue());
		jenisFormatGaji.setKeterangan(keterangan.getValue());
		jenisFormatGaji.setSatuanKerja((SatuanKerja) satuanKerja.getAttribute("satuanKerja"));

		Common.refreshSaveOrUpdate(session, jenisFormatGaji);

		try {
			session = StreamingHibernateUtil.getInstance().currentSession();

			if (lampiran != null && lampiran.getId() != null) {
				session.refresh(lampiran);
				lampiran.setRef(jenisFormatGaji.getId());

				session.getTransaction().begin();
				session.update(lampiran);
				session.getTransaction().commit();
			}

			StreamingHibernateUtil.getInstance().closeSession();
		} catch (Exception e) {
			StreamingHibernateUtil.getInstance().rollbackTransaction();
			Common.tampilErrorJikaAdmin(e);
		}

		if (maps != null && !maps.isEmpty()) {
			try {
				session = StreamingHibernateUtil.getInstance().currentSession();

				for (LampiranLain lampiran : maps.values()) {
					if (lampiran != null && lampiran.getId() != null) {
						session.refresh(lampiran);
						lampiran.setRef(jenisFormatGaji.getId());

						session.getTransaction().begin();
						session.update(lampiran);
						session.getTransaction().commit();
					}
				}

				StreamingHibernateUtil.getInstance().closeSession();
			} catch (Exception e) {
				StreamingHibernateUtil.getInstance().rollbackTransaction();
				Common.tampilErrorJikaAdmin(e);
			}
		}

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();

		SatuanKerja parent = (SatuanKerja) searchparent.getAttribute("satuanKerja");
		Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
		if (parent != null) {
			satuanKerjas.clear();
			satuanKerjas.add(parent);
			satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
		}

		Criteria criteria = session.createCriteria(JenisFormatGaji.class)
				.add(satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(
								parent == null ? Restrictions.isNull("satuanKerja")
										: Restrictions.sqlRestriction("false"),
								Restrictions.in("satuanKerja", satuanKerjas)));

		if (order)
			criteria.addOrder(Order.asc("nama"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))

		;
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<JenisFormatGaji> jenisFormatGaji = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(jenisFormatGaji);
		grid.setRowRenderer(new JenisFormatGajiRenderer());
		grid.setModelCheckMobile(strset);

	}

}
