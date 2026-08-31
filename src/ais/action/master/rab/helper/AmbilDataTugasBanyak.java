package ais.action.master.rab.helper;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import ais.ui.util.MyCheckboxConfig;
import org.zkoss.zul.Label;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.South;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Tree;
import org.zkoss.zul.Treecell;
import org.zkoss.zul.Treecol;
import org.zkoss.zul.Treecols;
import org.zkoss.zul.Treeitem;
import org.zkoss.zul.TreeitemRenderer;
import org.zkoss.zul.Treerow;

import ais.action.master.rab.util.TugasTreeModel;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.rab.Proyek;
import ais.database.model.rab.Tugas;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Tipe khusus untuk ambil data tugas banyak. Kelas ini memberi nama dan batas tanggung jawab yang
 * eksplisit pada perilaku yang diwarisi atau kontrak yang diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Tree tree}, {@code EventListener
 * eventListener}, {@code TugasTreeModel tugasTreeModel}, {@code AmbilDataProyekBanbox proyek}, {@code Integer
 * debetCredit}, {@code Boolean chooseAll}, {@code List tugass}, {@code Proyek myproyek}; pembacaan/pencarian
 * ({@code onSearchDefault()}, {@code setEventListener()}, {@code getEventListener()}); mutasi data ({@code
 * setProyek()}); operasi domain lain ({@code display()}). Bagian lain dari kontrak tetap mengikuti kelas induk
 * atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class AmbilDataTugasBanyak extends MyWindow {

	/**
	 * 
	 */
	protected static final long serialVersionUID = 6452461056684904810L;
	protected Tree tree;

	protected EventListener eventListener;
	protected TugasTreeModel tugasTreeModel;

	private AmbilDataProyekBanbox proyek;

	protected Integer debetCredit = null;

	private Boolean chooseAll = false;
	private List<Tugas> tugass;
	private Proyek myproyek;

	public AmbilDataTugasBanyak(List<Tugas> tugass, Proyek proyek)
			throws Exception {
		this(true, tugass, proyek);
	}

	public AmbilDataTugasBanyak(Boolean chooseAll, List<Tugas> tugass,
			Proyek proyek) throws Exception {
		super();
		this.tugass = tugass;
		this.chooseAll = chooseAll;
		this.myproyek = proyek;
		display();

	}

	class TugasTreeRenderer extends ais.ui.util.MyTreeitemRenderer {

		@Override
		public void render(final Treeitem treeitem, Object arg1) {
			// TODO Auto-generated method stub
			final Tugas tugas = (Tugas) arg1;

			try {
				Treerow treerow = new Treerow();
				treerow.setParent(treeitem);

				Treecell arg0 = new Treecell();
				arg0.setParent(treerow);

				new Label(tugas.toString()).setParent(arg0);

				arg0 = new Treecell();
				arg0.setParent(treerow);
				final MyCheckboxConfig checkbox = new MyCheckboxConfig();
				checkbox.setVisible(chooseAll
						|| tugasTreeModel.getChildCount(tugas) == 0);
				checkbox.setChecked(tugass.contains(tugas));
				checkbox.setParent(arg0);arg0.setAttribute("checkbox", checkbox);
				checkbox.setAttribute("tugas", tugas);

				checkbox.addEventListener("onCheck", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						if (checkbox.isChecked()) {
							if (!tugass.contains(tugas)) {
								tugass.add(tugas);
							}
							Session session = HibernateUtil.currentSession();
							Long count = (Long) session
									.createCriteria(Tugas.class)
									.setProjection(
											Projections.property("jmlDipakai"))
									.add(Restrictions.idEq(tugas.getId()))
									.uniqueResult();
							count = count == null ? 0L : count;
							tugas.setJmlDipakai(++count);
							Common.refreshUpdate(session,(tugas));

						} else {
							tugass.remove(tugas);
						}
					}
				});

			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e); 
			}

		}

	}

	public void display() throws Exception {

		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(this);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Daftar Tugas");
		panel.setBorder("none");
		panel.setStyle("border:0px;");

		Toolbar toolbar = new Toolbar();
		toolbar.setParent(panel);

		toolbar.appendChild(new Label("Program / Kegiatan"));
		toolbar.appendChild(this.proyek = new AmbilDataProyekBanbox());

		proyek.setValue(myproyek == null ? "" : myproyek.toString());
		proyek.setAttribute("proyek", myproyek);
		this.proyek.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});

		Panelchildren panelchildren = new Panelchildren();
		panelchildren.setParent(panel);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(panelchildren);

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		tree = new Tree();
		tree.setZclass("z-dottree");
		tree.setParent(center);

		Treecols columns = new Treecols();

		columns.setParent(tree);

		Treecol column = new Treecol();
		column.setParent(columns);
		column.setLabel("Nama Item");

		column = new Treecol();
		column.setParent(columns);
		column.setLabel("Pilih");
		column.setWidth("5%");

		onSearchDefault(null);

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(south);

		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				AmbilDataTugasBanyak.this.detach();
			}
		});
		cancel.setParent(toolbar);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				Event myEvent = new Event("myEvent", event.getTarget(), tugass);
				eventListener.onEvent(myEvent);
				AmbilDataTugasBanyak.this.detach();
			}
		});
		button.setParent(toolbar);

	}

	public void onSearchDefault(Event event) throws Exception {

		if (proyek.getAttribute("proyek") == null) {
			return;
		}

		final Proyek proyek = (Proyek) this.proyek.getAttribute("proyek");

		Integer revisi = (Integer) HibernateUtil.currentSession()
				.createCriteria(Tugas.class)
				.add(Restrictions.eq("proyek", proyek))
				.setProjection(Projections.max("revisi")).uniqueResult();
		revisi = revisi == null ? 1 : revisi;

		// System.out.println("revisi = " + revisi);

		tugasTreeModel = new TugasTreeModel(revisi, proyek);
		tree.setModel(tugasTreeModel);
		tree.setItemRenderer(new TugasTreeRenderer());
	}

	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	public EventListener getEventListener() {
		return eventListener;
	}

	public void setProyek(Proyek proyek) throws Exception {
		if (this.proyek.getAttribute("proyek") != null && proyek != null) {
			Proyek myKerja = (Proyek) this.proyek.getAttribute("proyek");
			if (myKerja.getId().equals(proyek.getId())) {
				return;
			}
		}

		this.proyek.setAttribute("proyek", proyek);
		this.proyek.setValue(proyek == null ? "" : proyek.toString());

		onSearchDefault(null);
	}

}
