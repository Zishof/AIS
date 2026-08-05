package ais.common;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.Projections;
import org.hibernate.type.StringType;
import org.json.JSONObject;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;

import ais.action.master.kursus.helper.KursusUtil;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataMahasiswa;
import ais.database.model.Dosen;
import ais.database.model.Mahasiswa;
import ais.database.model.OrangTua;
import ais.database.model.Pegawai;
import ais.database.model.sekolah.Siswa;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.kursus.PesertaKursus;
import ais.database.model.sirkulasisurat.PeminjamSurat;

/**
 * Helper pembuatan data turunan otomatis: orang tua, peminjam surat, dan peserta kursus.
 * Session/transaction ditangani terpusat agar tidak ada session manual yang tertinggal terbuka.
 */
public class CommonLibraryAutoHelper {

    private CommonLibraryAutoHelper() {
    }

    public static OrangTua checkApakahMahasiswaOtomatisMenjadiOrangTua(BiodataMahasiswa biodataMahasiswa) {
        if (biodataMahasiswa == null || isBlank(biodataMahasiswa.getNamaAyah())
                || isBlank(biodataMahasiswa.getNamaIbu())) {
            return null;
        }

        ensureRoleOrangTua();

        Session session = null;
        Transaction tx = null;
        boolean mulai = false;
        try {
            session = HibernateUtil.currentNativeSession();
            Mahasiswa mahasiswa = biodataMahasiswa.getMahasiswa();
            if (mahasiswa != null && mahasiswa.getId() != null) {
                try {
                    session.refresh(mahasiswa);
                } catch (Exception e) {
                    Common.tampilErrorJikaAdmin(e);
                }
            }

            OrangTua orangTua = cariOrangTua(session, biodataMahasiswa.getNamaAyah(), biodataMahasiswa.getNamaIbu());
            if (orangTua == null) {
                orangTua = new OrangTua();
                orangTua.setAktif(Boolean.TRUE);
                orangTua.setEmailAyah(biodataMahasiswa.getEmail());
                orangTua.setEmailIbu(biodataMahasiswa.getEmail());
                orangTua.setJenisPenghasilanWali(biodataMahasiswa.getJenisPenghasilanAyah());
                orangTua.setNikWali(biodataMahasiswa.getNikAyah());
            }

            isiOrangTuaDariBiodataMahasiswa(orangTua, biodataMahasiswa);
            tambahAnakMahasiswa(orangTua, mahasiswa);

            tx = session.getTransaction();
            if (tx == null || !tx.isActive()) {
                tx = session.beginTransaction();
                mulai = true;
            }
            Common.refreshSaveOrUpdate(session, orangTua);
            if (mahasiswa != null) {
                siapkanAkunOrangTuaMahasiswa(session, mahasiswa, orangTua);
                mahasiswa.setOrangTua(orangTua);
                Common.refreshUpdate(session, mahasiswa);
            }
            simpanUserOrangTua(session, orangTua, mahasiswa == null ? null : mahasiswa.getUserOrtu(),
                    biodataMahasiswa.getNamaAyah(), biodataMahasiswa.getNamaIbu(), biodataMahasiswa.getEmail());
            commit(tx, mulai);
            return orangTua;
        } catch (Exception e) {
            rollback(tx, mulai);
            Common.tampilErrorJikaAdmin(e);
            return null;
        } finally {
            Common.closeNativeSessionQuietly(session);
        }
    }

