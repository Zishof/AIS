package ais.action.master.rab;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTabConfig;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import ais.ui.util.MyToolbarbuttonConfig;

import ais.action.master.rab.helper.AmbilDataProyekBanbox;
import ais.action.master.rab.util.RabUtil;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.rab.Proyek;
import ais.database.model.rab.Tugas;
import ais.ui.util.MyIframe;

/**
 * Controller/action ZK untuk tugas. Tipe ini merupakan titik masuk UI yang menghubungkan event
 * layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code AmbilDataProyekBanbox proyek}, {@code
 * Tabs tabs}, {@code Tabpanels tabpanels}, {@code MyToolbarbuttonConfig addNewRevisi}, {@code boolean edit},
 * {@code boolean add}; inisialisasi/lifecycle ({@code doBeforeCompose()}, {@code doAfterCompose()}, {@code
 * init()}); pembacaan/pencarian ({@code onReloadTab()}, {@code loadTabRevisi()}); operasi domain lain ({@code
 * onCreateNewRevisi()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di
 * atas.</p>
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
public class TugasAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1851233776989964898L;

	private AmbilDataProyekBanbox proyek;

	private Tabs tabs;
	private Tabpanels tabpanels;
	private MyToolbarbuttonConfig addNewRevisi;

	private boolean edit = false;
	private boolean add = false;

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
		onReloadTab(null);

		add = CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE);
		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		// delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		if (addNewRevisi != null) { addNewRevisi.setVisible(add && edit); }
	}

	public void onCreateNewRevisi(Event event) throws Exception {

		if (proyek.getAttribute("proyek") == null) {
			return;
		}

		final Proyek proyek = (Proyek) this.proyek.getAttribute("proyek");

		session.setAttribute("proyek", proyek);

		final Integer revisi = (Integer) HibernateUtil.currentSession().createCriteria(Tugas.class)
				.add(Restrictions.eq("proyek", proyek)).setProjection(Projections.max("revisi")).uniqueResult();
		Integer newrevisi = revisi == null ? 1 : revisi;
		newrevisi += 1;

		final Integer revisiTerakhir = newrevisi;

		MyMessageboxConfig.show(
				"Apakah anda ingin membuat revisi baru untuk proyek " + proyek.getNama() + " revisi ke "
						+ revisiTerakhir + "  ?\n\n\nCatatan: Revisi ini akan disalin dari revisi ke " + revisi,
				"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
				new EventListener() {

					@Override
					public void onEvent(Event event) throws Exception {
						int i = Integer.parseInt(event.getData().toString());
						if (i == MyMessageboxConfig.OK) {

							RabUtil.createNewRevisi(revisi, revisiTerakhir, proyek, proyek, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									MyMessageboxConfig.show(
											"Pembuatan Revisi ke " + revisiTerakhir + " berhasil dilakukan. Revisi "
													+ revisiTerakhir + " merupakan hasil copy dari revisi " + revisi,
											"Pemberitahuan", 1, MyMessageboxConfig.INFORMATION, new EventListener() {

												@Override
												public void onEvent(Event arg0) throws Exception {

													// RabUtil.executeCopyPegawaiTugas(
													// revisiTerakhir,
													// proyek,
													// null);

													onReloadTab(arg0);
												}
											});

								}
							});

						}

					}
				});

	}

	public void onReloadTab(Event event) throws Exception {
		Common.clear(tabpanels);
		Common.clear(tabs);
		loadTabRevisi();
	}

	private void init() throws Exception {

		String sql = "update rab.tugas set revisi = 1 where revisi is null;";
		Session session = HibernateUtil.currentSession();
		session.createSQLQuery(sql).executeUpdate();

		this.proyek.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onReloadTab(null);
			}
		});

	}

	@SuppressWarnings("unchecked")
	private void loadTabRevisi() throws Exception {

		if (proyek.getAttribute("proyek") == null) {
			return;
		}

		Proyek proyek = (Proyek) this.proyek.getAttribute("proyek");

		session.setAttribute("proyek", proyek);

		Common.clear(tabs);
		Common.clear(tabpanels);

		Session session = HibernateUtil.currentSession();
		List<Integer> revisis = session.createCriteria(Tugas.class).addOrder(Order.asc("revisi"))
				.add(Restrictions.eq("proyek", proyek)).setProjection(Projections.groupProperty("revisi"))
				.add(Restrictions.gt("revisi", 0)).list();

		if (!revisis.isEmpty()) {
			for (Integer revisi : revisis) {
				MyTabConfig tab = new MyTabConfig("Revisi " + revisi);
				tab.setSelected(true);
				tab.setParent(tabs);

				Tabpanel tabpanel = new ais.ui.util.MyTabpanel();
				MyIframe iframe = new MyIframe("/pages/master/rab/tugas_revisi.zul?revisi=" + revisi);
				tabpanel.setParent(tabpanels);
				tabpanel.appendChild(iframe);
			}
		} else {
			MyTabConfig tab = new MyTabConfig("Revisi " + 1);
			tab.setParent(tabs);

			Tabpanel tabpanel = new ais.ui.util.MyTabpanel();
			MyIframe iframe = new MyIframe("/pages/master/rab/tugas_revisi.zul?revisi=" + 1);
			tabpanel.setParent(tabpanels);
			tabpanel.appendChild(iframe);
		}

	}
}
