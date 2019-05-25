/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package GUI;

import java.awt.Container;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;
import javafx.util.Pair;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import juegosTablero.dominio.elementos.Jugador;

/**
 *
 * @author Roberto Martínez Fernández
 */
public class Conecta4JFrame extends JFrame{
    
    public static final int CASILLA_VACIA = 0;
    public static final int CASILLA_J1 = 2;
    public static final int CASILLA_J2 = 1;
    public static final int VICTORIA_J1 = 4;
    public static final int VICTORIA_J2 = 3;
    public static final int JUGADOR_1 = 0;
    public static final int JUGADOR_2 = 1;
    public static final int NUM_JUGADORES = 2;
    
    private JPanel tablero[][];
    private int estadoTablero[][];
    private ImageIcon casillas[];
    private int numColumnas;
    private int numFilas;
    private ArrayList<Jugador> jugadores;
    private int puntuaciones[];
    
    public Conecta4JFrame(int _numColumnas, int _numFilas, ArrayList<Jugador> _jugadores){
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setResizable(false);
        numColumnas = _numColumnas;
        numFilas = _numFilas;
        tablero = new JPanel[numFilas][numColumnas];
        for(int i=0; i<numFilas; i++){
            for(int j=0; j<numColumnas; j++){
                tablero[i][j] = new JPanel();
            }
        }
        estadoTablero = new int[numFilas][numColumnas];
            for(int i=0; i<numFilas; i++){
                for(int j=0; j<numColumnas; j++){
                    estadoTablero[i][j] = CASILLA_VACIA;
                }
            }
        casillas = new ImageIcon[5];
        casillas[CASILLA_VACIA] = new ImageIcon("src/main/java/graficos/casilla_vacia.png");
        casillas[CASILLA_J1] = new ImageIcon("src/main/java/graficos/casilla_j1.png");
        casillas[CASILLA_J2] = new ImageIcon("src/main/java/graficos/casilla_j2.png");
        casillas[VICTORIA_J1] = new ImageIcon("src/main/java/graficos/victoria_j1.png");
        casillas[VICTORIA_J2] = new ImageIcon("src/main/java/graficos/victoria_j2.png");
        jugadores = _jugadores;
        puntuaciones = new int[2];
        for(int i=0; i<NUM_JUGADORES; i++){
            puntuaciones[i] = 0;
        }
        iniciarTablero();
        visualizarTablero();
    }
    
    public void iniciarTablero(){
        for(int i=0; i<numFilas; i++){
            for(int j=0; j<numColumnas; j++){
                tablero[i][j].removeAll();
                tablero[i][j].add(new JLabel(casillas[CASILLA_VACIA]));
                tablero[i][j].repaint();
                estadoTablero[i][j] = CASILLA_VACIA;
            }
        }
    }
    
    public void visualizarTablero(){
        Container container = this.getContentPane();
        container.removeAll();
        JPanel interfaz = new JPanel();
//        interfaz.setLayout(new GridLayout(2,1));
        interfaz.setLayout(new BoxLayout(interfaz, BoxLayout.Y_AXIS));
        JPanel infoJugadores = new JPanel();
//        infoJugadores.setLayout();
        Font fuenteNombres = new Font("SansSerif", Font.BOLD, 20);
        Font fuentePuntuacion = new Font("SansSerif", Font.BOLD, 50);
        JLabel imagenFichaJ1 = new JLabel(casillas[CASILLA_J1]);
        JLabel nombreJ1 = new JLabel(jugadores.get(JUGADOR_1).getNombre());
        nombreJ1.setFont(fuenteNombres);
        JLabel puntuacionJ1 = new JLabel(Integer.toString(puntuaciones[JUGADOR_1]));
        puntuacionJ1.setFont(fuentePuntuacion);
        JLabel guion = new JLabel(" - ");
        guion.setFont(fuentePuntuacion);
        JLabel puntuacionJ2 = new JLabel(Integer.toString(puntuaciones[JUGADOR_2]));
        puntuacionJ2.setFont(fuentePuntuacion);
        JLabel nombreJ2 = new JLabel(jugadores.get(JUGADOR_2).getNombre());
        nombreJ2.setFont(fuenteNombres);
        JLabel imagenFichaJ2 = new JLabel(casillas[CASILLA_J2]);
        infoJugadores.add(imagenFichaJ1);
        infoJugadores.add(nombreJ1);
        infoJugadores.add(puntuacionJ1);
        infoJugadores.add(guion);
        infoJugadores.add(puntuacionJ2);
        infoJugadores.add(nombreJ2);
        infoJugadores.add(imagenFichaJ2);
        JPanel tableroVisual = new JPanel();
        tableroVisual.setLayout(new GridLayout(numFilas,numColumnas));
        for(int i=0; i<numFilas; i++){
            for(int j=0; j<numColumnas; j++){
                tableroVisual.add(tablero[i][j]);
            }
        }
        interfaz.add(infoJugadores);
        interfaz.add(tableroVisual);
        container.add(interfaz);
        this.pack();
    }
    
