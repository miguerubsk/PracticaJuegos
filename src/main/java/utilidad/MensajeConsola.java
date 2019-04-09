/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package utilidad;

/**
 *
 * @author Roberto Martínez Fernández
 */
public class MensajeConsola {
    
    private String nombreAgente;
    private String mensaje;

    public MensajeConsola(String nomAgente, String _mensaje) {
        this.nombreAgente = nomAgente;
        this.mensaje = _mensaje;
    }

    //Getter de nombreAgente
    public String getNombreAgente() {
        return nombreAgente;
    }

    //Setter de nombreAgente
    public void setNombreAgente(String _nombreAgente) {
        this.nombreAgente = _nombreAgente;
    }

    //Getter de mensaje
    public String getMensaje() {
        return mensaje;
    }

    //Setter de mensaje
    public void setMensaje(String _mensaje) {
        this.mensaje = _mensaje;
    }

    //Metodo toString de MensajeConsola
    @Override
    public String toString() {
        String ts = "Mensaje enviado por: "+nombreAgente+"\nContenido: "+mensaje+"\n";
        return ts;
    }
    
}
