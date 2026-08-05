package ais.action.master.pmb.statistik;

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

import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.GelombangPendaftaran;
import ais.database.model.JenisSeleksi;
import ais.database.model.Kegiatan;
import ais.database.model.Paket;
import ais.database.model.RuangPMB;
import ais.database.model.RuangPaketPMB;

public class RekapPendaftarSpmbPerPaket extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3173385938131248092L;

	private MyGrid grid;
	private Combobox jenisseleksisearch;
	private Combobox searchTahunAjaran;
	private Combobox searchGelombang;
	Label labelJumlahPendaftar;
	Label labelJumlahRuang;
	Label labelJumlahRuangPenuh;
	
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

		Common.initLaguage();
		if (session.getAttribute("usersTemp") == null) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		Common.insertCombo(jenisseleksisearch, "nama", JenisSeleksi.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		String tahunAkademikPenerimaanMahasiswaBaru = Common
				.getKonfigurasi("tahunAkademikPenerimaanMahasiswaBaru", Common.getCurrentTahunAkademik()).getNilai();

		Common.generateTahunAjaranDanSemua(searchTahunAjaran);Common.selectComboItem(searchTahunAjaran, Common.getCurrentTahunAkademik());

		Common.selectComboItem(searchTahunAjaran, tahunAkademikPenerimaanMahasiswaBaru);

		EventListener gelombangEventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.insertCombo(searchGelombang, "nama", "tahunAkademik", GelombangPendaftaran.class,
						Restrictions.and(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
								searchTahunAjaran.getSelectedItem() == null || searchTahunAjaran.getSelectedItem().getValue() == null  ? Restrictions.sqlRestriction("true")
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
		// List<Date> biodataCalonMahasiswas = session.createCriteria(
		// BiodataCalonMahasiswa.class).addOrder(
		// Order.asc("tanggalDaftar"))
		//
		// .setProjection(Projections.groupProperty("tanggalDaftar")).add(
		// jenisseleksisearch.getSelectedItem() == null ? Restrictions
		// .sqlRestriction("1=1") : Restrictions.eq(
		// "jenisSeleksi", jenisseleksisearch.getSelectedItem()
		// .getValue()))
		//
		// .add(
		// tahunsearch.getSelectedItem() == null ? Restrictions
		// .sqlRestriction("1=1") : Restrictions.eq("tahun",
		// tahunsearch.getSelectedItem().getValue()))
		//
		// .add(
		// jenjangsearch.getSelectedItem() == null ? Restrictions
		// .sqlRestriction("1=1") : Restrictions.eq("jenjang",
		// jenjangsearch.getSelectedItem().getValue()))
		// // ConstantValues.s1))

		List<Paket> pakets = session.createCriteria(Paket.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.addOrder(Order.asc("id"))

		.list();

		Integer kapasitasGedung = 0;
		List<RuangPMB> ruangPMBs = session.createCriteria(RuangPMB.class).createAlias("ujianPMB", "ujianPMB")
				.add(searchTahunAjaran.getSelectedItem() == null || searchTahunAjaran.getSelectedItem().getValue() == null  ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("tahunAkademik", searchTahunAjaran.getSelectedItem().getValue()))
				.add(searchGelombang.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("ujianPMB.gelombangPendaftaran",
								searchGelombang.getSelectedItem().getValue()))

		.list();

		List<RuangPMB> ruangPMBPenuh = session.createCriteria(RuangPMB.class).add(Restrictions.eq("penuh", 1))
				.createAlias("ujianPMB", "ujianPMB")
				.add(searchTahunAjaran.getSelectedItem() == null || searchTahunAjaran.getSelectedItem().getValue() == null  ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("tahunAkademik", searchTahunAjaran.getSelectedItem().getValue()))
				.add(searchGelombang.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("ujianPMB.gelombangPendaftaran",
								searchGelombang.getSelectedItem().getValue()))
				.list();

		for (RuangPMB r : ruangPMBs) {
			// Kapasitas ruangan bisa null (belum diisi) → hindari NPE saat unboxing Integer.
			Integer kap = r == null ? null : r.getKapasitasRuangan();
			if (kap != null) {
				kapasitasGedung += kap.intValue();
			}
		}

		// Guard label (auto-wire bisa null bila id tidak ada di ZUL yang ter-include).
		if (labelJumlahPendaftar != null) {
			labelJumlahPendaftar.setValue("Kapasitas Total Gedung Untuk Ujian : " + kapasitasGedung);
		}
		if (labelJumlahRuang != null) {
			labelJumlahRuang.setValue("Jumlah Ruang Total : " + ruangPMBs.size());
		}
		if (labelJumlahRuangPenuh != null) {
			labelJumlahRuangPenuh.setValue("Jumlah Ruang Penuh : " + ruangPMBPenuh.size());
		}

		ListModel strset = new SimpleListModel(pakets);
		grid.setRowRenderer(new BiodataCalonRenderer());
		grid.setModelCheckMobile(strset);

	}

	class BiodataCalonRenderer extends ais.ui.util.MyRowRenderer {

		@SuppressWarnings("unchecked")
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {arg0.setValign("top");
			// TODO Auto-generated method stub
			// final Date biodataCalonMahasiswa = (Date) arg1;
			Paket paket = (Paket) arg1;

			new Label(paket.getNama() + " / " + paket.getKeterangan()).setParent(arg0);
			Session session = HibernateUtil.currentSession();
			Number count = (Number) (session.createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.setProjection(Projections.rowCount()).add(Restrictions.eq("paket", paket))
					.add(jenisseleksisearch.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("jenisSeleksi", jenisseleksisearch.getSelectedItem().getValue()))

			.add(searchTahunAjaran.getSelectedItem() == null || searchTahunAjaran.getSelectedItem().getValue() == null  ? Restrictions.sqlRestriction("1=1")
					: Restrictions.eq("tahunAkademik", searchTahunAjaran.getSelectedItem().getValue()))
					.add(searchGelombang.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("gelombangPendaftaran", searchGelombang.getSelectedItem().getValue()))

			.uniqueResult());
			new Label(count + "").setParent(arg0);

			Integer countBayar = ((Number) session.createCriteria(Kegiatan.class).add(Restrictions.eq("aktif", true)).add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif"))).setProjection(Projections.rowCount())
					.createCriteria("calonMahasiswa").add(Restrictions.eq("paket", paket))
					.add(jenisseleksisearch.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("jenisSeleksi", jenisseleksisearch.getSelectedItem().getValue()))

			.add(searchTahunAjaran.getSelectedItem() == null || searchTahunAjaran.getSelectedItem().getValue() == null  ? Restrictions.sqlRestriction("1=1")
					: Restrictions.eq("tahunAkademik", searchTahunAjaran.getSelectedItem().getValue()))
					.add(searchGelombang.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("gelombangPendaftaran", searchGelombang.getSelectedItem().getValue()))

			.uniqueResult()).intValue();
			new Label(countBayar + "").setParent(arg0);

			Integer countUjian = ((Number) session.createCriteria(RuangPaketPMB.class)
					.setProjection(Projections.rowCount()).createCriteria("biodataCalonMahasiswa")
					.add(Restrictions.ne("noUjian", "")).add(Restrictions.isNotNull("noUjian"))
					.add(Restrictions.eq("paket", paket))
					.add(jenisseleksisearch.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("jenisSeleksi", jenisseleksisearch.getSelectedItem().getValue()))

			.add(searchTahunAjaran.getSelectedItem() == null || searchTahunAjaran.getSelectedItem().getValue() == null  ? Restrictions.sqlRestriction("1=1")
					: Restrictions.eq("tahunAkademik", searchTahunAjaran.getSelectedItem().getValue()))
					.add(searchGelombang.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("gelombangPendaftaran", searchGelombang.getSelectedItem().getValue()))

			.uniqueResult()).intValue();

			new Label(countUjian + "").setParent(arg0);

			List<RuangPMB> ruangPMBs = session.createCriteria(RuangPMB.class).createAlias("ujianPMB", "ujianPMB")
					// FIX QueryException "could not resolve property jenisSeleksi of RuangPMB": properti jenisSeleksi
					// tidak ada di RuangPMB/UjianPMB; ia milik GelombangPendaftaran (RuangPMB.ujianPMB.gelombangPendaftaran).
					// LEFT JOIN agar ruang tanpa gelombang tidak terbuang saat filter jenisSeleksi tidak dipilih.
					.createAlias("ujianPMB.gelombangPendaftaran", "gelombangUjianRuang", org.hibernate.Criteria.LEFT_JOIN)
					.add(jenisseleksisearch.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("gelombangUjianRuang.jenisSeleksi", jenisseleksisearch.getSelectedItem().getValue()))

			.add(searchTahunAjaran.getSelectedItem() == null || searchTahunAjaran.getSelectedItem().getValue() == null  ? Restrictions.sqlRestriction("1=1")
					: Restrictions.eq("tahunAkademik", searchTahunAjaran.getSelectedItem().getValue()))
					.add(searchGelombang.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("ujianPMB.gelombangPendaftaran",
									searchGelombang.getSelectedItem().getValue()))

			.add(Restrictions.eq("paket", paket)).list();
			Integer jumlahRuangan = ruangPMBs.size();
			Integer jumlahKursi = 0;
			for (RuangPMB r : ruangPMBs) {
				jumlahKursi += r.getKapasitasRuangan();
			}
			Integer kekurangan = 0;
			if (countBayar > jumlahKursi) {
				kekurangan = countBayar - jumlahKursi;
			}
			new Label(jumlahRuangan + " Ruangan / " + jumlahKursi + " Kursi (Kekurangan " + kekurangan + " Kursi)")
					.setParent(arg0);

		}

	}
}
