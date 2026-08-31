package ais.action.master.pmb;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

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
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Paket;
import ais.database.model.PaketPunyaParameterVerifikasiCalonMahasiswa;
import ais.database.model.ParameterVerifikasiCalonMahasiswa;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Controller/action ZK untuk paket punya parameter verifikasi calon mahasiswa. Tipe ini merupakan
 * titik masuk UI yang menghubungkan event layar dengan perilaku domain yang diwarisi atau
 * dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code
 * PaketPunyaParameterVerifikasiCalonMahasiswa paketPunyaParameterVerifikasiCalonMahasiswa}, {@code MyWindow
 * addWindow}, {@code MyGrid grid}, {@code Textbox searchnama}, {@code Textbox searchjudul}, {@code Combobox
 * paket}, {@code Combobox searchpaket}, {@code boolean edit}; inisialisasi/lifecycle ({@code doBeforeCompose()},
 * {@code doAfterCompose()}, {@code init()}); pembacaan/pencarian ({@code onSearchDefault()}); mutasi data
 * ({@code onSave()}); operasi domain lain ({@code onAdd()}). Bagian lain dari kontrak tetap mengikuti kelas
 * induk atau interface yang disebut di atas.</p>
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
public class PaketPunyaParameterVerifikasiCalonMahasiswaAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7896939206824822505L;
	private PaketPunyaParameterVerifikasiCalonMahasiswa paketPunyaParameterVerifikasiCalonMahasiswa;
	private MyWindow addWindow;
	private MyGrid grid;

	private Textbox searchnama;
	private Textbox searchjudul;
	private Combobox paket;
	private Combobox searchpaket;

	private boolean edit = true;
	private boolean delete = true;

	private Paket selectedPaket;
	private Textbox judul;
	private Textbox nama;
	private Set<ParameterVerifikasiCalonMahasiswa> selectedParameterVerifikasiCalonMahasiswa;

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
		public void render(final Row arg0, Object arg1) throws Exception {arg0.setValign("top");
			// TODO Auto-generated method stub
			final PaketPunyaParameterVerifikasiCalonMahasiswa paketPunyaParameterVerifikasiCalonMahasiswa = (PaketPunyaParameterVerifikasiCalonMahasiswa) arg1;

			if (paketPunyaParameterVerifikasiCalonMahasiswa.getPaket() == null && selectedPaket != null) {
				paketPunyaParameterVerifikasiCalonMahasiswa.setPaket(selectedPaket);
			}

			new Label(paketPunyaParameterVerifikasiCalonMahasiswa.getPaket().getNama()).setParent(arg0);
			new Label(paketPunyaParameterVerifikasiCalonMahasiswa.getJudul()).setParent(arg0);
			new Label(paketPunyaParameterVerifikasiCalonMahasiswa.getNama()).setParent(arg0);

			Vbox vbox = new Vbox();
			vbox.setParent(arg0);
			int i = 1;
			for (ParameterVerifikasiCalonMahasiswa parameterVerifikasiCalonMahasiswa : new TreeSet<ParameterVerifikasiCalonMahasiswa>(
					paketPunyaParameterVerifikasiCalonMahasiswa.getParameterVerifikasiCalonMahasiswas())) {
				vbox.appendChild(new MyLabelAgakKecil(i + ". " + parameterVerifikasiCalonMahasiswa.getNama()));
				i++;
			}

			// Kolom aksi rapi (pola MahasiswaAction): semua tombol dibungkus kebab popup (⋯)
			// via UIHelper.buatBarisAksi — kolom aksi jadi kecil dan konsisten antar layar.
			final java.util.List<org.zkoss.zk.ui.Component> aksiButtons =
					new java.util.ArrayList<org.zkoss.zk.ui.Component>();

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(paketPunyaParameterVerifikasiCalonMahasiswa);
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
									Common.refreshDelete(paketPunyaParameterVerifikasiCalonMahasiswa);
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
			aksiButtons.add(button);
			ais.ui.util.UIHelper.buatBarisAksi(arg0, 3, aksiButtons);
		}

	}

	public void onAdd(Event event) throws Exception {
		init(new PaketPunyaParameterVerifikasiCalonMahasiswa());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@SuppressWarnings({ "unchecked", "deprecation" })
	private void init(PaketPunyaParameterVerifikasiCalonMahasiswa paketPunyaParameterVerifikasiCalonMahasiswa) {
		this.paketPunyaParameterVerifikasiCalonMahasiswa = paketPunyaParameterVerifikasiCalonMahasiswa;
		addWindow.setTitle(paketPunyaParameterVerifikasiCalonMahasiswa.getId() == null ? "Tambah Parameter Verifikasi" : "Ubah Parameter Verifikasi");
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

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Judul *"));
		row.appendChild(judul = new Textbox(paketPunyaParameterVerifikasiCalonMahasiswa.getJudul()));
		judul.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Parameter *"));
		row.appendChild(nama = new Textbox(paketPunyaParameterVerifikasiCalonMahasiswa.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Paket *"));
		Common.selectComboItem(paket, paketPunyaParameterVerifikasiCalonMahasiswa.getPaket() == null ? null
				: paketPunyaParameterVerifikasiCalonMahasiswa.getPaket());
		row.appendChild(paket);
		paket.setWidth("90%");

		if (selectedPaket != null) {
			Common.selectComboItem(paket, selectedPaket);
			paket.setDisabled(true);
		}

		row = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.setParent(rows);
		MyGrid subGrid = new MyGrid();
		row.appendChild(subGrid);

		Columns subColumns = new Columns();
		subColumns.setParent(subGrid);
		subColumns.appendChild(new Column("Parameter Rinci"));

		Rows subRows = new Rows();
		subRows.setParent(subGrid);

		MyFormRow subRow = new MyFormRow();
		subRow.setStyle("border:0px;background: transparent;");
		subRow.setParent(subRows);
		subRow.setValign("top");

		List<ParameterVerifikasiCalonMahasiswa> parameterVerifikasiCalonMahasiswas = HibernateUtil.currentSession()
				.createCriteria(ParameterVerifikasiCalonMahasiswa.class).addOrder(Order.asc("nomorUrut"))
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).list();

		if (paketPunyaParameterVerifikasiCalonMahasiswa.getId() != null) {
			HibernateUtil.currentSession().refresh(this.paketPunyaParameterVerifikasiCalonMahasiswa);
		}
		selectedParameterVerifikasiCalonMahasiswa = this.paketPunyaParameterVerifikasiCalonMahasiswa
				.getParameterVerifikasiCalonMahasiswas();

		Vbox vboxSkala = new Vbox();
		vboxSkala.setPack("top");
		vboxSkala.setParent(subRow);
		for (final ParameterVerifikasiCalonMahasiswa parameterVerifikasiCalonMahasiswa : parameterVerifikasiCalonMahasiswas) {
			final Checkbox checkbox = new Checkbox(parameterVerifikasiCalonMahasiswa.getNama());
			checkbox.setParent(vboxSkala);
			checkbox.setChecked(selectedParameterVerifikasiCalonMahasiswa.contains(parameterVerifikasiCalonMahasiswa));
			checkbox.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (checkbox.isChecked()) {
						selectedParameterVerifikasiCalonMahasiswa.add(parameterVerifikasiCalonMahasiswa);
					} else {
						selectedParameterVerifikasiCalonMahasiswa.remove(parameterVerifikasiCalonMahasiswa);
					}
				}
			});
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
		if (judul.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Judul Parameter belum diisi. Langkah yang dapat dilakukan: (1) isi kolom Judul Parameter dengan judul yang sesuai; (2) pastikan kolom tidak kosong atau hanya spasi; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Nama Parameter belum diisi. Langkah yang dapat dilakukan: (1) isi kolom Nama Parameter dengan nama yang sesuai; (2) pastikan kolom tidak kosong atau hanya spasi; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (paket.getSelectedItem() == null) {
			MyMessageboxConfig.show("Mohon maaf, Paket belum dipilih. Langkah yang dapat dilakukan: (1) pilih Paket dari daftar dropdown yang tersedia; (2) pastikan data paket sudah terdaftar di sistem; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		paketPunyaParameterVerifikasiCalonMahasiswa.setJudul(judul.getValue().trim());
		paketPunyaParameterVerifikasiCalonMahasiswa.setNama(nama.getValue().trim());
		paketPunyaParameterVerifikasiCalonMahasiswa
				.setPaket(selectedPaket != null ? selectedPaket : (Paket) paket.getSelectedItem().getValue());
		paketPunyaParameterVerifikasiCalonMahasiswa
				.setParameterVerifikasiCalonMahasiswas(selectedParameterVerifikasiCalonMahasiswa);

		Common.refreshSaveOrUpdate(paketPunyaParameterVerifikasiCalonMahasiswa);
		return true;
	}

	// matapelajaranSekolah

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Session session = HibernateUtil.currentSession();
				List<PaketPunyaParameterVerifikasiCalonMahasiswa> paketPunyaParameterVerifikasiCalonMahasiswa = session
						.createCriteria(PaketPunyaParameterVerifikasiCalonMahasiswa.class)

				.add(searchpaket.getSelectedItem() == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("paket", searchpaket.getSelectedItem().getValue()))

				.add(searchjudul.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("judul", searchjudul.getValue().trim(), MatchMode.ANYWHERE))

				.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))

				.setMaxResults(Common.MAX_RESULT).list();
				ListModel strset = new SimpleListModel(paketPunyaParameterVerifikasiCalonMahasiswa);
				grid.setRowRenderer(new PilihanPaketRenderer());
				grid.setModelCheckMobile(strset);
			}
		});

	}

}
