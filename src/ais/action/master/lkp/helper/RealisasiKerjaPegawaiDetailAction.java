package ais.action.master.lkp.helper;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.poi.ss.usermodel.Hyperlink;
import org.zkoss.poi.xssf.usermodel.XSSFCell;
import org.zkoss.poi.xssf.usermodel.XSSFCellStyle;
import org.zkoss.poi.xssf.usermodel.XSSFColor;
import org.zkoss.poi.xssf.usermodel.XSSFFont;
import org.zkoss.poi.xssf.usermodel.XSSFHyperlink;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import ais.ui.util.MyDetail;
import org.zkoss.zul.East;
import org.zkoss.zul.Foot;
import org.zkoss.zul.Footer;
import org.zkoss.zul.Group;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.Window;

import ais.action.master.akunting.helper.AmbilDataPegawaiBanbox;
import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.Konfigurasi;
import ais.database.model.ParameterTambahan;
import ais.database.model.Pegawai;
import ais.database.model.file.LampiranLain;
import ais.database.model.lkp.KegiatanTugasJabatan;
import ais.database.model.lkp.KegiatanTugasJabatanPunyaIndikator;
import ais.database.model.lkp.KegiatanTugasJabatanPunyaPredecessor;
import ais.database.model.lkp.KegiatanTugasJabatanPunyaSasaran;
import ais.database.model.lkp.KelompokParameterTambahanKegiatan;
import ais.database.model.lkp.ParameterTambahanKegiatan;
import ais.database.model.lkp.RealisasiKerjaPegawai;
import ais.database.model.lkp.TargetKerjaPegawai;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyLabelKecilBold;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyTimebox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.WaktuUtil;

/**
 * Controller/action ZK untuk realisasi kerja pegawai detail. Tipe ini merupakan titik masuk UI
 * yang menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus
 * oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyDetail}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Row rowBulan}, {@code Row rowTahun},
 * {@code Map lampiranLains}, {@code TargetKerjaPegawai targetKerjaPegawai}, {@code MyGrid grid}, {@code boolean
 * edit}, {@code boolean add}, {@code boolean delete}; inisialisasi/lifecycle ({@code initDataLain()}, {@code
 * init()}, {@code initCriteria()}); pembacaan/pencarian ({@code setTargetKerjaPegawai()}, {@code
 * getChildCount()}, {@code loadData()}, {@code onSearchDefault()}); mutasi data ({@code onSave()}); operasi
 * domain lain ({@code displayRow()}, {@code onAdd()}, {@code onAddExternal()}, {@code onEditExternal()}, {@code
 * display()}); konfigurasi constructor: {@code add}, {@code delete}, {@code edit}. Bagian lain dari kontrak
 * tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 * <p><b>Lifecycle:</b> instance mengikuti lifecycle komponen ZK dan menyimpan state layar; jangan digunakan
 * sebagai singleton atau dibagikan antar desktop/session. Event handler harus tetap memakai konteks pengguna
 * serta session Hibernate milik request yang aktif.</p>
 *
 * @see MyDetail
 */
