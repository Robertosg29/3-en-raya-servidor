package data;

import GUI.Ventana;

/**
 *
 * @author Rober y Cris
 */
public class Main {

    public static void main(String[] args) {
       Servidor s=new Servidor();
       new Thread(s).start();
       new Ventana(s).setVisible(true);
    }
    
}
