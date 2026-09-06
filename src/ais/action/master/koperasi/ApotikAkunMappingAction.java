package ais.action.master.koperasi;

import java.util.LinkedHashMap;
import java.util.Map;

import org.hibernate.Session;
import org.json.JSONObject;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Div;
import org.zkoss.zul.Hlayout;
import org.zkoss.zul.Label;
import org.zkoss.zul.Vlayout;

import ais.action.master.akunting.helper.AmbilDataAkunBanbox;
import ais.action.master.akunting.helper.AmbilDataAkunDebetBanbox;
import ais.action.master.akunting.helper.AmbilDataAkunKreditBanbox;
import ais.action.servlet.api.ApotikPostingHelper;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.akunting.Akun;
import ais.database.model.sirs.ApotikAkunMapping;
import ais.ui.util.DashboardUiKit;
import ais.ui.util.MyToolbarbuttonConfig;

/** CRUD ringkas pemetaan akun khusus Apotik untuk layar ZK. */
public class ApotikAkunMappingAction extends GenericAutowireComposer {

	private static final long serialVersionUID = 1L;
	private Div apotikAkunMappingHost;
	private final Map<String, AmbilDataAkunBanbox> picker = new LinkedHashMap<String, AmbilDataAkunBanbox>();
	private final String[] peran = { ApotikAkunMapping.PENDAPATAN, ApotikAkunMapping.HPP,
			ApotikAkunMapping.PERSEDIAAN, ApotikAkunMapping.PIUTANG, ApotikAkunMapping.UTANG_PBF };
	private final String[] label = { "Pendapatan Penjualan Apotik (Kredit)", "HPP Apotik (Debet)",
			"Persediaan Obat (Debet)", "Piutang Pasien/Asuransi (Debet)", "Utang PBF (Kredit)" };
	private final String[] keterangan = {
			"Akun kredit untuk penjualan obat.", "Akun debet biaya pokok penjualan.",
			"Akun debet saat penerimaan dan kredit saat HPP.", "Akun debet untuk transaksi kredit/asuransi.",
			"Akun kredit saat penerimaan PBF dan debet saat pembayaran." };

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	@Override
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		Common.initLaguage();
		if (apotikAkunMappingHost == null) return;
		DashboardUiKit.attachIntro(comp, "Pemetaan Akun Apotik",
				"Pilih akun khusus Apotik. Sistem tidak pernah menyalin kode akun Kantin secara otomatis.");
		Vlayout isi = new Vlayout();
		isi.setWidth("100%");
		isi.setStyle("padding:12px;gap:10px;max-width:980px;");
		isi.setParent(apotikAkunMappingHost);

		boolean admin = Common.getApakahAdminLain(Common.getCurrentUser());
		for (int i = 0; i < peran.length; i++) {
			Hlayout baris = new Hlayout();
			baris.setWidth("100%");
			baris.setStyle("gap:12px;align-items:center;padding:8px;border-bottom:1px solid #e2e8f0;");
			Vlayout teks = new Vlayout();
			teks.setWidth("360px");
			Label judul = new Label(label[i]);
			judul.setStyle("font-weight:600;color:#0f172a;");
			teks.appendChild(judul);
			Label deskripsi = new Label(keterangan[i]);
			deskripsi.setStyle("font-size:11px;color:#64748b;");
			teks.appendChild(deskripsi);
			baris.appendChild(teks);
			AmbilDataAkunBanbox akun = posisiKredit(i)
					? new AmbilDataAkunKreditBanbox() : new AmbilDataAkunDebetBanbox();
			akun.setWidth("520px");
			akun.setDisabled(!admin);
			picker.put(peran[i], akun);
			baris.appendChild(akun);
			isi.appendChild(baris);
		}
		muatNilai();
		if (admin) {
			MyToolbarbuttonConfig simpan = new MyToolbarbuttonConfig(Common.getBahasaConfig("Simpan Pemetaan"));
			simpan.addEventListener("onClick", new EventListener() {
				@Override public void onEvent(Event event) throws Exception { simpan(); }
			});
			isi.appendChild(simpan);
		} else {
			isi.appendChild(new Label("Hanya admin yang boleh mengubah pemetaan akun Apotik."));
		}
	}

	private boolean posisiKredit(int indeks) {
		return indeks == 0 || indeks == 4;
	}

	private void muatNilai() {
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			for (String namaPeran : peran) {
				ApotikAkunMapping mapping = (ApotikAkunMapping) session.createCriteria(ApotikAkunMapping.class)
						.add(org.hibernate.criterion.Restrictions.eq("peran", namaPeran))
						.add(org.hibernate.criterion.Restrictions.eq("aktif", Boolean.TRUE))
						.setMaxResults(1).uniqueResult();
				Akun akun = mapping == null ? null : mapping.getAkun();
				if (akun != null) {
					AmbilDataAkunBanbox komponen = picker.get(namaPeran);
					komponen.setAttribute("akun", akun);
					komponen.setValue(akun.toString());
				}
			}
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	private void simpan() {
		JSONObject akun = new JSONObject();
		try {
			for (String namaPeran : peran) {
				Object nilai = picker.get(namaPeran).getAttribute("akun");
				if (!(nilai instanceof Akun) || ((Akun) nilai).getId() == null)
					throw new IllegalArgumentException("Seluruh lima peran akun wajib dipilih.");
				akun.put(namaPeran, ((Akun) nilai).getId());
			}
			JSONObject payload = new JSONObject();
			payload.put("akun", akun);
			JSONObject hasil = new JSONObject();
			ApotikPostingHelper.proses("apotik_pemetaan_akun_terapkan", Common.getCurrentUser(), payload, hasil);
			String pesan = hasil.optString("message", hasil.optString("description", "Pemetaan diproses."));
			if (!"00".equals(hasil.optString("status"))) throw new IllegalStateException(pesan);
			show(pesan);
			muatNilai();
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "ApotikAkunMappingAction.simpan");
			show("Pemetaan gagal disimpan: " + e.getMessage());
		}
	}

	private void show(String pesan) {
		try {
			ais.ui.util.MyMessageboxConfig.show(pesan, "Pemetaan Akun Apotik",
					org.zkoss.zul.Messagebox.OK, org.zkoss.zul.Messagebox.INFORMATION);
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "ApotikAkunMappingAction.show");
		}
	}
}
