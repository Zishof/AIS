package ais.action.master.pmb;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Toolbar;

import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GelombangPendaftaran;
import ais.database.model.Paket;
import ais.database.model.PaketPunyaGelombangPendaftaran;
import ais.database.model.PerguruanTinggi;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Controller/action ZK untuk paket punya gelombang pendaftaran. Tipe ini merupakan titik masuk UI
 * yang menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus
 * oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code PaketPunyaGelombangPendaftaran
 * paketPunyaGelombangPendaftaran}, {@code MyWindow addWindow}, {@code MyGrid grid}, {@code Combobox
 * gelombangPendaftaran}, {@code Combobox searchgelombangPendaftaran}, {@code Combobox paket}, {@code Combobox
 * searchpaket}, {@code boolean edit}; inisialisasi/lifecycle ({@code doBeforeCompose()}, {@code
 * doAfterCompose()}, {@code init()}); pembacaan/pencarian ({@code onSearchDefault()}); mutasi data ({@code
 * onSave()}); operasi domain lain ({@code onAdd()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau
 * interface yang disebut di atas.</p>
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
public class PaketPunyaGelombangPendaftaranAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7896939206824822505L;
	private PaketPunyaGelombangPendaftaran paketPunyaGelombangPendaftaran;
	private MyWindow addWindow;
	private MyGrid grid;

	private Combobox gelombangPendaftaran;
	private Combobox searchgelombangPendaftaran;

	private Combobox paket;
	private Combobox searchpaket;

	private boolean edit = true;
	private boolean delete = true;

	private Paket selectedPaket;

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

		Common.insertCombo(gelombangPendaftaran = new Combobox(),
				new String[] { "nama", "tahunAkademik", "mulai", "sampai", "jenisSeleksi" }, "tahunAkademik",
				GelombangPendaftaran.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.insertCombo(searchgelombangPendaftaran, new String[] { "nama", "mulai", "sampai", "jenisSeleksi" },
				"tahunAkademik", GelombangPendaftaran.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		Common.insertCombo(paket = new Combobox(), "nama", "keterangan", Paket.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.insertCombo(searchpaket, "nama", "keterangan", Paket.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		if (execution.getParameter("paket") != null) {
			selectedPaket = (Paket) HibernateUtil.currentSession().createCriteria(Paket.class)
					.add(Restrictions.idEq(Long.parseLong(execution.getParameter("paket")))).uniqueResult();
			Common.selectComboItem(searchpaket, selectedPaket);
			searchpaket.setDisabled(true);
		}

		onSearchDefault(null);
	}

	class PilihanPaketRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final PaketPunyaGelombangPendaftaran paketPunyaGelombangPendaftaran = (PaketPunyaGelombangPendaftaran) arg1;

			new Label(paketPunyaGelombangPendaftaran.getPaket().getNama()).setParent(arg0);
			new Label(paketPunyaGelombangPendaftaran.getGelombangPendaftaran() == null ? ""
					: paketPunyaGelombangPendaftaran.getGelombangPendaftaran().getNama()).setParent(arg0);

			new Label(paketPunyaGelombangPendaftaran.getGelombangPendaftaran() == null ? ""
					: paketPunyaGelombangPendaftaran.getGelombangPendaftaran().getTahunAkademik()).setParent(arg0);

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(paketPunyaGelombangPendaftaran);
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
											Common.refreshDelete(paketPunyaGelombangPendaftaran);
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
		init(new PaketPunyaGelombangPendaftaran());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(PaketPunyaGelombangPendaftaran paketPunyaGelombangPendaftaran) {
		this.paketPunyaGelombangPendaftaran = paketPunyaGelombangPendaftaran;
		addWindow.setTitle(paketPunyaGelombangPendaftaran.getId() == null ? "Tambah Gelombang Pendaftaran" : "Ubah Gelombang Pendaftaran");
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

		Rows rows = new Rows();
		rows.setParent(grid);
		PerguruanTinggi selectedPerguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Gelombang Pendaftaran *"));
		Common.insertComboDanSemua(gelombangPendaftaran,
				new String[] { "nama", "tahunAkademik", "mulai", "sampai", "jenisSeleksi" }, "tahunAkademik",
				GelombangPendaftaran.class, "== Klik disini untuk pilih ==",
				selectedPerguruanTinggi == null || selectedPerguruanTinggi.getId() == null
						? Restrictions.sqlRestriction("true")
						: Restrictions.or(Restrictions.eq("perguruanTinggi", selectedPerguruanTinggi),
								Restrictions.isNull("perguruanTinggi")),
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		row.appendChild(gelombangPendaftaran);
		gelombangPendaftaran.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Paket"));
		Common.selectComboItem(paket,
				paketPunyaGelombangPendaftaran.getPaket() == null ? null : paketPunyaGelombangPendaftaran.getPaket());
		row.appendChild(paket);
		paket.setWidth("90%");

		if (selectedPaket != null) {
			Common.selectComboItem(true, paket, selectedPaket);
			paket.setDisabled(true);
		}

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

		if (gelombangPendaftaran.getSelectedItem() == null
				|| gelombangPendaftaran.getSelectedItem().getValue() == null) {
			MyMessageboxConfig.show("Mohon maaf, Gelombang Pendaftaran belum dipilih. Langkah yang dapat dilakukan: (1) pilih Gelombang Pendaftaran dari daftar dropdown yang tersedia; (2) pastikan gelombang pendaftaran sudah aktif dan terdaftar di sistem; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (paketPunyaGelombangPendaftaran.getId() != null) {
			paketPunyaGelombangPendaftaran = (PaketPunyaGelombangPendaftaran) session
					.load(PaketPunyaGelombangPendaftaran.class, paketPunyaGelombangPendaftaran.getId());

		}

		paketPunyaGelombangPendaftaran
				.setGelombangPendaftaran((GelombangPendaftaran) gelombangPendaftaran.getSelectedItem().getValue());

		paketPunyaGelombangPendaftaran.setPaket((Paket) paket.getSelectedItem().getValue());

		Common.refreshSaveOrUpdate(session, paketPunyaGelombangPendaftaran);

		return true;
	}

	// gelombangPendaftaran

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Session session = HibernateUtil.currentSession();
		List<PaketPunyaGelombangPendaftaran> paketPunyaGelombangPendaftaran = session
				.createCriteria(PaketPunyaGelombangPendaftaran.class)

				.add(searchpaket.getSelectedItem() == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("paket", searchpaket.getSelectedItem().getValue()))

				.add(searchgelombangPendaftaran.getSelectedItem() == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("gelombangPendaftaran",
								searchgelombangPendaftaran.getSelectedItem().getValue()))

				.setMaxResults(Common.MAX_RESULT).list();
		ListModel strset = new SimpleListModel(paketPunyaGelombangPendaftaran);
		grid.setRowRenderer(new PilihanPaketRenderer());
		grid.setModelCheckMobile(strset);

	}

}
