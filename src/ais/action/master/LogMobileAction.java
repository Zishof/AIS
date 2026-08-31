package ais.action.master;


import ais.common.CommonSearchFilterHelper;
import java.util.Calendar;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.A;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.LogMobile;
import ais.database.model.Mahasiswa;
import ais.database.model.Tbmuser;
import ais.database.model.sekolah.Siswa;
import ais.ui.util.DataCriteria;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Controller/action ZK untuk log mobile. Tipe ini merupakan titik masuk UI yang menghubungkan
 * event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code org.zkoss.zul.Html dashboardHtml},
 * {@code org.zkoss.zul.Html progressHtml}, {@code MyGrid grid}, {@code Paging paging}, {@code Textbox
 * searchnim}, {@code Textbox searchnama}, {@code Textbox searchip}, {@code Textbox searchKeterangan};
 * inisialisasi/lifecycle ({@code doBeforeCompose()}, {@code doAfterCompose()}, {@code initCriteria()});
 * pembacaan/pencarian ({@code onSearchDefault()}, {@code refreshDashboardAman()}). Bagian lain dari kontrak
 * tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
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
public class LogMobileAction extends GenericAutowireComposer implements DataCriteria {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;

	private org.zkoss.zul.Html dashboardHtml;
	private org.zkoss.zul.Html progressHtml;

	private MyGrid grid;

	private Paging paging;
	private Textbox searchnim;
	private Textbox searchnama;
	private Textbox searchip;
	private Textbox searchKeterangan;
	private Textbox searchLinkProfile;
	private Combobox searchfakultas;
	private Combobox searchjurusan;
	private MyDatebox start;
	private MyDatebox end;
	private MyToolbarbuttonConfig find;

	private MyCheckboxConfig mahasiswa;
	private MyCheckboxConfig dosen;
	private MyCheckboxConfig admin;
	private MyCheckboxConfig siswa;
	private MyCheckboxConfig guru;

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

