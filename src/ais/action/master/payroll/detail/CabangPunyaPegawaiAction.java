package ais.action.master.payroll.detail;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Groupbox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Space;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.generic.AmbilDataPegawaiBanyak;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Pegawai;
import ais.database.model.payroll.Cabang;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Controller/action ZK untuk cabang punya pegawai. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyDetail}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Cabang cabang}, {@code Paging paging},
 * {@code MyGrid grid}, {@code MyTextbox kode}, {@code MyTextbox nama}; inisialisasi/lifecycle ({@code
 * initCriteria()}); pembacaan/pencarian ({@code loadData()}); operasi domain lain ({@code display()}). Bagian
 * lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
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
public class CabangPunyaPegawaiAction extends MyDetail {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5086031585928643232L;

	private Cabang cabang;
	private Paging paging;
	private MyGrid grid;

	public CabangPunyaPegawaiAction(Cabang cabang) {
		super();
		this.cabang = cabang;
		this.addEventListener(Events.ON_OPEN, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(CabangPunyaPegawaiAction.this);
				if (isOpen()) {
					display();
				}
			}
		});

	}

	class PegawaiRenderer extends ais.ui.util.MyRowRenderer {

		public PegawaiRenderer() {

		}

		@Override
		public void render(final Row arg0, Object data) throws Exception {
			final Pegawai pegawai = (Pegawai) data;
			CommonMedia.tampilkanGambarKecil(pegawai).setParent(arg0);
			RevisiHelper.createNewRevisi(Pegawai.class, pegawai, pegawai.getNama()).setParent(arg0);

			new Label(pegawai.getMycode() == null ? "" : pegawai.getMycode()).setParent(arg0);
			new Label(pegawai.getJabatan() == null ? "" : pegawai.getJabatan()).setParent(arg0);
			new Label(pegawai.getPangkat()).setParent(arg0);
			new Label(pegawai.getStatusPegawai() == null ? "" : pegawai.getStatusPegawai().getNama()).setParent(arg0);
			new Label(pegawai.getAlamat()).setParent(arg0);

			final MyTextbox keterangan = new MyTextbox(pegawai.getKeterangan() == null ? "" : pegawai.getKeterangan());
			keterangan.setWidth("90%");
			keterangan.setParent(arg0);

			keterangan.addEventListener(Events.ON_CHANGE, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					pegawai.setKeterangan(keterangan.getValue());
					Common.refreshUpdate(session, (pegawai));
					
				}
			});

			Hbox toolbar = new Hbox();

			Toolbarbutton button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show(
							"Apakah Bapak/Ibu yakin ingin menghapus data ini? Data yang telah dihapus tidak dapat dikembalikan. Tekan OK untuk melanjutkan penghapusan, atau Batal untuk membatalkan.",
							"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

						@Override
						public void onEvent(Event event) throws Exception {
							int i = Integer.parseInt(event.getData().toString());
							if (i == MyMessageboxConfig.OK) {
								try {
									Session session = HibernateUtil.currentSession();

									pegawai.setCabang(null);
									Common.refreshUpdate(session, (pegawai));

									loadData(null);

								} catch (Exception e) {
									Common.tampilErrorJikaAdmin(e);
									MyMessageboxConfig.show(MyMessageboxConfig.format(
											"Mohon maaf, data ini tidak dapat dihapus karena masih berelasi dengan data lainnya. Rincian teknis: {V1}. Langkah yang dapat dilakukan: (1) hapus atau lepaskan terlebih dahulu data lain yang berelasi; (2) periksa kembali keterkaitan data; (3) hubungi administrator apabila kendala masih berlanjut.",
											e.getMessage()));
								}

							}

						}
					});

				}

			});
			button.setParent(toolbar);
			toolbar.setParent(arg0);

		}
	}

	private MyTextbox kode;
	private MyTextbox nama;

	private Criteria initCriteria(boolean order) {

		Criterion critKode = Restrictions.sqlRestriction("false");
		if (!kode.getValue().trim().equals("")) {
			critKode = Restrictions.or(critKode,
					Restrictions.ilike("kode", kode.getValue().trim(), MatchMode.ANYWHERE));
		} else {
			critKode = Restrictions.sqlRestriction("true");
		}

		Criterion critNama = Restrictions.sqlRestriction("false");
		if (!nama.getValue().trim().equals("")) {
			critNama = Restrictions.or(critNama,
					Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.ANYWHERE));
		} else {
			critNama = Restrictions.sqlRestriction("true");
		}

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(Pegawai.class).add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")))

		.add(critKode).add(critNama).add(Restrictions.eq("cabang", cabang));

		if (order)
			criteria.addOrder(Order.asc("nama"));

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void loadData(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<Pegawai> pegawais = cabang == null || cabang.getId() == null ? new ArrayList<Pegawai>()
				: initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
						.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage()))
						.list();

		ListModel strset = new SimpleListModel(pegawais);
		grid.setRowRenderer(new PegawaiRenderer());
		grid.setModelCheckMobile(strset);
		grid.renderAll();
	}

	private void display() {

		Groupbox groupbox = new ais.ui.util.MyGroupboxStyled();
		groupbox.setParent(this);
		groupbox.appendChild(new MyCaptionStyled("Daftar Pegawai"));

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(groupbox);
		Toolbarbutton button = new MyToolbarbuttonConfig("Ambil Data Pegawai", "/img/add_item.png");
		button.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {
				Session session = HibernateUtil.currentSession();

				List<Pegawai> pegawais = session.createCriteria(Pegawai.class).add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif"))).add(Restrictions.eq("cabang", cabang))
						.list();

				AmbilDataPegawaiBanyak ambilDataPegawaiBanyak = new AmbilDataPegawaiBanyak(pegawais);
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(ambilDataPegawaiBanyak);
				ambilDataPegawaiBanyak.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						List<Pegawai> pegawais = (List<Pegawai>) arg0.getData();
						Session session = HibernateUtil.currentSession();
						for (Pegawai pegawai : pegawais) {
							pegawai.setCabang(cabang);
							session.update(pegawai);
						}

						loadData(null);
					}
				});
				ambilDataPegawaiBanyak.setWidth("90%");
				ambilDataPegawaiBanyak.setHeight("97%");
				ambilDataPegawaiBanyak.setVisible(true);
				ambilDataPegawaiBanyak.onModal();
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

		Toolbarbutton search;
		toolbar.appendChild(search = new MyToolbarbuttonConfig("", "/img/svg/search.svg"));
		search.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(arg0);
			}
		});

		Toolbarbutton delete;
		toolbar.appendChild(delete = new MyToolbarbuttonConfig("", "/img/svg/trash.svg"));
		delete.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				MyMessageboxConfig.show(
						"Apakah Bapak/Ibu yakin ingin menghapus seluruh data ini? Data yang telah dihapus tidak dapat dikembalikan. Tekan OK untuk melanjutkan penghapusan, atau Batal untuk membatalkan.",
						"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {

					@Override
					public void onEvent(Event event) throws Exception {
						int i = Integer.parseInt(event.getData().toString());
						if (i == MyMessageboxConfig.OK) {
							try {

								Session session = HibernateUtil.currentSession();
								session.createSQLQuery(
										"update pegawai set cabang=null where cabang = " + cabang.getId())
										.executeUpdate();

								loadData(event);
							} catch (Exception e) {
								Common.tampilErrorJikaAdmin(e);
								MyMessageboxConfig.show(MyMessageboxConfig.format(
										"Mohon maaf, data ini tidak dapat dihapus karena masih berelasi dengan data lainnya. Rincian teknis: {V1}. Langkah yang dapat dilakukan: (1) hapus atau lepaskan terlebih dahulu data lain yang berelasi; (2) periksa kembali keterkaitan data; (3) hubungi administrator apabila kendala masih berlanjut.",
										e.getMessage()));
							}

						}

					}
				});

			}
		});

		grid = new MyGrid();
		grid.setMold("paging");
		grid.setPageSize(25);
		grid.setParent(groupbox);

		Columns columns = new Columns();

		columns.setParent(grid);

		Column column = new Column();
		column.setParent(columns);
		column.setLabel("Foto");
		column.setWidth("70px");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Nama");
		column.setWidth("20%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Bagian");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Jabatan");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Pangkat");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Status");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Alamat");
		column.setWidth("15%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Ket.");
		column.setWidth("10%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("5%");

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