public class RealisasiKerjaPegawaiDetailAction extends MyDetail implements DataCriteria, DataSearchDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5086031585928643232L;

	private static Row rowBulan;

	private static Row rowTahun;

	private Map<String, LampiranLain> lampiranLains = new HashMap<String, LampiranLain>();
	private TargetKerjaPegawai targetKerjaPegawai;
	private MyGrid grid;

	private boolean edit = false;
	private boolean add = false;
	private boolean delete = false;

	private boolean merupakanAsesor;

	private EventListener ubahEventListener;

	public RealisasiKerjaPegawaiDetailAction(TargetKerjaPegawai targetKerjaPegawai, boolean merupakanAsesor,
			EventListener ubahEventListener) {
		super();
		this.merupakanAsesor = merupakanAsesor;
		this.ubahEventListener = ubahEventListener;
		add = CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE);
		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		this.targetKerjaPegawai = targetKerjaPegawai;
		this.addEventListener(Events.ON_OPEN, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(RealisasiKerjaPegawaiDetailAction.this);
				if (isOpen()) {
					display();
				}
			}
		});
	}

	public void setTargetKerjaPegawai(TargetKerjaPegawai targetKerjaPegawai) {
		this.targetKerjaPegawai = targetKerjaPegawai;
	}

	public static void displayRow(Row row, final RealisasiKerjaPegawai realisasiKerjaPegawai, final boolean edit,
			final boolean delete, final boolean merupakanAsesor, final EventListener ubahEventListener,
			EventListener ubahListener, EventListener hapusListener) throws Exception {
		TargetKerjaPegawai targetKerjaPegawai = realisasiKerjaPegawai.getTargetKerjaPegawai();
		final MyDetail detail = new MyDetail();
		detail.setParent(row);
		detail.addEventListener("onOpen", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(detail);
				if (detail.isOpen()) {

					ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
					groupbox.setStyle("min-height: 200px;");
					groupbox.setParent(detail);

					MyGrid grid = new MyGrid();
					grid.setWidth("100%");
					grid.setParent(groupbox);
					grid.setWidth("100%");
					grid.setHeight("100%");

					Columns columns = new Columns();
					columns.setParent(grid);

					MyColumnConfig column = new MyColumnConfig("");
					column.setParent(columns);
					column.setWidth("30%");

					column = new MyColumnConfig("");
					column.setParent(columns);

					Rows rows = new Rows();
					rows.setParent(grid);

					Session session = HibernateUtil.currentSession();
					KegiatanTugasJabatan kegiatanTugasJabatan = realisasiKerjaPegawai.getTargetKerjaPegawai()
							.getKegiatanTugasJabatan();
					session.refresh(kegiatanTugasJabatan);
					Set<KelompokParameterTambahanKegiatan> kelompokParameterTambahanKegiatans = kegiatanTugasJabatan
							.getKelompokParameterTambahanKegiatans();

					for (KelompokParameterTambahanKegiatan kelompokParameterTambahanKegiatan : kelompokParameterTambahanKegiatans) {

						Group group = new ais.ui.util.MyGroupConfig();
						group.setVisible(false);
						group.setParent(rows);
						group.appendChild(
								new ais.ui.util.MyHtml("<b>" + kelompokParameterTambahanKegiatan.getNama() + "</b>"));

						List<ParameterTambahan> parameterTambahans = session
								.createCriteria(ParameterTambahanKegiatan.class)
								.add(Restrictions.eq("kelompokParameterTambahanKegiatan",
										kelompokParameterTambahanKegiatan))
								.createAlias("parameterTambahan", "parameterTambahan")
								.createAlias("kelompokParameterTambahanKegiatan", "kelompokParameterTambahanKegiatan")
								.add(Restrictions.eq("parameterTambahan.aktif", true))
								.add(Restrictions.eq("kelompokParameterTambahanKegiatan.aktif", true))
								.setProjection(Projections.groupProperty("parameterTambahan")).list();
						Collections.sort(parameterTambahans);

						group.setVisible(!parameterTambahans.isEmpty());
						if (!parameterTambahans.isEmpty()) {
							for (final ParameterTambahan parameterTambahan : parameterTambahans) {
								final String jenis = kelompokParameterTambahanKegiatan.getId() + "->"
										+ parameterTambahan.getId();

								MyFormRow row = new MyFormRow();
								row.setValign("top");
								row.setParent(rows);
								row.appendChild(new Label(parameterTambahan.getLabelInputan()
										+ (parameterTambahan.getWajibDiisi() ? " (*)" : " ")));
								String val = "";
								String[] spl = realisasiKerjaPegawai.getParameterTambahanInds().split("\n");
								for (String d : spl) {
									String[] value = d.split("<=>");
									if (value[0].trim().equalsIgnoreCase(jenis)) {
										val = value.length > 1 ? value[1].trim() : "";
									}
								}
								row.appendChild(new Label(val));

								if (parameterTambahan.getHarusMenyertakanLampiran()) {
									MyFormRow rowUpload = new MyFormRow();
									rowUpload.setStyle("border:0px;background: transparent;");
									rowUpload.setParent(rows);
									rowUpload.appendChild(new Label());

									final Hbox hbox = new Hbox();
									hbox.setWidth("100%");
									hbox.setStyle("border:0px;background: transparent;");
									hbox.setParent(rowUpload);
									LampiranLain.createDownloadUploadFileLain(hbox, realisasiKerjaPegawai.getId(),
											jenis,
											parameterTambahan.getLabelInputan()
													+ (parameterTambahan.getLampiranWajibDiisi() ? " (*)" : " "),
											false, new EventListener() {

												@Override
												public void onEvent(Event arg0) throws Exception {

												}
											}, null, false, false, false, false);

								}

								if (!parameterTambahan.getKeterangan().trim().isEmpty()) {
									Common.initKeterangan(rows, parameterTambahan.getKeterangan().trim());
								}
							}
						}
					}

				}
			}
		});

		Pegawai pegawai = realisasiKerjaPegawai.getPegawai();
		CommonMedia.tampilkanGambarKecil(pegawai).setParent(row);
		Vbox vbox = new Vbox();
		vbox.setParent(row);
		RevisiHelper.createNewRevisi(Pegawai.class, pegawai,
				pegawai.getDosen() == null ? pegawai.getNama() : pegawai.getDosen().getNama()).setParent(vbox);
		new MyLabelKecil(pegawai.getMycode() == null ? "" : pegawai.getMycode()).setParent(vbox);
