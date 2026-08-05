package ais.action.master.rab;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;

import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.rab.SumberDana;
import ais.database.model.rab.Workspace;
import ais.ui.util.MyIframe;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTabConfig;

/**
 * Halaman <b>Anggaran/RAB - Timeline & Timeplot</b>.
 *
 * <p>Setelah pengguna memilih Tahun Anggaran, Satuan Kerja, dan Sumber Dana, halaman menampilkan
 * anggaran per Revisi dalam tab, dilengkapi tab visual <b>Timeline</b> dan <b>Timeplot</b> yang
 * memetakan rencana dan penyerapan anggaran sepanjang waktu (lewat {@link TimelineAnggaranAction}),
 * sehingga progres pelaksanaan kegiatan mudah dipantau secara visual tanpa membaca angka satu per satu.</p>
 *
 * <h3>Higiene session basis data</h3>
 * Kelas ini hanya membaca lewat session ThreadLocal ({@code currentSession()}) yang ditutup OTOMATIS
 * oleh kerangka kerja; TIDAK ada {@code openSession()}/{@code currentNativeSession()} yang perlu
 * ditutup manual, sehingga tidak ada risiko koneksi menggantung dari kelas ini.
 *
 * <h3>Catatan teknis</h3>
 * Kompatibel Java 1.7 dan ZK 5.5 (penanganan {@code try/catch} gaya Java 1.6). Visual memakai
 * {@link TimelineAnggaranAction} (HTML/CSS, bukan JFreeChart) dan dimuat lewat timer singkat agar
 * kerangka halaman tampil lebih dahulu (ringan di perangkat mobile maupun desktop).
 */
public class WorkspaceProjectAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1851233776989964898L;

	private Combobox tahunWorkspace;
	private AmbilDataSatuanKerjaBanbox satuanKerja;
	private Combobox sumberDana;

	private Tabs tabs;
	private Tabpanels tabpanels;

	private Tabpanel laporanTimeline;
	private Tabpanel laporanTimeplot;

	public void onTimeline(Event event) {

		if (laporanTimeline.getChildren().size() == 0) {
			TimelineAnggaranAction laporanDaftarHadirDosen = new TimelineAnggaranAction();
			laporanDaftarHadirDosen.setHeight("100%");
			laporanDaftarHadirDosen.setWidth("100%");
			laporanDaftarHadirDosen.setParent(laporanTimeline);
		}
	}

	public void onTimeplot(Event event) {

		if (laporanTimeplot.getChildren().size() == 0) {
			TimelineAnggaranAction laporanDaftarHadirDosen = new TimelineAnggaranAction();
			laporanDaftarHadirDosen.setHeight("100%");
			laporanDaftarHadirDosen.setWidth("100%");
			laporanDaftarHadirDosen.setParent(laporanTimeplot);
		}
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

		init();

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onReloadTab(null);
			}
		});

	}

	public void onReloadTab(Event event) throws Exception {
		sumberDana.setSelectedItem(null);
		SatuanKerja mySatuanKerja = (SatuanKerja) satuanKerja.getAttribute("satuanKerja");
		Integer thn = (Integer) (tahunWorkspace.getSelectedItem() == null ? Calendar.getInstance().get(Calendar.YEAR)
				: tahunWorkspace.getSelectedItem().getValue());

		Common.insertComboDanSemua(sumberDana, new String[] { "kode", "nama" }, "satuanKerja", SumberDana.class,
				"== Pilih Sumber Dana ==",
				Restrictions.and(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
						Restrictions.and(Restrictions.eq("tahun", thn), Restrictions.or(
								Restrictions.isNull("satuanKerja"), Restrictions.eq("satuanKerja", mySatuanKerja)))));

		if (sumberDana.getChildren().size() == 2) {
			sumberDana.setSelectedIndex(0);
		}

		Common.clear(tabpanels);
		Common.clear(tabs);
		loadTabRevisi();
	}

	private void init() throws Exception {
		Integer tahun = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
		List<Integer> tahuns = new ArrayList<Integer>();
		for (int i = tahun + 5; i > (tahun - 20); i--) {
			tahuns.add(i);
		}
		Common.insertComboItems(tahunWorkspace, "", tahuns);
		Common.selectComboItem(tahunWorkspace, tahun);

		this.satuanKerja.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onReloadTab(null);
			}
		});

		sumberDana.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				loadTabRevisi();
			}
		});
	}

	@SuppressWarnings("unchecked")
	private void loadTabRevisi() throws Exception {

		if (tahunWorkspace.getSelectedItem() == null) {
			MyMessageboxConfig.show("Tahun Anggaran harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return;
		}
		if (satuanKerja.getAttribute("satuanKerja") == null) {
			return;
		}
		if (sumberDana.getSelectedItem() == null || sumberDana.getSelectedItem().getValue() == null) {
			return;
		}

		session.setAttribute("selectedTahun", tahunWorkspace.getSelectedItem().getValue());
		SatuanKerja satuanKerja = (SatuanKerja) this.satuanKerja.getAttribute("satuanKerja");
		SumberDana sumberDana = (SumberDana) this.sumberDana.getSelectedItem().getValue();
		Integer selectedTahun = (Integer) tahunWorkspace.getSelectedItem().getValue();

		session.setAttribute("satuanKerja", satuanKerja);
		session.setAttribute("sumberDana", sumberDana);

		Common.clear(tabs);
		Common.clear(tabpanels);

		Session session = HibernateUtil.currentSession();
		List<Integer> revisis = session.createCriteria(Workspace.class)
				.add(Restrictions.or(Restrictions.eq("carryOver", true),
						Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))))
				.addOrder(Order.asc("revisi")).add(Restrictions.eq("satuanKerja", satuanKerja))
				.add(sumberDana == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("sumberDana", sumberDana))
				.setProjection(Projections.groupProperty("revisi")).add(Restrictions.gt("revisi", 0))
				.add(Restrictions.eq("tahunWorkspace", tahunWorkspace.getSelectedItem().getValue())).list();

		if (!revisis.isEmpty()) {
			tabs.setHeight("40px");
			for (Integer revisi : revisis) {
				MyTabConfig tab = new MyTabConfig(revisi < 0 ? "" : "Revisi " + revisi);
				tab.setSelected(true);
				tab.setParent(tabs);

				Tabpanel tabpanel = new ais.ui.util.MyTabpanel();
				MyIframe iframe = new MyIframe("/pages/master/rab/workspace_project_revisi.zul?revisi=" + revisi
						+ "&satuanKerja=" + satuanKerja.getId() + "&sumberDana=" + sumberDana.getId()
						+ "&selectedTahun=" + selectedTahun);
				tabpanel.setParent(tabpanels);
				tabpanel.appendChild(iframe);
			}
		} else {
			MyTabConfig tab = new MyTabConfig("Revisi " + 1);
			tab.setParent(tabs);
			tabs.setHeight("0px");
			Tabpanel tabpanel = new ais.ui.util.MyTabpanel();
			MyIframe iframe = new MyIframe(
					"/pages/master/rab/workspace_project_revisi.zul?revisi=" + 1 + "&satuanKerja=" + satuanKerja.getId()
							+ "&sumberDana=" + sumberDana.getId() + "&selectedTahun=" + selectedTahun);
			tabpanel.setParent(tabpanels);
			tabpanel.appendChild(iframe);
		}

	}
}
