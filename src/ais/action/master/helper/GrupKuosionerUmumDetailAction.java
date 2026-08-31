package ais.action.master.helper;
import ais.common.PesanFormalHelper;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Columns;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Toolbar;

import ais.action.master.helper.generic.AmbilDataTbmuserBanyak;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GrupKuesionerUmum;
import ais.database.model.GrupKuosionerUmumDetail;
import ais.database.model.Tbmuser;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Controller/action ZK untuk grup kuosioner umum detail. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyDetail}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code GrupKuesionerUmum grupKuesionerUmum},
 * {@code MyGrid grid}, {@code boolean edit}, {@code boolean add}, {@code boolean delete}, {@code
 * MyCheckboxConfig hanyaYgAktif}; pembacaan/pencarian ({@code loadData()}, {@code onSearchDefault()}); operasi
 * domain lain ({@code display()}); konfigurasi constructor: {@code add}, {@code delete}, {@code edit}. Bagian
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
public class GrupKuosionerUmumDetailAction extends MyDetail implements DataSearchDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5086031585928643232L;

	private GrupKuesionerUmum grupKuesionerUmum;
	private MyGrid grid;

	private boolean edit = false;
	private boolean add = false;
	private boolean delete = false;

	private MyCheckboxConfig hanyaYgAktif;

	public GrupKuosionerUmumDetailAction(GrupKuesionerUmum grupKuesionerUmum) {
		super();
		add = CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE);
		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		this.grupKuesionerUmum = grupKuesionerUmum;
		this.addEventListener(Events.ON_OPEN, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(GrupKuosionerUmumDetailAction.this);
				if (isOpen()) {
					display();
				}
			}
		});
	}

	/**
	 * Renderer lokal untuk layar/komponen {@link GrupKuosionerUmumDetailAction}. Kelas ini menerjemahkan satu item
	 * data menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link GrupKuosionerUmumDetailAction} dan dapat
	 * mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see GrupKuosionerUmumDetailAction
	 */
	class GrupKuosionerUmumDetailRenderer extends ais.ui.util.MyRowRenderer {

		public GrupKuosionerUmumDetailRenderer() {

		}

		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");
			final GrupKuosionerUmumDetail grupKuosionerUmumDetail = (GrupKuosionerUmumDetail) data;

			CommonMedia.tampilkanGambarKecil(grupKuosionerUmumDetail.getTbmuser()).setParent(row);

			new Label(grupKuosionerUmumDetail.getTbmuser() == null ? ""
					: grupKuosionerUmumDetail.getTbmuser().getUserId()).setParent(row);

			RevisiHelper.createNewRevisi(GrupKuosionerUmumDetail.class, grupKuosionerUmumDetail,
					grupKuosionerUmumDetail.getTbmuser() == null ? ""
							: grupKuosionerUmumDetail.getTbmuser().getUserNama())
					.setParent(row);

			final MyTextbox keterangan = new MyTextbox(
					grupKuosionerUmumDetail.getKeterangan() == null ? "" : grupKuosionerUmumDetail.getKeterangan());
			keterangan.setWidth("90%");
			keterangan.setHeight("95%");
			keterangan.setDisabled(!edit);
			keterangan.setParent(row);
			keterangan.addEventListener(Events.ON_CHANGE, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					grupKuosionerUmumDetail.setKeterangan(keterangan.getValue());
					Common.refreshUpdate(session, (grupKuosionerUmumDetail));
				}
			});

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(grupKuosionerUmumDetail.getAktif());
			checkbox.setParent(row);
			row.setValign("top");row.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					grupKuosionerUmumDetail.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(grupKuosionerUmumDetail);
				}
			});

			Hbox toolbar = new Hbox();

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setVisible(delete);
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

											Common.refreshDelete(grupKuosionerUmumDetail);

											loadData(null);

										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											PesanFormalHelper.tampilkanGagalException("Menghapus data", "Data yang Bapak/Ibu coba hapus kemungkinan besar masih memiliki keterkaitan/relasi dengan data lain pada tabel terkait (misalnya digunakan sebagai referensi oleh transaksi, detail, atau riwayat lain), sehingga sistem basis data menolak proses penghapusan ini demi menjaga integritas data secara keseluruhan.", e, new String[]{"Periksa kembali apakah data ini masih digunakan atau direferensikan oleh data lain yang berelasi.", "Hapus atau lepaskan terlebih dahulu keterkaitan/relasi data tersebut sebelum mencoba menghapus data ini kembali.", "Jika Bapak/Ibu yakin data ini seharusnya sudah tidak digunakan lagi, hubungi Administrator untuk pengecekan lebih lanjut."});
										}

									}

								}
							});

				}

			});
			button.setParent(toolbar);
			toolbar.setParent(row);

		}
	}

	@SuppressWarnings("unchecked")
	public void loadData(Object value) {
		Session session = HibernateUtil.currentSession();
		List<GrupKuosionerUmumDetail> grupKuosionerUmumDetails = session.createCriteria(GrupKuosionerUmumDetail.class)
				.add(!hanyaYgAktif.isChecked() ? Restrictions.sqlRestriction("true")
						: Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.addOrder(Order.desc("id")).add(Restrictions.eq("grupKuesionerUmum", grupKuesionerUmum)).list();

		ListModel strset = new SimpleListModel(grupKuosionerUmumDetails);
		grid.setRowRenderer(new GrupKuosionerUmumDetailRenderer());
		grid.setModelCheckMobile(strset);

	}

	public void display() {

		ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
		groupbox.setStyle("min-height: 200px;");
		groupbox.setParent(this);
		groupbox.appendChild(new MyCaptionStyled("Daftar " + grupKuesionerUmum.getNama()));
		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(groupbox);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Ambil Pengguna", "/img/add_item.png");
		button.setDisabled(!add);
		button.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {
				Session session = HibernateUtil.currentSession();

				List<Tbmuser> tbmusers = session.createCriteria(GrupKuosionerUmumDetail.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.setProjection(Projections.groupProperty("tbmuser")).add(Restrictions.isNotNull("tbmuser"))
						.add(Restrictions.eq("grupKuesionerUmum", grupKuesionerUmum)).list();

				AmbilDataTbmuserBanyak ambilDataTbmuserBanyak = new AmbilDataTbmuserBanyak(tbmusers);
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(ambilDataTbmuserBanyak);
				ambilDataTbmuserBanyak.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						List<Tbmuser> tbmusers = (List<Tbmuser>) arg0.getData();

						for (Tbmuser tbmuser : tbmusers) {
							GrupKuosionerUmumDetail grupKuosionerUmumDetail = new GrupKuosionerUmumDetail();
							grupKuosionerUmumDetail.setTbmuser(tbmuser);
							grupKuosionerUmumDetail.setKeterangan("");
							grupKuosionerUmumDetail.setGrupKuesionerUmum(grupKuesionerUmum);
							Common.refreshSaveOrUpdate(grupKuosionerUmumDetail);
						}

						loadData(null);
					}
				});
				ambilDataTbmuserBanyak.setWidth("850px");
				ambilDataTbmuserBanyak.setHeight("97%");
				ambilDataTbmuserBanyak.setVisible(true);
				ambilDataTbmuserBanyak.onModal();
			}

		});
		button.setParent(toolbar);

		hanyaYgAktif = new MyCheckboxConfig("Hanya yg aktif");
		hanyaYgAktif.setChecked(true);
		hanyaYgAktif.setParent(toolbar);
		hanyaYgAktif.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});

		String[] contents = new String[] { "id", "grupKuesionerUmum", "tbmuser", "aktif", "keterangan" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(new DataCriteria() {

			@Override
			public Criteria initCriteria(boolean order) {

				return HibernateUtil.currentSession().createCriteria(GrupKuosionerUmumDetail.class)
						.createAlias("tbmuser", "tbmuser")
						.add(!hanyaYgAktif.isChecked() ? Restrictions.sqlRestriction("true")
								: Restrictions.or(Restrictions.isNull("grupKuosionerUmumDetail.aktif"),
										Restrictions.eq("grupKuosionerUmumDetail.aktif", true)))
						.add(Restrictions.eq("grupKuesionerUmum", grupKuesionerUmum))
						.addOrder(Order.asc("tbmuser.userNama"));
			}
		}, contents);
		toolbar.appendChild(cetakToolbarbutton);

		MyToolbarbuttonConfig upload = Common.uploadData(this, GrupKuosionerUmumDetail.class, contents);
		upload.setVisible(edit && delete);
		toolbar.appendChild(upload);

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(50);
		grid.getPagingChild().setMold("os");
		grid.setParent(groupbox);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Foto");
		column.setWidth("70px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Id Pengguna");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama Pengguna");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Keterangan");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Aktif");
		column.setWidth("5%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("5%");

		loadData(null);
	}

	@Override
	public void onSearchDefault(Event event) {
		loadData(null);
	}

}