    public static OrangTua checkApakahSiswaOtomatisMenjadiOrangTua(Siswa siswa) {
        if (siswa == null || isBlank(siswa.getNamaAyah()) || isBlank(siswa.getNamaIbu())) {
            return null;
        }

        ensureRoleOrangTua();

        Session session = null;
        Transaction tx = null;
        boolean mulai = false;
        try {
            session = HibernateUtil.currentNativeSession();
            if (siswa.getId() != null) {
                try {
                    session.refresh(siswa);
                } catch (Exception e) {
                    Common.tampilErrorJikaAdmin(e);
                }
            }

            OrangTua orangTua = cariOrangTua(session, siswa.getNamaAyah(), siswa.getNamaIbu());
            if (orangTua == null) {
                orangTua = new OrangTua();
                orangTua.setAktif(Boolean.TRUE);
                orangTua.setEmailAyah(siswa.getAlamatEmail());
                orangTua.setEmailIbu(siswa.getAlamatEmail());
                orangTua.setNikWali(siswa.getNikAyah());
            }

            isiOrangTuaDariSiswa(orangTua, siswa);
            tambahAnakSiswa(orangTua, siswa);

            tx = session.getTransaction();
            if (tx == null || !tx.isActive()) {
                tx = session.beginTransaction();
                mulai = true;
            }
            Common.refreshSaveOrUpdate(session, orangTua);
            siapkanAkunOrangTuaSiswa(session, siswa, orangTua);
            siswa.setOrangTua(orangTua);
            Common.refreshUpdate(session, siswa);
            simpanUserOrangTua(session, orangTua, siswa.getUserOrtu(), siswa.getNamaAyah(), siswa.getNamaIbu(),
                    siswa.getAlamatEmail());
            commit(tx, mulai);
            return orangTua;
        } catch (Exception e) {
            rollback(tx, mulai);
            Common.tampilErrorJikaAdmin(e);
            return null;
        } finally {
            Common.closeNativeSessionQuietly(session);
        }
    }

    public static PeminjamSurat checkApakahMahasiswaOtomatisMenjadiPeminjamSuratPerpustakaan(String nim) {
        if (isBlank(nim)) {
            return null;
        }
        Session session = null;
        Transaction tx = null;
        boolean mulai = false;
        try {
            session = HibernateUtil.currentNativeSession();
            PeminjamSurat peminjamSurat = cariPeminjamMahasiswa(session, nim);
            if (peminjamSurat == null) {
                Mahasiswa mahasiswa = (Mahasiswa) session.createCriteria(Mahasiswa.class).add(Restrictions.eq("nim", nim))
                        .setMaxResults(1).uniqueResult();
                if (mahasiswa != null) {
                    peminjamSurat = new PeminjamSurat();
                    peminjamSurat.setKode(mahasiswa.getNim());
                    peminjamSurat.setNama(mahasiswa.getNama());
                    peminjamSurat.setMahasiswa(mahasiswa);
                    peminjamSurat.setJenisIdentitas("NIM");
                    peminjamSurat.setKeterangan("PeminjamSurat ini mendaftar otomatis");
                    tx = session.getTransaction();
                    if (tx == null || !tx.isActive()) { tx = session.beginTransaction(); mulai = true; }
                    session.save(peminjamSurat);
                    commit(tx, mulai);
                }
            }
            return peminjamSurat;
        } catch (Exception e) {
            rollback(tx, mulai);
            Common.tampilErrorJikaAdmin(e);
            return null;
        } finally { Common.closeNativeSessionQuietly(session); }
    }

    public static PeminjamSurat checkApakahPegawaiOtomatisMenjadiPeminjamSuratPerpustakaan(String nidn) {
        if (isBlank(nidn)) { return null; }
        Session session = null;
        Transaction tx = null;
        boolean mulai = false;
        try {
            session = HibernateUtil.currentNativeSession();
            PeminjamSurat peminjamSurat = (PeminjamSurat) session.createCriteria(PeminjamSurat.class)
                    .createAlias("pegawai", "pegawai", Criteria.LEFT_JOIN)
                    .add(Restrictions.or(Restrictions.eq("pegawai.mycode", nidn), Restrictions.sqlRestriction(
                            "replace(trim(this_.kode),'.','') = replace(trim(?),'.','')", nidn, new StringType())))
                    .setMaxResults(1).uniqueResult();
            if (peminjamSurat == null) {
                Pegawai pegawai = (Pegawai) session.createCriteria(Pegawai.class).add(Restrictions.eq("mycode", nidn))
                        .setMaxResults(1).uniqueResult();
                if (pegawai != null) {
                    peminjamSurat = new PeminjamSurat();
                    peminjamSurat.setKode(pegawai.getMycode());
                    peminjamSurat.setNama(pegawai.getNama());
                    peminjamSurat.setPegawai(pegawai);
                    peminjamSurat.setJenisIdentitas("NIP");
                    peminjamSurat.setKeterangan("PeminjamSurat ini mendaftar otomatis");
                    tx = session.getTransaction(); if (tx == null || !tx.isActive()) { tx = session.beginTransaction(); mulai = true; }
                    session.save(peminjamSurat); commit(tx, mulai);
                }
            }
            return peminjamSurat;
        } catch (Exception e) { rollback(tx, mulai); Common.tampilErrorJikaAdmin(e); return null; }
        finally { Common.closeNativeSessionQuietly(session); }
    }

