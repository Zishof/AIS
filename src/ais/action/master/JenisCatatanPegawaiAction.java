package ais.action.master;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Column;
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
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.JenisCatatanPegawai;
import ais.database.model.KelompokParameterTambahanCatatanPegawai;
import ais.database.model.file.FileFotoLain;
import ais.database.model.file.LampiranLain;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Controller/action ZK untuk jenis catatan pegawai. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code Textbox searchnama}, {@code Textbox nama}, {@code Textbox keterangan},
 * {@code boolean edit}, {@code boolean delete}; inisialisasi/lifecycle ({@code doBeforeCompose()}, {@code
 * doAfterCompose()}, {@code init()}, {@code init()}, {@code initKelompokParameterTambahanCatatanPegawai()},
 * {@code initCriteria()}); pembacaan/pencarian ({@code tampilkanButton()}, {@code reloadDataGambar()}, {@code
 * onSearchDefault()}); mutasi data ({@code onSave()}); operasi domain lain ({@code onAdd()}). Bagian lain dari
 * kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 * <p><b>Lifecycle:</b> instance mengikuti lifecycle komponen ZK dan menyimpan state layar; jangan digunakan
 * sebagai singleton atau dibagikan antar desktop/session. Event handler harus tetap memakai konteks pengguna
 * serta session Hibernate milik request yang aktif.</p>
 *
 * @see GenericAutowireComposer
 */
public class JenisCatatanPegawaiAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;

	private Textbox nama;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private JenisCatatanPegawai jenisCatatanPegawai;
	private MyToolbarbuttonConfig add;
	private Set<KelompokParameterTambahanCatatanPegawai> selectedKelompokParameterTambahanCatatanPegawai;
	protected LampiranLain lampiran;
	private HashMap<Long, LampiranLain> maps;
	private Rows rowsGridGaleri;
	protected LampiranLain lampiran1;

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

		String[] contents = new String[] { "id", "nama", "keterangan", "aktif" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, JenisCatatanPegawai.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});
	}

	class JenisCatatanPegawaiRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final JenisCatatanPegawai jenisCatatanPegawai = (JenisCatatanPegawai) arg1;

			RevisiHelper.createNewRevisi(JenisCatatanPegawai.class, jenisCatatanPegawai, jenisCatatanPegawai.getNama())
					.setParent(arg0);

			new Label(jenisCatatanPegawai.getKeterangan()).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(jenisCatatanPegawai.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					jenisCatatanPegawai.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(jenisCatatanPegawai);
				}
			});

			Common.copyEditDeleteButtons(edit, delete, jenisCatatanPegawai, JenisCatatanPegawaiAction.this)
					.setParent(arg0);

		}

	}

	public void onAdd(Event event) throws Exception {
		init(new JenisCatatanPegawai());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		jenisCatatanPegawai = (JenisCatatanPegawai) obj;
		init(jenisCatatanPegawai);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@SuppressWarnings({ "deprecation", "unchecked" })
	private void init(JenisCatatanPegawai jenisCatatanPegawai) {
		this.jenisCatatanPegawai = jenisCatatanPegawai;
		addWindow.setTitle(jenisCatatanPegawai.getId() == null ? "Tambah Jenis Catatan Pegawai" : "Ubah Jenis Catatan Pegawai");
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
		column.setWidth("30%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Jenis Catatan Pegawai *"));
		row.appendChild(nama = new Textbox(jenisCatatanPegawai.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(jenisCatatanPegawai.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		lampiran1 = null;
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("File Laporan Form (jrxml atau jasper)"));
		Hbox hbox = new Hbox();
		hbox.setParent(row);
		LampiranLain.createDownloadUploadFileLain(hbox, jenisCatatanPegawai.getId(),
				LampiranLain.FILE_JRXML_LAYOUT_JENIS_FORM_CATATAN_PEGAWAI, "File Laporan form jrxml", false,
				new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						lampiran1 = (LampiranLain) arg0.getData();
					}
				}, null, false, false, false, true);

		lampiran = null;
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("File Laporan Rekap (jrxml atau jasper)"));
		hbox = new Hbox();
		hbox.setParent(row);
		LampiranLain.createDownloadUploadFileLain(hbox, jenisCatatanPegawai.getId(),
				LampiranLain.FILE_JRXML_LAYOUT_JENIS_CATATAN_PEGAWAI, "File Laporan rekap jrxml", false,
				new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						lampiran = (LampiranLain) arg0.getData();
					}
				}, null, false, false, false, true);

		initKelompokParameterTambahanCatatanPegawai(rows);

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

			if (jenisCatatanPegawai.getId() != null) {
				try {
					Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();
					List<LampiranLain> lampiranLains = streamingSession.createCriteria(LampiranLain.class)
							.addOrder(Order.asc("id")).add(Restrictions.eq("ref", jenisCatatanPegawai.getId()))
							.add(Restrictions.ilike("jenis", "Catatan_Pegawai_", MatchMode.START)).list();
					for (LampiranLain lampiran : lampiranLains) {
						maps.put(lampiran.getId(), lampiran);
					}

					StreamingHibernateUtil.getInstance().closeSession();

				} catch (Exception e1) {
					StreamingHibernateUtil.getInstance().rollbackTransaction();
					e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/JenisCatatanPegawaiAction.java:323");
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
		LampiranLain.createDownloadUploadFileLain(hboxGambar, jenisCatatanPegawai.getId(),
				"Catatan_Pegawai_" + Common.getGeneratedBarCode(), "Gambar Pendukung", false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						LampiranLain lainMahapegawaiCover = (LampiranLain) arg0.getData();
						maps.put(lainMahapegawaiCover.getId(), lainMahapegawaiCover);
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

	@SuppressWarnings("deprecation")
	private void initKelompokParameterTambahanCatatanPegawai(Rows rows) {
		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		final MyGrid subGrid = new MyGrid();
		row.appendChild(subGrid);

		Columns subColumns = new Columns();
		subColumns.setParent(subGrid);
		Column c = new Column("Parameter Catatan Pegawai");
		subColumns.appendChild(c);

		Rows subRows = new Rows();
		subRows.setParent(subGrid);

		MyFormRow subRow = new MyFormRow();
		subRow.setStyle("border:0px;background: transparent;");
		subRow.setParent(subRows);
		subRow.setValign("top");

		@SuppressWarnings("unchecked")
		Map<Long, KelompokParameterTambahanCatatanPegawai> kelompokParameterTambahanCatatanPegawais = ConstantValues
				.ambilBerdasarClass(KelompokParameterTambahanCatatanPegawai.class);

		if (jenisCatatanPegawai != null && jenisCatatanPegawai.getId() != null) {
			HibernateUtil.currentSession().refresh(jenisCatatanPegawai);
		}

		selectedKelompokParameterTambahanCatatanPegawai = this.jenisCatatanPegawai
				.getKelompokParameterTambahanCatatanPegawais();
		Set<Long> ids = new HashSet<Long>();
		for (KelompokParameterTambahanCatatanPegawai v : selectedKelompokParameterTambahanCatatanPegawai) {
			ids.add(v.getId());
		}

		System.out.println("ids ->" + ids);

		Vbox vboxSkala = new Vbox();
		vboxSkala.setPack("top");
		vboxSkala.setParent(subRow);
		for (final KelompokParameterTambahanCatatanPegawai kelompokParameterTambahanCatatanPegawai : kelompokParameterTambahanCatatanPegawais
				.values()) {
			final Checkbox checkbox = new Checkbox(kelompokParameterTambahanCatatanPegawai.getNama());
			checkbox.setParent(vboxSkala);
			checkbox.setChecked(ids.contains(kelompokParameterTambahanCatatanPegawai.getId()));
			checkbox.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (checkbox.isChecked()) {
						selectedKelompokParameterTambahanCatatanPegawai.add(kelompokParameterTambahanCatatanPegawai);
					} else {

						for (KelompokParameterTambahanCatatanPegawai a : selectedKelompokParameterTambahanCatatanPegawai) {
							if (a.getId().equals(kelompokParameterTambahanCatatanPegawai.getId())) {
								selectedKelompokParameterTambahanCatatanPegawai.remove(a);
								break;
							}
						}

					}

					System.out.println("selectedKelompokParameterTambahanCatatanPegawai => "
							+ selectedKelompokParameterTambahanCatatanPegawai);
				}
			});
		}
	}

	public boolean onSave(Event event) throws Exception {
		if (nama.getValue().trim().equals("")) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Jenis catatan pegawai",
					"Kolom Nama Jenis catatan pegawai belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Nama Jenis catatan pegawai.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (jenisCatatanPegawai.getId() != null) {
			jenisCatatanPegawai = (JenisCatatanPegawai) session.load(JenisCatatanPegawai.class,
					jenisCatatanPegawai.getId());

		}

		jenisCatatanPegawai.setNama(nama.getValue());
		jenisCatatanPegawai.setKeterangan(keterangan.getValue());
		jenisCatatanPegawai
				.setKelompokParameterTambahanCatatanPegawais(selectedKelompokParameterTambahanCatatanPegawai);

		Common.refreshSaveOrUpdate(session, jenisCatatanPegawai);

		try {
			session = StreamingHibernateUtil.getInstance().currentSession();

			if (lampiran != null && lampiran.getId() != null) {
				session.refresh(lampiran);
				lampiran.setRef(jenisCatatanPegawai.getId());

				session.getTransaction().begin();
				session.update(lampiran);
				session.getTransaction().commit();
			}

			if (lampiran1 != null && lampiran1.getId() != null) {
				session.refresh(lampiran1);
				lampiran1.setRef(jenisCatatanPegawai.getId());

				session.getTransaction().begin();
				session.update(lampiran1);
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
						lampiran.setRef(jenisCatatanPegawai.getId());

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
		Criteria criteria = session.createCriteria(JenisCatatanPegawai.class);

		if (order)
			criteria.addOrder(Order.asc("nama"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<JenisCatatanPegawai> jenisCatatanPegawai = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(jenisCatatanPegawai);
		grid.setRowRenderer(new JenisCatatanPegawaiRenderer());
		grid.setModelCheckMobile(strset);

	}

}
