package ais.action.master.sekolah.psb.noreg;

import java.util.List;

import ais.database.model.sekolah.CalonSiswa;

public interface NoRegGeneratorPsb {

	public String generateNoReg(CalonSiswa calonSiswa);

	public String generateNoReg(List<String> noRegPengecualian, CalonSiswa calonSiswa);

}
