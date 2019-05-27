/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package utilidad;

import juegosTablero.dominio.elementos.Jugador;

/**
 *
 * @author Miguel Gonzalez Garcia y Roberto Martinez Fernandez
 */
public class Participante implements Comparable<Participante>{
    
    private int id; //Numero identificador del Participante en el torneo.
    private Jugador jugador; //Datos del Participante.
    private int puntos; //Puntuacion del Participante tras las distintas rondas.
    private int victorias; //Numero de victorias del Participante.
    private int empates; //Numero de empates del Participante.
    private int valor; //Valor que permite asignar al Participante a algun grupo del torneo.
    
    /**
     * Constructor de Participante.
     * @param _id Identificador para el Participante.
     * @param _jugador Datos del Participante.
     */
    public Participante(int _id, Jugador _jugador){
        id = _id;
        jugador = _jugador;
        puntos = 0;
        victorias = 0;
        empates = 0;
        valor = 0;
    }
    
    /**
     * Funcion que representa una victoria del Participante.
     */
    public void victoria(){
        victorias++;
        puntos+=3;
        valor+=1;
    }
    
    /**
     * Funcion que representa un empate del Participante.
     * @param val Valor que sumara el Participante con el empate.
     */
    public void empate(int val){
        empates++;
        puntos+=1;
        valor+=val;
    }
    
    /**
     * Funcion que representa una ronda libre para el Participante.
     */
    public void BYE(){
        puntos+=3;
        valor+=1;
    }

    /**
     * Getter del atributo id.
     * @return id del Participante.
     */
    public int getId() {
        return id;
    }
    
    /**
     * Getter del atributo nick
     * @return nick del Participante.
     */
    public Jugador getJugador() {
        return jugador;
    }
    
    /**
     * Getter del atributo puntos.
     * @return puntos del Participante.
     */
    public int getPuntos() {
        return puntos;
    }
    
    /**
     * Getter del atributo valor.
     * @return valor actual del Participante.
     */
    public int getValor() {
        return valor;
    }
    
    /**
     * Getter del atributo victorias.
     * @return numero de victorias del Participante.
     */
    public int getVictorias(){
        return victorias;
    }

    /**
     * Getter del atributo empates.
     * @return numero de empates del Participante.
     */
    public int getEmpates() {
        return empates;
    }
    
    /**
     * Suma los puntos recibidos a la puntuacion actual del Participante.
     * @param _puntos Puntos a sumar.
     */
    public void sumarPuntuacion(long _puntos){
        puntos += _puntos;
    }

    @Override
    public int compareTo(Participante o) {
        if(valor < o.valor){
            return -1;
        }else if(valor > o.valor){
            return 1;
        }else{
            if(puntos < o.puntos){
                return -1;
            }else if(puntos > o.puntos){
                return 1;
            }else{
                return 0;
            }
        }
    }
    
}