    public static PeminjamSurat checkApakahDosenOtomatisMenjadiPeminjamSuratPerpustakaan(String nidn) {
        if (isBlank(nidn)) { return null; }
        Session session = null; Transaction tx = null; boolean mulai = false;
        try {
            session = HibernateUtil.currentNativeSession();
            PeminjamSurat peminjamSurat = (PeminjamSurat) session.createCriteria(PeminjamSurat.class)
                    .createAlias("dosen", "dosen", Criteria.LEFT_JOIN)
                    .add(Restrictions.or(Restrictions.eq("dosen.nidn", nidn), Restrictions.sqlRestriction(
                            "replace(trim(this_.kode),'.','') = replace(trim(?),'.','')", nidn, new StringType())))
                    .setMaxResults(1).uniqueResult();
            if (peminjamSurat == null) {
                Dosen dosen = (Dosen) session.createCriteria(Dosen.class).add(Restrictions.eq("nidn", nidn)).setMaxResults(1).uniqueResult();
                if (dosen != null) {
                    peminjamSurat = new PeminjamSurat();
                    peminjamSurat.setKode(dosen.getNidn());
                    peminjamSurat.setNama(dosen.getNama());
                    peminjamSurat.setDosen(dosen);
                    peminjamSurat.setJenisIdentitas("NIDN");
                    peminjamSurat.setKeterangan("PeminjamSurat ini mendaftar otomatis");
                    tx = session.getTransaction(); if (tx == null || !tx.isActive()) { tx = session.beginTransaction(); mulai = true; }
                    session.save(peminjamSurat); commit(tx, mulai);
                }
            }
            return peminjamSurat;
        } catch (Exception e) { rollback(tx, mulai); Common.tampilErrorJikaAdmin(e); return null; }
        finally { Common.closeNativeSessionQuietly(session); }
    }

    public static PeminjamSurat checkApakahSiswaOtomatisMenjadiPeminjamSuratPerpustakaan(Siswa siswa) {
        if (siswa == null) { return null; }
        String nim = siswa.getNomorInduk();
        String nim1 = siswa.getNomorIndukNasional();
        if (isBlank(nim) && isBlank(nim1)) { return null; }
        Session session = null; Transaction tx = null; boolean mulai = false;
        try {
            session = HibernateUtil.currentNativeSession();
            PeminjamSurat peminjamSurat = cariPeminjamSiswa(session, siswa, nim, nim1);
            tx = session.getTransaction(); if (tx == null || !tx.isActive()) { tx = session.beginTransaction(); mulai = true; }
            if (peminjamSurat == null) {
                peminjamSurat = new PeminjamSurat();
                peminjamSurat.setKode(isBlank(nim) ? nim1 : nim);
                peminjamSurat.setNama(siswa.getNamaSiswa());
                peminjamSurat.setSiswa(siswa);
                peminjamSurat.setJenisIdentitas("NIS");
                peminjamSurat.setKeterangan("PeminjamSurat ini mendaftar otomatis");
                session.save(peminjamSurat);
            } else {
                peminjamSurat.setSiswa(siswa);
                peminjamSurat.setJenisIdentitas("NIS");
                peminjamSurat.setKeterangan("PeminjamSurat ini mendaftar otomatis");
                session.update(peminjamSurat);
            }
            commit(tx, mulai);
            return peminjamSurat;
        } catch (Exception e) { rollback(tx, mulai); Common.tampilErrorJikaAdmin(e); return null; }
        finally { Common.closeNativeSessionQuietly(session); }
    }

