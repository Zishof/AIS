package ais.action.master;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Set;

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
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.CommonOnSearchdefault;
import ais.common.CommonPrivilages;
import ais.database.dao.DaoFactory;
import ais.database.dao.PegawaiDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataPegawai;
import ais.database.model.Pegawai;
import ais.database.model.StatusPegawai;
import ais.database.model.Tbmuser;
import ais.database.model.rab.SatuanKerja;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Controller/action ZK untuk pegawai simple. Tipe ini merupakan titik masuk UI yang menghubungkan
 * event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyGrid grid}, {@code Paging paging},
 * {@code Textbox searchcode}, {@code Textbox searchnama}, {@code Combobox searchstatus}, {@code
 * AmbilDataSatuanKerjaBanbox searchparent}, {@code MyToolbarbuttonConfig add}, {@code boolean edit};
 * inisialisasi/lifecycle ({@code doBeforeCompose()}, {@code doAfterCompose()}, {@code initCriteria()});
 * pembacaan/pencarian ({@code getKeterangan()}, {@code onSearchDefault()}); operasi domain lain ({@code main()},
 * {@code onAdd()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di
 * atas.</p>
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
public class PegawaiSimpleAction extends GenericAutowireComposer implements CommonOnSearchdefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3786091220301468178L;
	private MyGrid grid;
	private Paging paging;
	private Textbox searchcode;
	private Textbox searchnama;
	private Combobox searchstatus;
	private AmbilDataSatuanKerjaBanbox searchparent;
	private MyToolbarbuttonConfig add;

	private boolean edit = false;
	private boolean delete = false;
	private boolean approve = false;
	private boolean reject = false;
	private BiodataPegawaiSimpleAction biodataPegawaiAction;

	private SatuanKerja satuanKerjaOnSession;
	private SatuanKerjaTreeModel satuanKerjaTreeModel;

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
		// Common.prosesDosen();

		Common.insertComboDanSemua(searchstatus, "nama", StatusPegawai.class, Restrictions.eq("aktif", true));

		satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);

		if (session.getAttribute("satuanKerjaOnSession") != null) {
			satuanKerjaOnSession = (SatuanKerja) session.getAttribute("satuanKerjaOnSession");
			session.removeAttribute("satuanKerjaOnSession");
		}

		SatuanKerja satuanKerjaData = satuanKerjaOnSession;
		Tbmuser tbmuser = Common.getCurrentUser();
		if (satuanKerjaData != null && tbmuser != null && tbmuser.hakAkses() != null
				&& !tbmuser.hakAkses().getMelihatDataSatkerLain()) {
			searchparent.setValue(satuanKerjaData.getNama());
			searchparent.setAttribute("satuanKerja", satuanKerjaData);
			searchparent.setAttribute("myValue", satuanKerjaData);
			searchparent.setDisabled(true);
		}

		searchparent.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		approve = CommonPrivilages.checkPrevilages(CommonPrivilages.APPROVE);
		reject = CommonPrivilages.checkPrevilages(CommonPrivilages.REJECT);

		onSearchDefault(null);

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});
	}

	public static void main(String[] argv) {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmssSSS");
		System.out.println("dateFormat = " + dateFormat.format(ais.ui.util.WaktuUtil.getDate()));

	}

	class PegawaiRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Pegawai pegawai = (Pegawai) arg1;

			CommonMedia.tampilkanGambarKecil(pegawai).setParent(arg0);

			new Label(pegawai.getMycode() == null ? "" : pegawai.getMycode()).setParent(arg0);

			RevisiHelper
					.createNewRevisi(Pegawai.class, pegawai,
							pegawai.getDosen() == null ? pegawai.getNama() : pegawai.getDosen().getNama())
					.setParent(arg0);

			// new ais.ui.util.MyHtml("<font>" + gaji +
			// "</font>")
			// .setParent(arg0);

			new Label(pegawai.getEmail()).setParent(arg0);
			// new Label(pegawai.getTanggalmasuk() == null ? ""
			// : Common.dateFormat1.get().format(pegawai.getTanggalmasuk()))
			// .setParent(arg0);

			// new ais.ui.util.MyHtml("<font>"
			// + (pegawai.getJabatanStruktural() == null ? "" : pegawai
			// .getJabatanStruktural().getNama() + "<br>")
			// + (pegawai.getJabatanFungsional() == null ? "" : pegawai
			// .getJabatanFungsional().getNama()) + "</font>")
			// .setParent(arg0);

			new Label(pegawai.getSatuanKerja() == null ? "" : pegawai.getSatuanKerja().getNama()).setParent(arg0);

			// String ket = "";
			// ket += getKeterangan(pegawai, RiwayatKartuIdentitasPegawai.class,
			// "Kartu Identitas") + "<br>";
			// ket += getKeterangan(pegawai, Keluarga.class, "Keluarga") +
			// "<br>";
			// ket += getKeterangan(pegawai, KenaikanPangkat.class,
			// "Pangkat/Jabatan") + "<br>";
			// ket += getKeterangan(pegawai, RiwayatPelatihanPegawai.class,
			// "Pelatihan") + "<br>";
			// ket += getKeterangan(pegawai, Seminar.class, "Seminar") + "<br>";
			// ket += getKeterangan(pegawai, RiwayatPendidikanPegawai.class,
			// "Pendidikan") + "<br>";
			// ket += getKeterangan(pegawai, RiwayatTandaJasaPegawai.class,
			// "Tanda Jasa") + "<br>";
			// ket += getKeterangan(pegawai, RiwayatKeluarNegeriPegawai.class,
			// "Keluar Negeri") + "<br>";
			// ket += getKeterangan(pegawai,
			// RiwayatOrganisasiSekolahPegawai.class, "Organisasi Sekolah")
			// + "<br>";
			// ket += getKeterangan(pegawai,
			// RiwayatOrganisasiKampusPegawai.class,
			// "Organisasi Kampus") + "<br>";
			// ket += getKeterangan(pegawai, RiwayatOrganisasiLainPegawai.class,
			// "Organisasi Lain") + "<br>";
			//
			// new ais.ui.util.MyHtml("<font>" + ket +
			// "</font>")
			// .setParent(arg0);

			// kebab popup (⋯) via UIHelper.buatBarisAksi — kolom aksi jadi kecil dan konsisten.
			final java.util.List<org.zkoss.zk.ui.Component> aksiButtons =
					new java.util.ArrayList<org.zkoss.zk.ui.Component>();

			Tbmuser tbmuser = Common.getCurrentUser();

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit
					|| (tbmuser.ambilFakultas() != null && pegawai.getFakultas() != null
							&& tbmuser.ambilFakultas().getId().equals(pegawai.getFakultas().getId()))
					|| (tbmuser.ambilJurusan() != null && pegawai.getJurusan() != null
							&& tbmuser.ambilJurusan().getId().equals(pegawai.getJurusan().getId())));
			button.setTooltiptext("Ubah data");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					biodataPegawaiAction = new BiodataPegawaiSimpleAction(pegawai);
					biodataPegawaiAction.setCommonOnSearchdefault(PegawaiSimpleAction.this);
					biodataPegawaiAction.setHeight("95%");
					biodataPegawaiAction.setWidth("90%");
					page.getFirstRoot().appendChild(biodataPegawaiAction);
					biodataPegawaiAction.setVisible(true);
					biodataPegawaiAction.onModal();
				}

			});
			aksiButtons.add(button);

			button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
			button.setTooltiptext("Hapus data");
			button.setVisible(delete
					|| (tbmuser.ambilFakultas() != null && pegawai.getFakultas() != null
							&& tbmuser.ambilFakultas().getId().equals(pegawai.getFakultas().getId()))
					|| (tbmuser.ambilJurusan() != null && pegawai.getJurusan() != null
							&& tbmuser.ambilJurusan().getId().equals(pegawai.getJurusan().getId())));
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
											PegawaiDao pegawaiDao = DaoFactory.getInstance().getPegawaiDao();

											String sql1 = "delete from log_user_actifity where detail_log_login in (select id from detail_log_login where log_login in (select id from log_login where pegawai in (select id from pegawai where id = "
													+ pegawai.getId() + ")));";
											pegawaiDao.getCurrentSession().createSQLQuery(sql1).executeUpdate();

											sql1 = "delete from log_user_actifity where detail_log_login in (select id from detail_log_login where log_login in (select id from log_login where pegawai in (select id from pegawai where id = "
													+ pegawai.getId() + ")));";
											pegawaiDao.getCurrentSession().createSQLQuery(sql1).executeUpdate();

											sql1 = "delete from detail_log_login where log_login in (select id from log_login where pegawai in (select id from pegawai where id = "
													+ pegawai.getId() + "));";
											pegawaiDao.getCurrentSession().createSQLQuery(sql1).executeUpdate();

											sql1 = "delete from log_login where pegawai in (select id from pegawai where id = "
													+ pegawai.getId() + ");";
											pegawaiDao.getCurrentSession().createSQLQuery(sql1).executeUpdate();

											sql1 = "delete from log_user_actifity where detail_log_login in (select id from detail_log_login where log_login in (select id from log_login where tbmuser in (select userid from tbmuser where pegawai = "
													+ pegawai.getId() + " )));";

											pegawaiDao.getCurrentSession().createSQLQuery(sql1).executeUpdate();

											sql1 = "delete from log_user_actifity where detail_log_login in (select id from detail_log_login where log_login in (select id from log_login where tbmuser in (select userid from tbmuser where pegawai = "
													+ pegawai.getId() + " )));";
											pegawaiDao.getCurrentSession().createSQLQuery(sql1).executeUpdate();

											sql1 = "delete from detail_log_login where log_login in (select id from log_login where tbmuser in (select userid from tbmuser where pegawai = "
													+ pegawai.getId() + " ));";
											pegawaiDao.getCurrentSession().createSQLQuery(sql1).executeUpdate();

											sql1 = "delete from log_login where tbmuser in (select userid from tbmuser where pegawai = "
													+ pegawai.getId() + " );";
											pegawaiDao.getCurrentSession().createSQLQuery(sql1).executeUpdate();

											Session session = HibernateUtil.currentSession();
											String sql = "delete from employ.riwayat_kartu_identitas_pegawai where status = false and pegawai = "
													+ pegawai.getId();
											session.createSQLQuery(sql).executeUpdate();

											sql = "delete from employ.keluarga where status = false and pegawai = "
													+ pegawai.getId();
											session.createSQLQuery(sql).executeUpdate();

											sql = "delete from employ.riwayat_keluar_negeri_pegawai where status = false and pegawai = "
													+ pegawai.getId();
											session.createSQLQuery(sql).executeUpdate();

											sql = "delete from employ.riwayat_keterangan_lain_pegawai where status = false and pegawai = "
													+ pegawai.getId();
											session.createSQLQuery(sql).executeUpdate();

											sql = "delete from employ.riwayat_organisasi_kampus_pegawai where status = false and pegawai = "
													+ pegawai.getId();
											session.createSQLQuery(sql).executeUpdate();

											sql = "delete from employ.riwayat_organisasi_lain_pegawai where status = false and pegawai = "
													+ pegawai.getId();
											session.createSQLQuery(sql).executeUpdate();

											sql = "delete from employ.riwayat_organisasi_sekolah_pegawai where status = false and pegawai = "
													+ pegawai.getId();
											session.createSQLQuery(sql).executeUpdate();

											sql = "delete from employ.riwayat_pelatihan_pegawai where status = false and pegawai = "
													+ pegawai.getId();
											session.createSQLQuery(sql).executeUpdate();

											sql = "delete from employ.riwayat_pendidikan_pegawai where status = false and pegawai = "
													+ pegawai.getId();
											session.createSQLQuery(sql).executeUpdate();

											sql = "delete from employ.riwayat_tanda_jasa_pegawai where status = false and pegawai = "
													+ pegawai.getId();
											session.createSQLQuery(sql).executeUpdate();

											sql = "delete from employ.kenaikan_pangkat where status = false and pegawai = "
													+ pegawai.getId();
											session.createSQLQuery(sql).executeUpdate();

											sql1 = "delete from tbmuser where pegawai = " + pegawai.getId() + ";";
											pegawaiDao.getCurrentSession().createSQLQuery(sql1).executeUpdate();

											BiodataPegawai biodataPegawai = (BiodataPegawai) pegawaiDao
													.getCurrentSession().createCriteria(BiodataPegawai.class)
													.add(Restrictions.eq("pegawai", pegawai)).setMaxResults(1)
													.uniqueResult();
											if (biodataPegawai != null) {
												pegawaiDao.getCurrentSession().delete(biodataPegawai);
											}

											Common.refreshDelete(pegawai);

											onSearchDefault(event);
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											MyMessageboxConfig.show(
													"Data ini tidak dapat dihapus .., karena terdapat relasi data yang sudah disetujui atau karena berelasi dengan data lainnya, error-nya adalah sbagai berikut:"
															+ e.getMessage());
										}

									}

								}
							});

				}
			});
			aksiButtons.add(button);

			button = new MyToolbarbuttonConfig("", "/img/svg/check2.svg");
			button.setVisible(approve);
			button.setTooltiptext("Setujui semua pengajuan");
			button.addEventListener("onClick", new EventListener() {
				@SuppressWarnings({})
				@Override
				public void onEvent(Event event) throws Exception {

					MyMessageboxConfig.show("Apakah yakin ingin mensetujui semua pengajuan pegawai ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {

										Session session = HibernateUtil.currentSession();
										String sql = "update employ.riwayat_kartu_identitas_pegawai set status = true where pegawai = "
												+ pegawai.getId();
										session.createSQLQuery(sql).executeUpdate();

										sql = "update employ.keluarga set status = true where pegawai = "
												+ pegawai.getId();
										session.createSQLQuery(sql).executeUpdate();

										sql = "update employ.riwayat_keluar_negeri_pegawai set status = true where pegawai = "
												+ pegawai.getId();
										session.createSQLQuery(sql).executeUpdate();

										sql = "update employ.riwayat_keterangan_lain_pegawai set status = true where pegawai = "
												+ pegawai.getId();
										session.createSQLQuery(sql).executeUpdate();

										sql = "update employ.riwayat_organisasi_kampus_pegawai set status = true where pegawai = "
												+ pegawai.getId();
										session.createSQLQuery(sql).executeUpdate();

										sql = "update employ.riwayat_organisasi_lain_pegawai set status = true where pegawai = "
												+ pegawai.getId();
										session.createSQLQuery(sql).executeUpdate();

										sql = "update employ.riwayat_organisasi_sekolah_pegawai set status = true where pegawai = "
												+ pegawai.getId();
										session.createSQLQuery(sql).executeUpdate();

										sql = "update employ.riwayat_pelatihan_pegawai set status = true where pegawai = "
												+ pegawai.getId();
										session.createSQLQuery(sql).executeUpdate();

										sql = "update employ.riwayat_pendidikan_pegawai set status = true where pegawai = "
												+ pegawai.getId();
										session.createSQLQuery(sql).executeUpdate();

										sql = "update employ.riwayat_tanda_jasa_pegawai set status = true where pegawai = "
												+ pegawai.getId();
										session.createSQLQuery(sql).executeUpdate();

										sql = "update employ.kenaikan_pangkat set status = true where pegawai = "
												+ pegawai.getId();
										session.createSQLQuery(sql).executeUpdate();

										onSearchDefault(event);

									}

								}
							});
				}

			});
			aksiButtons.add(button);

			button = new MyToolbarbuttonConfig("", "/img/svg/warning-outline.svg");
			button.setVisible(reject);
			button.setTooltiptext("Batalkan semua pengajuan");
			button.addEventListener("onClick", new EventListener() {
				@SuppressWarnings({})
				@Override
				public void onEvent(Event event) throws Exception {

					MyMessageboxConfig.show("Apakah yakin ingin membatalkan semua pengajuan pegawai ini ?",
							"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
							MyMessageboxConfig.QUESTION, new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {

										Session session = HibernateUtil.currentSession();
										String sql = "update employ.riwayat_kartu_identitas_pegawai set status = false where pegawai = "
												+ pegawai.getId();
										session.createSQLQuery(sql).executeUpdate();

										sql = "update employ.keluarga set status = false where pegawai = "
												+ pegawai.getId();
										session.createSQLQuery(sql).executeUpdate();

										sql = "update employ.riwayat_keluar_negeri_pegawai set status = false where pegawai = "
												+ pegawai.getId();
										session.createSQLQuery(sql).executeUpdate();

										sql = "update employ.riwayat_keterangan_lain_pegawai set status = false where pegawai = "
												+ pegawai.getId();
										session.createSQLQuery(sql).executeUpdate();

										sql = "update employ.riwayat_organisasi_kampus_pegawai set status = false where pegawai = "
												+ pegawai.getId();
										session.createSQLQuery(sql).executeUpdate();

										sql = "update employ.riwayat_organisasi_lain_pegawai set status = false where pegawai = "
												+ pegawai.getId();
										session.createSQLQuery(sql).executeUpdate();

										sql = "update employ.riwayat_organisasi_sekolah_pegawai set status = false where pegawai = "
												+ pegawai.getId();
										session.createSQLQuery(sql).executeUpdate();

										sql = "update employ.riwayat_pelatihan_pegawai set status = false where pegawai = "
												+ pegawai.getId();
										session.createSQLQuery(sql).executeUpdate();

										sql = "update employ.riwayat_pendidikan_pegawai set status = false where pegawai = "
												+ pegawai.getId();
										session.createSQLQuery(sql).executeUpdate();

										sql = "update employ.riwayat_tanda_jasa_pegawai set status = false where pegawai = "
												+ pegawai.getId();
										session.createSQLQuery(sql).executeUpdate();

										sql = "update employ.kenaikan_pangkat set status = false where pegawai = "
												+ pegawai.getId();
										session.createSQLQuery(sql).executeUpdate();

										onSearchDefault(event);

									}

								}
							});
				}

			});
			aksiButtons.add(button);

			ais.ui.util.UIHelper.buatBarisAksi(arg0, 3, aksiButtons);

		}
	}

	@SuppressWarnings("rawtypes")
	public String getKeterangan(Pegawai pegawai, Class clazz, String ket) {
		Session session = HibernateUtil.currentSession();

		Integer countDisetujuai = ((Number) session.createCriteria(clazz).add(Restrictions.eq("pegawai", pegawai))
				// .add(Restrictions.eq("status", true))
				.setProjection(Projections.rowCount()).uniqueResult()).intValue();

		Integer countBelumDisetujuai = ((Number) session.createCriteria(clazz).add(Restrictions.eq("pegawai", pegawai))
				// .add(Restrictions.eq("status", false))
				.setProjection(Projections.rowCount()).uniqueResult()).intValue();

		return ket + ": <font>" + Common.numberFormat.get().format(countBelumDisetujuai)
				+ "</font>, <font>" + Common.numberFormat.get().format(countDisetujuai)
				+ "</font>";
	}

	public void onAdd(Event event) throws Exception {
		biodataPegawaiAction = new BiodataPegawaiSimpleAction(new Pegawai(), satuanKerjaOnSession);
		biodataPegawaiAction.setCommonOnSearchdefault(PegawaiSimpleAction.this);
		biodataPegawaiAction.setHeight("95%");
		biodataPegawaiAction.setWidth("90%");
		page.getFirstRoot().appendChild(biodataPegawaiAction);
		biodataPegawaiAction.setVisible(true);
		biodataPegawaiAction.onModal();
	}

	public Criteria initCriteria(boolean order) {

		SatuanKerja parent = (SatuanKerja) searchparent.getAttribute("satuanKerja");
		Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
		if (parent != null) {
			satuanKerjas.clear(); satuanKerjas.add(parent);
			satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
		}

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(Pegawai.class)
				.add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")));
		if (order)
			criteria.addOrder(Order.asc("nama"));

		criteria.add(satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
				: Restrictions.or(parent==null ? Restrictions.isNull("satuanKerja") : Restrictions.sqlRestriction("false"), Restrictions.in("satuanKerja", satuanKerjas)))

				.add(searchstatus.getSelectedItem() == null || searchstatus.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("statusPegawai", searchstatus.getSelectedItem().getValue()))

				.add(satuanKerjaOnSession == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("satuanKerja", satuanKerjaOnSession))
				.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true") : Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))
				.add(searchcode.getText().trim().equals("") ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.ilike("code", searchcode.getText().trim(), MatchMode.ANYWHERE),
								Restrictions.ilike("mycode", searchcode.getText().trim(), MatchMode.ANYWHERE)));

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<Pegawai> pegawai = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(pegawai);
		grid.setRowRenderer(new PegawaiRenderer());
		grid.setModelCheckMobile(strset);

		if (biodataPegawaiAction != null) {
			biodataPegawaiAction.detach();
		}

	}

}
