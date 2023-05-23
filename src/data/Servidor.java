package data;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Rober
 */
public class Servidor implements Runnable {

    final static int NUMPUERTO=50000;
    ServerSocket puertoEscucha=null;
    boolean finServer=false;
    static List <GestionaCliente> gClientes=new ArrayList<>();
    static List<Partida>partidas=new ArrayList<>();

    public Servidor() {
      
    }
    @Override
    public void run() {
     try {
            puertoEscucha =new ServerSocket(NUMPUERTO);
           
        } catch (IOException ex) {
            System.out.println("No se puede escuchar en este puerto: " + NUMPUERTO + ", " + ex); 
            System.exit(1);        
        }
        
        Socket conexion=null;
        
        while(!finServer){
             try {
            System.out.println("Esperando conexión");
            puertoEscucha.setSoTimeout(3000);
            conexion = puertoEscucha.accept();
            GestionaCliente gb=new GestionaCliente(conexion);
            gClientes.add(gb);
            new Thread(gb).start();
            System.out.println("Conexion completada!");
        } catch (IOException ex) {
            
           }
        }
        
        try {
            if(conexion!=null){
             desconectarGestionaClientes();
             conexion.close(); 
            }
            puertoEscucha.close();
            System.out.println("Se ha finalizado el chat");
        } catch (IOException ex) {
            System.out.println("Algun flujo no puede cerrarse");
        }    
    }

    public void setFinServer(boolean finServer) {
        this.finServer = finServer;
    }

    private void desconectarGestionaClientes() {
        for (GestionaCliente gCliente : gClientes) {
            try {
                gCliente.out.writeUTF(Protocolo.FIN_SERVIDOR+"");
                gCliente.setFinServidor(true);
            } catch (IOException ex) {
                Logger.getLogger(Servidor.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
      
}