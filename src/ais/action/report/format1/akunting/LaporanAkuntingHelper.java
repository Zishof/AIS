package ais.action.report.format1.akunting;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Restrictions;

import ais.action.master.rab.util.WorkspaceTreeModel;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.akunting.GrupTransaksi;
import ais.database.model.akunting.JenisTransaksi;
import ais.database.model.akunting.Transaksi;
import ais.database.model.rab.Pejabat;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.rab.SatuanLokasi;
import ais.database.model.rab.SumberDana;
import ais.database.model.rab.Workspace;

public class LaporanAkuntingHelper {

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public synchronized static Map getDefaultParameter(Workspace workspace, Transaksi transaksi,
			WorkspaceTreeModel workspaceTreeModel) {
		GrupTransaksi grupTransaksi = transaksi.getGrupTransaksi();
		Session session = HibernateUtil.currentSession();
		JenisTransaksi jenisTransaksi = grupTransaksi.getJenisTransaksi();
		List<SatuanKerja> satuanKerjas = new ArrayList<SatuanKerja>();
		satuanKerjas.add(workspace.getSatuanKerja());
		workspaceTreeModel.getSatuanKerjaTreeModel().getParentSet(workspace.getSatuanKerja(), satuanKerjas);
		SatuanKerja lembaga = satuanKerjas.size() > 0 ? satuanKerjas.get(satuanKerjas.size() - 1) : null;
		SatuanKerja unit = satuanKerjas.size() > 1 ? satuanKerjas.get(satuanKerjas.size() - 2) : null;
		SatuanKerja satuanKerja = satuanKerjas.size() > 2 ? satuanKerjas.get(satuanKerjas.size() - 3) : null;
		SatuanLokasi satuanLokasi = satuanKerja == null ? null : satuanKerja.getSatuanLokasi();
		SumberDana sumberDana = workspace.getSumberDana();

		TreeSet<Workspace> workspaces = new TreeSet<Workspace>();
		workspaceTreeModel.getParentSet(workspace, workspaces);

		Workspace kegiatan = workspaceTreeModel.getJenisWorkspace(workspaces, "kegiatan");
		Workspace subkegiatan = workspaceTreeModel.getJenisWorkspace(workspaces, "sub kegiatan");

		Workspace fungsi = workspaceTreeModel.getJenisWorkspace(workspaces, "fungsi");
		Workspace subfungsi = workspaceTreeModel.getJenisWorkspace(workspaces, "sub fungsi");
		Workspace program = workspaceTreeModel.getJenisWorkspace(workspaces, "program");

		// Pejabat pejabat = (Pejabat) session
		// .createCriteria(Pejabat.class)
		// .add(Restrictions.eq("satuanKerja", workspace.getSatuanKerja()))
		// .createAlias("jenisJabatan", "jenisJabatan")
		// .add(Restrictions.ilike("jenisJabatan.nama",
		// "Pejabat Pembuat Komitmen", MatchMode.ANYWHERE))
		// .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).setMaxResults(1)
		// .uniqueResult();

		Pejabat pejabat = (Pejabat) session.createCriteria(Pejabat.class)
				.add(Restrictions.or(Restrictions.isNotNull("pegawai"),
						Restrictions.or(Restrictions.isNotNull("guru"), Restrictions.isNotNull("dosen"))))
				.add(Restrictions.or(Restrictions.or(Restrictions.eq("satuanKerja", satuanKerja),
						Restrictions.eq("satuanKerja", unit)), Restrictions.eq("satuanKerja", lembaga)))
				.createAlias("jenisJabatan", "jenisJabatan")
				.add(Restrictions.ilike("jenisJabatan.nama", "Pejabat Pembuat Komitmen", MatchMode.ANYWHERE))
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).setMaxResults(1).uniqueResult();

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.setTime(grupTransaksi.getTanggalTransaksi());

		final Map parameters = ais.common.HashMapGenerator.getRand();
		parameters.put("tanggal", grupTransaksi.getTanggalTransaksi() == null ? ""
				: Common.dateFormat2.get().format(grupTransaksi.getTanggalTransaksi()));
		parameters.put("nomor", ".../SPP-" + (jenisTransaksi == null ? "" : jenisTransaksi.getKode()) + "/BLSDM.6/"
				+ (calendar.get(Calendar.YEAR)));

		parameters.put("sifat", (jenisTransaksi == null ? "" : jenisTransaksi.getKode()));

		parameters.put("lembaga", lembaga == null ? "" : lembaga.getNama());
		parameters.put("unit_organisasi", unit == null ? "" : unit.getNama());
		parameters.put("satuan_kerja", satuanKerja == null ? "" : satuanKerja.getNama());
		parameters.put("kode_satuan_kerja", satuanKerja == null ? "" : satuanKerja.getKode());
		parameters.put("lokasi", satuanLokasi == null ? "" : satuanLokasi.getKode() + "-" + satuanLokasi.getNama());
		parameters.put("tempat", satuanLokasi == null ? "" : satuanLokasi.getNama());
		parameters.put("alamat", satuanKerja == null ? "" : satuanKerja.getAlamat());

		parameters.put("kegiatan", kegiatan == null ? "" : kegiatan.getNama());
		parameters.put("kode_kegiatan", (kegiatan == null ? "" : kegiatan.getKode()));

		parameters.put("sub_kegiatan", (subkegiatan == null ? "" : subkegiatan.getNama()));
		parameters.put("kode_sub_kegiatan", (subkegiatan == null ? "" : subkegiatan.getKode()));

		String fun = (fungsi == null ? "" : fungsi.getKode() + ".")
				+ (subfungsi == null ? "" : subfungsi.getKode() + ".") + (program == null ? "" : program.getKode());

		parameters.put("kode_fungsi", fun);
		parameters.put("jumlah", grupTransaksi.getTotalKredit());
		parameters.put("keperluan", grupTransaksi.getKeperluan());
		parameters.put("jenis_belanja", transaksi.getAkun() == null ? ""
				: transaksi.getAkun().getKode() + " - " + transaksi.getAkun().getNama());

		parameters.put("atas_nama", grupTransaksi.getKepada());
		parameters.put("alamat_nama", grupTransaksi.getAlamat());
		parameters.put("rekening", grupTransaksi.getKeteranganRekening());
		parameters.put("no_tag",
				grupTransaksi.getNomorTagihan() + " - " + (grupTransaksi.getTanggalTagihan() == null ? ""
						: Common.dateFormat2.get().format(grupTransaksi.getTanggalTagihan())));

		parameters.put("pejabat",
				pejabat == null || pejabat.getPegawai() == null
						? (pejabat.getDosen() == null ? "" : pejabat.getDosen().getNama())
						: pejabat.getPegawai().getNama());
		parameters.put("nip_pejabat",
				pejabat == null || pejabat.getPegawai() == null
						? (pejabat.getDosen() == null ? "" : pejabat.getDosen().getCode())
						: pejabat.getPegawai().getCode());

		parameters.put("sumber_dana", sumberDana == null ? "" : (sumberDana.getNama() + " " + sumberDana.getTahun()));
		parameters.put("nomor_sumber_dana", sumberDana == null ? "" : (sumberDana.getKode()));
		parameters.put("tanggal_sumber_dana", sumberDana == null || sumberDana.getTanggal() == null ? ""
				: (Common.dateFormat2.get().format(sumberDana.getTanggal())));
		parameters.put("keterangan", transaksi == null ? "" : (transaksi.getKeterangan()));

		return parameters;

	}

}