//		new MyLabelKecil(pegawai.getCode() == null ? "" : pegawai.getCode()).setParent(vbox);

		vbox = new Vbox();
		vbox.setParent(row);
		new MyLabelAgakKecil(realisasiKerjaPegawai.getKeterangan()).setParent(vbox);
		new MyLabelKecilBold(realisasiKerjaPegawai.getTargetKerjaPegawai() == null
				|| realisasiKerjaPegawai.getTargetKerjaPegawai().getKegiatanTugasJabatan() == null ? ""
						: realisasiKerjaPegawai.getTargetKerjaPegawai().getKegiatanTugasJabatan().getNama())
				.setParent(vbox);
		RevisiHelper
				.createNewRevisi(RealisasiKerjaPegawai.class, realisasiKerjaPegawai,
						Common.dateFormat3.get().format(realisasiKerjaPegawai.getTanggalWaktu())
								+ (realisasiKerjaPegawai.getTanggalWaktuSampai() == null ? ""
										: " s.d " + Common.dateFormat3.get().format(realisasiKerjaPegawai.getTanggalWaktu())))
				.setParent(vbox);
		Vbox myvbox = new Vbox();
		myvbox.setParent(vbox);

		Hbox hbox = new Hbox();
		hbox.setParent(myvbox);
		LampiranLain.createDownloadUploadFileLain(hbox, realisasiKerjaPegawai.getId(),
				RealisasiKerjaPegawai.class.getName(), "Lampiran", true, null, null, false, false, false, false);

		hbox = new Hbox();
		hbox.setParent(row);
		new Label(Common.numberFormat.get().format(realisasiKerjaPegawai.getKuantitas())).setParent(hbox);

		new Label(targetKerjaPegawai.getKegiatanTugasJabatan().getSatuanKuantitas() == null ? ""
				: targetKerjaPegawai.getKegiatanTugasJabatan().getSatuanKuantitas().getNama()).setParent(hbox);

		hbox = new Hbox();
		hbox.setParent(row);
		new Label(Common.numberFormat.get().format(realisasiKerjaPegawai.getWaktu())).setParent(hbox);
		new Label(targetKerjaPegawai.getKegiatanTugasJabatan().getSatuanWaktu()).setParent(hbox);

		new Label(Common.numberFormat.get().format(realisasiKerjaPegawai.getBiaya())).setParent(row);

		if (targetKerjaPegawai.getVerifikasi()) {
			new Label(realisasiKerjaPegawai.getVerifikasi() ? "Ya" : "Belum").setParent(row);
			new MyLabelAgakKecil(realisasiKerjaPegawai.getCatatan()).setParent(row);
			new Label().setParent(row);
		} else {

			final Hbox toolbar = new Hbox();

			if (!merupakanAsesor) {
				new Label(realisasiKerjaPegawai.getVerifikasi() ? "Ya" : "Belum").setParent(row);
				new MyLabelAgakKecil(realisasiKerjaPegawai.getCatatan()).setParent(row);
			} else {

				final MyTextbox catatan = new MyTextbox(realisasiKerjaPegawai.getCatatan());
				final MyCheckboxConfig checkbox = new MyCheckboxConfig("Sesuai");
				checkbox.setChecked(realisasiKerjaPegawai.getVerifikasi());
				checkbox.setParent(row);
				row.setValign("top");
				row.setAttribute("checkbox", checkbox);
				checkbox.addEventListener("onCheck", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						realisasiKerjaPegawai.setVerifikasi(checkbox.isChecked());
						Common.refreshSaveOrUpdate(realisasiKerjaPegawai);

						toolbar.setVisible(!realisasiKerjaPegawai.getVerifikasi());
						catatan.setDisabled(realisasiKerjaPegawai.getVerifikasi());

						if (ubahEventListener != null) {
							Common.createDefaultTimerNoBusy(ubahEventListener, "", false, 500);
						}
					}
				});

				catatan.setDisabled(realisasiKerjaPegawai.getVerifikasi());
				catatan.setWidth("90%");
				catatan.setRows(2);
				catatan.setParent(row);
				catatan.addEventListener("onChange", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						realisasiKerjaPegawai.setCatatan(catatan.getValue());
						Common.refreshSaveOrUpdate(realisasiKerjaPegawai);
					}
				});
			}

			toolbar.setVisible(!realisasiKerjaPegawai.getVerifikasi());
			if (ubahListener != null) {
				MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
				button.setVisible(edit);
				button.setTooltiptext("Ubah Data");
				button.addEventListener("onClick", ubahListener);
				button.setParent(toolbar);
			}

			if (hapusListener != null) {
				MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
				button.setVisible(delete);
				button.setTooltiptext("Hapus Data");
				button.addEventListener("onClick", hapusListener);
				button.setParent(toolbar);
			}
			ais.ui.util.MenuAksiBaris.pasang(toolbar);
			toolbar.setParent(row);
		}
	}

	class RealisasiKerjaPegawaiRenderer extends ais.ui.util.MyRowRenderer {

		public RealisasiKerjaPegawaiRenderer() {

		}

		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");
			final RealisasiKerjaPegawai realisasiKerjaPegawai = (RealisasiKerjaPegawai) data;

			RealisasiKerjaPegawaiDetailAction.displayRow(row, realisasiKerjaPegawai, edit, delete, merupakanAsesor,
					ubahEventListener, new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {
							init(realisasiKerjaPegawai);
						}

					}, new EventListener() {
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

													Common.refreshDelete(realisasiKerjaPegawai);

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
		}
	}

	private RealisasiKerjaPegawai realisasiKerjaPegawai;

	private MyDoublebox kuantitas;

	private MyDoublebox waktu;

	private MyDoublebox biaya;

	private Textbox keterangan;

	private MyDatebox tanggalWaktu;

	private ParameterTambahanKegiatanListener parameterTambahanKegiatanListener;

	private Label tanggalWaktuSampai;

	protected LampiranLain buktiPenugasan;

	private Date t;

	private MyTimebox timebox;

	public void onAdd(Event event) throws Exception {
		RealisasiKerjaPegawai realisasiKerjaPegawai = new RealisasiKerjaPegawai();
		realisasiKerjaPegawai.setTargetKerjaPegawai(targetKerjaPegawai);
		init(realisasiKerjaPegawai);
	}

	private void initDataLain(East east) throws Exception {
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(east);

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		MyGrid gridEast = new MyGrid();
		gridEast.setWidth("100%");
		gridEast.setParent(center);
		gridEast.setWidth("100%");
		gridEast.setHeight("100%");

		org.zkoss.zul.Columns columns = new org.zkoss.zul.Columns();
		columns.setParent(gridEast);
		MyColumnConfig column = new MyColumnConfig();
		column.setWidth("40%");
		columns.appendChild(column);
		column = new MyColumnConfig();
		columns.appendChild(column);

		Rows rowsEast = new Rows();
		rowsEast.setParent(gridEast);

		List<Row> parameterRows = new ArrayList<Row>();
		parameterTambahanKegiatanListener = new ParameterTambahanKegiatanListener(realisasiKerjaPegawai, parameterRows,
				lampiranLains, rowsEast);
		boolean visible = parameterTambahanKegiatanListener.check();
		// System.out.println("parameterTambahanKegiatanListener visible => " + visible);
		east.setVisible(visible);

		parameterTambahanKegiatanListener.onEvent(null);
	}

	public int getChildCount(KegiatanTugasJabatan induk) {
		Session session = HibernateUtil.currentSession();
		Integer count = ((Number) session.createCriteria(KegiatanTugasJabatan.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

				.add(Restrictions.eq("satuanKerja", induk.getSatuanKerja()))

				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

				.add(Restrictions.eq("induk", induk)).setProjection(Projections.rowCount()).uniqueResult()).intValue();

		return count;
	}

	public static void onAddExternal(final EventListener eventListener) throws Exception {

		final Window addWindow = new Window();
		addWindow.setHeight("300px");
		addWindow.setWidth("600px");
		addWindow.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		addWindow.setTitle("Pilih Kegiatan");

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

		final Combobox searchbulan = new Combobox();
		final Combobox searchtahun = new Combobox();

		int tahun = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
		for (int i = tahun - 10; i < tahun + 2; i++) {
			Comboitem comboitem = new Comboitem(i + "");
			comboitem.setValue(i);
			searchtahun.appendChild(comboitem);
		}

		Common.selectComboItem(searchtahun, tahun);
		searchtahun.setReadonly(true);

		for (int i = 0; i < 12; i++) {
			Comboitem comboitem = new Comboitem(Common.BULAN[i]);
			comboitem.setValue(i);
			searchbulan.appendChild(comboitem);
		}
		int bulan = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.MONTH);
		Common.selectComboItem(searchbulan, bulan);
		searchbulan.setReadonly(true);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pegawai *"));
		final AmbilDataPegawaiBanbox pegawai;
		row.appendChild(pegawai = new AmbilDataPegawaiBanbox(true));
		pegawai.setWidth("90%");
		pegawai.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kegiatan *"));
		final AmbilDataKegiatanTugasJabatanBanbox induk;
		row.appendChild(induk = new AmbilDataKegiatanTugasJabatanBanbox(false));
		induk.setWidth("90%");
		induk.setReadonly(true);

		EventListener targetKerjaPegawai = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Pegawai p = (Pegawai) pegawai.getAttribute("pegawai");
				if (p != null) {
					induk.setSatuanKerja(p.getSatuanKerja(), p.ambilHakAkses(), false);
				}
			}
		};

		pegawai.setEventListener(targetKerjaPegawai);
		targetKerjaPegawai.onEvent(null);

		boolean tidakBolehEdit = Common.bolehKonfigurasi("tidak_boleh_entry_kegiatan_yang_sudah_terlewat");

		rowBulan = new MyFormRow();
		rowBulan.setParent(rows);
		rowBulan.appendChild(new ais.ui.util.MyLabelConfig("Bulan *"));
		if (tidakBolehEdit) {
			rowBulan.appendChild(new Label(Common.BULAN[bulan] + ""));
		} else {
			rowBulan.appendChild(searchbulan);
		}
		searchbulan.setWidth("90%");
		searchbulan.setReadonly(true);

		rowTahun = new MyFormRow();
		rowTahun.setParent(rows);
		rowTahun.appendChild(new ais.ui.util.MyLabelConfig("Tahun *"));
		if (tidakBolehEdit) {
			rowTahun.appendChild(new Label(tahun + ""));
		} else {
			rowTahun.appendChild(searchtahun);
		}
		searchtahun.setWidth("90%");
		searchtahun.setReadonly(true);

		EventListener indukEventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				// Renderer (AmbilDataKegiatanTugasJabatanBanbox) bisa memicu listener ini dari DESKTOP
				// LAMA (komponen sudah di-render ulang di desktop lain) → setVisible pada Rows milik
				// desktop lain melempar IllegalStateException "belongs to another desktop". Abaikan bila
				// komponen sudah tidak berada di desktop yang sedang aktif.
				if (rowBulan == null || rowBulan.getDesktop() == null
					|| (org.zkoss.zk.ui.Executions.getCurrent() != null
						&& rowBulan.getDesktop() != org.zkoss.zk.ui.Executions.getCurrent().getDesktop())) {
					return;
				}

				rowBulan.setVisible(false);
				rowTahun.setVisible(false);

				KegiatanTugasJabatan kegiatanTugasJabatan = (KegiatanTugasJabatan) induk
						.getAttribute("kegiatanTugasJabatan");

				if (kegiatanTugasJabatan != null) {
					rowBulan.setVisible(kegiatanTugasJabatan.getPeriode().equals(KegiatanTugasJabatan.BULANAN));
					rowTahun.setVisible(true);
				}

			}
		};

		indukEventListener.onEvent(null);
		induk.setEventListener(indukEventListener);

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
		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Lanjut", "/img/save.gif");
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				Pegawai p = (Pegawai) pegawai.getAttribute("pegawai");
				if (p == null) {
					MyMessageboxConfig.show("Pilih salah satu pegawai", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return;
				}

				KegiatanTugasJabatan kegiatanTugasJabatan = (KegiatanTugasJabatan) induk
						.getAttribute("kegiatanTugasJabatan");

				if (kegiatanTugasJabatan == null) {
					MyMessageboxConfig.show("Pilih salah satu kegiatan", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return;
				}
				addWindow.detach();
				Session session = HibernateUtil.currentSession();

				TargetKerjaPegawai targetKerjaPegawai = ((TargetKerjaPegawai) session
						.createCriteria(TargetKerjaPegawai.class)
						.add(Restrictions.eq("tahun", searchtahun.getSelectedItem().getValue()))
						.add(searchbulan == null ? Restrictions.sqlRestriction("true")
								: Restrictions.eq("bulan", searchbulan.getSelectedItem().getValue()))
						.add(Restrictions.eq("pegawai", p))
						.add(Restrictions.eq("kegiatanTugasJabatan", kegiatanTugasJabatan)).uniqueResult());
				if (targetKerjaPegawai == null) {
					targetKerjaPegawai = new TargetKerjaPegawai();
					targetKerjaPegawai.setTahun((Integer) searchtahun.getSelectedItem().getValue());
					targetKerjaPegawai.setBulan((Integer) searchbulan.getSelectedItem().getValue());
					targetKerjaPegawai.setPegawai(p);
					targetKerjaPegawai.setKegiatanTugasJabatan(kegiatanTugasJabatan);
					session.save(targetKerjaPegawai);
					session.flush();
				}

				RealisasiKerjaPegawai realisasiKerjaPegawai = new RealisasiKerjaPegawai();
				realisasiKerjaPegawai.setTargetKerjaPegawai(targetKerjaPegawai);

				RealisasiKerjaPegawaiDetailAction realisasiKerjaPegawaiDetailAction = new RealisasiKerjaPegawaiDetailAction(
						targetKerjaPegawai, false, eventListener);
				realisasiKerjaPegawaiDetailAction.init(realisasiKerjaPegawai);

			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);
		addWindow.setVisible(true);
		addWindow.onModal();

	}

	public static void onEditExternal(EventListener eventListener, RealisasiKerjaPegawai realisasiKerjaPegawai)
			throws Exception {
		RealisasiKerjaPegawaiDetailAction realisasiKerjaPegawaiDetailAction = new RealisasiKerjaPegawaiDetailAction(
				realisasiKerjaPegawai.getTargetKerjaPegawai(), false, eventListener);
		realisasiKerjaPegawaiDetailAction.init(realisasiKerjaPegawai);
	}

	@SuppressWarnings("unchecked")
	private void init(RealisasiKerjaPegawai realisasiKerjaPegawai) throws Exception {
		final Window addWindow = new Window();
		addWindow.setHeight("95%");
		addWindow.setWidth("600px");
		addWindow.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		this.realisasiKerjaPegawai = realisasiKerjaPegawai;
		if (realisasiKerjaPegawai != null && realisasiKerjaPegawai.getTargetKerjaPegawai() != null) {
			this.targetKerjaPegawai = realisasiKerjaPegawai.getTargetKerjaPegawai();
		}
		addWindow.setTitle("Realisasi Kinerja");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

		East east = new East();
		east.setParent(borderlayout);
		east.setWidth("0%");
		initDataLain(east);

		if (east.isVisible()) {
			east.setWidth("60%");
			addWindow.setHeight("98%");
			addWindow.setWidth("98%");
		}

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
		row.appendChild(new ais.ui.util.MyLabelConfig("Kegiatan"));
		row.appendChild(new Label(targetKerjaPegawai.getKegiatanTugasJabatan().getNama()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Periode"));
		row.appendChild(new Label(targetKerjaPegawai.getKegiatanTugasJabatan().getPeriode()));

		tanggalWaktu = new MyDatebox(realisasiKerjaPegawai.getTanggalWaktu());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal / Waktu Kegiatan"));
		Hbox hbox = new Hbox();
		row.appendChild(hbox);
		timebox = null;
		if (Common.bolehKonfigurasi("tidak_boleh_entry_kegiatan_yang_sudah_terlewat")) {
			hbox.appendChild(new Label(Common.dateFormat4.get().format(realisasiKerjaPegawai.getTanggalWaktu())));
			timebox = new MyTimebox(realisasiKerjaPegawai.getTanggalWaktu());
			timebox.setCols(3);
			hbox.appendChild(timebox);
		} else {
			if (realisasiKerjaPegawai.getId() != null) {
				hbox.appendChild(new Label(Common.dateFormat51.get().format(realisasiKerjaPegawai.getTanggalWaktu())));
			} else {
				hbox.appendChild(tanggalWaktu);
			}
			tanggalWaktu.setFormat(Common.dateFormat.get().toPattern());
			tanggalWaktu.setCols(16);
			tanggalWaktu.setReadonly(true);
		}

		hbox.appendChild(new Label(ais.common.Common.getBahasaConfig(" s.d ")));
		t = realisasiKerjaPegawai.getTanggalWaktuSampai();
		hbox.appendChild(tanggalWaktuSampai = new Label(t == null ? "" : Common.dateFormat51.get().format(t)));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Lama / Waktu Pengerjaan (*)"));
		hbox = new Hbox();
		row.appendChild(hbox);
		Label satuanWaktu = new Label(targetKerjaPegawai.getKegiatanTugasJabatan() == null ? ""
				: targetKerjaPegawai.getKegiatanTugasJabatan().getSatuanWaktu());
		hbox.appendChild(waktu = new MyDoublebox(realisasiKerjaPegawai.getWaktu()));
		hbox.appendChild(satuanWaktu);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Target Waktu Pengerjaan"));
		row.appendChild(new Label(Common.numberFormat.get().format(targetKerjaPegawai.getWaktu()) + " "
				+ targetKerjaPegawai.getKegiatanTugasJabatan().getSatuanWaktu()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Catatan (*)"));
		row.appendChild(keterangan = new Textbox(realisasiKerjaPegawai.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(5);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		hbox = new Hbox();
		hbox.setParent(row);
		LampiranLain.createDownloadUploadFileLain(hbox, realisasiKerjaPegawai.getId(),
				RealisasiKerjaPegawai.class.getName(), "Lampiran", false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						buktiPenugasan = (LampiranLain) arg0.getData();
					}
				}, null, false, false, false, true);

		Common.initKeterangan(rows,
				"*) Kompres atau zip dulu jika bukti penugasan lebih dari satu file, sehingga menjadi satu file yang Anda upload");

		if (targetKerjaPegawai.getKegiatanTugasJabatan().getPeriode().equals(KegiatanTugasJabatan.BULANAN)) {
			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Tahun / Bulan"));
			row.appendChild(
					new Label(targetKerjaPegawai.getTahun() + " / " + Common.BULAN[targetKerjaPegawai.getBulan()]));
		} else {
			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Tahun"));
			row.appendChild(new Label(targetKerjaPegawai.getTahun() + ""));
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sasaran"));
		hbox = new Hbox();
		row.appendChild(hbox);
		Session session = HibernateUtil.currentSession();
		List<String> sas = session.createCriteria(KegiatanTugasJabatanPunyaSasaran.class)
				.add(Restrictions.eq("kegiatanTugasJabatan", targetKerjaPegawai.getKegiatanTugasJabatan()))
				.createAlias("sasaran", "sasaran").setProjection(Projections.property("sasaran.nama")).list();
		int i = 1;
		for (String s : sas) {
			hbox.appendChild(new MyLabelAgakKecil((i++) + ". " + s));
		}

		row.setVisible(!sas.isEmpty());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Indikator"));
		hbox = new Hbox();
		row.appendChild(hbox);

		List<String> ind = session.createCriteria(KegiatanTugasJabatanPunyaIndikator.class)
				.add(Restrictions.eq("kegiatanTugasJabatan", targetKerjaPegawai.getKegiatanTugasJabatan()))
				.createAlias("indikator", "indikator").setProjection(Projections.property("indikator.nama")).list();
		i = 1;
		for (String s : ind) {
			hbox.appendChild(new MyLabelAgakKecil((i++) + ". " + s));
		}

		row.setVisible(!ind.isEmpty());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pendahuluan / Predecessor"));
		hbox = new Hbox();
		row.appendChild(hbox);

		List<String> predecessor = session.createCriteria(KegiatanTugasJabatanPunyaPredecessor.class)
				.add(Restrictions.eq("kegiatanTugasJabatan", targetKerjaPegawai.getKegiatanTugasJabatan()))
				.createAlias("kegiatanTugasJabatanPredecessor", "kegiatanTugasJabatanPredecessor")
				.setProjection(Projections.property("kegiatanTugasJabatanPredecessor.nama")).list();
		i = 1;
		for (String s : predecessor) {
			hbox.appendChild(new MyLabelAgakKecil((i++) + ". " + s));
		}

		row.setVisible(!predecessor.isEmpty());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kuantitas (*)"));

		hbox = new Hbox();
		row.appendChild(hbox);
		Label satuanKuantitas = new Label(targetKerjaPegawai.getKegiatanTugasJabatan() == null ? ""
				: targetKerjaPegawai.getKegiatanTugasJabatan().getSatuanKuantitas().getNama());
		hbox.appendChild(kuantitas = new MyDoublebox(realisasiKerjaPegawai.getKuantitas()));
		hbox.appendChild(satuanKuantitas);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Target Kuantitas"));
		row.appendChild(new Label(Common.numberFormat.get().format(targetKerjaPegawai.getKuantitas()) + " "
				+ targetKerjaPegawai.getKegiatanTugasJabatan().getSatuanKuantitas().getNama()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Biaya"));
		row.appendChild(biaya = new MyDoublebox(realisasiKerjaPegawai.getBiaya()));

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tanggalWaktu.getValue() != null && waktu.getValue() != null) {
					Date wkt = tanggalWaktu.getValue();
					int w = waktu.getValue().intValue();
					Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
					calendar.setTime(wkt);

					if (timebox != null) {
						Calendar calendar1 = ais.ui.util.WaktuUtil.getCalendar();
						calendar1.setTime(timebox.getValue());

						calendar.set(Calendar.HOUR_OF_DAY, calendar1.get(Calendar.HOUR_OF_DAY));
						calendar.set(Calendar.MINUTE, calendar1.get(Calendar.MINUTE));
						calendar.set(Calendar.SECOND, calendar1.get(Calendar.SECOND));
						calendar.set(Calendar.MILLISECOND, calendar1.get(Calendar.MILLISECOND));

						tanggalWaktu.setValue(calendar.getTime());
					}

					if (targetKerjaPegawai.getKegiatanTugasJabatan().getSatuanWaktu().equalsIgnoreCase("Menit")) {
						calendar.set(Calendar.MINUTE, calendar.get(Calendar.MINUTE) + w);
					} else if (targetKerjaPegawai.getKegiatanTugasJabatan().getSatuanWaktu().equalsIgnoreCase("Jam")) {
						calendar.set(Calendar.HOUR_OF_DAY, calendar.get(Calendar.HOUR_OF_DAY) + w);
					} else if (targetKerjaPegawai.getKegiatanTugasJabatan().getSatuanWaktu().equalsIgnoreCase("Hari")) {
						calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + w);
					} else if (targetKerjaPegawai.getKegiatanTugasJabatan().getSatuanWaktu()
							.equalsIgnoreCase("Minggu")) {
						calendar.set(Calendar.WEEK_OF_MONTH, calendar.get(Calendar.WEEK_OF_MONTH) + w);
					} else if (targetKerjaPegawai.getKegiatanTugasJabatan().getSatuanWaktu()
							.equalsIgnoreCase("Bulan")) {
						calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) + w);
					}
					t = calendar.getTime();
					tanggalWaktuSampai.setValue(Common.dateFormat51.get().format(t));
				}
			}
		};

		if (timebox != null) {
			timebox.addEventListener("onChange", eventListener);
		} else {
			tanggalWaktu.addEventListener("onChange", eventListener);
		}
		waktu.addEventListener("onChange", eventListener);
		eventListener.onEvent(null);

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
				if (onSave(event)) {
					loadData(null);
					addWindow.detach();

					if (ubahEventListener != null) {
						ubahEventListener.onEvent(event);
					}
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	public boolean onSave(Event event) throws Exception {
		if (kuantitas.getValue() == null) {
			MyMessageboxConfig.show("Kuantitas harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (waktu.getValue() == null) {
			MyMessageboxConfig.show("Waktu harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (keterangan.getValue().trim().isEmpty()) {
			MyMessageboxConfig.show("Keterangan / Catatan harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (realisasiKerjaPegawai.getId() == null
				&& Common.bolehKonfigurasi("tidak_boleh_entry_kegiatan_yang_sudah_terlewat")) {
			Date kemarin = WaktuUtil.kemarin();
			Date d = tanggalWaktu.getValue();
			if (d.before(kemarin) || Common.dateFormat3.get().format(kemarin).equals(Common.dateFormat3.get().format(d))) {
				MyMessageboxConfig.show("Anda tidak boleh entry kegiatan yang sudah terlewat", "Peringatan",
						MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
				return false;
			}
		}

		Session session = HibernateUtil.currentSession();
		if (realisasiKerjaPegawai.getId() != null) {
			realisasiKerjaPegawai = (RealisasiKerjaPegawai) session.load(RealisasiKerjaPegawai.class,
					realisasiKerjaPegawai.getId());

		}

		parameterTambahanKegiatanListener.onSave(realisasiKerjaPegawai);
		if (!parameterTambahanKegiatanListener.validate(realisasiKerjaPegawai)) {
			return false;
		}

		realisasiKerjaPegawai.setKuantitas(kuantitas.getValue());
		realisasiKerjaPegawai.setWaktu(waktu.getValue());
		realisasiKerjaPegawai.setBiaya(biaya.getValue());
		realisasiKerjaPegawai.setKeterangan(keterangan.getValue());
		realisasiKerjaPegawai.setTargetKerjaPegawai(targetKerjaPegawai);

		Date wkt = tanggalWaktu.getValue();
		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.setTime(wkt);

		if (timebox != null) {
			Calendar calendar1 = ais.ui.util.WaktuUtil.getCalendar();
			calendar1.setTime(timebox.getValue());

			calendar.set(Calendar.HOUR_OF_DAY, calendar1.get(Calendar.HOUR_OF_DAY));
			calendar.set(Calendar.MINUTE, calendar1.get(Calendar.MINUTE));
			calendar.set(Calendar.SECOND, calendar1.get(Calendar.SECOND));
			calendar.set(Calendar.MILLISECOND, calendar1.get(Calendar.MILLISECOND));

			realisasiKerjaPegawai.setTanggalWaktu(calendar.getTime());
		} else {
			realisasiKerjaPegawai.setTanggalWaktu(wkt);
		}

		realisasiKerjaPegawai.setTanggalWaktuSampai(t);

		Common.refreshSaveOrUpdate(session, realisasiKerjaPegawai);

		try {
			session = StreamingHibernateUtil.getInstance().currentSession();

			if (buktiPenugasan != null && buktiPenugasan.getId() != null) {
				session.refresh(buktiPenugasan);
				buktiPenugasan.setRef(realisasiKerjaPegawai.getId());

				session.getTransaction().begin();
				session.update(buktiPenugasan);
				session.getTransaction().commit();
			}

			StreamingHibernateUtil.getInstance().closeSession();
		} catch (Exception e) {
			StreamingHibernateUtil.getInstance().rollbackTransaction();
			Common.tampilErrorJikaAdmin(e);
		}

		if (!lampiranLains.isEmpty()) {
			Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();
			streamingSession.getTransaction().begin();
			for (LampiranLain lampiranLain : lampiranLains.values()) {
				streamingSession.refresh(lampiranLain);
				lampiranLain.setRef(realisasiKerjaPegawai.getId());
				streamingSession.update(lampiranLain);
			}
			streamingSession.getTransaction().commit();
			StreamingHibernateUtil.getInstance().closeSession();
		}

		Common.createDefaultTimerNoBusy(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				parameterTambahanKegiatanListener.onSave(realisasiKerjaPegawai);
				Common.refreshUpdate(realisasiKerjaPegawai);

			}
		}, "", false, 1000);

		return true;
	}

	@SuppressWarnings("unchecked")
	public void loadData(Object value) {

		if (grid == null) {
			return;
		}

		Session session = HibernateUtil.currentSession();
		List<RealisasiKerjaPegawai> realisasiKerjaPegawais = session.createCriteria(RealisasiKerjaPegawai.class)
				.addOrder(Order.desc("id")).add(Restrictions.eq("targetKerjaPegawai", targetKerjaPegawai)).list();

		ListModel strset = new SimpleListModel(realisasiKerjaPegawais);
		grid.setRowRenderer(new RealisasiKerjaPegawaiRenderer());
		grid.setModelCheckMobile(strset);

		Foot foot = grid.getFoot() == null ? new Foot() : grid.getFoot();
		foot.setParent(grid);

		Common.clear(foot);

		Double kuantitas = 0.0;
		Double waktu = 0.0;
		Double biaya = 0.0;
		for (RealisasiKerjaPegawai realisasiKerjaPegawai : realisasiKerjaPegawais) {
			kuantitas += realisasiKerjaPegawai.getKuantitas();
			waktu += realisasiKerjaPegawai.getWaktu();
			biaya += realisasiKerjaPegawai.getBiaya();
		}

		foot.appendChild(new Footer());
		foot.appendChild(new Footer("Total"));
		Footer footer;

		String persen = "";
		if (targetKerjaPegawai.getKuantitas() > 0.0) {
			persen = " (" + Common.numberFormat.get().format((kuantitas * 100.0) / targetKerjaPegawai.getKuantitas()) + " %)";
		}

		foot.appendChild(footer = new Footer(Common.numberFormat.get().format(kuantitas) + " "
				+ targetKerjaPegawai.getKegiatanTugasJabatan().getSatuanKuantitas().getNama() + persen));
		footer.setStyle("font-weight:bold;font-size:14px;");

		persen = "";
		if (targetKerjaPegawai.getWaktu() > 0.0) {
			persen = " (" + Common.numberFormat.get().format((waktu * 100.0) / targetKerjaPegawai.getWaktu()) + " %)";
		}

		foot.appendChild(footer = new Footer(Common.numberFormat.get().format(waktu) + " "
				+ targetKerjaPegawai.getKegiatanTugasJabatan().getSatuanWaktu() + persen));
		footer.setStyle("font-weight:bold;font-size:14px;");

		foot.appendChild(footer = new Footer(Common.numberFormat.get().format(biaya)));
		footer.setStyle("font-weight:bold;font-size:14px;");
	}

	public void display() {

		ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
		groupbox.setStyle("min-height: 200px;");
		groupbox.setParent(this);
		groupbox.appendChild(new MyCaptionStyled("Daftar " + targetKerjaPegawai.getNama()));
		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(groupbox);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Tambah Realisasi Kegiatan", "/img/add_item.png");
		button.setDisabled(!add);
		button.setVisible(!targetKerjaPegawai.getVerifikasi());
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				onAdd(event);
			}

		});
		button.setParent(toolbar);

		List<String> columnHeadersAddingTambahan = new ArrayList<String>();
		columnHeadersAddingTambahan.add("ID");
		columnHeadersAddingTambahan.add("keterangan");
		columnHeadersAddingTambahan.add("Waktu Kegiatan");
		columnHeadersAddingTambahan.add("Sampai Waktu Kegiatan");

		String[] contents = new String[] { "id", "keterangan", "tanggalWaktu", "kuantitas", "waktu", "biaya",
				"verifikasi", "catatan" };

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakDataCustomButton(RealisasiKerjaPegawai.class, this,
				"Download Data", "/img/excel.png", null, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Object[] objects = (Object[]) arg0.getData();
						RealisasiKerjaPegawai realisasiKerjaPegawai = (RealisasiKerjaPegawai) objects[0];
						XSSFWorkbook workbook = (XSSFWorkbook) objects[3];
						XSSFRow rowTambahan = (XSSFRow) objects[4];
						XSSFRow rowheadTambahan = (XSSFRow) objects[5];
						XSSFFont hlink_font = workbook.createFont();
						hlink_font.setUnderline(XSSFFont.U_SINGLE);
						hlink_font.setColor(new XSSFColor(Color.BLUE));

						final XSSFCellStyle hlink_style = workbook.createCellStyle();
						hlink_style.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
						hlink_style.setFillForegroundColor(new XSSFColor(Color.LIGHT_GRAY));
						hlink_style.setFont(hlink_font);

						if (rowTambahan != null && !realisasiKerjaPegawai.getParameterTambahan().isEmpty()) {
							rowTambahan.createCell(0).setCellValue(realisasiKerjaPegawai.getId());
							rowTambahan.createCell(1).setCellValue(realisasiKerjaPegawai.getKeterangan());
							rowTambahan.createCell(2)
									.setCellValue(Common.dateFormat3.get().format(realisasiKerjaPegawai.getTanggalWaktu()));
							rowTambahan.createCell(3)
									.setCellValue(realisasiKerjaPegawai.getTanggalWaktuSampai() == null ? ""
											: Common.dateFormat3.get().format(realisasiKerjaPegawai.getTanggalWaktuSampai()));

							String[] splNama = realisasiKerjaPegawai.getParameterTambahan().split("\n");
							for (int j = 0; j < splNama.length; j++) {
								int indexCol = j + 4;
								String namaCol = splNama.length > j ? splNama[j] : "";

								String[] value = namaCol.split("<=>");
								String lbl = value.length > 0 ? value[0].trim() : "";
								String url = value.length > 2 ? value[2].trim() : "";
								String val = value.length > 1 ? value[1].trim() : "";

								if (rowheadTambahan != null) {
									XSSFCell hssfCell = rowheadTambahan.getCell(indexCol);
									if (hssfCell == null) {
										rowheadTambahan.createCell(indexCol).setCellValue(lbl);
									}
								}

								XSSFCell cellTambahan = rowTambahan.createCell(indexCol);
								cellTambahan.setCellValue(val);
								if (url != null && !url.trim().isEmpty()) {
									cellTambahan.setCellStyle(hlink_style);
									XSSFHyperlink link = workbook.getCreationHelper()
											.createHyperlink(Hyperlink.LINK_URL);
									link.setAddress(url);
									cellTambahan.setHyperlink(link);
								}
							}
						}
					}
				}, true, columnHeadersAddingTambahan, contents);
		toolbar.appendChild(cetakToolbarbutton);

		MyToolbarbuttonConfig upload = Common.uploadData(this, RealisasiKerjaPegawai.class, contents);
		upload.setVisible(merupakanAsesor && !targetKerjaPegawai.getVerifikasi());
		toolbar.appendChild(upload);

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		// grid.setStyle("min-height: 1000px;");
		grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(50);
		grid.getPagingChild().setMold("os");
		grid.setParent(groupbox);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("0px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("0px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("0px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Kegiatan");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Kuantitas");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Waktu");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Biaya");
		column.setWidth("10%");
		column.setAlign("right");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Verifikasi Asesor");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Catatan Asesor");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("5%");

		loadData(null);
	}

	@Override
	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		return session.createCriteria(RealisasiKerjaPegawai.class).addOrder(Order.desc("id"))
				.add(Restrictions.eq("targetKerjaPegawai", targetKerjaPegawai));
	}

	@Override
	public void onSearchDefault(Event event) {
		loadData(null);

	}

}