    public static PesertaKursus checkApakahMahasiswaOtomatisMenjadiPesertaKursusPerpustakaan(String nim) {
        return ensurePesertaKursusMahasiswa(nim);
    }

    public static PesertaKursus checkApakahPegawaiOtomatisMenjadiPesertaKursusPerpustakaan(String nidn) {
        return ensurePesertaKursusPegawai(nidn);
    }

    public static PesertaKursus checkApakahDosenOtomatisMenjadiPesertaKursusPerpustakaan(String nidn) {
        return ensurePesertaKursusDosen(nidn);
    }

    public static PesertaKursus checkApakahSiswaOtomatisMenjadiPesertaKursusPerpustakaan(Siswa siswa) {
        return ensurePesertaKursusSiswa(siswa);
    }

    private static PesertaKursus ensurePesertaKursusMahasiswa(String nim) {
        if (isBlank(nim)) { return null; }
        Session session = null; Transaction tx = null; boolean mulai = false;
        try {
            session = HibernateUtil.currentNativeSession();
            PesertaKursus peserta = (PesertaKursus) session.createCriteria(PesertaKursus.class)
                    .createAlias("mahasiswa", "mahasiswa", Criteria.LEFT_JOIN)
                    .add(Restrictions.or(Restrictions.eq("mahasiswa.nim", nim), Restrictions.sqlRestriction(
                            "replace(replace(trim(this_.kode),'.',''),',','') = replace(replace(trim(?),'.',''),',','')", nim, new StringType())))
                    .setMaxResults(1).uniqueResult();
            Mahasiswa mahasiswa = (Mahasiswa) session.createCriteria(Mahasiswa.class).add(Restrictions.eq("nim", nim)).setMaxResults(1).uniqueResult();
            if (peserta == null && mahasiswa != null) {
                peserta = new PesertaKursus();
                peserta.setKode(mahasiswa.getNim()); peserta.setNama(mahasiswa.getNama()); peserta.setMahasiswa(mahasiswa);
                peserta.setTipePeserta(KursusUtil.MAHASISWA); peserta.setTipe(KursusUtil.MAHASISWA.getNama());
                tx = session.getTransaction(); if (tx == null || !tx.isActive()) { tx = session.beginTransaction(); mulai = true; }
                Common.refreshSaveOrUpdate(session, peserta); commit(tx, mulai);
            }
            return peserta;
        } catch (Exception e) { rollback(tx, mulai); Common.tampilErrorJikaAdmin(e); return null; }
        finally { Common.closeNativeSessionQuietly(session); }
    }

    private static PesertaKursus ensurePesertaKursusPegawai(String nidn) {
        if (isBlank(nidn)) { return null; }
        Session session = null; Transaction tx = null; boolean mulai = false;
        try {
            session = HibernateUtil.currentNativeSession();
            PesertaKursus peserta = (PesertaKursus) session.createCriteria(PesertaKursus.class)
                    .createAlias("pegawai", "pegawai", Criteria.LEFT_JOIN)
                    .add(Restrictions.or(Restrictions.eq("pegawai.mycode", nidn), Restrictions.sqlRestriction(
                            "replace(trim(this_.kode),'.','') = replace(trim(?),'.','')", nidn, new StringType())))
                    .setMaxResults(1).uniqueResult();
            if (peserta == null) {
                Pegawai pegawai = (Pegawai) session.createCriteria(Pegawai.class).add(Restrictions.eq("mycode", nidn)).setMaxResults(1).uniqueResult();
                if (pegawai != null) {
                    peserta = new PesertaKursus(); peserta.setKode(pegawai.getMycode()); peserta.setNama(pegawai.getNama());
                    peserta.setPegawai(pegawai); peserta.setTipePeserta(KursusUtil.PEGAWAI); peserta.setTipe(KursusUtil.PEGAWAI.getNama());
                    tx = session.getTransaction(); if (tx == null || !tx.isActive()) { tx = session.beginTransaction(); mulai = true; }
                    session.save(peserta); commit(tx, mulai);
                }
            }
            return peserta;
        } catch (Exception e) { rollback(tx, mulai); Common.tampilErrorJikaAdmin(e); return null; }
        finally { Common.closeNativeSessionQuietly(session); }
    }

