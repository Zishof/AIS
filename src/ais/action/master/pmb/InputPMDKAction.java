package ais.action.master.pmb;

import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
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
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.common.CommonPMB;
import ais.common.CommonPrivilages;
import ais.database.dao.BiodataCalonMahasiswaDao;
import ais.database.dao.DaoFactory;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.JenisKartuIdentitasMahasiswaBaru;
import ais.database.model.JenisSekolahMahasiswaBaru;
import ais.database.model.JenisSeleksi;
import ais.database.model.Jenjang;
import ais.database.model.Mahasiswa;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.action.master.helper.FilterLanjutHelper;

public class InputPMDKAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private MyGrid grid;

	private Textbox searchnama;
	private Textbox searchsekolah;
	private Combobox searchtahun;
	private Textbox nama;
	private MyDatebox tanggalLahir;
	private Combobox asalSekolah;
	private Combobox jenisSekolah;
	private Textbox nis;
	private Label jumlahPendaftar;

	private boolean edit = false;
	private boolean delete = false;

	private BiodataCalonMahasiswa biodataCalonMahasiswa;
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

		Common.initLaguage();
		if (session.getAttribute("usersTemp") == null) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		Common.insertCombo(jenisSekolah = new Combobox(), "nama", JenisSekolahMahasiswaBaru.class, Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		int tahunCurrent = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
		MyComboitemConfig comboitem;
		for (int i = tahunCurrent - 5; i <= tahunCurrent; i++) {
			comboitem = new MyComboitemConfig(i + "");
			comboitem.setValue(i);
			searchtahun.appendChild(comboitem);
		}
		Common.selectComboItem(searchtahun, tahunCurrent);
		onSearchDefault(null);
	        FilterLanjutHelper.setup(comp);
}

	class BiodataCalonRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {arg0.setValign("top");
			// TODO Auto-generated method stub
			final BiodataCalonMahasiswa biodataCalonMahasiswa = (BiodataCalonMahasiswa) arg1;
			new Label(biodataCalonMahasiswa.getNoRegistrasi()).setParent(arg0);
			new Label(biodataCalonMahasiswa.getNama().toUpperCase()).setParent(arg0);
			new Label(Common.dateFormat2.get().format(biodataCalonMahasiswa.getTanggalLahir())).setParent(arg0);
			new Label(biodataCalonMahasiswa.getAsalSma().toUpperCase()).setParent(arg0);

			String myJurusan = "";
			String myfakultas = "";
			if (biodataCalonMahasiswa.getProdi1() != null) {
				myJurusan += biodataCalonMahasiswa.getProdi1().getNama();
				myfakultas += biodataCalonMahasiswa.getProdi1().getFakultas().getNama();
			}
			if (biodataCalonMahasiswa.getProdi2() != null) {
				myJurusan += !myJurusan.equals("") ? " dan " + biodataCalonMahasiswa.getProdi2().getNama()
						: biodataCalonMahasiswa.getProdi2().getNama();
				myfakultas += !myfakultas.equals("")
						? " dan " + biodataCalonMahasiswa.getProdi2().getFakultas().getNama()
						: biodataCalonMahasiswa.getProdi2().getFakultas().getNama();
			}

			new Label(myJurusan).setParent(arg0);
			new Label(myfakultas).setParent(arg0);

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(biodataCalonMahasiswa);
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
									BiodataCalonMahasiswaDao biodataCalondao = DaoFactory.getInstance()
											.getBiodataCalonMahasiswaDao();
									// agamaDao.beginTransaction();
									biodataCalondao.delete(biodataCalondao.merge(biodataCalonMahasiswa));
									// agamaDao.commitTransaction();
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
			toolbar.setParent(arg0);
		}

	}

	public void onAdd(Event event) throws Exception {
		init(new BiodataCalonMahasiswa());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(BiodataCalonMahasiswa biodataCalonMahasiswa) {
		this.biodataCalonMahasiswa = biodataCalonMahasiswa;
		addWindow.setTitle(biodataCalonMahasiswa.getId() == null ? "Tambah Calon PMDK" : "Ubah Calon PMDK");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama "));
		row.appendChild(
				nama = new Textbox(biodataCalonMahasiswa.getNama() == null ? "" : biodataCalonMahasiswa.getNama()));
		nama.setWidth("90%");
//		nama.setConstraint("no empty");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Lahir"));
		row.appendChild(tanggalLahir = new MyDatebox(biodataCalonMahasiswa.getTanggalLahir() == null ? ais.ui.util.WaktuUtil.getDate()
				: biodataCalonMahasiswa.getTanggalLahir()));
//		tanggalLahir.setConstraint("no empty");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Sekolah"));
		Common.selectComboItem(jenisSekolah, biodataCalonMahasiswa.getJenisSekolah());
		row.appendChild(jenisSekolah);
		jenisSekolah.setWidth("90%");

		@SuppressWarnings("unchecked")
		final List<String> asalsekolahs = HibernateUtil.currentSession().createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.eq("tahun", ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR)))
				.setProjection(Projections.property("asalSma")).list();
		System.out.println(asalsekolahs.size() + "size");
		Set<String> setsample = new HashSet<String>(asalsekolahs);
		System.out.println(setsample.size() + "size");
		asalSekolah = new Combobox();
		MyComboitemConfig comboitem;
		for (String asalskl : setsample) {
			comboitem = new MyComboitemConfig(asalskl);
			comboitem.setValue(asalskl);
			asalSekolah.appendChild(comboitem);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Asal Sekolah"));
		row.appendChild(asalSekolah);
		// asalSekolah.setAttribute("sekolah", asalSekolah.getRawText());
		// row.appendChild(asalSekolah = new Textbox(biodataCalonMahasiswa
		// .getAsalSma() == null ? "" : biodataCalonMahasiswa.getAsalSma()));
		Common.selectComboItem(asalSekolah,
				biodataCalonMahasiswa.getAsalSma() == null ? null : biodataCalonMahasiswa.getAsalSma());
		asalSekolah.setWidth("90%");
		// asalSekolah.addEventListener("onChange", new EventListener() {
		//
		// @Override
		// public void onEvent(Event arg0) throws Exception {
		// // TODO Auto-generated method stub
		// String sekolah = (String) asalSekolah.getStringCellValue();
		// // System.out.println(sekolah);
		// // sekolah = asalSekolah.getRawText();
		// // System.out.println(sekolah);
		// for (String asalskl : asalsekolahs) {
		// if (asalskl.trim().equals(sekolah.trim())) {
		// Common.selectComboItem(asalSekolah, asalskl);
		// }
		// }
		// }
		// });

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("No. Induk Siswa"));
		row.appendChild(nis = new Textbox(
				biodataCalonMahasiswa.getNoIdentitas() == null ? "" : biodataCalonMahasiswa.getNoIdentitas()));
		nis.setWidth("90%");

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
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Nama belum diisi. Langkah yang dapat dilakukan: (1) isi kolom Nama dengan nama lengkap yang benar; (2) pastikan kolom tidak kosong atau hanya spasi; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		BiodataCalonMahasiswaDao biodataCalonMahasiswaDao = DaoFactory.getInstance().getBiodataCalonMahasiswaDao();
		if (biodataCalonMahasiswa.getId() != null) {
			biodataCalonMahasiswa = biodataCalonMahasiswaDao.load(biodataCalonMahasiswa.getId());

		}

		Session session = HibernateUtil.currentSession();
		JenisSeleksi jenisSeleksi = (JenisSeleksi) session.createCriteria(JenisSeleksi.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.eq("nama", "PMDK")).uniqueResult();
		Jenjang jenjang = (Jenjang) session.createCriteria(Jenjang.class)
				// .add(Restrictions.eq("nama", "S1"))
				.add(Restrictions.idEq(1L)).uniqueResult();
		JenisKartuIdentitasMahasiswaBaru kartu = (JenisKartuIdentitasMahasiswaBaru) session
				.createCriteria(JenisKartuIdentitasMahasiswaBaru.class).add(Restrictions.idEq(1L)).uniqueResult();
		// Program program = (Program) session.createCriteria(Program.class)
		// .add(Restrictions.eq("nama", "Reguler")).uniqueResult();
		// System.out.println(program +" c");

		biodataCalonMahasiswa.setNama(nama.getValue().toUpperCase());
		biodataCalonMahasiswa.setTanggalLahir(tanggalLahir.getValue());
		biodataCalonMahasiswa.setJenisSeleksi(jenisSeleksi);
		// biodataCalonMahasiswa.setAsalSma(asalSekolah.getValue().toUpperCase());
		// biodataCalonMahasiswa.setAsalSma(asalSekolah.getRawText());

		@SuppressWarnings("unchecked")
		final List<String> asalsekolahs = HibernateUtil.currentSession().createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.eq("tahun", ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR)))
				.setProjection(Projections.property("asalSma")).list();
		Set<String> setsample = new HashSet<String>(asalsekolahs);
		// System.out.println(setsample.size() + "size");
		String sekolah = (String) asalSekolah.getValue();
		for (String asalskl : setsample) {
			if (asalskl.trim().equals(sekolah.trim())) {
				biodataCalonMahasiswa.setAsalSma(asalskl);
			} else {
				biodataCalonMahasiswa.setAsalSma(sekolah);
			}
		}
		biodataCalonMahasiswa.setJenjang(jenjang);
		biodataCalonMahasiswa.setNoIdentitas(nis.getValue());
		biodataCalonMahasiswa.setJenisKartuIdentitas(kartu);
		biodataCalonMahasiswa.setJenisSekolah((JenisSekolahMahasiswaBaru) jenisSekolah.getSelectedItem().getValue());

		biodataCalonMahasiswa.setProgram("Reguler");

		if (biodataCalonMahasiswa.getId() != null) {
			biodataCalonMahasiswaDao.update(biodataCalonMahasiswa);
		} else {
			String noReg = CommonPMB.generateNoRegistrasi(biodataCalonMahasiswa);
			biodataCalonMahasiswa.setNoRegistrasi(noReg);
			biodataCalonMahasiswa.setNoUjian(noReg);
			biodataCalonMahasiswa.setKewarganegaraan(Mahasiswa.WNI);
			biodataCalonMahasiswaDao.save(biodataCalonMahasiswa);
		}

		return true;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Session session = HibernateUtil.currentSession();
		JenisSeleksi jenisSeleksi = (JenisSeleksi) session.createCriteria(JenisSeleksi.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.eq("nama", "PMDK")).uniqueResult();
		List<BiodataCalonMahasiswa> biodataCalonMahasiswa = session.createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.addOrder(Order.asc("nama")).add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true") : Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))
				.add(searchtahun.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("tahun", searchtahun.getSelectedItem().getValue()))
				.add(Restrictions.eq("jenisSeleksi", jenisSeleksi))
				.add(Restrictions.ilike("asalSma", searchsekolah.getValue(), MatchMode.ANYWHERE))
				.setMaxResults(Common.MAX_RESULT).list();
		ListModel strset = new SimpleListModel(biodataCalonMahasiswa);
		grid.setRowRenderer(new BiodataCalonRenderer());
		grid.setModelCheckMobile(strset);

		jumlahPendaftar.setValue("Jumlah : " + biodataCalonMahasiswa.size());

	}

}
