package ais.action.master;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Intbox;
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
import org.zkoss.zul.Vbox;

import ais.action.master.helper.AmbilDataDosenBanbox;
import ais.action.master.helper.AmbilDataNegaraBanbox;
import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.Jenjang;
import ais.database.model.Negara;
import ais.database.model.TugasBelajarDosen;
import ais.database.model.file.LampiranLain;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Controller/action ZK untuk tugas belajar dosen. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code Textbox searchnama}, {@code Textbox nama}, {@code AmbilDataDosenBanbox
 * searchdosen1}, {@code boolean edit}, {@code boolean delete}; inisialisasi/lifecycle ({@code
 * doBeforeCompose()}, {@code doAfterCompose()}, {@code init()}, {@code initCriteria()}); pembacaan/pencarian
 * ({@code onSearchDefault()}); mutasi data ({@code onSave()}); operasi domain lain ({@code displayRow()}, {@code
 * onAdd()}, {@code onAddExternal()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang
 * disebut di atas.</p>
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
public class TugasBelajarDosenAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;

	private Textbox nama;

	private AmbilDataDosenBanbox searchdosen1;

	private boolean edit = false;
	private boolean delete = false;

	private TugasBelajarDosen tugasBelajarDosen;
	private EventListener eventListener;
	private Dosen dosen;
	private AmbilDataNegaraBanbox negara;

	protected LampiranLain lainMahasiswa;

	private AmbilDataDosenBanbox dosenPengarang1;
	private Textbox keterangan;
	private Textbox bidang;
	private Intbox tahun;
	private Combobox jenjang;

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

		if (execution.getParameter("dosen") != null) {
			dosen = (Dosen) HibernateUtil.currentSession().createCriteria(Dosen.class)
					.add(Restrictions.idEq(Long.parseLong(execution.getParameter("dosen")))).uniqueResult();
			searchdosen1.setValue(dosen.getNama());
			searchdosen1.setAttribute("myValue", dosen);
			searchdosen1.setAttribute("dosen", dosen);
			searchdosen1.setDisabled(true);
		}

		System.out.println("dosen => " + execution.getParameter("dosen") + ", " + dosen);

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		onSearchDefault(null);

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

	}

	public static void displayRow(Row arg0, final TugasBelajarDosen tugasBelajarDosen) throws Exception {

		Vbox vbox = new Vbox();
		vbox.setParent(arg0);

		RevisiHelper
				.createNewRevisi(TugasBelajarDosen.class, tugasBelajarDosen,
						tugasBelajarDosen.getDosen() == null ? "" : tugasBelajarDosen.getDosen().getNama())
				.setParent(vbox);

		Vbox myvbox = new Vbox();
		myvbox.setParent(vbox);

		Hbox hbox = new Hbox();
		hbox.setParent(myvbox);
		LampiranLain.createDownloadUploadFileLain(hbox, tugasBelajarDosen.getId(), LampiranLain.SK_TUGAS_BELAJAR,
				LampiranLain.SK_TUGAS_BELAJAR, false, null, null, false, false, false, false);

		new Label(tugasBelajarDosen.getJenjang() == null ? "" : tugasBelajarDosen.getJenjang().getNama())
				.setParent(arg0);
		new Label(tugasBelajarDosen.getBidang()).setParent(arg0);
		new Label(tugasBelajarDosen.getNama()).setParent(arg0);

		new Label(tugasBelajarDosen.getNegara() == null ? "" : tugasBelajarDosen.getNegara().getNamaNegara())
				.setParent(arg0);
		new Label(tugasBelajarDosen.getTahun() == null ? "" : tugasBelajarDosen.getTahun().toString()).setParent(arg0);
	}

	class TugasBelajarDosenRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final TugasBelajarDosen tugasBelajarDosen = (TugasBelajarDosen) arg1;

			TugasBelajarDosenAction.displayRow(arg0, tugasBelajarDosen);

			// Kolom aksi rapi: seluruh tombol dibungkus kebab popup (⋯) via UIHelper.buatBarisAksi
			final java.util.List<org.zkoss.zk.ui.Component> aksiButtons =
					new java.util.ArrayList<org.zkoss.zk.ui.Component>();

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(tugasBelajarDosen);
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			aksiButtons.add(button);

			button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
			button.setVisible(delete);
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

											Common.refreshDelete(tugasBelajarDosen);

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
			aksiButtons.add(button);

			ais.ui.util.UIHelper.buatBarisAksi(arg0, 3, aksiButtons);
		}

	}

	public void onAdd(Event event) throws Exception {
		init(new TugasBelajarDosen());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	public static void onAddExternal(Event event, EventListener eventListener, TugasBelajarDosen tugasBelajarDosen)
			throws Exception {
		TugasBelajarDosenAction tugasBelajarDosenAction = new TugasBelajarDosenAction();
		tugasBelajarDosenAction.eventListener = eventListener;
		tugasBelajarDosenAction.addWindow = new MyWindow();

		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(tugasBelajarDosenAction.addWindow);
		tugasBelajarDosenAction.addWindow.setHeight("98%");
		tugasBelajarDosenAction.addWindow.setWidth("750px");

		tugasBelajarDosenAction.init(tugasBelajarDosen);

		tugasBelajarDosenAction.addWindow.setVisible(true);
		tugasBelajarDosenAction.addWindow.onModal();
	}

	private void init(TugasBelajarDosen tugasBelajarDosen) throws Exception {
		this.tugasBelajarDosen = tugasBelajarDosen;
		addWindow.setTitle(tugasBelajarDosen.getId() == null ? "Tambah Tugas Belajar" : "Ubah Tugas Belajar");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		Borderlayout myborderlayout = new ais.ui.util.MyBorderlayout();
		myborderlayout.setParent(center);
		Center mycenter = new Center();
		mycenter.setParent(myborderlayout);
		ais.ui.util.ZkCompat.setFlex(mycenter, true);

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(mycenter);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("35%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		final MyFormRow rowPengarangDosen1 = new MyFormRow();
		rowPengarangDosen1.setStyle("border:0px;background: transparent;");
		rowPengarangDosen1.setParent(rows);
		rowPengarangDosen1.appendChild(new ais.ui.util.MyLabelConfig("Dosen *"));
		rowPengarangDosen1.appendChild(dosenPengarang1 = new AmbilDataDosenBanbox(false));
		dosenPengarang1.setValue(tugasBelajarDosen.getDosen() == null ? "" : (tugasBelajarDosen.getDosen().getNama()));

		if (tugasBelajarDosen.getDosen() != null) {
			dosenPengarang1.setAttribute("myValue", tugasBelajarDosen.getDosen());
			dosenPengarang1.setAttribute("dosen", tugasBelajarDosen.getDosen());
		} else if (dosen != null) {
			dosenPengarang1.setValue(dosen.getNama());
			dosenPengarang1.setAttribute("myValue", dosen);
			dosenPengarang1.setAttribute("dosen", dosen);
			dosenPengarang1.setDisabled(true);
		}

		dosenPengarang1.setWidth("90%");

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Perguruan Tinggi *"));
		row.appendChild(nama = new Textbox(tugasBelajarDosen.getNama() == null ? "" : tugasBelajarDosen.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Bidang Studi *"));
		row.appendChild(bidang = new Textbox(tugasBelajarDosen.getBidang()));
		bidang.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenjang *"));
		row.appendChild(jenjang = new Combobox());
		Common.insertCombo(jenjang, "nama", "keterangan", Jenjang.class,
				Restrictions.or(Restrictions.eq("aktifDipilih", true), Restrictions.isNull("aktifDipilih")));
		Common.selectComboItem(jenjang, tugasBelajarDosen.getJenjang());
		jenjang.setWidth("90%");
		jenjang.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Negara *"));
		row.appendChild(negara = new AmbilDataNegaraBanbox());
		negara.setAttribute("negara", tugasBelajarDosen.getNegara());
		negara.setValue(tugasBelajarDosen.getNegara() == null ? "" : tugasBelajarDosen.getNegara().getNamaNegara());
		negara.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun *"));
		row.appendChild(tahun = new Intbox(tugasBelajarDosen.getTahun()));
		tahun.setCols(3);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("SK Tugas Belajar"));
		Hbox hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, tugasBelajarDosen.getId(), LampiranLain.SK_TUGAS_BELAJAR,
				LampiranLain.SK_TUGAS_BELAJAR, false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						lainMahasiswa = (LampiranLain) arg0.getData();
					}
				});
		hbox.setParent(row);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(
				tugasBelajarDosen.getKeterangan() == null ? "" : tugasBelajarDosen.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);
		keterangan.setRows(4);

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

					if (eventListener != null) {
						eventListener.onEvent(new Event("", addWindow, TugasBelajarDosenAction.this.tugasBelajarDosen));
					}
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);

	}

	public boolean onSave(Event event) throws Exception {
		if (dosenPengarang1.getAttribute("dosen") == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Dosen",
					"Kolom Dosen belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Dosen.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (nama.getValue().trim().equals("")) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Perguruan Tinggi",
					"Kolom Nama Perguruan Tinggi belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Nama Perguruan Tinggi.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (bidang.getValue().trim().equals("")) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Bidang",
					"Kolom Bidang belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Bidang.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (negara.getAttribute("negara") == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Negara",
					"Kolom Negara belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Negara.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (jenjang.getSelectedItem() == null || jenjang.getSelectedItem().getValue() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Jenjang",
					"Kolom Jenjang belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Jenjang.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (bidang.getValue().trim().equals("")) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Bidang",
					"Kolom Bidang belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Bidang.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (tahun.getValue() == null || tahun.getValue() < 1) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Tahun",
					"Kolom Tahun belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Tahun.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (tugasBelajarDosen.getId() != null) {
			tugasBelajarDosen = (TugasBelajarDosen) session.load(TugasBelajarDosen.class, tugasBelajarDosen.getId());

		}

		tugasBelajarDosen.setNama(nama.getValue());
		tugasBelajarDosen.setBidang(bidang.getValue());
		tugasBelajarDosen.setTahun(tahun.getValue());
		tugasBelajarDosen.setKeterangan(keterangan.getValue());

		tugasBelajarDosen.setDosen((Dosen) dosenPengarang1.getAttribute("myValue"));

		tugasBelajarDosen.setJenjang((Jenjang) jenjang.getSelectedItem().getValue());
		tugasBelajarDosen.setNegara((Negara) negara.getAttribute("negara"));

		Common.refreshSaveOrUpdate(session, tugasBelajarDosen);

		if (lainMahasiswa != null && lainMahasiswa.getId() != null) {
			try {
				session = StreamingHibernateUtil.getInstance().currentSession();

				session.refresh(lainMahasiswa);
				lainMahasiswa.setRef(tugasBelajarDosen.getId());

				session.getTransaction().begin();
				session.update(lainMahasiswa);
				session.getTransaction().commit();

				StreamingHibernateUtil.getInstance().closeSession();
			} catch (Exception e) {
				StreamingHibernateUtil.getInstance().rollbackTransaction();
				Common.tampilErrorJikaAdmin(e);
			}

		}

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();

		Dosen dosenPemimbing = (Dosen) searchdosen1.getAttribute("myValue");

		Criterion criterion = Restrictions.eq("dosen", dosenPemimbing);

		Criteria criteria = session.createCriteria(TugasBelajarDosen.class)
				.add(dosenPemimbing == null ? Restrictions.sqlRestriction("1=1") : criterion);
		if (order)
			criteria.addOrder(Order.desc("id"));
		criteria

				.add(searchnama.getValue().trim().equals("") ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		if (searchnama == null) {
			return;
		}
		Common.initPaging(initCriteria(false), paging);

		List<TugasBelajarDosen> tugasBelajarDosen = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(tugasBelajarDosen);
		grid.setRowRenderer(new TugasBelajarDosenRenderer());
		grid.setModelCheckMobile(strset);

	}

}
