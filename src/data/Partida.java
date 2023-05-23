package data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Rober
 */
public class Partida implements Runnable {

    final int id;
    static int _id = 1;
    int turno;
    boolean finPartida = false;
    boolean sinElegirPosicion = false;
    Tablero t;
    List<GestionaCliente> jugadores = new ArrayList<>();

    public Partida() {
        this.id = _id++;
        t = new Tablero();
    }

    public int getId() {
        return id;
    }

    public Tablero getT() {
        return t;
    }

    public List<GestionaCliente> getJugadores() {
        return jugadores;
    }

    public void addJugador(GestionaCliente gc) throws Exception {
        if (gc != null) {
            jugadores.add(gc);
        } else {
            throw new Exception("No puedes añadir jugadores nulos");
        }
    }

    public void cambiarTurno() {
        turno = (turno + 1) % jugadores.size();
    }

    @Override
    public void run() {
        Random r = new Random();
        //elijo un turno al azar la primera vez
        turno = r.nextInt(2);//me devuelve 0 y 1 aleatoriamente

        while (!finPartida) {

            try {

                //eligo al GCliente que tiene la posicion de este num aleatorio en la partida
                jugadores.get(turno).out.writeUTF(Protocolo.COLOCA_FICHA_S + "");
                jugadores.get(turno).out.flush();
                //paro al hilo hasta que recibo una posicion del cliente con turno
                while (!sinElegirPosicion) {
                    System.out.println(jugadores.get(turno).nombre + " esta esperando.");
                    synchronized (this) {
                        this.wait();
                    }
                }
                //vuelvo a poner el booleano a false para que vuelva a esperar 
                sinElegirPosicion = false;

                
                if (!finPartida) {
                //SI ALGUIEN ABANDONA, EL CAMBIAR TURNO NO SE PUEDE HACER Y LANZA UN ERROR, DE ESTA MANERA 
                //SOLO ENTRO AQUI SI NADIE HA CAMBIADO LA VARIABLE FIN PARTIDA DESDE FUERA DE LA PARTIDA
                    if (comprobarGanador()) {
                        finPartida = true;
                    }
                    //ahora le enviaremos al siguiente jugador el protocolo coloca ficha
                    cambiarTurno();

                }

            } catch (IOException ex) {
                Logger.getLogger(Partida.class.getName()).log(Level.SEVERE, null, ex);
            } catch (InterruptedException ex) {
                Logger.getLogger(Partida.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private boolean comprobarGanador() {
        char fichaGanadora = t.comprobarGanador();
        if (fichaGanadora != 'F') { //comprobarGanador me devuelve una F si no hay ganador
            //si estoy aqui es porque existe un ganador si o si
            for (GestionaCliente j : jugadores) {
                try {
                    if (j.getFicha() == fichaGanadora) {

                        j.out.writeUTF(Protocolo.HAS_GANADO_S + Protocolo.SEPARADOR);
                        j.out.flush();

                    } else {
                        j.out.writeUTF(Protocolo.HAS_PERDIDO_S + Protocolo.SEPARADOR);
                        j.out.flush();
                    }
                } catch (IOException ex) {
                    Logger.getLogger(Partida.class.getName()).log(Level.SEVERE, null, ex);
                }
                //cortamos el gC
                j.finalizarPartidaNormalmente();
            }
        } else {
            //no hay ganador fichaGanadora=S
            return false;
        }
        //entramos en el for y enviamos ganado y perdido
        return true;

    }

}