    private static PesertaKursus ensurePesertaKursusDosen(String nidn) {
        if (isBlank(nidn)) { return null; }
        Session session = null; Transaction tx = null; boolean mulai = false;
        try {
            session = HibernateUtil.currentNativeSession();
            PesertaKursus peserta = (PesertaKursus) session.createCriteria(PesertaKursus.class)
                    .createAlias("dosen", "dosen", Criteria.LEFT_JOIN)
                    .add(Restrictions.or(Restrictions.eq("dosen.nidn", nidn), Restrictions.sqlRestriction(
                            "replace(trim(this_.kode),'.','') = replace(trim(?),'.','')", nidn, new StringType())))
                    .setMaxResults(1).uniqueResult();
            if (peserta == null) {
                Dosen dosen = (Dosen) session.createCriteria(Dosen.class).add(Restrictions.eq("nidn", nidn)).setMaxResults(1).uniqueResult();
                if (dosen != null) {
                    peserta = new PesertaKursus(); peserta.setKode(dosen.getNidn()); peserta.setNama(dosen.getNama()); peserta.setDosen(dosen);
                    peserta.setTipePeserta(KursusUtil.DOSEN); peserta.setTipe(KursusUtil.DOSEN.getNama());
                    tx = session.getTransaction(); if (tx == null || !tx.isActive()) { tx = session.beginTransaction(); mulai = true; }
                    session.save(peserta); commit(tx, mulai);
                }
            }
            return peserta;
        } catch (Exception e) { rollback(tx, mulai); Common.tampilErrorJikaAdmin(e); return null; }
        finally { Common.closeNativeSessionQuietly(session); }
    }

    private static PesertaKursus ensurePesertaKursusSiswa(Siswa siswa) {
        if (siswa == null) { return null; }
        String nim = siswa.getNomorInduk(); String nim1 = siswa.getNomorIndukNasional();
        if (isBlank(nim) && isBlank(nim1)) { return null; }
        Session session = null; Transaction tx = null; boolean mulai = false;
        try {
            session = HibernateUtil.currentNativeSession();
            PesertaKursus peserta = (PesertaKursus) session.createCriteria(PesertaKursus.class)
                    .createAlias("siswa", "siswa", Criteria.LEFT_JOIN)
                    .add(Restrictions.or(Restrictions.sqlRestriction("replace(trim(this_.kode),'.','') = replace(trim(?),'.','')", isBlank(nim1) ? nim : nim1, new StringType()),
                            Restrictions.or(Restrictions.eq("siswa.nomorIndukNasional", nim1), Restrictions.eq("siswa.nomorInduk", nim))))
                    .setMaxResults(1).uniqueResult();
            tx = session.getTransaction(); if (tx == null || !tx.isActive()) { tx = session.beginTransaction(); mulai = true; }
            if (peserta == null) {
                peserta = new PesertaKursus(); peserta.setKode(isBlank(nim) ? nim1 : nim); peserta.setNama(siswa.getNamaSiswa()); peserta.setSiswa(siswa);
                peserta.setTipePeserta(KursusUtil.SISWA); peserta.setTipe(KursusUtil.SISWA.getNama()); session.save(peserta);
            } else {
                peserta.setSiswa(siswa); peserta.setTipePeserta(KursusUtil.SISWA); peserta.setTipe(KursusUtil.SISWA.getNama()); session.update(peserta);
            }
            commit(tx, mulai); return peserta;
        } catch (Exception e) { rollback(tx, mulai); Common.tampilErrorJikaAdmin(e); return null; }
        finally { Common.closeNativeSessionQuietly(session); }
    }

