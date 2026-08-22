package ais.action.master;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyDetail;
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

import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.database.dao.DaoFactory;
import ais.database.dao.FormatNilaiDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.FormatNilai;
import ais.database.model.StatusPertemuan;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class FormatNilaiAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	// private Textbox nama;
	private Textbox keterangan;

	// private Label formatNilai;
	// private Combobox matakuliahKonversi;
	private MyDoublebox persen;
	private Combobox statusPertemuan;
	// private AmbilDataDosenBanbox dosenPengampu;
	// private Combobox parent;

	private boolean edit = false;
	private boolean delete = false;
	private FormatNilai parent;
	private FormatNilai formatNilai;
	private MyToolbarbuttonConfig add;

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
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		if (session.getAttribute("formatNilai") != null) {
			parent = (FormatNilai) session.getAttribute("formatNilai");
			session.removeAttribute("formatNilai");
		}

		if (parent == null) {
			return;
		}

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

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

	class FormatNilaiRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {arg0.setValign("top");
			// TODO Auto-generated method stub
			final FormatNilai formatNilai = (FormatNilai) arg1;

			final MyDetail detail = new MyDetail();
			detail.setParent(arg0);
			detail.setAttribute("formatNilai", formatNilai);
			// detail.addEventListener("onOpen", new EventListener() {
			// @Override
			// public void onEvent(Event event) throws Exception {
			// Common.clear(detail);
			//
			// if (detail.isOpen()) {
			// eventListener.onEvent(null);
			// DetailperkuliahanForPenilaianPerFormatNilaiHelper
			// detailformatNilaiHelper = new
			// DetailperkuliahanForPenilaianPerFormatNilaiHelper(
			// null, formatNilai);
			// detailformatNilaiHelper.display(
			// parent.getPerkuliahan(), detail, addWindow,
			// konfigurasi, eventListener);
			// }
			//
			// }
			//
			// });

			RevisiHelper.createNewRevisi(FormatNilai.class, formatNilai, formatNilai.getNama()).setParent(arg0);
			new Label(formatNilai.getPerkuliahan().getMatakuliah().getNama()).setParent(arg0);

			new Label(formatNilai.getPerkuliahan().infoSimple()).setParent(arg0);

			// new
			// Label(formatNilai.getDosenPengampu().getNama()).setParent(arg0);

			new Label(formatNilai.getPersen() + "").setParent(arg0);

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(formatNilai);
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			button.setParent(toolbar);

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

											Common.refreshDelete(formatNilai);

											onSearchDefault(event);
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											MyMessageboxConfig
													.show("Data ini tidak dapat dihapus .., karena berelasi dengan data lainnya, error-nya adalah sbagai berikut:"
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
		init(new FormatNilai());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(FormatNilai formatNilai) {
		this.formatNilai = formatNilai;
		addWindow.setTitle(formatNilai.getId() == null ? "Tambah FormatNilai" : "Ubah FormatNilai");
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
		column.setWidth("35%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Matakuliah"));
		row.appendChild(new ais.ui.util.MyLabelConfig(parent.getPerkuliahan().getMatakuliah().getNama()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status Pertemuan"));
		row.appendChild(statusPertemuan = new Combobox());
		Common.insertCombo(statusPertemuan, "nama", StatusPertemuan.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.selectComboItem(statusPertemuan,
				formatNilai.getStatusPertemuan() == null ? null : formatNilai.getStatusPertemuan());
		statusPertemuan.setWidth("90%");

		// row = new MyFormRow();
		//		// row.setParent(rows);
		// row.appendChild(new ais.ui.util.MyLabelConfig("Dosen pengampu"));
		// row.appendChild(dosenPengampu = new AmbilDataDosenBanbox());
		// dosenPengampu.setAttribute("dosen", formatNilai.getDosenPengampu() ==
		// null ? parent.getPerkuliahan().getDosen1()
		// : formatNilai.getDosenPengampu());
		// dosenPengampu
		// .setValue(formatNilai.getDosenPengampu() == null
		// ? parent.getPerkuliahan() == null ||
		// parent.getPerkuliahan().getDosen1() == null ? ""
		// : parent.getPerkuliahan().getDosen1().getNama()
		// : formatNilai.getDosenPengampu().getNama());
		// dosenPengampu.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Persen"));
		row.appendChild(persen = new MyDoublebox(formatNilai.getPersen()));
		persen.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(
				keterangan = new Textbox(formatNilai.getKeterangan() == null ? "" : formatNilai.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

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

		if (statusPertemuan.getSelectedItem() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Status pertemuan",
					"Kolom Status pertemuan belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Status pertemuan.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		// if (dosenPengampu.getAttribute("dosen") == null) {
		// MyMessageboxConfig.show("Dosen pengampu harus diisi", "Peringatan",
		// MyMessageboxConfig.OK,
		// MyMessageboxConfig.INFORMATION);
		// return false;
		// }

		if (persen.getValue() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Persen",
					"Kolom Persen belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Persen.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		FormatNilaiDao FormatNilaiDao = DaoFactory.getInstance().getFormatNilaiDao();
		if (formatNilai.getId() != null) {
			formatNilai = FormatNilaiDao.load(formatNilai.getId());

		}

		// formatNilai.setNama(nama.getValue());
		formatNilai.setKeterangan(keterangan.getValue());
		formatNilai.setPerkuliahan(parent.getPerkuliahan());
		// formatNilai.setDosenPengampu((Dosen)
		// dosenPengampu.getAttribute("dosen"));
		// formatNilai.setMatakuliahKonversi(parent.getMatakuliahKonversi());
		// formatNilai.setParent(parent);
		formatNilai.setPersen(persen.getValue());

		StatusPertemuan spDipilih = (StatusPertemuan) statusPertemuan.getSelectedItem().getValue();
		// ── GUARD ANTI-DUPLIKAT KOMPONEN ─────────────────────────────────────────────
		// Nilai mahasiswa disimpan memakai KUNCI StatusPertemuan.getId() (lihat
		// Detailperkuliahan.populateDetailNilai/retreiveDetailNilai). Bila DUA komponen format
		// pada satu perkuliahan memakai Status Pertemuan yang SAMA, nilainya akan saling MENIMPA
		// (mis. "Praktikum" menimpa "Responsi") sehingga seolah tidak tersimpan dan total bobot
		// tidak mencapai 100%. Karena itu Status Pertemuan WAJIB UNIK per format perkuliahan.
		if (spDipilih != null && spDipilih.getId() != null && parent != null
				&& parent.getPerkuliahan() != null) {
			try {
				Session sessCek = HibernateUtil.currentSession();
				Criteria cek = sessCek.createCriteria(FormatNilai.class)
						.add(org.hibernate.criterion.Restrictions.eq("perkuliahan", parent.getPerkuliahan()))
						.add(org.hibernate.criterion.Restrictions.eq("statusPertemuan", spDipilih));
				if (formatNilai.getId() != null) {
					cek.add(org.hibernate.criterion.Restrictions.ne("id", formatNilai.getId()));
				}
				Long jml = (Long) cek.setProjection(org.hibernate.criterion.Projections.rowCount()).uniqueResult();
				if (jml != null && jml.longValue() > 0) {
					MyMessageboxConfig.show("Komponen penilaian \"" + spDipilih.getNama()
							+ "\" sudah dipakai pada format perkuliahan ini. Setiap komponen (Status Pertemuan) "
							+ "hanya boleh dipakai SEKALI, karena nilai mahasiswa disimpan berdasarkan komponen "
							+ "tersebut. Silakan pilih Status Pertemuan yang BERBEDA untuk komponen ini.",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
					return false;
				}
			} catch (Exception eCek) {
				ais.common.Common.tampilErrorJikaAdmin(eCek);
			}
		}

		formatNilai.setStatusPertemuan(spDipilih);
		if (formatNilai.getId() != null) {
			FormatNilaiDao.update(formatNilai);
		} else {
			FormatNilaiDao.save(formatNilai);
		}

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(FormatNilai.class);

		if (order)
			criteria.addOrder(Order.desc("id"));
		criteria.createAlias("statusPertemuan", "statusPertemuan")
				.add(Restrictions.ilike("statusPertemuan.nama", searchnama.getValue(), MatchMode.ANYWHERE))
				.add(Restrictions.eq("parent", parent));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<FormatNilai> FormatNilai = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(FormatNilai);
		grid.setRowRenderer(new FormatNilaiRenderer());
		grid.setModelCheckMobile(strset);

	}

}
