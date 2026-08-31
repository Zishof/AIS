package ais.action.master;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.LogicalExpression;
import org.hibernate.criterion.Order;
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
import org.zkoss.zul.Paging;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GrupChecklistPenilaianDosen;
import ais.database.model.GrupChecklistPenilaianUmum;
import ais.database.model.GrupKuesionerUmum;
import ais.database.model.JadwalChecklistPenilaianUmum;
import ais.database.model.Perkuliahan;
import ais.database.model.sekolah.GrupChecklistPenilaianGuru;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyRadioConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.action.master.helper.FilterLanjutHelper;

/**
 * Controller/action ZK untuk jadwal checklist penilaian umum. Tipe ini merupakan titik masuk UI
 * yang menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus
 * oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code Combobox searchgrupChecklistPenilaianUmum}, {@code Combobox
 * searchgrupChecklistPenilaianDosen}, {@code Combobox searchgrupChecklistPenilaianGuru}, {@code Combobox
 * searchtahunakademik}, {@code Textbox keterangan}; inisialisasi/lifecycle ({@code doBeforeCompose()}, {@code
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
public class JadwalChecklistPenilaianUmumAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Combobox searchgrupChecklistPenilaianUmum;
	private Combobox searchgrupChecklistPenilaianDosen;
	private Combobox searchgrupChecklistPenilaianGuru;
	private Combobox searchtahunakademik;

	private Textbox keterangan;
	private Combobox grupChecklistPenilaianUmum;
	private Combobox searchDiperuntukkan;
	private Combobox tahunAkademik;
	private Radiogroup ganjil;
	private MyDatebox mulai;
	private MyDatebox sampai;

	private boolean edit = false;
	private boolean delete = false;

	private JadwalChecklistPenilaianUmum jadwalChecklistPenilaianUmum;
	private MyToolbarbuttonConfig add;
	private Combobox grupKuesionerUmum;
	private Combobox grupChecklistPenilaianDosen;
	private Combobox grupChecklistPenilaianGuru;

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

		GrupChecklistPenilaianUmumAction.diperuntukkan(searchDiperuntukkan);
		GrupChecklistPenilaianUmumAction.diperuntukkanPertemuan(searchDiperuntukkan);

		searchDiperuntukkan.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				LogicalExpression crit = Restrictions.and(
						searchDiperuntukkan.getSelectedItem() == null
								|| searchDiperuntukkan.getSelectedItem().getValue() == null
										? Restrictions.sqlRestriction("true")
										: Restrictions.eq("diperuntukkan",
												searchDiperuntukkan.getSelectedItem().getValue()),
						Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")));

				Common.insertCombo(searchgrupChecklistPenilaianUmum, "isi", "diperuntukkan",
						GrupChecklistPenilaianUmum.class, crit);
				onSearchDefault(null);
			}
		});

		Common.insertComboDanSemua(searchgrupChecklistPenilaianUmum, "isi", GrupChecklistPenilaianUmum.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		Common.insertComboDanSemua(searchgrupChecklistPenilaianDosen, "isi", GrupChecklistPenilaianDosen.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		Common.insertComboDanSemua(searchgrupChecklistPenilaianGuru, "isi", GrupChecklistPenilaianGuru.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		Common.generateTahunAjaranDanSemua(searchtahunakademik);

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});
	        FilterLanjutHelper.setup(comp);
}

	class JadwalChecklistPenilaianUmumRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final JadwalChecklistPenilaianUmum jadwalChecklistPenilaianUmum = (JadwalChecklistPenilaianUmum) arg1;

			new Label(jadwalChecklistPenilaianUmum.getMulai() == null ? ""
					: Common.dateFormat2.get().format(jadwalChecklistPenilaianUmum.getMulai())).setParent(arg0);
			new Label(jadwalChecklistPenilaianUmum.getSampai() == null ? ""
					: Common.dateFormat2.get().format(jadwalChecklistPenilaianUmum.getSampai())).setParent(arg0);

			Vbox vbox = new Vbox();
			vbox.setParent(arg0);
			if (jadwalChecklistPenilaianUmum.getGrupChecklistPenilaianUmum() != null)
				RevisiHelper.createNewRevisi(JadwalChecklistPenilaianUmum.class, jadwalChecklistPenilaianUmum,
						jadwalChecklistPenilaianUmum.getGrupChecklistPenilaianUmum().getIsi()).setParent(vbox);

			if (jadwalChecklistPenilaianUmum.getGrupChecklistPenilaianDosen() != null)
				RevisiHelper.createNewRevisi(JadwalChecklistPenilaianUmum.class, jadwalChecklistPenilaianUmum,
						jadwalChecklistPenilaianUmum.getGrupChecklistPenilaianDosen().getIsi()).setParent(vbox);

			if (jadwalChecklistPenilaianUmum.getGrupChecklistPenilaianGuru() != null)
				RevisiHelper.createNewRevisi(JadwalChecklistPenilaianUmum.class, jadwalChecklistPenilaianUmum,
						jadwalChecklistPenilaianUmum.getGrupChecklistPenilaianGuru().getIsi()).setParent(vbox);

			new Label(jadwalChecklistPenilaianUmum.getGrupChecklistPenilaianDosen() != null ? "Mahasiswa"
					: jadwalChecklistPenilaianUmum.getGrupChecklistPenilaianGuru() != null ? "Siswa"
							: (jadwalChecklistPenilaianUmum.getGrupChecklistPenilaianUmum() == null ? ""
									: jadwalChecklistPenilaianUmum.getGrupChecklistPenilaianUmum().getDiperuntukkan()))
					.setParent(arg0);

			new Label(jadwalChecklistPenilaianUmum.getGrupKuesionerUmum() == null ? ""
					: jadwalChecklistPenilaianUmum.getGrupKuesionerUmum().getNama()).setParent(arg0);

			new Label(jadwalChecklistPenilaianUmum.getTahunAkademik()).setParent(arg0);
			new Label(jadwalChecklistPenilaianUmum.getSemester()).setParent(arg0);
			new Label(jadwalChecklistPenilaianUmum.getKeterangan()).setParent(arg0);

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
					init(jadwalChecklistPenilaianUmum);
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

											Common.refreshDelete(jadwalChecklistPenilaianUmum);

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
		JadwalChecklistPenilaianUmum jadwalChecklistPenilaianUmum = new JadwalChecklistPenilaianUmum();
		jadwalChecklistPenilaianUmum.setTahunAkademik((String) (searchtahunakademik.getSelectedItem() == null
				|| searchtahunakademik.getSelectedItem().getValue() == null ? null
						: searchtahunakademik.getSelectedItem().getValue()));
		jadwalChecklistPenilaianUmum.setGrupChecklistPenilaianUmum(
				(GrupChecklistPenilaianUmum) (searchgrupChecklistPenilaianUmum.getSelectedItem() == null ? null
						: searchgrupChecklistPenilaianUmum.getSelectedItem().getValue()));
		init(jadwalChecklistPenilaianUmum);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(JadwalChecklistPenilaianUmum jadwalChecklistPenilaianUmum) {

		Common.generateTahunAjaran(tahunAkademik = new Combobox());

		Common.insertCombo(grupChecklistPenilaianUmum = new Combobox(), "isi", GrupChecklistPenilaianUmum.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		this.jadwalChecklistPenilaianUmum = jadwalChecklistPenilaianUmum;
		addWindow.setTitle(jadwalChecklistPenilaianUmum.getId() == null ? "Tambah Jadwal Angket Umum" : "Ubah Jadwal Angket Umum");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Mulai"));
		row.appendChild(mulai = new MyDatebox(
				jadwalChecklistPenilaianUmum.getMulai() == null ? null : jadwalChecklistPenilaianUmum.getMulai()));
//		mulai.setConstraint("no empty");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Selesai"));
		row.appendChild(sampai = new MyDatebox(
				jadwalChecklistPenilaianUmum.getSampai() == null ? null : jadwalChecklistPenilaianUmum.getSampai()));
//		sampai.setConstraint("no empty");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Grup Angket Umum"));
		Common.selectComboItem(grupChecklistPenilaianUmum,
				jadwalChecklistPenilaianUmum.getGrupChecklistPenilaianUmum());
		row.appendChild(grupChecklistPenilaianUmum);
		grupChecklistPenilaianUmum.setWidth("90%");

		Common.insertComboDanSemua(grupChecklistPenilaianDosen = new Combobox(), "isi",
				GrupChecklistPenilaianDosen.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Grup Angket dosen oleh mahasiswa"));
		Common.selectComboItem(grupChecklistPenilaianDosen,
				jadwalChecklistPenilaianUmum.getGrupChecklistPenilaianDosen());
		row.appendChild(grupChecklistPenilaianDosen);
		grupChecklistPenilaianDosen.setWidth("90%");

		Common.insertComboDanSemua(grupChecklistPenilaianGuru = new Combobox(), "isi", GrupChecklistPenilaianGuru.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Grup Angket guru oleh siswa"));
		Common.selectComboItem(grupChecklistPenilaianGuru,
				jadwalChecklistPenilaianUmum.getGrupChecklistPenilaianGuru());
		row.appendChild(grupChecklistPenilaianGuru);
		grupChecklistPenilaianGuru.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
		Common.selectComboItem(tahunAkademik, jadwalChecklistPenilaianUmum.getTahunAkademik());
		row.appendChild(tahunAkademik);
		tahunAkademik.setWidth("90%");
//		tahunAkademik.setConstraint("no empty");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester"));
		row.appendChild(ganjil = new Radiogroup());
		MyRadioConfig radio = new MyRadioConfig(Perkuliahan.GANJIL);

		radio.setParent(ganjil);
		radio.setChecked(jadwalChecklistPenilaianUmum.getSemester().equals(Perkuliahan.GANJIL));
		radio = new MyRadioConfig(Perkuliahan.GENAP);

		radio.setParent(ganjil);
		radio.setChecked(jadwalChecklistPenilaianUmum.getSemester().equals(Perkuliahan.GENAP));

		grupKuesionerUmum = new Combobox();
		Common.insertComboDanSemua(grupKuesionerUmum, new String[] { "nama", "satuanKerja" }, "keterangan",
				GrupKuesionerUmum.class, "=Tanpa Grup Kuosioner=", Restrictions.eq("aktif", true));
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Grup Kuosioner"));
		Common.selectComboItem(grupKuesionerUmum, jadwalChecklistPenilaianUmum.getGrupKuesionerUmum());
		row.appendChild(grupKuesionerUmum);
		grupKuesionerUmum.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(jadwalChecklistPenilaianUmum.getKeterangan() == null ? ""
				: jadwalChecklistPenilaianUmum.getKeterangan()));
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
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);

	}

	public boolean onSave(Event event) throws Exception {

		if (grupChecklistPenilaianUmum.getSelectedItem() == null
				|| grupChecklistPenilaianDosen.getSelectedItem() == null
				|| grupChecklistPenilaianGuru.getSelectedItem() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Grup umum atau dosen atau guru",
					"Kolom Grup umum atau dosen atau guru belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Grup umum atau dosen atau guru.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		if (tahunAkademik.getSelectedItem() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Tahun Akademik",
					"Kolom Tahun Akademik belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Tahun Akademik.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		Session session = HibernateUtil.currentSession();

		JadwalChecklistPenilaianUmum count = (JadwalChecklistPenilaianUmum) session
				.createCriteria(JadwalChecklistPenilaianUmum.class)

				.add(

						Restrictions
								.or(grupChecklistPenilaianDosen.getSelectedItem() == null ? Restrictions.sqlRestriction(
										"false")
										: Restrictions.eq("grupChecklistPenilaianDosen",
												grupChecklistPenilaianDosen.getSelectedItem().getValue()),

										Restrictions.or(
												grupChecklistPenilaianGuru.getSelectedItem() == null
														? Restrictions.sqlRestriction("false")
														: Restrictions.eq(
																"grupChecklistPenilaianGuru",
																grupChecklistPenilaianGuru.getSelectedItem()
																		.getValue()),
												grupChecklistPenilaianUmum.getSelectedItem() == null
														? Restrictions.sqlRestriction("false")
														: Restrictions.eq("grupChecklistPenilaianUmum",
																grupChecklistPenilaianUmum.getSelectedItem()
																		.getValue())))

				)

				.add(Restrictions.eq("tahunAkademik", tahunAkademik.getSelectedItem().getValue()))
				.add(Restrictions.eq("semester", ganjil.getSelectedItem().getLabel()))
				.add(jadwalChecklistPenilaianUmum.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", jadwalChecklistPenilaianUmum.getId()))
				.setMaxResults(1).uniqueResult();

		if (count != null) {
			MyMessageboxConfig.show(
					"Jadwal penilaian angket untuk semester \"" + (ganjil.getSelectedItem().getLabel())
							+ "\",  Tahun Akademik \"" + (tahunAkademik.getSelectedItem().getValue()) + "\" sudah ada",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (jadwalChecklistPenilaianUmum.getId() != null) {
			jadwalChecklistPenilaianUmum = (JadwalChecklistPenilaianUmum) session
					.load(JadwalChecklistPenilaianUmum.class, jadwalChecklistPenilaianUmum.getId());
		}

		jadwalChecklistPenilaianUmum.setSemester(
				ganjil.getSelectedItem().getLabel().equals("Ganjil") ? Perkuliahan.GANJIL : Perkuliahan.GENAP);
		jadwalChecklistPenilaianUmum.setGrupChecklistPenilaianUmum(
				(GrupChecklistPenilaianUmum) grupChecklistPenilaianUmum.getSelectedItem().getValue());

		jadwalChecklistPenilaianUmum.setGrupChecklistPenilaianDosen(
				(GrupChecklistPenilaianDosen) (grupChecklistPenilaianDosen.getSelectedItem() == null ? null
						: grupChecklistPenilaianDosen.getSelectedItem().getValue()));

		jadwalChecklistPenilaianUmum.setGrupChecklistPenilaianGuru(
				(GrupChecklistPenilaianGuru) (grupChecklistPenilaianGuru.getSelectedItem() == null ? null
						: grupChecklistPenilaianGuru.getSelectedItem().getValue()));

		jadwalChecklistPenilaianUmum.setSampai(sampai.getValue());
		jadwalChecklistPenilaianUmum.setMulai(mulai.getValue());
		jadwalChecklistPenilaianUmum.setTahunAkademik((String) tahunAkademik.getSelectedItem().getValue());
		jadwalChecklistPenilaianUmum.setKeterangan(keterangan.getValue());
		jadwalChecklistPenilaianUmum
				.setGrupKuesionerUmum((GrupKuesionerUmum) (grupKuesionerUmum.getSelectedItem() == null ? null
						: grupKuesionerUmum.getSelectedItem().getValue()));

		Common.refreshSaveOrUpdate(session, jadwalChecklistPenilaianUmum);
		return true;
	}

	public Criteria initCriteria(boolean order) {

		GrupChecklistPenilaianUmum grupChecklistPenilaianUmum = (GrupChecklistPenilaianUmum) (searchgrupChecklistPenilaianUmum
				.getSelectedItem() == null ? null : searchgrupChecklistPenilaianUmum.getSelectedItem().getValue());

		GrupChecklistPenilaianDosen grupChecklistPenilaianDosen = (GrupChecklistPenilaianDosen) (searchgrupChecklistPenilaianDosen
				.getSelectedItem() == null ? null : searchgrupChecklistPenilaianDosen.getSelectedItem().getValue());

		GrupChecklistPenilaianGuru grupChecklistPenilaianGuru = (GrupChecklistPenilaianGuru) (searchgrupChecklistPenilaianGuru
				.getSelectedItem() == null ? null : searchgrupChecklistPenilaianGuru.getSelectedItem().getValue());

		String tahunAkademik = (String) (searchtahunakademik.getSelectedItem() == null
				|| searchtahunakademik.getSelectedItem().getValue() == null ? null
						: searchtahunakademik.getSelectedItem().getValue());

		String untuk = (String) (searchDiperuntukkan.getSelectedItem() == null ? null
				: searchDiperuntukkan.getSelectedItem().getValue());

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(JadwalChecklistPenilaianUmum.class);

		if (untuk != null) {
			criteria.createAlias("grupChecklistPenilaianUmum", "grupChecklistPenilaianUmum", Criteria.LEFT_JOIN);
		}
		if (order)
			criteria.addOrder(Order.desc("id"));

		criteria.add(tahunAkademik == null ? Restrictions.sqlRestriction("1=1")
				: Restrictions.eq("tahunAkademik", tahunAkademik))

				.add(untuk == null ? Restrictions.sqlRestriction("1=1") :

						untuk.equals(GrupChecklistPenilaianUmum.UNTUK_MAHASISWA)
								? Restrictions.or(Restrictions.isNotNull("grupChecklistPenilaianDosen"),
										Restrictions.eq("grupChecklistPenilaianUmum.diperuntukkan", untuk))
								:

								untuk.equals(GrupChecklistPenilaianUmum.UNTUK_SISWA)
										? Restrictions.or(Restrictions.isNotNull("grupChecklistPenilaianGuru"),
												Restrictions.eq("grupChecklistPenilaianUmum.diperuntukkan", untuk))
										:

										Restrictions.eq("grupChecklistPenilaianUmum.diperuntukkan", untuk))

				.add(grupChecklistPenilaianUmum == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("grupChecklistPenilaianUmum", grupChecklistPenilaianUmum))

				.add(grupChecklistPenilaianGuru == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("grupChecklistPenilaianGuru", grupChecklistPenilaianGuru))

				.add(grupChecklistPenilaianDosen == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("grupChecklistPenilaianDosen", grupChecklistPenilaianDosen))

		;
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<JadwalChecklistPenilaianUmum> jadwalChecklistPenilaianUmum = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(jadwalChecklistPenilaianUmum);
		grid.setRowRenderer(new JadwalChecklistPenilaianUmumRenderer());
		grid.setModelCheckMobile(strset);

	}

}
