package ais.action.master.sirs;

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
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Window;

import ais.action.master.akunting.helper.AmbilDataAkunKreditBanbox;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.sirs.detail.PajakDetailAction;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.akunting.Akun;
import ais.database.model.akunting.Pajak;
import ais.database.model.sirs.Asuransi;
import ais.database.model.sirs.Komunitas;
import ais.database.model.sirs.PajakMedis;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;

/**
 * Controller/action ZK untuk pajak. Tipe ini merupakan titik masuk UI yang menghubungkan event
 * layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Window addWindow}, {@code Grid grid},
 * {@code Paging paging}, {@code MyTextbox searchnama}, {@code Combobox searchAsuransi}, {@code Combobox
 * searchKomunitas}, {@code MyTextbox kode}, {@code MyTextbox nama}; inisialisasi/lifecycle ({@code
 * doAfterCompose()}, {@code init()}, {@code initCriteria()}); pembacaan/pencarian ({@code onSearchDefault()});
 * mutasi data ({@code onSave()}); operasi domain lain ({@code onAdd()}). Bagian lain dari kontrak tetap
 * mengikuti kelas induk atau interface yang disebut di atas.</p>
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
public class PajakAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private Window addWindow;
	private Grid grid;
	private Paging paging;

	private MyTextbox searchnama;
	private Combobox searchAsuransi;
	private Combobox searchKomunitas;

	private MyTextbox kode;
	private MyTextbox nama;
	private MyDatebox mulai;
	private MyDatebox sampai;
	private MyDoublebox jumlah;
	private MyTextbox keterangan;
	private Checkbox aktif;

	private AmbilDataAkunKreditBanbox akun;

	private boolean edit = false;
	private boolean delete = false;

	private PajakMedis pajak;
	private Toolbarbutton add;
	private Combobox asuransi;
	private Combobox komunitas;

	@Override
	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			execution.sendRedirect("/logoff");
			return;
		}

		Common.insertCombo(searchAsuransi, "nama", Asuransi.class);
		Common.insertCombo(searchKomunitas, "nama", Komunitas.class);

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

	class PajakRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			// TODO Auto-generated method stub
			final PajakMedis pajak = (PajakMedis) arg1;

			

			new PajakDetailAction(pajak).setParent(arg0);

			RevisiHelper.createNewRevisi(Pajak.class, pajak, pajak.getNama()).setParent(arg0);
			new Label(pajak.getMulai() == null ? "" : Common.dateFormat3.get().format(pajak.getMulai())).setParent(arg0);
			new Label(pajak.getSampai() == null ? "" : Common.dateFormat3.get().format(pajak.getSampai())).setParent(arg0);
			new Label(Common.numberFormat.get().format(pajak.getJumlah())).setParent(arg0);
			new Label(pajak.getAkun().toString()).setParent(arg0);
			new Label(pajak.getAsuransi() == null ? "" : pajak.getAsuransi().toString()).setParent(arg0);
			new Label(pajak.getKomunitas() == null ? "" : pajak.getKomunitas().toString()).setParent(arg0);
			new Label(pajak.getAktif() ? "Ya" : "Tidak").setParent(arg0);
			new Label(pajak.getKeterangan()).setParent(arg0);

			Hbox toolbar = new Hbox();
			Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig("", "/img/edit.gif");
			button.setTooltiptext("Rubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(pajak);
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			button.setParent(toolbar);

			button = new ais.ui.util.MyToolbarbuttonConfig("", "/img/delete.gif");
			button.setTooltiptext("Hapus Data");
			button.setVisible(delete);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah Bapak/Ibu yakin ingin menghapus data pajak ini? Data yang telah dihapus tidak dapat dikembalikan.", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = new Integer(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {
											Session session = HibernateUtil.currentSession();

											session.createSQLQuery(
													"delete from sirs.pajak_detail where pajak = " + pajak.getId())
													.executeUpdate();

											Common.refreshDelete(session, pajak);
											onSearchDefault(event);
										} catch (Exception e) {
											ais.common.Common.tampilErrorJikaAdmin(e);
											MyMessageboxConfig.show(Common.pesan(
													"Mohon maaf, data pajak ini tidak dapat dihapus karena masih berelasi dengan data lain. Rincian teknis: {V1}. Langkah yang dapat dilakukan: (1) hapus terlebih dahulu data lain yang berkaitan dengan data ini; (2) pastikan data tidak sedang digunakan pada transaksi lain; (3) hubungi administrator apabila kendala masih berlanjut.",
													e.getMessage()));
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
		init(new PajakMedis());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(PajakMedis pajak) {
		this.pajak = pajak;
		addWindow.setTitle(pajak.getId() == null ? "Tambah Pajak" : "Ubah Pajak");
		Common.clear(addWindow);
		Borderlayout borderlayout = new Borderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		Grid grid = new Grid();
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Rows rows = new Rows();
		rows.setParent(grid);

		Row row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Kode Pajak")));
		row.appendChild(
				kode = new MyTextbox(pajak.getKode() == null ? Common.generateCode(Pajak.class, 8) : pajak.getKode()));
		kode.setWidth("90%");
		kode.setDisabled(true);

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Nama Pajak")));
		row.appendChild(nama = new MyTextbox(pajak.getNama() == null ? "" : pajak.getNama()));
		nama.setWidth("90%");

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Pajak berlaku mulai")));
		row.appendChild(mulai = new MyDatebox(pajak.getMulai()));
		mulai.setWidth("90%");

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Pajak berlaku sampai")));
		row.appendChild(sampai = new MyDatebox(pajak.getSampai()));
		sampai.setWidth("90%");

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Nilai PajakMedis (%)")));
		row.appendChild(jumlah = new MyDoublebox(pajak.getJumlah()));
		jumlah.setWidth("90%");

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Pajak ini akan masuk ke akun")));
		row.appendChild(akun = new AmbilDataAkunKreditBanbox());
		akun.setValue(pajak.getAkun() == null ? "" : pajak.getAkun().toString());
		akun.setAttribute("akun", pajak.getAkun());
		akun.setWidth("90%");

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Pajak ini berlaku untuk asuransi atau kelompok pasien")));
		row.appendChild(asuransi = new Combobox());
		Common.insertCombo(asuransi, "nama", Asuransi.class);
		Common.selectComboItem(asuransi, pajak.getAsuransi());
		asuransi.setWidth("90%");

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Pajak ini berlaku untuk komunitas pasien")));
		row.appendChild(komunitas = new Combobox());
		Common.insertCombo(komunitas, "nama", Komunitas.class);
		Common.selectComboItem(komunitas, pajak.getKomunitas());
		komunitas.setWidth("90%");

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Aktif (berlaku)")));
		row.appendChild(aktif = new Checkbox());
		aktif.setChecked(pajak.getAktif());

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Keterangan")));
		row.appendChild(keterangan = new MyTextbox(pajak.getKeterangan() == null ? "" : pajak.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("30px");
		toolbar.setParent(south);
		Toolbarbutton cancel = new ais.ui.util.MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				addWindow.setVisible(false);
			}
		});
		cancel.setParent(toolbar);
		Toolbarbutton save = new ais.ui.util.MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (onSave(event)) {
					onSearchDefault(null);
					Common.initPaging(paging, new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							onSearchDefault(null);
						}
					});
					addWindow.setVisible(false);
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);

	}

	public boolean onSave(Event event) throws Exception {
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Nama Pajak Medis wajib diisi terlebih dahulu. Langkah yang dapat dilakukan: (1) isikan Nama Pajak Medis pada kolom yang tersedia; (2) pastikan kolom tidak dikosongkan; (3) simpan kembali data setelah kolom terisi.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (mulai.getValue() == null) {
			MyMessageboxConfig.show("Mohon maaf, tanggal mulai berlaku Pajak Medis wajib diisi terlebih dahulu. Langkah yang dapat dilakukan: (1) tentukan tanggal mulai berlaku pada kolom yang tersedia; (2) pastikan kolom tanggal tidak dikosongkan; (3) simpan kembali data setelah tanggal terisi.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (akun.getAttribute("akun") == null) {
			MyMessageboxConfig.show("Mohon maaf, Akun tujuan pajak wajib dipilih terlebih dahulu. Langkah yang dapat dilakukan: (1) pilih Akun melalui kolom pencarian akun yang tersedia; (2) pastikan Akun tidak dikosongkan; (3) simpan kembali data setelah Akun ditentukan.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (pajak.getId() != null) {
			pajak = (PajakMedis) session.load(PajakMedis.class, pajak.getId());

		}

		pajak.setKomunitas(
				(Komunitas) (komunitas.getSelectedItem() == null ? null : komunitas.getSelectedItem().getValue()));
		pajak.setAktif(aktif.isChecked());
		pajak.setAsuransi(
				(Asuransi) (asuransi.getSelectedItem() == null ? null : asuransi.getSelectedItem().getValue()));
		pajak.setAkun((Akun) akun.getAttribute("akun"));
		pajak.setJumlah(jumlah.getValue());
		pajak.setMulai(mulai.getValue());
		pajak.setSampai(sampai.getValue());
		pajak.setNama(nama.getValue());
		pajak.setKeterangan(keterangan.getValue());

		if (pajak.getId() != null) {
			Common.refreshUpdate(session, pajak);
		} else {
			pajak.setKode(Common.generateCode(Pajak.class, 8));
			session.save(pajak);
		}
		return true;
	}

	private Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(PajakMedis.class)
				.add((searchnama == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.ilike("nama", searchnama.getValue(), MatchMode.ANYWHERE)))
				.add(searchAsuransi.getSelectedItem() == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("asuransi", searchAsuransi.getSelectedItem().getValue()))
				.add(searchKomunitas.getSelectedItem() == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("komunitas", searchKomunitas.getSelectedItem().getValue()));
		;
		if (order)
			criteria.addOrder(Order.desc("id"));

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);
		List<PajakMedis> pajak = ConstantValues
				.simpleList(
						initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE).setFirstResult(
								Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())),
						PajakMedis.class);
		ListModel strset = new SimpleListModel(pajak);
		grid.setRowRenderer(new PajakRenderer());
		grid.setModel(strset);

		grid.renderAll();

	}

}
