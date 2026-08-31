package ais.action.master.sosial;

import java.io.File;
import java.util.ArrayList;
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
import ais.database.model.sosial.PenyaluranDonasi;
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

/**
 * Controller/action ZK untuk penyaluran donasi. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code Textbox searchnama}, {@code AmbilDataSatuanKerjaBanbox searchparent},
 * {@code boolean edit}, {@code boolean delete}, {@code PenyaluranDonasi penyaluranDonasi};
 * inisialisasi/lifecycle ({@code doBeforeCompose()}, {@code doAfterCompose()}, {@code init()}, {@code init()},
 * {@code initCriteria()}); pembacaan/pencarian ({@code tampilkanButton()}, {@code reloadDataGambar()}, {@code
 * onSearchDefault()}, {@code ambil()}, {@code ambilClass()}); mutasi data ({@code onSave()}, {@code
 * setPersetujuan()}); pelaporan/ekspor ({@code cetakData()}); operasi domain lain ({@code onAdd()}, {@code
 * form()}, {@code istilah()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut
 * di atas.</p>
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
public class PenyaluranDonasiAction extends GenericAutowireComposer
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

	private PenyaluranDonasi penyaluranDonasi;
	private MyToolbarbuttonConfig add;
	private AmbilDataSatuanKerjaBanbox satuanKerja;
	private Combobox programDonatur;

	private MyTextbox nama;
	private Textbox keterangan;
	private DisposisiSop disposisiSop;
	private MyTextbox kode;

	private A linkPeta;
	protected Rows myGridGaleri;
	protected HashMap<Long, LampiranLain> maps;
	private MyDatebox mulai;
	private MyDatebox sampai;

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

		String[] contents = new String[] { "id", "kode", "nama", "satuanKerja", "programDonatur", "gambars", "videos",
				"linkUrl", "tanggalPembuatan", "tanggalPersetujuan", "dibuatOleh", "disetujuiOleh" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(PenyaluranDonasi.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

//		MyToolbarbuttonConfig upload = Common.uploadData(this, PenyaluranDonasi.class, contents);
//		upload.setVisible(add.isVisible() && edit && delete);
//		add.getParent().appendChild(upload);

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});
	}

	class PenyaluranDonasiRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final PenyaluranDonasi penyaluranDonasi = (PenyaluranDonasi) arg1;

			Vbox a;
			(a = RevisiHelper.createNewRevisi(PenyaluranDonasi.class, penyaluranDonasi, (penyaluranDonasi.getNama())))
					.setParent(arg0);
			a.appendChild(new Label(penyaluranDonasi.getKode()));

			Vbox vbox = new Vbox();
			vbox.setParent(arg0);
			new Label(
					penyaluranDonasi.getProgramDonatur() == null ? "" : penyaluranDonasi.getProgramDonatur().getNama())
					.setParent(vbox);

			A lk = new A("Lokasi");
			lk.setHref(penyaluranDonasi.getProgramDonatur().getLinkPeta());
			lk.setTarget("_blank");
			lk.setParent(arg0);

			List<Long> ids = new ArrayList<Long>();
			for (String id : penyaluranDonasi.getProgramDonatur().getDonaturs().split(",")) {
				try {
					ids.add(Long.parseLong(id));
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sosial/PenyaluranDonasiAction.java:187");
					// TODO: handle exception
				}
			}

			new Label(Common.numberFormat.get().format(ids.size())).setParent(arg0);

			Vbox vbox2 = new Vbox();
			vbox2.setParent(arg0);
			vbox2.appendChild(new Label(penyaluranDonasi.getKeterangan()));
			if (penyaluranDonasi.getDisposisiSop() != null) {
				A aa;
				(aa = new A()).setParent(vbox2);
				aa.setStyle("font-size:9px;");
				UIClassHelper.applyReadMore(aa, "SOP " + penyaluranDonasi.getDisposisiSop().getKeterangan() + " ("
						+ penyaluranDonasi.getDisposisiSop().getSop().getNama() + ")");
				aa.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						TampilanAlurSopAction.prosess(penyaluranDonasi.getDisposisiSop().getId(), null, null, true,
								arg0.getTarget());
					}
				});
			}

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(penyaluranDonasi.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					penyaluranDonasi.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(penyaluranDonasi);
				}
			});

			Common.copyEditDeleteButtons(edit, delete, penyaluranDonasi, PenyaluranDonasiAction.this).setParent(arg0);

		}

	}

	public void onAdd(Event event) throws Exception {
		init(new PenyaluranDonasi());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		penyaluranDonasi = (PenyaluranDonasi) obj;
		init(penyaluranDonasi);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@SuppressWarnings("deprecation")
	@Override
	public MyGrid form(GeneralValueObject generalValueObject, DisposisiSop disposisiSop,
			final MyToolbarbuttonConfig save, EventListener setujui) throws Exception {
		this.penyaluranDonasi = (PenyaluranDonasi) generalValueObject;
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Penyaluran"));
		row.appendChild(kode = new MyTextbox(penyaluranDonasi.getKode()));
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Judul Penyaluran *"));
		row.appendChild(nama = new MyTextbox(penyaluranDonasi.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Penyaluran *"));
		mulai = new MyDatebox(penyaluranDonasi.getMulai());
		sampai = new MyDatebox(penyaluranDonasi.getSampai());

		Hbox hbox = new Hbox();
		row.appendChild(hbox);

		hbox.appendChild(mulai);
		hbox.appendChild(new Label(ais.common.Common.getBahasaConfig("s.d")));
		hbox.appendChild(sampai);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Satuan Kerja"));
		satuanKerja = new AmbilDataSatuanKerjaBanbox(true);
		satuanKerja
				.setValue(penyaluranDonasi.getSatuanKerja() == null ? "" : penyaluranDonasi.getSatuanKerja().getNama());
		satuanKerja.setAttribute("satuanKerja", penyaluranDonasi.getSatuanKerja());
		row.appendChild(satuanKerja);
		satuanKerja.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Program *"));
		row.appendChild(programDonatur = new Combobox());
		Common.insertCombo(programDonatur, new String[] { "nama", "kode" }, "keterangan", ProgramDonatur.class,
				Restrictions.eq("aktif", true));
		Common.selectComboItem(programDonatur, penyaluranDonasi.getProgramDonatur());
		programDonatur.setWidth("90%");
		programDonatur.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Link/Url Map Lokasi"));
		row.appendChild(linkPeta = new A(penyaluranDonasi.getProgramDonatur() == null ? ""
				: penyaluranDonasi.getProgramDonatur().getLinkPeta()));
		linkPeta.setWidth("90%");
		linkPeta.setTarget("_blank");

		final MyFormRow rowUsernameDisposisi = new MyFormRow();
		rowUsernameDisposisi.setParent(rows);
		rowUsernameDisposisi.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		rowUsernameDisposisi.appendChild(keterangan = new Textbox(penyaluranDonasi.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(2);

		final Rows rowsLampiran = new Rows();
		final EventListener eventListener = new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(rowsLampiran);
				penyaluranDonasi.setProgramDonatur((ProgramDonatur) (programDonatur.getSelectedItem() == null ? null
						: programDonatur.getSelectedItem().getValue()));

				if (penyaluranDonasi.getProgramDonatur() != null) {
					linkPeta.setLabel(penyaluranDonasi.getProgramDonatur().getLinkPeta());
					linkPeta.setHref(penyaluranDonasi.getProgramDonatur().getLinkPeta());
					List<Long> ids = new ArrayList<Long>();
					for (String id : penyaluranDonasi.getProgramDonatur().getDonaturs().split(",")) {
						try {
							ids.add(Long.parseLong(id));
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sosial/PenyaluranDonasiAction.java:350");
							// TODO: handle exception
						}
					}

					Session session = HibernateUtil.currentSession();
					List<Donatur> donaturs = ConstantValues.simpleList(session.createCriteria(Donatur.class)
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
							.add(ids.size() == 0 ? Restrictions.sqlRestriction("1!=1") : Restrictions.in("id", ids)),
							Donatur.class);

					for (final Donatur donatur : donaturs) {

						MyFormRow row = new MyFormRow();
						row.setValign("top");
						row.setParent(rowsLampiran);

						row.appendChild(new Label(donatur.getNama()));
						row.appendChild(new Label(donatur.getKeterangan()));
						row.appendChild(new Label(
								donatur.getGelombangDonatur() == null ? "" : donatur.getGelombangDonatur().getNama()));

					}
				}
			}

		};

		programDonatur.addEventListener("onChange", eventListener);

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

					if (penyaluranDonasi.getId() != null) {
						try {
							Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();
							List<LampiranLain> lampiranLains = streamingSession.createCriteria(LampiranLain.class)
									.addOrder(Order.asc("id")).add(Restrictions.eq("ref", penyaluranDonasi.getId()))
									.add(Restrictions.ilike("jenis", "Galery_PenyaluranDonasi_", MatchMode.START))
									.list();
							for (LampiranLain lampiran : lampiranLains) {
								maps.put(lampiran.getId(), lampiran);
							}

							StreamingHibernateUtil.getInstance().closeSession();

						} catch (Exception e1) {
							StreamingHibernateUtil.getInstance().rollbackTransaction();
							e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/sosial/PenyaluranDonasiAction.java:486");
						}
					}

					reloadDataGambar(penyaluranDonasi);
				}

			}
		};

		galeryEvent.onEvent(null);

		return grid;
	}

	private void tampilkanButton(final Hbox hboxGambar) {
		Common.clear(hboxGambar);
		LampiranLain.createDownloadUploadFileLain(hboxGambar, penyaluranDonasi.getId(),
				"Galery_PenyaluranDonasi_" + Common.getGeneratedBarCode(), "Galeri", false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						LampiranLain lainMahasiswaCover = (LampiranLain) arg0.getData();
						maps.put(lainMahasiswaCover.getId(), lainMahasiswaCover);
						reloadDataGambar(penyaluranDonasi);

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								tampilkanButton(hboxGambar);
							}
						});
					}
				});
	}

	private void reloadDataGambar(final PenyaluranDonasi penyaluranDonasi) throws Exception {
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

											reloadDataGambar(penyaluranDonasi);
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

	private void init(final PenyaluranDonasi penyaluranDonasi) throws Exception {
		this.penyaluranDonasi = penyaluranDonasi;
		addWindow.setTitle(penyaluranDonasi.getId() == null ? "Tambah Penyaluran Donasi" : "Ubah Penyaluran Donasi");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		disposisiSop=null;center.appendChild(form(penyaluranDonasi, disposisiSop, save, null));

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

		if (programDonatur.getSelectedItem() == null) {
			MyMessageboxConfig.show("Program harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (penyaluranDonasi.getId() != null) {
			penyaluranDonasi = (PenyaluranDonasi) session.load(PenyaluranDonasi.class, penyaluranDonasi.getId());

		}
		penyaluranDonasi.setMulai(mulai.getValue());
		penyaluranDonasi.setSampai(sampai.getValue());
		penyaluranDonasi.setKode(kode.getValue().trim());
		penyaluranDonasi.setSatuanKerja((SatuanKerja) satuanKerja.getAttribute("satuanKerja"));
		penyaluranDonasi.setProgramDonatur((ProgramDonatur) (programDonatur.getSelectedItem() == null ? null
				: programDonatur.getSelectedItem().getValue()));

		penyaluranDonasi.setNama(nama.getValue());

		if (disposisiSop != null && disposisiSop.getId() != null) {
			penyaluranDonasi.setDisposisiSop(disposisiSop);
		}

		if (penyaluranDonasi.getId() != null) {

			Common.refreshUpdate(session, penyaluranDonasi);
		} else {

			penyaluranDonasi.setTanggalPembuatan(WaktuUtil.getDate());
			penyaluranDonasi.setDibuatOleh(Common.getCurrentUser());
			session.save(penyaluranDonasi);
		}

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				try {
					Session session = StreamingHibernateUtil.getInstance().currentSession();

					for (LampiranLain lampiranLain : maps.values()) {

						if (lampiranLain.getId() != null) {
							session.refresh(lampiranLain);
							lampiranLain.setRef(penyaluranDonasi.getId());

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
		Criteria criteria = session.createCriteria(PenyaluranDonasi.class);

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

		List<PenyaluranDonasi> penyaluranDonasi = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(penyaluranDonasi);
		grid.setRowRenderer(new PenyaluranDonasiRenderer());
		grid.setModelCheckMobile(strset);

	}

	@Override
	public String istilah() throws Exception {
		// TODO Auto-generated method stub
		return "Penyaluran Donasi";
	}

	@Override
	public DataSop ambil() throws Exception {
		// TODO Auto-generated method stub
		return penyaluranDonasi;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Class ambilClass() throws Exception {
		// TODO Auto-generated method stub
		return PenyaluranDonasi.class;
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
