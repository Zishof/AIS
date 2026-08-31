package ais.action.master.lkp;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
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

import ais.action.master.akunting.helper.AmbilDataPegawaiBanbox;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.lkp.helper.AmbilDataKegiatanTugasJabatanTreeBanbox;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.AsesorPegawai;
import ais.database.model.Dosen;
import ais.database.model.Pegawai;
import ais.database.model.Tbmrole;
import ais.database.model.lkp.KegiatanTugasJabatan;
import ais.database.model.lkp.TargetKerjaPegawai;
import ais.database.model.rab.SatuanKerja;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Controller/action ZK untuk target kerja pegawai. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code Textbox searchnama}, {@code Combobox searchbulan}, {@code Combobox
 * searchtahun}, {@code AmbilDataPegawaiBanbox searchpegawai}, {@code AmbilDataSatuanKerjaBanbox searchparent};
 * inisialisasi/lifecycle ({@code doBeforeCompose()}, {@code doAfterCompose()}, {@code init()}, {@code
 * initCriteria()}); pembacaan/pencarian ({@code onSearchDefault()}); mutasi data ({@code onSave()}); operasi
 * domain lain ({@code displayRow()}, {@code onAdd()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau
 * interface yang disebut di atas.</p>
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
public class TargetKerjaPegawaiAction extends GenericAutowireComposer implements DataCriteria, DataSearchDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Combobox searchbulan;
	private Combobox searchtahun;
	private AmbilDataPegawaiBanbox searchpegawai;
	private AmbilDataSatuanKerjaBanbox searchparent;

	private Textbox keterangan;

	private TargetKerjaPegawai targetKerjaPegawai;

	private AmbilDataPegawaiBanbox pegawai;

	private MyToolbarbuttonConfig add;

	private Pegawai pegawaiTerpilih;
	private boolean edit;
	private boolean delete;
	private AmbilDataKegiatanTugasJabatanTreeBanbox kegiatanTugasJabatan;
	private MyDoublebox kuantitas;
	private MyDoublebox kualitas;
	private Label satuanKuantitas;
	private Label satuanWaktu;
	private MyDoublebox waktu;
	private MyDoublebox biaya;

	private String periode = KegiatanTugasJabatan.BULANAN;

	public TargetKerjaPegawaiAction() {
		super();
	}

	public TargetKerjaPegawaiAction(String periode) {
		super();
		this.periode = periode;
	}

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
				onSearchDefault(null);
			}
		});

		if (execution.getParameter("pegawai") != null) {
			pegawaiTerpilih = (Pegawai) HibernateUtil.currentSession().createCriteria(Pegawai.class)
					.add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")))
					.add(Restrictions.idEq(Long.parseLong(execution.getParameter("pegawai").trim()))).uniqueResult();
		}

		if (pegawaiTerpilih != null) {
			searchpegawai.setAttribute("pegawai", pegawaiTerpilih);
			searchpegawai.setValue(pegawaiTerpilih.getNama());
			searchpegawai.setDisabled(true);
		}

		searchpegawai.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		if (searchbulan != null) {
			for (int i = 0; i < 12; i++) {
				Comboitem comboitem = new Comboitem(Common.BULAN[i]);
				comboitem.setValue(i);
				searchbulan.appendChild(comboitem);
			}

			Common.selectComboItem(searchbulan, ais.ui.util.WaktuUtil.getCalendar().get(Calendar.MONTH));
			searchbulan.setReadonly(true);
		}

		int tahun = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
		for (int i = tahun - 10; i < tahun + 2; i++) {
			Comboitem comboitem = new Comboitem(i + "");
			comboitem.setValue(i);
			searchtahun.appendChild(comboitem);
		}

		Common.selectComboItem(searchtahun, tahun);
		if (searchtahun != null) { searchtahun.setReadonly(true); }

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

		String[] contents = new String[] { "id", "tahun", "bulan", "pegawai", "kegiatanTugasJabatan", "kuantitas",
				"kualitas", "waktu", "biaya", "keterangan" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, TargetKerjaPegawai.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);

		MyToolbarbuttonConfig kegiatanDefault = new MyToolbarbuttonConfig("Masukkan Semua Kegiatan Wajib",
				"/img/options.png");
		Common.appendKeToolbar(kegiatanDefault, add, comp);
		kegiatanDefault.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Pegawai pil = (Pegawai) searchpegawai.getAttribute("pegawai");

						if (pil != null && pil.getSatuanKerja() == null) {
							MyMessageboxConfig.show(
									"Pegawai \"" + pil.getNama() + "\" harus memiliki satuan atau unit kerja",
									"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
							return;
						}

						List<Pegawai> pegawais = new ArrayList<Pegawai>();
						if (pil != null) {
							pegawais.add(pil);
						} else {
							Session session = HibernateUtil.currentSession();
							pegawais = ConstantValues.simpleList(
									session.createCriteria(Pegawai.class)
											.add(Restrictions.or(Restrictions.eq("aktif", true),
													Restrictions.isNull("aktif")))
											.add(Restrictions.isNotNull("satuanKerja"))
											.add(Restrictions.eq("statusPegawai", ConstantValues.AKTIF_PEGAWAI)),
									Pegawai.class);
						}

						for (Pegawai p : pegawais) {
							SatuanKerja satuanKerja = p.getSatuanKerja();
							List<Tbmrole> tbmroles = p.ambilHakAkses();

							Criterion criterion = satuanKerja == null ? Restrictions.sqlRestriction("false")
									: Restrictions.eq("satuanKerja", satuanKerja);

							Session session = HibernateUtil.currentSession();
							List<KegiatanTugasJabatan> kegiatanTugasJabatans = ConstantValues.simpleList(
									session.createCriteria(KegiatanTugasJabatan.class)
											.add(Restrictions.or(Restrictions.isNull("aktif"),
													Restrictions.eq("aktif", true)))
											.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).add(Restrictions.eq("wajib", true))

											.add(periode.equals(KegiatanTugasJabatan.BULANAN)
													? Restrictions.or(Restrictions.isNull("periode"),
															Restrictions.eq("periode", periode))
													: Restrictions.eq("periode", periode))

											.add(Restrictions.or(criterion,
													tbmroles == null || tbmroles.isEmpty()
															? Restrictions.sqlRestriction("false")
															: Restrictions.or(
																	Restrictions.and(criterion,
																			Restrictions.isNull("userRole")),
																	Restrictions.in("userRole", tbmroles)))),
									KegiatanTugasJabatan.class);
							for (KegiatanTugasJabatan kegiatanTugasJabatan : kegiatanTugasJabatans) {
								int count = ((Number) session.createCriteria(TargetKerjaPegawai.class)
										.setProjection(Projections.rowCount())
										.add(Restrictions.eq("tahun", searchtahun.getSelectedItem().getValue()))
										.add(searchbulan == null ? Restrictions.sqlRestriction("false")
												: Restrictions.eq("bulan", searchbulan.getSelectedItem().getValue()))
										.add(Restrictions.eq("pegawai", p))
										.add(Restrictions.eq("kegiatanTugasJabatan", kegiatanTugasJabatan))
										.uniqueResult()).intValue();
								if (count == 0) {
									TargetKerjaPegawai targetKerjaPegawai = new TargetKerjaPegawai();
									targetKerjaPegawai.setTahun((Integer) searchtahun.getSelectedItem().getValue());
									if (searchbulan != null) {
										targetKerjaPegawai.setBulan((Integer) searchbulan.getSelectedItem().getValue());
									}
									targetKerjaPegawai.setPegawai(p);
									targetKerjaPegawai.setKegiatanTugasJabatan(kegiatanTugasJabatan);

									Common.refreshSaveOrUpdate(session, targetKerjaPegawai);
								}
							}
						}

						onSearchDefault(arg0);
					}
				});
			}
		});

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});
	}

	public static void displayRow(Row arg0, final TargetKerjaPegawai targetKerjaPegawai) throws Exception {

		CommonMedia.tampilkanGambarKecil(targetKerjaPegawai.getPegawai()).setParent(arg0);

		RevisiHelper.createNewRevisi(TargetKerjaPegawai.class, targetKerjaPegawai,
				targetKerjaPegawai.getPegawai().getNama()).setParent(arg0);

		new MyLabelAgakKecil(Common.numberFormat.get().format(targetKerjaPegawai.getKegiatanTugasJabatan().getNoUrut()) + ". "
				+ targetKerjaPegawai.getKegiatanTugasJabatan().getNama()).setParent(arg0);
		Hbox hbox = new Hbox();
		hbox.setParent(arg0);
		new Label(Common.numberFormat.get().format(targetKerjaPegawai.getKuantitas())).setParent(hbox);

		new Label(targetKerjaPegawai.getKegiatanTugasJabatan().getSatuanKuantitas() == null ? ""
				: targetKerjaPegawai.getKegiatanTugasJabatan().getSatuanKuantitas().getNama()).setParent(hbox);

		new Label(Common.numberFormat.get().format(targetKerjaPegawai.getKualitas())).setParent(arg0);

		hbox = new Hbox();
		hbox.setParent(arg0);
		new Label(Common.numberFormat.get().format(targetKerjaPegawai.getWaktu())).setParent(hbox);

		new Label(targetKerjaPegawai.getKegiatanTugasJabatan().getSatuanWaktu()).setParent(hbox);

		new MyLabelAgakKecil(targetKerjaPegawai.getKeterangan()).setParent(arg0);
	}

	/**
	 * Renderer lokal untuk layar/komponen {@link TargetKerjaPegawaiAction}. Kelas ini menerjemahkan satu item data
	 * menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link TargetKerjaPegawaiAction} dan dapat mengakses
	 * state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see TargetKerjaPegawaiAction
	 */
	class TargetKerjaPegawaiRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final TargetKerjaPegawai targetKerjaPegawai = (TargetKerjaPegawai) arg1;

			TargetKerjaPegawaiAction.displayRow(arg0, targetKerjaPegawai);

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(targetKerjaPegawai);
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			button.setParent(toolbar);

			button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setVisible(!targetKerjaPegawai.getKegiatanTugasJabatan().getWajib());
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
											Common.refreshDelete(targetKerjaPegawai);
											onSearchDefault(event);
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
			ais.ui.util.MenuAksiBaris.pasang(toolbar);
			toolbar.setParent(arg0);
		}

	}

	public void onAdd(Event event) throws Exception {
		init(new TargetKerjaPegawai());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(final TargetKerjaPegawai targetKerjaPegawai) throws Exception {
		this.targetKerjaPegawai = targetKerjaPegawai;
		addWindow.setTitle(targetKerjaPegawai.getId() == null ? "Tambah Target Kerja Pegawai" : "Ubah Target Kerja Pegawai");
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

		final Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pegawai (*)"));
		row.appendChild(pegawai = new AmbilDataPegawaiBanbox(true));
		pegawai.setAttribute("myValue", targetKerjaPegawai.getPegawai());
		pegawai.setAttribute("pegawai", targetKerjaPegawai.getPegawai());
		pegawai.setValue(targetKerjaPegawai.getPegawai() == null ? "" : targetKerjaPegawai.getPegawai().getNama());
		pegawai.setWidth("90%");
		pegawai.setReadonly(true);

		if (pegawaiTerpilih != null) {
			pegawai.setAttribute("myValue", pegawaiTerpilih);
			pegawai.setAttribute("pegawai", pegawaiTerpilih);
			pegawai.setValue(pegawaiTerpilih.getNama());
			pegawai.setDisabled(true);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kegiatan / Jobdesk (*)"));
		row.appendChild(kegiatanTugasJabatan = new AmbilDataKegiatanTugasJabatanTreeBanbox(periode));
		kegiatanTugasJabatan.setWidth("90%");
		kegiatanTugasJabatan.setReadonly(true);
		kegiatanTugasJabatan.setAttribute("kegiatanTugasJabatan", targetKerjaPegawai.getKegiatanTugasJabatan());
		kegiatanTugasJabatan.setValue(targetKerjaPegawai.getKegiatanTugasJabatan() == null ? ""
				: targetKerjaPegawai.getKegiatanTugasJabatan().getNama());

		final EventListener myeventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Pegawai p = (Pegawai) pegawai.getAttribute("pegawai");
				Common.freeze(rows, p == null);
				pegawai.setDisabled(false);
			}
		};

		pegawai.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Pegawai p = (Pegawai) pegawai.getAttribute("pegawai");
				if (p != null && p.getSatuanKerja() == null) {
					pegawai.setAttribute("myValue", null);
					pegawai.setAttribute("pegawai", null);
					pegawai.setValue("");

					MyMessageboxConfig.show("Pegawai \"" + p.getNama() + "\" harus memiliki satuan atau unit kerja",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
				} else if (p != null) {
					kegiatanTugasJabatan.setSatuanKerja(p.getSatuanKerja(), p.ambilHakAkses());
				}
				myeventListener.onEvent(null);
			}
		});

		if (searchpegawai.getAttribute("pegawai") != null) {
			Pegawai pilih = (Pegawai) searchpegawai.getAttribute("pegawai");
			if (pilih != null && pilih.getSatuanKerja() == null) {
				MyMessageboxConfig.show("Pegawai \"" + pilih.getNama() + "\"  harus memiliki satuan atau unit kerja",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
				addWindow.setVisible(false);
				return;
			}

			pegawai.setAttribute("myValue", pilih);
			pegawai.setAttribute("pegawai", pilih);
			pegawai.setValue(pilih.getNama());
			pegawai.setDisabled(true);

			kegiatanTugasJabatan.setSatuanKerja(pilih.getSatuanKerja(), pilih.ambilHakAkses());
		}

		if (targetKerjaPegawai.getId() != null && targetKerjaPegawai.getKegiatanTugasJabatan() != null
				&& targetKerjaPegawai.getKegiatanTugasJabatan().getWajib()) {
			pegawai.setDisabled(true);
			kegiatanTugasJabatan.setDisabled(true);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kuantitas (*)"));
		Hbox hbox = new Hbox();
		row.appendChild(hbox);
		satuanKuantitas = new Label(targetKerjaPegawai.getKegiatanTugasJabatan() == null ? ""
				: targetKerjaPegawai.getKegiatanTugasJabatan().getSatuanKuantitas().getNama());
		hbox.appendChild(kuantitas = new MyDoublebox(targetKerjaPegawai.getKuantitas()));
		hbox.appendChild(satuanKuantitas);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kualitas (*)"));
		row.appendChild(kualitas = new MyDoublebox(targetKerjaPegawai.getKualitas()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Waktu (*)"));
		hbox = new Hbox();
		row.appendChild(hbox);
		satuanWaktu = new Label(targetKerjaPegawai.getKegiatanTugasJabatan() == null ? ""
				: targetKerjaPegawai.getKegiatanTugasJabatan().getSatuanWaktu());
		hbox.appendChild(waktu = new MyDoublebox(targetKerjaPegawai.getWaktu()));
		hbox.appendChild(satuanWaktu);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Biaya"));
		row.appendChild(biaya = new MyDoublebox(targetKerjaPegawai.getBiaya()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(
				targetKerjaPegawai.getKeterangan() == null ? "" : targetKerjaPegawai.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				KegiatanTugasJabatan myKegiatanTugasJabatan = (KegiatanTugasJabatan) kegiatanTugasJabatan
						.getAttribute("kegiatanTugasJabatan");
				kuantitas.setValue(myKegiatanTugasJabatan == null ? targetKerjaPegawai.getKuantitas()
						: myKegiatanTugasJabatan.getKuantitasDefault());
				satuanKuantitas
						.setValue(
								myKegiatanTugasJabatan == null
										? targetKerjaPegawai.getKegiatanTugasJabatan() == null ? ""
												: targetKerjaPegawai.getKegiatanTugasJabatan().getSatuanKuantitas()
														.getNama()
										: myKegiatanTugasJabatan.getSatuanKuantitas().getNama());
				kualitas.setValue(myKegiatanTugasJabatan == null ? targetKerjaPegawai.getKualitas()
						: myKegiatanTugasJabatan.getKualitasDefault());
				waktu.setValue(myKegiatanTugasJabatan == null ? targetKerjaPegawai.getWaktu()
						: myKegiatanTugasJabatan.getWaktuDefault());

				satuanWaktu.setValue(myKegiatanTugasJabatan == null
						? targetKerjaPegawai.getKegiatanTugasJabatan() == null ? ""
								: targetKerjaPegawai.getKegiatanTugasJabatan().getSatuanWaktu()
						: myKegiatanTugasJabatan.getSatuanWaktu());
				biaya.setValue(myKegiatanTugasJabatan == null ? targetKerjaPegawai.getBiaya()
						: myKegiatanTugasJabatan.getBiayaDefault());
			}
		};

		kegiatanTugasJabatan.setEventListener(eventListener);
		myeventListener.onEvent(null);

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

	}

	public boolean onSave(Event event) throws Exception {
		if (pegawai.getAttribute("pegawai") == null) {
			MyMessageboxConfig.show("Pegawai harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (kegiatanTugasJabatan.getAttribute("kegiatanTugasJabatan") == null) {
			MyMessageboxConfig.show("Kegiatan / Jobdesk harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();

		Pegawai p = (Pegawai) pegawai.getAttribute("pegawai");
		KegiatanTugasJabatan k = (KegiatanTugasJabatan) kegiatanTugasJabatan.getAttribute("kegiatanTugasJabatan");

		int count = ((Number) session.createCriteria(TargetKerjaPegawai.class).setProjection(Projections.rowCount())
				.add(Restrictions.eq("tahun", searchtahun.getSelectedItem().getValue()))
				.add(searchbulan == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("bulan", searchbulan.getSelectedItem().getValue()))
				.add(Restrictions.eq("pegawai", p)).add(Restrictions.eq("kegiatanTugasJabatan", k))
				.add(targetKerjaPegawai.getId() == null ? Restrictions.sqlRestriction("true")
						: Restrictions.ne("id", targetKerjaPegawai.getId()))
				.uniqueResult()).intValue();
		if (count > 0) {
			Integer bln = (Integer) (searchbulan == null ? null : searchbulan.getSelectedItem().getValue());
			MyMessageboxConfig.show(
					"Target kegiatan \"" + k.getNama() + "\" untuk pegawai \"" + p.getNama() + "\" tahun "
							+ searchtahun.getSelectedItem().getValue()
							+ (bln == null ? "" : " bulan " + Common.BULAN[bln]) + " sudah ada di database",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (targetKerjaPegawai.getId() != null) {
			targetKerjaPegawai = (TargetKerjaPegawai) session.load(TargetKerjaPegawai.class,
					targetKerjaPegawai.getId());

		}

		targetKerjaPegawai.setTahun((Integer) searchtahun.getSelectedItem().getValue());
		if (searchbulan != null) {
			targetKerjaPegawai.setBulan((Integer) searchbulan.getSelectedItem().getValue());
		}
		targetKerjaPegawai.setBiaya(biaya.getValue());
		targetKerjaPegawai.setKualitas(kualitas.getValue());
		targetKerjaPegawai.setKuantitas(kuantitas.getValue());
		targetKerjaPegawai.setWaktu(waktu.getValue());
		targetKerjaPegawai.setKeterangan(keterangan.getValue());
		targetKerjaPegawai.setPegawai((Pegawai) pegawai.getAttribute("pegawai"));
		targetKerjaPegawai.setKegiatanTugasJabatan(
				(KegiatanTugasJabatan) kegiatanTugasJabatan.getAttribute("kegiatanTugasJabatan"));

		Common.refreshSaveOrUpdate(session, targetKerjaPegawai);

		return true;
	}

	private SatuanKerjaTreeModel satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);

	@SuppressWarnings("unchecked")
	public Criteria initCriteria(boolean order) {

		SatuanKerja parent = (SatuanKerja) searchparent.getAttribute("satuanKerja");
		Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
		if (parent != null) {
			satuanKerjas.clear(); satuanKerjas.add(parent);
			satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
		}

		Session session = HibernateUtil.currentSession();

		List<Pegawai> merupakanAsesor = null;

		if (searchpegawai.getAttribute("pegawai") != null) {
			Pegawai pegawai = (Pegawai) searchpegawai.getAttribute("pegawai");
			Dosen dosen = pegawai.getDosen();
			merupakanAsesor = session.createCriteria(AsesorPegawai.class)
					.setProjection(Projections.groupProperty("pegawai")).createAlias("asesor", "asesor")
					.add(Restrictions.or(Restrictions.isNull("asesor.aktif"), Restrictions.eq("asesor.aktif", true)))
					.createAlias("asesor.asesorPenunjangKinerjaDosen", "asesorPenunjangKinerjaDosen")
					.createAlias("asesor.tbmuser", "tbmuser")
					.add(Restrictions.or(Restrictions.eq("tbmuser.pegawai", pegawai),
							Restrictions.eq("tbmuser.dosen", dosen)))
					.add(Restrictions.eq("asesorPenunjangKinerjaDosen.aktif", true)).list();

			merupakanAsesor.add(pegawai);
		}

		Criteria criteria = session.createCriteria(TargetKerjaPegawai.class)
				.createAlias("kegiatanTugasJabatan", "kegiatanTugasJabatan")

				.add(periode.equals(KegiatanTugasJabatan.BULANAN)
						? Restrictions.or(Restrictions.isNull("kegiatanTugasJabatan.periode"),
								Restrictions.eq("kegiatanTugasJabatan.periode", periode))
						: Restrictions.eq("kegiatanTugasJabatan.periode", periode))

				.add(satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
						: Restrictions.in("kegiatanTugasJabatan.satuanKerja", satuanKerjas));

		if (order)
			criteria.addOrder(Order.asc("kegiatanTugasJabatan.noUrut")).addOrder(Order.desc("id"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("kegiatanTugasJabatan.nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))
				.add(Restrictions.eq("tahun", searchtahun.getSelectedItem().getValue()))
				.add(searchbulan == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("bulan", searchbulan.getSelectedItem().getValue()))
				.add(merupakanAsesor == null ? Restrictions.sqlRestriction("true")
						: Restrictions.in("pegawai", merupakanAsesor));

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<TargetKerjaPegawai> targetKerjaPegawai = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(targetKerjaPegawai);
		grid.setRowRenderer(new TargetKerjaPegawaiRenderer());
		grid.setModelCheckMobile(strset);

	}

}
