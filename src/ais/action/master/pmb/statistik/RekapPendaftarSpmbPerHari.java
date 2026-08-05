package ais.action.master.pmb.statistik;

import java.util.Date;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.UIUtil;

import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Toolbarbutton;

import ais.action.ws.util.ConstantUtil;
import ais.common.Common;
import ais.common.CommonPMB;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.GelombangPendaftaran;
import ais.database.model.JenisKegiatan;
import ais.database.model.JenisSeleksi;
import ais.database.model.Kegiatan;
import ais.database.model.Perkuliahan;
import ais.ui.util.MyComboitemConfig;

public class RekapPendaftarSpmbPerHari extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3173385938131248092L;

	private MyGrid grid;

	private Combobox jenisseleksisearch;
	private Combobox searchTahunAjaran;
	protected Combobox searchJenisSemester;
	private Combobox searchGelombang;

	private Label labelJumlahPendaftar;
	private Label labelJumlahBayar;
	private Label labelJumlahBayar1;
	private Label labelJumlahPeserta;

	Integer jumlahPendaftar;
	Integer jumlahPembayar;
	Integer jumlahPeserta;
	
	private MyToolbarbuttonConfig find;

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

		org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
		comboitem.setLabel(Perkuliahan.GANJIL);
		comboitem.setValue(Perkuliahan.GANJIL);
		searchJenisSemester.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		comboitem.setLabel(Perkuliahan.GENAP);
		comboitem.setValue(Perkuliahan.GENAP);
		searchJenisSemester.appendChild(comboitem);

		Common.selectComboItem(searchJenisSemester,
				Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP);
		searchJenisSemester.setReadonly(true);

		Common.insertCombo(jenisseleksisearch, "nama", JenisSeleksi.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		String tahunAkademikPenerimaanMahasiswaBaru = Common
				.getKonfigurasi("tahunAkademikPenerimaanMahasiswaBaru", Common.getCurrentTahunAkademik()).getNilai();

		Common.generateTahunAjaranDanSemua(searchTahunAjaran);
		Common.selectComboItem(searchTahunAjaran, Common.getCurrentTahunAkademik());

		Common.selectComboItem(searchTahunAjaran, tahunAkademikPenerimaanMahasiswaBaru);

		EventListener gelombangEventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.insertCombo(searchGelombang, "nama", "tahunAkademik", GelombangPendaftaran.class,
						Restrictions.and(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
								searchTahunAjaran.getSelectedItem() == null
										|| searchTahunAjaran.getSelectedItem().getValue() == null
												? Restrictions.sqlRestriction("true")
												: Restrictions.eq("tahunAkademik",
														searchTahunAjaran.getSelectedItem().getValue())));
			}
		};

		gelombangEventListener.onEvent(null);
		searchTahunAjaran.addEventListener("onChange", gelombangEventListener);

		onSearchDefault(null);
		
		
		if (find != null) {
			MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Download", "/img/print.png");
			toolbarbutton.setParent(find.getParent());
			toolbarbutton.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					UIUtil.downloadGrid(grid);
				}
			});
		}
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Session session = HibernateUtil.currentSession();
		List<Date> biodataCalonMahasiswas = session.createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.addOrder(Order.asc("tanggalDaftar"))

				.setProjection(Projections.groupProperty("tanggalDaftar"))
				.add(jenisseleksisearch.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("jenisSeleksi", jenisseleksisearch.getSelectedItem().getValue()))

				.add(searchTahunAjaran.getSelectedItem() == null
						|| searchTahunAjaran.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("tahunAkademik", searchTahunAjaran.getSelectedItem().getValue()))
				.add(searchGelombang.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("gelombangPendaftaran", searchGelombang.getSelectedItem().getValue()))

				.list();
		ListModel strset = new SimpleListModel(biodataCalonMahasiswas);
		grid.setRowRenderer(new BiodataCalonRenderer());
		grid.setModelCheckMobile(strset);

		if (biodataCalonMahasiswas.size() == 0) {
			jumlahPembayar = 0;
			jumlahPendaftar = 0;
			jumlahPeserta = 0;
			labelJumlahPendaftar.setValue("Total Pendaftar : " + jumlahPendaftar);
			labelJumlahBayar.setValue("Total Pendaftar yang melakukan pembayaran : " + jumlahPembayar);
			labelJumlahPeserta.setValue("Total Peserta Ujian : " + jumlahPeserta);
		}

	}

	class BiodataCalonRenderer extends ais.ui.util.MyRowRenderer {

		Integer jumlahPendaftar = 0;
		Integer jumlahPembayar = 0;
		Integer jumlahPembayar1 = 0;
		Integer jumlahPeserta = 0;

		private JenisKegiatan jenisKegiatanReg = CommonPMB.pembayaranUtil
				.generateJenisKegiatan(ConstantUtil.PENDAFTARAN_CALON_MAHASISWA);
		private JenisKegiatan jenisKegiatanUlang = CommonPMB.pembayaranUtil
				.generateJenisKegiatan(ConstantUtil.PENDAFTARAN_ULANG_MAHASISWA_BARU);

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {arg0.setValign("top");
			// TODO Auto-generated method stub
			final Date biodataCalonMahasiswa = (Date) arg1;
			new Label(Common.dateFormat2.get().format(biodataCalonMahasiswa)).setParent(arg0);
			Session session = HibernateUtil.currentSession();

			Number jumlah = (Number) session.createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.setProjection(Projections.rowCount())
					.add(Restrictions.eq("semesterMulai", searchJenisSemester.getSelectedItem().getValue()))
					.add(Restrictions.eq("tanggalDaftar", biodataCalonMahasiswa))
					.add(jenisseleksisearch.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("jenisSeleksi", jenisseleksisearch.getSelectedItem().getValue()))

					.add(searchTahunAjaran.getSelectedItem() == null
							|| searchTahunAjaran.getSelectedItem().getValue() == null
									? Restrictions.sqlRestriction("1=1")
									: Restrictions.eq("tahunAkademik", searchTahunAjaran.getSelectedItem().getValue()))
					.add(searchGelombang.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("gelombangPendaftaran", searchGelombang.getSelectedItem().getValue()))

					.uniqueResult();
			new Label(jumlah + "").setParent(arg0);
			jumlahPendaftar += jumlah.intValue();

			Number jumlahBayar = (Number) session.createCriteria(Kegiatan.class).add(Restrictions.eq("aktif", true)).add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")))
					.add(Restrictions.eq("jenisKegiatan", jenisKegiatanReg))

					.setProjection(Projections.rowCount())

					.createCriteria("calonMahasiswa")
					.add(Restrictions.eq("semesterMulai", searchJenisSemester.getSelectedItem().getValue()))
					.add(Restrictions.eq("tanggalDaftar", biodataCalonMahasiswa))

					.add(jenisseleksisearch.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("jenisSeleksi", jenisseleksisearch.getSelectedItem().getValue()))

					.add(searchTahunAjaran.getSelectedItem() == null
							|| searchTahunAjaran.getSelectedItem().getValue() == null
									? Restrictions.sqlRestriction("1=1")
									: Restrictions.eq("tahunAkademik", searchTahunAjaran.getSelectedItem().getValue()))
					.add(searchGelombang.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("gelombangPendaftaran", searchGelombang.getSelectedItem().getValue()))

					.uniqueResult();
			new Label(jumlahBayar + "").setParent(arg0);
			jumlahPembayar += jumlahBayar.intValue();

			jumlahBayar = (Number) session.createCriteria(Kegiatan.class).add(Restrictions.eq("aktif", true)).add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")))
					.add(Restrictions.eq("jenisKegiatan", jenisKegiatanUlang))

					.setProjection(Projections.rowCount())

					.createCriteria("calonMahasiswa")
					.add(Restrictions.eq("semesterMulai", searchJenisSemester.getSelectedItem().getValue()))
					.add(Restrictions.eq("tanggalDaftar", biodataCalonMahasiswa))

					.add(jenisseleksisearch.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("jenisSeleksi", jenisseleksisearch.getSelectedItem().getValue()))

					.add(searchTahunAjaran.getSelectedItem() == null
							|| searchTahunAjaran.getSelectedItem().getValue() == null
									? Restrictions.sqlRestriction("1=1")
									: Restrictions.eq("tahunAkademik", searchTahunAjaran.getSelectedItem().getValue()))
					.add(searchGelombang.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("gelombangPendaftaran", searchGelombang.getSelectedItem().getValue()))

					.uniqueResult();
			new Label(jumlahBayar + "").setParent(arg0);
			jumlahPembayar1 += jumlahBayar.intValue();

			Number jumlahPesertaUjian = (Number) session.createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.setProjection(Projections.rowCount())
					.add(Restrictions.eq("semesterMulai", searchJenisSemester.getSelectedItem().getValue()))
					.add(Restrictions.eq("tanggalDaftar", biodataCalonMahasiswa)).add(Restrictions.ne("noUjian", ""))
					.add(Restrictions.isNotNull("noUjian"))
					.add(jenisseleksisearch.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("jenisSeleksi", jenisseleksisearch.getSelectedItem().getValue()))

					.add(searchTahunAjaran.getSelectedItem() == null
							|| searchTahunAjaran.getSelectedItem().getValue() == null
									? Restrictions.sqlRestriction("1=1")
									: Restrictions.eq("tahunAkademik", searchTahunAjaran.getSelectedItem().getValue()))
					.add(searchGelombang.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("gelombangPendaftaran", searchGelombang.getSelectedItem().getValue()))

					.uniqueResult();
			new Label(jumlahPesertaUjian.intValue() == 0 ? "0" : jumlahPesertaUjian.toString()).setParent(arg0);
			jumlahPeserta += jumlahPesertaUjian.intValue();

			labelJumlahPendaftar.setValue("Total Pendaftar : " + jumlahPendaftar);
			labelJumlahBayar.setValue("Total pembayaran reg : " + jumlahPembayar);
			labelJumlahBayar1.setValue("Total pembayaran daftar ulang: " + jumlahPembayar1);
			labelJumlahPeserta.setValue("Total Peserta Ujian : " + jumlahPeserta);

		}

	}

}