    private static void ensureRoleOrangTua() {
        if (ConstantValues.roleOrangTua != null && ConstantValues.roleOrangTua.getRoleId() != null) {
            return;
        }
        Session session = null; Transaction tx = null; boolean mulai = false;
        try {
            session = HibernateUtil.currentNativeSession();
            Tbmrole role = (Tbmrole) session.createCriteria(Tbmrole.class).add(Restrictions.eq("roleId", Tbmrole.ORANG_TUA_KODE)).setMaxResults(1).uniqueResult();
            if (role == null) {
                role = new Tbmrole(); role.setRoleId(Tbmrole.ORANG_TUA_KODE); role.setRoleName(Tbmrole.ORANG_TUA);
                tx = session.getTransaction(); if (tx == null || !tx.isActive()) { tx = session.beginTransaction(); mulai = true; }
                session.save(role); commit(tx, mulai);
            }
            ConstantValues.roleOrangTua = role;
        } catch (Exception e) { rollback(tx, mulai); Common.tampilErrorJikaAdmin(e); }
        finally { Common.closeNativeSessionQuietly(session); }
    }

    private static OrangTua cariOrangTua(Session session, String namaAyah, String namaIbu) {
        return (OrangTua) session.createCriteria(OrangTua.class).add(Restrictions.sqlRestriction(
                "replace(replace(trim(this_.nama_ayah),'.',''),',','') = replace(replace(trim(?),'.',''),',','')",
                namaAyah, new StringType())).add(Restrictions.sqlRestriction(
                "replace(replace(trim(this_.nama_ibu),'.',''),',','') = replace(replace(trim(?),'.',''),',','')",
                namaIbu, new StringType())).setMaxResults(1).uniqueResult();
    }

    private static void simpanUserOrangTua(Session session, OrangTua orangTua, String userId, String namaAyah,
            String namaIbu, String email) {
        if (orangTua == null || ConstantValues.roleOrangTua == null || isBlank(userId)) {
            return;
        }
        try {
            Tbmuser tbmuser = (Tbmuser) session.createCriteria(Tbmuser.class)
                    .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", Boolean.TRUE)))
                    .add(Restrictions.eq("orangTua", orangTua)).setMaxResults(1).uniqueResult();
            if (tbmuser == null) {
                tbmuser = (Tbmuser) session.createCriteria(Tbmuser.class)
                        .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", Boolean.TRUE)))
                        .add(Restrictions.eq("userId", userId)).setMaxResults(1).uniqueResult();
            }
            if (tbmuser == null) {
                tbmuser = new Tbmuser();
                tbmuser.setUserId(userId);
                tbmuser.setEmail(email);
                tbmuser.setIs_encripted(Boolean.TRUE);
                tbmuser.setRoot(Boolean.FALSE);
                tbmuser.setUserNama(namaOrangTua(namaAyah, namaIbu));
                tbmuser.setOrangTua(orangTua);
                tbmuser.setUserPassword(Common.desEncrypter.get().encrypt(userId));
                tbmuser.setUserRole(ConstantValues.roleOrangTua);
                tbmuser.setUserShow(Integer.valueOf(1));
                tbmuser.setAktif(Boolean.TRUE);
                session.save(tbmuser);
            } else {
                tbmuser.setOrangTua(orangTua);
                tbmuser.setUserRole(ConstantValues.roleOrangTua);
                if (isBlank(tbmuser.getUserNama())) {
                    tbmuser.setUserNama(namaOrangTua(namaAyah, namaIbu));
                }
                if (isBlank(tbmuser.getEmail()) && !isBlank(email)) {
                    tbmuser.setEmail(email);
                }
                session.update(tbmuser);
            }
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        }
    }