		if (start != null) start.setReadonly(true);
		if (end != null) end.setReadonly(true);

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) - 1);
		if (start != null) start.setValue(calendar.getTime());
		calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1);
		if (end != null) end.setValue(calendar.getTime());

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, "nama", "ip", "keterangan", "login", "logout",
				"dosen", "pegawai", "mahasiswa", "tbmuser", "jurusan", "fakultas", "hostname", "success_status",
				"description", "linkProfile", "header");
		Common.appendKeToolbar(cetakToolbarbutton, find, comp);

		Common.appendKeToolbar(siswa = new MyCheckboxConfig("Siswa"), find, comp);
		Common.appendKeToolbar(guru = new MyCheckboxConfig("Guru"), find, comp);
		Common.appendKeToolbar(mahasiswa = new MyCheckboxConfig("Mahasiswa"), find, comp);
		Common.appendKeToolbar(dosen = new MyCheckboxConfig("Dosen"), find, comp);
		Common.appendKeToolbar(admin = new MyCheckboxConfig("Admin"), find, comp);

		if (siswa != null) { siswa.setChecked(true); }
		if (guru != null) { guru.setChecked(true); }

		if (mahasiswa != null) { mahasiswa.setChecked(true); }
		if (dosen != null) { dosen.setChecked(true); }
		if (admin != null) { admin.setChecked(true); }

		siswa.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
		refreshDashboardAman();
		}
		});

		guru.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
		refreshDashboardAman();
		}
		});

		mahasiswa.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
		refreshDashboardAman();
		}
		});
		dosen.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
		refreshDashboardAman();
		}
		});
		admin.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
		refreshDashboardAman();
		}
		});

		onSearchDefault(null);
		refreshDashboardAman();
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
		refreshDashboardAman();
		}
		});
	}

	/**
	 * Renderer lokal untuk layar/komponen {@link LogMobileAction}. Kelas ini menerjemahkan satu item data menjadi
	 * baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link LogMobileAction} dan dapat mengakses state
	 * kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see LogMobileAction
	 */
	class LogMobileRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final LogMobile logMobile = (LogMobile) arg1;
			if ((logMobile.getSuccess_status() != null && !logMobile.getSuccess_status())) {
				arg0.setStyle("background-color: rgba(205,92,92,0.4);font-weight: bold;");
			}

			RevisiHelper
					.createNewRevisi(LogMobile.class, logMobile,
							logMobile.getLogin() == null ? "" : Common.dateFormat3.get().format(logMobile.getLogin()))
					.setParent(arg0);

			Vbox myVbox = new Vbox();
			myVbox.setParent(arg0);

			A myIp = new A(logMobile.getIp());
			myIp.setTarget("new");
			myIp.setHref("http://whatismyipaddress.com/ip/" + logMobile.getIp());
			myIp.setParent(myVbox);

			Tbmuser tbmuser = logMobile.getTbmuser();
			Dosen dosen = logMobile.getDosen();
			Mahasiswa mahasiswa = logMobile.getMahasiswa();
			Siswa siswa = logMobile.getSiswa();

			String nama = siswa != null ? siswa.getNama() + " (" + siswa.getNomorIndukNasional() + ")"
					: mahasiswa != null ? mahasiswa.getNama() + " (" + mahasiswa.getNim() + ")"
							: dosen != null ? dosen.getNama() : tbmuser == null ? "" : tbmuser.getUserNama();

			Vbox vbox = new Vbox();
			vbox.setParent(arg0);
			if (dosen != null) {
				CommonMedia.tampilkanGambarKecil(dosen).setParent(vbox);
			} else if (mahasiswa != null) {
				CommonMedia.tampilkanGambarKecil(mahasiswa).setParent(vbox);
			} else if (siswa != null) {
				CommonMedia.tampilkanGambarKecil(siswa).setParent(vbox);
			} else {
				CommonMedia.tampilkanGambarKecil(tbmuser).setParent(vbox);
			}
			vbox.appendChild(new Label(nama));

			if (siswa != null) {
				new Label(siswa.getYayasan() == null ? "" : logMobile.getYayasan().getNama()).setParent(arg0);
				new Label(logMobile.getSekolah() == null ? "" : logMobile.getSekolah().getNama()).setParent(arg0);
			} else {

				new Label(logMobile.getFakultas() == null ? "" : logMobile.getFakultas().getNama()).setParent(arg0);
				new Label(logMobile.getJurusan() == null ? "" : logMobile.getJurusan().getNama()).setParent(arg0);
			}

			vbox = new Vbox();
			vbox.setParent(arg0);
			new MyLabelKecil(logMobile.getLinkProfile() == null ? "" : logMobile.getLinkProfile()).setParent(vbox);

			new MyLabelKecil(logMobile.getHeader() == null ? "" : logMobile.getHeader()).setParent(vbox);
		}
	}

	public Criteria initCriteria(boolean order) {

		Criterion criterion = Restrictions.sqlRestriction("false");

		if (mahasiswa.isChecked() && dosen.isChecked() && admin.isChecked() && siswa.isChecked() && guru.isChecked()) {
			criterion = Restrictions.sqlRestriction("true");
		} else {
			if (siswa.isChecked()) {
				criterion = Restrictions.or(criterion, Restrictions.isNotNull("siswa"));
			}
			if (mahasiswa.isChecked()) {
				criterion = Restrictions.or(criterion, Restrictions.isNotNull("mahasiswa"));
			}
			if (dosen.isChecked()) {
				criterion = Restrictions.or(criterion, Restrictions.isNotNull("dosen"));
			}
			if (admin.isChecked()) {
				criterion = Restrictions.or(criterion,
						Restrictions.and(
								Restrictions.and(Restrictions.isNotNull("tbmuser"), Restrictions.isNull("dosen")),
								Restrictions.isNull("tbmuser.guru")));
			}
			if (guru.isChecked()) {
				criterion = Restrictions.or(criterion, Restrictions.isNotNull("tbmuser.guru"));
			}
		}

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(LogMobile.class)
				.add((start == null || end == null || start.getValue() == null || end.getValue() == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.sqlRestriction(
						"date(this_.login) between date('" + Common.databaseDateFormat.get().format(start.getValue())
								+ "') and date('" + Common.databaseDateFormat.get().format(end.getValue()) + "')")))

				.createAlias("siswa", "siswa", Criteria.LEFT_JOIN)
				.createAlias("mahasiswa", "mahasiswa", Criteria.LEFT_JOIN)
				.createAlias("tbmuser", "tbmuser", Criteria.LEFT_JOIN).createAlias("dosen", "dosen", Criteria.LEFT_JOIN)
				.add(criterion);
		if (order)
			criteria.addOrder(Order.desc("id"));

		criteria

				.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
						|| searchfakultas.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false))
				.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
						|| searchjurusan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false))

				.add(searchnim.getValue().trim().equals("") ? Restrictions.sqlRestriction("1=1") :

						Restrictions.or(
								Restrictions.ilike("tbmuser.userId", searchnim.getValue().trim(), MatchMode.ANYWHERE),
								Restrictions.or(
										Restrictions.ilike("siswa.nomorIndukNasional", searchnim.getValue().trim(),
												MatchMode.ANYWHERE),
										Restrictions.or(
												Restrictions.ilike("siswa.nomorInduk", searchnim.getValue().trim(),
														MatchMode.ANYWHERE),
												Restrictions.ilike("mahasiswa.nim", searchnim.getValue().trim(),
														MatchMode.ANYWHERE)))))

				.add(searchKeterangan.getValue().trim().equals("") ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("header", searchKeterangan.getValue(), MatchMode.ANYWHERE))

				.add(searchip.getValue().trim().equals("") ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("ip", searchip.getValue(), MatchMode.ANYWHERE))

				.add(searchLinkProfile == null || searchLinkProfile.getValue().trim().equals("")
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("linkProfile", searchLinkProfile.getValue(), MatchMode.ANYWHERE))

				.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1") :

						Restrictions
								.or(Restrictions.ilike("tbmuser.userNama", searchnama.getValue().trim(),
										MatchMode.ANYWHERE),
										Restrictions
												.or(Restrictions.ilike("siswa.nomorIndukNasional",
														searchnama.getValue().trim(), MatchMode.ANYWHERE),

														Restrictions.or(Restrictions.ilike("siswa.nama",
																searchnama.getValue().trim(), MatchMode.ANYWHERE),

																Restrictions.or(
																		Restrictions.ilike("siswa.nomorInduk",
																				searchnama.getValue().trim(),
																				MatchMode.ANYWHERE),

																		Restrictions.or(
																				Restrictions.or(
																						Restrictions.ilike(
																								"mahasiswa.nama",
																								searchnama
																										.getValue()
																										.trim(),
																								MatchMode.ANYWHERE),
																						Restrictions.ilike(
																								"tbmuser.userNama",
																								searchnama.getValue()
																										.trim(),
																								MatchMode.ANYWHERE)),
																				Restrictions.ilike("dosen.nama",
																						searchnama.getValue().trim(),
																						MatchMode.ANYWHERE)))))));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<LogMobile> logMobile = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(logMobile);
		grid.setRowRenderer(new LogMobileRenderer());
		grid.setModelCheckMobile(strset);

	}


	private void refreshDashboardAman() {
		try {
			// paging.getTotalSize() sudah dihitung onSearchDefault (selalu dipanggil tepat sebelum
			// method ini) -> hindari SELECT count(*) yang sama dijalankan dua kali.
			ais.action.master.helper.GenericActionDashboardHelper.refreshFromCriteria(dashboardHtml, progressHtml, this,
					"Dasbor Akses Mobile", "Pantau penggunaan aplikasi mobile, login berhasil/gagal, serta kelompok pengguna yang paling aktif.",
					paging == null ? -1L : paging.getTotalSize());
		} catch (Exception e) {
			try {
				ais.common.Common.tampilErrorJikaAdmin(e);
			} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/LogMobileAction.java:369");
			}
		}
	}

}
