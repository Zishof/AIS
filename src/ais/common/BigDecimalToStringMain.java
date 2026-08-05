package ais.common;


import java.math.BigDecimal;

public class BigDecimalToStringMain {

   public static void main(String[] args) {
       BigDecimal bigDecimal=new BigDecimal(999999999999999999L);
       String toStringBigDec=bigDecimal.toString();
       System.out.println(toStringBigDec);
   }
}