    private static void isiOrangTuaDariBiodataMahasiswa(OrangTua orangTua, BiodataMahasiswa biodataMahasiswa) {
        orangTua.setAlamat(biodataMahasiswa.getAlamat());
        orangTua.setDusun(biodataMahasiswa.getDusun());
        orangTua.setJenisPekerjaanAyah(biodataMahasiswa.getJenisPekerjaanAyah());
        orangTua.setJenisPekerjaanIbu(biodataMahasiswa.getJenisPekerjaanIbu());
        orangTua.setJenisPekerjaanWali(biodataMahasiswa.getJenisPekerjaanWali());
        orangTua.setJenisPenghasilanAyah(biodataMahasiswa.getJenisPenghasilanAyah());
        orangTua.setJenisPenghasilanIbu(biodataMahasiswa.getJenisPenghasilanIbu());
        orangTua.setJenjangPendidikanAyah(biodataMahasiswa.getJenjangPendidikanAyah());
        orangTua.setJenjangPendidikanIbu(biodataMahasiswa.getJenjangPendidikanIbu());
        orangTua.setJenjangPendidikanWali(biodataMahasiswa.getJenjangPendidikanWali());
        orangTua.setKecamatan(biodataMahasiswa.getKecamatan());
        orangTua.setKelurahan(biodataMahasiswa.getKelurahan());
        orangTua.setKodepos(biodataMahasiswa.getKodepos());
        orangTua.setKota(biodataMahasiswa.getKota());
        orangTua.setNamaAyah(biodataMahasiswa.getNamaAyah());
        orangTua.setNamaIbu(biodataMahasiswa.getNamaIbu());
        orangTua.setNamaWali(biodataMahasiswa.getNamaWali());
        orangTua.setNikAyah(biodataMahasiswa.getNikAyah());
        orangTua.setNikIbu(biodataMahasiswa.getNikIbu());
        orangTua.setNoKK(biodataMahasiswa.getNoKK());
        orangTua.setPendapatanWali(biodataMahasiswa.getPendapatanWali());
        orangTua.setPropinsi(biodataMahasiswa.getPropinsi());
        orangTua.setRt(biodataMahasiswa.getRt());
        orangTua.setRw(biodataMahasiswa.getRw());
        orangTua.setTanggalLahirAyah(biodataMahasiswa.getTanggalLahirAyah());
        orangTua.setTanggalLahirIbu(biodataMahasiswa.getTanggalLahirIbu());
        orangTua.setTanggalLahirWali(biodataMahasiswa.getTanggalLahirWali());
        orangTua.setTelpAyah(biodataMahasiswa.getTelpAyah());
        orangTua.setTelpIbu(biodataMahasiswa.getTelpIbu());
        orangTua.setTelpWali(biodataMahasiswa.getTelpWali());
    }

    private static void isiOrangTuaDariSiswa(OrangTua orangTua, Siswa siswa) {
        orangTua.setAlamat(siswa.getAlamatSiswa());
        orangTua.setDusun(siswa.getDusun());
        orangTua.setKecamatan(siswa.getKecamatan());
        orangTua.setKelurahan(siswa.getKelurahan());
        orangTua.setKodepos(siswa.getKodePos());
        orangTua.setNamaAyah(siswa.getNamaAyah());
        orangTua.setNamaIbu(siswa.getNamaIbu());
        orangTua.setNamaWali(siswa.getNamaWali());
        orangTua.setNikAyah(siswa.getNikAyah());
        orangTua.setNikIbu(siswa.getNikIbu());
        orangTua.setRt(siswa.getRt());
        orangTua.setRw(siswa.getRw());
        orangTua.setTanggalLahirAyah(siswa.getTanggalLahirAyah());
        orangTua.setTanggalLahirIbu(siswa.getTanggalLahirIbu());
        orangTua.setTanggalLahirWali(siswa.getTanggalLahirWali());
        orangTua.setTelpAyah(siswa.getTeleponOrangTua());
    }

    private static void tambahAnakMahasiswa(OrangTua orangTua, Mahasiswa mahasiswa) {
        if (orangTua == null || mahasiswa == null || mahasiswa.getId() == null) {
            return;
        }
        try {
            JSONObject jsonObject = new JSONObject(orangTua.getAnak());
            jsonObject.put("mahasiswa_" + mahasiswa.getId(), mahasiswa.getId());
            orangTua.setAnak(jsonObject.toString());
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        }
    }

    private static void tambahAnakSiswa(OrangTua orangTua, Siswa siswa) {
        if (orangTua == null || siswa == null || siswa.getId() == null) {
            return;
        }
        try {
            JSONObject jsonObject = new JSONObject(orangTua.getAnak());
            jsonObject.put("siswa_" + siswa.getId(), siswa.getId());
            orangTua.setAnak(jsonObject.toString());
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        }
    }

