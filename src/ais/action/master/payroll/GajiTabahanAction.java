package ais.action.master.payroll;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Window;

import ais.action.master.akunting.helper.AmbilDataPegawaiBanbox;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.generic.AmbilDataPegawaiBanyak;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.Pegawai;
import ais.database.model.payroll.AdjusVariablePenggajian;
import ais.database.model.payroll.Cabang;
import ais.database.model.payroll.Departemen;
import ais.database.model.payroll.GajiTabahan;
import ais.database.model.payroll.LevelJabatan;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Controller/action ZK untuk gaji tabahan. Tipe ini merupakan titik masuk UI yang menghubungkan
 * event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Window addWindow}, {@code MyGrid grid},
 * {@code Paging paging}, {@code MyTextbox searchkode}, {@code MyTextbox searchnama}, {@code MyTextbox
 * searchpegawai}, {@code MyTextbox searchketerangan}, {@code MyTextbox kode}; inisialisasi/lifecycle ({@code
 * doAfterCompose()}, {@code init()}, {@code init()}, {@code initCriteria()}); pembacaan/pencarian ({@code
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
public class GajiTabahanAction extends GenericAutowireComposer
		implements DataInitDefault, DataSearchDefault, DataCriteria {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private Window addWindow;
	private MyGrid grid;
	private Paging paging;

	private MyTextbox searchkode;
	private MyTextbox searchnama;
	private MyTextbox searchpegawai;
	private MyTextbox searchketerangan;

	private MyTextbox kode;
	private MyTextbox nama;
	private MyDatebox mulai;
	private MyDatebox sampai;

	private Combobox cabang;
	private Combobox departemen;
	private Combobox levelJabatan;

	private Column kode_col;
	private Column mulai_col;
	private Column sampai_col;
	private Column cabang_col;
	private Column departemen_col;
	private Column jabatan_col;

	private MyTextbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private GajiTabahan gajiTabahan;
	private MyToolbarbuttonConfig add;
	private AmbilDataPegawaiBanbox pegawai;

	private AdjusVariablePenggajian adjusVariablePenggajian = null;

	@Override
	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		try {
			if (execution.getParameter("adjusVariablePenggajian") != null) {
				adjusVariablePenggajian = (AdjusVariablePenggajian) ConstantValues.ambil(
						AdjusVariablePenggajian.class.getName(),
						Long.parseLong(execution.getParameter("adjusVariablePenggajian")));
			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		if (adjusVariablePenggajian != null) {

			kode_col.setVisible(false);
			mulai_col.setVisible(false);
			sampai_col.setVisible(false);
			cabang_col.setVisible(false);
			departemen_col.setVisible(false);
			jabatan_col.setVisible(false);

			add.setVisible(false);

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Ambil Pegawai", "/img/add_item.png");
			button.setDisabled((add != null && add.isVisible()) && edit && delete);
			button.addEventListener("onClick", new EventListener() {

				@SuppressWarnings("unchecked")
				@Override
				public void onEvent(Event event) throws Exception {
					Session session = HibernateUtil.currentSession();

					List<Pegawai> pegawais = session.createCriteria(GajiTabahan.class)
							.setProjection(Projections.groupProperty("pegawai")).add(Restrictions.isNotNull("pegawai"))
							.add(Restrictions.eq("adjusVariablePenggajian", adjusVariablePenggajian)).list();

					AmbilDataPegawaiBanyak ambilDataPegawaiBanyak = new AmbilDataPegawaiBanyak(pegawais);
					ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(ambilDataPegawaiBanyak);
					ambilDataPegawaiBanyak.setEventListener(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							List<Pegawai> pegawais = (List<Pegawai>) arg0.getData();

							for (Pegawai pegawai : pegawais) {
								GajiTabahan gajiTabahan = new GajiTabahan();
								gajiTabahan.setPegawai(pegawai);
								gajiTabahan.setKeterangan("");
								gajiTabahan.setAdjusVariablePenggajian(adjusVariablePenggajian);
								Common.refreshSaveOrUpdate(gajiTabahan);
							}

							onSearchDefault(null);
						}
					});
					ambilDataPegawaiBanyak.setWidth("850px");
					ambilDataPegawaiBanyak.setHeight("97%");
					ambilDataPegawaiBanyak.setVisible(true);
					ambilDataPegawaiBanyak.onModal();
				}

			});
			button.setParent(add.getParent());

			String[] contents = new String[] { "pegawai", "nama||formula||nilai" };

			MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
			Common.appendKeToolbar(cetakToolbarbutton, add, comp);

			MyToolbarbuttonConfig upload = new MyToolbarbuttonConfig("Upload" + Common.ukuranLabelFileUpload(),
					"/img/excel.png");
			upload.setVisible(edit && delete);
			upload.setUpload(Common.ukuranFileUpload());
			upload.addEventListener("onUpload", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					UploadEvent uploadEvent = (UploadEvent) event;
					final Media media = uploadEvent.getMedia();
					if (!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media))
						return;

					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							if (media.getName().toLowerCase().endsWith("xlsx")) {

								InputStream inputStream = media.getStreamData();
								// System.out.println("media = " + media);
								File file = new File(
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

								XSSFWorkbook workbook = new XSSFWorkbook(file.getAbsolutePath());
								XSSFSheet sheet = workbook.getSheetAt(0);

								String peringatan = "";
								for (int i = 1; i < (sheet.getLastRowNum() + 1); i++) {
									try {

										Session session = HibernateUtil.currentNativeSession();

										Pegawai pegawai = (Pegawai) Common.getSheetContentAsObject(sheet, 0, i,
												Pegawai.class);
										if (pegawai != null) {
											String nama = Common.getSheetContentAsString(sheet, 1, i);
											GajiTabahan gajiTabahan = (GajiTabahan) session
													.createCriteria(GajiTabahan.class)
													.add(Restrictions.eq("pegawai", pegawai)).add(Restrictions
															.eq("adjusVariablePenggajian", adjusVariablePenggajian))
													.setMaxResults(1).uniqueResult();

											if (gajiTabahan == null) {
												gajiTabahan = new GajiTabahan();
											}
											gajiTabahan.setPegawai(pegawai);
											gajiTabahan.setNama(nama);
											gajiTabahan.setAdjusVariablePenggajian(adjusVariablePenggajian);

											session.getTransaction().begin();
											session.saveOrUpdate(gajiTabahan);
											session.getTransaction().commit();

											// session.disconnect();
											if (session.isOpen()) {session.disconnect();session.close();}
											HibernateUtil.closeSession();
										}

									} catch (Exception e) {
										Common.tampilErrorJikaAdmin(e);
									}

								}

								MyMessageboxConfig.show(
										"Upload data berhasil dilakukan."
												+ (peringatan.isEmpty() ? "" : "\n" + peringatan),
										"Pemberitahuan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
										new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {
												onSearchDefault(null);
											}
										});

							} else {
								MyMessageboxConfig.show(
										"File yang anda upload harus ber-format Excel Open XML Spreadsheet (xlsx). Jika masih menggunakan format lain, buka file excel tersebut, kemudian Save As Excel Open XML Spreadsheet (xlsx). "
												+ media,
										"Error", MyMessageboxConfig.OK, MyMessageboxConfig.ERROR);
							}
						}
					});
				}
			});
			Common.appendKeToolbar(upload, add, comp);

		} else {
			String[] contents = new String[] { "id", "kode", "nama", "keterangan", "mulai", "sampai", "pegawai",
					"cabang", "departemen", "levelJabatan", "adjusVariablePenggajian" };

			MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
			Common.appendKeToolbar(cetakToolbarbutton, add, comp);

			MyToolbarbuttonConfig upload = Common.uploadData(this, GajiTabahan.class, contents);
			upload.setVisible((add != null && add.isVisible()) && edit && delete);
			Common.appendKeToolbar(upload, add, comp);
		}
	}

	class GajiTabahanRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final GajiTabahan gajiTabahan = (GajiTabahan) arg1;

			new Label(gajiTabahan.getKode()).setParent(arg0);

			RevisiHelper.createNewRevisi(GajiTabahan.class, gajiTabahan, gajiTabahan.getNama()).setParent(arg0);
			new Label(gajiTabahan.getMulai() == null ? "" : Common.dateFormat6.get().format(gajiTabahan.getMulai()))
					.setParent(arg0);
			new Label(gajiTabahan.getSampai() == null ? "" : Common.dateFormat6.get().format(gajiTabahan.getSampai()))
					.setParent(arg0);
			new Label(gajiTabahan.getCabang() == null ? "Semua" : gajiTabahan.getCabang().toString()).setParent(arg0);
			new Label(gajiTabahan.getDepartemen() == null ? "Semua" : gajiTabahan.getDepartemen().toString())
					.setParent(arg0);
			new Label(gajiTabahan.getLevelJabatan() == null ? "Semua" : gajiTabahan.getLevelJabatan().toString())
					.setParent(arg0);
			new Label(gajiTabahan.getPegawai() == null ? "Semua" : gajiTabahan.getPegawai().getNama()).setParent(arg0);
			new Label(gajiTabahan.getKeterangan()).setParent(arg0);

			Common.copyEditDeleteButtons(edit, delete, gajiTabahan, GajiTabahanAction.this).setParent(arg0);
		}

	}

	public void onAdd(Event event) throws Exception {
		init(new GajiTabahan());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		init((GajiTabahan) obj);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(GajiTabahan gajiTabahan) {
		this.gajiTabahan = gajiTabahan;
		addWindow.setTitle(gajiTabahan.getId() == null ? "Tambah Variable Gaji" : "Ubah Variable Gaji");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		MyGrid grid = new MyGrid();
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode *"));

		kode = new MyTextbox(gajiTabahan.getKode());
		if (adjusVariablePenggajian != null) {
			new Label(gajiTabahan.getKode()).setParent(row);
		} else {
			row.appendChild(kode);
		}
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Formula *"));
		row.appendChild(nama = new MyTextbox(gajiTabahan.getNama() == null ? "" : gajiTabahan.getNama()));
		nama.setWidth("90%");
		nama.setRows(4);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Variable Gaji berlaku mulai *"));
		mulai = new MyDatebox(gajiTabahan.getMulai());

		if (adjusVariablePenggajian != null) {
			new Label(gajiTabahan.getMulai() == null ? "" : Common.dateFormat6.get().format(gajiTabahan.getMulai()))
					.setParent(row);
		} else {
			mulai.setParent(row);
		}

		mulai.setReadonly(false);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Variable Gaji berlaku sampai"));
		sampai = new MyDatebox(gajiTabahan.getSampai());
		if (adjusVariablePenggajian != null) {
			new Label(gajiTabahan.getSampai() == null ? "" : Common.dateFormat6.get().format(gajiTabahan.getSampai()))
					.setParent(row);
		} else {
			sampai.setParent(row);
		}
		sampai.setWidth("90%");
		sampai.setReadonly(false);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pegawai"));
		row.appendChild(pegawai = new AmbilDataPegawaiBanbox());
		pegawai.setAttribute("pegawai", gajiTabahan.getPegawai());
		pegawai.setAttribute("myValue", gajiTabahan.getPegawai());
		pegawai.setValue(gajiTabahan.getPegawai() == null ? "" : gajiTabahan.getPegawai().getNama());
		pegawai.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Cabang")));
		row.appendChild(cabang = new Combobox());
		Common.insertCombo(cabang, "nama", Cabang.class);
		Common.selectComboItem(cabang, gajiTabahan.getCabang());
		cabang.setWidth("90%");
		Common.sisipkanSemuaDiCombo(cabang, null);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Departemen")));
		row.appendChild(departemen = new Combobox());
		Common.insertCombo(departemen, "nama", Departemen.class);
		Common.selectComboItem(departemen, gajiTabahan.getDepartemen());
		departemen.setWidth("90%");
		Common.sisipkanSemuaDiCombo(departemen, null);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Jabatan")));
		row.appendChild(levelJabatan = new Combobox());
		Common.insertCombo(levelJabatan, "nama", LevelJabatan.class);
		Common.selectComboItem(levelJabatan, gajiTabahan.getLevelJabatan());
		levelJabatan.setWidth("90%");
		Common.sisipkanSemuaDiCombo(levelJabatan, null);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Keterangan")));
		row.appendChild(
				keterangan = new MyTextbox(gajiTabahan.getKeterangan() == null ? "" : gajiTabahan.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("30px");
		toolbar.setParent(south);
		Toolbarbutton cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				addWindow.setVisible(false);
			}
		});
		cancel.setParent(toolbar);
		Toolbarbutton save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (onSave(event)) {
					onSearchDefault(null);
					Common.initPaging(paging, new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							onSearchDefault(null);
						}
					});
					addWindow.setVisible(false);
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);

	}

	public boolean onSave(Event event) throws Exception {
		if (kode.getValue().trim().equals("")) {
			MyMessageboxConfig.show(
					"Mohon maaf, kolom Kode belum diisi. Kolom Kode wajib diisi terlebih dahulu. Langkah yang dapat dilakukan: (1) mohon Bapak/Ibu mengisi kolom Kode; (2) pastikan kolom tersebut tidak dikosongkan; (3) kemudian tekan tombol Simpan kembali.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show(
					"Mohon maaf, kolom Formula belum diisi. Kolom Formula wajib diisi terlebih dahulu. Langkah yang dapat dilakukan: (1) mohon Bapak/Ibu mengisi kolom Formula; (2) pastikan kolom tersebut tidak dikosongkan; (3) kemudian tekan tombol Simpan kembali.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (mulai.getValue() == null) {
			MyMessageboxConfig.show(
					"Mohon maaf, tanggal Mulai berlaku belum diisi. Kolom Mulai berlaku wajib diisi terlebih dahulu. Langkah yang dapat dilakukan: (1) mohon Bapak/Ibu memilih tanggal Mulai berlaku; (2) pastikan tanggal telah ditentukan; (3) kemudian tekan tombol Simpan kembali.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (gajiTabahan.getId() != null) {
			gajiTabahan = (GajiTabahan) session.load(GajiTabahan.class, gajiTabahan.getId());

		}
		gajiTabahan.setPegawai((Pegawai) pegawai.getAttribute("pegawai"));
		gajiTabahan.setCabang(
				(Cabang) (cabang.getSelectedItem() == null || cabang.getSelectedItem().getValue() == null ? null
						: cabang.getSelectedItem().getValue()));
		gajiTabahan.setDepartemen(
				(Departemen) (departemen.getSelectedItem() == null || departemen.getSelectedItem().getValue() == null
						? null
						: departemen.getSelectedItem().getValue()));
		gajiTabahan.setLevelJabatan((LevelJabatan) (levelJabatan.getSelectedItem() == null
				|| levelJabatan.getSelectedItem().getValue() == null ? null
						: levelJabatan.getSelectedItem().getValue()));
		gajiTabahan.setKode(kode.getValue().trim());
		gajiTabahan.setMulai(mulai.getValue());
		gajiTabahan.setSampai(sampai.getValue());
		gajiTabahan.setNama(nama.getValue().trim());
		gajiTabahan.setKeterangan(keterangan.getValue());
		gajiTabahan.setAdjusVariablePenggajian(adjusVariablePenggajian);

		if (gajiTabahan.getId() != null) {
			Common.refreshUpdate(session, gajiTabahan);
		} else {
			session.save(gajiTabahan);
			session.flush();
		}
		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(GajiTabahan.class)

				.add(adjusVariablePenggajian == null ? Restrictions.isNull("adjusVariablePenggajian")
						: Restrictions.eq("adjusVariablePenggajian", adjusVariablePenggajian))

				.add((searchkode == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchkode.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("kode", searchkode.getValue().trim(), MatchMode.ANYWHERE)))
				.add((searchketerangan == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchketerangan.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("keterangan", searchketerangan.getValue().trim(), MatchMode.ANYWHERE)))
				.add((searchnama == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE)));

		if (!searchpegawai.getValue().trim().isEmpty()) {
			criteria.createAlias("pegawai", "pegawai").add((searchpegawai == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.or(
					Restrictions.ilike("pegawai.nama", searchpegawai.getValue().trim(), MatchMode.ANYWHERE),
					Restrictions.or(
							Restrictions.ilike("pegawai.code", searchpegawai.getValue().trim(), MatchMode.ANYWHERE),
							Restrictions.ilike("pegawai.mycode", searchpegawai.getValue().trim(),
									MatchMode.ANYWHERE)))));
		}

		if (order)
			criteria.addOrder(Order.desc("id"));

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);
		List<GajiTabahan> gajiTabahan = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(gajiTabahan);
		grid.setRowRenderer(new GajiTabahanRenderer());
		grid.setModelCheckMobile(strset);

		grid.renderAll();

	}

}
