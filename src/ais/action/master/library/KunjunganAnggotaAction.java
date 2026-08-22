package ais.action.master.library;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

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
import ais.action.master.library.helper.AmbilDataAnggotaBanbox;
import ais.action.master.library.helper.AmbilDataPerpustakaanBanbox;
import ais.action.master.library.util.LibraryUtil;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.library.Anggota;
import ais.database.model.library.KunjunganAnggota;
import ais.database.model.library.Perpustakaan;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.action.master.helper.FilterLanjutHelper;

public class KunjunganAnggotaAction extends GenericAutowireComposer implements DataCriteria, DataSearchDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private AmbilDataPerpustakaanBanbox searchperpustakaan;
	private AmbilDataAnggotaBanbox searchanggota;

	private MyDatebox tanggal;
	private AmbilDataAnggotaBanbox anggota;
	private Textbox nama;
	private Textbox alamat;
	private AmbilDataPerpustakaanBanbox perpustakaan;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private MyToolbarbuttonConfig add;

	private Textbox kodeAnggota;
	private Row rowAnggota;
	private Row rowNama;
	private Row rowAlamat;
	private MyCheckboxConfig checkbox;

	private Label label_universitas;
	private Tbmuser tbmuser = null;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		Common.initBahasaParameter(execution.getParameter("lang"));
		// Clients.confirmClose(Common.getBahasaConfig("Apakah Anda yakin ingin keluar
		// dari aplikasi ini ?"));
		HttpServletRequest request = (HttpServletRequest) execution.getNativeRequest();

		int loginTimeout = 3000;
		request.getSession().setMaxInactiveInterval(loginTimeout * 60);

		if (label_universitas != null) {
			label_universitas.setValue(Common.getKonfigurasi("label_universitas", "Universitas").getNilai());
		}
		Common.ROOT = execution.getContextPath();
		tbmuser = Common.getCurrentUser();
		if (tbmuser != null) {
			if (add != null) {
			add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
			add.setTooltiptext("Tambah");
			}

			edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
			delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		}

		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		searchperpustakaan.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

				if (searchanggota == null) {
					searchperpustakaan.getParent().setVisible(false);
				}
			}
		});

		if (searchanggota != null) {
			searchanggota.setEventListener(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					onSearchDefault(null);
				}
			});
		}

		kodeAnggota.focus();

		String[] contents = new String[] { "id", "anggota", "kode", "nama", "alamat", "perpustakaan", "tanggal", "tgl",
				"keterangan" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(KunjunganAnggota.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, KunjunganAnggota.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);
	        FilterLanjutHelper.setup(comp);
}

	class KunjunganAnggotaRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final KunjunganAnggota kunjunganAnggota = (KunjunganAnggota) arg1;

			LibraryUtil.gambarAnggota(kunjunganAnggota.getAnggota()).setParent(arg0);

			new Label(kunjunganAnggota.getKode() + " " + kunjunganAnggota.getNama()).setParent(arg0);
			new Label(kunjunganAnggota.getAlamat()).setParent(arg0);

			new Label(kunjunganAnggota.getPerpustakaan() == null ? "" : kunjunganAnggota.getPerpustakaan().getNama())
					.setParent(arg0);

			RevisiHelper.createNewRevisi(KunjunganAnggota.class, kunjunganAnggota,
					kunjunganAnggota.getTanggal() == null ? ""
							: Common.dateFormat5.get().format(kunjunganAnggota.getTanggal()))
					.setParent(arg0);

			new Label(kunjunganAnggota.getKeterangan()).setParent(arg0);

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(kunjunganAnggota);
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

											Common.refreshDelete(kunjunganAnggota);

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
		init(new KunjunganAnggota());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(KunjunganAnggota kunjunganAnggota) throws Exception {
		addWindow.setTitle(kunjunganAnggota.getId() == null ? "Tambah Kunjungan Anggota" : "Ubah Kunjungan Anggota");
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

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Waktu/Tanggal Kunjungan"));
		row.appendChild(tanggal = new MyDatebox(kunjunganAnggota.getTanggal()));
		tanggal.setFormat(Common.dateFormat.get().toPattern());

		tanggal.setWidth("90%");
		tanggal.setDisabled(tbmuser == null);
		checkbox = new MyCheckboxConfig("Bukan Anggota Perpustakaan");

		rowAnggota = new MyFormRow();
		rowAnggota.setVisible(checkbox.isChecked());
		rowAnggota.setStyle("border:0px;background: transparent;");
		rowAnggota.setParent(rows);
		rowAnggota.appendChild(new Label(ais.common.Common.getBahasaConfig("Anggota")));
		rowAnggota.appendChild(anggota = new AmbilDataAnggotaBanbox());
		anggota.setAttribute("anggota", kunjunganAnggota.getAnggota());
		anggota.setValue(kunjunganAnggota.getAnggota() == null ? "" : kunjunganAnggota.getAnggota().toString());
		anggota.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(checkbox);
		checkbox.setChecked(kunjunganAnggota.getAnggota() == null);

		rowNama = new MyFormRow();
		rowNama.setVisible(checkbox.isChecked());
		rowNama.setStyle("border:0px;background: transparent;");
		rowNama.setParent(rows);
		rowNama.appendChild(new Label(ais.common.Common.getBahasaConfig("Nama Pengunjung")));
		rowNama.appendChild(nama = new Textbox(kunjunganAnggota.getNama()));
		nama.setWidth("90%");

		rowAlamat = new MyFormRow();
		rowAlamat.setVisible(checkbox.isChecked());
		rowAlamat.setStyle("border:0px;background: transparent;");
		rowAlamat.setParent(rows);
		rowAlamat.appendChild(new Label(ais.common.Common.getBahasaConfig("Alamat Pengunjung")));
		rowAlamat.appendChild(alamat = new Textbox(kunjunganAnggota.getAlamat()));
		alamat.setWidth("90%");
		alamat.setRows(5);

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				rowAnggota.setVisible(!checkbox.isChecked());
				rowNama.setVisible(checkbox.isChecked());
				rowAlamat.setVisible(checkbox.isChecked());
			}
		};

		checkbox.addEventListener("onClick", eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Perpustakaan"));
		row.appendChild(perpustakaan = new AmbilDataPerpustakaanBanbox());
		perpustakaan.setAttribute("perpustakaan", kunjunganAnggota.getPerpustakaan());
		perpustakaan.setValue(
				kunjunganAnggota.getPerpustakaan() == null ? "" : kunjunganAnggota.getPerpustakaan().getNama());
		perpustakaan.setWidth("90%");

		if (searchperpustakaan.getAttribute("perpustakaan") != null) {
			kunjunganAnggota.setPerpustakaan((Perpustakaan) searchperpustakaan.getAttribute("perpustakaan"));
			perpustakaan.setAttribute("perpustakaan", kunjunganAnggota.getPerpustakaan());
			perpustakaan.setValue(
					kunjunganAnggota.getPerpustakaan() == null ? "" : kunjunganAnggota.getPerpustakaan().getNama());
			perpustakaan.setDisabled(true);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(
				kunjunganAnggota.getKeterangan() == null ? "" : kunjunganAnggota.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);
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
		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Berkunjung", "/img/save.gif");
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

	public void onKodeAnggota(Event event) throws Exception {
		Perpustakaan perpustakaanTerpilih = getPerpustakaanKunjunganDariFilter();
		if (perpustakaanTerpilih == null) {
			MyMessageboxConfig.show("Perpustakaan harus dipilih", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			kodeAnggota.select();
			kodeAnggota.focus();
			return;
		}
		String kodeScan = kodeAnggota.getValue() == null ? "" : kodeAnggota.getValue().trim();
		if (kodeScan.isEmpty()) {
			kodeAnggota.select();
			kodeAnggota.focus();
			return;
		}
		Session session = HibernateUtil.currentSession();
		Anggota anggota = LibraryUtil.cariAnggotaDariIdentitas(session, kodeScan);

		if (anggota == null) {
			MyMessageboxConfig.show("Kode/NIM/NIDN/NIK anggota tidak ditemukan", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			kodeAnggota.setValue("");
			kodeAnggota.select();
			kodeAnggota.focus();
			return;
		}

		KunjunganAnggota kunjunganAnggota = (KunjunganAnggota) session.createCriteria(KunjunganAnggota.class)

				.add(Restrictions.and(Restrictions.and(

						anggota == null
								? Restrictions.and(Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.EXACT),
										Restrictions.ilike("alamat", alamat.getValue().trim(), MatchMode.EXACT))
								:

								Restrictions.eq("anggota", anggota),
						Restrictions.eq("perpustakaan", perpustakaanTerpilih)),
						Restrictions.eq("tgl", ais.ui.util.WaktuUtil.getDate())))
				.setMaxResults(1).uniqueResult();

		if (kunjunganAnggota == null) {
			kunjunganAnggota = new KunjunganAnggota();
			kunjunganAnggota.setPerpustakaan(perpustakaanTerpilih);
			kunjunganAnggota.setAnggota(anggota);
			kunjunganAnggota.setTanggal(ais.ui.util.WaktuUtil.getDate());
			kunjunganAnggota.setTgl(ais.ui.util.WaktuUtil.getDate());
			kunjunganAnggota.setKeterangan("Berkunjung via scan barcode/kartu anggota");
			session.save(kunjunganAnggota);
		}

		onSearchDefault(null);

		kodeAnggota.setValue("");
		kodeAnggota.select();
		kodeAnggota.focus();
	}

	private Perpustakaan getPerpustakaanKunjunganDariFilter() {
		Perpustakaan perpustakaanTerpilih = null;
		if (searchperpustakaan != null && searchperpustakaan.getAttribute("perpustakaan") instanceof Perpustakaan) {
			perpustakaanTerpilih = (Perpustakaan) searchperpustakaan.getAttribute("perpustakaan");
		}
		if (perpustakaanTerpilih == null) {
			perpustakaanTerpilih = Common.getCurrentPerpustakaan();
		}
		return perpustakaanTerpilih;
	}

	private Perpustakaan getPerpustakaanKunjunganDariForm() {
		Perpustakaan perpustakaanTerpilih = null;
		if (perpustakaan != null && perpustakaan.getAttribute("perpustakaan") instanceof Perpustakaan) {
			perpustakaanTerpilih = (Perpustakaan) perpustakaan.getAttribute("perpustakaan");
		}
		if (perpustakaanTerpilih == null) {
			perpustakaanTerpilih = getPerpustakaanKunjunganDariFilter();
		}
		return perpustakaanTerpilih;
	}

	public boolean onSave(Event event) throws Exception {
		if (tanggal.getValue() == null) {
			MyMessageboxConfig.show("Tanggal Kunjungan harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Perpustakaan perpustakaanTerpilih = getPerpustakaanKunjunganDariForm();
		if (perpustakaanTerpilih == null) {
			MyMessageboxConfig.show("Perpustakaan harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Anggota anggota = (Anggota) this.anggota.getAttribute("anggota");

		if (!checkbox.isChecked()) {
			if (anggota == null) {
				MyMessageboxConfig.show("Kode anggota tidak ditemukan", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.INFORMATION);
				kodeAnggota.setValue("");
				kodeAnggota.select();
				kodeAnggota.focus();
				return false;
			}
		} else {
			if (nama.getValue().trim().isEmpty()) {
				MyMessageboxConfig.show("Nama pengunjung harus diisi", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.INFORMATION);
				nama.focus();
				return false;
			}
			if (alamat.getValue().trim().isEmpty()) {
				MyMessageboxConfig.show("Alamat pengunjung harus diisi", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.INFORMATION);
				nama.focus();
				return false;
			}
		}

		Session session = HibernateUtil.currentSession();
		KunjunganAnggota kunjunganAnggota = (KunjunganAnggota) session.createCriteria(KunjunganAnggota.class)

				.add(Restrictions.and(Restrictions.and(

						anggota == null
								? Restrictions.and(Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.EXACT),
										Restrictions.ilike("alamat", alamat.getValue().trim(), MatchMode.EXACT))
								:

								Restrictions.eq("anggota", anggota),
						Restrictions.eq("perpustakaan", perpustakaanTerpilih)),
						Restrictions.eq("tgl", ais.ui.util.WaktuUtil.getDate())))
				.setMaxResults(1).uniqueResult();

		if (kunjunganAnggota == null) {
			kunjunganAnggota = new KunjunganAnggota();
			kunjunganAnggota.setPerpustakaan(perpustakaanTerpilih);
			kunjunganAnggota.setAnggota(anggota);
			kunjunganAnggota.setAlamat(alamat.getValue());
			kunjunganAnggota.setNama(nama.getValue());
			kunjunganAnggota.setTanggal(tanggal.getValue());
			kunjunganAnggota.setTgl(tanggal.getValue());
			kunjunganAnggota.setKeterangan(keterangan.getValue());
			session.save(kunjunganAnggota);
		} else {
			kunjunganAnggota.setPerpustakaan(perpustakaanTerpilih);
			kunjunganAnggota.setAnggota(anggota);
			kunjunganAnggota.setAlamat(alamat.getValue());
			kunjunganAnggota.setNama(nama.getValue());
			kunjunganAnggota.setTanggal(tanggal.getValue());
			kunjunganAnggota.setTgl(tanggal.getValue());
			kunjunganAnggota.setKeterangan(keterangan.getValue());
			Common.refreshUpdate(session, kunjunganAnggota);
		}

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(KunjunganAnggota.class);
		if (order)
			criteria.addOrder(Order.desc("id"));
		criteria.add((searchperpustakaan == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchperpustakaan.getAttribute("perpustakaan") == null ? Restrictions.sqlRestriction("true")
				: Restrictions.eq("perpustakaan", searchperpustakaan.getAttribute("perpustakaan"))))
				.add(searchanggota == null || searchanggota.getAttribute("anggota") == null
						? Restrictions.sqlRestriction("true")
						: Restrictions.eq("anggota", searchanggota.getAttribute("anggota")));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<KunjunganAnggota> kunjunganAnggota = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(kunjunganAnggota);
		grid.setRowRenderer(new KunjunganAnggotaRenderer());
		grid.setModelCheckMobile(strset);

	}

}
