package data;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Rober
 */
public class GestionaCliente implements Runnable {

    Socket s;
    DataInputStream in = null;
    DataOutputStream out = null;
    boolean finServidor = false;
    String nombre = "";
    final int id;
    static int _id;
//    Partida p;
    int cod_Partida=-1;
    char ficha = ' ';

    public GestionaCliente(Socket s) {

        this.id = _id++;
        this.s = s;
        try {
            in = new DataInputStream(s.getInputStream());
            out = new DataOutputStream(s.getOutputStream());
        } catch (IOException ex) {
            Logger.getLogger(GestionaCliente.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public void setFinServidor(boolean finServidor) {
        this.finServidor = finServidor;
    }

    public char getFicha() {
        return ficha;
    }

    public void setFicha(char ficha) {
        this.ficha = ficha;
    }

    @Override
    public void run() {
        try {
            while (!finServidor) {
                try {
                    s.setSoTimeout(1000);
                    String cad = in.readUTF();
                    System.out.println("Cliente dice : " + cad);
                    recibirCliente(cad);
                } catch (IOException iOException) {
                }
            }
        } finally {
            try {

                out.close();
                in.close();
                s.close();
                Servidor.gClientes.remove(this);//OJOOOOO QUE SI NO BORRAS AL GESTIONACLIENTE DE LA LISTA, TE SALTAN
                //400 EXCEPCIONES PORQUE NO PUEDE USAR LOS FLUJOS EN LOS METODOS QUE RECORREN LA LISTA
                System.out.println("Se ha finalizado el chat.");
            } catch (IOException ex) {
                System.out.println("Algún flujo no puede cerrarse.");
            }
        }
    }

    private void recibirCliente(String cad) {
        try {

            String[] msj = cad.split(Protocolo.SEPARADOR);
            switch (msj[0]) {
                case Protocolo.ENVIAR_NOMBRE_C + "":
                    comprobarSiExisteNombre(msj[1]);
                    break;
                case Protocolo.DAME_PARTIDA_C + "":
                    buscarOCrearPartida();// le asigno una partida una vez que se compruebo su nombre e hizo click en el buscarPartida;
                    break;
                case Protocolo.INICIAR_PARTIDA_C + "":
                    elegirFichas(msj[1]);//aqui voy a darle a mi gC su ficha y al rival la otra
                    iniciarPartida();//lanzaremos el hilo del juego(Partida) y enviaremos a los dos gC orden de que cierren sus Ventanas
                    break;
                case Protocolo.ENVIAR_POSICION_C + "":
                    actualizarTablero(msj[1]);//troceo y actualizo el tablero de la partida 
                    enviarPosicionAJugadores(msj[1]);//envio el movimiento a los dos jug para que actualicen sus tableros
                    //hasta que no recibo el movimiento no desbloqueo el hilo para que mande al otro cliente  COLOCA_FICHA y nos mande su pos
                    enviarNotifyAPartida();
                    break;
                case Protocolo.FIN_CLIENTE_C + "":
                    finalizarPartidaNormalmente();//HA TERMINADO LA PARTIDA CORRECTAMENTE
                    break;

                case Protocolo.FIN_CLIENTE_ABANDONO_C + "":
                    finalizarPartidaPorAbandono(msj[1]);//EL CLIENTE HA CERRADO MANUALMENTE LA VENTANA
                    break;
            }
            if (!finServidor) {
                out.flush();
            }
        } catch (IOException ex) {
            Logger.getLogger(GestionaCliente.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void comprobarSiExisteNombre(String nombre) {
        boolean nombreRepetido = false;
        //En este metodo hago el envio desde this porque solo quiero que mi cliente reciba la notificacion, nadie mas
        for (GestionaCliente gCliente : Servidor.gClientes) {
            if (nombre.compareToIgnoreCase(gCliente.nombre.trim()) == 0) {
                try {
                    this.out.writeUTF(Protocolo.NOMBRE_REPETIDO_S + "");
                    this.out.flush();
                    nombreRepetido = true;
                } catch (IOException ex) {
                    Logger.getLogger(GestionaCliente.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        //este if solo se ejecutara si ningun gC coincide con el nombre que paso por parametro
        if (!nombreRepetido) {
            try {
                this.out.writeUTF(Protocolo.NOMBRE_OK_S + Protocolo.SEPARADOR + id);//le mando confirmacion de nombre y su id
                this.out.flush();
                this.nombre = nombre;
            } catch (IOException ex) {
                Logger.getLogger(GestionaCliente.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

    }

    private void buscarOCrearPartida() {
        boolean partidasDisponibles = false;
        //Compruebo la lista de partidas del servidor, si esta vacia o si todas las partidas tienen 2 gC, entro en el siguiente 
        //if y creo una nueva.
        //Esto me sirve tanto si entro la primera vez como si entro cualquier otra.
        for (Partida pa : Servidor.partidas) {
            if (pa.getJugadores().size() < 2) {
                try {
                    pa.addJugador(this);
                    cod_Partida=pa.getId();
//                    p = pa;//asignamos esta partida pa a la p que es la partida que tiene el gC como atributo.
                    partidasDisponibles = true;
//                    asignarGCAPartidaDeGCYaCreada();//OJO una vez añades un nuevo gc a una partida ya creada,
                    //necesitas que el primer gC actualice su partida para añadir a este pollo.
                    enviarReadyAClientes();
                } catch (Exception ex) {
                    Logger.getLogger(GestionaCliente.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }

        if (!partidasDisponibles) {
            try {
                Partida p = new Partida();
                cod_Partida=p.getId();
                p.addJugador(this);
                Servidor.partidas.add(p);
            } catch (Exception ex) {
                Logger.getLogger(GestionaCliente.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private void asignarGCAPartidaDeGCYaCreada() {
        //En un principio esto sirve para todos los gc que queramos meter en una partida
        //OJO una vez añades un nuevo gc a una partida ya creada,
        //necesitas que el primero gC actualice su partida para añadir a este pollo.
//        for (GestionaCliente gCliente : p.getJugadores()) {//hay que recorrer la lista de la partida no del servidor OJOOOO
//            if (p.getId() == gCliente.p.getId() && !gCliente.equals(this)) {
//                gCliente.p = p;
//                System.out.println("La lista del primer jugador de partida" + gCliente.p.getId() + " tiene " + gCliente.p.getJugadores().size());
//            }
//        }
    }

    private void enviarReadyAClientes() {
        //voy a enviar que la partida esta lista para empezar y el nombre de cada adversario para que lo pinten
        //OJOOO SI HUBIESE MAS JUGADORES ESTE METODO HABRIA QUE CAMBIARLO, NO SIRVE
        for (GestionaCliente gC : buscarPartidaEnServidor(cod_Partida).getJugadores()) {
            try {
                if (!gC.equals(this)) {//si el gC es distinto de mi, yo envio a mi cliente el nombre del gC de la lista
                    this.out.writeUTF(Protocolo.PARTIDA_READY_S + Protocolo.SEPARADOR + gC.nombre);
                    this.out.flush();
                    //y al gC de la lista mi nombre
                    gC.out.writeUTF(Protocolo.PARTIDA_READY_S + Protocolo.SEPARADOR + nombre);
                    gC.out.flush();
                }
            } catch (IOException ex) {
                Logger.getLogger(GestionaCliente.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    
    
    
    

    private void elegirFichas(String cadFicha) {
        ficha = cadFicha.charAt(0);//damos valor a la ficha del GC
        //Y a continuacion elegimos la contraria para el rival
        char fichaRival = ' ';
        for (GestionaCliente gC : buscarPartidaEnServidor(cod_Partida).getJugadores()) {
            if (!gC.equals(this)) {//si no soy yo
                if (this.getFicha() == 'X') {//compruebo mi elección y le doy la contraria al rival
                    fichaRival = 'O';
                    gC.setFicha(fichaRival);

                } else {
                    fichaRival = 'X';
                    gC.setFicha(fichaRival);
                }
                //tambien necesito darsela al cliente rival
                try {
                    gC.out.writeUTF(Protocolo.ENVIAR_FICHA_RIVAL_S + Protocolo.SEPARADOR + fichaRival);
                    gC.out.flush();
                } catch (IOException ex) {
                    Logger.getLogger(GestionaCliente.class.getName()).log(Level.SEVERE, null, ex);
                }

            }

        }
    }

    private void iniciarPartida() {
        //VOY A ENVIAR ORDEN DE CERRAR A LOS CLIENTES Y A LANZAR MI HILO PARTIDA
        for (GestionaCliente gC : buscarPartidaEnServidor(cod_Partida).getJugadores()) {
            try {
                gC.out.writeUTF(Protocolo.CERRAR_VENTANAS_S + "");
                gC.out.flush();

            } catch (IOException ex) {
                Logger.getLogger(GestionaCliente.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        //LANZO HILO
        // para que funcione bien, necesito lanzar el hilo de la partida que esta en el servidor y no de la que tiene el gC
        new Thread(buscarPartidaEnServidor(cod_Partida)).start();

    }

    

    private void actualizarTablero(String posFicha) {

        String[] cadPosicion = posFicha.split(Protocolo.SEPARADOR2);//troceo la cadena
        int fila = Integer.parseInt(cadPosicion[0]);
        int columna = Integer.parseInt(cadPosicion[1]);
        char fich = cadPosicion[2].charAt(0);
        
        //actualizo el tablero de la partida del servidor si cojo la p del gC el tablero del otro gC no se modificará
        //esto equivale a partida.getTablero.colocarficha()    
        buscarPartidaEnServidor(cod_Partida).getT().colocarFicha(fila, columna, fich);

    }

    private void enviarPosicionAJugadores(String posYFicha) {
        //PARA QUE ACTUALICEN SUS TABLEROS Y VEAN DONDE PUEDEN COLOCAR LA FICHA
        
        String mensaje = Protocolo.ENVIAR_POSICION_S + Protocolo.SEPARADOR + posYFicha;
        for (GestionaCliente gC : buscarPartidaEnServidor(cod_Partida).getJugadores()) {
            try {
                gC.out.writeUTF(mensaje);
                gC.out.flush();
            } catch (IOException ex) {
                Logger.getLogger(GestionaCliente.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private void enviarNotifyAPartida() {
        
       //necesito acceder a la partida que esta en el servidor ya que es la que es común, la del propio gC no me sirve
       // DESBLOQUEO EL HILO Y EL WHILE PARA QUE SE CAMBIE DE TURNO
        buscarPartidaEnServidor(cod_Partida).sinElegirPosicion = true;
        
        synchronized (buscarPartidaEnServidor(cod_Partida)) {
            buscarPartidaEnServidor(cod_Partida).notify();
        }
    }
    
    
    
    private Partida buscarPartidaEnServidor(int cod) {
        for (Partida partida : Servidor.partidas) {
            if (cod == partida.getId()) {
                return partida;
            }
        }
        return null;
    }
    
    public void finalizarPartidaNormalmente() {
        Partida p=buscarPartidaEnServidor(cod_Partida);
        //buscamos partida para eliminarla solo una vez, de esta manera si el segundo gC no podrá borrar algo que no existe
        if(p!=null){
            Servidor.partidas.remove(buscarPartidaEnServidor(cod_Partida));
        }
        Servidor.gClientes.remove(this);
        this.setFinServidor(true);
       
    } 
    
    private void finalizarPartidaPorAbandono(String id) {
        int cod = Integer.parseInt(id);//recibo el id del cliente que se ha desconectado
        
        if (buscarPartidaEnServidor(cod_Partida) != null) {
        // esta parte solo hago si el cliente previamente habia buscado partida(y su p!=null),
            int posPartida = -1;

            for (GestionaCliente jug : buscarPartidaEnServidor(cod_Partida).getJugadores()) {
                // me recorro la partida y busco por ID el gC que no coincida,
                //xq sera el que siga vivo y haya que mandarle GANAR POR ABANDONO  
                if (jug.id != cod) {
                    try {
                        jug.out.writeUTF(Protocolo.GANAR_POR_ABANDONO_S + Protocolo.SEPARADOR + id);
                        jug.out.flush();
                    } catch (IOException ex) {
                        Logger.getLogger(GestionaCliente.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
            //Si tuviese la partida en wait necesitaria liberarla y despues cambiar el booleando para que acabe el run
            //de esta forma cortaria este hilo en ejecucion y no tendria problemas en el servidor
            
            buscarPartidaEnServidor(cod_Partida).finPartida=true;
            enviarNotifyAPartida();
            
            
            //VACIO LA PARTIDA 
            buscarPartidaEnServidor(cod_Partida).getJugadores().clear();
            // Me recorro la lista, saco la posicion en la que esta mi partida para pasarsela al remove y borrarla
            for (int i = 0; i < Servidor.partidas.size(); i++) {
                if (Servidor.partidas.get(i).getId() == cod_Partida) {
                    posPartida = i;
                }
            }
            Servidor.partidas.remove(posPartida);//aqui la borro
        }
        
        //ahora corto el while del gC que es quien se desconecto
        this.setFinServidor(true);
       

    }
}