    public void instertarFicha(int jugador, int columna, int fila){
        tablero[fila][columna].removeAll();
        tablero[fila][columna].add(new JLabel(casillas[jugador]));
        tablero[fila][columna].repaint();
        estadoTablero[fila][columna] = jugador;
        visualizarTablero();
    }
    
    public void sumarVictoria(int jugador){
        puntuaciones[jugador]++;
        visualizarTablero();
    }
    
    public void marcarVictoria(int fila, int columna, int jugador){
        ArrayList<Pair<Integer,Integer>> cas = ComprobarTablero(fila, columna, jugador);
        for(int i=0; i<cas.size(); i++){
            tablero[cas.get(i).getKey()][cas.get(i).getValue()].removeAll();
            tablero[cas.get(i).getKey()][cas.get(i).getValue()].add(new JLabel(casillas[jugador+2]));
            tablero[cas.get(i).getKey()][cas.get(i).getValue()].repaint();
        }
        visualizarTablero();
    }
    
    public void nuevaRonda(){
        jugadores.add(jugadores.remove(JUGADOR_1));
        int tmp = puntuaciones[JUGADOR_1];
        puntuaciones[JUGADOR_1] = puntuaciones[JUGADOR_2];
        puntuaciones[JUGADOR_2] = tmp;
        iniciarTablero();
    }
    
