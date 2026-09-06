package ais.action.master.sirs.detail;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Caption;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Groupbox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Messagebox;
import ais.ui.util.MyMessageboxConfig;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import org.zkoss.zul.RowRenderer;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Space;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;

import ais.action.master.sirs.helper.AmbilDataPasienBanyak;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.sirs.Komunitas;
import ais.database.model.sirs.KomunitasPunyaPasien;
import ais.database.model.sirs.Pasien;
import ais.ui.util.MyTextbox;

/**
 * Controller/action ZK untuk komunitas punya pasien. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyDetail}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Komunitas komunitas}, {@code Paging
 * paging}, {@code Grid grid}, {@code boolean delete}, {@code MyTextbox kode}, {@code MyTextbox nama}, {@code
 * MyTextbox telp}, {@code MyTextbox alamat}; inisialisasi/lifecycle ({@code initCriteria()});
 * pembacaan/pencarian ({@code loadData()}); operasi domain lain ({@code display()}); konfigurasi constructor:
 * {@code delete}. Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
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
public class KomunitasPunyaPasienAction extends MyDetail {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5086031585928643232L;

	private Komunitas komunitas;
	private Paging paging;
	private Grid grid;

	private boolean delete = false;

	public KomunitasPunyaPasienAction(Komunitas komunitas) {
		super();
		this.komunitas = komunitas;
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		this.addEventListener(Events.ON_OPEN, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(KomunitasPunyaPasienAction.this);
				if (isOpen()) {
					display();
				}
			}
		});

	}

	/**
	 * Renderer lokal untuk layar/komponen {@link KomunitasPunyaPasienAction}. Kelas ini menerjemahkan satu item
	 * data menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link KomunitasPunyaPasienAction} dan dapat
	 * mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see KomunitasPunyaPasienAction
	 */
	class KomunitasPunyaPasienRenderer extends ais.ui.util.MyRowRenderer {

		public KomunitasPunyaPasienRenderer() {

		}

		@Override
		public void render(final Row arg0, Object data) throws Exception {
			final KomunitasPunyaPasien komunitasPunyaPasien = (KomunitasPunyaPasien) data;
			final Pasien pasien = komunitasPunyaPasien.getPasien();

			if (pasien.getAktif() == null || !pasien.getAktif()) {
				arg0.setStyle("background-color:red;");
			}
			new Label(pasien.getKode()).setParent(arg0);
			new Label(pasien.getNama()).setParent(arg0);
			new Label(pasien.getNoTelp()).setParent(arg0);
			new Label(pasien.getNoHp()).setParent(arg0);
			new Label(pasien.getAsuransi() == null ? "" : pasien.getAsuransi().getNama()).setParent(arg0);
			new Label(pasien.getAlamatLengkap()).setParent(arg0);

			final MyTextbox keterangan = new MyTextbox(
					komunitasPunyaPasien.getKeterangan() == null ? "" : komunitasPunyaPasien.getKeterangan());
			keterangan.setWidth("90%");
			keterangan.setParent(arg0);

			keterangan.addEventListener(Events.ON_CHANGE, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					komunitasPunyaPasien.setKeterangan(keterangan.getValue());
					Common.refreshUpdate(session, (komunitasPunyaPasien));
				}
			});

			Hbox toolbar = new Hbox();

			Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig("", "/img/delete.gif");
			button.setTooltiptext("Hapus Data");
			button.setVisible(delete);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah Bapak/Ibu yakin ingin menghapus data ini? Data yang sudah dihapus tidak dapat dikembalikan.", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = new Integer(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {
											Session session = HibernateUtil.currentSession();

											session.delete(session.merge(komunitasPunyaPasien));

											loadData(null);

										} catch (Exception e) {
											e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sirs/detail/KomunitasPunyaPasienAction.java:134");
											MyMessageboxConfig.show(Common.pesan(
													"Mohon maaf, data ini tidak dapat dihapus karena masih berelasi dengan data lainnya. Langkah yang dapat dilakukan: (1) periksa dan hapus terlebih dahulu data lain yang terkait dengan data ini; (2) pastikan tidak ada transaksi yang masih menggunakan data ini; (3) apabila kendala berlanjut, mohon hubungi administrator sistem. Rincian kesalahan: {V1}"
															, e.getMessage()));
										}

									}

								}
							});

				}

			});
			button.setParent(toolbar);
			ais.ui.util.MenuAksiBaris.pasangSelalu(toolbar);
			toolbar.setParent(arg0);

		}
	}

	private MyTextbox kode;
	private MyTextbox nama;
	private MyTextbox telp;
	private MyTextbox alamat;

	private Criteria initCriteria(boolean order) {

		Criterion criterion = Restrictions.ilike("pasien.alamat", alamat.getValue(), MatchMode.ANYWHERE);

		criterion = Restrictions.or(criterion,
				Restrictions.ilike("propinsi.nama", alamat.getValue(), MatchMode.ANYWHERE));

		criterion = Restrictions.or(criterion, Restrictions.ilike("kota.nama", alamat.getValue(), MatchMode.ANYWHERE));

		criterion = Restrictions.or(criterion,
				Restrictions.ilike("kecamatan.nama", alamat.getValue(), MatchMode.ANYWHERE));

		criterion = Restrictions.or(criterion,
				Restrictions.ilike("kelurahan.nama", alamat.getValue(), MatchMode.ANYWHERE));

		Criterion critKode = Restrictions.sqlRestriction("false");
		if (!kode.getValue().trim().equals("")) {
			critKode = Restrictions.or(critKode,
					Restrictions.ilike("pasien.kode", kode.getValue().trim(), MatchMode.ANYWHERE));
		} else {
			critKode = Restrictions.sqlRestriction("true");
		}

		Criterion critNama = Restrictions.sqlRestriction("false");
		if (!nama.getValue().trim().equals("")) {
			critNama = Restrictions.or(critNama,
					Restrictions.ilike("pasien.nama", nama.getValue().trim(), MatchMode.ANYWHERE));
		} else {
			critNama = Restrictions.sqlRestriction("true");
		}

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(KomunitasPunyaPasien.class)

				.createAlias("pasien", "pasien")

				.createAlias("pasien.propinsi", "propinsi", Criteria.LEFT_JOIN)
				.createAlias("pasien.kota", "kota", Criteria.LEFT_JOIN)
				.createAlias("pasien.kecamatan", "kecamatan", Criteria.LEFT_JOIN)
				.createAlias("pasien.kelurahan", "kelurahan", Criteria.LEFT_JOIN)

				.add((telp == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.or(Restrictions.ilike("pasien.noTelp", telp.getValue(), MatchMode.ANYWHERE),
						Restrictions.ilike("pasien.noHp", telp.getValue(), MatchMode.ANYWHERE))))

				.add(critKode).add(critNama).add(criterion)

				.add(Restrictions.eq("komunitas", komunitas));
		if (order)
			criteria.addOrder(Order.asc("pasien.nama"));

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void loadData(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<KomunitasPunyaPasien> komunitasPunyaPasiens = komunitas == null || komunitas.getId() == null
				? new ArrayList<KomunitasPunyaPasien>()
				: initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
						.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage()))
						.list();

		ListModel strset = new SimpleListModel(komunitasPunyaPasiens);
		grid.setRowRenderer(new KomunitasPunyaPasienRenderer());
		grid.setModel(strset);
		grid.renderAll();
	}

	private void display() {
		Groupbox groupbox = new Groupbox();
		groupbox.setParent(this);
		groupbox.appendChild(new Caption("Daftar Pasien"));

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("25px");
		toolbar.setParent(groupbox);
		Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig("Ambil Data Pasien", "/img/add_item.png");
		button.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		button.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {
				Session session = HibernateUtil.currentSession();

				List<Pasien> pasiens = session.createCriteria(KomunitasPunyaPasien.class)
						.setProjection(Projections.groupProperty("pasien")).add(Restrictions.eq("komunitas", komunitas))
						.list();

				AmbilDataPasienBanyak ambilDataPasienBanyak = new AmbilDataPasienBanyak(pasiens);
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(ambilDataPasienBanyak);
				ambilDataPasienBanyak.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						List<Pasien> pasiens = (List<Pasien>) arg0.getData();
						Session session = HibernateUtil.currentSession();
						for (Pasien pasien : pasiens) {
							KomunitasPunyaPasien komunitasPunyaPasien = new KomunitasPunyaPasien();
							komunitasPunyaPasien.setPasien(pasien);
							komunitasPunyaPasien.setKeterangan("");
							komunitasPunyaPasien.setKomunitas(komunitas);
							session.save(komunitasPunyaPasien);

						}

						loadData(null);
					}
				});
				ambilDataPasienBanyak.setWidth("95%");
				ambilDataPasienBanyak.setHeight("97%");
				ambilDataPasienBanyak.setVisible(true);
				ambilDataPasienBanyak.onModal();
			}

		});
		button.setParent(toolbar);

		toolbar.appendChild(new Space());
		toolbar.appendChild(new Space());
		toolbar.appendChild(new Space());
		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Kode:")));
		toolbar.appendChild(kode = new MyTextbox());
		kode.setWidth("80px");
		kode.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(arg0);
			}
		});

		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Nama:")));
		toolbar.appendChild(nama = new MyTextbox());
		nama.setWidth("80px");
		nama.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(arg0);
			}
		});

		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Telp:")));
		toolbar.appendChild(telp = new MyTextbox());
		telp.setWidth("80px");
		telp.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(arg0);
			}
		});

		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Alamat:")));
		toolbar.appendChild(alamat = new MyTextbox());
		alamat.setWidth("80px");
		alamat.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(arg0);
			}
		});

		Toolbarbutton search;
		toolbar.appendChild(search = new ais.ui.util.MyToolbarbuttonConfig("", "/img/search.gif"));
		search.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(arg0);
			}
		});

		Toolbarbutton delete;
		toolbar.appendChild(delete = new ais.ui.util.MyToolbarbuttonConfig("", "/img/delete.gif"));
		delete.setVisible(this.delete);
		delete.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				MyMessageboxConfig.show("Apakah Bapak/Ibu yakin ingin menghapus seluruh data ini? Data yang sudah dihapus tidak dapat dikembalikan.", "Pertanyaan",
						MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

							@Override
							public void onEvent(Event event) throws Exception {
								int i = new Integer(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {
									try {

										Session session = HibernateUtil.currentSession();
										session.createSQLQuery("delete from sirs.komunitas_punya_pasien where komunitas = "
												+ komunitas.getId()).executeUpdate();

										loadData(event);
									} catch (Exception e) {
										e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sirs/detail/KomunitasPunyaPasienAction.java:359");
										MyMessageboxConfig.show(Common.pesan(
												"Mohon maaf, data ini tidak dapat dihapus karena masih berelasi dengan data lainnya. Langkah yang dapat dilakukan: (1) periksa dan hapus terlebih dahulu data lain yang terkait dengan data ini; (2) pastikan tidak ada transaksi yang masih menggunakan data ini; (3) apabila kendala berlanjut, mohon hubungi administrator sistem. Rincian kesalahan: {V1}"
														, e.getMessage()));
									}

								}

							}
						});

			}
		});

		grid = new Grid();
		grid.setMold("paging");
		grid.setPageSize(25);
		grid.setParent(groupbox);

		Columns columns = new Columns();

		columns.setParent(grid);

		Column column = new Column();
		column.setParent(columns);
		column.setLabel("MR");
		column.setWidth("10%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Nama");
		column.setWidth("15%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("No. Telp.");
		column.setWidth("10%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("No. Hp");
		column.setWidth("10%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Asuransi");
		column.setWidth("10%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Alamat");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Ket.");

		column = new Column();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("8%");

		paging = new Paging();
		Common.initPaging(paging, new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(arg0);
			}
		});
		paging.setParent(groupbox);

		loadData(null);
	}

}
