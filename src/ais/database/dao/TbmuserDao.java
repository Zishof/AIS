package ais.database.dao;

import ais.database.model.Tbmuser;




public interface TbmuserDao extends GenericDao<Tbmuser, Long> {
    public Boolean login(Tbmuser users);
    
    public Tbmuser loadByUsernameAndPassWithNewSession(Tbmuser users);
    
    public Boolean loginWithNewSession(Tbmuser users);

    public Tbmuser loadByUsernameAndPass(Tbmuser users);

    public Boolean isExist(Tbmuser users);

}