    public ArrayList<Pair<Integer,Integer>> ComprobarTablero(int coordX, int coordY, int jugador){
        ArrayList<Pair<Integer,Integer>> cadena = new ArrayList<>();
        Pair<Integer,Integer> movInicial = new Pair<>(coordX, coordY);
        int coordXact, coordYact;
        boolean fin;
        //Vertical.
        cadena.clear();
        cadena.add(movInicial);
        //Hacia arriba.
        coordXact = coordX;
        coordYact = coordY-1; //Se evita comprobar la casilla central.
        fin = false;
        while((coordXact >= 0 && coordXact < estadoTablero.length && coordYact >= 0 && coordYact < estadoTablero[0].length) && !fin){
            if((estadoTablero[coordXact][coordYact] == jugador)){
                cadena.add(new Pair<>(coordXact,coordYact));
                coordYact--;
            }else{
                fin = true;
            }
            if(cadena.size()==4){
                return cadena;
            }
        }
        //Hacia abajo.
        coordXact = coordX;
        coordYact = coordY+1; //Se evita comprobar la casilla central.
        fin = false;
        while((coordXact >= 0 && coordXact < estadoTablero.length && coordYact >= 0 && coordYact < estadoTablero[0].length) && !fin){
            if((estadoTablero[coordXact][coordYact] == jugador)){
                cadena.add(new Pair<>(coordXact,coordYact)); 
                coordYact++;
            }else{
                fin = true;
            }
            if(cadena.size()==4){
                return cadena;
            }
        }
        
        //Horizontal
        cadena.clear();
        cadena.add(movInicial);
        //Hacia la izquierda.
        coordXact = coordX-1; //Se evita comprobar la casilla central.
        coordYact = coordY;
        fin = false;
        while((coordXact >= 0 && coordXact < estadoTablero.length && coordYact >= 0 && coordYact < estadoTablero[0].length) && !fin){
            if((estadoTablero[coordXact][coordYact] == jugador)){
                cadena.add(new Pair<>(coordXact,coordYact));
                coordXact--;
            }else{
                fin = true;
            }
            if(cadena.size()==4){
                return cadena;
            }
        }
        //Hacia la derecha.
        coordXact = coordX+1; //Se evita comprobar la casilla central.
        coordYact = coordY;
        fin = false;
        while((coordXact >= 0 && coordXact < estadoTablero.length && coordYact >= 0 && coordYact < estadoTablero[0].length) && !fin){
            if((estadoTablero[coordXact][coordYact] == jugador)){
                cadena.add(new Pair<>(coordXact,coordYact));  
                coordXact++;
            }else{
                fin = true;
            }
            if(cadena.size()==4){
                return cadena;
            }
        }
        
        //Diagonal desde la izquierda-arriba
        cadena.clear();
        cadena.add(movInicial);
        //Hacia arriba.
        coordXact = coordX-1; 
        coordYact = coordY-1; //Se evita comprobar la casilla central.
        fin = false;
        while((coordXact >= 0 && coordXact < estadoTablero.length && coordYact >= 0 && coordYact < estadoTablero[0].length) && !fin){
            if((estadoTablero[coordXact][coordYact] == jugador)){
                cadena.add(new Pair<>(coordXact,coordYact));
                coordXact--;
                coordYact--;
            }else{
                fin = true;
            }
            if(cadena.size()==4){
                return cadena;
            }
        }
        //Hacia la abajo.
        coordXact = coordX+1; 
        coordYact = coordY+1; //Se evita comprobar la casilla central.
        fin = false;
        while((coordXact >= 0 && coordXact < estadoTablero.length && coordYact >= 0 && coordYact < estadoTablero[0].length) && !fin){
            if((estadoTablero[coordXact][coordYact] == jugador)){
                cadena.add(new Pair<>(coordXact,coordYact));
                coordXact++;
                coordYact++;  
            }else{
                fin = true;
            }
            if(cadena.size()==4){
                return cadena;
            }
        }
        
        //Diagonal desde la derecha-abajo
        cadena.clear();
        cadena.add(movInicial);
        //Hacia abajo.
        coordXact = coordX-1; 
        coordYact = coordY+1; //Se evita comprobar la casilla central.
        fin = false;
        while((coordXact >= 0 && coordXact < estadoTablero.length && coordYact >= 0 && coordYact < estadoTablero[0].length) && !fin){
            if((estadoTablero[coordXact][coordYact] == jugador)){
                cadena.add(new Pair<>(coordXact,coordYact));
                coordXact--;
                coordYact++;  
            }else{
                fin = true;
            }
            if(cadena.size()==4){
                return cadena;
            }
        }
        //Hacia arriba.
        coordXact = coordX+1; 
        coordYact = coordY-1; //Se evita comprobar la casilla central.
        fin = false;
        while((coordXact >= 0 && coordXact < estadoTablero.length && coordYact >= 0 && coordYact < estadoTablero[0].length) && !fin){
            if((estadoTablero[coordXact][coordYact] == jugador)){
                cadena.add(new Pair<>(coordXact,coordYact));  
                coordXact++;
                coordYact--;
            }else{
                fin = true;
            }
            if(cadena.size()==4){
                return cadena;
            }
        }
        cadena.clear();
        return cadena;
    }
    
    public int getPuntuacionJ1(){
        return puntuaciones[JUGADOR_1];
    }
    
    public int getPuntuacionJ2(){
        return puntuaciones[JUGADOR_2];
    }
    
}