    private static void siapkanAkunOrangTuaMahasiswa(Session session, Mahasiswa mahasiswa, OrangTua orangTua)
            throws Exception {
        if (mahasiswa == null || orangTua == null) {
            return;
        }
        if (isBlank(mahasiswa.getUserOrtu())) {
            String userId = buatUsernameOrangTua(session, orangTua.getNamaAyah());
            String passw = RandomStringUtils.randomNumeric(5);
            mahasiswa.setUserOrtu(userId);
            mahasiswa.setPassOrtu(Common.desEncrypter.get().encrypt(passw.trim()));
        }
    }

    private static void siapkanAkunOrangTuaSiswa(Session session, Siswa siswa, OrangTua orangTua) throws Exception {
        if (siswa == null || orangTua == null) {
            return;
        }
        if (isBlank(siswa.getUserOrtu())) {
            String userId = buatUsernameOrangTua(session, orangTua.getNamaAyah());
            String passw = RandomStringUtils.randomNumeric(5);
            siswa.setUserOrtu(userId);
            siswa.setPassOrtu(Common.desEncrypter.get().encrypt(passw.trim()));
        }
    }

    private static String buatUsernameOrangTua(Session session, String namaAyah) {
        String depan = "ortu";
        try {
            String[] parts = StringUtils.split(namaAyah, " ");
            if (parts != null && parts.length > 0 && !isBlank(parts[0])) {
                depan = parts[0].replaceAll("[^A-Za-z0-9]", "").toLowerCase();
            }
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        }
        if (isBlank(depan)) {
            depan = "ortu";
        }
        for (int i = 0; i < 20; i++) {
            String userId = (depan + RandomStringUtils.randomNumeric(3)).toLowerCase();
            try {
                Number count = (Number) session.createCriteria(Tbmuser.class)
                        .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", Boolean.TRUE)))
                        .add(Restrictions.eq("userId", userId)).setProjection(Projections.rowCount()).uniqueResult();
                if (count == null || count.intValue() == 0) {
                    return userId;
                }
            } catch (Exception e) {
                Common.tampilErrorJikaAdmin(e);
            }
        }
        return (depan + ais.ui.util.WaktuUtil.getCalendar().getTimeInMillis()).toLowerCase();
    }

    private static String namaOrangTua(String namaAyah, String namaIbu) {
        if (!isBlank(namaAyah) && !isBlank(namaIbu)) {
            return namaAyah + " / " + namaIbu;
        }
        if (!isBlank(namaAyah)) {
            return namaAyah;
        }
        if (!isBlank(namaIbu)) {
            return namaIbu;
        }
        return "Orang Tua";
    }

    private static PeminjamSurat cariPeminjamMahasiswa(Session session, String nim) {
        return (PeminjamSurat) session.createCriteria(PeminjamSurat.class).createAlias("mahasiswa", "mahasiswa", Criteria.LEFT_JOIN)
                .add(Restrictions.or(Restrictions.eq("mahasiswa.nim", nim), Restrictions.sqlRestriction(
                        "replace(replace(trim(this_.kode),'.',''),',','') = replace(replace(trim(?),'.',''),',','')", nim, new StringType())))
                .setMaxResults(1).uniqueResult();
    }

    private static PeminjamSurat cariPeminjamSiswa(Session session, Siswa siswa, String nim, String nim1) {
        return (PeminjamSurat) session.createCriteria(PeminjamSurat.class).createAlias("siswa", "siswa", Criteria.LEFT_JOIN)
                .add(Restrictions.or(Restrictions.sqlRestriction("replace(trim(this_.kode),'.','') = replace(trim(?),'.','')", isBlank(nim1) ? nim : nim1, new StringType()),
                        Restrictions.or(Restrictions.eq("siswa.nomorIndukNasional", nim1), Restrictions.eq("siswa.nomorInduk", nim))))
                .setMaxResults(1).uniqueResult();
    }

    private static boolean isBlank(String value) { return value == null || value.trim().length() == 0; }
    private static void commit(Transaction tx, boolean mulai) { if (mulai && tx != null && tx.isActive()) { tx.commit(); } }
    private static void rollback(Transaction tx, boolean mulai) { try { if (mulai && tx != null && tx.isActive()) { tx.rollback(); } } catch (Exception e) { Common.tampilErrorJikaAdmin(e); } }
}
