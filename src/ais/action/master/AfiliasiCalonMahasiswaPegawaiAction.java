package ais.action.master;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.A;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.pmb.AfiliasiCalonMahasiswaPegawaiDetailAction;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.Pegawai;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataCriteriaWithColumn;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyToolbarbuttonConfig;

public class AfiliasiCalonMahasiswaPegawaiAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;

	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Textbox searchcalon;
	private Checkbox searchaktif;
	private Combobox searchTahunAjaran;

	private MyToolbarbuttonConfig add;

	public static String[] contents = new String[] { "afiliasiPegawai.nama", "noRegistrasi", "noUjian", "nama",
			"totalSkor", "alamat", "rt", "rw", "kelurahanCalon", "kecamatanCalon", "kotaCalon", "propinsiCalon",
			"namaSekolahAsal", "namaSekolahAsal.kode",

			// ----

			"pembayaranRegistrasi", "pembayaranDaftarUlang", "kodePos", "tempatLahir", "tanggalLahir", "jenisKelamin",
			"asalNegara", "kewarganegaraan", "jenisKartuIdentitas", "noIdentitas", "email", "nisn", "jenisSekolah",
			"akreditasiSekolah", "kodePosSekolah", "kecamatanSekolah", "kotaSekolah", "propinsiSekolah",
			"tahunKelulusan", "jurusanSekolah", "jurusanSekolahLain", "namaWali", "noTelpOrtu", "pendapatanOrtu",
			"pendidikanOrtu", "alamatOrtu", "rtOrtu", "rwOrtu", "kodePosOrtu", "kecamatanOrtu", "kelurahanOrtu",
			"propinsiOrtu", "kotaOrtu", "paket", "prodi1", "prodi2", "prodi3", "prodi4", "prodi5", "jenjang",
			"statusLulus", "prodiLulus", "nimGenerated", "cetakKartu", "program", "jenisSeleksi", "tanggalDaftar",
			"tahun", "semesterMulai", "tahunAkademik", "gelombangPendaftaran", "tanggalPendaftaran", "agama",
			"semesterMulai", "program", "hp", "namaAyah", "pendidikanAyah", "pekerjaanAyah", "namaIbu", "pendidikanIbu",
			"pekerjaanIbu", "namaUntukIjazah", "noIjazah", "ukuranJaket", "tinggiBadan", "pernahMenetapDiLuarNegeri",
			"beratBadan", "teleponRumah", "suratIzinMengemudi", "kendaraanKuliah", "pernahMemimpinOrganisasi",
			"namaOrganisasi", "hobi", "minatSeni", "kemampuanBahasa1", "kemampuanBahasa2", "kemampuanBahasa3",
			"asalSma", "alamatAsalSma", "asalSmp", "alamatAsalSmp", "asalSd", "alamatAsalSd", "golonganDarah",
			"statusNikah", "jenisKuliah", "statusPembayaran", "nim", "mahasiswa", "merupakanPindahan",
			"pindahanDariKampus", "pindahanDariProdi", "nimLamaSebelumPindah", "pindahDariKampusLamaDiSemester",
			"tanggalPindah", "keteranganPindah", "infoKampusDariMana", "namaTemanInfoKampusDariMana",
			"keteranganInfoKampusDariMana", "pinPassword", "parameterTambahan", "parameterTambahanInds",
			"tanggal_dirubah", "oleh", "keterangan", "telahLogin", "waktuLogin" };

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

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		Common.generateTahunAjaranDanSemua(searchTahunAjaran);
		String tahunAkademikPenerimaanMahasiswaBaru = Common
				.getKonfigurasi("tahunAkademikPenerimaanMahasiswaBaru", Common.getCurrentTahunAkademik()).getNilai();

		Common.selectComboItem(searchTahunAjaran, tahunAkademikPenerimaanMahasiswaBaru);

		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakDataCustomButton(BiodataCalonMahasiswa.class,
				new DataCriteria() {

					@SuppressWarnings("unchecked")
					@Override
					public Object initCriteria(boolean order) {
						List<Long> pegawais = AfiliasiCalonMahasiswaPegawaiAction.this.initCriteria(true)
								.setProjection(Projections.property("id")).list();
						Session session = HibernateUtil.currentSession();
						return session.createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

								.add(searchTahunAjaran.getSelectedItem() == null
										|| searchTahunAjaran.getSelectedItem().getValue() == null
												? Restrictions.sqlRestriction("true")
												: Restrictions.eq("tahunAkademik",
														searchTahunAjaran.getSelectedItem().getValue()))

								.createAlias("afiliasiPegawai", "afiliasiPegawai")
								.addOrder(Order.asc("afiliasiPegawai.nama")).addOrder(Order.asc("noRegistrasi"))
								.add(pegawais.isEmpty() ? Restrictions.sqlRestriction("false")
										: Restrictions.in("afiliasiPegawai.id", pegawais));
					}
				}, "Download Afiliasi Calon Mahasiswa", "/img/print.png", AfiliasiCalonMahasiswaPegawaiAction.contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		Common.appendKeToolbar(AfiliasiCalonMahasiswaAction.tampilkanSemuaDownload(searchTahunAjaran), add, comp);
	}

	class AfiliasiCalonMahasiswaRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Pegawai afiliasiPegawai = (Pegawai) arg1;

			(new AfiliasiCalonMahasiswaPegawaiDetailAction(afiliasiPegawai, searchTahunAjaran)).setParent(arg0);

			Vbox vbox = new Vbox();
			vbox.setParent(arg0);
			new MyLabelKecil(afiliasiPegawai.getMycode() == null ? "" : afiliasiPegawai.getMycode()).setParent(vbox);
			new MyLabelKecil(afiliasiPegawai.getCode() == null ? "" : afiliasiPegawai.getCode()).setParent(vbox);

			RevisiHelper.createNewRevisi(Pegawai.class, afiliasiPegawai, afiliasiPegawai.getNama()).setParent(arg0);

			CommonMedia.tampilkanGambarKecil(afiliasiPegawai).setParent(arg0);

			Session session = HibernateUtil.currentSession();
			Number d = (Number) session.createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(searchTahunAjaran.getSelectedItem() == null
							|| searchTahunAjaran.getSelectedItem().getValue() == null
									? Restrictions.sqlRestriction("true")
									: Restrictions.eq("tahunAkademik", searchTahunAjaran.getSelectedItem().getValue()))
					.setProjection(Projections.rowCount()).add(Restrictions.eq("afiliasiPegawai", afiliasiPegawai))
					.uniqueResult();

			A a;
			(a = new A(d == null ? "0" : Common.numberFormat.get().format(d.intValue()))).setParent(arg0);

			EventListener eventListener = new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					EventListener eventListener = (EventListener) Common
							.cetakDataCustomButton(BiodataCalonMahasiswa.class, new DataCriteriaWithColumn() {

								@Override
								public Object[] initCriteria(boolean order) {

									try {

										Session session = HibernateUtil.currentSession();
										Criteria criteria = session.createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
												.add(searchTahunAjaran.getSelectedItem() == null
														|| searchTahunAjaran.getSelectedItem().getValue() == null
																? Restrictions.sqlRestriction("true")
																: Restrictions.eq("tahunAkademik",
																		searchTahunAjaran.getSelectedItem().getValue()))
												.setProjection(Projections.rowCount())
												.add(Restrictions.eq("afiliasiPegawai", afiliasiPegawai));

										return new Object[] { criteria, AfiliasiCalonMahasiswaPegawaiAction.contents };

									} catch (Exception e) {
										Common.tampilErrorJikaAdmin(e);
									}
									return null;
								}

							}, null, "Download Data", "/img/print.png", null, null, false, null, "DATA TAMBAHAN",
									new String[] { "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
											"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
											"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
											"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
											"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
											"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
											"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
											"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
											"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "" })
							.getAttribute("eventListener");

					eventListener.onEvent(null);
				}
			};
			a.addEventListener("onClick", eventListener);

			new Label(afiliasiPegawai.getKeterangan()).setParent(arg0);

		}

	}

	@SuppressWarnings("unchecked")
	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();

		List<Long> ids = session.createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(searchTahunAjaran.getSelectedItem() == null
						|| searchTahunAjaran.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("true")
								: Restrictions.eq("tahunAkademik", searchTahunAjaran.getSelectedItem().getValue()))
				.add(Restrictions.isNotNull("afiliasiPegawai"))
				.add(searchcalon.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.or(
								Restrictions.ilike("noRegistrasi", searchcalon.getValue().trim(), MatchMode.ANYWHERE),
								Restrictions.ilike("nama", searchcalon.getValue().trim(), MatchMode.ANYWHERE)))
				.setProjection(Projections.groupProperty("afiliasiPegawai.id")).list();

		Criteria criteria = session.createCriteria(Pegawai.class)
				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"));
		criteria.add(ids.isEmpty() ? Restrictions.sqlRestriction("false") : Restrictions.in("id", ids));
		if (order)
			criteria.addOrder(Order.asc("nama"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<Pegawai> afiliasiCalonMahasiswa = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(afiliasiCalonMahasiswa);
		grid.setRowRenderer(new AfiliasiCalonMahasiswaRenderer());
		grid.setModelCheckMobile(strset);

	}

}
