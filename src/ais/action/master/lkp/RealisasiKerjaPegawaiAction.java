package ais.action.master.lkp;

import java.util.Calendar;
import java.util.List;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;

import ais.action.master.akunting.helper.AmbilDataPegawaiBanbox;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.lkp.helper.RealisasiKerjaPegawaiDetailAction;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.AsesorPegawai;
import ais.database.model.Dosen;
import ais.database.model.Pegawai;
import ais.database.model.Tbmuser;
import ais.database.model.lkp.KegiatanTugasJabatan;
import ais.database.model.lkp.RealisasiKerjaPegawai;
import ais.database.model.lkp.TargetKerjaPegawai;
import ais.database.model.rab.SatuanKerja;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Controller/action ZK untuk realisasi kerja pegawai. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Paging paging}, {@code MyGrid grid},
 * {@code Textbox searchnama}, {@code Combobox searchbulan}, {@code Combobox searchtahun}, {@code
 * AmbilDataPegawaiBanbox searchpegawai}, {@code AmbilDataSatuanKerjaBanbox searchparent}, {@code
 * MyToolbarbuttonConfig find}; inisialisasi/lifecycle ({@code doBeforeCompose()}, {@code doAfterCompose()},
 * {@code initCriteria()}); pembacaan/pencarian ({@code onSearchDefault()}); operasi domain lain ({@code
 * displayRow()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
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
public class RealisasiKerjaPegawaiAction extends GenericAutowireComposer implements DataCriteria, DataSearchDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Combobox searchbulan;
	private Combobox searchtahun;
	private AmbilDataPegawaiBanbox searchpegawai;
	private AmbilDataSatuanKerjaBanbox searchparent;

	private MyToolbarbuttonConfig find;

	private Pegawai pegawaiTerpilih;
	// private boolean edit;
	// private boolean delete;
	private Tbmuser tbmuser;

	private String periode = KegiatanTugasJabatan.BULANAN;

	public static String[] contents = new String[] { "id", "tahun", "bulan", "pegawai", "kegiatanTugasJabatan",
			"kualitasRealisasi", "verifikasi", "catatan" };

	public RealisasiKerjaPegawaiAction() {
		super();
	}

	public RealisasiKerjaPegawaiAction(String periode) {
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

		tbmuser = Common.getCurrentUser();

		searchparent.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		if (execution.getParameter("pegawai") != null) {
			pegawaiTerpilih = (Pegawai) ConstantValues.ambil(Pegawai.class.getName(),
					Long.parseLong(execution.getParameter("pegawai").trim()));
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

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, find, comp);

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

	}

	public static void displayRow(Row arg0, Tbmuser tbmuser, final TargetKerjaPegawai targetKerjaPegawai)
			throws Exception {

		boolean merupakanAsesor = ((Number) HibernateUtil.currentSession().createCriteria(AsesorPegawai.class)
				.createAlias("asesor", "asesor")
				.add(Restrictions.or(Restrictions.isNull("asesor.aktif"), Restrictions.eq("asesor.aktif", true)))
				.createAlias("asesor.asesorPenunjangKinerjaDosen", "asesorPenunjangKinerjaDosen")
				.add(Restrictions.eq("pegawai", targetKerjaPegawai.getPegawai()))
				.createAlias("asesor.tbmuser", "tbmuser")
				.add(Restrictions.or(Restrictions.eq("tbmuser.pegawai", tbmuser.ambilPegawai()),
						Restrictions.eq("asesor.tbmuser", tbmuser)))
				.add(Restrictions.eq("asesorPenunjangKinerjaDosen.aktif", true)).setProjection(Projections.rowCount())
				.uniqueResult()).intValue() > 0;

		final Hbox hboxSatuan = new Hbox();
		final Hbox hboxWaktu = new Hbox();

		final EventListener ubahEventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(hboxSatuan);
				Common.clear(hboxWaktu);
				Session session = HibernateUtil.currentSession();
				Number qty = (Number) session.createCriteria(RealisasiKerjaPegawai.class)
						.add(Restrictions.eq("targetKerjaPegawai", targetKerjaPegawai))
						.add(Restrictions.eq("verifikasi", true)).setProjection(Projections.sum("kuantitas"))
						.uniqueResult();

				new Label(Common.numberFormat.get().format(targetKerjaPegawai.getKuantitas())
						+ (qty == null ? " / 0" : " / " + Common.numberFormat.get().format(qty.doubleValue())))
						.setParent(hboxSatuan);

				new Label(targetKerjaPegawai.getKegiatanTugasJabatan().getSatuanKuantitas() == null ? ""
						: targetKerjaPegawai.getKegiatanTugasJabatan().getSatuanKuantitas().getNama())
						.setParent(hboxSatuan);

				if (qty != null && targetKerjaPegawai.getKuantitas() > 0.0) {
					new Label(" (" + Common.numberFormat.get()
							.format((qty.doubleValue() * 100.0) / targetKerjaPegawai.getKuantitas()) + " %)")
							.setParent(hboxSatuan);
				}

				qty = (Number) session.createCriteria(RealisasiKerjaPegawai.class)
						.add(Restrictions.eq("targetKerjaPegawai", targetKerjaPegawai))
						.add(Restrictions.eq("verifikasi", true)).setProjection(Projections.sum("waktu"))
						.uniqueResult();

				new Label(Common.numberFormat.get().format(targetKerjaPegawai.getWaktu())
						+ (qty == null ? " / 0" : " / " + Common.numberFormat.get().format(qty.doubleValue())))
						.setParent(hboxWaktu);

				new Label(targetKerjaPegawai.getKegiatanTugasJabatan().getSatuanWaktu()).setParent(hboxWaktu);

				if (qty != null && targetKerjaPegawai.getWaktu() > 0.0) {
					new Label(" ("
							+ Common.numberFormat.get().format((qty.doubleValue() * 100.0) / targetKerjaPegawai.getWaktu())
							+ " %)").setParent(hboxWaktu);
				}

			}
		};

		final RealisasiKerjaPegawaiDetailAction realisasiKerjaPegawaiDetailAction = new RealisasiKerjaPegawaiDetailAction(
				targetKerjaPegawai, merupakanAsesor, ubahEventListener);
		realisasiKerjaPegawaiDetailAction.setParent(arg0);

		CommonMedia.tampilkanGambarKecil(targetKerjaPegawai.getPegawai()).setParent(arg0);

		RevisiHelper.createNewRevisi(TargetKerjaPegawai.class, targetKerjaPegawai,
				targetKerjaPegawai.getPegawai().getNama()).setParent(arg0);

		new MyLabelAgakKecil(Common.numberFormat.get().format(targetKerjaPegawai.getKegiatanTugasJabatan().getNoUrut()) + ". "
				+ targetKerjaPegawai.getKegiatanTugasJabatan().getNama()).setParent(arg0);

		ais.ui.util.MenuAksiBaris.pasang(hboxSatuan);
		hboxSatuan.setParent(arg0);

		Hbox hbox = new Hbox();
		hbox.setParent(arg0);
		final Label labelKualitas = new Label();
		new Label(Common.numberFormat.get().format(targetKerjaPegawai.getKualitas())).setParent(hbox);
		if (merupakanAsesor) {
			final MyDoublebox kualitasRealisasi = new MyDoublebox(targetKerjaPegawai.getKualitasRealisasi());
			kualitasRealisasi.setCols(3);
			kualitasRealisasi.setParent(hbox);
			kualitasRealisasi.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					targetKerjaPegawai.setKualitasRealisasi(kualitasRealisasi.getValue());
					Common.refreshSaveOrUpdate(targetKerjaPegawai);

					if (targetKerjaPegawai.getKualitas() > 0.0) {
						labelKualitas.setValue(" (" + Common.numberFormat.get().format(
								(targetKerjaPegawai.getKualitasRealisasi() * 100.0) / targetKerjaPegawai.getKualitas())
								+ " %)");
					}
				}
			});
		} else {
			new Label(" / " + Common.numberFormat.get().format(targetKerjaPegawai.getKualitasRealisasi())).setParent(hbox);
		}

		if (targetKerjaPegawai.getKualitas() > 0.0) {
			labelKualitas.setValue(" ("
					+ Common.numberFormat.get().format(
							(targetKerjaPegawai.getKualitasRealisasi() * 100.0) / targetKerjaPegawai.getKualitas())
					+ " %)");
		}

		labelKualitas.setParent(hbox);

		ais.ui.util.MenuAksiBaris.pasang(hboxWaktu);
		hboxWaktu.setParent(arg0);

		if (merupakanAsesor) {

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Sesuai");
			checkbox.setChecked(targetKerjaPegawai.getVerifikasi());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					targetKerjaPegawai.setVerifikasi(checkbox.isChecked());
					Common.refreshSaveOrUpdate(targetKerjaPegawai);
					realisasiKerjaPegawaiDetailAction.setTargetKerjaPegawai(targetKerjaPegawai);

					realisasiKerjaPegawaiDetailAction.setOpen(false);

					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							realisasiKerjaPegawaiDetailAction.setOpen(true);
							Common.clear(realisasiKerjaPegawaiDetailAction);
							realisasiKerjaPegawaiDetailAction.display();

							Common.createDefaultTimer(ubahEventListener);
						}
					});

				}
			});

			final MyTextbox catatan = new MyTextbox(targetKerjaPegawai.getCatatan());
			catatan.setWidth("90%");
			catatan.setRows(2);
			catatan.setParent(arg0);
			catatan.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					targetKerjaPegawai.setCatatan(catatan.getValue());
					Common.refreshSaveOrUpdate(targetKerjaPegawai);
				}
			});
		} else {
			new Label(targetKerjaPegawai.getVerifikasi() ? "Ya" : "Belum").setParent(arg0);
			new MyLabelAgakKecil(targetKerjaPegawai.getCatatan()).setParent(arg0);
		}

		ubahEventListener.onEvent(null);
	}

	/**
	 * Renderer lokal untuk layar/komponen {@link RealisasiKerjaPegawaiAction}. Kelas ini menerjemahkan satu item
	 * data menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link RealisasiKerjaPegawaiAction} dan dapat
	 * mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see RealisasiKerjaPegawaiAction
	 */
	class TargetKerjaPegawaiRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			TargetKerjaPegawai targetKerjaPegawai = (TargetKerjaPegawai) arg1;

			RealisasiKerjaPegawaiAction.displayRow(arg0, tbmuser, targetKerjaPegawai);

		}

	}

	private SatuanKerjaTreeModel satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);

	@SuppressWarnings("unchecked")
	public Criteria initCriteria(boolean order) {

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

			if (!merupakanAsesor.isEmpty()) {
				searchparent.setAttribute("satuanKerja", null);
				searchparent.setValue("");
			}

			merupakanAsesor.add(pegawai);
		}

		SatuanKerja parent = (SatuanKerja) searchparent.getAttribute("satuanKerja");
		Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
		if (parent != null) {
			satuanKerjas.clear();
			satuanKerjas.add(parent);
			satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
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
